package com.github.fonoisrev.run;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.bean.Round;
import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionData;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * http://bath5.mggame.com.cn/zspkhscf/dj/game .html?state=home&session
 * =ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SlZjMlZ5VG1GdFpTSTZJakV6TVRBMk1qY2lMQ0pFYVhOd2JHRjVUbUZ0WlNJNkl1V1F0T2lJcWlJc0lrTnZaR1VpT2lJeE16QXdNRFEwTmlJc0lrRjJZWFJoY2xWeWJDSTZJaUlzSWs5d1pXNUpSQ0k2Ym5Wc2JDd2lTR1ZoWkdsdFoxVnliQ0k2Ym5Wc2JDd2lUbWxqYTA1aGJXVWlPbTUxYkd3c0lrMXBjMUJ5WldacGVDSTZJakV6SWl3aVEyOXRjR0Z1ZVU1aGJXVWlPaUxrdjZIbWdhX21pb0RtbktfbGhhemxqN2dpZlEuNVk0Q25iTGYzWlN0Q0dqb292aGl6Y1pteU1XbzNnczlkS1lQZjdaNjBEYw==
 */
public class MyWebSocketClient extends WebSocketClient {
    
    /** logger */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MyWebSocketClient.class);
    
    private static JsonSurfer SURFER = JsonSurferJackson.INSTANCE;
    
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    private static JsonPath MCMD = JsonPathCompiler.compile("$..mcmd");
    
    private static JsonPath SCMD = JsonPathCompiler.compile("$..scmd");
    
    private final CountDownLatch latch;
    
    private User user;
    
    private Question currentQuestion;
    
    private QuestionData questionData;
    
    /**
     * 关卡
     */
    private Round round;
    
    private long lastSend = 0L;
    
    public MyWebSocketClient(
            URI serverUri, Draft protocolDraft, User user, QuestionData questionData,
            CountDownLatch latch) {
        super(serverUri, protocolDraft);
        this.user = user;
        this.latch = latch;
        this.questionData = questionData;
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        sendAccountLogonReq();
    }
    
    
    @Override
    public void onMessage(String json) {
        
        if (json.equals("{\"mcmd\":\"Sys\",\"scmd\":\"Heart\"}")) {
            // heart beat response
            send("{\"mcmd\":\"Sys\",\"scmd\":\"Heart\"}");
            return;
        }
        
        LOGGER.info("{} Receive {}", user, json);
        
        String mcmd = SURFER.collectOne(json, String.class, MCMD);
        String scmd = SURFER.collectOne(json, String.class, SCMD);
        
        if (mcmd.equalsIgnoreCase("Account")
            && scmd.equalsIgnoreCase("LogonSuccess")) {
            getUserInfo(json);
            sendGetRoundListReq();
        } else if (mcmd.equalsIgnoreCase("TmMain")
                   && scmd.equalsIgnoreCase("TmListSuccess")) {
            // round list in json
            pickRound(json);
            if (this.round == null) {
                LOGGER.info("ERROR! {} 没有可选择的 ROUND!", user);
                quitGame();
            } else {
                sendValidateReq();
            }
        } else if (mcmd.equalsIgnoreCase("PowerMain")
                   && scmd.equalsIgnoreCase("validateRes")) {
            if (isValid(json)) {
                joinMatch();
            } else {
                quitGame();
            }
        } else if (mcmd.equalsIgnoreCase("Match")
                   && scmd.equalsIgnoreCase("JoinMatchSuccess")) {
            String getMapInfoReq = "{\"mcmd\":\"PKMain\",\"scmd\":\"MapInfo\",\"data\":{}}";
            doSend(getMapInfoReq);
            sendNextQuestionReq();
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("QuestionResult")) {
            parseQuestion(json);
            doAnswer();
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("AnswerResultCorrect")) {
            parseCorrectAnswer(json);
            questionData.putQuestion(currentQuestion);
            currentQuestion = null;
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("AnswerStepsResult")) {
            if (hasNextSteps(json)) {
                sendNextQuestionReq();
            } else {
                // nothing
            }
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("Statement")) {
            printResult(json);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            sendGetRoundListReq();
        }
        
    }
    
    
    
    JsonPath userId = JsonPathCompiler.compile("$..userId");
    
    private void getUserInfo(String json) {
        user.userId = SURFER.collectOne(json, Integer.class, userId);
    }
    
    private static String DJ_SSOLogon = "{\"mcmd\":\"Account\",\"scmd\":\"DJ_SSOLogon\"," +
                                        "\"data\":{\"state\":\"home\",\"session\":\"$SESSION\"}}";
    
    private void sendAccountLogonReq() {
        doSend(DJ_SSOLogon.replace("$SESSION", user.token));
    }
    
    private static String List = "{\"mcmd\":\"TmMain\",\"scmd\":\"List\",\"data\":{}}";
    
    private void sendGetRoundListReq() {
        doSend(List);
    }
    
    private static JsonPath roundList = JsonPathCompiler.compile("$..roundList[*]");
    
    private void pickRound(String json) {
        Collection<Round> rounds = SURFER.collectAll(json, Round.class, roundList);
        this.round = null;
        for (Round round : rounds) {
            if (!round.lock
                && round.starNum > round.userStarNum) {
                this.round = round;
                LOGGER.info("{} Pick {}", user, round);
                break;
            }
        }
        
        if (this.round == null) {
            for (Round round : rounds) {
                if (!round.lock) {
                    this.round = round;
                    break;
                }
            }
        }
    }
    
    private static String validate = "{\"mcmd\":\"PowerMain\",\"scmd\":\"validate\",\"data\":{}}";
    
    private void sendValidateReq() {
        doSend(validate);
    }
    
    private static JsonPath validateResult = JsonPathCompiler.compile("$..validateResult");
    
    private boolean isValid(String json) {
        return SURFER.collectOne(json, Boolean.class, validateResult);
    }
    
    private static String JoinMatch = "{\"mcmd\":\"Match\",\"scmd\":\"JoinMatch\"," +
                                      "\"data\":{\"roundId\":$ROUND_ID}}";
    
    private void joinMatch() {
        doSend(JoinMatch.replace("$ROUND_ID", String.valueOf(round.roundId)));
    }
    
    private void parseQuestion(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            String text = jsonNode.get("data").toString();
            Question question = objectMapper.readValue(text, Question.class);
            this.currentQuestion = question;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static JsonPath correctAnswerID = JsonPathCompiler.compile("$..correctAnswerID");
    
    private void parseCorrectAnswer(String json) {
        String id = SURFER.collectOne(json, String.class, correctAnswerID);
        currentQuestion.correctAnswerId = id;
        for (Question.Answer answer : currentQuestion.answers) {
            if (answer.answerId.equals(id)) {
                currentQuestion.correctAnswerContent = answer.content;
            }
        }
    }
    
    private static JsonPath hasNextSteps = JsonPathCompiler.compile("$..hasNextSteps");
    
    private boolean hasNextSteps(String json) {
        return SURFER.collectOne(json, Boolean.class, hasNextSteps);
    }
    
    private void sendNextQuestionReq() {
        doSend("{\"mcmd\":\"PKMain\",\"scmd\":\"NextQuestion\",\"data\":{}}");
    }
    
    private static String AiAutoAnswer =
            "{\"mcmd\":\"PKMain\",\"scmd\":\"AiAutoAnswer\",\"data\":{}}";
    
    private static JsonPath correctAnswer = JsonPathCompiler.compile("$..correctAnswer");
    
    private static String Answer = "{\"mcmd\":\"PKMain\",\"scmd\":\"Answer\"," +
                                   "\"data\":{\"answerId\":$ANSWER_ID}}";
    
    private void doAnswer() {
        try {
            Thread.sleep(1000);
            doSend(AiAutoAnswer);
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        
        Question.Answer answer = questionData.findCorrectAnswer(currentQuestion);
        if (answer == null) {
            LOGGER.info("{} 没有找到问题{}的正确答案", user, currentQuestion.questionId);
            answer = currentQuestion.answers.get(0);
        } else {
            LOGGER.info("{} 从题库中的正确答案为{}", user, answer);
        }
        doSend(Answer.replace("$ANSWER_ID", answer.answerId));
    }
    
    private void doSend(String text) {
        LOGGER.info("{} Send {}", user, text);
        lastSend = System.currentTimeMillis();
        send(text);
    }
    
    JsonPath winnerPath = JsonPathCompiler.compile("$..winner");
    private void printResult(String json) {
        int winner = SURFER.collectOne(json, Integer.class, winnerPath);
        
        if (winner == user.userId) {
            LOGGER.info("{} 本关获胜", user);
        }else if(winner == 0){
            LOGGER.info("{} 本关平局", user);
        }else {
            LOGGER.info("{} 本关惜败", user);
        }
        
    }
    
    public boolean isHalt() {
        return System.currentTimeMillis() - lastSend > 1000 * 60;
    }
    
    public void quitGame() {
        close();
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("{} 退出游戏", user);
        latch.countDown();
    }
    
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
}
