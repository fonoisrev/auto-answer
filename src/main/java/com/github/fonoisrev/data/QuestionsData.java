package com.github.fonoisrev.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.cs.ext.GBK;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionsData {
    
    /** logger */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(QuestionsData.class);
    private static Map<String, String> qa = new ConcurrentHashMap<>();
    
    private static final String DATA_FILE_PATH = "data/QA.csv";
    
    @PostConstruct
    public void init() throws IOException {
        // load QA.csv
        Reader in = new FileReader(DATA_FILE_PATH);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            String question = record.get("Q");
            String answer = record.get("A");
            qa.putIfAbsent(question, answer);
        }
        LOGGER.info("loaded data size is {}", qa.size());
    }
    
    public String getAnswer(String questionStr) {
        return qa.get(questionStr);
    }
}
