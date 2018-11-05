package com.github.fonoisrev.run;

import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionData;
import com.github.fonoisrev.data.UserData;
import org.java_websocket.drafts.Draft_6455;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MyRunner implements CommandLineRunner {
    
    /** logger */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MyRunner.class);
    
    @Autowired
    UserData userData;
    
    @Autowired
    QuestionData questionData;
    
    @Autowired
    RestTemplate template;
    
    @Value("${proxy.ip}")
    String proxyIp = "";
    
    @Value("${proxy.port}")
    Integer proxyPort = 0;
    
    @Override
    public void run(String... args) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        List<User> users = userData.getUsers();
        CountDownLatch latch = new CountDownLatch(users.size());
        
//        Master master = new Master();
        
        for (User user : users) {
//            Runnable game = new Runnable() {
//
//                @Override
//                public void run() {
//
                    MyWebSocketClient client = null;
//                    try {
                        URI uri = new URI("ws://bath5.mggame.com.cn/wshscf");
                        client = new MyWebSocketClient(
                                uri, new Draft_6455(), user, questionData, latch);
                        if (!StringUtils.isEmpty(proxyIp) && proxyPort != 0) {
                            client.setProxy(new Proxy(
                                    Type.HTTP, new InetSocketAddress(proxyIp, proxyPort)));
                        }
//                        master.addGame(client);
                        client.connect();
//                    } catch (URISyntaxException e) {
//                        e.printStackTrace();
//                    }
//                }
//            };
//            threadPool.submit(game);
            
        }
    
//        ScheduledExecutorService haltDetect =
//                Executors.newSingleThreadScheduledExecutor();
//        haltDetect.scheduleAtFixedRate(master, 20, 10, TimeUnit.SECONDS);
        latch.await();
    }
    
    private static class Master implements Runnable {
        
        List<MyWebSocketClient> games = new ArrayList<>();
        
        public void addGame(MyWebSocketClient game) {
            games.add(game);
        }
        
        
        @Override
        public void run() {
            LOGGER.info("正检查是否失去响应..");
            for (MyWebSocketClient game : games) {
                if (game.isHalt()) {
                    game.quitGame();
                    games.remove(game);
                }
            }
        }
    }
}
