@echo off
echo Starting WebSocket Bridge Server on port 8889...
echo.
echo NOTE: This requires Java-WebSocket and Gson libraries in the lib/ folder
echo.

if not exist lib\Java-WebSocket-1.5.3.jar (
    echo ERROR: Java-WebSocket-1.5.3.jar not found in lib/ folder
    echo Please download from: https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.3/Java-WebSocket-1.5.3.jar
    pause
    exit /b 1
)

if not exist lib\gson-2.8.9.jar (
    echo ERROR: gson-2.8.9.jar not found in lib/ folder
    echo Please download from: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
    pause
    exit /b 1
)

cd src\main\java
java -cp ".;..\..\..\lib\Java-WebSocket-1.5.3.jar;..\..\..\lib\gson-2.8.9.jar;..\..\..\lib\slf4j-api-1.7.36.jar;..\..\..\lib\slf4j-simple-1.7.36.jar" com.chatapp.server.WebSocketBridgeServer
pause