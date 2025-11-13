# NetTalk Quiz System - Port Configuration 🚀

## Port Structure Overview

### **🌐 Port Assignments:**

| Port | Service | Purpose | Interface |
|------|---------|---------|-----------|
| **8888** | ChatServer (TCP) | Core chat server | Java clients, Internal communication |
| **8889** | WebSocket Bridge | Regular user interface | `index.html`, Web users |
| **8890** | Admin WebSocket | Admin management | `admin.html`, Quiz administrators |

---

## **🎯 How Each Port Works:**

### **Port 8888 - ChatServer (TCP)**
- **Core Java server** with quiz system
- Handles all chat and quiz logic
- Direct TCP connections for Java clients
- Command: `java com.chatapp.server.ChatServer`

### **Port 8889 - WebSocket Bridge (Regular Users)**
- **Web interface** for regular users
- Real-time chat and quiz participation  
- Access via `frontend/index.html`
- Command: `.\run-websocket-server.bat`

### **Port 8890 - Admin WebSocket (Administrators)**  
- **Dedicated admin interface** for quiz management
- Enhanced admin controls and monitoring
- Access via `frontend/admin.html`
- Command: `.\run-admin-websocket-server.bat`

---

## **🚀 Complete Startup Commands:**

### **Auto-Startup (Recommended):**
```bash
# Starts all 3 servers + opens web interfaces
.\start-complete-system.bat
```

### **Manual Startup:**
```bash
# Terminal 1: Core Server
java com.chatapp.server.ChatServer

# Terminal 2: Regular WebSocket (8889) 
.\run-websocket-server.bat

# Terminal 3: Admin WebSocket (8890)
.\run-admin-websocket-server.bat
```

---

## **🎮 Access Points:**

### **Regular Users:**
- **Web**: Open `frontend/index.html` → Connect to **localhost:8889**
- **Java**: `java com.chatapp.client.ChatClient` → Connects to **8888**

### **Quiz Administrators:**
- **Admin Panel**: Open `frontend/admin.html` → Connect to **localhost:8890**
- **Enhanced Features**: 
  - Visual quiz creation
  - Participant management
  - Real-time monitoring
  - Admin console

---

## **🔧 Benefits of Separate Admin Port:**

✅ **Enhanced Security**: Admin functions isolated from regular users  
✅ **Better Performance**: Dedicated connection for admin operations  
✅ **Advanced Features**: Admin-specific functionality and monitoring  
✅ **Easy Management**: Clear separation of user vs admin interfaces  
✅ **Scalability**: Independent admin operations don't affect user experience

---

## **🎯 Quiz Commands by Interface:**

### **Web Interface (Port 8889):**
```
/createquiz "My Quiz"
/joinquiz quiz123  
/answer quiz123 2
```

### **Admin Panel (Port 8890):**
- **Visual Forms**: Point-and-click quiz creation
- **Bulk Operations**: Invite multiple users at once
- **Live Monitoring**: Real-time quiz status
- **Console Commands**: All quiz management commands

### **Java Client (Port 8888):**
```
/createquiz "Console Quiz"
/addquestion quiz123|Question|opt1,opt2,opt3,opt4|1|30
/invitequiz quiz123 user1,user2
/startquiz quiz123
```

---

## **Summary:**
Your NetTalk Quiz System now has **dedicated admin port 8890** for enhanced quiz management, while regular users continue using **port 8889** for chat and quiz participation. The admin interface provides a comprehensive dashboard for creating, managing, and monitoring quiz sessions! 🎊