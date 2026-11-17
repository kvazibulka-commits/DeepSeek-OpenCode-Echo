"""Тесты архетипов и источника жанра."""

from __future__ import annotations

import unittest

from server.archetypes import archetype_for, pick_motif
from server.genres import genre_label, resolve_genre
from server.resonance import Acoustics, infer_acoustics, resonance


def build(artist: str, title: str, album: str = "", source_genre: str = ""):
    info = resolve_genre(artist, title, album, source_genre)
    acoustics = infer_acoustics(artist, title, album, genre=info.genre)
    archetype = archetype_for(artist, title, album, info.genre, info.label, acoustics)
    return info, archetype


class GenreSourceTest(unittest.TestCase):
    def test_player_wins(self) -> None:
        info = resolve_genre("Любой Артист", "Любой Трек", "", "Пост-панк")
        self.assertEqual("post-punk", info.genre)
        self.assertEqual("player", info.origin)
        self.assertEqual("пост-панк", info.label)

    def test_player_synth_label(self) -> None:
        info = resolve_genre("X", "Y", "", "холодный синти")
        self.assertEqual("synthwave", info.genre)
        self.assertEqual("player", info.origin)

    def test_player_unknown_label_kept(self) -> None:
        info = resolve_genre("X", "Y", "", "Народная музыка")
        self.assertEqual("folk", info.genre)
        self.assertEqual("player", info.origin)

    def test_catalog_artist(self) -> None:
        info = resolve_genre("Молчат Дома", "Судно", "Этажи")
        self.assertEqual("post-punk", info.genre)
        self.assertEqual("catalog", info.origin)

    def test_catalog_title_hint(self) -> None:
        info = resolve_genre("Неизвестный", "Drums of Drakkar", "Vikings")
        self.assertEqual("nordic folk", info.genre)
        self.assertEqual("catalog", info.origin)

    def test_diacritics_fold(self) -> None:
        info = resolve_genre("Munknörr", "Vanir", "Norðr")
        self.assertEqual("nordic folk", info.genre)
        self.assertEqual("catalog", info.origin)

    def test_unknown_genre_is_not_invented(self) -> None:
        info = resolve_genre("E-L-R", "Seeds", "Vague")
        self.assertEqual("unknown", info.genre)
        self.assertEqual("жанр не указан", info.label)
        self.assertEqual("guess", info.origin)


class ArchetypeTest(unittest.TestCase):
    def test_nordic_track_gets_nordic_archetype(self) -> None:
        info, archetype = build("Danheim", "Drums of Drakkar", "Vikings")
        self.assertEqual("nordic folk", info.genre)
        self.assertEqual("nordic", archetype["motif"])
        images = ("драккар", "северн", "рунн", "фьорд", "пастбищ", "валгалл", "берега")
        self.assertTrue(any(word in archetype["personaTitle"].lower() for word in images))
        self.assertNotIn("панел", archetype["personaTitle"].lower())

    def test_archetype_is_stable(self) -> None:
        first = build("Danheim", "Drums of Drakkar", "Vikings")[1]
        second = build("Danheim", "Drums of Drakkar", "Vikings")[1]
        self.assertEqual(first["personaTitle"], second["personaTitle"])
        self.assertEqual(first["colorHex"], second["colorHex"])

    def test_archetype_differs_between_tracks(self) -> None:
        titles = {"Судно", "Тоска", "Клетка", "Люди Надоели", "Звезды"}
        names = {
            build("Молчат Дома", title, "Этажи")[1]["personaTitle"] for title in titles
        }
        self.assertGreater(len(names), 1)

    def test_archetype_is_two_words(self) -> None:
        _, archetype = build("Kavinsky", "Nightcall", "OutRun")
        self.assertGreaterEqual(len(archetype["personaTitle"].split()), 2)
        self.assertTrue(archetype["personaObservation"])

    def test_mood_label_is_genre_label(self) -> None:
        info, archetype = build("Kavinsky", "Nightcall", "OutRun")
        self.assertEqual(info.label, archetype["moodLabel"])
        self.assertEqual(genre_label(info.genre), archetype["moodLabel"])

    def test_motif_fallback_by_genre(self) -> None:
        motif = pick_motif("Неизвестный", "Без слов", "", "ambient")
        self.assertEqual("dream", motif["id"])

    def test_short_latin_keyword_needs_word_start(self) -> None:
        # «bar» не должен срабатывать на фамилии Hubbard.
        motif = pick_motif("Ray Wylie Hubbard", "Choctaw Bingo", "", "folk")
        self.assertEqual("folk", motif["id"])

    def test_colour_is_opaque(self) -> None:
        _, archetype = build("Burial", "Archangel", "Untrue")
        self.assertEqual(0xFF, (int(archetype["colorHex"]) >> 24) & 0xFF)


class ResonanceWithGenreTest(unittest.TestCase):
    def test_unknown_genres_do_not_leak_into_story(self) -> None:
        first = Acoustics(bpm=110, camelot="8A", genre="unknown", energy=0.5, valence=0.5,
                          artist="A", title="One", album="X")
        second = Acoustics(bpm=112, camelot="8A", genre="unknown", energy=0.5, valence=0.5,
                           artist="B", title="Two", album="Y")
        percent, story = resonance(first, second)
        self.assertNotIn("не указан", story)
        self.assertGreaterEqual(percent, 58)

    def test_same_genre_from_catalog_connects(self) -> None:
        first_info, _ = build("Danheim", "Drums of Drakkar", "Vikings")
        second_info, _ = build("Wardruna", "Helvegen", "Runaljod")
        third_info, _ = build("Kavinsky", "Nightcall", "OutRun")
        self.assertEqual(first_info.genre, second_info.genre)

        first = infer_acoustics("Danheim", "Drums of Drakkar", "Vikings", genre=first_info.genre)
        second = infer_acoustics("Wardruna", "Helvegen", "Runaljod", genre=second_info.genre)
        third = infer_acoustics("Kavinsky", "Nightcall", "OutRun", genre=third_info.genre)

        close, _ = resonance(first, second)
        far, _ = resonance(first, third)
        self.assertGreater(close, far)

    def test_player_genre_used_by_acoustics(self) -> None:
        acoustics = infer_acoustics("X", "Y", "", genre="metal")
        self.assertEqual("metal", acoustics.genre)
        self.assertGreater(acoustics.energy, 0.7)


if __name__ == "__main__":
    unittest.main()
