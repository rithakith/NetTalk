# Private Chat Testing Guide

## Prerequisites
- NetTalk server running on port 8888 (TCP) and 8889 (WebSocket)
- At least 2 browser windows/tabs for testing

## Step-by-Step Testing

### Test 1: Basic Private Chat
1. **Open User 1 (Browser 1)**
   - Navigate to `http://localhost:8889` or your server address
   - Connect with username: `Alice`
   - You should see the chat interface

2. **Open User 2 (Browser 2)**
   - Open a new browser window/incognito tab
   - Navigate to same address
   - Connect with username: `Bob`

3. **Verify User List**
   - In Alice's window: Check right column "Online Users"
   - Should see: `Alice` (bold, purple) and `Bob` (clickable)
   - In Bob's window: Should see: `Bob` (bold, purple) and `Alice` (clickable)

4. **Open Private Chat**
   - In Alice's window: Click on `Bob` in the user list
   - Private chat interface should appear with:
     - Purple gradient header showing "Private Chat with Bob"
     - "← Back to Public Chat" button
     - Empty state message: "No messages yet"
     - Input placeholder: "Message Bob..."

5. **Send First Message**
   - In Alice's chat with Bob, type: `Hello Bob!`
   - Press Enter
   - Message should appear on the right side with purple gradient bubble
   - Timestamp should show current time

6. **Verify Message Reception**
   - In Bob's window: Red badge with "1" should appear next to Alice's name
   - Click on Alice in the user list
   - Private chat opens showing Alice's message on the left (white bubble)

7. **Reply**
   - In Bob's chat with Alice, type: `Hi Alice! How are you?`
   - Press Enter
   - Message appears on right side for Bob
   - In Alice's window: Message appears on left side immediately

8. **Continue Conversation**
   - Send 3-4 more messages from each user
   - Verify all messages appear correctly
   - Verify timestamps are accurate
   - Verify scroll works if many messages

### Test 2: Multiple Private Chats
1. **Open User 3 (Browser 3)**
   - Connect with username: `Charlie`

2. **Alice Chats with Charlie**
   - In Alice's window: Click "← Back to Public Chat"
   - User list should show Bob with unread badge if he sent messages
   - Click on `Charlie`
   - New private chat opens
   - Previous chat with Bob is stored

3. **Send Messages to Charlie**
   - Send: `Hey Charlie!`
   - Verify it appears

4. **Switch Back to Bob**
   - Click "← Back to Public Chat"
   - Click on `Bob`
   - Previous conversation should be intact
   - All previous messages visible

5. **Verify Isolation**
   - Messages to Bob should NOT appear in Charlie chat
   - Messages to Charlie should NOT appear in Bob chat

### Test 3: Unread Badges
1. **Bob Sends Multiple Messages**
   - In Bob's window: Open chat with Alice
   - Send 3 messages quickly:
     - `Message 1`
     - `Message 2`
     - `Message 3`

2. **Verify Unread Count**
   - In Alice's window: Switch to public chat
   - Bob's name should show red badge with "3"

3. **Read Messages**
   - Click on Bob
   - Badge should disappear
   - All 3 messages should be visible

### Test 4: Active Chat Highlighting
1. **Open Chat**
   - Click on any user
   - User list should highlight that user with purple gradient

2. **Switch Chats**
   - Go back to public chat
   - Open different user
   - New user should be highlighted
   - Previous user returns to normal style

### Test 5: Message Privacy
1. **Send Private Message**
   - Alice sends message to Bob: `This is private`

2. **Verify Charlie Cannot See**
   - In Charlie's window: Check public chat area
   - Should NOT see Alice's message to Bob
   - Charlie's user list should NOT show badges for Alice-Bob conversation

3. **Public Message**
   - In Alice's window: Click "← Back to Public Chat"
   - Type and send: `Hello everyone!`
   - All users (Alice, Bob, Charlie) should see this in public chat

### Test 6: UI/UX Features
1. **Empty State**
   - Open chat with new user who hasn't messaged
   - Should see friendly empty state with icon and hint

2. **Back Button**
   - Verify "← Back to Public Chat" button works
   - Should return to public view
   - Welcome screen or previous messages visible

3. **Input Placeholder**
   - Private chat: Should say "Message {username}..."
   - Public chat: Should say "Type a message to start chatting..."

4. **Message Bubbles**
   - Sent messages: Right side, purple gradient
   - Received messages: Left side, white background
   - Both should have rounded corners and timestamps

### Test 7: Edge Cases
1. **User Disconnects**
   - Close Bob's browser
   - In Alice's window: Bob should disappear from user list
   - Try to open chat with Bob: Should fail gracefully

2. **Reconnection**
   - Reconnect as Bob
   - Alice should see Bob in user list again
   - Previous messages cleared (expected - no persistence)

3. **Multiple Messages Rapidly**
   - Send 10 messages very quickly
   - All should appear in order
   - Scroll should work
   - No duplicates

4. **Long Messages**
   - Send a very long message (200+ characters)
   - Should wrap properly in bubble
   - Bubble should not overflow

5. **Special Characters**
   - Send message with emojis: `Hello! 😊 🎉`
   - Send message with symbols: `Test @#$%^&*()`
   - All should display correctly

### Test 8: Public vs Private Context
1. **While in Private Chat**
   - Type message WITHOUT `@username`
   - Press Enter
   - Should send as private message automatically

2. **In Public Chat**
   - Type: `@Bob Testing @username format`
   - Should still work (backward compatibility)

3. **Commands in Private Chat**
   - Type: `/help`
   - Should process as command, not private message
   - Type: `/quizzes`
   - Should work normally

## Expected Results Summary

### ✅ Pass Criteria
- [ ] Users can click names to open private chat
- [ ] Private chat interface displays correctly
- [ ] Messages appear in correct bubbles (sent/received)
- [ ] Timestamps are accurate
- [ ] Unread badges show correct count
- [ ] Active chat is highlighted in user list
- [ ] Back button returns to public chat
- [ ] Message isolation works (privacy maintained)
- [ ] Multiple private chats can be managed
- [ ] Switching between chats preserves history
- [ ] UI is responsive and smooth
- [ ] No console errors

### ❌ Known Limitations (Expected)
- Messages clear on page refresh (no persistence)
- Cannot send to offline users
- No typing indicators in private chat yet
- No message status (delivered/seen) in private chat yet

## Troubleshooting

### Issue: User list not showing
- **Check:** Server is running
- **Check:** WebSocket connection established (look for "Connected" status)
- **Check:** Browser console for errors

### Issue: Messages not appearing
- **Check:** Both users are online
- **Check:** Network tab shows WebSocket messages being sent
- **Check:** Server logs show message routing

### Issue: Unread badges not updating
- **Refresh:** User list by clicking back to public chat
- **Check:** Browser console for JavaScript errors

### Issue: Styling looks wrong
- **Clear:** Browser cache
- **Refresh:** Page with Ctrl+F5
- **Check:** style.css loaded properly (Network tab)

## Console Commands for Debugging

Open browser console (F12) and try:

```javascript
// Check private chat state
console.log('Current chat view:', chatView);
console.log('Current private chat:', currentPrivateChat);
console.log('All private chats:', privateChats);

// Check online users
console.log('Online users:', onlineUsers);

// Manually open chat
openPrivateChat('Bob');

// Close chat
closePrivateChat();

// Check specific chat history
console.log(privateChats.get('Bob'));
```

## Performance Testing

### Test with Many Messages
1. Send 50+ messages in one chat
2. Check scroll performance
3. Verify no lag when typing
4. Check memory usage (Chrome DevTools > Memory)

### Test with Many Users
1. Connect 10+ users
2. Verify user list renders correctly
3. Check if clicking users still responsive
4. Verify no slowdown

## Success Indicators
If all tests pass:
- ✅ Feature is working correctly
- ✅ Ready for production use
- ✅ User experience is smooth

If some tests fail:
- 📝 Note which tests failed
- 🔍 Check browser console for errors
- 🐛 Review PRIVATE_CHAT_IMPLEMENTATION.md for troubleshooting
- 💬 Report issues with error messages

## Next Steps After Testing
1. Add localStorage persistence (optional)
2. Implement typing indicators for private chat
3. Add message status (delivered/seen)
4. Consider notification sounds
5. Add emoji picker
6. Implement file sharing
