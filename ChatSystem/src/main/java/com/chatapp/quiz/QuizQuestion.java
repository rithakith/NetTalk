package com.chatapp.quiz;

import java.io.Serializable;

/**
 * QuizQuestion Model - Represents a single quiz question
 */
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String questionId;
    private String questionText;
    private String[] options;
    private int correctAnswerIndex;
    private int timeLimit; // in seconds
    
    public QuizQuestion(String questionId, String questionText, String[] options, int correctAnswerIndex, int timeLimit) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.timeLimit = timeLimit;
    }
    
    // Getters and setters
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
    
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    
    // Helper methods
    public boolean isCorrectAnswer(int answerIndex) {
        return answerIndex == correctAnswerIndex;
    }
    
    public String getCorrectAnswerText() {
        if (correctAnswerIndex >= 0 && correctAnswerIndex < options.length) {
            return options[correctAnswerIndex];
        }
        return "No correct answer";
    }
    
    @Override
    public String toString() {
        return String.format("Question[%s: %s, %d options, %ds]", 
            questionId, questionText, options.length, timeLimit);
    }
}