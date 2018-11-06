package com.github.fonoisrev.data;

import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.bean.Question.Answer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionData {
    
    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionData.class);
    
    public static final Map<String, Question> questionById = new ConcurrentHashMap<>();
    
    public static final Map<String, Question> questionByContent = new ConcurrentHashMap<>();
    
    private static final String QUESTION_FILE_PATH = "data/question.csv";
    
    private static CSVFormat csvFormat = CSVFormat.EXCEL.withFirstRecordAsHeader().withTrim();
    
    @PostConstruct
    public void init() throws IOException {
        Reader in = new FileReader(QUESTION_FILE_PATH);
        Iterable<CSVRecord> records = csvFormat.parse(in);
        for (CSVRecord record : records) {
            Question question = new Question();
            question.questionId = record.get("QUESTION_ID");
            question.content = record.get("CONTENT");
            question.correctAnswerId = record.get("CORRECT_ANSWER_ID");
            question.correctAnswerContent = record.get("CORRECT_ANSWER_CONTENT");
            putQuestionIntoMap(question);
        }
        LOGGER.info("loaded question size is {}", questionById.size());
    }
    
    public Answer findCorrectAnswer(Question question) {
        if (!"0".equals(question.correctAnswerId)) {
            for (Answer answer : question.answers) {
                if (answer.answerId.equals(question.correctAnswerId)) {
                    return answer;
                }
            }
        }
        
        Question record = null;
        if (questionById.containsKey(question.questionId)) {
            record = questionById.get(question.questionId);
        } else if (questionByContent.containsKey(question.content)) {
            record = questionByContent.get(question.content);
        }
        if (record != null) {
            for (Answer answer : question.answers) {
                if (record.correctAnswerId.equals(answer.answerId)
                    || record.correctAnswerContent.equals(answer.content)) {
                    return answer;
                }
            }
        }
        
        return null;
    }
    
    public synchronized void putQuestion(Question question) {
        if (questionById.containsKey(question.questionId)) {
            return;
        }
        putQuestionIntoMap(question);
        writeQuestionToFile(question);
    }
    
    private void putQuestionIntoMap(Question question) {
        if (questionById.putIfAbsent(question.questionId, question) != null) {
            LOGGER.warn("Question : id={} 已经存在", question.questionId);
        }
        if (questionByContent.putIfAbsent(question.content, question) != null) {
            LOGGER.warn("Question : content={} 己经存在", question.content);
        }
    }
    
    private void writeQuestionToFile(Question question) {
        try (
                FileWriter out = new FileWriter(QUESTION_FILE_PATH, true);
                CSVPrinter printer = new CSVPrinter(out, csvFormat)
        ) {
            printer.printRecord(
                    question.questionId,
                    question.content,
                    question.correctAnswerId,
                    question.correctAnswerContent);
            
            LOGGER.info("向题库中写入问题 {}", question);
        } catch (IOException e) {
        }
        
    }
    
}
