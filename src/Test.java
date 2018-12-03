import java.io.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class Test {
    public static void main(String[] args)
    {
        try {
            Document document = Jsoup.connect("https://www.baidu.com").get();
            System.out.println((document).title());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
