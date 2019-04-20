package informationRetrieval;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

public class WordFrequencyCount {
	
	static class MyComparator implements Comparator<Entry<String,Integer>> {
		  public int compare(Entry<String,Integer> o1, Entry<String, Integer> o2) {
		    return o1.getValue().compareTo(o2.getValue());
		  }
		}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		File inputFolder =new File(args[0]);
		String outputFolder = args[1];
		//File inputFolder = new File("F:/InformationRetrieval/files");
		//String outputFolder="F:/InformationRetrieval/output";
		File directory = new File(String.valueOf(outputFolder));
		if(!directory.exists()) {
			directory.mkdir();
		}
		File[] files = inputFolder.listFiles();
		HashMap<String, Integer> globalMap= new HashMap<String, Integer>();
		for(int i=0;i<files.length;i++) {
			if(files[i].isFile()) {
				Document doc = Jsoup.parse(files[i], "utf-8");
				doc = new Cleaner(Whitelist.simpleText()).clean(doc);
				if(doc.body()!=null) {
					String text=doc.body().text();
					text=text.replaceAll("[0-9]","");
					text=text.replaceAll("\\u000B","");
					text=text.replaceAll("\\u0001","");

					HashMap<String, Integer> freqMapForFile = createTokenFrequencyMap(text, globalMap);
					writeTokenFile(freqMapForFile,outputFolder+"/"+ String.valueOf(i+1) +".properties");
					//bodyText.append("\n"+text);
				}
			}
		}
		//sort by key and store contents of global map into a file
        TreeMap<String, Integer> mapSortedByKey = new TreeMap<>(); 
        mapSortedByKey.putAll(globalMap);
        createFinalOutputFile(mapSortedByKey,outputFolder+"/"+"frequencySortedByToken.properties");
        LinkedHashMap<String, Integer> sortedMapByValue = sortMapByValue(globalMap);
        createFinalOutputValFile(sortedMapByValue,outputFolder+"/"+"frequencySortedByValue.properties");

        long endTime = System.currentTimeMillis();
        System.out.println("Took "+(endTime - startTime) + " ms"); 

	}

	private static LinkedHashMap<String, Integer> sortMapByValue(HashMap<String, Integer> unsortedMap) {
		List<Entry<String,Integer>> list = new LinkedList<Entry<String,Integer>>(
				unsortedMap.entrySet());

		Collections.sort(list, new MyComparator());

		LinkedHashMap<String,Integer> sortedMap = new LinkedHashMap<>();
		for (Entry<String,Integer> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}	
		return sortedMap;
	}

	private static void createFinalOutputValFile(Map<String, Integer> mapSortedByVal, String filePath) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(filePath, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		for(Entry<String, Integer> entry: mapSortedByVal.entrySet()) {
			writer.println(entry.getKey()+"="+entry.getValue());
		}
		writer.close();
	}
	private static void createFinalOutputFile(TreeMap<String, Integer> mapSortedByKey, String filePath) {
		Properties properties = new Properties() {
		    @Override
		    public synchronized Enumeration<Object> keys() {
		        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		    }
		};
		for(Entry<String, Integer> entry: mapSortedByKey.entrySet()) {
		    properties.put(entry.getKey().toString(), entry.getValue().toString());
		}
		try {
			properties.store(new OutputStreamWriter(new FileOutputStream(filePath),"UTF-8"), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static HashMap<String,Integer> createTokenFrequencyMap(String bodyText, HashMap<String, Integer> wordCountMap)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyText.getBytes(StandardCharsets.UTF_8))));
		HashMap<String, Integer> freqMapForFile = new HashMap<String, Integer>();
		StringTokenizer tokens;
		String line;
		while((line=buffReader.readLine())!=null) {
			tokens = new StringTokenizer(line, " \t\n\r\f,.:;?![]{}()|%#$/<>@\\'*_+-=&\"“”~—`'’‘");
			while(tokens.hasMoreTokens()) {
				String token=tokens.nextToken().toLowerCase();
				//operations for map respective to one file
				if(freqMapForFile.containsKey(token)) {
					int cnt = freqMapForFile.get(token);
					freqMapForFile.put(token, cnt+1);
				}else {
					freqMapForFile.put(token,1);
				}

				//global map operations
				if(wordCountMap.containsKey(token)) {
					int cnt = wordCountMap.get(token);
					wordCountMap.put(token, cnt+1);
				}else {
					wordCountMap.put(token, 1);
				}
			}
		}
		return freqMapForFile;
	}

	private static void writeTokenFile(HashMap<String,Integer> freqMapForFile, String filePath) {
		Properties properties = new Properties();
		
		for(Entry<String, Integer> entry: freqMapForFile.entrySet()) {
		    properties.put(entry.getKey(), entry.getValue().toString());
		}
		try {
			properties.store(new OutputStreamWriter(new FileOutputStream(filePath),"utf-8"), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
