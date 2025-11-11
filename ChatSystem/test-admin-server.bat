@echo off
echo ===============================================
echo   Testing Admin WebSocket Server Setup
echo ===============================================

cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"

echo.
echo [1/3] Starting ChatServer on port 8888...
start "ChatServer" cmd /k "java com.chatapp.server.ChatServer"

echo.
echo [2/3] Waiting 5 seconds for ChatServer to initialize...
timeout /t 5 /nobreak > nul

echo.
echo [3/3] Starting Admin WebSocket Server on port 8890...
call .\run-admin-websocket-server.bat

echo.
echo If successful, admin server is running on ws://localhost:8890
pause