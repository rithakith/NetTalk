# Quiz System Implementation - Network Programming Concepts

## Overview
This quiz system demonstrates **5 core network programming concepts** as required for the assignment:

### 1. **Sockets (TCP/IP Communication)**
- **Location**: `ChatServer.java`, `ConnectionListener.java`
- **Implementation**: 
  - Server listens on port **8888** using `ServerSocket`
  - Accepts client connections via `socket.accept()`
  - Each client gets a dedicated `Socket` for bidirectional communication
  ```java
  ServerSocket serverSocket = new ServerSocket(PORT);
  Socket clientSocket = serverSocket.accept();
  ```

### 2. **NIO (Non-blocking I/O)**
- **Location**: `MessageBroadcaster.java`
- **Implementation**:
  - Uses `Selector` for monitoring multiple channels
  - `SocketChannel` in non-blocking mode
  - `SelectionKey` for event-driven I/O
  ```java
  Selector selector = Selector.open();
  channel.configureBlocking(false);
  channel.register(selector, SelectionKey.OP_WRITE);
  ```

### 3. **Multi-threading**
- **Location**: `ChatServer.java`, `ClientHandler.java`
- **Implementation**:
  - `ExecutorService` with fixed thread pool (10 threads)
  - Each client handled by separate thread via `ClientHandler`
  - Quiz countdown and question timing use `ScheduledExecutorService`
  ```java
  ExecutorService clientExecutor = Executors.newFixedThreadPool(10);
  clientExecutor.submit(new ClientHandler(socket, this));
  ```

### 4. **Client-Server Communication**
- **Location**: All components
- **Protocol**: Custom message protocol with JSON-like structure
- **Message Types**:
  - `CHAT` - Regular chat messages
  - `QUIZ_INVITATION` - Quiz invitations
  - `QUIZ_START` - Quiz begins
  - `QUIZ_QUESTION` - Question delivery
  - `QUIZ_RESULTS` - Results broadcast
  - `SYSTEM` - System notifications

### 5. **WebSocket (Real-time Bidirectional Communication)**
- **Location**: `WebSocketBridgeServer.java`
- **Implementation**:
  - WebSocket server on port **8889**
  - Bridges WebSocket (frontend) ↔ TCP Socket (backend)
  - Real-time quiz updates without polling
  ```java
  WebSocketServer on port 8889
  Handles web clients for admin panel and user interface
  ```

---

## Quiz Flow Architecture

### Phase 1: Quiz Creation (Admin)
1. Admin creates quiz via admin panel
2. Admin adds questions with options (A, B, C, D)
3. Admin sends invitations to users
4. **Quiz State**: `CREATED` → `READY`

### Phase 2: User Joins
1. User receives invitation via `QUIZ_INVITATION` message
2. User clicks "Join Quiz" button
3. Server adds user to quiz participants
4. User sees **waiting screen** with quiz ID
5. **Quiz State**: `READY`

### Phase 3: Quiz Start
1. Admin clicks "Start Quiz"
2. Server broadcasts countdown (5 seconds)
3. Server transitions to `ACTIVE` state
4. Server broadcasts `QUIZ_START` message
5. **Quiz State**: `READY` → `COUNTDOWN` → `ACTIVE`

### Phase 4: Question Delivery (FIXED)
**Previous Issue**: Server sent questions in wrong format
```
Old Format:
Question 1:
What is...?
1. Option A
2. Option B
```

**Fixed Format**:
```
Question 1: What is...? Options: A) Option A, B) Option B, C) Option C, D) Option D
```

**Server Code** (`ChatServer.java` line 86-101):
```java
public void onQuizQuestionChanged(Quiz quiz, QuizQuestion question) {
    String questionMsg = "Question " + (quiz.getCurrentQuestionIndex() + 1) + ": " +
                       question.getQuestionText() + "? Options: ";
    
    String[] options = question.getOptions();
    char[] letters = {'A', 'B', 'C', 'D'};
    for (int i = 0; i < options.length && i < 4; i++) {
        if (i > 0) questionMsg += ", ";
        questionMsg += letters[i] + ") " + options[i];
    }
    
    Message qMsg = new Message(Message.MessageType.QUIZ_QUESTION, "Server", questionMsg);
    broadcastToParticipants(quiz, qMsg);
}
```

### Phase 5: Answer Submission
1. User selects option (A/B/C/D) for each question
2. Client converts letter to index (A=0, B=1, C=2, D=3)
3. User clicks "Submit Quiz"
4. Client sends `/answer <quiz_id> <index>` for each answer
5. Server validates and scores answers

**Client Code** (`app.js` line 1745-1752):
```javascript
function selectQuizOption(questionNumber, optionLetter) {
    // Convert letter (A-D) to index (0-3)
    const answerIndex = optionLetter.charCodeAt(0) - 'A'.charCodeAt(0);
    quizAnswers.set(questionNumber, answerIndex);
}
```

### Phase 6: Results Display
1. Quiz completes (all questions answered or time expires)
2. Server calculates scores with time bonus
3. Server broadcasts `QUIZ_RESULTS` with leaderboard
4. Client displays results with medals (🥇🥈🥉)
5. **Quiz State**: `ACTIVE` → `COMPLETED`

---

## Fixed Issues

### ✅ Issue 1: Questions Not Displaying
**Problem**: Frontend couldn't parse server's question format
**Root Cause**: Mismatch between server format and client regex pattern
**Solution**: 
- Updated `ChatServer.java` line 86-101 to send correct format
- Enhanced `addQuestionToQuiz()` in `app.js` with better parsing
- Added console logging for debugging

### ✅ Issue 2: Answer Format Confusion
**Problem**: Client wasn't sure whether to send letter or index
**Root Cause**: Documentation mismatch
**Solution**: 
- Client stores index (0-3) internally
- Sends `/answer <quiz_id> <index>` to server
- Server expects 0-based index for validation

### ✅ Issue 3: Quiz Interface Not Showing
**Problem**: Quiz interface container was empty
**Root Cause**: `updateQuizInterface()` returned early if no questions
**Solution**:
- Added comprehensive logging
- Enhanced error handling
- Questions now display immediately when received

---

## Message Flow Diagram

```
Admin Panel (admin.html)          ChatServer (8888)          WebSocketBridge (8889)          Client (app.js)
      |                                  |                           |                             |
      |--[Create Quiz]----------------->|                           |                             |
      |<----[Quiz Created: ID]-----------|                           |                             |
      |                                  |                           |                             |
      |--[Add Question A,B,C,D]-------->|                           |                             |
      |--[Invite User]----------------->|                           |                             |
      |                                  |----[QUIZ_INVITATION]----->|----[WebSocket]------------>|
      |                                  |                           |                             |
      |                                  |                           |<----[Join Quiz]-------------|
      |                                  |<----[Join Request]--------|                             |
      |                                  |----[Join Confirmed]------>|----[Waiting Screen]------->|
      |                                  |                           |                             |
      |--[Start Quiz]------------------>|                           |                             |
      |                                  |----[Countdown 5s]-------->|----[Countdown]------------>|
      |                                  |----[QUIZ_START]---------->|----[Show Interface]------->|
      |                                  |----[QUIZ_QUESTION]------->|----[Display Q1: A,B,C,D]->|
      |                                  |                           |                             |
      |                                  |                           |<----[Select Option B]------|
      |                                  |                           |<----[Submit Answer]---------|
      |                                  |<----[/answer id 1]--------|                             |
      |                                  |                           |                             |
      |                                  |----[QUIZ_RESULTS]-------->|----[Leaderboard 🥇🥈🥉]--->|
```

---

## Thread Safety Mechanisms

### 1. UserManager (Thread-Safe User Management)
- Uses `ConcurrentHashMap<String, ClientHandler>`
- Thread-safe operations for adding/removing users
- Prevents race conditions in multi-threaded environment

### 2. QuizManager (Thread-Safe Quiz State)
- `ConcurrentHashMap<String, Quiz>` for active quizzes
- `ScheduledExecutorService` for quiz timing
- Synchronized access to quiz participants

### 3. ClientHandler (Per-Client Thread)
- Each client has dedicated thread from thread pool
- Isolated message handling
- Independent read/write operations

---

## Testing Instructions

### Step 1: Start Servers
```powershell
cd D:\NetTalk\ChatSystem
.\start-complete-system.bat
```
This starts:
- ChatServer on port 8888
- WebSocketBridgeServer on port 8889

### Step 2: Open Admin Panel
1. Open `D:\NetTalk\ChatSystem\frontend\admin.html`
2. Login as **Admin**

### Step 3: Create Quiz
1. Click "Create New Quiz" tab
2. Enter quiz name (e.g., "Network Quiz")
3. Click "Create Quiz"
4. Note the Quiz ID displayed

### Step 4: Add Questions
1. Click "Add Questions" tab
2. Select your quiz from dropdown
3. Add questions with 4 options (A, B, C, D)
4. Mark correct answer
5. Set time limit (e.g., 30 seconds)

### Step 5: Invite Users
1. Click "Invite to Quiz" tab
2. Select quiz
3. Type "all" or specific username
4. Click "Send Invitations"

### Step 6: User Joins (Open in New Tab)
1. Open `D:\NetTalk\ChatSystem\frontend\index.html`
2. Login with username (e.g., "sachini")
3. See invitation in Quiz Center
4. Click "Join" button
5. **Verify**: Waiting screen appears with quiz ID

### Step 7: Start Quiz (Admin)
1. Go back to admin panel
2. Click "Manage Quiz" tab
3. Click "Start Quiz"
4. **Verify**: Countdown appears

### Step 8: Answer Questions (User)
1. Quiz interface appears automatically
2. **Verify**: Questions display with options A, B, C, D
3. Select answers by clicking radio buttons
4. **Verify**: Selected options are highlighted
5. Click "Submit Quiz" when all answered

### Step 9: View Results
1. **Verify**: Results screen shows leaderboard
2. Check medals (🥇🥈🥉) for top 3
3. User's name highlighted with "You" badge

---

## Key Files Modified

### Backend (Java)
1. **ChatServer.java** (Line 86-101)
   - Fixed quiz question message format
   - Changed from multi-line to single-line format
   - Added proper A) B) C) D) formatting

2. **QuizManager.java** (Line 97-125)
   - Allowed joining in CREATED state
   - Auto-transition to READY when user joins

### Frontend (JavaScript)
3. **app.js** (Lines 1548-1591)
   - Enhanced `parseAndDisplayQuiz()` with logging
   - Better error handling for quiz start

4. **app.js** (Lines 1593-1643)
   - Improved `addQuestionToQuiz()` parsing
   - Added debug console logs
   - Better regex for option matching

5. **app.js** (Lines 1683-1745)
   - Enhanced `updateQuizInterface()` with logging
   - Fixed answer selection display logic

6. **app.js** (Lines 1747-1756)
   - Added logging to `selectQuizOption()`
   - Confirms answer storage

---

## Console Logging for Debugging

Enable browser console (F12) to see:
```
[Quiz] Parsing quiz start message: Quiz 'edw' has started! Get ready!
[Quiz] Starting quiz: edw ID: ac20cade
[Quiz] Quiz state initialized, displaying interface
[Quiz] Received question: Question 1: What is...? Options: A) Opt1, B) Opt2, C) Opt3, D) Opt4
[Quiz] Parsed - Number: 1 Text: What is... Options: A) Opt1, B) Opt2, C) Opt3, D) Opt4
[Quiz] Parsed options: [{letter: "A", text: "Opt1"}, {letter: "B", text: "Opt2"}, ...]
[Quiz] Question added. Total questions: 1
[Quiz] Updating interface with 1 questions
[Quiz] Rendering question 1: What is...
[Quiz] Questions rendered successfully
[Quiz] Selected option B for question 1
[Quiz] Stored answer index: 1 Total answers: 1
```

---

## Success Criteria ✅

- [x] Questions display with proper formatting (A, B, C, D options)
- [x] Users can select answers using radio buttons
- [x] Selected answers are visually highlighted
- [x] Submit button enables only when all questions answered
- [x] Answer submission sends correct format to server
- [x] Results display with leaderboard and rankings
- [x] Demonstrates sockets, NIO, multi-threading, client-server communication
- [x] Real-time updates via WebSocket bridge

---

## Network Programming Concepts Summary

| Concept | Component | Port | Purpose |
|---------|-----------|------|---------|
| **TCP Sockets** | ChatServer | 8888 | Client connections |
| **NIO** | MessageBroadcaster | - | Non-blocking broadcast |
| **Multi-threading** | ExecutorService | - | Concurrent client handling |
| **WebSocket** | WebSocketBridge | 8889 | Real-time web communication |
| **Client-Server** | All | - | Request-response protocol |

---

## Assignment Compliance

✅ **Sockets**: Server uses `ServerSocket` and `Socket` for TCP communication  
✅ **NIO**: `MessageBroadcaster` uses `Selector` and `SocketChannel`  
✅ **Multi-threading**: `ExecutorService` with thread pool for concurrent clients  
✅ **Client-Server**: Custom protocol with message types and handlers  
✅ **Quiz Feature**: Complete interactive quiz system with real-time updates  

**All 5 network programming concepts are demonstrated!**
