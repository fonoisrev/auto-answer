package com.github.fonoisrev.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionsData {
    
    /** logger */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(QuestionsData.class);
    
    private static Map<String, String> qa = new ConcurrentHashMap<>();
    
    private static final String DATA_FILE_PATH = "data/QA.csv";
    
    private static final String DATA_FILE_ADDTION_PATH = "data/QA.add.csv";
    
    private static Map<String, String> qa_add = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws IOException {
        // load QA.csv
        try (Reader in = new FileReader(DATA_FILE_PATH);) {
            Iterable<CSVRecord> records =
                    CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                String question = record.get("Q");
                String answer = record.get("A");
                qa.putIfAbsent(question, answer);
            }
        }
        LOGGER.info("loaded data size is {}", qa.size());
    }
    
    public String getAnswer(String questionStr) {
        return qa.get(questionStr);
    }
    
    public void addQuestion(String question, String answer) {
        qa_add.putIfAbsent(question, answer);
    }
    
    public void doSave() throws IOException {
        if (qa_add.size() == 0) {
            return;
        }
        
        LOGGER.info("向ADD题库写入题目");
        try (Writer out = new FileWriter(DATA_FILE_ADDTION_PATH, true);) {
            CSVFormat format =
                    CSVFormat.EXCEL.withHeader("Q", "A").withSkipHeaderRecord();
            CSVPrinter printer = new CSVPrinter(out, format);
            qa_add.forEach((q, a) -> {
                try {
                    printer.printRecord(q, a);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            qa_add.clear();
        }
    }
}
