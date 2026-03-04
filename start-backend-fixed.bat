@echo off
echo Starting Spring Boot Backend on port 8080...
cd backend
call mvn spring-boot:run -Dspring-boot.run.fork=true
echo Backend stopped.
pause