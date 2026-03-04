@echo off
echo ==========================================
echo    Smart PomoTodo Backend Startup
echo ==========================================
echo.

cd /d "%~dp0"
cd backend

echo [1/3] Checking JAR file...
if exist "target\smart-pomotodo-1.0.0.jar" (
    echo     OK - JAR file exists
) else (
    echo [Error] JAR file not found. Please compile first:
    echo   cd backend
    echo   mvn clean package -DskipTests
    pause
    exit /b 1
)

echo.
echo [2/3] Setting environment variables...
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_DB=pomotodo
set MYSQL_USER=pomotodo
set MYSQL_PASSWORD=040317
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PASSWORD=
set JWT_SECRET=P67p1P75qbBCdVh8wJoK2ZaIIVbhIdlUFUq6tqY4LzCHZCxFeF6nAVyVm7WCYlnXBKD/NhZHO7gR2h4wPUkNyg==
echo     OK

echo.
echo [3/3] Starting backend service...
echo ==========================================
echo Backend: http://localhost:8080
echo Frontend: http://localhost:3000
echo Press Ctrl+C to stop
echo ==========================================
echo.

"C:\Program Files\Java\jdk-17\bin\java.exe" -Xmx512m -jar target\smart-pomotodo-1.0.0.jar --spring.profiles.active=prod

pause
