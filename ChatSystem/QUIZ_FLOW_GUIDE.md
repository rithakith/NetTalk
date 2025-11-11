# 🎯 Quiz Creation Flow - Simple Guide

## The 4-Step Process

```
┌─────────────────┐
│  1️⃣ GIVE NAME   │ → Name your quiz
└────────┬────────┘
         ↓
┌─────────────────┐
│ 2️⃣ MAKE QUESTIONS│ → Add questions with 4 options each
└────────┬────────┘
         ↓
┌─────────────────┐
│ 3️⃣ SEND INVITES │ → Invite users (type "all" or names)
└────────┬────────┘
         ↓
┌─────────────────┐
│  4️⃣ START QUIZ  │ → Click "Start Quiz Now"
└─────────────────┘
```

## Step-by-Step Instructions

### Step 1: Give Your Quiz a Name
1. Open admin panel → You'll see "Step 1: Give Your Quiz a Name"
2. Type a quiz name (e.g., "Science Quiz")
3. Click **"Create Quiz & Continue"**
4. ✅ Quiz is created!
5. You automatically move to Step 2

### Step 2: Make Questions
1. You'll see "Step 2: Make Questions"
2. Your quiz name and question count is shown at the top
3. Fill in the question form:
   - **Question**: Type your question
   - **Option A, B, C, D**: Four answer choices
   - **Correct Answer**: Select which option is correct
   - **Time Limit**: Seconds to answer (default: 30)
4. Click **"Add This Question"**
5. Form clears → Add more questions
6. When done, click **"Save Quiz & Invite Users →"**
7. ✅ Questions are saved!
8. You move to Step 3

### Step 3: Send Invitations
1. You'll see "Step 3: Send Invitations"
2. Your quiz name and question count is shown
3. In the text box, type:
   - `all` to invite everyone, OR
   - `Alice, Bob, Charlie` for specific users
4. Click **"Send Invitations"**
5. ✅ Invites are sent!
6. Click **"Next: Start Quiz"**
7. You move to Step 4

### Step 4: Start Quiz
1. You'll see "Step 4: Start Quiz"
2. Your quiz details are displayed
3. Status shows "CREATED"
4. Click **"Start Quiz Now"**
5. ✅ Quiz is live!
6. Status changes to "ACTIVE"
7. Users can now join and answer
8. When finished, click **"End Quiz"**

## Quick Tips

### ✨ Auto-Flow
- After creating quiz → Auto moves to questions
- After saving questions → Auto moves to invites  
- After sending invites → Auto moves to start
- **You're always guided to the next step!**

### 💡 Smart Features
- **Question Counter**: Shows "Question #1", "Question #2"
- **Live Count**: See how many questions you've added
- **Auto-Save**: Questions save immediately to browser
- **Clear Forms**: Form clears after each question

### 🎯 Best Practices
1. **Name clearly**: "History Quiz" not just "Quiz 1"
2. **Add 3-5 questions**: Good quiz length
3. **Test first**: Create a test quiz before real one
4. **Invite early**: Send invites before starting

### 🚫 Common Mistakes

**DON'T:**
- ❌ Skip steps (flow is locked until you complete each)
- ❌ Start without questions (system won't let you)
- ❌ Forget to send invites (users won't know about quiz)

**DO:**
- ✅ Follow the numbered steps in order
- ✅ Add at least 1 question before moving on
- ✅ Use "all" to invite everyone easily
- ✅ Start quiz only when ready

## Example Walkthrough

### Creating "General Knowledge Quiz"

```
STEP 1: Give Name
→ Type: "General Knowledge Quiz"
→ Click: "Create Quiz & Continue"
→ Result: Quiz created, moved to Step 2

STEP 2: Make Questions
→ Question 1:
   Q: "What is the capital of France?"
   A: Paris | B: London | C: Berlin | D: Rome
   Correct: 0 (Paris)
→ Click: "Add This Question"
→ Question 2:
   Q: "What is 2+2?"
   A: 3 | B: 4 | C: 5 | D: 6
   Correct: 1 (4)
→ Click: "Add This Question"
→ Click: "Save Quiz & Invite Users →"
→ Result: 2 questions saved, moved to Step 3

STEP 3: Send Invitations
→ Type: "all"
→ Click: "Send Invitations"
→ Click: "Next: Start Quiz"
→ Result: Everyone invited, moved to Step 4

STEP 4: Start Quiz
→ See: "General Knowledge Quiz" with 2 questions
→ Click: "Start Quiz Now"
→ Result: Quiz is ACTIVE, users can join!
```

## Troubleshooting

### "Please create a quiz first!"
- You tried to skip Step 1
- Go back to "Create Quiz" tab
- Enter a quiz name

### "Please fill in all fields"
- Some question fields are empty
- Make sure all 4 options are filled

### "Please add at least one question"
- You tried to skip to invites without questions
- Add at least 1 question first

### Questions disappeared after refresh?
- Clear your browser cache
- Or localStorage might be full
- Check browser console (F12) for errors

## Technical Notes

- **Storage**: Quizzes save to browser localStorage
- **Server**: Uses port 8889 (WebSocket Bridge)
- **Format**: Questions sent as `/addquestion quizId|question|opt1,opt2,opt3,opt4|correctIndex|timeLimit`
- **Auto-progression**: JavaScript automatically switches tabs after each step

---

**🎉 That's it! Just 4 simple steps to create and run a quiz!**
