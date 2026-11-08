@echo off
title TestForge AI - System Launcher
cd /d "%~dp0"

echo ============================================================
echo           TestForge AI - System Launcher
echo ============================================================
echo.

:: 1. Check if backend is already running
curl.exe -s -o nul http://localhost:8080/api/dashboard
if %errorlevel% equ 0 goto backend_ready

echo [1/2] Starting Spring Boot Backend Service on port 8080...
if exist "testforge-backend\target\testforge-backend-0.0.1-SNAPSHOT.jar" (
    start "TestForge Backend (Spring Boot)" cmd /k "cd /d "%~dp0testforge-backend" && java -jar target\testforge-backend-0.0.1-SNAPSHOT.jar"
) else (
    echo       [INFO] Pre-built JAR not found. Launching via Maven Wrapper...
    start "TestForge Backend (Spring Boot)" cmd /k "cd /d "%~dp0testforge-backend" && mvnw.cmd spring-boot:run"
)

echo [2/2] Waiting for Backend to initialize...
set retries=0

:wait_loop
timeout /t 2 /nobreak >nul
set /a retries+=1
curl.exe -s -o nul http://localhost:8080/api/dashboard
if %errorlevel% equ 0 goto backend_ready
if %retries% geq 25 goto timeout_warning
echo       Waiting for backend response... (%retries%/25)
goto wait_loop

:timeout_warning
echo.
echo [WARNING] Backend is taking longer than usual to respond. Launching UI anyway...
goto launch_desktop

:backend_ready
echo.
echo [OK] Backend is live on http://localhost:8080!

:launch_desktop
echo.
echo Launching JavaFX Desktop Application...
if exist "testforge-javafx\target\testforge-javafx-2.0.0.jar" (
    start "TestForge Desktop UI" cmd /c "cd /d "%~dp0" && java -jar testforge-javafx\target\testforge-javafx-2.0.0.jar"
) else (
    echo       [INFO] UI JAR not found. Launching via Maven JavaFX runner...
    start "TestForge Desktop UI" cmd /c "cd /d "%~dp0testforge-javafx" && ..\testforge-backend\mvnw.cmd javafx:run"
)

echo.
echo ============================================================
echo Ready! Both Backend and JavaFX UI are running.
echo You can minimize this window.
echo ============================================================
timeout /t 4 /nobreak >nul
