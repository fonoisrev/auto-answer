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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

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
    String proxyIp;
    
    @Value("${proxy.port:0}")
    Integer proxyPort;
    
    List<MyWebSocketClient> games = new ArrayList<>();
    
    @Override
    public void run(String... args) throws Exception {
        int concurrency = 5;
        Semaphore s = new Semaphore(concurrency);// 5个用户同时
        
        List<User> users = userData.getUsers();
        
        for (int i = 0; i < users.size(); ++i) {
            s.acquire();
            
            MyWebSocketClient client = null;
            User user = users.get(i);
            URI uri = new URI("ws://bath5.mggame.com.cn/wsqcxxd");
            client = new MyWebSocketClient(
                    uri, new Draft_6455(), user, questionData, s);
            if (!StringUtils.isEmpty(proxyIp) && proxyPort != 0) {
                client.setProxy(new Proxy(
                        Type.HTTP, new InetSocketAddress(proxyIp, proxyPort)));
            }
            client.connect();
            games.add(client);
            Thread.sleep(3000); // 避免相遇
        }
        
        for (int i = 0; i < concurrency; i++) {
            s.acquire();
        }
    }
    
    
    @Scheduled(fixedRate = 10000)
    public void haltCheck() {
        Iterator<MyWebSocketClient> iterator = games.iterator();
        for (; iterator.hasNext(); ) {
            MyWebSocketClient game = iterator.next();
            if (game.isHalt()) {
                LOGGER.info("{} 失去响应...尝试关闭", game.user);
                game.quitGame();
                iterator.remove();
            } else if (game.isClosed()) {
                iterator.remove();
            }
        }
    }
}
