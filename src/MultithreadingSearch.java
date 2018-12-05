import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.wltea.analyzer.lucene.*;

public class MultithreadingSearch {
    public static void main(String[] args) throws IOException {
        Crawler crawler = new Crawler();
        crawler.setThread(10);
        crawler.setBuffer(100);
        crawler.addRoot(304118539);

        crawler.crawl();
        //crawler.index();


    }
}

class Crawler {
    // parameters to control crawling scale
    private int max_threads;
    private int max_questions;

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

    // initialize waiting queue
    public void init(){

    }

    // set the number of crawling thread
    public boolean setThread(int max_threads) {
        if (max_threads > 0) {
            this.max_threads = max_threads;
            return true;
        }
        return false;
    }

    // set the number of questions
    public boolean setBuffer(int max_questions) {
        if (max_questions > this.max_questions) {
            this.max_questions = max_questions;
            return true;
        }
        return false;
    }


    // add the root question
    public boolean addRoot(int root) {
        // if root question had been searched, return false
        if (finished_set.contains(root)) {
            return false;
        }
        // add the first id into the pool
        waiting_queue.add(root);
        return true;
    }

    // start to crawl website
    public void crawl() {
        // if no item in waiting queue, return
        if (waiting_queue.isEmpty()) {
            System.out.println("No specific root question given!");
            return;
        }

        // create search thread
        for (int i = 0; i < max_threads; i++) {
            new CrawlerThread(i, max_questions, pattern, waiting_queue, finished_set);
        }
    }

    Crawler() {
        // initialize object
        this.max_questions = 100;
        this.max_threads = 1;
        waiting_queue = new LinkedList<>();
        finished_set = new HashSet<>();
    }
}


class CrawlerThread extends Thread {
    // identifier
    private int id;
    private int max_questions;

    // reference
    private String[] pattern;
    private LinkedList<Integer> waiting_queue;
    private HashSet<Integer> finished_set;

    // list to store temporary data
    private List<Integer> list;

    CrawlerThread(int thread_id, int max_questions, String[] pattern, LinkedList<Integer> waiting_queue,
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

    private void parseSimilarText(String str) {
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
                e.printStackTrace();
                break;
            }

            // print the question
            System.out.println("Thread " + id + " " + "(" + current_id + "): " + doc.select(pattern[5]).text());

            // reserved for extract two answers
            // TODO: create index for <key = Q, value = A>

            // find more questions
            try {
                doc = Jsoup.connect(pattern[1] + current_id + pattern[2]).ignoreContentType(true).get();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            parseSimilarText(doc.text());

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

class IndexerThread extends Thread{

}
