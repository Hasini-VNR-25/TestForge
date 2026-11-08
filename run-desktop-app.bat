@echo off
title TestForge AI - JavaFX Desktop Application Launcher
cd /d "%~dp0"

echo ============================================================
echo    Launching TestForge AI - JavaFX Desktop Application
echo ============================================================
echo.

:: 1. Check if backend is already running on port 8080
curl.exe -s -o nul http://localhost:8080/api/dashboard
if %errorlevel% equ 0 goto backend_ready

echo [INFO] Backend is not running. Starting Spring Boot Backend service...
start "TestForge Backend (Spring Boot)" cmd /k "cd /d "%~dp0testforge-backend" && java -jar target\testforge-backend-0.0.1-SNAPSHOT.jar"

echo Waiting for backend to initialize on port 8080...
set retries=0

:wait_loop
timeout /t 2 /nobreak >nul
set /a retries+=1
curl.exe -s -o nul http://localhost:8080/api/dashboard
if %errorlevel% equ 0 goto backend_ready
if %retries% geq 25 goto timeout_warning
echo    Waiting for backend... (%retries%/25)
goto wait_loop

:timeout_warning
echo.
echo [WARNING] Backend took longer than expected. Launching UI anyway...
goto launch_ui

:backend_ready
echo.
echo [OK] Backend is active and responding on http://localhost:8080!

:launch_ui
echo.
echo Launching JavaFX Desktop Application...
java -jar testforge-javafx\target\testforge-javafx-2.0.0.jar
if %errorlevel% neq 0 (
    echo.
    echo [INFO] Direct jar execution ended. If jar had an issue, attempting Maven JavaFX runner...
    cd testforge-javafx
    ..\testforge-backend\mvnw.cmd javafx:run
)

echo.
echo Application closed.
pause
