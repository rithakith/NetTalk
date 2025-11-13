# NetTalk Chat System - Setup Instructions

## Quick Start (Automated - Recommended)

### Option 1: Start Everything at Once
```bash
.\start-chat-system.bat
```
This will:
1. Compile all Java files
2. Start ChatServer (port 8888)
3. Start WebSocket Bridge (port 8889)
4. Open the web frontend in your browser

---

## Manual Setup (Your Friend's Instructions)

### Step 1: Start the Backend Server
```powershell
cd ChatSystem
.\start-server.bat
```
OR manually:
```powershell
cd ChatSystem/src/main/java
java -cp ".;..\..\..\lib\*" com.chatapp.server.ChatServer
```

### Step 2: Start WebSocket Bridge (for web frontend)
**Open another PowerShell window:**
```powershell
cd ChatSystem
.\run-websocket-server.bat
```

### Step 3: Connect Java Clients
**Open another PowerShell window for each client:**
```powershell
cd ChatSystem
.\start-client.bat
```
OR manually:
```powershell
cd ChatSystem/src/main/java
java -cp ".;..\..\..\lib\*" com.chatapp.client.ChatClient
```

You can open multiple clients by running this in multiple PowerShell windows.

### Step 4: Connect Web Frontend
Simply open `frontend/index.html` in your browser, or it will open automatically with `start-chat-system.bat`.

---

## Important Notes

### PowerShell Commands
When running `.bat` files in PowerShell, you MUST use `.\` prefix:
- ❌ `run-websocket-server.bat` - Won't work
- ✅ `.\run-websocket-server.bat` - Correct

### Port Usage
- **8888** - TCP Chat Server (for Java clients)
- **8889** - WebSocket Bridge Server (for web frontend)

### Required Libraries (Already Included)
All required JAR files are in the `lib/` folder:
- `Java-WebSocket-1.5.3.jar`
- `gson-2.8.9.jar`
- `slf4j-api-1.7.36.jar`
- `slf4j-simple-1.7.36.jar`

### Automatic Compilation
All batch files now automatically compile Java source files before running, so you don't need to manually compile.

---

## Features

✅ Real-time chat messaging
✅ Private messaging (`@username message`)
✅ Typing indicators
✅ Message status (sent ✓, delivered ✓✓, seen ✓✓)
✅ User management
✅ API integration
✅ WebSocket support for web frontend
✅ Local storage for message persistence

---

## Troubleshooting

### "Command not found" in PowerShell
Always use `.\` before batch file names in PowerShell:
```powershell
.\start-server.bat
.\run-websocket-server.bat
.\start-client.bat
```

### Connection Error
Make sure:
1. ChatServer (port 8888) is running first
2. WebSocket Bridge (port 8889) is running before opening frontend
3. No firewall blocking the ports

### Compilation Errors
Make sure you have JDK 8 or higher installed:
```powershell
java -version
javac -version
```

---

## File Structure

```
ChatSystem/
├── start-chat-system.bat    # Start everything (recommended)
├── start-server.bat          # Start ChatServer only
├── run-websocket-server.bat  # Start WebSocket Bridge
├── start-client.bat          # Start Java client
├── lib/                      # Required JAR libraries
├── frontend/                 # Web interface
│   ├── index.html
│   ├── app.js
│   └── style.css
└── src/main/java/           # Java source code
    └── com/chatapp/
        ├── server/          # Server components
        ├── client/          # Client components
        ├── common/          # Shared classes
        └── api/             # API integration
```
