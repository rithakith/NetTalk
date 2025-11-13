# 🔐 Admin Commands & Moderation System

## Overview
The ChatSystem now includes a comprehensive admin and moderation system with role-based access control. This feature allows designated administrators to manage users and maintain chat quality.

## ⭐ Features

- **Role-Based Access Control** - Automatic admin promotion for specific usernames
- **User Management** - Kick, ban, and manage problematic users
- **Moderation Tools** - Mute/unmute users to control message spam
- **Server Broadcasting** - Send important announcements to all users
- **Statistics Dashboard** - View real-time server statistics

## 🎯 Becoming an Admin

### Automatic Admin Promotion
Users with the following usernames are automatically promoted to admin:
- `admin`
- `Admin`
- `administrator`
- `Administrator`

### Manual Promotion (Programmatic)
Admins can be manually promoted in the `UserManager` class:
```java
userManager.promoteToAdmin("username");
```

## 📋 Admin Commands

### 1. `/kick <username>` - Disconnect User
**Purpose:** Temporarily disconnect a user from the server.

**Usage:**
```
/kick troublemaker
```

**Effect:**
- User is immediately disconnected
- User can reconnect with the same username
- Broadcast message sent to all users

**Use Case:** Quick response to minor disruptions

---

### 2. `/ban <username>` - Block User Permanently
**Purpose:** Permanently block a user from reconnecting to the server.

**Usage:**
```
/ban spammer123
```

**Effect:**
- User is kicked if currently online
- Username is added to banned list
- User cannot reconnect with that username
- Broadcast message sent to all users

**Use Case:** Dealing with repeat offenders or malicious users

---

### 3. `/mute <username>` - Prevent User from Sending Messages
**Purpose:** Prevent a user from sending chat messages while still allowing them to view chat.

**Usage:**
```
/mute annoying_user
```

**Effect:**
- User can still see messages
- User cannot send chat or private messages
- User is notified they are muted
- Mute status persists until unmuted

**Use Case:** Controlling spam or giving users a "timeout"

---

### 4. `/unmute <username>` - Restore User's Messaging Ability
**Purpose:** Remove mute restriction from a previously muted user.

**Usage:**
```
/unmute annoying_user
```

**Effect:**
- User can send messages again
- User is notified they are unmuted

---

### 5. `/broadcast <message>` - Admin Announcement
**Purpose:** Send an important message to all connected users.

**Usage:**
```
/broadcast Server maintenance in 10 minutes!
```

**Effect:**
- Message is sent to all online users
- Displayed with special formatting (📢 icon)
- Clearly marked as admin announcement

**Use Case:** Important server notifications, events, or warnings

---

### 6. `/stats` - Server Statistics
**Purpose:** View comprehensive server statistics and moderation data.

**Usage:**
```
/stats
```

**Displays:**
- Active user count
- Total messages sent
- List of online users
- List of administrators
- List of banned users
- List of muted users

**Use Case:** Monitoring server health and moderation status

---

## 🎨 Frontend Interface

### Admin Panel
When logged in as admin, a special "⭐ Admin Commands" section appears in the sidebar with buttons for:
- **Kick User** - Opens dialog to kick a user
- **Ban User** - Opens dialog to ban a user
- **Mute User** - Opens dialog to mute a user
- **Unmute User** - Opens dialog to unmute a user
- **Broadcast** - Opens dialog to send announcement
- **Server Stats** - Instantly displays server statistics

### Admin Detection
The system automatically detects admin status when:
1. User logs in with admin username
2. Server sends admin confirmation message
3. Admin panel becomes visible automatically

---

## 🔧 Technical Implementation

### Backend Architecture

#### 1. Message Types (Message.java)
```java
public enum MessageType {
    ADMIN_KICK,
    ADMIN_BAN,
    ADMIN_MUTE,
    ADMIN_UNMUTE,
    ADMIN_BROADCAST,
    ADMIN_STATS
}
```

#### 2. UserManager (UserManager.java)
Thread-safe data structures for moderation:
```java
private final ConcurrentHashMap<String, Boolean> adminUsers;
private final ConcurrentHashMap<String, Boolean> bannedUsers;
private final ConcurrentHashMap<String, Boolean> mutedUsers;
```

Key methods:
- `isAdmin(username)` - Check admin status
- `banUser(username)` - Ban a user
- `muteUser(username)` - Mute a user
- `isMuted(username)` - Check mute status
- `getBannedList()` - Get all banned users
- `getMutedList()` - Get all muted users

#### 3. ChatServer (ChatServer.java)
Admin command handlers:
- `handleAdminKick()` - Process kick command
- `handleAdminBan()` - Process ban command
- `handleAdminMute()` - Process mute command
- `handleAdminUnmute()` - Process unmute command
- `handleAdminBroadcast()` - Process broadcast command
- `handleAdminStats()` - Process stats command

#### 4. ClientHandler (ClientHandler.java)
Message validation:
- Checks if user is muted before broadcasting
- Routes admin commands to server handlers
- Enforces permission checks

### Frontend Implementation

#### JavaScript Functions (app.js)
- `showAdminPanel()` - Display admin controls
- `showAdminDialog(command)` - Show command input dialog
- `executeAdminCommand()` - Send admin command to server
- `sendAdminStats()` - Request server statistics
- `checkAdminStatus(msg)` - Detect admin promotion

#### UI Components (index.html)
- Admin command buttons section
- Modal dialog for command input
- Dynamic visibility based on admin status

#### Styling (style.css)
- `.btn-admin` - Styled admin buttons
- `.modal` - Dialog overlay
- `.modal-content` - Dialog box styling

---

## 🛡️ Security Considerations

### Permission Checks
- All admin commands verify user has admin permissions
- Non-admins receive error messages when attempting admin actions
- Server-side validation prevents client-side manipulation

### Automatic Admin Detection
```java
public boolean isAdmin(String username) {
    // Auto-promote specific usernames
    if (username.equalsIgnoreCase("admin") || 
        username.equalsIgnoreCase("administrator")) {
        adminUsers.putIfAbsent(username, true);
        return true;
    }
    return adminUsers.getOrDefault(username, false);
}
```

### Mute Enforcement
```java
// In ClientHandler.processMessage()
if (server.isUserMuted(username)) {
    sendMessage(new Message(MessageType.SYSTEM, "Server",
        "You are muted and cannot send messages."));
    return;
}
```

### Ban Enforcement
```java
// In ChatServer.registerClient()
if (userManager.isBanned(username)) {
    Message errorMsg = new Message(MessageType.SYSTEM, "Server",
        "You are banned from this server.");
    handler.sendMessage(errorMsg);
    handler.disconnect();
    return;
}
```

---

## 📊 Usage Examples

### Example 1: Handling a Spammer
```
Admin logs in as "admin"
User "spammer123" sends 10 messages rapidly

Admin actions:
1. /mute spammer123          (Stop the spam)
2. Wait to see if behavior improves
3. /unmute spammer123        (Give another chance)
4. If spam continues: /ban spammer123
```

### Example 2: Server Maintenance
```
Admin needs to restart server:

1. /broadcast Server will restart in 5 minutes. Please save your work!
2. Wait 5 minutes
3. /stats (Check how many users are online)
4. Perform maintenance
```

### Example 3: Managing Inappropriate Behavior
```
User "troublemaker" posts offensive content:

1. /kick troublemaker        (Immediate removal)
2. User tries to reconnect
3. /ban troublemaker         (Prevent reconnection)
```

---

## 🎯 Best Practices

### For Administrators
1. **Use Mute First** - Try muting before banning
2. **Communicate** - Use broadcast to explain actions
3. **Check Stats Regularly** - Monitor server health
4. **Be Fair** - Give warnings before permanent bans
5. **Log Actions** - Server automatically logs all admin actions

### For Server Operators
1. **Limit Admin Access** - Only promote trusted users
2. **Monitor Logs** - Review admin actions periodically
3. **Backup Ban List** - Keep records of banned users
4. **Clear Communication** - Establish moderation policies

---

## 🔍 Troubleshooting

### Admin Panel Not Showing
**Problem:** Logged in but no admin panel appears

**Solutions:**
1. Check username is "admin" or "administrator" (case-insensitive)
2. Reconnect to trigger admin detection
3. Check browser console for JavaScript errors
4. Verify WebSocket connection is established

### Commands Not Working
**Problem:** Admin commands have no effect

**Solutions:**
1. Verify you're connected to server
2. Check server console for error messages
3. Ensure target username exists and is spelled correctly
4. Check if user has already been kicked/banned

### Permission Denied Errors
**Problem:** "You don't have admin permissions" message

**Solutions:**
1. Verify you're logged in with admin username
2. Check server logs to confirm admin status
3. Try disconnecting and reconnecting
4. Contact server administrator for manual promotion

---

## 📈 Future Enhancements

Potential additions for future versions:
- [ ] Timed bans (ban expires after X hours/days)
- [ ] Warning system (3 warnings = auto-ban)
- [ ] Admin hierarchy (super admin, moderator, helper)
- [ ] Command history log visible to admins
- [ ] User report system for non-admins
- [ ] IP-based banning
- [ ] Chat logs export for admins
- [ ] Customizable admin commands
- [ ] Multi-level mute (mute from specific channels)
- [ ] Admin dashboard with charts and analytics

---

## 📚 Related Documentation

- [Main README](README.md) - Project overview
- [Setup Instructions](SETUP_INSTRUCTIONS.md) - Installation guide
- [Message Status System](MESSAGE_SEEN_STATUS_README.md) - Message tracking
- [Typing Indicators](TYPING_INDICATOR_README.md) - Real-time typing status

---

## 💡 Tips

### Quick Admin Login
For testing, simply use:
- Username: `admin`
- Connect to server
- Admin panel appears automatically

### Command Shortcuts
In future versions, keyboard shortcuts could be added:
- `Ctrl+K` - Quick kick
- `Ctrl+B` - Quick ban
- `Ctrl+M` - Quick mute
- `Ctrl+Alt+S` - View stats

### Integration with External Systems
The admin system can be extended to integrate with:
- Discord webhooks for notifications
- Database for persistent ban/mute lists
- Logging systems for audit trails
- External authentication systems

---

**Last Updated:** November 13, 2025
**Version:** 1.0.0
**Author:** NetTalk Chat System Team
