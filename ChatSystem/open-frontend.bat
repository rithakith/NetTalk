@echo off
echo ============================================================
echo         Opening Web Frontend
echo ============================================================
echo.
echo Checking if servers are running...

REM Check if port 8888 is listening
netstat -ano | findstr ":8888" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [WARNING] ChatServer is NOT running on port 8888
    echo Please start the server first: .\start-server.bat
    echo.
)

REM Check if port 8889 is listening
netstat -ano | findstr ":8889" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [WARNING] WebSocket Bridge is NOT running on port 8889
    echo Please start the bridge: .\run-websocket-server.bat
    echo.
)

echo Opening frontend in browser...
start frontend\index.html

echo.
echo ============================================================
echo Frontend opened!
echo.
echo Make sure both servers are running:
echo   - ChatServer (port 8888)
echo   - WebSocket Bridge (port 8889)
echo ============================================================
echo.
pause
