"""Резонанс ЭХО: чистые функции без БД и сети.

Формула из канона:
    B = 0.15·K + 0.10·T + 0.25·W + 0.15·M + 0.35·E
    R = 100 · [B + 0.25·(1 − B)·S]
где K — Camelot, T — темп, W — фиксированный вес 0.84, M — валентность,
E — энергия, S — культурный мост. Итог ограничен 58–99.
"""

from __future__ import annotations

import hashlib
import math
from dataclasses import dataclass

GENRES = [
    "post-punk",
    "synthwave",
    "ambient",
    "electronic",
    "indie",
    "rock",
    "jazz",
    "lo-fi",
    "classical",
    "trip-hop",
]

# Жанровые мосты: пары, которые звучат вместе неожиданно, но убедительно.
BRIDGES = [
    ("post-punk", "electronic"),
    ("post-punk", "jazz"),
    ("synthwave", "ambient"),
    ("ambient", "classical"),
    ("indie", "lo-fi"),
    ("trip-hop", "jazz"),
    ("rock", "classical"),
]


def seed_int(*parts: str) -> int:
    raw = "|".join(parts).lower().encode("utf-8")
    return int.from_bytes(hashlib.sha256(raw).digest()[:8], "big")


@dataclass(frozen=True)
class Acoustics:
    bpm: int
    camelot: str
    genre: str
    energy: float
    valence: float


def infer_acoustics(artist: str, title: str, album: str = "", mood: str = "") -> Acoustics:
    probe = f"{artist} {title} {album} {mood}".lower()
    seed = seed_int(artist, title, album)

    genre = GENRES[seed % len(GENRES)]
    for candidate in GENRES:
        if candidate in probe:
            genre = candidate
            break
    if "пост" in probe or "post" in probe:
        genre = "post-punk"
    if "эмбиент" in probe or "ambient" in probe or "drone" in probe:
        genre = "ambient"

    bpm = 68 + (seed >> 7) % 96
    key_number = 1 + (seed >> 13) % 12
    key_letter = "A" if (seed >> 17) % 2 == 0 else "B"

    energy = 0.35 + ((seed >> 19) % 60) / 100.0
    valence = 0.30 + ((seed >> 23) % 65) / 100.0
    return Acoustics(
        bpm=bpm,
        camelot=f"{key_number}{key_letter}",
        genre=genre,
        energy=round(min(0.98, energy), 2),
        valence=round(min(0.98, valence), 2),
    )


def parse_camelot(value: str) -> tuple[int, str] | None:
    text = (value or "").strip().upper()
    if len(text) < 2 or text[-1] not in ("A", "B"):
        return None
    try:
        number = int(text[:-1])
    except ValueError:
        return None
    if not 1 <= number <= 12:
        return None
    return number, text[-1]


def camelot_score(first: str, second: str) -> float:
    a = parse_camelot(first)
    b = parse_camelot(second)
    if not a or not b:
        return 0.5
    ring = abs(a[0] - b[0])
    ring = min(ring, 12 - ring)
    if a[1] == b[1]:
        if ring == 0:
            return 1.0
        if ring == 1:
            return 0.86
        if ring == 2:
            return 0.62
        return 0.30
    # Относительные тональности: та же цифра, другая буква.
    if a[0] == b[0]:
        return 0.72
    if ring == 1:
        return 0.58
    return 0.32


def tempo_score(first: int, second: int) -> float:
    if first <= 0 or second <= 0:
        return 0.5
    ratio = second / first
    # Октавы считаем одним и тем же темпом.
    while ratio < 0.8:
        ratio *= 2
    while ratio > 1.25:
        ratio /= 2
    diff = abs(math.log2(ratio))
    return max(0.0, min(1.0, 1.0 - diff / 0.28))


def cultural_score(first: str, second: str) -> float:
    if first == second:
        return 0.95
    pair = tuple(sorted((first, second)))
    if pair in {tuple(sorted(item)) for item in BRIDGES}:
        return 0.62
    return 0.32


def resonance(mine: Acoustics, other: Acoustics) -> tuple[int, str]:
    k = camelot_score(mine.camelot, other.camelot)
    t = tempo_score(mine.bpm, other.bpm)
    w = 0.84
    m = 1.0 - abs(mine.valence - other.valence)
    e = 1.0 - abs(mine.energy - other.energy)
    s = cultural_score(mine.genre, other.genre)

    base = 0.15 * k + 0.10 * t + 0.25 * w + 0.15 * m + 0.35 * e
    percent = round(100 * (base + 0.25 * (1 - base) * s))
    percent = max(58, min(99, percent))

    if k >= 0.85 and t >= 0.8:
        story = "Редкое созвучие Camelot и фазовый синхрон темпа."
    elif s >= 0.6:
        story = f"Культурный мост: {mine.genre} встречается с {other.genre}."
    elif e >= 0.8:
        story = "Совпадает плотность звука: обе волны дышат в одном темпе."
    else:
        story = "Тёплый вечерний резонанс: ваши звуковые траектории пересеклись в этой точке города."

    return percent, story


def distance_label(meters: int) -> str:
    if meters < 50:
        return f"~{meters} м · рядом"
    if meters < 150:
        return f"~{meters} м"
    return f"~{meters} м · размыто"
