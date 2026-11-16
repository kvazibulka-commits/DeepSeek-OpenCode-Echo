# ЭХО · сервер 4.0

Новый сервер вместо legacy `Echo_vinyl`. Совместим с Android-клиентом и отдаёт
больше данных: альбом, обложку, персону и резонанс.

## Запуск

Из корня проекта:

```bat
run-server.bat
```

Батник сначала ищет `server\echo_server.py`, затем старый `..\Echo_vinyl`.
Пульт: `http://<ip>:8080/`. API: `/api/state`.

Порт: `PORT` (по умолчанию 8080). Токен: `ECHO_TOKEN` или `echo_config.json`.
Файл `server/echo_config.json` в `.gitignore`; пример — `echo_config.example.json`.

## Что внутри

| Файл | Роль |
|---|---|
| `echo_server.py` | HTTP API, SQLite, обложки, реакции, PC-мост, UDP-поиск |
| `resonance.py` | Camelot, темп, культурный мост, формула резонанса |
| `personas.py` | архетипы, образы, зоны |
| `templates/dashboard.html` | новый пульт без emoji и legacy |
| `tests/test_core.py` | юнит-тесты ядра |

## API

Совместим с клиентом:

- `GET /api/state?deviceId=` — состояние эфира;
- `GET /api/playlist?limit=` — летопись треков;
- `GET /api/cover/<id>` — обложка;
- `GET /api/health` — статус;
- `POST /api/signal`, `/api/react`, `/api/drop_capsule`, `/api/bump_encounter`,
  `/api/needle_sync`, `/api/send_warmth`, `/api/wave_impulse`, `/api/delete_signal`,
  `/api/delete_capsule`.

Внешние запросы требуют `X-Echo-Token`; loopback — без токена.
CORS открыт, `Cache-Control: no-store` для состояния.

## Пульт

`/` — чистый ночной пульт: KPI, сигналы с обложками, тайники, отклики и летопись.
Рычаги:

- «Волна в эфир» — broadcast-импульс;
- на карточке сигнала — «Волна», «Отклик», «Удалить»;
- «Создать тайник» — форма с альбомом, посланием, замком и бонусом;
- у тайника — «Удалить».

Каждый POST пишется в лог: `POST /api/... target=... sender=...`.

## Данные

SQLite `server/echo_memory.db` (WAL). Таблицы: `devices`, `capsules`, `history`.
Сигналы старше 90 минут удаляются, призраки — старше 5 минут.
Координаты размываются джиттером по зоне.

Обложки живут в памяти (LRU 40) и не переживают перезапуск — так же, как в старом
сервере. Если нужна постоянная обложка, её стоит складывать в файл рядом с БД.

## PC-узел

`pc_bridge` раз в 2.5 секунды читает Windows SMTC:
артист, трек, **альбом** и обложку. Если `winrt` не установлен, сервер работает
без ПК-узла и пишет об этом в лог.

Отключить мост: `ECHO_DISABLE_SMTC=1`.

## Поиск сервера

Сервер каждые 3 секунды шлёт в LAN UDP-пакет
`ECHO_SERVER_DISCOVERY:<port>` на `255.255.255.255:8888`.
Android-клиент ловит его кнопкой «Найти сервер в Wi-Fi».

## Тесты

```bat
python -m unittest discover -s server/tests -v
```
