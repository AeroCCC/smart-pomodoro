@echo off
setlocal
cd /d "%~dp0backend"

set SPRING_PROFILES_ACTIVE=prod
if not defined MYSQL_HOST set MYSQL_HOST=localhost
if not defined MYSQL_PORT set MYSQL_PORT=3306
if not defined MYSQL_DB set MYSQL_DB=pomotodo
if not defined MYSQL_USER set MYSQL_USER=root
if not defined MYSQL_PASSWORD (
  echo [ERROR] MYSQL_PASSWORD is not set.
  echo Set it before running, for example:
  echo   set MYSQL_PASSWORD=your_password
  exit /b 1
)

if not defined REDIS_HOST set REDIS_HOST=localhost
if not defined REDIS_PORT set REDIS_PORT=6379
if not defined REDIS_PASSWORD set REDIS_PASSWORD=
if not defined CORS_ALLOWED_ORIGINS set CORS_ALLOWED_ORIGINS=http://localhost:*,http://127.0.0.1:*
if not defined SERVER_PORT set SERVER_PORT=8080
if not defined DASHSCOPE_BASE_URL set DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
if not defined DASHSCOPE_MODEL set DASHSCOPE_MODEL=qwen-max
if not defined AI_NEXT_STEP_USE_MODEL set AI_NEXT_STEP_USE_MODEL=false
if not defined JWT_SECRET (
  set JWT_SECRET=local-dev-jwt-secret-min-32-characters!!
  echo [WARN] JWT_SECRET not set. Using local fallback secret.
)

for /f %%P in ('powershell -NoProfile -Command "$port=[int]$env:SERVER_PORT; while ($true) { $listener = $null; try { $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $port); $listener.Start(); $listener.Stop(); break } catch { if ($listener) { try { $listener.Stop() } catch {} }; $port++ } }; Write-Output $port"') do set SERVER_PORT=%%P
if not "%SERVER_PORT%"=="8080" (
  echo [WARN] Port 8080 is already in use. Falling back to port %SERVER_PORT%.
)

echo Starting backend with MySQL + Redis on port %SERVER_PORT%...
mvn spring-boot:run
