package com.github.fonoisrev.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Question {
    
    public String questionID;
    
    public String questionContent;
    
    public List<Answer> answers;
    
    @JsonIgnore
    public volatile String correctAnswerId;
    
    @JsonIgnore
    public volatile String correctAnswerContent;
    
    @Data
    public static class Answer {
        
        public String answerID;
        
        public String content;
    
    }
    
}
