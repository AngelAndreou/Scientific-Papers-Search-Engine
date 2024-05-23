package luceneTest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSVIndexer {
    //private final IndexWriter indexWriter;
    private  String csvFilePath="C:\\Users\\angel\\eclipse-workspace2\\csv\\papers.csv";
    private  String indexPath="C:\\Users\\angel\\eclipse-workspace2\\index";
    
 
    
    private boolean build;
    
	private Directory indexDirectory;
	private Analyzer analyzer;
	private IndexWriter indexWriter;
	
	 //Suggestions Index
    private String suggestPath= "\\tmp\\testindex";
    private Directory suggestDirectory;
    private IndexWriter suggestWriter;

    public CSVIndexer( boolean build ) throws IOException {
        this.build=build;
        
        analyzer = new StandardAnalyzer();
        
        
        indexDirectory = FSDirectory.open(Paths.get(indexPath));
       // IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
    	indexWriter = new IndexWriter(indexDirectory, config);
    	indexWriter.commit();
        indexWriter.close();
        
        DirectoryReader indexReader = DirectoryReader.open(indexDirectory);
        
        
        
        
        OfflineSearchEngine.indexSearcher=new IndexSearcher(indexReader);
        
        //suggest
        suggestDirectory= FSDirectory.open(Paths.get(suggestPath));
        IndexWriterConfig suggestConfig = new IndexWriterConfig(analyzer);
        suggestWriter = new IndexWriter(suggestDirectory, suggestConfig);
        suggestWriter.commit();
        suggestWriter.close();
        DirectoryReader suggestReader = DirectoryReader.open(suggestDirectory);
        OfflineSearchEngine.suggestSearcher = new IndexSearcher(suggestReader);
        
        
        buildIndex();
        
       
    }

    public void buildIndex() throws IOException {
    	
    	if(build) {
    	IndexWriterConfig config = new IndexWriterConfig(analyzer);
    	indexWriter = new IndexWriter(indexDirectory, config);
    	
    	//suggest
    	IndexWriterConfig configS = new IndexWriterConfig(analyzer);
    	suggestWriter = new IndexWriter(suggestDirectory, configS);
    	

    	
    	String regex = "(\\d+),(\\d+),([^,]+),\"(.*?)\",\"(.*?)\"";
    	
    	
        Pattern pattern = Pattern.compile(regex);
    	
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            int count = 0;

            // Skip the header line
            br.readLine();
            //Made for n lines
            int totalArticlesForIndex=251;
            while (((line = br.readLine())!=null)&& count <= totalArticlesForIndex) {
            	
            	Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    Document doc = new Document();
                    doc.add(new StringField("source_id", String.valueOf(matcher.group(1)), Field.Store.YES));
                    doc.add(new IntPoint("year", Integer.parseInt(matcher.group(2))));
                    doc.add(new StoredField("year", Integer.parseInt(matcher.group(2))));
                    doc.add(new NumericDocValuesField("year", Integer.parseInt(matcher.group(2))));
                    doc.add(new TextField("title", matcher.group(3), Field.Store.YES));
                    
                    if(!(matcher.group(4)==null)) {
                    doc.add(new TextField("abstract", matcher.group(4), Field.Store.YES));
                    }
                    else {
                    	doc.add(new TextField("abstract","null",Field.Store.YES));
                    }
                    
                    
                    doc.add(new TextField("content", matcher.group(5), Field.Store.YES));

                    indexWriter.addDocument(doc);
                    count++;
                    
                    //for suggest
                    Document docS = new Document();
                    docS.add(new TextField("suggest", matcher.group(4), Field.Store.YES));
                    suggestWriter.addDocument(doc);
                }
            }
        }
        indexWriter.commit();
        indexWriter.close();
        
        suggestWriter.commit();
        suggestWriter.close();
    	
    	}
    	
    	
    }

    public void suggestions() throws IOException {
    	
    	
    }
    
}
