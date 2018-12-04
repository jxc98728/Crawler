import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MultithreadingSearch {
    public static void main(String[] args) throws IOException {
        StartSearch s = new StartSearch(21638597, 50, 10);
    }

}

class StartSearch {
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
        waiting_queue = new LinkedList<>();
        finished_set = new HashSet<>();

        // add the first id into the pool
        waiting_queue.add(root);

        for (int i = 0; i < max_threads; i++) {
            new SearchThread(i, max_questions, pattern, waiting_queue, finished_set);
        }

    }
}


class SearchThread extends Thread {
    // identifier
    private int id;
    private int max_questions;

    // reference
    private String[] pattern;
    private LinkedList<Integer> waiting_queue;
    private HashSet<Integer> finished_set;

    // list to store temporary data
    private List<Integer> list;

    SearchThread(int thread_id, int max_questions, String[] pattern, LinkedList<Integer> waiting_queue,
                 HashSet<Integer> finished_set) {
        // binding reference
        this.id = thread_id;
        this.max_questions = max_questions;
        this.pattern = pattern;
        this.waiting_queue = waiting_queue;
        this.finished_set = finished_set;

        // initialize object
        this.list = new ArrayList<>();

        // run
        start();
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
        Document doc;
        while (true) {
            synchronized (finished_set) {
                if (finished_set.size() == max_questions)
                    break;
            }
            synchronized (waiting_queue) {
                if (waiting_queue.size() == 0)
                    continue;
                current_id = waiting_queue.removeFirst();
            }
            synchronized (finished_set) {
                if (finished_set.contains(current_id))
                    continue;
                finished_set.add(current_id);
            }

            // access the Internet
            try {
                doc = Jsoup.connect(pattern[0] + current_id).get();
            } catch (IOException e) {
                System.err.println("IOException!");
                break;
            }

            // print the question
            System.out.println("Thread " + id + " " + "(" + current_id + "): " + doc.select(pattern[5]).text());

            // reserved for extract answer
            // TODO: create index for <key = Q, value = A>

            // find more questions
            try {
                doc = Jsoup.connect(pattern[1] + current_id + pattern[2]).ignoreContentType(true).get();
            } catch (IOException e) {
                System.err.println("IOException!");
                break;
            }
            getSubUtil(doc.text());

            // add new item to pool
            for (int similar_id : list) {
                synchronized (finished_set) {
                    if (finished_set.size() == max_questions)
                        break;
                    if (finished_set.contains(similar_id))
                        continue;
                    synchronized (waiting_queue) {
                        waiting_queue.add(similar_id);
                    }
                }
            }
        }
    }
}
