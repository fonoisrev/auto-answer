package com.github.fonoisrev;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fonoisrev.bean.Question;
import com.github.fonoisrev.bean.Round;
import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionData;
import org.jsfr.json.JacksonParser;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

public class JsonSurferTest {
    
    static JsonSurfer surfer = JsonSurferJackson.INSTANCE;
    static ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    
    @Test
    public void test() {
        String json = "{\"data\":{\"chapterDtoList\":[{\"chapterId\":1,\"end\":false," +
                      "\"medalDesc\":\"1978年至1982年全部关卡满星后可获得此徽章。\",\"medalStatus\":false," +
                      "\"roundList\":[{\"lock\":false,\"roundId\":1,\"roundName\":\"1978年\"," +
                      "\"starNum\":2,\"userStarNum\":0},{\"lock\":false,\"roundId\":2," +
                      "\"roundName\":\"1979年\",\"starNum\":2,\"userStarNum\":0},{\"lock\":false," +
                      "\"roundId\":3,\"roundName\":\"1980年\",\"starNum\":2,\"userStarNum\":0}," +
                      "{\"lock\":false,\"roundId\":4,\"roundName\":\"1981年\",\"starNum\":2," +
                      "\"userStarNum\":0},{\"lock\":false,\"roundId\":5,\"roundName\":\"1982年\"," +
                      "\"starNum\":2,\"userStarNum\":0}]},{\"chapterId\":2,\"end\":false," +
                      "\"medalDesc\":\"1983年至1987年全部关卡满星后可获得此徽章。\",\"medalStatus\":false," +
                      "\"roundList\":[{\"lock\":false,\"roundId\":6,\"roundName\":\"1983年\"," +
                      "\"starNum\":2,\"userStarNum\":0},{\"lock\":false,\"roundId\":7," +
                      "\"roundName\":\"1984年\",\"starNum\":2,\"userStarNum\":0},{\"lock\":false," +
                      "\"roundId\":8,\"roundName\":\"1985年\",\"starNum\":2,\"userStarNum\":0}," +
                      "{\"lock\":true,\"roundId\":9,\"roundName\":\"1986年\",\"starNum\":2," +
                      "\"userStarNum\":0},{\"lock\":true,\"roundId\":10,\"roundName\":\"1987年\"," +
                      "\"starNum\":2,\"userStarNum\":0}]}],\"userInfoDto\":{\"power\":120," +
                      "\"recoverLeftTimestamp\":0}},\"mcmd\":\"TmMain\"," +
                      "\"scmd\":\"TmListSuccess\"}";
        
        JsonPath path = JsonPathCompiler.compile("$..roundList[*]");
        Collection<Round> rounds = surfer.collectAll(json, Round.class, path);
        System.out.println(rounds);
    }
    
    @Test
    public void test2() throws IOException {
        String json = "{\"data\":{\"corporationId\":39,\"corporationName\":\"信息技术公司\"," +
                      "\"isFirstLogin\":\"0\",\"photoId\":6,\"pkRate\":\"0\",\"roundIndex\":1," +
                      "\"roundName\":\"1978年\",\"uid\":\"1310627\",\"userId\":29832," +
                      "\"userName\":\"吴航\",\"winCnt\":0},\"mcmd\":\"Account\"," +
                      "\"scmd\":\"LogonSuccess\"}";

        JsonNode jsonNode = objectMapper.readTree(json);
        String text = jsonNode.get("data").toString();
        User user = objectMapper.readValue(text, User.class);
        System.out.println(user);
    }
    
    @Test
    public void test3() throws IOException {
        //language=JSON
        String json = "{\"data\":{\"answers\":[{\"answerId\":2779,\"content\":\"《中日联合宣言》\"," +
                      "\"isCorrect\":0},{\"answerId\":2780,\"content\":\"《中日建交条约》\"," +
                      "\"isCorrect\":0},{\"answerId\":2777,\"content\":\"《中日和平友好条约》\"," +
                      "\"isCorrect\":0},{\"answerId\":2778,\"content\":\"《中日联合声明》\"," +
                      "\"isCorrect\":0}],\"backSteps\":1," +
                      "\"content\":\"1978年8月12日，中国外交部长黄华与日本外相园田直在北京正式签订( )。\"," +
                      "\"correctAnswer\":0,\"forwardSteps\":4,\"label\":\"6\"," +
                      "\"pictureIndex\":\"\",\"questionId\":734},\"mcmd\":\"PKMain\"," +
                      "\"scmd\":\"QuestionResult\"}";
    
        JsonNode jsonNode = objectMapper.readTree(json);
        String text = jsonNode.get("data").toString();
        Question question = objectMapper.readValue(text, Question.class);
        System.out.println(question);
    }
}
