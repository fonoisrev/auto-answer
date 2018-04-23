package com.github.fonoisrev;

import com.github.fonoisrev.data.QuestionsData;
import com.github.fonoisrev.data.UserData;
import com.github.fonoisrev.run.MyRunner;
import com.github.fonoisrev.run.MyWebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
public class AutoAnswer {
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(AutoAnswer.class).web(false).run(args);
    }
    
    @Bean
    public QuestionsData questionsData() {
        return new QuestionsData();
    }
    
    
    @Bean
    public MyRunner myRunner() {
        return new MyRunner();
    }
    
    @Bean
    public UserData userData() {
        return new UserData();
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
