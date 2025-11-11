package com.chatapp.quiz;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * QuizResult Model - Represents user's answer to a question
 */
public class QuizResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String questionId;
    private int selectedAnswerIndex;
    private long responseTime; // in milliseconds
    private boolean isCorrect;
    private double score;
    private LocalDateTime answeredAt;
    
    public QuizResult(String username, String questionId, int selectedAnswerIndex, 
                     long responseTime, boolean isCorrect, double score) {
        this.username = username;
        this.questionId = questionId;
        this.selectedAnswerIndex = selectedAnswerIndex;
        this.responseTime = responseTime;
        this.isCorrect = isCorrect;
        this.score = score;
        this.answeredAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    
    public int getSelectedAnswerIndex() { return selectedAnswerIndex; }
    public void setSelectedAnswerIndex(int selectedAnswerIndex) { this.selectedAnswerIndex = selectedAnswerIndex; }
    
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    
    @Override
    public String toString() {
        return String.format("Result[%s: %s, answer=%d, time=%dms, score=%.2f]", 
            username, questionId, selectedAnswerIndex, responseTime, score);
    }
}