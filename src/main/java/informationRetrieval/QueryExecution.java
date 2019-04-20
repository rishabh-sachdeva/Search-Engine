package informationRetrieval;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import java.util.StringTokenizer;

import informationRetrieval.util.UtilityClass;

/**
 * Term-at-a-time query execution strategy
 * Query term weights are also taken into account
 * 
 * @author Rishabh
 *
 */
public class QueryExecution {

	HashMap<String, Integer> docFrequenceMap= new HashMap<String, Integer>();// stores in how many docs a token occurred
	HashMap<String, Double> queryTermWeightsMap= new HashMap<String, Double>();// stores in how many docs a token occurred

	HashMap<String, Double> docScoreMap = new HashMap<>(); // doc vs score


	public void execute(String[] queryTokens) {
		BufferedReader reader;

		String dictionaryFile = System.getProperty("user.dir")+"/src/main/java/informationRetrieval/dictAndPostings/dictionary.txt";
		//iterate : term at a time
		for(String term : queryTokens) {

			term = term.toLowerCase();
			try {
				reader = new BufferedReader(new FileReader(dictionaryFile));
				String line = reader.readLine();
				while (line !=null) {
					if(line.equals(term)) {
						int freq = Integer.parseInt(reader.readLine());
						int start = Integer.parseInt(reader.readLine());
						readSpecificLinesOfPostings(start, start+freq-1, term);
					}
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		HashMap<String, Double> sortedMap = sortMapByValue();


		System.out.println("TOP 10 RESULTS");
		displayResults(sortedMap);
	}

	public void createDocFreqMap(String bodyText)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyText.getBytes(StandardCharsets.UTF_8))));
		HashMap<String, Integer> freqMapForFile = new HashMap<String, Integer>();
		StringTokenizer tokens;
		String line;
		HashSet<String> stopWords=UtilityClass.buildSetForStopWords();
		while((line=buffReader.readLine())!=null) {
			tokens = new StringTokenizer(line, " \t\n\r\f,.:;?![]{}()|%#$/<>@\\'*_+-=&\"“”~—`'’‘");
			while(tokens.hasMoreTokens()) {
				String token=tokens.nextToken().toLowerCase();
				if(stopWords.contains(token)) {
					//do nothing
				}else if(docFrequenceMap.containsKey(token)) {
					int cnt = docFrequenceMap.get(token);
					docFrequenceMap.put(token, cnt+1);
				}else {
					docFrequenceMap.put(token, 1);
				}

			}
		}
	}


	private void displayResults(Map<String,Double> sortedMap) {
		Iterator it = sortedMap.entrySet().iterator();
		int cnt=0;
		while(it.hasNext()) {
			Entry pair = (Entry) it.next();
			System.out.println("Doc Id: "+pair.getKey()+" Score: "+pair.getValue());
			if(++cnt ==10) {
				break;
			}
		}

	}

	private HashMap<String, Double> sortMapByValue() {
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(docScoreMap.entrySet()); 
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() { 
			public int compare(Map.Entry<String, Double> o1,  
					Map.Entry<String, Double> o2) 
			{ 
				return (o2.getValue()).compareTo(o1.getValue()); 
			} 
		}); 

		HashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>(); 
		for (Map.Entry<String, Double> entry : list) { 
			sortedMap.put(entry.getKey(), entry.getValue()); 
		} 
		return sortedMap; 
	}

	private void readSpecificLinesOfPostings(int start, int end, String term) {
		for(int i= start;i<=end;i++) {
			try {
				String docAndScore	= Files.readAllLines(Paths.get(System.getProperty("user.dir")+"/src/main/java/informationRetrieval/dictAndPostings/postings.txt")).get(i-1);
				String doc=docAndScore.split(",")[0];
				float score = Float.parseFloat(docAndScore.split(",")[1]);
				updateScores(doc, score, term);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateScores(String doc, float score, String term) {
		double queryWeight = queryTermWeightsMap.get(term);
		double upd_score = queryWeight * score;
		if(docScoreMap.containsKey(doc)) {
			docScoreMap.put(doc,docScoreMap.get(doc)+upd_score);
		}else {
			docScoreMap.put(doc, upd_score);
		}
	}

	private void initiateDocFreqMap(String[] args) {
		for(String token : args) {
		docFrequenceMap.put(token,0);
		}
	}

	private void evaluateQueryTermWeights() {
		File inputFolder =new File(System.getProperty("user.dir")+"/files");
		File[] files = inputFolder.listFiles();
		int num_docs = files.length;
		for(File file : files) {
			try {
				Document doc = Jsoup.parse(file, "utf-8");
				doc = new Cleaner(Whitelist.simpleText()).clean(doc);
				if(doc.body()!=null) {
					String text=doc.body().text();
					for(String queryTerm : docFrequenceMap.keySet()) {
						if(text.contains(queryTerm)) {
							docFrequenceMap.put(queryTerm, docFrequenceMap.get(queryTerm)+1);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		calculateIDF(num_docs);
	}

	private void calculateIDF(int num_docs) {
		for(Entry<String, Integer> entry : docFrequenceMap.entrySet()) {
			double idfOfTerm = Math.log(num_docs/(entry.getValue()+1));
			queryTermWeightsMap.put(entry.getKey(),idfOfTerm);
		}
	}

	public static void main(String args[]) {

		QueryExecution obj = new QueryExecution();
		obj.initiateDocFreqMap(args);
		obj.evaluateQueryTermWeights();
		obj.execute(args);
	}
}
