"""Юнит-тесты чистого ядра нового сервера."""

from __future__ import annotations

import unittest

from server.personas import persona_for
from server.resonance import (
    camelot_score,
    cultural_score,
    infer_acoustics,
    resonance,
    tempo_score,
)


class CamelotTest(unittest.TestCase):
    def test_same_key(self) -> None:
        self.assertEqual(1.0, camelot_score("8A", "8A"))

    def test_neighbour_key(self) -> None:
        self.assertGreater(camelot_score("8A", "9A"), 0.8)

    def test_relative_key(self) -> None:
        self.assertGreater(camelot_score("8A", "8B"), 0.7)

    def test_far_key(self) -> None:
        self.assertLess(camelot_score("8A", "2A"), 0.4)

    def test_invalid_key(self) -> None:
        self.assertEqual(0.5, camelot_score("", "8A"))


class TempoTest(unittest.TestCase):
    def test_same(self) -> None:
        self.assertEqual(1.0, tempo_score(120, 120))

    def test_octave(self) -> None:
        self.assertEqual(1.0, tempo_score(120, 240))
        self.assertEqual(1.0, tempo_score(120, 60))

    def test_close(self) -> None:
        self.assertGreater(tempo_score(120, 128), 0.6)

    def test_far(self) -> None:
        self.assertLess(tempo_score(120, 180), 0.3)


class CulturalTest(unittest.TestCase):
    def test_same_genre(self) -> None:
        self.assertEqual(0.95, cultural_score("post-punk", "post-punk"))

    def test_bridge(self) -> None:
        self.assertGreater(cultural_score("post-punk", "jazz"), 0.5)

    def test_foreign(self) -> None:
        self.assertLess(cultural_score("classical", "lo-fi"), 0.5)


class ResonanceTest(unittest.TestCase):
    def test_bounds(self) -> None:
        first = infer_acoustics("Молчат Дома", "Судно", "Этажи")
        second = infer_acoustics("Burial", "Archangel", "Untrue")
        percent, story = resonance(first, second)
        self.assertGreaterEqual(percent, 58)
        self.assertLessEqual(percent, 99)
        self.assertTrue(story)

    def test_self_is_high(self) -> None:
        track = infer_acoustics("Purl", "Renovatio", "Renovatio")
        percent, _ = resonance(track, track)
        self.assertGreaterEqual(percent, 85)

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
