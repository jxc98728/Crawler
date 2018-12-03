import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Main {
    public static void main(String[] args) throws IOException {
        Search s = new Search(304118539, 20);
    }
}

class Search {
    private HashSet<Integer> set;
    private Hashtable<Integer, Integer> Set;

    private static String url_pattern = "https://www.zhihu.com/question/";
    private static String similar_url_pattern1 = "https://www.zhihu.com/api/v4/questions/";
    private static String similar_url_pattern2 = "/similar-questions?include=data%5B*%5D.answer_count%2Cauthor%2Cfollower_count&limit=5";
    private static String selector_pattern1 = "#QuestionAnswers-answers > div > div > div:nth-child(2) > div > div:nth-child(";
    private static String selector_pattern2 = ") > div > div.RichContent.RichContent--unescapable > div.RichContent-inner";
    private static String question_selector = "#root > div > main > div > div:nth-child(11) > div.QuestionHeader > div.QuestionHeader-content > div.QuestionHeader-main > h1";

    public Search(int q_id, int recursion_level) throws IOException {
        set = new HashSet<>();
        Set = new Hashtable<>();
        recursive_search(q_id, recursion_level);
    }


    private static List<Integer> getSubUtil(String str) {
        List<Integer> list = new ArrayList<Integer>();
        Pattern pattern = Pattern.compile("\"id\":(.*?),\"title\"");
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            list.add(Integer.parseInt(m.group(1)));
        }
        return list;
    }

    private void recursive_search(int q_id, int recursion_level) throws IOException {
        if (recursion_level == 0)
            return;
        if (set.contains(q_id))
            return;
        set.add(q_id);
        String url = url_pattern + q_id;
        String similar_url = similar_url_pattern1 + q_id + similar_url_pattern2;

        Document doc = Jsoup.connect(url).get();
        Document similar_doc = Jsoup.connect(similar_url).ignoreContentType(true).get();

        String similar_data = similar_doc.text();
        List<Integer> similar_ID = getSubUtil(similar_data);

        // Print the question
        System.out.println(doc.select(question_selector).text());

        // Recursive search
        for (int similar_id : similar_ID) {
            recursive_search(similar_id, recursion_level - 1);
        }
    }
}
