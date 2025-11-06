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

echo [1/2] Starting TCP Chat Server on port 8888...
cd src\main\java
start "ChatServer" java -cp ".;..\..\..\lib\*" com.chatapp.server.ChatServer

echo Waiting for ChatServer to initialize...
timeout /t 3 /nobreak > nul

echo [2/2] Starting WebSocket Bridge Server on port 8889...
start "WebSocketBridge" java -cp ".;..\..\..\lib\Java-WebSocket-1.5.3.jar;..\..\..\lib\gson-2.8.9.jar;..\..\..\lib\slf4j-api-1.7.36.jar;..\..\..\lib\slf4j-simple-1.7.36.jar" com.chatapp.server.WebSocketBridgeServer

echo.
echo ============================================================
echo Both servers are starting in separate windows!
echo.
echo TCP Server:      localhost:8888 (for Java clients)
echo WebSocket Server: ws://localhost:8889 (for web frontend)
echo.
echo Open frontend/index.html in your browser to test!
echo ============================================================
echo.
pause