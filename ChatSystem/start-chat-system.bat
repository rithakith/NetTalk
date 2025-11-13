@echo off
echo ============================================================
echo              NetTalk Chat System Startup
echo ============================================================
echo.
echo Features:
echo - Real-time chat messaging
echo - Private messaging
echo - Typing indicators 
echo - User management
echo - API integration
echo - WebSocket support for web frontend
echo.
echo Starting servers in order...
echo.

echo [Step 0/3] Compiling Java source files...
cd src\main\java
javac -encoding UTF-8 -cp ".;..\..\..\lib\*" com\chatapp\server\*.java com\chatapp\common\*.java com\chatapp\client\*.java com\chatapp\api\*.java 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Please check your Java installation.
    cd ..\..\..
    pause
    exit /b 1
)
echo [SUCCESS] Compilation completed!
echo.

echo [Step 1/3] Starting TCP Chat Server on port 8888...
start "ChatServer" java -cp ".;..\..\..\lib\*" com.chatapp.server.ChatServer

echo Waiting for ChatServer to initialize...
timeout /t 3 /nobreak > nul

echo [Step 2/3] Starting WebSocket Bridge Server on port 8889...
start "WebSocketBridge" java -cp ".;..\..\..\lib\Java-WebSocket-1.5.3.jar;..\..\..\lib\gson-2.8.9.jar;..\..\..\lib\slf4j-api-1.7.36.jar;..\..\..\lib\slf4j-simple-1.7.36.jar" com.chatapp.server.WebSocketBridgeServer

echo.
echo [Step 3/3] Opening frontend in browser...
timeout /t 2 /nobreak > nul
cd ..\..\..
start frontend\index.html

echo.
echo ============================================================
echo All servers are running!
echo.
echo TCP Server:      localhost:8888 (for Java clients)
echo WebSocket Server: ws://localhost:8889 (for web frontend)
echo Frontend:        opened in default browser
echo.
echo To connect Java clients, run: .\start-client.bat
echo ============================================================
echo.
pause