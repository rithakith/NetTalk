# Admin Panel Testing Checklist

## ✅ Testing the New Workflow

### Pre-Test Setup
1. Start the WebSocket server: Run `run-websocket-server.bat`
2. Open `admin.html` in your browser
3. Connect as admin (username: `admin` or any name)
4. Clear localStorage if needed (F12 → Application → Local Storage → Clear)

### Test 1: Create Quiz Flow
- [ ] Click "Create Quiz" tab (should be default/active)
- [ ] Enter quiz name: "Test Quiz 1"
- [ ] Click "Create Quiz" button
- [ ] **Expected**: Auto-switch to "Add Questions" tab
- [ ] **Expected**: "Current Quiz Display" box appears showing quiz name and ID
- [ ] **Expected**: Console logs: "Creating quiz: Test Quiz 1"

### Test 2: Add Questions Flow
- [ ] Questions tab should now be active
- [ ] Quiz dropdown should show "Test Quiz 1"
- [ ] Question form should be visible
- [ ] Enter question: "What is 2+2?"
- [ ] Enter options: A="3", B="4", C="5", D="6"
- [ ] Select correct answer: 1 (option B)
- [ ] Time limit: 30 seconds
- [ ] Click "Add Question"
- [ ] **Expected**: Form clears
- [ ] **Expected**: Question counter updates to "Question #2"
- [ ] **Expected**: Current quiz questions count shows "1"
- [ ] Add 2-3 more questions
- [ ] Click "Next: Invite Users →"
- [ ] **Expected**: Auto-switch to "Invite Users" tab

### Test 3: Invite Users Flow
- [ ] Invite tab should now be active
- [ ] Current quiz info displayed at top
- [ ] Enter participants: "all" or specific usernames
- [ ] Click "Send Invitations"
- [ ] **Expected**: Console logs invitation message
- [ ] Click "Next: Start Quiz →"
- [ ] **Expected**: Auto-switch to "Start Quiz" tab

### Test 4: Start Quiz Flow
- [ ] Start Quiz tab should now be active
- [ ] Select quiz from dropdown: "Test Quiz 1 (CREATED)"
- [ ] **Expected**: Quiz info card displays
- [ ] **Expected**: Status badge shows "CREATED" (green)
- [ ] **Expected**: Questions count matches added questions
- [ ] **Expected**: Start button is enabled, End button is disabled
- [ ] Click "Start Quiz" button
- [ ] **Expected**: Status changes to "ACTIVE" (blue)
- [ ] **Expected**: Start button disables, End button enables
- [ ] **Expected**: Console logs "Starting quiz"
- [ ] Click "End Quiz" button
- [ ] **Expected**: Status changes to "ENDED" (red)
- [ ] **Expected**: Console logs "Ending quiz"

### Test 5: Manage All Tab
- [ ] Click "Manage All Quizzes" tab
- [ ] **Expected**: List of all created quizzes displays
- [ ] **Expected**: Each quiz shows name, ID, status, question count

### Test 6: Page Refresh (Persistence)
- [ ] Refresh the page (F5)
- [ ] Reconnect as admin
- [ ] Click "Manage All" tab
- [ ] **Expected**: All previously created quizzes still exist
- [ ] Go to "Start Quiz" tab
- [ ] Select a quiz
- [ ] **Expected**: Question count is correct
- [ ] **Expected**: Status is preserved

### Test 7: Multiple Quizzes
- [ ] Go back to "Create Quiz" tab
- [ ] Create a second quiz: "Test Quiz 2"
- [ ] Add 2 questions to it
- [ ] Go to "Start Quiz" tab
- [ ] **Expected**: Dropdown shows both quizzes
- [ ] Select "Test Quiz 1"
- [ ] **Expected**: Shows Test Quiz 1 details
- [ ] Select "Test Quiz 2"
- [ ] **Expected**: Shows Test Quiz 2 details

### Test 8: Tab Progression (New Session)
- [ ] Clear localStorage (F12 → Application → Clear)
- [ ] Refresh page
- [ ] Reconnect as admin
- [ ] **Expected**: Only "Create Quiz" and "Manage All" tabs are enabled
- [ ] **Expected**: Other tabs are disabled/grayed out
- [ ] Create a new quiz
- [ ] **Expected**: "Add Questions" tab becomes enabled
- [ ] Add questions and invite users
- [ ] **Expected**: "Invite Users" and "Start Quiz" tabs become enabled

### Test 9: Error Handling
- [ ] Go to "Add Questions" tab
- [ ] Try to add a question without creating a quiz first
- [ ] **Expected**: Alert: "Please create a quiz first!"
- [ ] Go to "Invite Users" tab
- [ ] Try to invite without current quiz
- [ ] **Expected**: Alert: "Please create a quiz first!"
- [ ] Try to submit question form with empty fields
- [ ] **Expected**: Alert: "Please fill in all fields"

### Test 10: Console Commands (Optional)
- [ ] Scroll to bottom Admin Console
- [ ] Type command: `/listquizzes`
- [ ] Press Enter
- [ ] **Expected**: Server responds with quiz list
- [ ] Try: `/createquiz "Console Quiz"`
- [ ] **Expected**: Quiz created, appears in Manage All tab

## 🐛 Bug Reporting
If any test fails, note:
1. Which test failed
2. What you expected
3. What actually happened
4. Any console errors (F12 → Console)
5. Network errors (F12 → Network → WS)

## 🎯 Success Criteria
All checkboxes should be checked ✅

If 80%+ pass: **Minor issues, system is functional**
If 50-80% pass: **Moderate issues, needs fixes**
If <50% pass: **Major issues, needs debugging**

---

**Last Updated**: After workflow redesign with tab navigation
**Version**: 2.0 - Guided Workflow
