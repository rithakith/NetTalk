# Private Chat Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PRIVATE CHAT USER FLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ Public Chat  │
│ (Welcome)    │
└──────┬───────┘
       │
       │ User clicks on "Bob" in Online Users list
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │ ← Back to Public Chat    💬 Private Chat with Bob                   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                        💭 No messages yet                            │ │
│  │                                                                       │ │
│  │           Start the conversation by typing a message below           │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  [Message Bob...                                             ] [📤 Send]  │
└──────────────────────────────────────────────────────────────────────────┘
       │
       │ User types: "Hello Bob!"
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │ ← Back to Public Chat    💬 Private Chat with Bob                   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│                                                 ┌──────────────────────┐  │
│                                                 │ Hello Bob!           │  │
│                                                 │              10:30 AM│  │
│                                                 └──────────────────────┘  │
│  ┌──────────────────────┐                                                 │
│  │ Hi Alice!            │                                                 │
│  │ 10:31 AM             │                                                 │
│  └──────────────────────┘                                                 │
│                                                                            │
│  [Message Bob...                                             ] [📤 Send]  │
└──────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                         ONLINE USERS LIST STATES                             │
└─────────────────────────────────────────────────────────────────────────────┘

NORMAL STATE:
┌──────────────┐
│ 👥 Online    │
│    Users     │
├──────────────┤
│ 👤 Alice     │ ← You (bold, purple)
│ 👤 Bob       │ ← Clickable
│ 👤 Charlie   │ ← Clickable
└──────────────┘


WITH UNREAD MESSAGES:
┌──────────────┐
│ 👥 Online    │
│    Users     │
├──────────────┤
│ 👤 Alice     │
│ 👤 Bob    [3]│ ← Red badge showing 3 unread
│ 👤 Charlie   │
└──────────────┘


ACTIVE CHAT:
┌──────────────┐
│ 👥 Online    │
│    Users     │
├──────────────┤
│ 👤 Alice     │
│ 👤 Bob       │ ← Purple gradient background (active)
│ 👤 Charlie   │
└──────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                         MESSAGE FLOW ARCHITECTURE                            │
└─────────────────────────────────────────────────────────────────────────────┘

Alice's Browser                 Server                    Bob's Browser
─────────────────              ────────                  ─────────────────
     │                            │                            │
     │ Click "Bob"                │                            │
     ├──────────────────────────► │                            │
     │ openPrivateChat('Bob')     │                            │
     │ chatView = 'private'       │                            │
     │                            │                            │
     │ Type: "Hello"              │                            │
     │ Press Enter                │                            │
     │                            │                            │
     │ {                          │                            │
     │   type: 'PRIVATE_MSG',     │                            │
     │   sender: 'Alice',         │                            │
     │   receiver: 'Bob',         │                            │
     │   content: 'Hello'         │                            │
     │ } ─────────────────────────►│                            │
     │                            │                            │
     │ addPrivateMessageToHistory │ validate & route           │
     │ updatePrivateMessageArea   │                            │
     │ (show on right)            │                            │
     │                            ├───────────────────────────►│
     │                            │                            │
     │                            │  addPrivateMessageToHistory│
     │                            │  show unread badge [1]     │
     │                            │                            │
     │                            │  User clicks "Alice"       │
     │                            │  openPrivateChat('Alice')  │
     │                            │  updatePrivateMessageArea  │
     │                            │  (show on left)            │
     │                            │                            │


┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA STRUCTURE                                       │
└─────────────────────────────────────────────────────────────────────────────┘

Global Variables:
─────────────────
privateChats = Map {
  "Bob" => [
    { sender: "Alice", content: "Hello", timestamp: "2025-11-11T10:30:00", read: true },
    { sender: "Bob", content: "Hi!", timestamp: "2025-11-11T10:31:00", read: true }
  ],
  "Charlie" => [
    { sender: "Alice", content: "Hey", timestamp: "2025-11-11T10:35:00", read: true },
    { sender: "Charlie", content: "Hello Alice", timestamp: "2025-11-11T10:36:00", read: false }
  ]
}

currentPrivateChat = "Bob"
chatView = "private"
onlineUsers = ["Alice", "Bob", "Charlie"]


Message Object Structure:
─────────────────────────
{
  sender: "Alice",           // Who sent the message
  content: "Hello Bob!",     // Message text
  timestamp: "ISO-8601",     // When it was sent
  read: true/false          // Has the user seen it?
}


┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY MODEL                                       │
└─────────────────────────────────────────────────────────────────────────────┘

CLIENT SIDE (Alice's Browser):
┌────────────────────────────────────┐
│ Private Chats Map                  │
│ ┌────────────┬──────────────────┐  │
│ │ "Bob"      │ [msg1, msg2]     │  │ ← Only Alice-Bob messages
│ │ "Charlie"  │ [msg3, msg4]     │  │ ← Only Alice-Charlie messages
│ └────────────┴──────────────────┘  │
└────────────────────────────────────┘
          ▲
          │ ISOLATED - Charlie cannot access
          │


SERVER SIDE:
┌────────────────────────────────────────────────┐
│ Receives: PRIVATE_MSG                          │
│   sender: "Alice"                              │
│   receiver: "Bob"                              │
│   content: "Hello"                             │
│                                                │
│ Validates:                                     │
│   ✓ Both users online?                         │
│   ✓ Valid usernames?                           │
│                                                │
│ Routes to:                                     │
│   → Alice's WebSocket (sender confirmation)    │
│   → Bob's WebSocket (receiver notification)    │
│   ✗ NOT sent to Charlie or anyone else         │
└────────────────────────────────────────────────┘


CLIENT SIDE (Charlie's Browser):
┌────────────────────────────────────┐
│ Private Chats Map                  │
│ ┌────────────┬──────────────────┐  │
│ │ "Alice"    │ [msg5, msg6]     │  │ ← Only Charlie-Alice messages
│ └────────────┴──────────────────┘  │
└────────────────────────────────────┘
          ▲
          │ CANNOT see Alice-Bob conversation
          │


┌─────────────────────────────────────────────────────────────────────────────┐
│                         KEY FUNCTIONS REFERENCE                              │
└─────────────────────────────────────────────────────────────────────────────┘

openPrivateChat(targetUser)
├─ Set currentPrivateChat = targetUser
├─ Set chatView = 'private'
├─ Initialize privateChats.get(targetUser) if new
├─ Call markPrivateChatAsRead(targetUser)
├─ Call displayPrivateChatInterface(targetUser)
└─ Update user list highlighting

closePrivateChat()
├─ Set currentPrivateChat = null
├─ Set chatView = 'public'
├─ Call displayPublicChatInterface()
└─ Update user list

displayPrivateChatInterface(targetUser)
├─ Hide welcome screen
├─ Create private chat header with back button
├─ Create messages container
└─ Call updatePrivateMessageArea(targetUser)

updatePrivateMessageArea(targetUser)
├─ Get messages from privateChats.get(targetUser)
├─ Clear container
├─ For each message:
│  ├─ Create bubble (left for received, right for sent)
│  ├─ Add content and timestamp
│  └─ Append to container
└─ Scroll to bottom

addPrivateMessageToHistory(targetUser, message, sender)
├─ Get or create chat array for targetUser
├─ Add message object { sender, content, timestamp, read }
├─ If active chat: call updatePrivateMessageArea()
└─ Update user list (for badges)

sendMessage(message)
├─ If chatView === 'private' && currentPrivateChat:
│  ├─ Create PRIVATE_MSG with receiver = currentPrivateChat
│  ├─ Send via WebSocket
│  └─ Call addPrivateMessageToHistory() immediately
├─ Else if message.startsWith('@'):
│  └─ Parse @username and create PRIVATE_MSG
└─ Else: Handle as public message

handleMessage(message) - PRIVATE_MSG case
├─ Determine otherUser (sender or receiver)
├─ If sender !== username:
│  └─ Call addPrivateMessageToHistory(otherUser, content, sender)
└─ If chatView !== 'private':
   └─ Also show in public chat (backward compat)
```
