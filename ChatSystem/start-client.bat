@echo off
echo ============================================================
echo         Starting ChatClient (Java Client)
echo ============================================================
echo.
echo Connecting to: localhost:8888
echo.
echo NOTE: Make sure ChatServer is running before starting clients!
echo ============================================================
echo.

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
echo Detected Java version: %JAVA_VERSION%
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

echo [Step 2/2] Starting ChatClient...
java -cp ".;..\..\..\lib\*" com.chatapp.client.ChatClient
pause
