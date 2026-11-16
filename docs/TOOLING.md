# Инструменты ЭХО · скилы и MCP

Документ живой: здесь собрано, чем удобнее вести Android-клиент, дизайн и релизы.

## Скилы (загружать перед работой)

| Скил | Когда |
|---|---|
| `echo-engineering` | Правки Kotlin, API, медиа-службы, сборка, деплой |
| `echo-visual-design` | Токены, вёрстка, анимации, ревью скриншотов |
| `echo-product-concept` | Тексты, названия, онбординг, границы фич |
| `echo-quality` | Ревью, безопасность, производительность, Definition of Done |
| `jetpack-compose` | Compose-экраны, состояния, навигация, Material 3 |
| `android-cli` | Сборка, установка APK, эмулятор, скриншоты, инспекция UI |
| `browser` | Открыть HTML-макет, снять скриншот, проверить вёрстку |

Правило сессии: перед дизайн-решением — HTML-визуализация, минимум три варианта,
потом выбор и перенос в Compose.

## MCP-серверы

OpenCode V2 хранит серверы под `mcp.servers`. Поля `enabled` нет: сервер подключается
сразу, отключение — `"disabled": true`. Конфиг перечитывается без перезапуска.

### Playwright MCP — включён

Управляет установленным Chrome: открывает HTML-макеты, снимает скриншоты, кликает
по прототипу, собирает console и network. Проверено на `docs/design/variants-0.2.html`.

Что установлено (Node в системе не было):

- Node.js LTS 26.13.1 → `C:\Users\Public\node-portable\node-v26.13.1-win-x64`
- `@playwright/mcp` 0.0.85 → `C:\Users\Public\playwright-mcp`

```json
{
  "mcp": {
    "servers": {
      "playwright": {
        "type": "local",
        "command": [
          "C:\\Users\\Public\\node-portable\\node-v26.13.1-win-x64\\node.exe",
          "C:\\Users\\Public\\playwright-mcp\\node_modules\\@playwright\\mcp\\cli.js",
          "--browser",
          "chrome",
          "--headless",
          "--isolated"
        ],
        "timeout": { "startup": 60000 }
      }
    }
  }
}
```

`cwd` не задан — сервер работает из корня текущего проекта, поэтому видит
`docs/design`. Chrome берётся установленный, браузеры Playwright не скачиваются.

### GitHub MCP — репозиторий и CI

Issues, PR, статусы Actions и артефакты без ручных curl. Нужен Node и PAT:
fine-grained, права **Contents: Read and write**, **Workflows: Read and write**,
**Actions: Read**, **Issues: Read and write**. Токен — только в переменной окружения.

```json
{
  "mcp": {
    "servers": {
      "github": {
        "type": "local",
        "command": [
          "C:\\Users\\Public\\node-portable\\node-v26.13.1-win-x64\\node.exe",
          "C:\\Users\\Public\\playwright-mcp\\node_modules\\@modelcontextprotocol\\server-github\\dist\\index.js"
        ],
        "environment": { "GITHUB_PERSONAL_ACCESS_TOKEN": "{env:GITHUB_TOKEN}" }
      }
    }
  }
}
```

Пакет ставится в тот же prefix: `npm install --prefix C:\Users\Public\playwright-mcp @modelcontextprotocol/server-github`.

### Context7 MCP — свежая документация AndroidX/Compose

Удобнее remote-сервер, без ключа и установки:

```json
{
  "mcp": {
    "servers": {
      "context7": {
        "type": "remote",
        "url": "https://mcp.context7.com/mcp"
      }
    }
  }
}
```

### Mobile MCP — эмулятор и устройство

Тапы, скриншоты, проверки сценариев на Android. Дополняет `android-cli`.
Требует запущенного эмулятора или подключённого телефона.

### Figma MCP — если макеты уйдут в Figma

Dev Mode MCP отдаёт токены и структуру компонентов; полезен, когда дизайн
перестанет жить в HTML.

## Порядок работы над экраном

1. `echo-product-concept` — смысл и текст.
2. `echo-visual-design` — 3 HTML-варианта в `docs/design/`, скриншот через Playwright MCP.
3. Выбор варианта пользователем.
4. `jetpack-compose` + `echo-engineering` — перенос в код.
5. `android-cli` — сборка, установка, скриншот с устройства.
6. `echo-quality` — ревью, тесты, чек-лист.
7. Коммит и CI, APK в артефактах.

## Локальные команды

```bat
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
adb install -r echo-radar-0.2.0-debug.apk
```

Если путь проекта содержит кириллицу, unit-тесты локально запускать из ASCII-пути
(например, `C:\Users\Public\echo-build`) или доверять CI.

`android preview render :app` рендерит Compose-экраны без устройства. Если кириллица
в `%USERPROFILE%`, задайте `ANDROID_USER_HOME=C:\Users\Public\android-home` — кэш
превью уйдёт в ASCII-путь, и CLI перестанет терять файлы.
