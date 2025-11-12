# NetTalk Quiz System - Complete Workflow

## Overview
The quiz system now includes **localStorage persistence**, ensuring quizzes persist across page refreshes for both admins and users.

---

## Admin Workflow (admin.html)

### 1. **Create Quiz**
- Admin enters quiz name in "Create Quiz" section
- Clicks "Create Quiz" button
- System sends `/createquiz "Quiz Name"` command
- **Quiz is saved to localStorage** immediately
- Quiz appears in all dropdown menus

### 2. **Add Questions**
- Select quiz from "Add Question" dropdown
- Enter:
  - Question text
  - 4 options (A, B, C, D)
  - Correct answer (1-4)
  - Time limit (seconds)
- Click "Add Question"
- System sends `/addquestion quizId|question|opt1,opt2,opt3,opt4|correct|time`
- **Question count updated in localStorage**
- Quiz list refreshes automatically

### 3. **Invite Participants**
- Select quiz from "Invite Participants" dropdown
- Enter usernames (comma-separated) or "all" for everyone
- Click "Send Invitations"
- System sends `/invitequiz quizId username1,username2` or `/invitequiz quizId all`
- Invited users receive notifications

### 4. **Start Quiz**
- Select quiz from "Quiz Control" dropdown
- Click "Start Quiz"
- System sends `/startquiz quizId`
- **Quiz status updated to "RUNNING" in localStorage**
- All invited users can now join
- Quiz interface appears for participants

### 5. **End Quiz**
- Select running quiz from "Quiz Control" dropdown
- Click "End Quiz" (confirmation required)
- System sends `/endquiz quizId`
- **Quiz status updated to "ENDED" in localStorage**

---

## User Workflow (index.html)

### 1. **View Available Quizzes**
- Quizzes appear in left sidebar "Quiz Center"
- Shows quiz name, admin, questions count, and status
- **Quizzes loaded from localStorage on page load**
- Auto-refreshes when admins create new quizzes

### 2. **Receive Quiz Invitation**
- Notification appears in "Active Invitations" section
- Shows quiz name and inviting admin
- "Join Quiz" button available

### 3. **Join Quiz**
- Click "Join" on any available quiz
- System sends `/joinquiz quizId`
- **Quiz interface displays in main chat area**
- Welcome screen hides, quiz interface shows

### 4. **Take Quiz**
- Quiz interface shows:
  - **Quiz header** with name and instructions
  - **Question cards** with multiple choice options (A, B, C, D)
  - **Progress tracking** (X/Y questions answered)
  - **Submit button** (disabled until all answered)

### 5. **Answer Questions**
- Click any option to select (radio button behavior)
- Selected option highlights with:
  - Purple gradient background
  - Checkmark icon
  - "✓ Answered" status badge
- Can change answer before submitting

### 6. **Submit Quiz**
- Submit button becomes **green with pulse animation** when all questions answered
- Shows "Submit Quiz (5/5)" with answer count
- Click to submit all answers
- System sends `/answer quizId questionNum option` for each question
- **Success confirmation** displayed
- "Back to Chat" button returns to normal chat

### 7. **Cancel Quiz**
- Click "Cancel" button to exit without submitting
- Confirmation dialog appears
- Returns to chat interface

---

## localStorage Persistence

### Admin Panel Storage
**Key:** `adminQuizzes`
**Stored Data:**
```javascript
[
  {
    id: "abc123",
    name: "Math Quiz",
    admin: "AdminUser",
    status: "CREATED" | "RUNNING" | "ENDED",
    questions: 5,
    createdAt: "2025-11-11T10:30:00.000Z"
  }
]
```

### Client Storage
**Key:** `chatActiveQuizzes`
**Stored Data:**
```javascript
[
  {
    id: "abc123",
    name: "Math Quiz",
    admin: "AdminUser",
    status: "created" | "running" | "ended",
    questions: 5
  }
]
```

### Benefits
- ✅ **Quizzes persist across page refreshes**
- ✅ **Admin can continue managing quizzes after reconnecting**
- ✅ **Users see all available quizzes immediately**
- ✅ **No data loss on browser refresh**
- ✅ **Syncs with server on connection**

---

## Quiz Status Flow

```
CREATED → RUNNING → ENDED
   ↓         ↓         ↓
Users     Users     Quiz
can see   can join  closed
```

1. **CREATED**: Quiz exists, questions can be added, invitations can be sent
2. **RUNNING**: Quiz active, users can join and take quiz
3. **ENDED**: Quiz closed, no more participation allowed

---

## Commands Reference

### Admin Commands
- `/createquiz "Quiz Name"` - Create new quiz
- `/addquestion quizId|question|opt1,opt2,opt3,opt4|correct|time` - Add question
- `/invitequiz quizId username1,username2` - Invite specific users
- `/invitequiz quizId all` - Invite all online users
- `/startquiz quizId` - Start the quiz
- `/endquiz quizId` - End the quiz
- `/quizzes` - List all active quizzes

### User Commands
- `/quizzes` - View available quizzes
- `/joinquiz quizId` - Join a quiz
- `/answer quizId questionNum option` - Answer question (auto-sent on submit)

---

## Features

### Admin Features
- ✅ Create multiple quizzes
- ✅ Add unlimited questions per quiz
- ✅ Invite specific users or all users
- ✅ Start/stop quizzes
- ✅ Track quiz status (CREATED/RUNNING/ENDED)
- ✅ View quiz list with question counts
- ✅ Persistent storage across sessions

### User Features
- ✅ View all available quizzes in sidebar
- ✅ Receive quiz invitations
- ✅ Beautiful quiz interface with card layout
- ✅ Multiple choice selection with visual feedback
- ✅ Progress tracking
- ✅ Smart submit button (disabled until complete)
- ✅ Success confirmation after submission
- ✅ Persistent quiz availability

### UI/UX Features
- ✅ **Grid layout** for quizzes (responsive)
- ✅ **Color-coded status badges** (blue/red/gray)
- ✅ **Animated interactions** (hover, selection, submission)
- ✅ **Purple gradient theme** throughout
- ✅ **Welcome screen** by default
- ✅ **Three-column layout** (quizzes | chat | users)
- ✅ **Mobile responsive** design

---

## Technical Implementation

### Storage Sync Strategy
1. **On Page Load**: Load from localStorage
2. **On Connection**: Refresh from server
3. **On Create/Update**: Save to localStorage immediately
4. **On Server Response**: Merge server data with local data

### Data Flow
```
Admin Creates Quiz
    ↓
Saved to Admin localStorage
    ↓
Sent to Server via WebSocket
    ↓
Server broadcasts to all users
    ↓
Saved to User localStorage
    ↓
Displayed in Quiz Center
```

---

## Example Complete Workflow

### Scenario: Admin creates and runs a quiz

1. **Admin opens admin.html**
   - Previous quizzes load from localStorage
   
2. **Admin creates "Science Quiz"**
   - Enters name, clicks "Create Quiz"
   - Quiz saved to localStorage with ID "sci123"
   - Appears in all dropdowns

3. **Admin adds 3 questions**
   - Selects quiz, fills question form
   - Each question increments count
   - localStorage updated after each question

4. **Admin invites "john, sarah"**
   - Selects quiz, enters names
   - Server sends invitations
   - John and Sarah receive notifications

5. **Admin starts quiz**
   - Clicks "Start Quiz"
   - Status changes to "RUNNING"
   - localStorage updated

6. **John joins quiz**
   - Sees quiz in sidebar
   - Clicks "Join"
   - Quiz interface displays with 3 questions

7. **John takes quiz**
   - Selects options: A, C, B
   - Submit button turns green
   - Clicks submit
   - All answers sent to server

8. **John completes quiz**
   - Success screen shows
   - Returns to chat
   - Quiz remains in sidebar

9. **Page refresh**
   - ✅ Quiz still visible
   - ✅ Status preserved
   - ✅ Can continue managing

---

## Browser Support
- ✅ localStorage supported in all modern browsers
- ✅ Data persists until cleared by user
- ✅ Approximately 5-10MB storage available

## Future Enhancements
- Quiz results display
- Leaderboard
- Quiz analytics
- Question bank
- Quiz templates
- Time limits enforcement
- Auto-submit on timeout
