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

### 1. GitHub MCP — репозиторий и CI без ручных curl

Позволяет агенту смотреть issues, PR, статусы Actions и артефакты.

```json
{
  "mcp": {
    "github": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
      "environment": { "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}" },
      "enabled": true
    }
  }
}
```

Токен: fine-grained, права **Contents: Read and write**, **Workflows: Read and write**,
**Actions: Read**, **Issues: Read and write**. Хранить только в переменной окружения.

### 2. Playwright MCP — макеты и визуальные регрессии

Рендер `docs/design/*.html` в PNG, клики по прототипу, сравнение скриншотов.

```json
{
  "mcp": {
    "playwright": {
      "type": "local",
      "command": ["npx", "-y", "@playwright/mcp@latest"],
      "enabled": true
    }
  }
}
```

### 3. Context7 MCP — свежая документация AndroidX/Compose

Помогает не угадывать API при обновлении библиотек.

```json
{
  "mcp": {
    "context7": {
      "type": "local",
      "command": ["npx", "-y", "@upstash/context7-mcp@latest"],
      "enabled": true
    }
  }
}
```

### 4. Mobile MCP — эмулятор и устройство

Тапы, скриншоты, проверки сценариев на Android. Дополняет `android-cli`.
Требует запущенного эмулятора или подключённого телефона.

### 5. Figma MCP — если макеты уйдут в Figma

Dev Mode MCP отдаёт токены и структуру компонентов; полезен, когда дизайн
перестанет жить в HTML.

## Порядок работы над экраном

1. `echo-product-concept` — смысл и текст.
2. `echo-visual-design` — 3 HTML-варианта в `docs/design/`, скриншот через Playwright.
3. Выбор варианта пользователем.
4. `jetpack-compose` + `echo-engineering` — перенос в код.
5. `android-cli` — сборка, установка, скриншот с устройства.
6. `echo-quality` — ревью, тесты, чек-лист.
7. Коммит и CI, APK в артефактах.

## Локальные команды

```bat
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
adb install -r echo-radar-0.1.0-debug.apk
```

Если путь проекта содержит кириллицу, unit-тесты локально запускать из ASCII-пути
(например, `C:\Users\Public\echo-build`) или доверять CI.

`android preview render :app` рендерит Compose-экраны без устройства. Если кириллица
в `%USERPROFILE%`, задайте `ANDROID_USER_HOME=C:\Users\Public\android-home` — кэш
превью уйдёт в ASCII-путь, и CLI перестанет терять файлы.
