package luceneTest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
//import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.index.memory.*;
//import org.apache.lucene.expressions.Bindings;
//import org.apache.lucene.expressions.Expression;
//import org.apache.lucene.expressions.js.JavascriptCompiler;
//import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.spell.DirectSpellChecker;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SuggestWord;
//suggest
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.automaton.*;

import org.apache.lucene.search.suggest.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OfflineSearchEngine extends JFrame implements ActionListener {
    private JTextField searchField;
    //exact Search
    //private JTextField exactSearchField;
    private JButton exactSearchButton;
   
    //private JTextArea resultArea;
    private JComboBox<String> fieldComboBox;
    private JTabbedPane tabbedPane;
    private  JLabel numberOfHits;
    
    //for sorting by year
    private JButton sortByYearButton;
    private ScoreDoc[] currentScoreDocs;
    private Query currentQuery;
    private Analyzer analyzer;
    
    //for History
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private DefaultListModel<String> suggestionModel;
    private JList<String> suggestionList;
    private Directory suggestDirectory;
    static IndexSearcher suggestSearcher;
    
    //For index
   // private Directory indexDirectory;
    static IndexSearcher indexSearcher;
    
    private IndexReader indexReader;
    
    //spellcheck
    private SpellChecker spellChecker;

    public OfflineSearchEngine() {
        super("Article Search Engine");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();  
        panel.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        
        searchField = new JTextField();
        inputPanel.add(searchField, BorderLayout.NORTH);
        
       
        
        
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(this);
        inputPanel.add(searchButton,BorderLayout.WEST);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        //exact search
        JPanel exactSearchPanel = new JPanel();
        exactSearchPanel.setLayout(new BorderLayout());

    

        exactSearchButton = new JButton("Exact Search");
        exactSearchButton.addActionListener(this);
   
        
        tabbedPane = new JTabbedPane();
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        
      //Search History
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createHistoryPanel());
        splitPane.setRightComponent(tabbedPane);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        String[] fields = {"all","title","abstract", "content"};
        fieldComboBox = new JComboBox<>(fields);
        JLabel textField=new JLabel("Search in: ");
        bottomPanel.add(textField, BorderLayout.WEST);
        bottomPanel.add(fieldComboBox);
        

        //sort button
      //sorting by year
        sortByYearButton=new JButton("Sort by year published");
        sortByYearButton.addActionListener(this);
        bottomPanel.add(sortByYearButton);
        
        inputPanel.add(bottomPanel, BorderLayout.CENTER);
        bottomPanel.add(exactSearchButton, BorderLayout.SOUTH);
        
        //panel.add(searchPanel, BorderLayout.NORTH);
        
       
        JPanel searchPanel = new JPanel();
        
        searchPanel.setLayout(new GridLayout(2, 1));
        searchPanel.add(inputPanel);
        searchPanel.add(exactSearchPanel);
        
        panel.add(inputPanel, BorderLayout.NORTH);

        

        
      //Search History
 
       
        splitPane.setLeftComponent(createHistoryPanel());
        splitPane.setRightComponent(tabbedPane);

        panel.add(splitPane,BorderLayout.CENTER);
        
        add(panel);
    }

    //FOR UI
    private JPanel createHistoryPanel() {
        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new BorderLayout());

        JLabel historyLabel = new JLabel("Search History");
        historyPanel.add(historyLabel, BorderLayout.NORTH);

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        JScrollPane historyScrollPane = new JScrollPane(historyList);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);

        JLabel suggestionsLabel = new JLabel("Suggestions");
        historyPanel.add(suggestionsLabel, BorderLayout.SOUTH);

        suggestionModel=new DefaultListModel<>();
        suggestionList = new JList<>(suggestionModel);
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
        historyPanel.add(suggestionScrollPane, BorderLayout.SOUTH);

        return historyPanel;
    }
    
   
    
   

    public void search(String field,String queryText,boolean isExactSearch) throws Exception {
    	String correctedQueryText;
    	if(isExactSearch) {
    		 analyzer = new StandardAnalyzer();
    		 correctedQueryText="\""+queryText+"\"";
    		
    	}
    	else {
    		
    	//Automatically correct spellenig mistakes
    	
    	DirectSpellChecker spellChecker=new DirectSpellChecker();
    	 
    	//TODO Get the index from CSVindexer
    	Directory indexDirectory = FSDirectory.open(Paths.get("\\tmp\\testindex"));
        
        DirectoryReader indexReader = DirectoryReader.open(indexDirectory);
        
    	SuggestWord[] suggestions = spellChecker.suggestSimilar(new Term("suggest",queryText.toLowerCase()), 5,indexReader);
    	 correctedQueryText = suggestions.length > 0 ? suggestions[0].toString() : queryText.toLowerCase();
    	 
    	 analyzer = new StandardAnalyzer();
    	 
    	 if(suggestions.length == 0) {
    	 if(queryText.length()>3) {
    	 correctedQueryText=queryText+"**"+"~2";
    	 }
    	 else {
    	 correctedQueryText=queryText;}
    	}
    	}
    	Query query;
    	
    	if("all".equals(field)) {
    		String[] fields = {"title","abstract", "content",};
            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, analyzer);
            query = queryParser.parse(correctedQueryText);
        } 
    	
    	else {
	        QueryParser queryParser = new QueryParser(field, analyzer);
	         query = queryParser.parse(correctedQueryText);
    		//PhraseQuery builder =  new PhraseQuery(field,queryText);
    		// query =builder;
    	}
    	
    	//for sorting
    	this.currentQuery=query;
    	
        this.currentScoreDocs = indexSearcher.search(query, 50).scoreDocs;
        displayResults(currentScoreDocs);
        
        //Save search to History
        historyModel.addElement(queryText);
        updateSuggestions(correctedQueryText);
    }
    
    public void displayResults(ScoreDoc[] scoreDocs)throws IOException, InvalidTokenOffsetsException{
    	
        tabbedPane.removeAll();
        
      //HIGHLIGHT
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("<i><u><span style='background:yellow'>","</i></u></span>");
    
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(currentQuery));
       
        highlighter.setTextFragmenter(new SimpleFragmenter(50));
     
  
        
        int totalPages=(int) Math.ceil((double) scoreDocs.length/10);
        
        for (int i=0;i<totalPages;i++) {
        	JPanel resultPanel=new JPanel();
        	resultPanel.setLayout(new BorderLayout());
        	
        	JEditorPane resultArea=new JEditorPane();
        	 resultArea.setContentType("text/html");
        	
        	resultArea.setEditable(false);
        	resultPanel.add(new JScrollPane(resultArea),BorderLayout.CENTER);
        	
        	int start=i*10;
        	int end=Math.min( start+10,scoreDocs.length);
        	
        	//size of the string to show at result
        	
        	StringBuilder resultBuilder=new StringBuilder(20);
        	
        	for (int j=start;j<end; j++) {
        		Document doc=indexSearcher.doc(scoreDocs[j].doc);
        		//resultArea.append("Title: "+doc.get("title")+"\n");
        		//resultArea.append("Content: "+doc.get("content")+"\n\n");
        		String sourceId=doc.get("source_id");
        		int year=doc.getField("year").numericValue().intValue();
        		String title=doc.get("title");
        		String _abstract =doc.get("abstract");
        		String content=doc.get("content");
        		
        		String highlightedTitle=highlighter.getBestFragment(analyzer, "title",title);
        		String highlightedAbstract=highlighter.getBestFragment(analyzer, "absrtact",_abstract);
        		String highlightedContent=highlighter.getBestFragment(analyzer, "content",content);
        		
        		
        		resultBuilder.append("<strong>Source ID:</strong>").append(sourceId).append("<br>");
        		resultBuilder.append("<strong>Year:</strong>").append(year).append("<br>");
        		resultBuilder.append("<strong>Title:</strong>").append(highlightedTitle != null ? highlightedTitle : title).append("<br>");
        		if(highlightedAbstract!=null)
        		resultBuilder.append("<strong>Abstract: ...</strong>").append( highlightedAbstract).append("...<br>");
        		if(highlightedContent!=null)
        		resultBuilder.append("<strong>Content: ...</strong>").append(highlightedContent != null ? highlightedContent : content.subSequence(0, 25)).append("...<br>");
        		resultBuilder.append("<ln>----------------------------------------------<br>");
        	}
        	
        	resultArea.setText(resultBuilder.toString());
        	tabbedPane.addTab("Page "+(i+1),resultPanel);
        	
        	//TOTAL RESULTS
        	 //total hits
        	
        	numberOfHits=new JLabel("Total Results: "+(scoreDocs.length));
        	resultPanel.add(numberOfHits,BorderLayout.SOUTH);
           
        	
        
           
        }
        
    }
    
    public void sortByYear()throws Exception{
    	
    	if(currentScoreDocs==null||currentScoreDocs.length<=1)
    		return;
    	
    	
    	Sort sort =new Sort(new SortField("year",SortField.Type.INT, false));
    	//test
    	//currentQuery=new MatchAllDocsQuery();
    	TopDocs topDocs = indexSearcher.search(currentQuery, 251, sort);
        currentScoreDocs = topDocs.scoreDocs;

        displayResults(currentScoreDocs);
        
   
       
    }
    
    //for Suggestions
    private List<String> getSuggestions(String prefix) throws IOException {
    	List<String> suggestions = new ArrayList<>();

        // Define an Automaton for matching terms with the given prefix
        Automaton automaton = new RegExp(prefix + ".*").toAutomaton();
        RunAutomaton runAutomaton = new CharacterRunAutomaton(automaton);

        for (LeafReaderContext leafContext : suggestSearcher.getIndexReader().leaves()) {
            Terms terms = leafContext.reader().terms("suggest");
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                BytesRef term;

                while ((term = termsEnum.next()) != null) {
                    String termString = term.utf8ToString();
                    if (((CharacterRunAutomaton) runAutomaton).run(termString)) {
                        suggestions.add(termString);
                    }
                }
            }
        }

        return suggestions;
    }

    private void updateSuggestions(String queryText) throws IOException {
        suggestionModel.clear();
        List<String> suggestions = getSuggestions(queryText);
        for (String suggestion : suggestions) {
            suggestionModel.addElement(suggestion);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Search")) {
        	String field = (String) fieldComboBox.getSelectedItem();
            String query = searchField.getText();
            try {
                search(field,query,false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        else if (e.getActionCommand().equals("Sort by year published")) {
            try {
                sortByYear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        else if (e.getActionCommand().equals("Exact Search")) {
        	String field = (String) fieldComboBox.getSelectedItem();
            String query = searchField.getText();
            try {
                search(field,query,true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        	
        }
    }

    public static void main(String[] args) {
    	boolean buildIndex=false;
        SwingUtilities.invokeLater(() -> {
            try {
                OfflineSearchEngine searchEngineGUI = new OfflineSearchEngine();
                CSVIndexer indexer=new CSVIndexer(buildIndex);
              
                
                searchEngineGUI.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}