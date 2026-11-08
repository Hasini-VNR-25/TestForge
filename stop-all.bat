@echo off
title TestForge AI - Stop Services
cd /d "%~dp0"

echo ============================================================
echo           TestForge AI - Stopping All Services
echo ============================================================
echo.

echo Checking for processes on port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Terminating backend process with PID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo All TestForge services stopped cleanly.
echo ============================================================
timeout /t 2 /nobreak >nul
