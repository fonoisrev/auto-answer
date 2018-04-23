package com.github.fonoisrev.data;

import com.github.fonoisrev.bean.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserData.class);
    
    private static final List<User> users = new ArrayList<>();
    
    private static final String USER_FILE_PATH = "data/user.csv";
    
    @PostConstruct
    public void init() throws IOException {
        Reader in = new FileReader(USER_FILE_PATH);
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            User user = new User();
            user.name =record.get("NAME");
            user.phoneNum =record.get("PHONE");
            user.answerToken = record.get("ANSWER_TOKEN");
            user.loginToken = record.get("LOGIN_TOKEN");
            users.add(user);
        }
        LOGGER.info("loaded users size is {}", users.size());
    }
    
    public List<User> getUsers() {
        return users;
    }
}
