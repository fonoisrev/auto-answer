package com.github.fonoisrev;

import com.github.fonoisrev.bean.Round;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.junit.Test;

import java.util.Collection;

public class JsonSurferTest {
    
    JsonSurfer surfer  = JsonSurferJackson.INSTANCE;
    
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
                      "\"recoverLeftTimestamp\":0}},\"mcmd\":\"TmMain\",\"scmd\":\"TmListSuccess\"}";
    
        JsonPath path = JsonPathCompiler.compile("$..roundList[*]");
    
        Collection<Round> rounds = surfer.collectAll(json, Round.class, path);
        System.out.println(rounds);
    }
}
