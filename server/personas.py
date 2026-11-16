"""Совместимость: архетип трека живёт в ``archetypes.py``.

``persona_for`` оставлен как прежняя точка входа, чтобы не ломать вызовы
в сервере и тестах. Он просто передаёт трек в смысловой движок архетипов.
"""

from __future__ import annotations

try:
    from .archetypes import archetype_for
    from .resonance import Acoustics, seed_int
except ImportError:
    from archetypes import archetype_for
    from resonance import Acoustics, seed_int


def persona_for(
    artist: str,
    title: str,
    album: str = "",
    acoustics: Acoustics | None = None,
    genre: str = "",
    label: str = "",
) -> dict:
    return archetype_for(
        artist=artist,
        title=title,
        album=album,
        genre=genre,
        label=label,
        acoustics=acoustics,
    )


def zone_for(device_id: str, zones: list[str]) -> str:
    if not zones:
        return "Вечерняя улица"
    return zones[seed_int(device_id, "zone") % len(zones)]


__all__ = ["persona_for", "zone_for"]
