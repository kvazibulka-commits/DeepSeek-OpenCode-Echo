"""Тесты GigaChat-клиента: разбор ответа, очистка и кэш-ключ."""

from __future__ import annotations

import os
import unittest
from unittest.mock import patch

from server import ai


class ParseTest(unittest.TestCase):
    def test_plain_json(self) -> None:
        value = ai._extract_json('{"archetype": "Дозорный фьорда", "observation": "Тихо."}')
        self.assertEqual("Дозорный фьорда", value["archetype"])

    def test_fenced_json(self) -> None:
        text = '```json\n{"archetype": "Ловец огней", "observation": "Идёт."}\n```'
        value = ai._extract_json(text)
        self.assertEqual("Ловец огней", value["archetype"])

    def test_json_with_prose(self) -> None:
        text = 'Вот ответ: {"archetype": "Певец сводов", "observation": "Эхо."} Спасибо.'
        value = ai._extract_json(text)
        self.assertIsNotNone(value)

    def test_invalid(self) -> None:
        self.assertIsNone(ai._extract_json("не json"))
        self.assertIsNone(ai._extract_json(""))
        self.assertIsNone(ai._extract_json("[1, 2]"))

    def test_clean_strips_quotes(self) -> None:
        self.assertEqual("Дозорный фьорда", ai._clean(" «Дозорный фьорда». ", 48))

    def test_valid_archetype(self) -> None:
        self.assertTrue(ai._valid_archetype("Дозорный фьорда"))
        self.assertFalse(ai._valid_archetype("Один"))
        self.assertFalse(ai._valid_archetype("Слишком много слов в этом архетипе"))
        self.assertFalse(ai._valid_archetype("Дозорный 42"))
        self.assertFalse(ai._valid_archetype("Дозорный фьорда!"))


class EnvTest(unittest.TestCase):
    def test_unavailable_without_keys(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            self.assertFalse(ai.available())
            self.assertIsNone(ai.generate("Danheim", "Drums of Drakkar"))

    def test_available_with_keys(self) -> None:
        env = {
            "ECHO_GIGACHAT_CLIENT_ID": "client",
            "ECHO_GIGACHAT_CLIENT_SECRET": "secret",
        }
        with patch.dict(os.environ, env, clear=True):
            self.assertTrue(ai.available())

    def test_scope_and_model_defaults(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            self.assertEqual("GIGACHAT_API_PERS", ai.scope())
            self.assertEqual("GigaChat", ai.model_name())


class CacheKeyTest(unittest.TestCase):
    def test_case_and_diacritics(self) -> None:
        first = ai.cache_key("Munknörr", "Vanir", "Norðr")
        second = ai.cache_key("munknorr", "VANIR", "nordr")
        self.assertEqual(first, second)
        self.assertIn("|", first)

    def test_album_matters(self) -> None:
        self.assertNotEqual(
            ai.cache_key("Кино", "Судно", "Этажи"),
            ai.cache_key("Кино", "Судно", "Другой"),
        )


class AuthKeyTest(unittest.TestCase):
    def test_auth_key_alone_is_enough(self) -> None:
        env = {"ECHO_GIGACHAT_AUTH_KEY": "ZmFrZTpzZWNyZXQ="}
        with patch.dict(os.environ, env, clear=True):
            self.assertTrue(ai.available())
            self.assertEqual("ZmFrZTpzZWNyZXQ=", ai.auth_key())

    def test_client_secret_pair_still_works(self) -> None:
        env = {
            "ECHO_GIGACHAT_CLIENT_ID": "id",
            "ECHO_GIGACHAT_CLIENT_SECRET": "secret",
        }
        with patch.dict(os.environ, env, clear=True):
            self.assertTrue(ai.available())
            self.assertEqual("", ai.auth_key())


class JunkFilterTest(unittest.TestCase):
    def test_extension_pages_are_junk(self) -> None:
        from server.echo_server import is_junk_track

        self.assertTrue(
            is_junk_track("", "chrome-extension://abc/offscreen.html")
        )
        self.assertTrue(is_junk_track("", "about:blank"))
        self.assertTrue(is_junk_track("", "https://example.com/stream"))

    def test_normal_tracks_pass(self) -> None:
        from server.echo_server import is_junk_track

        self.assertFalse(is_junk_track("Munknörr", "Vanir"))
        self.assertFalse(is_junk_track("Молчат Дома", "Судно"))
        self.assertFalse(is_junk_track("Ray Wylie Hubbard", "Choctaw Bingo"))


class MusicDbTest(unittest.TestCase):
    def test_itunes_mapping(self) -> None:
        from server.musicdb import ITUNES_GENRES

        self.assertEqual("rock", ITUNES_GENRES["rock"])
        self.assertEqual("indie", ITUNES_GENRES["alternative"])
        self.assertEqual("folk", ITUNES_GENRES["world"])
        self.assertEqual("electronic", ITUNES_GENRES["electronic"])

    def test_artist_matching_is_forgiving(self) -> None:
        from server.musicdb import _same_artist, _same_title

        self.assertTrue(_same_artist("Munknörr", "Munknorr"))
        self.assertTrue(_same_artist("The Devil and the Almighty Blues",
                                     "The Devil & the Almighty Blues"))
        self.assertTrue(_same_title("Vanir", "Vanir"))
        self.assertFalse(_same_title("Vanir", "Helvegen"))


class EnrichmentClaimTest(unittest.TestCase):
    def test_claim_is_exclusive(self) -> None:
        from server.echo_server import _claim_enrichment, _release_enrichment

        key = "тест|claim|key"
        self.assertTrue(_claim_enrichment(key))
        self.assertFalse(_claim_enrichment(key))
        _release_enrichment(key)
        self.assertTrue(_claim_enrichment(key))
        _release_enrichment(key)


if __name__ == "__main__":
    unittest.main()
