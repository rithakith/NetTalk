# Quick Test Guide - Quiz System

## 🚀 Quick Start (5 Steps)

### 1️⃣ Start System
```powershell
cd D:\NetTalk\ChatSystem
.\start-complete-system.bat
```
**Wait for**: "WebSocket server started on port 8889"

---

### 2️⃣ Admin Setup
1. Open `frontend/admin.html` in browser
2. Login as **Admin**
3. Create quiz: "Network Quiz"
4. Add 2-3 questions with options A, B, C, D
5. Invite: Type **"all"** or username
6. Note the Quiz ID shown

---

### 3️⃣ User Join
1. Open `frontend/index.html` in NEW TAB
2. Login as **"sachini"** (or any name)
3. See invitation in left sidebar
4. Click **"Join"** button
5. ✅ **Should See**: Waiting screen with quiz ID

---

### 4️⃣ Start Quiz
1. Back to **Admin** tab
2. Click "Manage Quiz"
3. Click **"Start Quiz"**
4. Countdown: 5 seconds

---

### 5️⃣ Answer & Submit
1. Back to **User** tab
2. ✅ **Should See**: Questions with A, B, C, D options
3. Select answers by clicking radio buttons
4. Click **"Submit Quiz"**
5. ✅ **Should See**: Results with leaderboard 🥇

---

## 🐛 Troubleshooting

### Issue: Questions Not Showing
**Check**:
1. Press F12 → Console tab
2. Look for: `[Quiz] Received question:`
3. If missing → Server not sending questions

**Fix**: 
- Restart servers
- Ensure quiz has questions added before starting

---

### Issue: Can't Join Quiz
**Check**:
1. Quiz state must be **CREATED** or **READY**
2. Quiz must have questions

**Fix**:
- Add questions before inviting
- Don't start quiz before users join

---

### Issue: Submit Button Disabled
**Check**:
1. All questions must be answered
2. Look for green checkmarks

**Fix**:
- Click radio button for each question
- Wait for "✓ Answered" status

---

## 📊 Console Output (Expected)

### Server Console (ChatServer)
```
[QuizManager] Quiz created: Quiz ID: ac20cade
[QuizManager] Question added to quiz ac20cade
[QuizManager] Invitations sent for quiz: ac20cade
[QuizEvents] Quiz started: ac20cade
[QuizEvents] New question for quiz: ac20cade
[QuizEvents] Question sent: Question 1: What is...? Options: A) Opt1, B) Opt2...
```

### Browser Console (F12)
```
[Quiz] Parsing quiz start message: Quiz 'Network Quiz' has started!
[Quiz] Received question: Question 1: What is...? Options: A) Opt1, B) Opt2, C) Opt3, D) Opt4
[Quiz] Parsed options: [{letter: "A", text: "Opt1"}, ...]
[Quiz] Updating interface with 1 questions
[Quiz] Questions rendered successfully
[Quiz] Selected option B for question 1
[Quiz] Stored answer index: 1
```

---

## ✅ Verification Checklist

- [ ] Servers start without errors
- [ ] Admin can create quiz
- [ ] Admin can add questions
- [ ] User receives invitation
- [ ] User joins successfully
- [ ] Waiting screen appears
- [ ] Quiz starts after countdown
- [ ] **Questions display with A, B, C, D options** ⭐
- [ ] Radio buttons work
- [ ] Selected options are highlighted
- [ ] Submit button enables when all answered
- [ ] Results display with rankings

---

## 🎯 Key Changes Made

### Server Side (ChatServer.java)
**Before**:
```java
String questionMsg = "Question 1:\nWhat is...?\n1. Option A\n2. Option B";
```

**After**:
```java
String questionMsg = "Question 1: What is...? Options: A) Option A, B) Option B, C) Option C, D) Option D";
```

### Client Side (app.js)
**Enhanced**:
- Added console logging for debugging
- Better error handling
- Improved regex parsing
- Visual feedback for answer selection

---

## 📱 Expected UI Flow

```
User Tab                          Admin Tab
=========                         ==========

1. Login Screen                   1. Login as Admin
   ↓                                 ↓
2. Chat Interface                 2. Create Quiz Form
   ↓                                 ↓
3. Invitation in Quiz Center      3. Add Questions (A,B,C,D)
   ↓                                 ↓
4. Click "Join"                   4. Invite Users ("all")
   ↓                                 ↓
5. ⏳ Waiting Screen               5. Start Quiz Button
   "Waiting for admin..."            ↓
   ↓                              6. Quiz Started (countdown)
6. 🚀 Countdown (5s)
   ↓
7. 📝 Quiz Interface
   Question 1: [Text]?
   ○ A) Option 1
   ○ B) Option 2
   ○ C) Option 3
   ○ D) Option 4
   [Question answered: ✓]
   
   Question 2: [Text]?
   ○ A) Option 1
   ● B) Option 2  ← Selected
   ○ C) Option 3
   ○ D) Option 4
   
   [📤 Submit Quiz] [❌ Cancel]
   ↓
8. 🏆 Results Screen
   ┌─────────────────────┐
   │  Quiz Results       │
   ├─────────────────────┤
   │ 🥇 User1 - 150 pts │
   │ 🥈 sachini (You) 120│
   │ 🥉 User3 - 90 pts  │
   └─────────────────────┘
```

---

## 🔧 Network Programming Concepts Demonstrated

1. **Sockets** → ChatServer.java (TCP on port 8888)
2. **NIO** → MessageBroadcaster.java (Selector, non-blocking)
3. **Multi-threading** → ExecutorService (10 thread pool)
4. **Client-Server** → WebSocketBridge ↔ ChatServer
5. **WebSocket** → Real-time quiz updates (port 8889)

---

## 💡 Pro Tips

- Open browser console (F12) for debugging
- Test with 2-3 browser tabs as different users
- Create simple quiz first (1-2 questions)
- Use meaningful question text for clarity
- Start quiz only after users join

---

**Ready to test? Start with Step 1️⃣ above!**
