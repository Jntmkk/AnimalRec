import java.util.ArrayList;
import java.util.List;

public class Rule {
    public String md5 = "";
    public ArrayList<String> requires = new ArrayList<String>();
    public ArrayList<String> results = new ArrayList<String>();
    public String index;

    public Rule(String md5, String[] strings) {
        this.md5 = md5;
        int len = strings.length;
        this.index = strings[0];
        results.add(strings[len - 1]);
        for (int i = 1; i < len - 1; i++) {
            requires.add(strings[i]);
        }
    }

    @Override
    public String toString() {
        return index + ":" + requires + "->" + results;
    }
}
