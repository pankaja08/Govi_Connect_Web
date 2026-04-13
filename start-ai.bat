@echo off
echo =======================================
echo   GoviCONNECT AI Engine (FastAPI)
echo =======================================
cd ai-engine
echo ^> Starting server on http://127.0.0.1:8000
echo ^> Press Ctrl+C to stop.
echo.
python -m uvicorn main:app --reload --host 127.0.0.1 --port 8000
pause
