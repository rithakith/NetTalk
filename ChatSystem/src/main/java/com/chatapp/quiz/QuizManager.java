package com.chatapp.quiz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * QuizManager - Manages all quiz operations
 */
public class QuizManager {
    private final ConcurrentHashMap<String, Quiz> activeQuizzes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private QuizEventListener eventListener;
    
    public interface QuizEventListener {
        void onQuizCreated(Quiz quiz);
        void onQuizStarted(Quiz quiz);
        void onQuizQuestionChanged(Quiz quiz, QuizQuestion question);
        void onQuizCompleted(Quiz quiz);
        void onParticipantJoined(Quiz quiz, String username);
        void onAnswerSubmitted(Quiz quiz, QuizResult result);
        void sendQuizMessage(String message, List<String> recipients);
    }
    
    public QuizManager() {}
    
    public void setEventListener(QuizEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Create a new quiz
     */
    public Quiz createQuiz(String quizName, String adminUsername) {
        String quizId = UUID.randomUUID().toString().substring(0, 8);
        Quiz quiz = new Quiz(quizId, quizName, adminUsername);
        activeQuizzes.put(quizId, quiz);
        
        if (eventListener != null) {
            eventListener.onQuizCreated(quiz);
        }
        
        System.out.println("[QuizManager] Quiz created: " + quiz);
        return quiz;
    }
    
    /**
     * Add question to quiz
     */
    public boolean addQuestion(String quizId, String questionText, String[] options, 
                              int correctIndex, int timeLimit, String adminUsername) {
        Quiz quiz = activeQuizzes.get(quizId);
        if (quiz == null || !quiz.getAdminUsername().equals(adminUsername) || 
            quiz.getState() != Quiz.QuizState.CREATED) {
            return false;
        }
        
        String questionId = UUID.randomUUID().toString().substring(0, 8);
        QuizQuestion question = new QuizQuestion(questionId, questionText, options, correctIndex, timeLimit);
        quiz.addQuestion(question);
        
        System.out.println("[QuizManager] Question added to quiz " + quizId + ": " + question);
        return true;
    }
    
    /**
     * Invite users to quiz
     */
    public boolean inviteToQuiz(String quizId, List<String> usernames, String adminUsername) {
        Quiz quiz = activeQuizzes.get(quizId);
        if (quiz == null || !quiz.getAdminUsername().equals(adminUsername) || 
            quiz.getQuestions().isEmpty()) {
            return false;
        }
        
        quiz.setState(Quiz.QuizState.READY);
        
        // Send invitations
        if (eventListener != null) {
            String inviteMessage = String.format("🎯 Quiz Invitation: '%s' by %s. Click 'Join Quiz' to participate!", 
                quiz.getQuizName(), adminUsername);
            eventListener.sendQuizMessage(inviteMessage, usernames);
        }
        
        System.out.println("[QuizManager] Invitations sent for quiz: " + quizId);
        return true;
    }
    
    /**
     * Join quiz
     */
    public boolean joinQuiz(String quizId, String username) {
        Quiz quiz = activeQuizzes.get(quizId);
        // Allow joining quizzes in CREATED or READY state (before they start)
        if (quiz == null || (quiz.getState() != Quiz.QuizState.CREATED && quiz.getState() != Quiz.QuizState.READY)) {
            System.out.println("[QuizManager] Cannot join quiz " + quizId + " - Quiz state: " + 
                (quiz != null ? quiz.getState() : "NULL"));
            return false;
        }
        
        // If quiz is in CREATED state and has questions, move it to READY
        if (quiz.getState() == Quiz.QuizState.CREATED && !quiz.getQuestions().isEmpty()) {
            quiz.setState(Quiz.QuizState.READY);
            System.out.println("[QuizManager] Quiz " + quizId + " state changed to READY");
        }
        
        quiz.addParticipant(username);
        
        if (eventListener != null) {
            eventListener.onParticipantJoined(quiz, username);
            String joinMessage = String.format("✅ %s joined the quiz '%s'. Waiting for admin to start...", 
                username, quiz.getQuizName());
            List<String> adminList = List.of(quiz.getAdminUsername());
            eventListener.sendQuizMessage(joinMessage, adminList);
        }
        
        System.out.println("[QuizManager] User " + username + " joined quiz: " + quizId);
        return true;
    }
    
    /**
     * Start quiz
     */
    public boolean startQuiz(String quizId, String adminUsername) {
        Quiz quiz = activeQuizzes.get(quizId);
        
        // Debug information
        System.out.println("[QuizManager] Attempting to start quiz: " + quizId);
        System.out.println("[QuizManager] Quiz exists: " + (quiz != null));
        if (quiz != null) {
            System.out.println("[QuizManager] Quiz admin: " + quiz.getAdminUsername() + ", Requesting user: " + adminUsername);
            System.out.println("[QuizManager] Quiz state: " + quiz.getState());
            System.out.println("[QuizManager] Participants count: " + quiz.getParticipants().size());
            System.out.println("[QuizManager] Questions count: " + quiz.getQuestions().size());
        }
        
        if (quiz == null) {
            System.out.println("[QuizManager] Quiz not found: " + quizId);
            return false;
        }
        
        if (!quiz.getAdminUsername().equals(adminUsername)) {
            System.out.println("[QuizManager] User not admin. Expected: " + quiz.getAdminUsername() + ", Got: " + adminUsername);
            return false;
        }
        
        if (quiz.getState() != Quiz.QuizState.READY) {
            System.out.println("[QuizManager] Quiz not in READY state. Current state: " + quiz.getState());
            return false;
        }
        
        if (quiz.getQuestions().isEmpty()) {
            System.out.println("[QuizManager] Quiz has no questions");
            return false;
        }
        
        // Allow starting even without participants for testing - admin can play alone
        if (quiz.getParticipants().isEmpty()) {
            System.out.println("[QuizManager] Warning: Starting quiz without participants (admin-only mode)");
        }
        
        quiz.setState(Quiz.QuizState.COUNTDOWN);
        quiz.setStartedAt(java.time.LocalDateTime.now());
        
        // Start countdown
        List<String> allUsers = new ArrayList<>(quiz.getParticipantUsernames());
        allUsers.add(quiz.getAdminUsername());
        
        if (eventListener != null) {
            eventListener.sendQuizMessage("🚀 Quiz starting in 5 seconds... Get ready!", allUsers);
        }
        
        // Schedule quiz start after countdown
        scheduler.schedule(() -> {
            quiz.setState(Quiz.QuizState.ACTIVE);
            quiz.setCurrentQuestionIndex(0);
            startNextQuestion(quiz);
        }, 5, TimeUnit.SECONDS);
        
        System.out.println("[QuizManager] Quiz countdown started: " + quizId);
        return true;
    }
    
    /**
     * Start next question
     */
    private void startNextQuestion(Quiz quiz) {
        QuizQuestion currentQuestion = quiz.getCurrentQuestion();
        if (currentQuestion == null) {
            endQuiz(quiz);
            return;
        }
        
        if (eventListener != null) {
            eventListener.onQuizQuestionChanged(quiz, currentQuestion);
        }
        
        // Schedule automatic progression to next question
        scheduler.schedule(() -> {
            if (quiz.getState() == Quiz.QuizState.ACTIVE) {
                if (quiz.hasMoreQuestions()) {
                    quiz.nextQuestion();
                    startNextQuestion(quiz);
                } else {
                    endQuiz(quiz);
                }
            }
        }, currentQuestion.getTimeLimit(), TimeUnit.SECONDS);
        
        System.out.println("[QuizManager] Question started: " + currentQuestion.getQuestionText());
    }
    
    /**
     * Submit answer
     */
    public boolean submitAnswer(String quizId, String username, int answerIndex, long responseTime) {
        Quiz quiz = activeQuizzes.get(quizId);
        if (quiz == null || quiz.getState() != Quiz.QuizState.ACTIVE || 
            !quiz.getParticipants().containsKey(username)) {
            return false;
        }
        
        QuizQuestion currentQuestion = quiz.getCurrentQuestion();
        if (currentQuestion == null) {
            return false;
        }
        
        // Calculate score: base_points + (remaining_time / total_time) * bonus
        boolean isCorrect = currentQuestion.isCorrectAnswer(answerIndex);
        double basePoints = isCorrect ? 100.0 : 0.0;
        double timeBonus = isCorrect ? (double)(currentQuestion.getTimeLimit() * 1000 - responseTime) / 
                          (currentQuestion.getTimeLimit() * 1000) * 50.0 : 0.0;
        double totalScore = basePoints + Math.max(0, timeBonus);
        
        QuizResult result = new QuizResult(username, currentQuestion.getQuestionId(), 
                                         answerIndex, responseTime, isCorrect, totalScore);
        
        // Add to participant's answers
        QuizParticipant participant = quiz.getParticipant(username);
        if (participant != null) {
            participant.addAnswer(result);
        }
        
        // Add to quiz results
        quiz.getResults().add(result);
        
        if (eventListener != null) {
            eventListener.onAnswerSubmitted(quiz, result);
        }
        
        System.out.println("[QuizManager] Answer submitted: " + result);
        return true;
    }
    
    /**
     * End quiz
     */
    public void endQuiz(Quiz quiz) {
        quiz.setState(Quiz.QuizState.COMPLETED);
        
        if (eventListener != null) {
            eventListener.onQuizCompleted(quiz);
        }
        
        // Generate leaderboard
        List<QuizParticipant> leaderboard = quiz.getParticipants().values()
            .stream()
            .sorted((a, b) -> {
                // Sort by total score (descending), then by total response time (ascending)
                int scoreComparison = Double.compare(b.getTotalScore(), a.getTotalScore());
                if (scoreComparison == 0) {
                    return Long.compare(a.getTotalResponseTime(), b.getTotalResponseTime());
                }
                return scoreComparison;
            })
            .collect(Collectors.toList());
        
        // Send final results
        StringBuilder results = new StringBuilder();
        results.append("🏆 QUIZ COMPLETED: ").append(quiz.getQuizName()).append("\n\n");
        results.append("📊 FINAL LEADERBOARD:\n");
        
        for (int i = 0; i < leaderboard.size(); i++) {
            QuizParticipant participant = leaderboard.get(i);
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "   ";
            results.append(String.format("%s %d. %s - %.2f points (%.1f%% accuracy)\n", 
                medal, i + 1, participant.getUsername(), participant.getTotalScore(), participant.getAccuracy()));
        }
        
        List<String> allUsers = new ArrayList<>(quiz.getParticipantUsernames());
        allUsers.add(quiz.getAdminUsername());
        
        if (eventListener != null) {
            eventListener.sendQuizMessage(results.toString(), allUsers);
        }
        
        System.out.println("[QuizManager] Quiz completed: " + quiz.getQuizId());
        
        // Remove quiz after some time
        scheduler.schedule(() -> activeQuizzes.remove(quiz.getQuizId()), 5, TimeUnit.MINUTES);
    }
    
    /**
     * Get quiz by ID
     */
    public Quiz getQuiz(String quizId) {
        return activeQuizzes.get(quizId);
    }
    
    /**
     * Delete a quiz permanently
     */
    public boolean deleteQuiz(String quizId) {
        Quiz quiz = activeQuizzes.remove(quizId);
        if (quiz != null) {
            System.out.println("[QuizManager] Quiz deleted: " + quizId + " (" + quiz.getQuizName() + ")");
            return true;
        }
        System.out.println("[QuizManager] Failed to delete quiz - not found: " + quizId);
        return false;
    }
    
    /**
     * Get all active quizzes
     */
    public List<Quiz> getActiveQuizzes() {
        return new ArrayList<>(activeQuizzes.values());
    }
    
    /**
     * Get count of active quizzes
     */
    public int getActiveQuizzesCount() {
        return activeQuizzes.size();
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}