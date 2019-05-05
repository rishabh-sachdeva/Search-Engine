package informationRetrieval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Term-at-a-time query execution strategy
 * Query term weights are also taken into account
 * 
 * @author Rishabh
 *
 */
public class QueryExecution {

	static HashMap<String, Double> queryTermWeightsMap= new HashMap<String, Double>();// maintain weights of query terms

	HashMap<String, Double> docScoreMap = new HashMap<>(); // doc vs score
	static boolean queryWeightFlag = false;

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
						break;
					}
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		HashMap<String, Double> sortedMap = sortMapByValue();

		System.out.println("TOP RESULTS");
		displayResults(sortedMap);
	}


	private void displayResults(Map<String,Double> sortedMap) {

		if(sortedMap.size()==0) {
			System.out.println("This Search Engine has no relevant documents for query provided.");
		}
		Iterator it = sortedMap.entrySet().iterator();
		int cnt=0;
		while(it.hasNext()) {
			Entry pair = (Entry) it.next();
			System.out.println("Doc: "+pair.getKey()+".html"+" Score: "+pair.getValue());
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


	private static void fetchQueryTems(String[] args, String[] queryTerms, Double[] weights) {
		int j=0;
		int k=0;
		for(int i=1;i<args.length;i++) {
			if(i%2==0) {
				queryTerms[j]=args[i];	
				j++;
			}else {
				weights[k]=Double.parseDouble(args[i]);
				k++;
			}
		}
	}
	private static void createQueryTermWeightMap(String[] queryTerms, Double[] weights) {
		for(int i=0;i<queryTerms.length;i++) {
			if(queryWeightFlag) {
				queryTermWeightsMap.put(queryTerms[i].toLowerCase(),weights[i]);
			}else {
				queryTermWeightsMap.put(queryTerms[i].toLowerCase(),1.0);
			}
		}
	}


	public static void main(String args[]) {

		QueryExecution obj = new QueryExecution();
		String[] queryTerms=null;
		Double[] weights = null;
		if(args[0].equalsIgnoreCase("wt")) {
			queryWeightFlag=true;
			queryTerms = new String[args.length/2];
			weights = new Double[args.length/2];
			fetchQueryTems(args,queryTerms,weights);
		}else {
			queryTerms=args;
		}
		createQueryTermWeightMap(queryTerms,weights);
		obj.execute(queryTerms);
	}
}
