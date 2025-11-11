# 🚀 NetTalk Quiz System - Complete Setup Guide

## Prerequisites
- Java JDK 8 or higher installed
- Windows Command Prompt or PowerShell
- Project files in: `c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem`

## 📋 Step-by-Step Setup Instructions

### 1. **Navigate to Project Directory**
```powershell
cd "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"
```

### 2. **Compile the Java Project**
```powershell
javac -d . src/main/java/com/chatapp/common/*.java src/main/java/com/chatapp/server/ChatServer.java src/main/java/com/chatapp/server/ClientHandler.java src/main/java/com/chatapp/server/UserManager.java src/main/java/com/chatapp/server/ConnectionListener.java src/main/java/com/chatapp/server/MessageBroadcaster.java src/main/java/com/chatapp/server/MessageTracker.java src/main/java/com/chatapp/client/*.java src/main/java/com/chatapp/api/*.java src/main/java/com/chatapp/quiz/*.java
```

### 3. **Start the Server**
**Option A: Using the batch file**
```powershell
.\start-server.bat
```

**Option B: Direct command**
```powershell
java com.chatapp.server.ChatServer
```

You should see output like:
```
============================================================
ChatSystem - Network Programming Assignment  
Demonstrating 5 Network Programming Concepts
============================================================

[ChatServer] Starting server on port 8888...
[ChatServer] Server started successfully!
[ChatServer] Ready to accept connections...
```

### 4. **Connect Clients (Open New Terminal Windows)**

**For each client, open a NEW PowerShell window and run:**
```powershell
cd "c:\PERSONAL\Projects\Network Project\NetTalk\ChatSystem"
java com.chatapp.client.ChatClient
```

**When prompted:**
- Enter a username (e.g., "Alice", "Bob", "Admin")
- Start typing messages or quiz commands

## 🎮 Testing the Quiz System

### Create and Run a Quiz Session:

**1. Admin creates a quiz:**
```
/createquiz "General Knowledge Quiz"
```

**2. Admin adds questions:**
```
/addquestion quiz_id|What is 2+2?|1,2,3,4|2|30
/addquestion quiz_id|Capital of France?|London,Paris,Berlin,Rome|1|25
```
*(Replace quiz_id with the actual ID shown after creating the quiz)*

**3. Admin invites users:**
```
/invitequiz quiz_id Alice,Bob
```

**4. Users join the quiz:**
```
/joinquiz quiz_id
```

**5. Admin starts the quiz:**
```
/startquiz quiz_id
```

**6. Users submit answers:**
```
/answer quiz_id 2
```

### Other Useful Commands:
- `/help` - Show all available commands
- `/quizzes` - List all active quizzes
- Regular chat messages work normally alongside quiz commands

## 🔧 Alternative Connection Methods

### Using Telnet (for testing):
```powershell
telnet localhost 8888
```
*(May need to enable telnet client in Windows Features)*

### Using Java Client Programmatically:
The `ChatClient.java` provides a console-based interface for connecting to the server.

## 📱 Web Interface (Optional - Requires Dependencies)

To use the web interface (`frontend/index.html`), you'll need:
1. WebSocket library (java-websocket)
2. Gson library for JSON processing
3. Run the `WebSocketBridgeServer.java`

For now, use the console clients to test the core functionality.

## 🐛 Troubleshooting

### "Class not found" errors:
- Ensure you've compiled all Java files
- Check you're in the correct directory
- Verify classpath is set correctly

### "Connection refused":
- Make sure the server is running on port 8888
- Check if another application is using port 8888
- Try restarting the server

### Quiz commands not working:
- Ensure you're using the exact command format
- Check that you have the correct quiz ID
- Verify you have admin privileges for admin-only commands

## 🎯 Expected Results

When everything is working correctly:
- Multiple clients can connect simultaneously
- Chat messages are broadcast to all connected users  
- Quiz commands are processed and appropriate responses are sent
- Quiz events are broadcast to participants in real-time
- Quiz state management works across multiple concurrent sessions

## 📊 Example Complete Quiz Flow

```
Terminal 1 (Admin):
> java com.chatapp.client.ChatClient
> Enter username: QuizMaster
> /createquiz "Monday Quiz"
> /addquestion quiz123|What is Java?|Language,Coffee,Island,All|3|30
> /invitequiz quiz123 Player1,Player2  
> /startquiz quiz123

Terminal 2 (Player1):
> java com.chatapp.client.ChatClient  
> Enter username: Player1
> /joinquiz quiz123
> /answer quiz123 3

Terminal 3 (Player2):
> java com.chatapp.client.ChatClient
> Enter username: Player2  
> /joinquiz quiz123
> /answer quiz123 2
```

The quiz system is now fully functional and ready for interactive multiplayer quiz sessions! 🎊