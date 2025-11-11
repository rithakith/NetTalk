@echo off
echo ===============================================
echo   NetTalk Quiz System - Core Server
echo ===============================================

cd /d "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"

echo.
echo Starting ChatServer with Quiz System on port 8888...
echo.
echo ===============================================
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
echo Connect using ChatClient or telnet localhost 8888
echo.

java com.chatapp.server.ChatServer