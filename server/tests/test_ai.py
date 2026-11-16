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


if __name__ == "__main__":
    unittest.main()
