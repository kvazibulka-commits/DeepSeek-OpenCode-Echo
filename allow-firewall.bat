@echo off
chcp 1251 >nul
rem ВАЖНО: файл сохранён в Windows-1251, не пересохраняйте его в UTF-8.
title ЭХО · разрешить доступ в брандмауэре

net session >nul 2>&1
if errorlevel 1 (
    echo Запрашиваю права администратора…
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo   Открываю входящий TCP-порт 8080 для телефона…
netsh advfirewall firewall delete rule name="Echo Radar Server 8080" >nul 2>&1
netsh advfirewall firewall add rule name="Echo Radar Server 8080" dir=in action=allow protocol=TCP localport=8080 profile=any
if errorlevel 1 (
    echo.
    echo   Не удалось добавить правило. Откройте порт 8080 вручную.
    echo.
    pause
    exit /b 1
)

echo.
echo   Готово. Телефон в той же Wi-Fi-сети может подключаться к ПК.
echo.
pause
