package com.github.fonoisrev.run;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.bean.Round;
import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionData;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.Semaphore;

@Slf4j
public class MyWebSocketClient extends WebSocketClient {
    
    
    private static JsonSurfer SURFER = JsonSurferJackson.INSTANCE;
    
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    private static JsonPath MCMD = JsonPathCompiler.compile("$..mcmd");
    
    private static JsonPath SCMD = JsonPathCompiler.compile("$..scmd");
    
    private final Semaphore semaphore;
    
    public User user;
    
    private Question currentQuestion;
    
    private QuestionData questionData;
    
    private volatile boolean isGaming = false;
    
    private int nextStage;
    
    /**
     * 关卡
     */
    private Round round;
    
    private long lastSend = 0L;
    
    public MyWebSocketClient(
            URI serverUri, Draft protocolDraft, User user, QuestionData questionData,
            Semaphore semaphore) {
        super(serverUri, protocolDraft);
        this.user = user;
        this.semaphore = semaphore;
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
        
        log.info("{} Rcv {}", user, json);
        
        String mcmd = SURFER.collectOne(json, String.class, MCMD);
        String scmd = SURFER.collectOne(json, String.class, SCMD);
        
        if (mcmd.equalsIgnoreCase("Account")) {
            if (scmd.equalsIgnoreCase("LogonSuccess")) {
                getUserInfo(json);
                sendBattle();
                sendGetRoundListReq();
            } else if (scmd.equalsIgnoreCase("LogonFail")) {
                quitGame();
            }
        } else if (mcmd.equalsIgnoreCase("Stage")
                   && scmd.equalsIgnoreCase("ListSuccess")) {
            getNextStage(json);
            sendValidateReq();
        } else if (mcmd.equalsIgnoreCase("PowerMain")
                   && scmd.equalsIgnoreCase("validateRes")) {
            boolean isValid = isValid(json);
            if (isValid) {
                startGame();
                joinMatch();
            } else {
                quitGame();
            }
            
        } else if (mcmd.equalsIgnoreCase("Match")
                   && scmd.equalsIgnoreCase("JoinMatchSuccess")) {
            getPlayer(json);
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("NextQuestion")) {
            parseQuestionAndDoAnswer(json);
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("AnswerResultCorrect")) {
            parseCorrectAnswer(json);
            questionData.putQuestion(currentQuestion);
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("Statement")) {
            printResult(json);
            isGaming = false;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            sendGetRoundListReq();
        }
        
    }
    
    
    private static String battle = "{\"mcmd\":\"Battle\",\"scmd\":\"queryUserBattle\"}";
    
    private void sendBattle() {
        doSend(battle);
    }
    
    private static String List = "{\"mcmd\":\"Stage\",\"scmd\":\"List\",\"data\":{}}";
    
    
    private void sendGetRoundListReq() {
        doSend(List);
    }
    
    
    // 登陆
    
    private static String DJ_SSOLogon = "{\"mcmd\":\"Account\",\"scmd\":\"DJ_SSOLogon\"," +
                                        "\"data\":{\"state\":\"home\",\"session\":\"$SESSION\"}}";
    
    private void sendAccountLogonReq() {
        doSend(DJ_SSOLogon.replace("$SESSION", user.token));
    }
    
    
    // 对局结果
    JsonPath winnerPath = JsonPathCompiler.compile("$..winner");
    
    private void printResult(String json) {
        int winner = SURFER.collectOne(json, Integer.class, winnerPath);
        
        if (winner == user.userId) {
            log.info("{} 本关获胜", user);
        } else if (winner == 0) {
            log.info("{} 本关平局", user);
        } else {
            log.info("{} 本关惜败", user);
        }
        
    }
    
    // 处理正确答案
    private static JsonPath correctAnswerID = JsonPathCompiler.compile("$..correctAnswerID");
    
    private void parseCorrectAnswer(String json) {
        String id = SURFER.collectOne(json, String.class, correctAnswerID);
        currentQuestion.correctAnswerId = id;
        for (Question.Answer answer : currentQuestion.answers) {
            if (answer.answerID.equals(id)) {
                currentQuestion.correctAnswerContent = answer.content;
            }
        }
    }
    
    // 作答
    private static String AiAutoAnswer =
            "{\"mcmd\":\"PKMain\",\"scmd\":\"AiAutoAnswer\",\"data\":{}}";
    
    private static String Answer = "{\"mcmd\":\"PKMain\",\"scmd\":\"Answer\"," +
                                   "\"data\":{\"answerId\":$ANSWER_ID}}";
    
    private void parseQuestionAndDoAnswer(String json) {
        try {
            Thread.sleep(7000); //假装正常答题
            doSend(AiAutoAnswer);
            Thread.sleep(2000); //假装正常答题
            
            
            JsonNode jsonNode = objectMapper.readTree(json);
            String text = jsonNode.get("data").toString();
            Question question = objectMapper.readValue(text, Question.class);
            this.currentQuestion = question;
            
            Question.Answer answer = questionData.findCorrectAnswer(question);
            if (answer == null) {
                log.info("{} 没有找到问题 {} 的正确答案", user, question.questionID);
                answer = question.answers.get(0);
            } else {
                log.info("{} 从题库中的正确答案为 {}", user, answer);
            }
            
            doSend(Answer.replace("$ANSWER_ID", answer.answerID));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    // 获取对手信息
    JsonPath corporationNamePath = JsonPathCompiler.compile("$..corporationName");
    
    JsonPath usernamePath = JsonPathCompiler.compile("$..username");
    
    private void getPlayer(String json) {
        String corporationName = SURFER.collectOne(json, String.class, corporationNamePath);
        String username = SURFER.collectOne(json, String.class, usernamePath);
        
        log.info("{} 对手是 {} {}", user, corporationName, username);
    }
    
    // 加入游戏
    private static String JoinMatch = "{\"mcmd\":\"Match\",\"scmd\":\"JoinMatch\"," +
                                      "\"data\":{\"choseStageID\":$ROUND_ID}}";
    
    private void joinMatch() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        doSend(JoinMatch.replace("$ROUND_ID", String.valueOf(nextStage)));
    }
    
    // 检查校验结果
    private static JsonPath validateResult = JsonPathCompiler.compile("$..data");
    
    private boolean isValid(String json) {
        return SURFER.collectOne(json, Boolean.class, validateResult);
    }
    
    // 校验
    private static String validate = "{\"mcmd\":\"PowerMain\",\"scmd\":\"validate\",\"data\":{}}";
    
    private void sendValidateReq() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        doSend(validate);
    }
    
    // 决定下一关
    JsonPath currentStagePath = JsonPathCompiler.compile("$..currentStage");
    
    JsonPath powerPath = JsonPathCompiler.compile("$..power");
    
    JsonPath starCntPath = JsonPathCompiler.compile("$..starCnt");
    
    private void getNextStage(String json) {
        int currentStage = SURFER.collectOne(json, Integer.class, currentStagePath);
        int power = SURFER.collectOne(json, Integer.class, powerPath);
        int startCnt = SURFER.collectOne(json, Integer.class, starCntPath);
        
        if (startCnt >= currentStage || startCnt >= 5) {
            nextStage = currentStage + 1;
        } else {
            nextStage = currentStage;
        }
        if (nextStage > 10) {
            nextStage = 10;
        }
        
        log.info("user {} power {} nextStage {}", user, power, nextStage);
    }
    
    
    JsonPath userIdPath = JsonPathCompiler.compile("$..userId");
    
    // 获取userid
    private void getUserInfo(String json) {
        user.userId = SURFER.collectOne(json, Integer.class, userIdPath);
    }
    
    public boolean isHalt() {
        return System.currentTimeMillis() - lastSend > 1000 * 60;
    }
    
    private void startGame() {
        isGaming = true;
    }
    
    public void quitGame() {
        isGaming = false;
        close();
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("{} 退出游戏", user);
        semaphore.release();
    }
    
    @Override
    public void onError(Exception ex) {
        log.error(ex.getMessage());
//        ex.printStackTrace();
    }
    
    
    private void doSend(String text) {
        log.info("{} Send {}", user, text);
        lastSend = System.currentTimeMillis();
        send(text);
    }
}
