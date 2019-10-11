import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnimalRec {
    private Rule currentUseRule;
    private int needMatchTruthNum = 0;
    private String[] inputs;
    private Set<Rule> usedRules = new HashSet<Rule>();
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private ArrayList<Rule> unusedRules = new ArrayList<Rule>();
    private ArrayList<Rule> availableRules = new ArrayList<Rule>();
    private TruthCollection truthCollection = new TruthCollection();
    private RulesCollection rulesCollection;
    private Logger logger;

    public AnimalRec() throws IOException {
        this.run();
    }

    public void insertRule(Rule rule) {

//        mongoDatabase.getCollection("rules").drop();
        MongoCollection<Document> rulesCollection = mongoDatabase.getCollection("rules");

        rulesCollection.createIndex(Document.parse("{\"md5\":1}，{\"unique\":true}"));
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(rule);
//            rulesCollection.insertOne(Document.parse(json));
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
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
        try {
            // 连接到 mongodb 服务
            mongoClient = MongoClients.create();
            // 连接到数据库
            mongoDatabase = mongoClient.getDatabase("animalRec");
            System.out.println("Connect to database successfully");

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

    }

    private void initRules() {
        rulesCollection = new RulesCollection();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("rules.txt"));
            String s = "";
            while ((s = bufferedReader.readLine()) != null) {
                String[] split = s.split("[:^→]");
                if (split.length < 3 || split[0].startsWith("#"))
                    continue;
                Rule rule = new Rule(MD5.getMD5(s), split);
                unusedRules.add(rule);
                insertRule(rule);
                rulesCollection.rules.add(rule);
                truthCollection.unUsedRules.add(rule);
                for (int i = 1; i < split.length - 1; i++)
                    rulesCollection.keyWords.add(split[i]);
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
        inputs = new String[]{"产奶", "有黑色条纹", "反刍"};
        needMatchTruthNum = inputs.length;
        for (String s : inputs) {
            if (!rulesCollection.keyWords.contains(s)) {
                System.out.println("关键字: " + s + " 不符合要求或未定义");
                break;
            }
            truthCollection.truthKeyWord.add(s);
        }
    }

    private boolean updateUnusedRules() {
        if (truthCollection.unUsedRules.size() <= 0)
            return false;
        return true;
    }

    private boolean matchUnusedAndTruth() {
        ArrayList<WRule> wRules = new ArrayList<WRule>();
        Set<Rule> unusedRules = truthCollection.unUsedRules;
        Iterator<Rule> iterator = unusedRules.iterator();
        while (iterator.hasNext()) {
            WRule next = new WRule(iterator.next());
            for (String s : next.rule.requires) {
                if (truthCollection.truthKeyWord.contains(s))
                    next.score += 1;
            }
            if (next.score == next.rule.requires.size())
                wRules.add(next);
        }
        if (wRules.size() <= 0)
            return false;
        Collections.sort(wRules, new Comparator<WRule>() {
            public int compare(WRule o1, WRule o2) {
                return o2.score - o1.score;
            }
        });
        availableRules.add(wRules.get(0).rule);
        return true;
    }

    private boolean updateAvailableRules() {
        return false;
    }

    private void inference() {
        for (int i = 0; i < availableRules.size(); i++) {
            Rule rule = availableRules.get(i);
            System.out.println("Use:" + rule);
            truthCollection.unUsedRules.remove(rule);
            truthCollection.truthKeyWord.add(rule.results.get(0));
            truthCollection.truths.add(new Truth(rule.index, rule.results.get(0)));
            unusedRules.remove(rule);
            usedRules.add(rule);
            availableRules.remove(rule);// ? bug
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
