import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MultithreadSearch {
    public static void main(String[] args) throws IOException {

    }

}

class StartSearch {
    // params to control scale
    private int max_questions;
    private int max_threads;

    // object to store data info
    private LinkedList<Integer> waiting_queue;
    private HashSet<Integer> finished_set;

    // search string pattern
    private static String[] pattern = {
            "https://www.zhihu.com/question/",
            "https://www.zhihu.com/api/v4/questions/",
            "/similar-questions?include=data%5B*%5D.answer_count%2Cauthor%2Cfollower_count&limit=5",
            "#QuestionAnswers-answers > div > div > div:nth-child(2) > div > div:nth-child(",
            ") > div > div.RichContent.RichContent--unescapable > div.RichContent-inner",
            "#root > div > main > div > div:nth-child(11) > div.QuestionHeader > " +
                    "div.QuestionHeader-content > div.QuestionHeader-main > h1"
    };

    StartSearch(int root, int max_questions, int max_threads) {

        // initialize object
        this.max_questions = max_questions;
        this.max_threads = max_threads;
        waiting_queue = new LinkedList<>();
        finished_set = new HashSet<>();

        // add the first id into the pool
        waiting_queue.add(root);

        for (int i = 0; i < max_threads; i++) {

        }

    }
}


class SearchThread extends Thread {
    // reference object
    private int thread_id;
    private int max_questions;
    private String[] pattern;
    private LinkedList<Integer> waiting_queue;
    private HashSet<Integer> finished_set;

    // local object to store data
    private List<Integer> list;

    SearchThread(int thread_id, int max_questions, String[] pattern, LinkedList<Integer> waiting_queue,
                 HashSet<Integer> finished_set) {
        // binding to reference
        this.thread_id = thread_id;
        this.max_questions = max_questions;
        this.pattern = pattern;
        this.waiting_queue = waiting_queue;
        this.finished_set = finished_set;

        // initialize object
        this.list = new ArrayList<>();
    }

    private void getSubUtil(String str) {
        list.clear();
        Pattern pattern = Pattern.compile("\"id\":(.*?),\"title\"");
        Matcher m = pattern.matcher(str);
        while (m.find()) {
            list.add(Integer.parseInt(m.group(1)));
        }
    }

    @Override
    public void run() {
        int current_id;
        while (true) {
            synchronized (finished_set) {
                if (finished_set.size() == max_questions)
                    break;
            }
            synchronized (waiting_queue) {
                if (waiting_queue.size() == 0)
                    continue;
                current_id = waiting_queue.poll();
            }


        }

    }


}
