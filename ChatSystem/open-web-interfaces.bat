@echo off
echo ===============================================
echo   Opening NetTalk Quiz System Web Interfaces
echo ===============================================
echo.
echo Opening main chat interface...
start "" "frontend/index.html"

echo.
echo Opening admin panel...
start "" "frontend/admin.html"

echo.
echo Both interfaces opened in your default browser.
echo Make sure both servers are running:
echo   - ChatServer on port 8888
echo   - WebSocket Bridge on port 8889
echo.
pause