package com.github.fonoisrev.run;

import com.github.fonoisrev.bean.Round;
import com.github.fonoisrev.bean.User;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static JsonPath MCMD = JsonPathCompiler.compile("$..mcmd");
    
    private static JsonPath SCMD = JsonPathCompiler.compile("$..scmd");
    
    private final CountDownLatch latch;
    
    private User user;
    
    /**
     * 关卡
     */
    private Round round;
    
    public MyWebSocketClient(URI serverUri, Draft protocolDraft, User user, CountDownLatch latch) {
        super(serverUri, protocolDraft);
        this.user = user;
        this.latch = latch;
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        sendAccountLogonReq();
    }
    
    
    @Override
    public void onMessage(String json) {
        
        if (json.equals("{\"mcmd\":\"Sys\",\"scmd\":\"Heart\"}")) {
            // heart beat response
            doSend("{\"mcmd\":\"Sys\",\"scmd\":\"Heart\"}");
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
                LOGGER.info("ERROR! {} has no available ROUND!", user);
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
        }
//        else if (mcmd.equalsIgnoreCase("PKMain")
//                   && scmd.equalsIgnoreCase("MapInfoResult")) {
//            sendNextQuestionReq();
//        }
        else if (mcmd.equalsIgnoreCase("PKMain")
                 && scmd.equalsIgnoreCase("QuestionResult")) {
            doAnswer(json);
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("AnswerStepsResult")) {
            if (hasNextSteps(json)) {
                sendNextQuestionReq();
            } else {
                // nothing
            }
        } else if (mcmd.equalsIgnoreCase("PKMain")
                   && scmd.equalsIgnoreCase("Statement")) {
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
    
    private void doAnswer(String question) {
        doSend(AiAutoAnswer);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        String answerId = SURFER.collectOne(question, String.class, correctAnswer);
        doSend(Answer.replace("$ANSWER_ID", answerId));
    }
    
    private void doSend(String text) {
        LOGGER.info("{} Send {}", user, text);
        send(text);
    }
    
    private void quitGame() {
        LOGGER.info("{} Quit Game", user);
        close();
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        latch.countDown();
    }
    
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
}
