package com.chatapp.quiz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizParticipant Model - Represents a participant in the quiz
 */
public class QuizParticipant implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private double totalScore;
    private int correctAnswers;
    private int totalQuestions;
    private List<QuizResult> answers;
    private boolean isReady;
    private long totalResponseTime;
    
    public QuizParticipant(String username) {
        this.username = username;
        this.totalScore = 0.0;
        this.correctAnswers = 0;
        this.totalQuestions = 0;
        this.answers = new ArrayList<>();
        this.isReady = false;
        this.totalResponseTime = 0;
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
    
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public List<QuizResult> getAnswers() { return answers; }
    public void setAnswers(List<QuizResult> answers) { this.answers = answers; }
    
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { isReady = ready; }
    
    public long getTotalResponseTime() { return totalResponseTime; }
    public void setTotalResponseTime(long totalResponseTime) { this.totalResponseTime = totalResponseTime; }
    
    // Helper methods
    public void addAnswer(QuizResult result) {
        answers.add(result);
        totalScore += result.getScore();
        totalQuestions++;
        totalResponseTime += result.getResponseTime();
        if (result.isCorrect()) {
            correctAnswers++;
        }
    }
    
    public double getAccuracy() {
        return totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 100.0 : 0.0;
    }
    
    public double getAverageResponseTime() {
        return totalQuestions > 0 ? (double) totalResponseTime / totalQuestions : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("Participant[%s: %.2f points, %d/%d correct, %.1f%% accuracy]", 
            username, totalScore, correctAnswers, totalQuestions, getAccuracy());
    }
}