"""Жанр ЭХО: сначала плеер, потом каталог, в конце — догадка.

Порядок доверия:

1. ``player`` — жанр, который прислал сам плеер (Яндекс Музыка, ВК, SMTC Windows).
2. ``yandex`` — жанр из API Яндекс Музыки, если задан ``ECHO_YANDEX_TOKEN``.
3. ``catalog`` — каталог артистов и альбомов внутри ЭХО.
4. ``guess`` — эвристика по словам и хешу (последний вариант).

Наружу отдаётся канонический жанр ЭХО и его русская подпись.
"""

from __future__ import annotations

import json
import os
import urllib.parse
import urllib.request
from dataclasses import dataclass

try:
    from .resonance import GENRES, GENRE_LABELS, genre_label, normalize
except ImportError:
    from resonance import GENRES, GENRE_LABELS, genre_label, normalize

# ── Подписи ──────────────────────────────────────────────────────────────────

ORIGIN_LABELS = {
    "player": "жанр из плеера",
    "yandex": "жанр из Яндекс Музыки",
    "itunes": "жанр из iTunes",
    "catalog": "жанр по каталогу ЭХО",
    "guess": "жанр — предположение",
    "demo": "демо-маяк",
}

# ── Словари жанров ───────────────────────────────────────────────────────────
# Порядок важен: специфичные жанры идут раньше общих.

GENRE_KEYS: tuple[tuple[str, str], ...] = (
    ("пост-панк", "post-punk"),
    ("пост панк", "post-punk"),
    ("post-punk", "post-punk"),
    ("post punk", "post-punk"),
    ("coldwave", "post-punk"),
    ("cold wave", "post-punk"),
    ("sovietwave", "post-punk"),
    ("дарквейв", "darkwave"),
    ("darkwave", "darkwave"),
    ("dark wave", "darkwave"),
    ("готик", "darkwave"),
    ("gothic", "darkwave"),
    ("северный фолк", "nordic folk"),
    ("nordic folk", "nordic folk"),
    ("viking", "nordic folk"),
    ("викинг", "nordic folk"),
    ("norse", "nordic folk"),
    ("neofolk", "dark folk"),
    ("неофолк", "dark folk"),
    ("dark folk", "dark folk"),
    ("дарк-фолк", "dark folk"),
    ("дарк фолк", "dark folk"),
    ("фолк", "folk"),
    ("folk", "folk"),
    ("народн", "folk"),
    ("этнич", "folk"),
    ("ethnic", "folk"),
    ("country", "folk"),
    ("кантри", "folk"),
    ("диско", "disco"),
    ("disco", "disco"),
    ("фанк", "funk"),
    ("funk", "funk"),
    ("соул", "soul"),
    ("soul", "soul"),
    ("хип-хоп", "hip-hop"),
    ("хип хоп", "hip-hop"),
    ("hip-hop", "hip-hop"),
    ("hip hop", "hip-hop"),
    ("рэп", "hip-hop"),
    ("rap", "hip-hop"),
    ("дрим-поп", "dream-pop"),
    ("дрим поп", "dream-pop"),
    ("dream pop", "dream-pop"),
    ("dream-pop", "dream-pop"),
    ("пост-рок", "post-rock"),
    ("пост рок", "post-rock"),
    ("post-rock", "post-rock"),
    ("post rock", "post-rock"),
    ("трип-хоп", "trip-hop"),
    ("трип хоп", "trip-hop"),
    ("trip-hop", "trip-hop"),
    ("trip hop", "trip-hop"),
    ("лоу-фай", "lo-fi"),
    ("лоуфай", "lo-fi"),
    ("lo-fi", "lo-fi"),
    ("lofi", "lo-fi"),
    ("lo fi", "lo-fi"),
    ("даунтемпо", "downtempo"),
    ("downtempo", "downtempo"),
    ("индастриал", "industrial"),
    ("industrial", "industrial"),
    ("синти", "synthwave"),
    ("synthwave", "synthwave"),
    ("retrowave", "synthwave"),
    ("synthpop", "synthwave"),
    ("synth-pop", "synthwave"),
    ("синт", "synthwave"),
    ("саундтрек", "soundtrack"),
    ("soundtrack", "soundtrack"),
    ("score", "soundtrack"),
    ("классик", "classical"),
    ("classical", "classical"),
    ("оркестр", "classical"),
    ("orchestra", "classical"),
    ("эмбиент", "ambient"),
    ("ambient", "ambient"),
    ("drone", "ambient"),
    ("джаз", "jazz"),
    ("jazz", "jazz"),
    ("блюз", "jazz"),
    ("blues", "jazz"),
    ("метал", "metal"),
    ("metal", "metal"),
    ("панк", "rock"),
    ("punk", "rock"),
    ("рок", "rock"),
    ("rock", "rock"),
    ("инди", "indie"),
    ("indie", "indie"),
    ("электрон", "electronic"),
    ("electronic", "electronic"),
    ("techno", "electronic"),
    ("техно", "electronic"),
    ("idm", "electronic"),
    ("house", "electronic"),
    ("хаус", "electronic"),
    ("поп", "pop"),
    ("pop", "pop"),
)

# ── Каталог артистов ─────────────────────────────────────────────────────────
# Ключ — нормализованное имя (или устойчивая часть имени).

ARTIST_GENRES: dict[str, str] = {
    # русский пост-панк и инди
    "молчат дома": "post-punk",
    "molchat doma": "post-punk",
    "кино": "post-punk",
    "kino": "post-punk",
    "плохо": "post-punk",
    "ploho": "post-punk",
    "буерак": "post-punk",
    "buerak": "post-punk",
    "motorama": "post-punk",
    "моторама": "post-punk",
    "nurnberg": "post-punk",
    "нюрнберг": "post-punk",
    "sad songs": "post-punk",
    "пошлая молли": "post-punk",
    "человек-невидимка": "post-punk",
    "человек невидимка": "post-punk",
    "sirotkin": "indie",
    "сироткин": "indie",
    "земфира": "indie",
    "zemfira": "indie",
    "мумий тролль": "rock",
    "сплин": "rock",
    "splean": "rock",
    "би-2": "rock",
    "наутилус": "rock",
    "алиса": "rock",
    "ария": "metal",
    # мировая классика жанров
    "joy division": "post-punk",
    "the cure": "post-punk",
    "bauhaus": "post-punk",
    "sisters of mercy": "post-punk",
    "interpol": "post-punk",
    "editors": "post-punk",
    "the national": "indie",
    "radiohead": "indie",
    "arcade fire": "indie",
    "mitski": "indie",
    "phoebe bridgers": "indie",
    "stereolab": "indie",
    "burial": "electronic",
    "aphex twin": "electronic",
    "boards of canada": "electronic",
    "autechre": "electronic",
    "four tet": "electronic",
    "jon hopkins": "electronic",
    "cortini": "electronic",
    "brian eno": "ambient",
    "stars of the lid": "ambient",
    "hammock": "ambient",
    "ulver": "dark folk",
    "danheim": "nordic folk",
    "wardruna": "nordic folk",
    "heilung": "nordic folk",
    "skald": "nordic folk",
    "скальд": "nordic folk",
    "myrkur": "nordic folk",
    "eivor": "nordic folk",
    "eivør": "nordic folk",
    "nytt land": "nordic folk",
    "runahild": "nordic folk",
    "munknörr": "nordic folk",
    "munknorr": "nordic folk",
    "ray wylie hubbard": "folk",
    "forndom": "dark folk",
    "perturbator": "synthwave",
    "kavinsky": "synthwave",
    "gunship": "synthwave",
    "timecop1983": "synthwave",
    "the midnight": "synthwave",
    "massive attack": "trip-hop",
    "portishead": "trip-hop",
    "tricky": "trip-hop",
    "nujabes": "lo-fi",
    "jinsang": "lo-fi",
    "lamp": "lo-fi",
    "miles davis": "jazz",
    "coltrane": "jazz",
    "bill evans": "jazz",
    "chet baker": "jazz",
    "nina simone": "soul",
    "bach": "classical",
    "debussy": "classical",
    "ravel": "classical",
    "satie": "classical",
    "opeth": "metal",
    "alcest": "metal",
    "deafheaven": "metal",
    "nirvana": "rock",
    "queens of the stone age": "rock",
    "qotsa": "rock",
    "daft punk": "electronic",
    "gorillaz": "electronic",
    "bjork": "electronic",
    "björk": "electronic",
    "бьорк": "electronic",
    "радиохед": "indie",
    "портисхед": "trip-hop",
    "депеш мод": "synthwave",
    "depeche mode": "synthwave",
    "рамштайн": "industrial",
    "rammstein": "industrial",
    "nine inch nails": "industrial",
    "кис-кис": "rock",
    "kis-kis": "rock",
    "ленинград": "rock",
    "leningrad": "rock",
    "coldplay": "pop",
    "колдплей": "pop",
    "muse": "rock",
    "мьюз": "rock",
    "дафт панк": "electronic",
}

# Слова в названии или альбоме, которые сразу выдают жанр.
TEXT_GENRES: tuple[tuple[str, str], ...] = (
    ("драккар", "nordic folk"),
    ("drakkar", "nordic folk"),
    ("викинг", "nordic folk"),
    ("viking", "nordic folk"),
    ("валгалл", "nordic folk"),
    ("valhalla", "nordic folk"),
    ("скальд", "nordic folk"),
    ("skald", "nordic folk"),
    ("фьорд", "nordic folk"),
    ("fjord", "nordic folk"),
    ("руны", "nordic folk"),
    ("rune", "nordic folk"),
    ("сага", "nordic folk"),
    ("saga", "nordic folk"),
    ("nordr", "nordic folk"),
    ("nord", "nordic folk"),
    ("симфони", "classical"),
    ("symphony", "classical"),
    ("соната", "classical"),
    ("sonata", "classical"),
    ("ноктюрн", "classical"),
    ("nocturne", "classical"),
    ("реквием", "classical"),
    ("requiem", "classical"),
    ("колыбельн", "ambient"),
    ("lullaby", "ambient"),
    ("вальс", "classical"),
    ("waltz", "classical"),
)


@dataclass(frozen=True)
class GenreInfo:
    genre: str
    label: str
    origin: str

    @property
    def origin_label(self) -> str:
        return ORIGIN_LABELS.get(self.origin, ORIGIN_LABELS["guess"])


def genre_label(genre: str) -> str:
    """Русская подпись жанра (общая с ядром резонанса)."""
    key = (genre or "").strip()
    return GENRE_LABELS.get(key, key)


def origin_label(origin: str) -> str:
    return ORIGIN_LABELS.get((origin or "").strip(), ORIGIN_LABELS["guess"])


def match_genre(text: str) -> str:
    """Ищет канонический жанр в произвольной строке плеера."""
    probe = normalize(text)
    if not probe:
        return ""
    for keyword, genre in GENRE_KEYS:
        if normalize(keyword) in probe:
            return genre
    return ""


def _artist_genre(artist: str) -> str:
    probe = normalize(artist)
    if not probe:
        return ""
    best = ""
    best_len = 0
    for key, genre in ARTIST_GENRES.items():
        needle = normalize(key)
        if needle and needle in probe and len(needle) > best_len:
            best = genre
            best_len = len(needle)
    return best


def _text_genre(title: str, album: str) -> str:
    probe = normalize(f"{title} {album}")
    if not probe:
        return ""
    for keyword, genre in TEXT_GENRES:
        if normalize(keyword) in probe:
            return genre
    return ""


def _catalog_genre(artist: str, title: str, album: str) -> str:
    return _artist_genre(artist) or _text_genre(title, album)


# ── Яндекс Музыка (необязательно) ────────────────────────────────────────────

_YANDEX_CACHE: dict[str, str] = {}
_YANDEX_FAILED = False


def _yandex_online(artist: str, title: str) -> str:
    """Жанр из API Яндекс Музыки. Работает только при ECHO_YANDEX_TOKEN."""
    global _YANDEX_FAILED
    if _YANDEX_FAILED:
        return ""
    token = os.environ.get("ECHO_YANDEX_TOKEN", "").strip()
    if not token or not artist or not title:
        return ""

    key = f"{normalize(artist)}|{normalize(title)}"
    if key in _YANDEX_CACHE:
        return _YANDEX_CACHE[key]

    query = urllib.parse.urlencode({"text": f"{artist} {title}", "type": "track", "page": "0"})
    request = urllib.request.Request(
        f"https://api.music.yandex.net/search?{query}",
        headers={"Authorization": f"OAuth {token}", "User-Agent": "EchoRadar/4.0"},
    )
    try:
        with urllib.request.urlopen(request, timeout=4) as response:
            data = json.loads(response.read().decode("utf-8", "replace"))
    except Exception:
        _YANDEX_FAILED = True
        return ""

    result = (data.get("result") or {}).get("tracks") or {}
    for track in (result.get("results") or [])[:5]:
        names = " ".join(
            normalize((item or {}).get("name", "")) for item in (track.get("artists") or [])
        )
        if normalize(artist).split()[0] not in names and names not in normalize(artist):
            continue
        for album in track.get("albums") or []:
            raw = (album.get("genre") or "").strip()
            if not raw:
                continue
            genre = match_genre(raw) or (raw if raw in GENRES else "")
            if genre:
                _YANDEX_CACHE[key] = genre
                return genre
    return ""


# ── Публичная функция ────────────────────────────────────────────────────────

def resolve_genre(
    artist: str,
    title: str,
    album: str = "",
    source_genre: str = "",
    online: bool = True,
) -> GenreInfo:
    """Определяет жанр: плеер → Яндекс → каталог → догадка."""
    raw = (source_genre or "").strip()
    if raw:
        matched = match_genre(raw)
        if matched:
            return GenreInfo(matched, genre_label(matched), "player")
        # Незнакомый ярлык плеера: подпись берём от него, акустику — из каталога.
        fallback = _catalog_genre(artist, title, album) or "unknown"
        return GenreInfo(fallback, raw.lower(), "player")

    catalog = _catalog_genre(artist, title, album)
    if catalog:
        return GenreInfo(catalog, genre_label(catalog), "catalog")

    if online:
        online_genre = _yandex_online(artist, title)
        if online_genre:
            return GenreInfo(online_genre, genre_label(online_genre), "yandex")

    # Ни плеер, ни каталог, ни Яндекс не дали жанр: не выдумываем его.
    return GenreInfo("unknown", genre_label("unknown"), "guess")


def lookup_source_label(source_name: str) -> str:
    """Человеческая подпись источника: Яндекс Музыка, ВК, Spotify и т. д."""
    probe = (source_name or "").lower()
    if "yandex" in probe or "яндекс" in probe:
        return "Яндекс Музыка"
    if "vk" in probe or "вк" in probe:
        return "ВК Музыка"
    if "spotify" in probe:
        return "Spotify"
    if "apple" in probe:
        return "Apple Music"
    if "pc" in probe or "пк" in probe:
        return "ПК"
    return "плеер"


__all__ = [
    "GENRE_LABELS",
    "GENRE_KEYS",
    "GenreInfo",
    "genre_label",
    "lookup_source_label",
    "match_genre",
    "origin_label",
    "resolve_genre",
]
