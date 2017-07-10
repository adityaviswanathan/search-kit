package com.core.lucene.file;
 
import java.io.File;
import java.io.Writer; 
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream; 
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Stack;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.utils.ExtractWikipedia;
import org.apache.lucene.util.IOUtils;  

public class LuceneWriteIndexFromFile
{
    static final int BASE = 10;
    public static void main(String[] args)
    {
        // Build params

        String indexPath = "indexedFiles";
        
        boolean WIKIFLAG = true;
        String wikiData = "/Users/aditya.viswanathan/Data/enwiki-20170420-pages-articles.xml.bz2";
        
        boolean PATHFLAG = false;
        String docsPath = "sample";

        final Path docDir = Paths.get(docsPath);
 
        try
        {
            //org.apache.lucene.store.Directory instance
            Directory dir = FSDirectory.open(Paths.get(indexPath));
             
            //analyzer with the default stop words
            Analyzer analyzer = new StandardAnalyzer();
             
            //IndexWriter Configuration
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
             
            //IndexWriter writes new index files to the directory
            IndexWriter writer = new IndexWriter(dir, iwc);
            
            if(WIKIFLAG) indexWikipedia(writer, indexPath, wikiData);
            if(PATHFLAG) indexDocs(writer, docDir);            
 
            writer.close();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    static String cleanWikiArticle(String body) {
        String s = null;
        String out = "";
        String loc = "./";
        try {
            String[] pythonCleaner = { "python", loc + "WikiExtractor.py", "-r", "\"" + body + "\"" };
            Process p = Runtime.getRuntime().exec(pythonCleaner);

            BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                 InputStreamReader(p.getErrorStream()));

            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                out += " " + s;
            }
            
        } catch (IOException e) {
            System.out.println("exception happened:");
            e.printStackTrace();
            System.exit(-1);
        }
        return out;
    }

    static void indexWikipedia(final IndexWriter writer, String indexPath, String wikipedia) throws IOException {
        File wikiFile = new File(wikipedia);

        Properties properties = new Properties();
        properties.setProperty("content.source.forever", "false"); // will parse each document only once
        properties.setProperty("docs.file", wikiFile.getAbsolutePath());
        properties.setProperty("keep.image.only.docs", "false");
        
        Config c = new Config(properties);
        EnwikiContentSource source = new EnwikiContentSource();
        source.setConfig(c);
        source.resetInputs();// though this does not seem needed, it is (gets the file opened?)
        
        DocMaker docMaker = new DocMaker();
        docMaker.setConfig(c, source);
        docMaker.resetInputs();
        
        int count = 0;
        System.out.println("Starting Indexing of Wikipedia dump " + wikiFile.getAbsolutePath());
        long start = System.currentTimeMillis();
        Document doc;
        File indexDir = new File(indexPath);
        try {
            while((doc = docMaker.makeDocument()) != null) {
                System.out.println(doc.get(DocMaker.TITLE_FIELD));
                System.out.println(doc.get(DocMaker.ID_FIELD));
                System.out.println(doc.get(DocMaker.DATE_FIELD));
                String body = doc.get(DocMaker.BODY_FIELD);

                // annotate article to prepare for Python cleaning step  
                body = body.replaceAll("===.+?===", "");
                body = body.replaceAll("==.+?==", "");
                body = body.replaceFirst("'''.+?'''", "\n$0");
                String prefix = "<page>\n<title>" + doc.get(DocMaker.TITLE_FIELD) + "</title>\n<id>" + doc.get(DocMaker.ID_FIELD) + "</id>\n";
                String suffix = "</page>";

                body = cleanWikiArticle(body);
                createWikipediaDoc(writer, indexDir, doc.get(DocMaker.ID_FIELD), doc.get(DocMaker.TITLE_FIELD), doc.get(DocMaker.DATE_FIELD), body, count);
                ++count;
                if (count % 1000 == 0) System.out.println("Indexed " + count + " documents in " + (System.currentTimeMillis() - start) + " ms");
                if(count == 3) break;
            }
            docMaker.close();
            writer.close();
        } catch(Exception nmd) {
            nmd.printStackTrace();
        }
    }

    static void createWikipediaDoc(IndexWriter writer, File indexFile, String id, String title, String time, String body, int count) { 
        //Create lucene Document
        Document doc = new Document();
         
        doc.add(new StringField("id", id, Field.Store.YES));
        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new TextField("contents", body, Field.Store.YES));

        try {
            writer.addDocument(doc);    
        } catch(Exception e) {
            e.printStackTrace();
        }     
    } 

    static void indexDocs(final IndexWriter writer, Path path) throws IOException 
    {
        //Directory?
        if (Files.isDirectory(path)) 
        {
            //Iterate directory
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() 
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException 
                {
                    try
                    {
                        //Index this file
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } 
                    catch (IOException ioe) 
                    {
                        ioe.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } 
        else
        {
            //Index this file
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }
 
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException 
    {
        try (InputStream stream = Files.newInputStream(file)) 
        {
            //Create lucene Document
            Document doc = new Document();
             
            doc.add(new StringField("path", file.toString(), Field.Store.YES));
            doc.add(new LongPoint("modified", lastModified));
            doc.add(new TextField("contents", new String(Files.readAllBytes(file)), Store.YES));
             
            //Updates a document by first deleting the document(s) 
            //containing <code>term</code> and then adding the new
            //document.  The delete and then add are atomic as seen
            //by a reader on the same index
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
    }
}