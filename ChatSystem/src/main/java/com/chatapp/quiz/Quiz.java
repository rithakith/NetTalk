package com.chatapp.quiz;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Quiz Model - Represents a quiz session
 */
public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum QuizState {
        CREATED,        // Quiz created, questions being added
        READY,          // Ready to start, waiting for admin
        COUNTDOWN,      // Starting countdown
        ACTIVE,         // Quiz in progress
        COMPLETED       // Quiz finished
    }
    
    private String quizId;
    private String quizName;
    private String adminUsername;
    private QuizState state;
    private List<QuizQuestion> questions;
    private int currentQuestionIndex;
    private Map<String, QuizParticipant> participants;
    private List<QuizResult> results;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    
    public Quiz(String quizId, String quizName, String adminUsername) {
        this.quizId = quizId;
        this.quizName = quizName;
        this.adminUsername = adminUsername;
        this.state = QuizState.CREATED;
        this.questions = new ArrayList<>();
        this.participants = new HashMap<>();
        this.results = new ArrayList<>();
        this.currentQuestionIndex = -1;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }
    
    public String getQuizName() { return quizName; }
    public void setQuizName(String quizName) { this.quizName = quizName; }
    
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    
    public QuizState getState() { return state; }
    public void setState(QuizState state) { this.state = state; }
    
    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }
    
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    
    public Map<String, QuizParticipant> getParticipants() { return participants; }
    public void setParticipants(Map<String, QuizParticipant> participants) { this.participants = participants; }
    
    public List<QuizResult> getResults() { return results; }
    public void setResults(List<QuizResult> results) { this.results = results; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    // Helper methods
    public void addQuestion(QuizQuestion question) {
        this.questions.add(question);
    }
    
    public void addParticipant(String username) {
        if (!participants.containsKey(username) && !username.equals(adminUsername)) {
            participants.put(username, new QuizParticipant(username));
        }
    }
    
    public void removeParticipant(String username) {
        participants.remove(username);
    }
    
    public QuizParticipant getParticipant(String username) {
        return participants.get(username);
    }
    
    public List<String> getParticipantUsernames() {
        return new ArrayList<>(participants.keySet());
    }
    
    public QuizQuestion getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }
    
    public boolean hasMoreQuestions() {
        return currentQuestionIndex < questions.size() - 1;
    }
    
    public void nextQuestion() {
        if (hasMoreQuestions()) {
            currentQuestionIndex++;
        }
    }
    
    public boolean isComplete() {
        return state == QuizState.COMPLETED || 
               (currentQuestionIndex >= 0 && currentQuestionIndex >= questions.size() - 1);
    }
    
    @Override
    public String toString() {
        return String.format("Quiz[%s: %s - %s, %d questions, %d participants]", 
            quizId, quizName, state, questions.size(), participants.size());
    }
}