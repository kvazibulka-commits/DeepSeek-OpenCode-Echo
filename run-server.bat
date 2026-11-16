@echo off
chcp 1251 >nul
rem ВАЖНО: файл сохранён в Windows-1251. Не пересохраняйте его в UTF-8, иначе cmd сломает кириллицу.
setlocal EnableExtensions
title ЭХО · сервер

rem Порт можно переопределить переменной PORT перед запуском.
if "%PORT%"=="" set "PORT=8080"

rem Ищем сервер: сначала рядом (server\), потом соседний проект Echo_vinyl.
set "SERVER_DIR="
if exist "%~dp0server\echo_server.py" set "SERVER_DIR=%~dp0server"
if not defined SERVER_DIR if exist "%~dp0..\Echo_vinyl\echo_server.py" set "SERVER_DIR=%~dp0..\Echo_vinyl"
if not defined SERVER_DIR if exist "%~dp0..\echo_server.py" set "SERVER_DIR=%~dp0.."

if not defined SERVER_DIR (
    echo.
    echo   Не найден echo_server.py.
    echo   Положите папку сервера рядом с приложением или поправьте путь в этом файле.
    echo.
    pause
    exit /b 1
)

rem Python: сначала python, затем лаунчер py.
where python >nul 2>nul
if errorlevel 1 (
    where py >nul 2>nul
    if errorlevel 1 (
        echo.
        echo   Python не найден. Установите Python 3 и добавьте его в PATH.
        echo.
        pause
        exit /b 1
    )
    set "PY=py -3"
) else (
    set "PY=python"
)

echo.
echo   ЭХО · сервер
echo   Папка:  %SERVER_DIR%
echo   Порт:   %PORT%
echo   Пульт:  http://127.0.0.1:%PORT%/
echo   Сонар:  http://127.0.0.1:%PORT%/app
echo.
echo   Остановить: Ctrl+C или закрыть окно.
echo.

cd /d "%SERVER_DIR%"

if /i "%~1"=="--no-browser" goto run_server
start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 3; Start-Process 'http://127.0.0.1:%PORT%/'"

:run_server
%PY% echo_server.py
set "EXITCODE=%ERRORLEVEL%"

echo.
echo   Сервер остановлен (код %EXITCODE%).
pause
