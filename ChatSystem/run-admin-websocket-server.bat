@echo off
echo Starting Admin WebSocket Server on port 8890...
echo.
echo Features included:
echo - Dedicated admin interface
echo - Enhanced quiz management
echo - Separate admin authentication
echo.
echo NOTE: This requires Java-WebSocket and Gson libraries in the lib/ folder
echo       Make sure ChatServer is running on port 8888 first!
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

REM Compile if needed
if not exist out\com\chatapp\server\AdminWebSocketServer.class (
    echo Compiling Java files...
    javac -cp "lib\*" -d out src\main\java\com\chatapp\api\ExternalApiClient.java src\main\java\com\chatapp\client\ChatClient.java src\main\java\com\chatapp\common\Message.java src\main\java\com\chatapp\quiz\Quiz.java src\main\java\com\chatapp\quiz\QuizManager.java src\main\java\com\chatapp\quiz\QuizParticipant.java src\main\java\com\chatapp\quiz\QuizQuestion.java src\main\java\com\chatapp\quiz\QuizResult.java src\main\java\com\chatapp\server\AdminWebSocketServer.java src\main\java\com\chatapp\server\ChatServer.java src\main\java\com\chatapp\server\ClientHandler.java src\main\java\com\chatapp\server\ConnectionListener.java src\main\java\com\chatapp\server\MessageBroadcaster.java src\main\java\com\chatapp\server\MessageTracker.java src\main\java\com\chatapp\server\SimpleWebServer.java src\main\java\com\chatapp\server\UserManager.java src\main\java\com\chatapp\server\WebSocketBridgeServer.java
)

java -cp "out;lib\Java-WebSocket-1.5.3.jar;lib\gson-2.8.9.jar;lib\slf4j-api-1.7.36.jar;lib\slf4j-simple-1.7.36.jar" com.chatapp.server.AdminWebSocketServer
pause