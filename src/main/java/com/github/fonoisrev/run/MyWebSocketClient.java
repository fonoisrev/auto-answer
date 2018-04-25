package com.github.fonoisrev.run;

import com.github.fonoisrev.bean.Option;
import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionsData;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class MyWebSocketClient extends WebSocketClient {
    
    /** logger */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MyWebSocketClient.class);
    
    private static JsonSurfer SURFER = JsonSurferJackson.INSTANCE;
    
    private static JsonPath TYPE_PATH = JsonPathCompiler.compile("$..type");
    
    private static JsonPath QUESTION_VERIFY_CODE_PATH =
            JsonPathCompiler.compile("$..verifyCode");
    
    private static JsonPath QUESTION_ID_PATH =
            JsonPathCompiler.compile("$..questionIdInt");
    
    private static JsonPath QUESTION_TITLE_PATH =
            JsonPathCompiler.compile("$..questionTitleStr");
    
    private static JsonPath QUESTION_OPTIONS_PATH =
            JsonPathCompiler.compile("$.questionOptionsArray.*");
    
    private static final String COMPUTER =
            "{\"type\":\"$COMPUTER\",\"token\":\"$ANSWER_TOKEN\"," +
            "\"phone\":\"$PHONE\"}";
    
    private static final String ANSWER =
            "{\"type\":\"answer_computer\"," +
            "\"answerMap\":[{\"answer\":\"$CHOICE\",\"id\":$ID}]," +
            "\"verifyCode\":$VERIFY_CODE,\"token\":\"$ANSWER_TOKEN\"," +
            "\"phone\":\"$PHONE\",\"time\":8}";
    
    private QuestionsData questionsData;
    
    private User user;
    
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private Question question;
    
    private boolean noMatch;
    
    public MyWebSocketClient(
            URI serverUri, Draft protocolDraft, User user,
            QuestionsData questionsData) {
        super(serverUri, protocolDraft);
        this.user = user;
        this.questionsData = questionsData;
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("open connection");
        String ready_computer = COMPUTER
                .replace("$COMPUTER", "ready_computer")
                .replace("$ANSWER_TOKEN", user.answerToken)
                .replace("$PHONE", user.phoneNum);
        LOGGER.info("Send {}", ready_computer);
        send(ready_computer);
    }
    
    @Override
    public void onMessage(String json) {
        LOGGER.info("Recieve {}", json);
        
        if ("FINISH".equals(json)) {
            close();
            return;
        }
        
        String type = SURFER.collectOne(json, String.class, TYPE_PATH);
        if ("USER".equals(type)) {
            String ready_computer = COMPUTER
                    .replace("$COMPUTER", "start_computer")
                    .replace("$ANSWER_TOKEN", user.answerToken)
                    .replace("$PHONE", user.phoneNum);
            
            LOGGER.info("Send {}", ready_computer);
            send(ready_computer);
        } else if ("Question".equals(type)) {
            question = new Question();
            question.id =
                    SURFER.collectOne(json, Integer.class, QUESTION_ID_PATH);
            question.title =
                    SURFER.collectOne(json, String.class, QUESTION_TITLE_PATH);
            question.verifyCode =
                    SURFER.collectOne(json, Integer.class,
                                      QUESTION_VERIFY_CODE_PATH);
            question.options = SURFER.collectAll(json, Option.class,
                                                 QUESTION_OPTIONS_PATH);
            
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            String choice = "a";
            
            String answerStr = questionsData.getAnswer(question.title);
            if (StringUtils.isEmpty(answerStr)) {
                noMatch = true;
                LOGGER.error("Question[{}] no match answers", question.title);
            } else {
                noMatch = false;
                for (Option o : question.options) {
                    String content = o.optionContentStr;
                    if (answerStr.equals(content)) {
                        choice = o.id;
                    }
                }
            }
            
            String answer = ANSWER
                    .replace("$CHOICE", choice)
                    .replace("$ID", String.valueOf(question.id))
                    .replace("$VERIFY_CODE",
                             String.valueOf(question.verifyCode))
                    .replace("$ANSWER_TOKEN", user.answerToken)
                    .replace("$PHONE", user.phoneNum);
            
            LOGGER.info("Send answer {}", answer);
            send(answer);
        } else if ("answer_complete".equals(type)) {
            String myAnswer = SURFER.collectOne(json, String.class, JsonPathCompiler.compile("$..answer"));
            String rightAnswer = SURFER.collectOne(json, String.class, JsonPathCompiler.compile("$..rightAnswer"));
            if (!myAnswer.equals(rightAnswer) || noMatch){
                LOGGER.error("答案错误 或 题库中不存在！！");
                String answerStr = null;
                for (Option option : question.options) {
                    if (option.id.equals(rightAnswer)) {
                        answerStr = option.optionContentStr;
                        break;
                    }
                }
                questionsData.addQuestion(question.title, answerStr);
            }
        } else {
//            throw new RuntimeException("No Match !!!");
            close();
        }
        
        return;
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        countDownLatch.countDown();
    }
    
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
    public void join() {
        try {
            countDownLatch.await();
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }
}
