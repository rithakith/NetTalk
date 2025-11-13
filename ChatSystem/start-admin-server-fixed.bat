@echo off
echo ===============================================
echo   NetTalk Admin Server - Fixed Version
echo ===============================================

cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"

echo.
echo Checking for required libraries...

if not exist "lib\Java-WebSocket-1.5.3.jar" (
    echo ERROR: Java-WebSocket-1.5.3.jar not found in lib folder
    pause
    exit /b 1
)

if not exist "lib\gson-2.8.9.jar" (
    echo ERROR: gson-2.8.9.jar not found in lib folder  
    pause
    exit /b 1
)

echo Libraries found!
echo.

echo Starting Admin WebSocket Server on port 8890...
echo Make sure ChatServer is running on port 8888!
echo.

REM Use current directory as classpath root
java -cp ".;lib\*" com.chatapp.server.AdminWebSocketServer

echo.
echo Admin server stopped.
pause