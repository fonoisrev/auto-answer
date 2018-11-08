package com.github.fonoisrev.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Question {
    
    public volatile String questionId;
    
    public volatile String content;
    
    public List<Answer> answers;
    
    @JsonProperty("correctAnswer")
    public volatile String correctAnswerId;
    
    @JsonIgnore
    public volatile String correctAnswerContent;
    
    
    public static class Answer {
        
        public String answerId;
        
        public boolean isCorrect;
        
        public String content;
    
        @Override
        public String toString() {
            return "Answer{" +
                   "answerId='" + answerId + '\'' +
                   ", content='" + content + '\'' +
                   '}';
        }
    }
    
    @Override
    public String toString() {
        return "Question{" +
               "questionId='" + questionId + '\'' +
               ", content='" + content + '\'' +
               ", correctAnswerId='" + correctAnswerId + '\'' +
               ", correctAnswerContent='" + correctAnswerContent + '\'' +
               '}';
    }
}
