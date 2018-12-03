import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Main {
    private static String url_pattern = "https://www.zhihu.com/question/";
    private static String similar_url_pattern1 = "https://www.zhihu.com/api/v4/questions/";
    private static String similar_url_pattern2 = "/similar-questions?include=data%5B*%5D.answer_count%2Cauthor%2Cfollower_count&limit=5";
    private static String selector_pattern1 = "#QuestionAnswers-answers > div > div > div:nth-child(2) > div > div:nth-child(";
    private static String selector_pattern2 = ") > div > div.RichContent.RichContent--unescapable > div.RichContent-inner";

    public static List<String> getSubUtil(String str, String regex) {
        List<String> list = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }


    public static void main(String[] args) throws IOException {
        String url_ID = args[0];
        String url = url_pattern + url_ID;
        Document doc = Jsoup.connect(url).get();
        Elements[] answer = new Elements[5];

        String similar_url = similar_url_pattern1 + url_ID + similar_url_pattern2;
        Document similar_doc = Jsoup.connect(similar_url).ignoreContentType(true).get();
        String similar_data = similar_doc.text();
        List<String> similar_ID = getSubUtil(similar_data, "\"id\":(.*?),\"title\"");

        // parse selector
        String[] selector = new String[5];

        // Print the content
        for (int i = 0; i < 5; ++i) {
            selector[i] = selector_pattern1 + (i + 1) + selector_pattern2;
            answer[i] = doc.select(selector[i]);
            if (answer[i].hasText())
                System.out.println(answer[i].text());
        }

        // Print the related question ID
        for (int i = 0; i < similar_ID.size(); ++i) {
            System.out.println(similar_ID.get(i));
        }
    }

}
