package com.github.fonoisrev;

import com.github.fonoisrev.data.QuestionData;
import com.github.fonoisrev.data.UserData;
import com.github.fonoisrev.run.MyRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

@SpringBootApplication
//@EnableScheduling
public class AutoAnswer {
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(AutoAnswer.class).web(false).run(args);
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
    public QuestionData questionData() {
        return new QuestionData();
    }
    
    
    @Value("${proxy.ip}")
    String proxyIp;
    
    @Value("${proxy.port:0}")
    Integer proxyPort;
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(60_000);
        if (!StringUtils.isEmpty(proxyIp) && proxyPort != 0) {
            factory.setProxy(new Proxy(Type.HTTP,
                                       new InetSocketAddress(proxyIp,
                                                             proxyPort)));
        }
        return new RestTemplate(factory);
    }
}
