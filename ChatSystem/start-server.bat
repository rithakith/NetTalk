@echo off
echo ===============================================
echo   NetTalk Chat System - Core Server
echo ===============================================
echo.
echo Port: 8888
echo Features: Chat, Admin Commands, Quiz System
echo.

cd /d "%~dp0"

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
echo Detected Java version: %JAVA_VERSION%
echo.

echo ===============================================
echo  Admin Commands Available:
echo ===============================================
echo  /kick ^<username^> - Kick user from server
echo  /ban ^<username^> - Ban user permanently
echo  /mute ^<username^> - Mute user
echo  /unmute ^<username^> - Unmute user
echo  /broadcast ^<message^> - Send message to all
echo  /stats - Show server statistics
echo.
echo  Quiz Commands Available:
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

echo [Step 1/2] Compiling Java source files...
cd src\main\java
javac -encoding UTF-8 -cp ".;..\..\..\lib\*" com\chatapp\server\*.java com\chatapp\common\*.java com\chatapp\client\*.java com\chatapp\api\*.java 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Please check your Java installation.
    echo [HINT] Make sure Java 21 is installed and JAVA_HOME is set correctly.
    cd ..\..\..
    pause
    exit /b 1
)
echo [SUCCESS] Compilation completed!
echo.

echo [Step 2/2] Starting ChatServer on port 8888...
echo Connect using ChatClient or telnet localhost 8888
echo.
java -cp ".;..\..\..\lib\*" com.chatapp.server.ChatServer
pause
