package com.github.fonoisrev;

import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.data.QuestionData;
import org.junit.Test;

import java.io.IOException;

public class QuestionDataTest {
    
    private static QuestionData questionData = new QuestionData();
    
    @Test
    public void testInit() throws IOException {
        
        questionData.init();
        
    }
    
    @Test
    public void testPut() {
        Question question = new Question();
        question.questionID = "100";
        question.questionContent = "测试内容";
        question.correctAnswerId = "200";
        question.correctAnswerContent = "答案,ge";
        questionData.putQuestion(question);
    }
}
