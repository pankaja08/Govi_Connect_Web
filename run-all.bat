@echo off
echo =======================================
echo   GoviCONNECT — All Systems Startup
echo =======================================

echo ^> Starting AI Engine in a new window...
start "GoviCONNECT — AI Engine" cmd /k "cd ai-engine && python -m uvicorn main:app --reload --host 127.0.0.1 --port 8000"

echo ^> Starting Spring Boot in this window...
echo.
call mvnw.cmd spring-boot:run

pause
