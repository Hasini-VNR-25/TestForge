@echo off
title TestForge Backend Service
cd /d "%~dp0"

echo ============================================================
echo           TestForge Backend Service (Spring Boot)
echo ============================================================
echo.

if exist "target\testforge-backend-0.0.1-SNAPSHOT.jar" (
    echo [INFO] Starting pre-built backend JAR on port 8080...
    java -jar target\testforge-backend-0.0.1-SNAPSHOT.jar
) else (
    echo [INFO] JAR not found. Starting backend via Maven Wrapper...
    call mvnw.cmd spring-boot:run
)

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Backend stopped unexpectedly with exit code %errorlevel%.
    pause
)
