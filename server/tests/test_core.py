"""Юнит-тесты ядра нового сервера: резонанс, артист, альбом, жанр."""

from __future__ import annotations

import unittest

from server.personas import persona_for
from server.resonance import (
    Acoustics,
    album_similarity,
    artist_similarity,
    camelot_score,
    genre_affinity,
    infer_acoustics,
    resonance,
    tempo_score,
    text_similarity,
)


class SimilarityTest(unittest.TestCase):
    def test_artist_exact_and_case(self) -> None:
        self.assertEqual(1.0, artist_similarity("Молчат Дома", "молчат дома"))

    def test_artist_collaboration(self) -> None:
        self.assertGreater(artist_similarity("Purl & Alveol", "Alveol"), 0.7)

    def test_artist_different(self) -> None:
        self.assertLess(artist_similarity("Кино", "Burial"), 0.2)

    def test_album_edition(self) -> None:
        self.assertGreater(album_similarity("Этажи (Deluxe Edition)", "Этажи"), 0.9)

    def test_album_different(self) -> None:
        self.assertLess(album_similarity("Этажи", "Untrue"), 0.3)

    def test_title_similarity(self) -> None:
        self.assertGreater(text_similarity("Судно", "Судно"), 0.99)
        self.assertLess(text_similarity("Судно", "Archangel"), 0.2)


class GenreTest(unittest.TestCase):
    def test_same_genre(self) -> None:
        self.assertEqual(1.0, genre_affinity("post-punk", "post-punk"))

    def test_sibling_genre(self) -> None:
        self.assertGreater(genre_affinity("post-punk", "synthwave"), 0.6)

    def test_bridge_genre(self) -> None:
        self.assertGreater(genre_affinity("ambient", "classical"), 0.4)

    def test_detect_post_punk(self) -> None:
        track = infer_acoustics("Молчат Дома", "Судно", "Этажи")
        self.assertEqual("post-punk", track.genre)

    def test_detect_ambient(self) -> None:
        track = infer_acoustics("Brian Eno", "An Ending", "Ambient 1")
        self.assertEqual("ambient", track.genre)


class AcousticTest(unittest.TestCase):
    def test_camelot(self) -> None:
        self.assertEqual(1.0, camelot_score("8A", "8A"))
        self.assertGreater(camelot_score("8A", "9A"), 0.8)
        self.assertGreater(camelot_score("8A", "8B"), 0.7)
        self.assertLess(camelot_score("8A", "2A"), 0.4)

    def test_tempo(self) -> None:
        self.assertEqual(1.0, tempo_score(120, 240))
        self.assertGreater(tempo_score(120, 128), 0.6)
        self.assertLess(tempo_score(120, 180), 0.3)


class ResonanceTest(unittest.TestCase):
    def test_same_track_is_max(self) -> None:
        track = infer_acoustics("Кино", "Группа крови", "Группа крови")
        percent, story = resonance(track, track)
        self.assertGreaterEqual(percent, 96)
        self.assertIn("Один и тот же трек", story)

    def test_same_artist_beats_foreign_artist(self) -> None:
        mine = infer_acoustics("Кино", "Группа крови", "Группа крови")
        same_artist = infer_acoustics("Кино", "Звезда по имени Солнце", "Звезда по имени Солнце")
        foreign = infer_acoustics("Burial", "Archangel", "Untrue")
        same_percent, _ = resonance(mine, same_artist)
        foreign_percent, _ = resonance(mine, foreign)
        self.assertGreater(same_percent, foreign_percent)

    def test_same_album_adds_weight(self) -> None:
        base = dict(bpm=120, camelot="8A", genre="post-punk", energy=0.5, valence=0.4)
        mine = Acoustics(**base, artist="A", title="One", album="Album")
        same_album = Acoustics(**base, artist="B", title="Two", album="Album")
        other_album = Acoustics(**base, artist="B", title="Two", album="Other")
        self.assertGreater(resonance(mine, same_album)[0], resonance(mine, other_album)[0])

    def test_same_genre_adds_weight(self) -> None:
        base = dict(bpm=120, camelot="8A", energy=0.5, valence=0.4)
        mine = Acoustics(**base, genre="post-punk", artist="A", title="One", album="X")
        same = Acoustics(**base, genre="post-punk", artist="B", title="Two", album="Y")
        foreign = Acoustics(**base, genre="classical", artist="B", title="Two", album="Y")
        self.assertGreater(resonance(mine, same)[0], resonance(mine, foreign)[0])

    def test_bounds(self) -> None:
        first = infer_acoustics("Молчат Дома", "Судно", "Этажи")
        second = infer_acoustics("Burial", "Archangel", "Untrue")
        percent, story = resonance(first, second)
        self.assertGreaterEqual(percent, 58)
        self.assertLessEqual(percent, 99)
        self.assertTrue(story)

    def test_deterministic(self) -> None:
        first = infer_acoustics("Кино", "Группа крови", "Группа крови")
        second = infer_acoustics("Кино", "Группа крови", "Группа крови")
        self.assertEqual(first, second)


class PersonaTest(unittest.TestCase):
    def test_stable_for_same_track(self) -> None:
        first = persona_for("Кино", "Группа крови", "Группа крови")
        second = persona_for("Кино", "Группа крови", "Группа крови")
        self.assertEqual(first, second)
        self.assertTrue(first["personaTitle"])
        self.assertTrue(first["personaObservation"])

    def test_color_is_opaque(self) -> None:
        persona = persona_for("Burial", "Archangel", "Untrue")
        self.assertEqual(0xFF, (int(persona["colorHex"]) >> 24) & 0xFF)


if __name__ == "__main__":
    unittest.main()
