"""Жанр из бесплатных баз, доступных в РФ: iTunes Search API.

Ключ не нужен, аккаунт не нужен. Один запрос на трек, результат кэшируется
в памяти и в SQLite (таблица `track_genre`).

Порядок в ЭХО: плеер → каталог → iTunes → «жанр не указан».
"""

from __future__ import annotations

import json
import logging
import threading
import urllib.parse
import urllib.request

try:
    from .genres import GenreInfo, genre_label, match_genre
    from .resonance import normalize, text_similarity
except ImportError:
    from genres import GenreInfo, genre_label, match_genre
    from resonance import normalize, text_similarity

logger = logging.getLogger("echo.musicdb")

ITUNES_URL = "https://itunes.apple.com/search"
USER_AGENT = "EchoRadar/4.0 (music sonar; contact: local)"
REQUEST_TIMEOUT = 5.0

# Жанры Apple → канон ЭХО.
ITUNES_GENRES = {
    "electronic": "electronic",
    "alternative": "indie",
    "rock": "rock",
    "pop": "pop",
    "hip-hop/rap": "hip-hop",
    "r&b/soul": "soul",
    "jazz": "jazz",
    "classical": "classical",
    "folk": "folk",
    "world": "folk",
    "metal": "metal",
    "dance": "electronic",
    "country": "folk",
    "singer/songwriter": "folk",
    "soundtrack": "soundtrack",
    "blues": "jazz",
    "new age": "ambient",
    "vocal": "pop",
    "easy listening": "downtempo",
    "spiritual": "ambient",
}

_cache_lock = threading.Lock()
_cache: dict[str, GenreInfo | None] = {}


def _same_artist(left: str, right: str) -> bool:
    a = normalize(left)
    b = normalize(right)
    if not a or not b:
        return False
    if a == b or a in b or b in a:
        return True
    return text_similarity(a, b) >= 0.6


def _same_title(left: str, right: str) -> bool:
    a = normalize(left)
    b = normalize(right)
    if not a or not b:
        return False
    if a == b:
        return True
    return text_similarity(a, b) >= 0.72


def _itunes(artist: str, title: str) -> GenreInfo | None:
    query = urllib.parse.urlencode(
        {
            "term": f"{artist} {title}".strip(),
            "entity": "song",
            "media": "music",
            "limit": "6",
        }
    )
    request = urllib.request.Request(
        f"{ITUNES_URL}?{query}",
        headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT) as response:
            payload = json.loads(response.read().decode("utf-8", "replace"))
    except Exception as exc:
        logger.debug("iTunes недоступен: %s", exc)
        return None

    for item in payload.get("results") or []:
        if not isinstance(item, dict):
            continue
        if not _same_artist(str(item.get("artistName") or ""), artist):
            continue
        if not _same_title(str(item.get("trackName") or ""), title):
            continue
        raw = str(item.get("primaryGenreName") or "").strip()
        if not raw:
            continue
        canonical = ITUNES_GENRES.get(raw.lower()) or match_genre(raw)
        if not canonical:
            continue
        return GenreInfo(canonical, genre_label(canonical), "itunes")
    return None


def lookup_genre(artist: str, title: str) -> GenreInfo | None:
    """Жанр из iTunes с кэшем в памяти. None — если база не знает трек."""
    if not artist.strip() or not title.strip():
        return None
    key = f"{normalize(artist)}|{normalize(title)}"
    with _cache_lock:
        if key in _cache:
            return _cache[key]
    found = _itunes(artist, title)
    with _cache_lock:
        _cache[key] = found
        if len(_cache) > 4000:
            _cache.clear()
    if found:
        logger.info("iTunes: %s — %s → %s", artist, title, found.label)
    return found
