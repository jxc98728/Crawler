import java.io.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.wltea.analyzer.lucene.*;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

public class MultithreadingSearch {
    public static void main(String[] args) throws IOException {
        // TODO: 加一个GUI模块，点击对应的按键执行相应的功能
        Crawler crawler = new Crawler("C:/index");
        crawler.init();
        crawler.setThreadSize(5);
        crawler.setBufferSize(20);
        crawler.crawl();
        crawler.search("浙江");
        crawler.exit();
    }
}

class Crawler {
    // parameters to control crawling scale
    private String file_path;
    private int max_threads;
    private int max_questions;


    // object to store data info
    private IndexWriter indexWriter;
    private LinkedList<Integer> waiting_queue;
    private HashSet<Integer> finished_set;

    // search string pattern
    private static String[] pattern = {
            "https://www.zhihu.com/question/",
            "https://www.zhihu.com/api/v4/questions/",
            "/similar-questions?include=data%5B*%5D.answer_count%2Cauthor%2Cfollower_count&limit=5",
            "#QuestionAnswers-answers > div > div > div:nth-child(2) > div > div:nth-child(",
            ") > div > div.RichContent.RichContent--unescapable > div.RichContent-inner > span",
            "#root > div > main > div > div:nth-child(11) > div.QuestionHeader > " +
                    "div.QuestionHeader-content > div.QuestionHeader-main > h1"
    };

    // initialize waiting queue
    public void init() {
        // read finished set data from file
        // TODO: 爬取过的ID应该保存到文件中
        File saved_ID = new File(file_path + "/saved_ID.dat");
        if (saved_ID.exists()) {
            try {
                ObjectInputStream input = new ObjectInputStream(new FileInputStream(file_path + "/saved_ID.dat"));
                finished_set = (HashSet<Integer>) (input.readObject());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            finished_set = new HashSet<>();
        }

        // initialize waiting queue
        Document doc;
        try {
            doc = Jsoup.connect("https://www.zhihu.com/explore").get();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Pattern pattern = Pattern.compile("link\" href=\"/question/(.*?)/answer");
        Matcher m = pattern.matcher(doc.toString());
        while (m.find()) {
            waiting_queue.add(Integer.parseInt(m.group(1)));
        }
        // insert ID manually
//        waiting_queue.add(304439740);
//        waiting_queue.add(304357397);
//        waiting_queue.add(544258238);
//        waiting_queue.add(544165647);
//        waiting_queue.add(544061865);

        // initialize index writer
        File file = new File(this.file_path + "/index");
        try {
            Directory dir = FSDirectory.open(file);
            Analyzer analyzer = new IKAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            indexWriter = new IndexWriter(dir, config);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exit() {
        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file_path + "/saved_ID.dat"));
            output.writeObject(finished_set);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // set the number of crawling thread
    public boolean setThreadSize(int max_threads) {
        if (max_threads > 0) {
            this.max_threads = max_threads;
            return true;
        }
        return false;
    }

    // set the number of questions
    public boolean setBufferSize(int max_questions) {
        if (max_questions > this.max_questions) {
            this.max_questions = max_questions;
            return true;
        }
        return false;
    }

    // add the question ID
    public boolean addID(int root) {
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
        ExecutorService threads = Executors.newFixedThreadPool(max_threads);
        for (int i = 0; i < max_threads; i++) {
            threads.execute(new CrawlerThread(i, max_questions, pattern, waiting_queue, finished_set, indexWriter));
        }
        threads.shutdown();
        while (true) {
            if (threads.isTerminated()) {
                System.out.println("All threads exit.");
                try {
                    indexWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void search(String queryStr) {
        File f = new File(this.file_path + "/index");
        try {
            //if (f.exists()) {
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(f)));
            Analyzer analyzer = new IKAnalyzer();
            QueryParser parser = new QueryParser(Version.LUCENE_4_10_4, "question", analyzer);
            Query query = parser.parse(queryStr);
            TopDocs hits = searcher.search(query, 100);
            for (ScoreDoc doc : hits.scoreDocs) {
                org.apache.lucene.document.Document d = searcher.doc(doc.doc);
                System.out.println(d.get("question"));
                System.out.println(d.get("answer1"));
                System.out.println(d.get("answer2"));
                System.out.println("-----------------------------------------");
            }
            //}
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }


    Crawler(String file_path) {
        // initialize object
        this.file_path = file_path;
        this.max_questions = 10;
        this.max_threads = 1;
        waiting_queue = new LinkedList<>();
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
    private IndexWriter indexWriter;

    // list to store temporary data
    private List<Integer> list;

    CrawlerThread(int thread_id, int max_questions, String[] pattern, LinkedList<Integer> waiting_queue,
                  HashSet<Integer> finished_set, IndexWriter indexWriter) {
        // binding reference
        this.id = thread_id;
        this.max_questions = max_questions;
        this.pattern = pattern;
        this.waiting_queue = waiting_queue;
        this.finished_set = finished_set;
        this.indexWriter = indexWriter;

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
        org.apache.lucene.document.Document record;
        Field f1, f2, f3;
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

            // create a record
            record = new org.apache.lucene.document.Document();

            // print the question
            // extract two answers
            System.out.println("Thread " + id + " " + "(" + current_id + "): " + doc.select(pattern[5]).text());
            f1 = new TextField("question", doc.select(pattern[5]).text(), Field.Store.YES);
            f2 = new TextField("answer1", doc.select(pattern[3] + "1" + pattern[4]).text(), Field.Store.YES);
            f3 = new TextField("answer2", doc.select(pattern[3] + "2" + pattern[4]).text(), Field.Store.YES);
            record.add(f1);
            record.add(f2);
            record.add(f3);
            synchronized (indexWriter) {
                try {
                    indexWriter.addDocument(record);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

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
