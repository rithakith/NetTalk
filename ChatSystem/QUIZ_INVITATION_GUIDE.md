# Quiz Invitation System - Quick Start Guide

## How to Use the New Quiz Features

### For Regular Users (Client Side)

#### 1. Connect to Chat
1. Open `frontend/index.html` in your browser
2. Enter your username
3. Click "Connect"
4. The quiz list will automatically refresh

#### 2. View Available Quizzes
- Look at the left sidebar under **"🎯 Available Quizzes"**
- You'll see all quizzes you can join
- Click the refresh button (🔄) to update the list

#### 3. Receive Quiz Invitation
When an admin invites you:
- A notification panel will appear at the top (with orange gradient)
- The quiz will show with an **"INVITATION"** badge
- It will pulse to get your attention
- You'll also see it in the sidebar

#### 4. Join a Quiz
Two ways to join:
- **From notification panel:** Click "Join Quiz" button
- **From sidebar:** Click "Join" button next to the quiz

#### 5. View Quiz Details
- Click "View Details" or "Details" button
- Server will send quiz information to chat

### For Admins

#### How to Invite Users to a Quiz

1. **Open Admin Panel**
   - Go to `frontend/admin.html`
   - Connect as admin

2. **Create a Quiz**
   ```
   /createquiz "Math Challenge"
   ```
   - Note the Quiz ID from the response

3. **Invite Specific Users**
   - In the "👥 Invite Participants" section
   - Select the quiz from dropdown
   - Enter usernames separated by commas
   - Click "Send Invitations"

   **Example:**
   ```
   Participants: Alice,Bob,Charlie,Diana
   ```

   Or use command directly:
   ```
   /invitequiz abc123 Alice,Bob,Charlie
   ```

4. **What Happens:**
   - Each invited user receives a notification
   - They see the quiz in their Available Quizzes list
   - They can join with one click

### Example Workflow

#### Scenario: Admin creates a quiz and invites students

1. **Admin (TeacherJohn):**
   ```
   /createquiz "Science Quiz"
   ```
   Response: `Quiz 'Science Quiz' created successfully! Quiz ID: sci001`

2. **Admin adds questions:**
   ```
   /addquestion sci001|What is H2O?|Water,Oxygen,Hydrogen,Carbon|0|30
   ```

3. **Admin invites students:**
   ```
   /invitequiz sci001 Alice,Bob,Charlie
   ```

4. **Students (Alice, Bob, Charlie):**
   - See notification panel appear
   - Quiz shows: "🎯 Science Quiz" with "INVITATION" badge
   - See "Invited by: TeacherJohn"
   - Click "Join Quiz"

5. **Admin starts quiz:**
   ```
   /startquiz sci001
   ```

6. **Students see:**
   - Quiz status changes to "IN PROGRESS"
   - Questions appear in chat
   - Can answer with `/answer sci001 1`

## Visual Indicators

### Quiz Status Colors

| Status | Color | Badge Text | Meaning |
|--------|-------|------------|---------|
| Invitation | Orange | INVITATION | You're specifically invited |
| Created | Blue | AVAILABLE | Open to join |
| Running | Red | IN PROGRESS | Quiz is active |
| Completed | Gray | COMPLETED | Quiz has ended |

### Animations

- **Pulse**: Quiz invitation badges pulse to draw attention
- **Slide In**: New notifications slide in from top
- **Fade Out**: Notifications fade when joining
- **Hover Effects**: Buttons lift slightly on hover

## Testing the Feature

### Test Case 1: Quiz Invitation Flow

**Admin Terminal:**
```
1. Connect as "Admin"
2. /createquiz "Test Quiz"
   → Note the Quiz ID (e.g., "test001")
3. /invitequiz test001 Alice,Bob
```

**User Terminal (Alice):**
```
1. Connect as "Alice"
2. Should see quiz notification panel appear
3. Should see "Test Quiz" with "INVITATION" badge
4. Click "Join Quiz"
5. Should see confirmation in chat
```

### Test Case 2: Browse Available Quizzes

**User Terminal:**
```
1. Connect to server
2. Type: /quizzes
3. Check sidebar "Available Quizzes" section
4. Should see list of all active quizzes
5. Click refresh button (🔄) to update
```

### Test Case 3: Multiple Invitations

**Admin Terminal:**
```
1. /createquiz "Quiz 1"
2. /createquiz "Quiz 2"
3. /createquiz "Quiz 3"
4. /invitequiz quiz1_id Alice
5. /invitequiz quiz2_id Alice
6. /invitequiz quiz3_id Alice
```

**User Terminal (Alice):**
```
1. Should see 3 quiz notifications
2. All should show in sidebar
3. Can join any quiz
4. Notifications stack nicely
```

## Keyboard Commands

| Command | Description |
|---------|-------------|
| `/quizzes` | Show all active quizzes |
| `/joinquiz <id>` | Join a quiz |
| `/answer <id> <option>` | Submit answer (1-4) |
| `/quizinfo <id>` | Get quiz details |

## Troubleshooting

### Quiz not appearing?
- Click refresh button (🔄) in sidebar
- Type `/quizzes` to manually request list
- Check connection status (should be 🟢)

### Can't join quiz?
- Make sure you're connected to server
- Check if quiz is still active
- Try typing `/joinquiz <quiz_id>` manually

### Notifications not showing?
- Check if notification panel is hidden (click to toggle)
- Refresh page and reconnect
- Make sure WebSocket connection is stable

## Files Overview

### Modified Files

1. **frontend/index.html**
   - Added quiz notification panel
   - Added available quizzes section in sidebar

2. **frontend/app.js**
   - Quiz state management
   - Quiz message handlers
   - UI update functions

3. **frontend/style.css**
   - Quiz panel styles
   - Status badges
   - Animations

### New Documentation

- **QUIZ_CLIENT_FEATURES.md**: Detailed feature documentation
- **QUIZ_INVITATION_GUIDE.md**: This quick start guide

## Next Steps

1. Start the servers:
   ```
   cd D:\NetTalk\ChatSystem
   .\start-complete-system.bat
   ```

2. Open admin panel:
   ```
   Open: frontend/admin.html
   Connect as: Admin
   ```

3. Open client interface:
   ```
   Open: frontend/index.html
   Connect as: Alice (or any username)
   ```

4. Create and test quiz invitations!

## Support

For issues or questions:
- Check console (F12) for errors
- Verify all servers are running (ports 8888, 8889, 8890)
- Review QUIZ_CLIENT_FEATURES.md for detailed implementation

---

**Enjoy the new quiz invitation system! 🎯**
