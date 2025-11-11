# NetTalk Admin Panel - User Guide

## 🎯 Overview
The NetTalk Admin Panel has been completely redesigned with a guided workflow to make quiz creation simple and organized.

## 📋 Workflow Steps

### Step 1: Create Quiz ✨
1. Click on the **"Create Quiz"** tab (default view)
2. Enter a quiz name in the text field
3. Click **"Create Quiz"** button
4. System automatically moves you to Step 2

**Current Quiz Display:**
- After creating a quiz, you'll see the quiz name, ID, and question count
- This box stays visible throughout the workflow

### Step 2: Add Questions ❓
1. Select your quiz from the dropdown (auto-selected after creation)
2. The question form appears automatically
3. Fill in the question details:
   - **Question text**: The question you want to ask
   - **4 Options (A, B, C, D)**: Multiple choice answers
   - **Correct Answer**: Select which option is correct (0-3)
   - **Time Limit**: How many seconds to answer (default: 30)
4. Click **"Add Question"** button
5. The question counter updates automatically
6. Repeat to add more questions
7. Click **"Next: Invite Users →"** when done

**Features:**
- Question counter shows: "Question #1", "Question #2", etc.
- Form clears after each question
- Questions are saved immediately to localStorage

### Step 3: Invite Users 👥
1. The current quiz info is displayed at the top
2. Enter participants in the text area:
   - Type usernames separated by commas: `Alice,Bob,Charlie`
   - OR type `all` to invite everyone online
3. Click **"Send Invitations"** button
4. System sends invites to specified users
5. Click **"Next: Start Quiz →"** to proceed

**Tips:**
- Use "all" for open quizzes
- Use specific names for targeted quizzes

### Step 4: Start Quiz 🚀
1. Select the quiz from the dropdown
2. The quiz info card displays:
   - Quiz name
   - Current status (CREATED/ACTIVE/ENDED)
   - Number of questions
   - Quiz ID
3. Click **"Start Quiz"** to begin
   - Button becomes disabled
   - Status changes to ACTIVE
   - "End Quiz" button becomes enabled
4. Click **"End Quiz"** when finished
   - Status changes to ENDED
   - Results are collected

**Quiz Status:**
- 🟢 **CREATED**: Quiz is ready but not started
- 🔵 **ACTIVE**: Quiz is running, users can join and answer
- 🔴 **ENDED**: Quiz is finished

### Step 5: Manage All Quizzes 📊
1. Click on the **"Manage All"** tab
2. View all quizzes in a list
3. See quiz details:
   - Name and ID
   - Status
   - Question count
   - Actions available

## 🔧 Admin Console
At the bottom of all tabs, there's a console for advanced commands:
- Type commands directly: `/createquiz`, `/addquestion`, etc.
- Click **"Send Command"** or press Enter
- Console displays all activity and responses

## 💾 Data Persistence
All quizzes are automatically saved to localStorage:
- Quizzes persist across page refreshes
- Questions are stored with each quiz
- Status updates are saved immediately

## 🎨 Tab Navigation
- **Tabs unlock progressively** as you complete each step
- Initially, only "Create Quiz" is active
- After creating a quiz, "Add Questions" unlocks
- After inviting users, "Start Quiz" unlocks
- "Manage All" is always accessible

## 📝 Workflow Benefits
1. **Guided Process**: Step-by-step prevents missing important setup
2. **Visual Feedback**: Question counters, status badges, progress indicators
3. **Auto-progression**: System moves you to next step automatically
4. **Validation**: Ensures quiz has questions before inviting users
5. **Clear Status**: Always know what state your quiz is in

## 🚨 Common Issues

### Questions not saving?
- Make sure you created a quiz first (Step 1)
- Check that all fields are filled in
- Look at the console for error messages

### Can't start quiz?
- Ensure quiz has at least one question
- Check that users have been invited
- Select the correct quiz from dropdown

### Users can't join?
- Make sure quiz status is ACTIVE
- Verify you sent invitations
- Check that users are connected to the same server (port 8889)

## 🎯 Best Practices
1. **Create quiz first**: Always start with Step 1
2. **Add multiple questions**: Quizzes with 5-10 questions work best
3. **Test locally**: Create a test quiz before going live
4. **Monitor console**: Watch for server responses and errors
5. **Use meaningful names**: Name quizzes descriptively

## 🔗 Server Connection
- Admin panel connects to **port 8889** (WebSocket Bridge Server)
- Same server as regular users
- All quiz data flows through ChatServer (port 8888)
- QuizManager handles all quiz operations

## 📊 Technical Details
- **Quiz IDs**: Generated as `quiz_[timestamp]`
- **Storage**: Browser localStorage (key: `adminQuizzes`)
- **Commands**: `/createquiz`, `/addquestion`, `/invitequiz`, `/startquiz`, `/endquiz`
- **Format**: Questions stored as arrays with options and correct index

---

**Happy Quiz Creating! 🎉**
