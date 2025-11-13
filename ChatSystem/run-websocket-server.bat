@echo off
echo ============================================================
echo         WebSocket Bridge Server Startup
echo ============================================================
echo.
echo Features included:
echo - Real-time chat messaging
echo - Private messaging support
echo - Typing indicators
echo - API integration
echo.
echo NOTE: Make sure ChatServer is running on port 8888 first!
echo ============================================================
echo.

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
echo Detected Java version: %JAVA_VERSION%
echo.

REM Check for required libraries
if not exist lib\Java-WebSocket-1.5.3.jar (
    echo [ERROR] Java-WebSocket-1.5.3.jar not found in lib/ folder
    echo Please download from: https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.3/Java-WebSocket-1.5.3.jar
    pause
    exit /b 1
)

if not exist lib\gson-2.8.9.jar (
    echo [ERROR] gson-2.8.9.jar not found in lib/ folder
    echo Please download from: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
    pause
    exit /b 1
)

cd src\main\java
java -cp ".;..\..\..\lib\Java-WebSocket-1.5.3.jar;..\..\..\lib\gson-2.8.9.jar;..\..\..\lib\slf4j-api-1.7.36.jar;..\..\..\lib\slf4j-simple-1.7.36.jar" com.chatapp.server.WebSocketBridgeServer
pause