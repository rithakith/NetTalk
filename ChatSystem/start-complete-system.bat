@echo off
echo ===============================================
echo   NetTalk Quiz System - Complete Startup
echo ===============================================

cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"

echo.
echo [1/3] Starting ChatServer on port 8888...
start "ChatServer" cmd /k "java com.chatapp.server.ChatServer"

echo.
echo [2/3] Waiting 3 seconds for ChatServer to initialize...
timeout /t 3 /nobreak > nul

echo.
echo [3/4] Starting WebSocket Bridge on port 8889...
start "WebSocket Bridge" cmd /k ".\run-websocket-server.bat"

echo.
echo [4/5] Starting Admin WebSocket Server on port 8890...
timeout /t 2 /nobreak > nul
start "Admin WebSocket" cmd /k ".\run-admin-websocket-server.bat"

echo.
echo [5/5] Opening web interfaces...
timeout /t 2 /nobreak > nul
start "" "frontend/index.html"
start "" "frontend/admin.html"

echo.
echo ===============================================
echo   🎊 NetTalk Quiz System Started Successfully!
echo ===============================================
echo.
echo Servers running:
echo   🟢 ChatServer: http://localhost:8888
echo   🟢 WebSocket Bridge: ws://localhost:8889
echo   👑 Admin WebSocket: ws://localhost:8890
echo.
echo Web Interfaces:
echo   🌐 Main Chat: frontend/index.html
echo   👑 Admin Panel: frontend/admin.html
echo.
echo Quiz Commands Available:
echo   /createquiz "name" - Create new quiz
echo   /addquestion quiz_id^|question^|opt1,opt2,opt3,opt4^|correct^|time
echo   /invitequiz quiz_id user1,user2 - Invite users
echo   /startquiz quiz_id - Start quiz
echo   /joinquiz quiz_id - Join quiz
echo   /answer quiz_id option_number - Submit answer
echo   /quizzes - List active quizzes
echo   /help - Show help
echo.
echo Press any key to close this window...
pause > nul