@echo off
cls
echo ==========================================
echo    Smart PomoTodo Production Startup
echo ==========================================
echo.

cd /d "%~dp0"

REM Check if backend folder exists
if not exist "backend\pom.xml" (
    echo [Error] Please run this script from project root directory!
    pause
    exit /b 1
)

echo [1/4] Checking MySQL service...
sc query MYSQL57 | find "RUNNING" >nul
if %ERRORLEVEL% == 0 (
    echo     OK - MySQL is running
) else (
    echo     Starting MySQL...
    net start MYSQL57
    if %ERRORLEVEL% NEQ 0 (
        echo [Error] Failed to start MySQL
        echo Please check if MySQL service name is correct
        pause
        exit /b 1
    )
)

echo.
echo [2/4] Checking Redis service...
redis-cli ping 2>nul | find "PONG" >nul
if %ERRORLEVEL% == 0 (
    echo     OK - Redis is running
) else (
    echo [Warning] Redis may not be running
    echo Please start Redis manually if needed
    timeout /t 3 >nul
)

echo.
echo [3/4] Testing database connection...
mysql -u pomotodo -p040317 -e "SELECT 1" >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo     OK - Database connection successful
) else (
    echo [Error] Cannot connect to database
    echo Please check:
    echo   1. MySQL is running
    echo   2. User 'pomotodo' exists
    echo   3. Database 'pomotodo' exists
    pause
    exit /b 1
)

echo.
echo [4/4] Setting environment variables...
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_DB=pomotodo
set MYSQL_USER=pomotodo
set MYSQL_PASSWORD=040317
set REDIS_HOST=localhost
set REDIS_PORT=6379
set JWT_SECRET=P67p1P75qbBCdVh8wJoK2ZaIIVbhIdlUFUq6tqY4LzCHZCxFeF6nAVyVm7WCYlnXBKD/NhZHO7gR2h4wPUkNyg==
set VAPID_PUBLIC_KEY=BJX3s3L_Mf9C9dJ8kQ2nR5pL7wX9yZ0aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0uV1wX2yZ3aB4cD5eF6gH7iJ8kL9mN0oP1qR2sT3uV4wX5yZ6
set VAPID_PRIVATE_KEY=aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0uV1wX2yZ3aB4cD5eF6gH7iJ8kL9
set VAPID_SUBJECT=mailto:admin@pomotodo.com
echo     OK - Environment variables set

echo.
echo ==========================================
echo Backend will start on: http://localhost:8080
echo.
echo Make sure you have compiled the project first:
echo   cd backend
echo   mvn clean package -DskipTests
echo ==========================================
echo.
echo Starting backend...
echo Press Ctrl+C to stop
echo.

cd backend

REM Check if jar file exists
if exist "target\smart-pomotodo-1.0.0.jar" (
    echo Running from JAR file...
    "C:\Program Files\Java\jdk-17\bin\java.exe" -jar target\smart-pomotodo-1.0.0.jar --spring.profiles.active=prod
) else (
    echo JAR file not found. Please compile first:
    echo   cd backend
    echo   mvn clean package -DskipTests
    pause
    exit /b 1
)

cd ..
pause
