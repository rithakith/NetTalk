@echo off
echo ===============================================
echo   NetTalk Quiz System - Integration Test
echo ===============================================

echo Starting ChatServer with Quiz System...
cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"

echo.
echo Building Java project...
javac -d . -cp "." src/main/java/com/chatapp/*/*.java src/main/java/com/chatapp/*/*/*.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo Starting ChatServer on port 8888...
echo.
echo ===============================================
echo  Quiz System Features Available:
echo ===============================================
echo  /createquiz ^<name^> - Create new quiz
echo  /addquestion quiz_id^|question^|opt1,opt2,opt3,opt4^|correct^|time
echo  /invitequiz quiz_id user1,user2 - Invite users  
echo  /startquiz quiz_id - Start quiz (admin only)
echo  /joinquiz quiz_id - Join existing quiz
echo  /answer quiz_id option_number - Submit answer
echo  /quizzes - List active quizzes
echo  /help - Show all commands
echo ===============================================
echo.

start "WebSocket Bridge" cmd /k "cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem" && timeout 3 && java com.chatapp.server.WebSocketBridgeServer"

java com.chatapp.server.ChatServer