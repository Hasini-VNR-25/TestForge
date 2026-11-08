@echo off
title TestForge Desktop UI
cd /d "%~dp0"

echo ============================================================
echo           TestForge JavaFX Desktop UI
echo ============================================================
echo.

if exist "target\testforge-javafx-2.0.0.jar" (
    echo [INFO] Starting JavaFX Desktop Application...
    java -jar target\testforge-javafx-2.0.0.jar
) else (
    echo [INFO] JAR not found. Running via Maven JavaFX plugin...
    call ..\testforge-backend\mvnw.cmd javafx:run
)

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Application closed with error code %errorlevel%.
    pause
)
