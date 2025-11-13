# 🚀 Quick Start - Testing Admin Commands

## Step 1: Start the Server
```powershell
cd ChatSystem
.\start-server.bat
```

Wait for: `[ChatServer] Server started successfully!`

## Step 2: Start WebSocket Bridge
Open a **new terminal**:
```powershell
cd ChatSystem
.\run-websocket-server.bat
```

Wait for: `WebSocket Bridge Server running on ws://localhost:8889`

## Step 3: Open Frontend (Admin User)
```powershell
.\open-frontend.bat
```

In the browser:
1. Username: **admin** (lowercase)
2. Server: localhost
3. Port: 8888
4. Click **Connect**

✅ You should see: **"⭐ You are logged in as ADMIN"**
✅ Admin Commands section appears in sidebar

## Step 4: Open Second Browser (Regular User)
Open another browser window or incognito mode:
1. Go to: `http://127.0.0.1:5500/frontend/index.html`
2. Username: **testuser**
3. Server: localhost
4. Port: 8888
5. Click **Connect**

## Step 5: Test Admin Commands

### Test 1: Mute User
**In Admin Window:**
1. Click **"Mute User"** button
2. Enter: `testuser`
3. Click **Execute**

**In testuser Window:**
- Try to send a message
- You should see: "You are muted and cannot send messages."

### Test 2: Unmute User
**In Admin Window:**
1. Click **"Unmute User"** button
2. Enter: `testuser`
3. Click **Execute**

**In testuser Window:**
- Try sending a message again
- It should work now!

### Test 3: Broadcast Message
**In Admin Window:**
1. Click **"Broadcast"** button
2. Enter: `Welcome everyone to the chat!`
3. Click **Execute**

**Both Windows:**
- Should see: "📢 ADMIN ANNOUNCEMENT from admin: Welcome everyone to the chat!"

### Test 4: Server Stats
**In Admin Window:**
1. Click **"Server Stats"** button

**Result:**
```
📊 SERVER STATISTICS
═══════════════════════
Active Users: 2
Total Messages: X
Online Users: admin, testuser
Admins: admin
Banned Users: None
Muted Users: None
═══════════════════════
```

### Test 5: Kick User
**In Admin Window:**
1. Click **"Kick User"** button
2. Enter: `testuser`
3. Click **Execute**

**In testuser Window:**
- User gets disconnected
- Can reconnect again

### Test 6: Ban User
**In Admin Window:**
1. Wait for testuser to reconnect
2. Click **"Ban User"** button
3. Enter: `testuser`
4. Click **Execute**

**In testuser Window:**
- Gets kicked immediately
- Try to reconnect
- Should see: "You are banned from this server."

## Expected Console Output

### Server Console
```
[UserManager] User 'admin' registered. Total users: 1
[UserManager] User 'admin' promoted to admin
[UserManager] User 'testuser' registered. Total users: 2
[Admin] admin muted testuser
[Admin] admin unmuted testuser
[Admin] admin broadcast: Welcome everyone to the chat!
[Admin] admin requested server statistics
[Admin] admin kicked testuser
[Admin] admin banned testuser
```

### WebSocket Console
```
[WebSocket] New connection from: /127.0.0.1:xxxxx
[WebSocket] Sent message: {"type":"SYSTEM",...}
[WebSocket] Received admin command: ADMIN_MUTE
[WebSocket] Received admin command: ADMIN_BROADCAST
```

## Troubleshooting

### Admin Panel Not Showing
- ✅ Check username is exactly "admin" (lowercase)
- ✅ Refresh browser page
- ✅ Check browser console (F12) for errors

### Commands Not Working
- ✅ Verify WebSocket connection (green indicator)
- ✅ Check server console for error messages
- ✅ Ensure target username is spelled correctly

### User Not Getting Kicked/Banned
- ✅ Verify target user is online
- ✅ Check server console for admin action logs
- ✅ Ensure you're logged in as admin

## Testing Checklist

- [ ] Admin login works
- [ ] Admin panel appears
- [ ] Mute command works
- [ ] Unmute command works
- [ ] Broadcast works
- [ ] Stats display correctly
- [ ] Kick disconnects user
- [ ] Ban prevents reconnection
- [ ] Non-admin cannot use commands
- [ ] Muted user cannot send messages

## Advanced Testing

### Test Multiple Admins
1. Open 3rd browser window
2. Username: `administrator`
3. Should also get admin status
4. Both admins can moderate

### Test Permission Denial
1. Open browser as "normaluser"
2. Open browser console (F12)
3. Try to send admin command manually:
```javascript
ws.send(JSON.stringify({
    type: 'ADMIN_KICK',
    sender: 'normaluser',
    target: 'testuser'
}));
```
4. Should see: "⛔ You don't have admin permissions"

### Stress Test
1. Connect 5+ regular users
2. Use admin to manage all
3. Check stats show correct counts
4. Mute multiple users
5. Verify all muted users can't send messages

## Demo Script

**For Presentation:**
1. Show normal chat between 2 users
2. User starts spamming → Admin mutes them
3. Spammer can't send messages anymore
4. Admin broadcasts announcement
5. Show server stats
6. Unmute the user
7. User continues normally
8. If needed → Admin kicks troublemaker
9. If persists → Admin bans permanently

**Key Points to Highlight:**
- ⭐ Role-based access control
- 🔒 Real-world moderation features
- 🎯 Interactive admin interface
- 📊 Real-time statistics
- 🛡️ Security enforcement (server-side validation)

---

**Time Required:** 5-10 minutes for full test
**Browsers Needed:** 2-3 windows
**Difficulty:** Easy
