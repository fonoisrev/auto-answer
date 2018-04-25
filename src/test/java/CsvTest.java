import com.github.fonoisrev.data.QuestionsData;
import com.github.fonoisrev.data.UserData;

import java.io.IOException;

public class CsvTest {
    
    public static void main(String[] args) throws IOException {
        QuestionsData data = new QuestionsData();
        data.init();
    
        UserData userData = new UserData();
        userData.init();
        
        data.addQuestion("abcd", "efg");
        data.doSave();
    }
}
