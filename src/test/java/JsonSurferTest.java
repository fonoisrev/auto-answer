import com.github.fonoisrev.bean.Option;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;

import java.util.Collection;

public class JsonSurferTest {
    
    public static void main(String[] args) {
        JsonSurfer surfer = JsonSurferJackson.INSTANCE;
        
        String json = "{\"questionIdInt\":1," +
                      "\"questionOptionsArray\":[{\"id\":\"a\"," +
                      "\"optionContentStr\":\"正确\",\"optionResultInt\":0}," +
                      "{\"id\":\"b\",\"optionContentStr\":\"错误\"," +
                      "\"optionResultInt\":0}],\"questionResultInt\":0," +
                      "\"questionTitleStr\":\"对于政府特殊安排等需要，需在同等条件下优先考虑的供应商或合作方，应在严格执行相关程序的基础上，做出专项登记备案，供决策参考。\",\"questionTypeInt\":1,\"scoreList\":[160,0,0,0,0],\"timeList\":[2,4,6,7,0],\"totalScore\":160,\"type\":\"Question\",\"verifyCode\":743}";
        Collection<Option> collection =
                surfer.collectAll(json, Option.class, JsonPathCompiler
                        .compile("$.questionOptionsArray.*"));
        
        for (Option object : collection) {
            System.out.println(object.toString());
        }
    }
}
