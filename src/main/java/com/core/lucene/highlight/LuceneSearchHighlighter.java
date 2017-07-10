package com.core.lucene.highlight;

import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

class WindowFragmenter implements Fragmenter {
  private static final int DEFAULT_FRAGMENT_SIZE = 100;
  // private int currentNumFrags;
  private int currentWindowSize;
  // private int fragmentSize;
  private int wordWindowSize;
  private OffsetAttribute offsetAtt;

  public WindowFragmenter() {
    this(10);
  }

  /**
   * 
   * @param wordWindowSize size in number of surrounding words of each fragment
   */
  public WindowFragmenter(int wordWindowSize) {
    // this.fragmentSize = fragmentSize;
    this.wordWindowSize = wordWindowSize;
  }


  /* (non-Javadoc)
   * @see org.apache.lucene.search.highlight.Fragmenter#start(java.lang.String, org.apache.lucene.analysis.TokenStream)
   */
  @Override
  public void start(String originalText, TokenStream stream) {
    offsetAtt = stream.addAttribute(OffsetAttribute.class);
    // currentNumFrags = 1;
  }


  /* (non-Javadoc)
   * @see org.apache.lucene.search.highlight.Fragmenter#isNewFragment()
   */
  @Override
  public boolean isNewFragment() {
    boolean isNewFrag = currentWindowSize >= wordWindowSize ? true : false;
    if (isNewFrag) currentWindowSize = 0;
    else currentWindowSize++;
    return isNewFrag;
  }

  // /**
  //  * @return size in number of characters of each fragment
  //  */
  // public int getFragmentSize() {
  //   return fragmentSize;
  // }

  // /**
  //  * @param size size in characters of each fragment
  //  */
  // public void setFragmentSize(int size) {
  //   fragmentSize = size;
  // }

}
 
public class LuceneSearchHighlighter 
{
    //This contains the lucene indexed documents
    private static final String INDEX_DIR = "indexedFiles";
 
    public static void main(String[] args) throws Exception 
    {
        //Get directory reference
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
         
        //Index reader - an interface for accessing a point-in-time view of a lucene index
        IndexReader reader = DirectoryReader.open(dir);
         
        //Create lucene searcher. It search over a single IndexReader.
        IndexSearcher searcher = new IndexSearcher(reader);
         
        //analyzer with the default stop words
        Analyzer analyzer = new StandardAnalyzer();
         
        //Query parser to be used for creating TermQuery
        QueryParser qp = new QueryParser("contents", analyzer);
         
        //Create the query
        Query query = qp.parse("anarchy");
         
        //Search the lucene documents
        TopDocs hits = searcher.search(query, 10);
         
        /** Highlighter Code Start ****/
         
        //Uses HTML &lt;B&gt;&lt;/B&gt; tag to highlight the searched terms
        Formatter formatter = new SimpleHTMLFormatter();
         
        //It scores text fragments by the number of unique query terms found
        //Basically the matching score in layman terms
        QueryScorer scorer = new QueryScorer(query);
         
        //used to markup highlighted terms found in the best sections of a text
        Highlighter highlighter = new Highlighter(formatter, scorer);
        int windowSize = 10;
        Fragmenter fragmenter = new WindowFragmenter(windowSize);
         
        //set fragmenter to highlighter
        highlighter.setTextFragmenter(fragmenter);
        
        HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
        //Iterate over found results
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            int docid = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);

            //Printing - to which document result belongs
            // String title = doc.get("title");
            // System.out.println("Path " + " : " + title);
             
            //Get stored text from found document
            String text = doc.get("contents");
 
            //Create token stream
            TokenStream stream = TokenSources.getAnyTokenStream(reader, docid, "contents", analyzer);
            
            //Get highlighted text fragments
            // String[] frags = highlighter.getBestFragments(stream, text, 100);
            TextFragment[] frags = highlighter.getBestTextFragments(stream, text, false, 25);
            System.out.println("\n====================\nSearch hit fragments\n====================\n");
            for(TextFragment frag : frags) {
                System.out.println(frag.toString());
                String[] splits = frag.toString().replaceAll("<B>.+?</B>", "").toLowerCase().split("\\s+");
                for(String word : splits) {
                    String cleanedWord = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
                    if(frequencies.get(cleanedWord) != null) frequencies.put(cleanedWord, frequencies.get(cleanedWord)+1);
                    else frequencies.put(cleanedWord, 1);
                }
            }

        }

        System.out.println("\n================================================\nAggregate word frequencies for window of size " + windowSize + "\n================================================\n");
        System.out.println(frequencies);
        dir.close();
    }
}