import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.wltea.analyzer.lucene.*;

public class LuceneTest {
    public static void main(String[] args) {
        LuceneTest w = new LuceneTest();
        String file_path = "C:/index";
        //w.createIndex(file_path);
        w.search(file_path);
    }

    public void createIndex(String file_path) {
        File file = new File(file_path);
        IndexWriter index_writer = null;
        try {
            Directory dir = FSDirectory.open(file);
            Analyzer analyzer = new IKAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            index_writer = new IndexWriter(dir, config);
            Document doc = getDocument();
            index_writer.addDocument(doc);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            index_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Document getDocument() {
        Document doc = new Document();
        Field f1 = new TextField("name", "XXX", Field.Store.YES);
        Field f2 = new TextField("pic", "梵高是一名画家", Field.Store.YES);
        Field f3 = new TextField("grade", "GPA 1.5", Field.Store.YES);
        Field f4 = new TextField("address", "浙江大学玉泉校区", Field.Store.YES);
        Field f5 = new StringField("id", "3160101309", Field.Store.YES);
        doc.add(f1);
        doc.add(f2);
        doc.add(f3);
        doc.add(f4);
        doc.add(f5);
        return doc;
    }

    public void search(String filePath) {
        File f = new File(filePath);
        try {
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(f)));
            String queryStr = "XXX";
            Analyzer analyzer = new IKAnalyzer();
            //指定field为“name”，Lucene会按照关键词搜索每个doc中的name。
            QueryParser parser = new QueryParser(Version.LUCENE_4_10_4, "name", analyzer);
            Query query = parser.parse(queryStr);
            TopDocs hits = searcher.search(query, 1);//前面几行代码也是固定套路，使用时直接改field和关键词即可
            for (ScoreDoc doc : hits.scoreDocs) {
                Document d = searcher.doc(doc.doc);
                System.out.println(d.get("address"));
                System.out.println(d.get("id"));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
