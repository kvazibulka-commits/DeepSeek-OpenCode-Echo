#!/usr/bin/env python3
"""ЭХО · новый сервер.

Чистый API для Android-клиента:
  GET  /api/state, /api/health, /api/playlist, /api/cover/<id>
  POST /api/signal, /api/react, /api/drop_capsule, /api/bump_encounter,
       /api/needle_sync, /api/send_warmth, /api/wave_impulse, /api/delete_signal

Никаких внешних ИИ-сервисов: архетипы и акустика детерминированы.
SQLite + WAL, один процесс, один поток на запрос.
"""

from __future__ import annotations

import base64
import hmac
import ipaddress
import json
import logging
import os
import secrets
import socket
import sqlite3
import sys
import threading
import time
import urllib.parse
from collections import OrderedDict
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    from .personas import persona_for
    from .resonance import (
        Acoustics,
        distance_label,
        infer_acoustics,
        resonance,
        seed_int,
    )
except ImportError:  # запуск как `python server/echo_server.py`
    from personas import persona_for
    from resonance import (
        Acoustics,
        distance_label,
        infer_acoustics,
        resonance,
        seed_int,
    )

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.path.join(BASE_DIR, "echo_config.json")
DB_PATH = os.path.join(BASE_DIR, "echo_memory.db")
TEMPLATES_DIR = os.path.join(BASE_DIR, "templates")
DEFAULT_COLOR = 0xFF38BDF8
MAX_TEXT = 240
MAX_COVER_BYTES = 2_500_000
MAX_BODY_BYTES = 4_000_000

ZONES = [
    "Вечерняя улица",
    "Сретенка",
    "Камерные бары",
    "Парк Горького",
    "Набережная",
    "Покровка",
    "Ночные кофейни",
    "Северный лесопарк",
]

ZONE_COORDS = {
    "Вечерняя улица": (55.7602, 37.6185),
    "Сретенка": (55.7655, 37.6310),
    "Камерные бары": (55.7620, 37.6100),
    "Парк Горького": (55.7298, 37.6019),
    "Набережная": (55.7460, 37.6060),
    "Покровка": (55.7590, 37.6460),
    "Ночные кофейни": (55.7680, 37.5920),
    "Северный лесопарк": (55.8420, 37.5800),
}

CITY_ZONES = [
    ("Вечерняя улица", "тёплый вечер", "Кино", 0xFF38BDF8),
    ("Сретенка", "пост-панк", "Молчат Дома", 0xFFA855F7),
    ("Камерные бары", "бархатный джаз", "Nina Simone", 0xFFFFB800),
    ("Парк Горького", "инди", "Stereolab", 0xFF34D399),
    ("Набережная", "ночной эмбиент", "Burial", 0xFF00F0FF),
    ("Покровка", "лоу-фай", "Кино", 0xFFF87171),
    ("Ночные кофейни", "даунтемпо", "Massive Attack", 0xFFFFB800),
    ("Северный лесопарк", "холодная акустика", "Ulver", 0xFF34D399),
]

logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("echo")

DB_LOCK = threading.RLock()
REACTIONS_LOCK = threading.RLock()
EVENT_LOCK = threading.Lock()
EVENT_LAST = 0

RECENT_REACTIONS: list[dict] = []
THERMAL_PATCHES: list[dict] = []
NEEDLE_SYNCS: dict[str, dict] = {}

COVERS: "OrderedDict[str, tuple[str, bytes]]" = OrderedDict()
MAX_COVERS = 40

DEMO_ENABLED = os.environ.get("ECHO_DEMO_SIGNALS", "1") != "0"
DEMO_MATCH_ID = "sig_demo_match"
DEMO_MISMATCH_ID = "sig_demo_mismatch"
DEMO_IDS = (DEMO_MATCH_ID, DEMO_MISMATCH_ID)


# ── Конфиг ───────────────────────────────────────────────────────────────────

def load_config() -> dict:
    config: dict = {}
    if os.path.exists(CONFIG_PATH):
        try:
            with open(CONFIG_PATH, "r", encoding="utf-8") as handle:
                config = json.load(handle)
        except Exception as exc:
            logger.warning("Не удалось прочитать echo_config.json: %s", exc)

    token = os.environ.get("ECHO_TOKEN", "").strip() or str(config.get("echo_token", "")).strip()
    if not token:
        token = secrets.token_hex(16)
        config["echo_token"] = token
        try:
            with open(CONFIG_PATH, "w", encoding="utf-8") as handle:
                json.dump(config, handle, ensure_ascii=False, indent=2)
        except Exception as exc:
            logger.warning("Не удалось сохранить echo_config.json: %s", exc)

    config["echo_token"] = token
    return config


SERVER_CONFIG = load_config()
ECHO_TOKEN = str(SERVER_CONFIG["echo_token"])
PORT = int(os.environ.get("PORT", "8080"))


# ── Мелкие утилиты ───────────────────────────────────────────────────────────

def sanitize_text(value: object, limit: int = MAX_TEXT) -> str:
    text = str(value or "")
    text = "".join(ch for ch in text if ch == "\n" or ch >= " ")
    text = text.replace("\r", " ").strip()
    return text[:limit]


def ensure_color(value: object) -> int:
    try:
        color = int(value)
    except (TypeError, ValueError):
        return DEFAULT_COLOR
    color &= 0xFFFFFFFF
    if color >> 24 == 0:
        color |= 0xFF000000
    return color


def normalize_zone(value: object) -> str:
    zone = sanitize_text(value, 64)
    return zone if zone in ZONES else ZONES[0]


def is_junk_track(artist: str, title: str) -> bool:
    probe = f"{artist} {title}".lower()
    if not title.strip():
        return True
    markers = (
        "прямой эфир", "прямая трансляция", "голосовое сообщение",
        "видеосообщение", "livestream", "mash", "топор",
    )
    if any(marker in probe for marker in markers):
        return True
    if any(ext in probe for ext in (".mp4", ".mkv", ".webm", ".mov", ".avi")):
        return True
    if title.strip().isdigit() and len(title.strip()) >= 5:
        return True
    return len(title) > 180


def next_event_id() -> int:
    global EVENT_LAST
    with EVENT_LOCK:
        candidate = int(time.time() * 1000)
        EVENT_LAST = max(EVENT_LAST + 1, candidate)
        return EVENT_LAST


def zone_coords(zone: str, seed_text: str) -> tuple[float, float]:
    base_lat, base_lon = ZONE_COORDS.get(zone, ZONE_COORDS[ZONES[0]])
    seed = seed_int(zone, seed_text)
    lat = base_lat + ((seed % 1000) - 500) / 100000.0
    lon = base_lon + (((seed >> 10) % 1000) - 500) / 100000.0
    return round(lat, 6), round(lon, 6)


def cover_mime(data: bytes) -> str:
    if data.startswith(b"\x89PNG"):
        return "image/png"
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data.startswith(b"RIFF") and b"WEBP" in data[:16]:
        return "image/webp"
    return "image/jpeg"


def store_cover(device_id: str, data: bytes, mime: str | None = None) -> None:
    if not data or len(data) > MAX_COVER_BYTES:
        return
    COVERS[device_id] = (mime or cover_mime(data), data)
    COVERS.move_to_end(device_id)
    while len(COVERS) > MAX_COVERS:
        COVERS.popitem(last=False)


def decode_cover(payload: object) -> bytes | None:
    if not isinstance(payload, str) or not payload:
        return None
    try:
        data = base64.b64decode(payload, validate=False)
    except Exception:
        return None
    if not data or len(data) > MAX_COVER_BYTES:
        return None
    return data


# ── База ─────────────────────────────────────────────────────────────────────

SCHEMA = """
CREATE TABLE IF NOT EXISTS devices (
    id TEXT PRIMARY KEY,
    artist TEXT NOT NULL,
    title TEXT NOT NULL,
    album TEXT DEFAULT '',
    source_name TEXT DEFAULT '',
    persona_title TEXT DEFAULT '',
    persona_observation TEXT DEFAULT '',
    mood_label TEXT DEFAULT '',
    color_hex INTEGER DEFAULT 4281908728,
    avatar_petals INTEGER DEFAULT 6,
    zone_name TEXT DEFAULT '',
    distance_meters INTEGER DEFAULT 50,
    angle_degrees REAL DEFAULT 45,
    distance_ratio REAL DEFAULT 0.45,
    compatibility_percent INTEGER DEFAULT 80,
    encounters_count INTEGER DEFAULT 1,
    reactions_json TEXT DEFAULT '{}',
    lat REAL DEFAULT 55.7602,
    lon REAL DEFAULT 37.6185,
    updated_at REAL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS capsules (
    id TEXT PRIMARY KEY,
    creator_id TEXT,
    artist TEXT,
    title TEXT,
    album TEXT DEFAULT '',
    mood_label TEXT,
    persona_title TEXT,
    zone_name TEXT,
    secret_message TEXT,
    distance_meters INTEGER,
    angle_degrees REAL,
    distance_ratio REAL,
    compatibility_percent INTEGER,
    is_unlocked INTEGER DEFAULT 1,
    is_venue INTEGER DEFAULT 0,
    venue_name TEXT DEFAULT '',
    min_resonance INTEGER DEFAULT 0,
    secret_perk TEXT DEFAULT '',
    lat REAL,
    lon REAL,
    created_at REAL
);
CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT,
    artist TEXT,
    title TEXT,
    album TEXT DEFAULT '',
    persona_title TEXT,
    zone_name TEXT,
    played_at TEXT,
    timestamp REAL
);
CREATE INDEX IF NOT EXISTS idx_devices_updated ON devices(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_history_device ON history(device_id, id DESC);
"""


def connect() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    conn.execute("PRAGMA busy_timeout=10000")
    return conn


def ensure_column(conn: sqlite3.Connection, table: str, column: str, declaration: str) -> None:
    columns = {row["name"] for row in conn.execute(f"PRAGMA table_info({table})")}
    if column not in columns:
        conn.execute(f"ALTER TABLE {table} ADD COLUMN {column} {declaration}")


def init_db() -> None:
    with DB_LOCK:
        conn = connect()
        conn.executescript(SCHEMA)
        ensure_column(conn, "devices", "album", "TEXT DEFAULT ''")
        ensure_column(conn, "capsules", "album", "TEXT DEFAULT ''")
        ensure_column(conn, "history", "album", "TEXT DEFAULT ''")
        conn.commit()
        conn.close()


def ensure_demo_signals() -> None:
    """Два статичных маяка для проверки сонара и луча на эмуляторе.

    Совпадение (92 %) и антипод (58 %) не зависят от вашего трека — это демо.
    Живой трек ПК добавляется отдельно через SMTC.
    """
    if not DEMO_ENABLED:
        return

    now = time.time()
    specs = [
        {
            "id": DEMO_MATCH_ID,
            "title": "Тихий маяк",
            "album": "Демо-созвучие",
            "persona": "Маяк тихого созвучия",
            "observation": "Стоит в тумане и звучит в вашей тональности.",
            "mood": "Демо-созвучие",
            "color": 0xFFFFD700,
            "petals": 7,
            "distance": 62,
            "angle": 48.0,
            "ratio": 0.38,
            "compat": 92,
        },
        {
            "id": DEMO_MISMATCH_ID,
            "title": "Холодный шум",
            "album": "Демо-антипод",
            "persona": "Маяк холодного шума",
            "observation": "Звучит намеренно мимо: для контраста на сонаре.",
            "mood": "Демо-антипод",
            "color": 0xFF64748B,
            "petals": 5,
            "distance": 148,
            "angle": 214.0,
            "ratio": 0.72,
            "compat": 58,
        },
    ]

    with DB_LOCK:
        conn = connect()
        try:
            present = {
                row["id"]
                for row in conn.execute(
                    "SELECT id FROM devices WHERE id IN (?, ?)",
                    DEMO_IDS,
                )
            }
            for spec in specs:
                if spec["id"] in present:
                    conn.execute(
                        "UPDATE devices SET updated_at = ? WHERE id = ?",
                        (now, spec["id"]),
                    )
                    continue
                lat, lon = zone_coords("Вечерняя улица", spec["id"])
                conn.execute(
                    """
                    INSERT OR REPLACE INTO devices (
                        id, artist, title, album, source_name, persona_title, persona_observation,
                        mood_label, color_hex, avatar_petals, zone_name, distance_meters,
                        angle_degrees, distance_ratio, compatibility_percent, encounters_count,
                        reactions_json, lat, lon, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, '{}', ?, ?, ?)
                    """,
                    (
                        spec["id"], "ЭХО", spec["title"], spec["album"], "Демо-маяк",
                        spec["persona"], spec["observation"], spec["mood"],
                        ensure_color(spec["color"]), spec["petals"], "Вечерняя улица",
                        spec["distance"], spec["angle"], spec["ratio"], spec["compat"],
                        lat, lon, now,
                    ),
                )
            conn.commit()
        finally:
            conn.close()


def record_history(
    conn: sqlite3.Connection,
    device_id: str,
    artist: str,
    title: str,
    album: str,
    persona: str,
    zone: str,
) -> None:
    last = conn.execute(
        "SELECT artist, title FROM history WHERE device_id = ? ORDER BY id DESC LIMIT 1",
        (device_id,),
    ).fetchone()
    if last and last["artist"].lower() == artist.lower() and last["title"].lower() == title.lower():
        return
    conn.execute(
        "INSERT INTO history (device_id, artist, title, album, persona_title, zone_name, played_at, timestamp)"
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        (device_id, artist, title, album, persona, zone, datetime.now().strftime("%H:%M"), time.time()),
    )
    conn.execute(
        "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 500)"
    )


def upsert_signal(
    device_id: str,
    artist: str,
    title: str,
    source_name: str,
    zone_name: str,
    album: str = "",
    cover_bytes: bytes | None = None,
) -> bool:
    artist = sanitize_text(artist, 160)
    title = sanitize_text(title, 180)
    album = sanitize_text(album, 160)
    if is_junk_track(artist, title):
        return False

    zone = normalize_zone(zone_name)
    persona = persona_for(artist, title, album)
    acoustics = infer_acoustics(artist, title, album, persona["moodLabel"])

    seed = seed_int(device_id, artist, title)
    distance = 25 + seed % 175
    angle = float(seed % 360)
    ratio = round(min(0.88, max(0.22, distance / 220.0)), 2)
    lat, lon = zone_coords(zone, device_id)
    now = time.time()

    with DB_LOCK:
        conn = connect()
        try:
            existing = conn.execute(
                "SELECT encounters_count, reactions_json FROM devices WHERE id = ?",
                (device_id,),
            ).fetchone()
            encounters = int(existing["encounters_count"]) if existing else 1
            reactions_json = existing["reactions_json"] if existing else "{}"

            conn.execute(
                """
                INSERT OR REPLACE INTO devices (
                    id, artist, title, album, source_name, persona_title, persona_observation,
                    mood_label, color_hex, avatar_petals, zone_name, distance_meters,
                    angle_degrees, distance_ratio, compatibility_percent, encounters_count,
                    reactions_json, lat, lon, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    device_id, artist, title, album,
                    sanitize_text(source_name, 80) or "ЭХО",
                    persona["personaTitle"], persona["personaObservation"],
                    persona["moodLabel"], ensure_color(persona["colorHex"]),
                    int(persona["avatarPetals"]), zone, distance, angle, ratio,
                    78 + seed % 20, encounters, reactions_json, lat, lon, now,
                ),
            )
            record_history(
                conn, device_id, artist, title, album,
                persona["personaTitle"], zone,
            )
            conn.commit()
        finally:
            conn.close()

    if cover_bytes:
        store_cover(device_id, cover_bytes)
    return True


# ── Состояние ────────────────────────────────────────────────────────────────

def build_state(device_id: str) -> dict:
    ensure_demo_signals()
    now = time.time()
    cutoff = now - 90 * 60

    with DB_LOCK:
        conn = connect()
        try:
            conn.execute(
                "DELETE FROM devices WHERE updated_at < ? AND id NOT LIKE 'sig_%'",
                (cutoff,),
            )
            conn.commit()

            rows = conn.execute(
                "SELECT * FROM devices ORDER BY updated_at DESC LIMIT 35"
            ).fetchall()

            my_row = next((row for row in rows if row["id"] == device_id), None)
            my_acoustics: Acoustics | None = None
            if my_row:
                my_acoustics = infer_acoustics(
                    my_row["artist"], my_row["title"], my_row["album"] or "",
                    my_row["mood_label"] or "",
                )

            history_rows = conn.execute(
                "SELECT artist, title, persona_title, zone_name, played_at, album"
                " FROM history ORDER BY id DESC LIMIT 15"
            ).fetchall()

            capsule_rows = conn.execute(
                "SELECT * FROM capsules ORDER BY created_at DESC LIMIT 30"
            ).fetchall()
        finally:
            conn.close()

    signals = []
    zone_counts: dict[str, list[dict]] = {}

    for row in rows:
        if row["id"] == device_id:
            continue
        is_demo = row["id"] in DEMO_IDS
        age_min = 0 if is_demo else max(0, int((now - float(row["updated_at"] or 0)) / 60))
        is_ghost = False if is_demo else age_min > 5

        other = infer_acoustics(
            row["artist"], row["title"], row["album"] or "",
            row["mood_label"] or "",
        )
        if is_demo:
            if row["id"] == DEMO_MATCH_ID:
                percent = 92
                story = "Демо-созвучие: маяк совпадает с вашим треком — так выглядит резонанс."
            else:
                percent = 58
                story = "Демо-антипод: намеренно не совпадает — для контраста на сонаре."
        elif my_acoustics:
            percent, story = resonance(my_acoustics, other)
        else:
            percent = int(row["compatibility_percent"] or 80)
            story = "Тёплый вечерний резонанс: звуковые траектории пересеклись в этой точке города."

        encounters = int(row["encounters_count"] or 1)
        if encounters > 1:
            story += f" Ваши волны встречаются уже {encounters}-й раз за неделю."

        signal = {
            "id": row["id"],
            "artist": row["artist"] or "Неизвестный исполнитель",
            "title": row["title"] or "Без названия",
            "album": row["album"] or "",
            "resonanceStory": story,
            "sourceName": row["source_name"] or "ЭХО",
            "distanceMeters": int(row["distance_meters"] or 50),
            "angleDegrees": float(row["angle_degrees"] or 45),
            "distanceRatio": float(row["distance_ratio"] or 0.45),
            "personaTitle": row["persona_title"] or "Архивариус забытых кассет",
            "personaObservation": (
                f"Призрачный след: звучал {age_min} мин назад. {row['persona_observation']}"
                if is_ghost else (row["persona_observation"] or "Слушает музыку в вечерней тишине.")
            ),
            "moodLabel": (
                f"Призрачный след ({age_min} мин)" if is_ghost
                else (row["mood_label"] or "Тёплый винил")
            ),
            "compatibilityPercent": percent,
            "bpm": other.bpm,
            "camelotKey": other.camelot,
            "genre": other.genre,
            "resonanceDays": max(1, encounters),
            "encountersCount": max(1, encounters),
            "colorHex": ensure_color(row["color_hex"]),
            "avatarPetals": int(row["avatar_petals"] or 6),
            "zoneName": row["zone_name"] or ZONES[0],
            "lat": float(row["lat"] or 55.7602),
            "lon": float(row["lon"] or 37.6185),
            "updatedAt": float(row["updated_at"] or 0),
            "isGhost": is_ghost,
            "ageMinutes": age_min,
        }
        if row["id"] in COVERS:
            signal["coverUrl"] = f"/api/cover/{row['id']}"
        signals.append(signal)
        zone_counts.setdefault(signal["zoneName"], []).append(signal)

    capsules = []
    for row in capsule_rows:
        min_res = int(row["min_resonance"] or 0)
        compat = int(row["compatibility_percent"] or 90)
        capsules.append({
            "id": row["id"],
            "creatorId": row["creator_id"] or "",
            "artist": row["artist"] or "Неизвестный автор",
            "title": row["title"] or "Без названия",
            "album": row["album"] or "",
            "moodLabel": row["mood_label"] or "Тёплый тайник",
            "personaTitle": row["persona_title"] or "Музыкальный тайник",
            "zoneName": row["zone_name"] or ZONES[0],
            "secretMessage": row["secret_message"] or "",
            "distanceMeters": int(row["distance_meters"] or 65),
            "angleDegrees": float(row["angle_degrees"] or 120),
            "distanceRatio": float(row["distance_ratio"] or 0.5),
            "compatibilityPercent": compat,
            "isUnlocked": min_res <= 0 or compat >= min_res,
            "isVenue": bool(row["is_venue"]),
            "venueName": row["venue_name"] or "",
            "minResonance": min_res,
            "secretPerk": row["secret_perk"] or "",
            "createdAt": float(row["created_at"] or 0),
        })

    traces = [
        f"[{row['played_at']}] {row['persona_title']} ({row['zone_name']}): "
        f"{row['artist']} — {row['title']}"
        for row in history_rows
    ]
    if not traces:
        traces = ["Сонар сканирует тишину. Включите музыку — и город ответит."]

    zones = []
    for name, vibe, artists, color in CITY_ZONES:
        bucket = zone_counts.get(name, [])
        zones.append({
            "placeName": name,
            "dominantVibe": bucket[0]["moodLabel"] if bucket else vibe,
            "recentArtists": ", ".join(list(dict.fromkeys(item["artist"] for item in bucket))[:4]) if bucket else artists,
            "listenersCount": len(bucket),
            "lat": ZONE_COORDS[name][0],
            "lon": ZONE_COORDS[name][1],
            "fogColorHex": ensure_color(bucket[0]["colorHex"] if bucket else color),
            "fogRadius": min(750, 480 + len(bucket) * 60) if bucket else 450,
        })

    with REACTIONS_LOCK:
        snapshot = list(RECENT_REACTIONS)

    banner = None
    for reaction in reversed(snapshot):
        if now - reaction["time"] >= 60:
            continue
        target = str(reaction.get("targetId", ""))
        sender = str(reaction.get("senderId", ""))
        if sender == device_id:
            continue
        if target in (device_id, "broadcast", "all", "", "pc_studio_node") or target.startswith(("sig_", "cap_")):
            banner = {
                "id": reaction["id"],
                "targetId": target,
                "senderId": sender,
                "symbol": reaction.get("symbol", "✦"),
                "message": reaction.get("message", "Вам отправлен импульс"),
                "time": reaction["time"],
            }
            break

    history_reactions = [
        {
            "id": reaction["id"],
            "targetId": reaction.get("targetId", ""),
            "senderId": reaction.get("senderId", ""),
            "symbol": reaction.get("symbol", "✦"),
            "message": reaction.get("message", "Спасибо за созвучие"),
            "time": reaction["time"],
        }
        for reaction in snapshot
        if reaction.get("targetId") in (device_id, "") or reaction.get("senderId") == device_id
    ]

    all_reactions = [
        {
            "id": reaction["id"],
            "targetId": reaction.get("targetId", ""),
            "senderId": reaction.get("senderId", ""),
            "symbol": reaction.get("symbol", "✦"),
            "message": reaction.get("message", "Спасибо за созвучие"),
            "time": reaction["time"],
        }
        for reaction in snapshot[-25:]
    ]

    eligible = [s for s in signals if not s["isGhost"] and s["compatibilityPercent"] >= 75]
    lagrange = None
    if len(eligible) >= 3:
        triplet = eligible[:3]
        lagrange = {
            "active": True,
            "angleDegrees": round(sum(s["angleDegrees"] for s in triplet) / 3.0, 1),
            "distanceRatio": round(sum(s["distanceRatio"] for s in triplet) / 3.0, 2),
            "secretTrack": "Burial — Archangel (Nocturnal Re-edit)",
            "secretMessage": "Акустическое затмение: три ночных слушателя образовали Точку Лагранжа.",
            "participants": [s["id"] for s in triplet],
        }

    with REACTIONS_LOCK:
        THERMAL_PATCHES[:] = [p for p in THERMAL_PATCHES if now - p["time"] <= 15]
        thermal = [
            {
                "id": patch["id"],
                "targetId": patch["targetId"],
                "senderId": patch["senderId"],
                "x": patch["x"],
                "y": patch["y"],
                "ageSec": round(now - patch["time"], 1),
            }
            for patch in THERMAL_PATCHES
            if now - patch["time"] <= 15
            and (
                patch["targetId"] in (device_id, "")
                or patch["senderId"] == device_id
            )
        ]

    needles = [
        entry["targetId"]
        for sender, entry in NEEDLE_SYNCS.items()
        if now - entry["time"] <= 60
        and (sender == device_id or entry["targetId"] == device_id)
    ]

    my_persona = ""
    my_mood = ""
    my_observation = ""
    my_reactions: dict = {}
    my_color = DEFAULT_COLOR
    if my_row:
        my_persona = my_row["persona_title"] or ""
        my_mood = my_row["mood_label"] or ""
        my_observation = my_row["persona_observation"] or ""
        my_color = ensure_color(my_row["color_hex"])
        try:
            my_reactions = json.loads(my_row["reactions_json"] or "{}")
        except Exception:
            my_reactions = {}

    return {
        "status": "ok",
        "serverTime": int(now),
        "signals": signals,
        "capsules": capsules,
        "eveningTraces": traces,
        "cityZones": zones,
        "incomingBanner": banner,
        "reactionsHistory": history_reactions[-25:],
        "allReactions": all_reactions,
        "myReactions": my_reactions,
        "myRefinedMood": my_mood,
        "myCuratedPersona": my_persona,
        "myCuratedObservation": my_observation,
        "myColorHex": my_color,
        "lagrangePoint": lagrange,
        "thermalPatches": thermal,
        "needleSyncedTargets": needles,
        "isDawnMode": 4 <= datetime.now().hour < 6,
    }


# ── Реакции и тайники ────────────────────────────────────────────────────────

def add_reaction(target_id: str, sender_id: str, symbol: str, message: str) -> None:
    with REACTIONS_LOCK:
        RECENT_REACTIONS.append({
            "id": next_event_id(),
            "targetId": target_id,
            "senderId": sender_id,
            "symbol": symbol or "✦",
            "message": sanitize_text(message, 200) or "Спасибо за созвучие",
            "time": time.time(),
        })
        del RECENT_REACTIONS[:-60]
        THERMAL_PATCHES[:] = [p for p in THERMAL_PATCHES if time.time() - p["time"] <= 15]


def bump_reaction_counter(target_id: str, symbol: str) -> None:
    with DB_LOCK:
        conn = connect()
        try:
            row = conn.execute("SELECT reactions_json FROM devices WHERE id = ?", (target_id,)).fetchone()
            if not row:
                return
            try:
                mapping = json.loads(row["reactions_json"] or "{}")
            except Exception:
                mapping = {}
            mapping[symbol] = int(mapping.get(symbol, 0)) + 1
            conn.execute(
                "UPDATE devices SET reactions_json = ? WHERE id = ?",
                (json.dumps(mapping, ensure_ascii=False), target_id),
            )
            conn.commit()
        finally:
            conn.close()


def drop_capsule(payload: dict) -> None:
    creator = sanitize_text(payload.get("creatorId"), 80) or "local_user"
    artist = sanitize_text(payload.get("artist"), 160) or "Sirotkin"
    title = sanitize_text(payload.get("title"), 180) or "Август"
    album = sanitize_text(payload.get("album"), 160)
    zone = normalize_zone(payload.get("zoneName"))
    message = sanitize_text(payload.get("secretMessage"), 220)
    is_venue = bool(payload.get("isVenue"))
    venue_name = sanitize_text(payload.get("venueName"), 120)
    min_res = max(0, min(99, int(payload.get("minResonance") or 0)))
    perk = sanitize_text(payload.get("secretPerk"), 160)

    persona = persona_for(artist, title, album)
    seed = seed_int(creator, artist, title, "capsule")
    capsule_id = f"cap_{next_event_id()}"
    distance = 25 + seed % 120
    angle = float(seed % 360)
    ratio = round(min(0.85, max(0.24, distance / 180.0)), 2)
    lat, lon = zone_coords(zone, capsule_id)
    compat = 72 + seed % 26

    with DB_LOCK:
        conn = connect()
        try:
            conn.execute(
                """
                INSERT INTO capsules (
                    id, creator_id, artist, title, album, mood_label, persona_title,
                    zone_name, secret_message, distance_meters, angle_degrees, distance_ratio,
                    compatibility_percent, is_unlocked, is_venue, venue_name,
                    min_resonance, secret_perk, lat, lon, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    capsule_id, creator, artist, title, album,
                    persona["moodLabel"],
                    venue_name if is_venue and venue_name else persona["personaTitle"],
                    zone, message, distance, angle, ratio, compat,
                    1 if is_venue else 0, venue_name, min_res, perk, lat, lon, time.time(),
                ),
            )
            conn.commit()
        finally:
            conn.close()


# ── Windows SMTC: ПК как источник сигнала ─────────────────────────────────────

def start_udp_broadcast() -> None:
    """Android-клиент ищет сервер по UDP 8888: ECHO_SERVER_DISCOVERY:<port>."""

    def worker() -> None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        payload = f"ECHO_SERVER_DISCOVERY:{PORT}".encode("utf-8")
        while True:
            try:
                sock.sendto(payload, ("255.255.255.255", 8888))
            except Exception:
                pass
            time.sleep(3.0)

    threading.Thread(target=worker, daemon=True).start()


def start_pc_bridge() -> None:
    if os.environ.get("ECHO_DISABLE_SMTC"):
        return

    def worker() -> None:
        try:
            import asyncio
            from winrt.windows.media.control import (
                GlobalSystemMediaTransportControlsSessionManager,
            )
        except Exception as exc:
            logger.info("Windows SMTC недоступен: %s", exc)
            return

        logger.info("Автоперехват музыки с ПК включён")
        last_track = ("", "", "")
        last_update = 0.0
        last_logged = ("", "", "")

        async def read_thumbnail(props) -> bytes | None:
            try:
                ref = getattr(props, "thumbnail", None)
                if ref is None:
                    return None
                stream = await ref.open_read_async()
                size = int(getattr(stream, "size", 0) or 0)
                if size <= 0 or size > MAX_COVER_BYTES:
                    return None
                from winrt.windows.storage.streams import DataReader

                reader = DataReader(stream)
                await reader.load_async(size)
                buffer = bytearray(size)
                reader.read_bytes(buffer)
                return bytes(buffer)
            except Exception:
                return None

        async def check() -> None:
            nonlocal last_track, last_update, last_logged
            try:
                manager = await GlobalSystemMediaTransportControlsSessionManager.request_async()
                session = manager.get_current_session()
                if not session:
                    return
                props = await session.try_get_media_properties_async()
                if not props:
                    return

                artist = (props.artist or "").strip()
                title = (props.title or "").strip()
                album = (getattr(props, "album_title", "") or "").strip()
                app_id = session.source_app_user_model_id or "ПК"
                app_name = app_id.split(".")[-1].split("!")[-1] or "ПК"
                if not title:
                    return

                current = (artist, title, album)
                now = time.time()
                if current == last_track and now - last_update < 15:
                    return
                last_track = current
                last_update = now

                cover = await read_thumbnail(props)
                upsert_signal(
                    device_id="pc_studio_node",
                    artist=artist or "Неизвестный исполнитель",
                    title=title,
                    source_name=f"ПК ({app_name})",
                    zone_name="Вечерняя улица",
                    album=album,
                    cover_bytes=cover,
                )
                if current != last_logged:
                    last_logged = current
                    logger.info("Сигнал ПК: %s — %s", artist, title)
            except Exception as exc:
                logger.debug("SMTC: %s", exc)

        while True:
            try:
                asyncio.run(check())
            except Exception as exc:
                logger.debug("SMTC цикл: %s", exc)
            time.sleep(2.5)

    threading.Thread(target=worker, daemon=True).start()


# ── HTTP ─────────────────────────────────────────────────────────────────────

_template_cache: dict[str, tuple[float, str]] = {}


def load_template(name: str) -> str:
    path = os.path.join(TEMPLATES_DIR, name)
    try:
        mtime = os.path.getmtime(path)
    except OSError:
        return "<h1>Шаблон не найден</h1>"
    cached = _template_cache.get(name)
    if cached and cached[0] == mtime:
        return cached[1]
    with open(path, "r", encoding="utf-8") as handle:
        text = handle.read()
    _template_cache[name] = (mtime, text)
    return text


def get_local_ip() -> str:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("8.8.8.8", 80))
        return sock.getsockname()[0]
    except Exception:
        return "127.0.0.1"
    finally:
        sock.close()


class EchoHandler(BaseHTTPRequestHandler):
    server_version = "EchoRadar/4.0"

    def log_message(self, fmt: str, *args) -> None:
        return

    # ── ответы ──
    def send_json(self, payload: dict, status: int = 200) -> None:
        raw = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Accept, X-Echo-Token, X-Device-Id")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def send_html(self, text: str, status: int = 200) -> None:
        raw = text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def send_bytes(self, data: bytes, mime: str, cache: str = "public, max-age=3600") -> None:
        self.send_response(200)
        self.send_header("Content-Type", mime)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", cache)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def read_json(self) -> dict:
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            return {}
        if length <= 0:
            return {}
        if length > MAX_BODY_BYTES:
            self.send_json({"status": "error", "message": "Payload too large"}, 413)
            return {}
        raw = self.rfile.read(length)
        try:
            data = json.loads(raw.decode("utf-8", errors="ignore"))
            return data if isinstance(data, dict) else {}
        except Exception:
            return {}

    # ── авторизация ──
    def is_local(self) -> bool:
        for header in ("CF-Connecting-IP", "CF-Ray", "X-Forwarded-For", "X-Real-IP", "Forwarded"):
            if self.headers.get(header):
                return False
        try:
            return ipaddress.ip_address(self.client_address[0]).is_loopback
        except ValueError:
            return False

    def check_token(self) -> bool:
        if self.is_local():
            return True
        provided = (
            self.headers.get("X-Echo-Token", "")
            or self.headers.get("X-API-Key", "")
        )
        if not provided:
            query = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            provided = (query.get("token") or [""])[0]
        if not provided:
            for part in self.headers.get("Cookie", "").split(";"):
                name, _, value = part.strip().partition("=")
                if name == "echo_token":
                    provided = value
                    break
        return bool(provided) and hmac.compare_digest(provided, ECHO_TOKEN)

    # ── маршруты ──
    def do_OPTIONS(self) -> None:
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Accept, X-Echo-Token, X-Device-Id")
        self.end_headers()

    def do_GET(self) -> None:
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        if path.startswith("/api/cover/"):
            device_id = path[len("/api/cover/"):]
            cover = COVERS.get(device_id)
            if not cover:
                self.send_json({"status": "error", "message": "Cover not found"}, 404)
                return
            self.send_bytes(cover[1], cover[0])
            return

        if path == "/api/health":
            self.send_json({
                "status": "ok",
                "version": "4.0.0",
                "serverTime": int(time.time()),
                "hasAi": False,
                "covers": len(COVERS),
            })
            return

        if path == "/api/state":
            if not self.check_token():
                self.send_json({"status": "error", "message": "Unauthorized"}, 401)
                return
            query = urllib.parse.parse_qs(parsed.query)
            device_id = (query.get("deviceId") or [None])[0] or self.headers.get("X-Device-Id", "")
            self.send_json(build_state(device_id or ""))
            return

        if path == "/api/playlist":
            if not self.check_token():
                self.send_json({"status": "error", "message": "Unauthorized"}, 401)
                return
            query = urllib.parse.parse_qs(parsed.query)
            try:
                limit = max(1, min(60, int((query.get("limit") or ["20"])[0])))
            except ValueError:
                limit = 20
            with DB_LOCK:
                conn = connect()
                try:
                    rows = conn.execute(
                        "SELECT artist, title, album, persona_title, zone_name, played_at"
                        " FROM history ORDER BY id DESC LIMIT ?",
                        (limit,),
                    ).fetchall()
                finally:
                    conn.close()
            tracks = [
                {
                    "artist": row["artist"],
                    "title": row["title"],
                    "album": row["album"] or "",
                    "personaTitle": row["persona_title"],
                    "zoneName": row["zone_name"],
                    "playedAt": row["played_at"],
                }
                for row in rows
            ]
            text = "\n".join(f"{row['artist']} — {row['title']}" for row in rows)
            self.send_json({"status": "ok", "tracks": tracks, "text": text})
            return

        if path in ("/", "/index.html", "/app", "/pwa"):
            self.send_html(load_template("dashboard.html"))
            return

        self.send_json({"status": "error", "message": "Not found"}, 404)

    def do_POST(self) -> None:
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        if path.startswith("/api/") and not self.check_token():
            logger.info("POST %s отклонён: 401 (%s)", path, self.client_address[0])
            self.send_json({"status": "error", "message": "Unauthorized"}, 401)
            return

        data = self.read_json()
        logger.info(
            "POST %s от %s: target=%s sender=%s",
            path,
            self.client_address[0],
            data.get("targetId", ""),
            data.get("senderId", ""),
        )

        if path == "/api/signal":
            device_id = sanitize_text(data.get("id"), 80) or f"dev_{int(time.time())}"
            cover = decode_cover(data.get("coverBase64"))
            ok = upsert_signal(
                device_id=device_id,
                artist=sanitize_text(data.get("artist"), 160) or "Неизвестный исполнитель",
                title=sanitize_text(data.get("title"), 180) or "Без названия",
                source_name=sanitize_text(data.get("sourceName"), 80) or "Android ЭХО",
                zone_name=normalize_zone(data.get("zoneName")),
                album=sanitize_text(data.get("album"), 160),
                cover_bytes=cover,
            )
            if not ok:
                self.send_json({"status": "ignored", "message": "Junk track skipped"})
                return
            self.send_json(build_state(device_id))
            return

        if path == "/api/react":
            target = sanitize_text(data.get("targetId") or data.get("signalId"), 80)
            sender = sanitize_text(data.get("senderId"), 80) or "unknown"
            symbol = sanitize_text(data.get("symbol"), 8) or "✦"
            add_reaction(target, sender, symbol, data.get("message", ""))
            bump_reaction_counter(target, symbol)
            self.send_json(build_state(sender))
            return

        if path == "/api/drop_capsule":
            drop_capsule(data)
            self.send_json(build_state(sanitize_text(data.get("creatorId"), 80)))
            return

        if path == "/api/bump_encounter":
            target = sanitize_text(data.get("targetId"), 80)
            sender = sanitize_text(data.get("senderId"), 80)
            with DB_LOCK:
                conn = connect()
                try:
                    conn.execute(
                        "UPDATE devices SET encounters_count = encounters_count + 1 WHERE id = ?",
                        (target,),
                    )
                    conn.commit()
                finally:
                    conn.close()
            self.send_json(build_state(sender))
            return

        if path == "/api/needle_sync":
            target = sanitize_text(data.get("targetId"), 80)
            sender = sanitize_text(data.get("senderId"), 80) or "anon"
            NEEDLE_SYNCS[sender] = {"targetId": target, "time": time.time()}
            add_reaction(target, sender, "∿", "Кто-то встал рядом и слушает с тобой")
            self.send_json({"status": "ok", "synced": True, "targetId": target})
            return

        if path == "/api/send_warmth":
            target = sanitize_text(data.get("targetId"), 80)
            sender = sanitize_text(data.get("senderId"), 80) or "anon"
            try:
                x = min(1.0, max(0.0, float(data.get("x", 0.5))))
                y = min(1.0, max(0.0, float(data.get("y", 0.5))))
            except (TypeError, ValueError):
                x, y = 0.5, 0.5
            patch_id = f"warmth_{next_event_id()}"
            with REACTIONS_LOCK:
                THERMAL_PATCHES.append({
                    "id": patch_id,
                    "targetId": target,
                    "senderId": sender,
                    "x": x,
                    "y": y,
                    "time": time.time(),
                })
            self.send_json({"status": "ok", "patchId": patch_id})
            return

        if path == "/api/wave_impulse":
            target = sanitize_text(data.get("targetId"), 80)
            sender = sanitize_text(data.get("senderId"), 80) or "anon"
            add_reaction(target, sender, "🌊", "Звуковой импульс сквозь ночной эфир")
            self.send_json({"status": "ok"})
            return

        if path == "/api/delete_signal":
            target = sanitize_text(data.get("targetId") or data.get("id"), 80)
            with DB_LOCK:
                conn = connect()
                try:
                    conn.execute("DELETE FROM devices WHERE id = ?", (target,))
                    conn.commit()
                finally:
                    conn.close()
            COVERS.pop(target, None)
            self.send_json(build_state(""))
            return

        if path == "/api/delete_capsule":
            target = sanitize_text(data.get("targetId") or data.get("id"), 80)
            with DB_LOCK:
                conn = connect()
                try:
                    conn.execute("DELETE FROM capsules WHERE id = ?", (target,))
                    conn.commit()
                finally:
                    conn.close()
            self.send_json({"status": "ok", "deleted": True})
            return

        if path == "/api/save_config":
            self.send_json({"status": "ok"})
            return

        self.send_json({"status": "error", "message": "Not found"}, 404)


class EchoServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def handle_error(self, request, client_address) -> None:
        ex_type, ex_value, _ = sys.exc_info()
        if ex_type in (ConnectionResetError, BrokenPipeError):
            return
        if isinstance(ex_value, OSError) and getattr(ex_value, "winerror", None) == 10054:
            return
        super().handle_error(request, client_address)


def main() -> None:
    init_db()
    ensure_demo_signals()
    start_udp_broadcast()
    start_pc_bridge()
    local_ip = get_local_ip()
    server = EchoServer(("0.0.0.0", PORT), EchoHandler)
    logger.info("=" * 62)
    logger.info("ЭХО · сервер 4.0 запущен")
    logger.info("  пульт:  http://%s:%d/", local_ip, PORT)
    logger.info("  API:    http://%s:%d/api/state", local_ip, PORT)
    logger.info("  токен:  %s", ECHO_TOKEN)
    logger.info("=" * 62)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("Остановка сервера")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
