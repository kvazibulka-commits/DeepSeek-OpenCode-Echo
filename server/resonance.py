"""Резонанс ЭХО 2.0: артист, альбом, жанр + акустика.

Формула (все слагаемые нормированы в 0..1):

    C = 0.36·Artist + 0.20·Album + 0.08·Title + 0.36·Genre
    A = 0.30·Camelot + 0.15·Tempo + 0.30·Energy + 0.15·Mood + 0.10·Weight
    R = 35 + 65·(0.60·C + 0.40·A),  затем clamp(58..99)

Где:
- Artist — сходство имён (нормализация, токены, триграммы, коллаборации);
- Album — сходство альбомов (без изданий и ремастеров);
- Title — сходство названий (ловит каверы и одноимённые треки);
- Genre — близость жанровых семейств;
- Weight — фиксированный 0.84 из канона;
- энергия и настроение выводятся из жанра + детерминированного шума трека.

Смысл: 35 — базовая тишина, 65 — вклад совпадения. Один и тот же трек
даёт 99, тот же артист с тем же жанром — 80-90, только жанр — 58-68,
разные миры — 58.
"""

from __future__ import annotations

import hashlib
import math
import re
import unicodedata
from dataclasses import dataclass

DEFAULT_COLOR = 0xFF38BDF8

GENRES = [
    "post-punk",
    "darkwave",
    "synthwave",
    "ambient",
    "electronic",
    "indie",
    "rock",
    "jazz",
    "soul",
    "lo-fi",
    "classical",
    "trip-hop",
    "metal",
    "folk",
    "nordic folk",
    "dark folk",
    "dream-pop",
    "post-rock",
    "downtempo",
    "hip-hop",
    "pop",
    "funk",
    "disco",
    "industrial",
    "soundtrack",
    "demo",
    "unknown",
]

# Жанровые семьи и мосты. Порядок важен: сначала более специфичные.
SIBLINGS = {
    ("post-punk", "synthwave"),
    ("post-punk", "darkwave"),
    ("ambient", "electronic"),
    ("jazz", "trip-hop"),
    ("indie", "rock"),
    ("lo-fi", "trip-hop"),
    ("classical", "folk"),
    ("metal", "rock"),
    ("electronic", "synthwave"),
    ("post-punk", "indie"),
    ("nordic folk", "dark folk"),
    ("dark folk", "folk"),
    ("nordic folk", "folk"),
    ("dream-pop", "indie"),
    ("post-rock", "ambient"),
    ("downtempo", "trip-hop"),
    ("hip-hop", "electronic"),
    ("pop", "disco"),
    ("funk", "disco"),
    ("soul", "jazz"),
    ("industrial", "electronic"),
    ("soundtrack", "classical"),
    ("synthwave", "pop"),
}

BRIDGES = {
    ("ambient", "classical"),
    ("jazz", "lo-fi"),
    ("rock", "folk"),
    ("post-punk", "electronic"),
    ("metal", "ambient"),
    ("folk", "ambient"),
    ("nordic folk", "metal"),
    ("post-rock", "rock"),
    ("dream-pop", "ambient"),
    ("hip-hop", "funk"),
    ("industrial", "metal"),
    ("soundtrack", "ambient"),
    ("disco", "electronic"),
    ("soul", "pop"),
}

SIBLING_SET = frozenset(frozenset(pair) for pair in SIBLINGS)
BRIDGE_SET = frozenset(frozenset(pair) for pair in BRIDGES)

GENRE_LABELS = {
    "post-punk": "пост-панк",
    "darkwave": "дарквейв",
    "synthwave": "синти-вейв",
    "ambient": "эмбиент",
    "electronic": "электроника",
    "indie": "инди",
    "rock": "рок",
    "jazz": "джаз",
    "soul": "соул",
    "lo-fi": "лоу-фай",
    "classical": "классика",
    "trip-hop": "трип-хоп",
    "metal": "метал",
    "folk": "фолк",
    "nordic folk": "северный фолк",
    "dark folk": "дарк-фолк",
    "dream-pop": "дрим-поп",
    "post-rock": "пост-рок",
    "downtempo": "даунтемпо",
    "hip-hop": "хип-хоп",
    "pop": "поп",
    "funk": "фанк",
    "disco": "диско",
    "industrial": "индастриал",
    "soundtrack": "саундтрек",
    "demo": "демо-маяк",
    "unknown": "жанр не указан",
}


def genre_label(genre: str) -> str:
    key = (genre or "").strip()
    return GENRE_LABELS.get(key, key)

GENRE_KEYWORDS = {
    "post-punk": (
        "post-punk", "post punk", "coldwave", "sovietwave", "molchat", "молчат",
        "кино", "joy division", "the cure", "bauhaus", "sisters of mercy",
        "ploho", "плохо", "буерак", "buerak", "пошлая молли", "sad songs",
        "sad songs", "nürnberg", "motorama", "человек-невидимка",
    ),
    "synthwave": (
        "synthwave", "retrowave", "synth", "синти", "kavinsky", "perturbator",
        "gunship", "timecop",
    ),
    "ambient": (
        "ambient", "эмбиент", "drone", "brian eno", "eno", "stars of the lid",
        "hammock", "ulver", "space",
    ),
    "electronic": (
        "electronic", "techno", "idm", "autechre", "cortini", "aphex", "boards of canada",
        "house", "garage", "burial", "rave",
    ),
    "indie": (
        "indie", "radiohead", "arcade fire", "stereolab", "mitski", "phoebe bridgers",
        "инди",
    ),
    "rock": (
        "rock", "рок", "nirvana", "qotsa", "queens of the stone age", "soviet rock",
        "guitar",
    ),
    "jazz": (
        "jazz", "джаз", "miles davis", "coltrane", "nina simone", "bill evans",
        "chet baker",
    ),
    "lo-fi": (
        "lo-fi", "lofi", "lo fi", "nujabes", "lamp", "лоу-фай", "лоуфай",
    ),
    "classical": (
        "classical", "классика", "bach", "debussy", "ravel", "satie", "piano",
        "орган",
    ),
    "trip-hop": (
        "trip-hop", "trip hop", "trip-hop", "massive attack", "portishead", "tricky",
        "трип-хоп",
    ),
    "metal": (
        "metal", "метал", "opeth", "alcest", "deafheaven", "black metal",
    ),
    "folk": (
        "folk", "фолк", "heilung", "wardruna", "neofolk", "danheim", "skald",
    ),
}

# Базовые акустические центры жанров: BPM, энергия, валентность, лад.
GENRE_BASE = {
    "post-punk": (128, 0.56, 0.36, "A"),
    "synthwave": (110, 0.66, 0.56, "A"),
    "ambient": (78, 0.28, 0.50, "B"),
    "electronic": (124, 0.76, 0.50, "A"),
    "indie": (118, 0.55, 0.60, "B"),
    "rock": (132, 0.72, 0.55, "B"),
    "jazz": (96, 0.45, 0.66, "B"),
    "lo-fi": (84, 0.35, 0.56, "B"),
    "classical": (70, 0.30, 0.62, "B"),
    "trip-hop": (92, 0.44, 0.44, "A"),
    "metal": (140, 0.86, 0.34, "A"),
    "folk": (88, 0.34, 0.52, "B"),
    "darkwave": (120, 0.50, 0.34, "A"),
    "nordic folk": (96, 0.55, 0.45, "B"),
    "dark folk": (90, 0.42, 0.36, "B"),
    "dream-pop": (104, 0.45, 0.62, "B"),
    "post-rock": (112, 0.58, 0.48, "B"),
    "downtempo": (88, 0.38, 0.50, "B"),
    "hip-hop": (92, 0.62, 0.48, "A"),
    "pop": (118, 0.66, 0.70, "B"),
    "funk": (108, 0.72, 0.72, "A"),
    "disco": (120, 0.75, 0.76, "A"),
    "industrial": (128, 0.82, 0.34, "A"),
    "soundtrack": (86, 0.40, 0.50, "B"),
    "soul": (94, 0.52, 0.68, "B"),
    "demo": (100, 0.50, 0.50, "B"),
    "unknown": (110, 0.55, 0.50, "B"),
}


def seed_int(*parts: str) -> int:
    raw = "|".join(part.lower() for part in parts).encode("utf-8")
    return int.from_bytes(hashlib.sha256(raw).digest()[:8], "big")


_LATIN_FOLD = str.maketrans({
    "ð": "d",
    "þ": "th",
    "ø": "o",
    "å": "a",
    "æ": "ae",
    "œ": "oe",
    "ß": "ss",
    "ł": "l",
    "ı": "i",
})


def normalize(text: str) -> str:
    value = unicodedata.normalize("NFKD", str(text or "").casefold())
    value = "".join(char for char in value if not unicodedata.combining(char))
    value = value.translate(_LATIN_FOLD)
    value = re.sub(r"\(.*?\)|\[.*?\]", " ", value)
    value = re.sub(r"\b(feat|ft|featuring|при\s+уч|при\s+участии)\b.*$", " ", value)
    value = re.sub(r"[^\w\s]", " ", value, flags=re.UNICODE)
    return " ".join(value.split())


def _trigrams(text: str) -> set[str]:
    if len(text) <= 3:
        return {text} if text else set()
    return {text[index:index + 3] for index in range(len(text) - 2)}


def _dice(left: str, right: str) -> float:
    a = _trigrams(left)
    b = _trigrams(right)
    if not a or not b:
        return 0.0
    return 2.0 * len(a & b) / (len(a) + len(b))


def _jaccard(left: str, right: str) -> float:
    a = set(left.split())
    b = set(right.split())
    if not a or not b:
        return 0.0
    return len(a & b) / len(a | b)


def text_similarity(left: str, right: str) -> float:
    a = normalize(left)
    b = normalize(right)
    if not a or not b:
        return 0.0
    if a == b:
        return 1.0
    if min(len(a), len(b)) >= 4 and (a in b or b in a):
        return 0.85
    score = max(_jaccard(a, b), _dice(a, b) * 0.92)
    return score if score >= 0.45 else 0.0


def artist_similarity(left: str, right: str) -> float:
    a = normalize(left)
    b = normalize(right)
    if not a or not b:
        return 0.0
    if a == b:
        return 1.0
    separators = r"\s*(?:,|&|/|\band\b|\bx\b|\bvs\b|×|\+)\s*"
    parts_a = [part for part in re.split(separators, a) if part]
    parts_b = [part for part in re.split(separators, b) if part]
    best = max((text_similarity(part_a, part_b) for part_a in parts_a for part_b in parts_b), default=0.0)
    return max(best, text_similarity(a, b) * 0.95)


def album_similarity(left: str, right: str) -> float:
    def clean(value: str) -> str:
        text = normalize(value)
        text = re.sub(r"\b(deluxe|remaster(ed)?|edition|ep|single|version)\b", " ", text)
        return " ".join(text.split())

    if not normalize(left) or not normalize(right):
        return 0.0
    return max(text_similarity(clean(left), clean(right)), 0.0)


def detect_genre(artist: str, title: str, album: str = "", mood: str = "") -> str:
    probe = normalize(f"{artist} {title} {album} {mood}")
    best_genre = ""
    best_hits = 0
    for genre, keywords in GENRE_KEYWORDS.items():
        hits = sum(1 for keyword in keywords if keyword in probe)
        if hits > best_hits:
            best_hits = hits
            best_genre = genre
    if best_genre:
        return best_genre
    seed = seed_int(artist, title, album or "unknown")
    return GENRES[seed % len(GENRES)]


def genre_affinity(left: str, right: str) -> float:
    if not left or not right:
        return 0.0
    if left == "unknown" and right == "unknown":
        return 0.55
    if left == right:
        return 1.0
    pair = frozenset((left, right))
    if pair in SIBLING_SET:
        return 0.72
    if pair in BRIDGE_SET:
        return 0.50
    return 0.15


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
    if a[0] == b[0]:
        return 0.72
    if ring == 1:
        return 0.58
    return 0.32


def tempo_score(first: int, second: int) -> float:
    if first <= 0 or second <= 0:
        return 0.5
    ratio = second / first
    while ratio < 0.8:
        ratio *= 2
    while ratio > 1.25:
        ratio /= 2
    diff = abs(math.log2(ratio))
    return max(0.0, min(1.0, 1.0 - diff / 0.28))


@dataclass(frozen=True)
class Acoustics:
    bpm: int
    camelot: str
    genre: str
    energy: float
    valence: float
    artist: str = ""
    title: str = ""
    album: str = ""


def infer_acoustics(
    artist: str,
    title: str,
    album: str = "",
    mood: str = "",
    genre: str | None = None,
) -> Acoustics:
    resolved = (genre or "").strip()
    if not resolved or resolved not in GENRE_BASE:
        resolved = detect_genre(artist, title, album, mood)
    genre = resolved
    base_bpm, base_energy, base_valence, base_key = GENRE_BASE.get(genre, (110, 0.55, 0.5, "B"))
    seed = seed_int(artist, title, album or mood or genre)

    bpm = max(55, min(170, base_bpm + (seed % 25) - 12))
    key_number = 1 + (seed >> 10) % 12
    key_letter = base_key if (seed >> 16) % 4 != 0 else ("B" if base_key == "A" else "A")
    energy = max(0.10, min(0.98, base_energy + ((seed >> 20) % 17 - 8) / 100.0))
    valence = max(0.10, min(0.98, base_valence + ((seed >> 24) % 21 - 10) / 100.0))

    return Acoustics(
        bpm=bpm,
        camelot=f"{key_number}{key_letter}",
        genre=genre,
        energy=round(energy, 2),
        valence=round(valence, 2),
        artist=artist,
        title=title,
        album=album,
    )


def resonance(mine: Acoustics, other: Acoustics) -> tuple[int, str]:
    artist = artist_similarity(mine.artist, other.artist)
    album = album_similarity(mine.album, other.album)
    title = text_similarity(mine.title, other.title)
    genre = genre_affinity(mine.genre, other.genre)

    camelot = camelot_score(mine.camelot, other.camelot)
    tempo = tempo_score(mine.bpm, other.bpm)
    energy = 1.0 - abs(mine.energy - other.energy)
    mood = 1.0 - abs(mine.valence - other.valence)
    weight = 0.84

    cultural = 0.36 * artist + 0.20 * album + 0.08 * title + 0.36 * genre
    acoustic = 0.30 * camelot + 0.15 * tempo + 0.30 * energy + 0.15 * mood + 0.10 * weight
    percent = round(35 + 65 * (0.60 * cultural + 0.40 * acoustic))
    percent = max(58, min(99, percent))

    same_track = (
        normalize(mine.artist) == normalize(other.artist)
        and normalize(mine.title) == normalize(other.title)
    )
    if same_track:
        percent = max(percent, 96)

    parts: list[str] = []
    if same_track:
        parts.append("Один и тот же трек — полное совпадение.")
    else:
        if artist >= 0.9:
            parts.append(f"Тот же артист: {mine.artist}.")
        elif album >= 0.9 and mine.album:
            parts.append(f"Один альбом: {mine.album}.")
        elif title >= 0.9:
            parts.append(f"Похожее название: «{mine.title}».")
        if genre >= 0.9:
            parts.append(f"Один жанр: {genre_label(mine.genre)}.")
        elif genre >= 0.5 and mine.genre and other.genre:
            parts.append(
                f"Соседние жанры: {genre_label(mine.genre)} и {genre_label(other.genre)}."
            )
        if camelot >= 0.8:
            parts.append("Совпали тональности Camelot.")
        if tempo >= 0.8:
            parts.append("Темп совпал в пределах октавы.")
        if not parts:
            parts.append("Тёплый вечерний резонанс: траектории пересеклись в этой точке города.")

    return percent, " ".join(parts[:2])


def distance_label(meters: int) -> str:
    if meters < 50:
        return f"~{meters} м · рядом"
    if meters < 150:
        return f"~{meters} м"
    return f"~{meters} м · размыто"
