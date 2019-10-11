import com.sun.org.apache.bcel.internal.generic.NEW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RulesCollection {
    public Set<String> keyWords = new HashSet<String>();
    public ArrayList<Rule> rules = new ArrayList<Rule>();
}
