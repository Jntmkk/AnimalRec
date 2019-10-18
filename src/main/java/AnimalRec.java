
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnimalRec {
    private Set<Rule> unUsedRules = new HashSet<Rule>();
    private Set<Rule> usedRules = new HashSet<Rule>();
    private Set<String> truths = new HashSet<String>();
    private ArrayList<Truth> truthsCollection = new ArrayList<Truth>();
    private Rule currentUseRule;
    private int needMatchTruthNum = 0;
    private ArrayList<Rule> matchedRules = new ArrayList<Rule>();
    //    private ArrayList<Rule> availableRules = new ArrayList<Rule>();
    private Set<String> keywords = new HashSet<String>();
    private String[] inputs;
    private Logger logger;

    public AnimalRec() throws IOException {
        this.run();
    }

    public void insertRule(Rule rule) {

    }

    private void run() throws IOException {
        init();
        initRules();
        initComprehensiveDB();
        while (true) {
            if (!updateUnusedRules()) {
                processOver(0);
                break;
            }
            if (!matchUnusedAndTruth()) {
                acquireUser();
                continue;
            }
            inference();
            if (checkHasAnswer()) {
                processOver(1);
                break;
            }
        }

    }

    private void init() {
        logger = Logger.getLogger("jntm.cf");
        logger.setLevel(Level.WARNING);
    }

    private void initRules() {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("rules.txt"));
            String s = "";
            while ((s = bufferedReader.readLine()) != null) {
                String[] split = s.split("[:^→]");
                if (split.length < 3 || split[0].startsWith("#"))
                    continue;
                Rule rule = new Rule(MD5.getMD5(s), split);
                unUsedRules.add(rule);
                insertRule(rule);
                unUsedRules.add(rule);
                for (int i = 1; i < split.length - 1; i++)
                    keywords.add(split[i]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initComprehensiveDB() {
        /**
         * 模拟用户输入
         */
        inputs = new String[]{"有羽毛", "会游泳", "黑白二色"};
        needMatchTruthNum = inputs.length;
        for (String s : inputs) {
            if (!keywords.contains(s)) {
                System.out.println("关键字: " + s + " 不符合要求或未定义");
                break;
            } else {
                truthsCollection.add(new Truth("input", s));
                truths.add(s);
            }

        }
    }

    private boolean updateUnusedRules() {
        if (unUsedRules.size() <= 0)
            return false;
        return true;
    }

    private boolean matchUnusedAndTruth() {
        boolean flag = false;
        ArrayList<WRule> wRules = new ArrayList<WRule>();
        Set<Rule> unusedRules = unUsedRules;
        Iterator<Rule> iterator = unusedRules.iterator();
        while (iterator.hasNext()) {
            WRule next = new WRule(iterator.next());
            for (String s : next.rule.requires) {
                if (truths.contains(s))
                    next.score += 1;
            }
            if (next.score == 0)
                continue;
            next.score = next.score * 100 / next.rule.requires.size();
//            if (next.score == next.rule.requires.size())

            wRules.add(next);

        }


        Collections.sort(wRules, new Comparator<WRule>() {
            public int compare(WRule o1, WRule o2) {
                return o2.score - o1.score;
            }
        });
//        availableRules.add(wRules.get(0).rule);

        Iterator<WRule> wRuleListIterator = wRules.iterator();
        while (wRuleListIterator.hasNext()) {
            WRule wRule = wRuleListIterator.next();
            if (wRule.score == 100) {
                flag = true;
                continue;
            }
            if (wRule.score < 100 && flag)
                wRuleListIterator.remove();
        }

        for (WRule wRule : wRules) {
            matchedRules.add(wRule.rule);
        }
        if (!flag)
            return false;
        return true;
    }

    private boolean updateAvailableRules() {
        return false;
    }

    private void inference() {
        Iterator<Rule> iterator = matchedRules.iterator();
        while (iterator.hasNext()) {
            Rule rule = iterator.next();
            System.out.println("Use:" + rule);
            unUsedRules.remove(rule);
            keywords.add(rule.results.get(0));
            truthsCollection.add(new Truth(rule.index, rule.results.get(0)));
            truths.add(rule.results.get(0));
            iterator.remove();
            needMatchTruthNum -= (rule.requires.size() - rule.results.size());
            currentUseRule = rule;
        }


    }

    private boolean checkHasAnswer() {
//        currentUseRule.requires.size() == (needMatchTruthNum + currentUseRule.requires.size() + 1)
        if (needMatchTruthNum == 1) {
//            HashSet<String> strings = new HashSet<String>(Arrays.asList(inputs));
//            for (String s : currentUseRule.requires) {
//                if (!strings.contains(s))
//                    return false;
//            }
            System.out.println("Result from rule:" + currentUseRule + "\n result:" + currentUseRule.results.get(0));
            return true;
        }


        return false;
    }

    private void acquireUser() {
        System.out.println("Acquire");
        System.exit(0);
    }

    private void processOver(int type) {
        if (type == 0)
            System.out.println("无解 over!");
        if (type == 1)
            System.out.println("得出结果！");
    }


    public static void main(String[] args) throws IOException {
        new AnimalRec();
    }
}
