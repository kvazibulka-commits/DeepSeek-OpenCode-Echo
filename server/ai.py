"""GigaChat: необязательный генератор архетипов для ЭХО.

Работает только если заданы ключи:

    ECHO_GIGACHAT_CLIENT_ID
    ECHO_GIGACHAT_CLIENT_SECRET

Остальное настраивается по желанию:

    ECHO_GIGACHAT_SCOPE=GIGACHAT_API_PERS   (физлица; для юрлиц — GIGACHAT_API_B2B)
    ECHO_GIGACHAT_MODEL=GigaChat            (GigaChat / GigaChat-Pro / GigaChat-Max)
    ECHO_GIGACHAT_CA=C:\\certs\\russian_trusted_root_ca.pem
    ECHO_GIGACHAT_INSECURE=1                (не проверять TLS; только для локальной сети)

Сеть вызывается в фоне, чтобы не задерживать приём сигналов. Любая ошибка
возвращает None — тогда остаётся архетип из правил.
"""

from __future__ import annotations

import base64
import json
import logging
import os
import re
import ssl
import threading
import time
import urllib.parse
import urllib.request
import uuid

try:
    from .resonance import normalize
except ImportError:
    from resonance import normalize

logger = logging.getLogger("echo.ai")

OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
DEFAULT_SCOPE = "GIGACHAT_API_PERS"
DEFAULT_MODEL = "GigaChat"

TOKEN_TTL_FALLBACK = 25 * 60
REQUEST_TIMEOUT = 8.0
MAX_ARCHETYPE_LEN = 48
MAX_OBSERVATION_LEN = 140

SYSTEM_PROMPT = """Ты — голос ночного музыкального сонара «ЭХО». Ты даёшь треку архетип — короткое имя образа.
Правила:
- архетип: 2–4 слова, роль в именительном падеже и образ в родительном: «Дозорный фьорда», «Ловец ночных фонарей», «Певец сводов собора»;
- опирайся на смысл названия, альбома, артиста и жанра; не придумывай жанр, если он не указан;
- без кавычек, эмодзи, цифр, имён артистов и названий треков;
- наблюдение: одно предложение до 90 символов, атмосферное, без «этот трек» и без оценок;
- отвечай строго JSON без пояснений и без markdown.

Примеры:
Трек: Vanir / Munknörr / Norðr, жанр северный фолк
{"archetype": "Дозорный фьорда", "observation": "Слышит в гуле вёсел тот же ритм, что и в любимом треке."}

Трек: Судно / Молчат Дома / Этажи, жанр пост-панк
{"archetype": "Ловец ночных фонарей", "observation": "Знает, как звучит пустой двор после полуночи."}
"""

USER_TEMPLATE = """Артист: {artist}
Трек: {title}
Альбом: {album}
Жанр: {genre}
Верни JSON: {{"archetype": "...", "observation": "..."}}"""

_token_lock = threading.Lock()
_token_cache: dict[str, float | str] = {"token": "", "expires_at": 0.0}


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def client_id() -> str:
    return _env("ECHO_GIGACHAT_CLIENT_ID")


def client_secret() -> str:
    return _env("ECHO_GIGACHAT_CLIENT_SECRET")


def scope() -> str:
    return _env("ECHO_GIGACHAT_SCOPE", DEFAULT_SCOPE) or DEFAULT_SCOPE


def model_name() -> str:
    return _env("ECHO_GIGACHAT_MODEL", DEFAULT_MODEL) or DEFAULT_MODEL


def available() -> bool:
    return bool(client_id() and client_secret())


def cache_key(artist: str, title: str, album: str = "") -> str:
    return f"{normalize(artist)}|{normalize(title)}|{normalize(album)}"


def _ssl_context() -> ssl.SSLContext:
    ca_file = _env("ECHO_GIGACHAT_CA")
    if ca_file:
        return ssl.create_default_context(cafile=ca_file)
    if _env("ECHO_GIGACHAT_STRICT") == "1":
        return ssl.create_default_context()
    # По умолчанию на Windows российских корневых сертификатов нет.
    logger.debug("GigaChat: TLS без проверки сертификата (нужен ECHO_GIGACHAT_CA)")
    return ssl._create_unverified_context()


def _post(url: str, data: bytes, headers: dict[str, str]) -> dict:
    request = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT, context=_ssl_context()) as response:
        raw = response.read().decode("utf-8", "replace")
    return json.loads(raw)


def _access_token(force: bool = False) -> str:
    """OAuth-токен GigaChat с кэшем до истечения."""
    with _token_lock:
        now = time.time()
        cached = str(_token_cache.get("token") or "")
        expires_at = float(_token_cache.get("expires_at") or 0.0)
        if cached and not force and now < expires_at:
            return cached

        pair = f"{client_id()}:{client_secret()}".encode("utf-8")
        headers = {
            "Authorization": "Basic " + base64.b64encode(pair).decode("ascii"),
            "RqUID": str(uuid.uuid4()),
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json",
        }
        payload = urllib.parse.urlencode({"scope": scope()}).encode("utf-8")
        data = _post(OAUTH_URL, payload, headers)
        token = str(data.get("access_token") or "")
        if not token:
            raise RuntimeError("GigaChat: пустой access_token")

        expires_at_ms = float(data.get("expires_at") or 0)
        if expires_at_ms > 1e12:
            expires_at = expires_at_ms / 1000.0
        elif expires_at_ms > 1e9:
            expires_at = expires_at_ms
        else:
            expires_at = now + TOKEN_TTL_FALLBACK

        _token_cache["token"] = token
        _token_cache["expires_at"] = max(now + 30.0, expires_at - 30.0)
        return token


def _extract_json(text: str) -> dict | None:
    if not text:
        return None
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?|```$", "", cleaned, flags=re.MULTILINE).strip()
    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        return None
    try:
        value = json.loads(match.group(0))
    except (ValueError, TypeError):
        return None
    return value if isinstance(value, dict) else None


def _clean(text: str, limit: int) -> str:
    value = " ".join(str(text or "").split())
    value = value.strip(" \t\n\r«»\"'`.,;:—-")
    if not value:
        return ""
    return value[:limit].strip()


def _valid_archetype(text: str) -> bool:
    if not text or len(text) > MAX_ARCHETYPE_LEN:
        return False
    if re.search(r"[0-9!?@#$%^&*_=+\[\]{}<>|/\\;:«»\"]", text):
        return False
    words = [word for word in text.split() if word]
    return 2 <= len(words) <= 5


def generate(
    artist: str,
    title: str,
    album: str = "",
    genre_label: str = "",
) -> dict | None:
    """Просит GigaChat архетип. Возвращает dict или None."""
    if not available():
        return None

    user_prompt = USER_TEMPLATE.format(
        artist=artist or "неизвестен",
        title=title or "без названия",
        album=album or "не указан",
        genre=genre_label or "не указан",
    )
    body = {
        "model": model_name(),
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        "temperature": 0.8,
        "top_p": 0.9,
        "max_tokens": 160,
        "stream": False,
    }

    for attempt in range(2):
        try:
            token = _access_token(force=attempt > 0)
            headers = {
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            }
            data = _post(API_URL, json.dumps(body).encode("utf-8"), headers)
            content = (
                data.get("choices", [{}])[0].get("message", {}).get("content", "")
            )
            parsed = _extract_json(content)
            if not parsed:
                return None
            archetype = _clean(parsed.get("archetype"), MAX_ARCHETYPE_LEN)
            observation = _clean(parsed.get("observation"), MAX_OBSERVATION_LEN)
            if not _valid_archetype(archetype) or not observation:
                return None
            return {
                "archetype": archetype,
                "observation": observation,
                "model": model_name(),
            }
        except Exception as exc:  # сеть, TLS, лимиты, разбор
            logger.debug("GigaChat попытка %s: %s", attempt + 1, exc)
            time.sleep(0.6)
    return None


def reset_token() -> None:
    """Сбрасывает кэш токена (для тестов и после 401)."""
    with _token_lock:
        _token_cache["token"] = ""
        _token_cache["expires_at"] = 0.0


__all__ = [
    "available",
    "cache_key",
    "generate",
    "model_name",
    "reset_token",
    "scope",
]
