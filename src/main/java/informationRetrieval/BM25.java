package informationRetrieval;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

public class BM25 {


	static HashMap<String, Integer> invertedIndexCountMap= new HashMap<String, Integer>();// stores in how many docs a token occurred
	public static int num_docs;
	public static int avg_doc_size;
	public static int total_words;
	public static double k = 1.2;
	public static double b = 0.75;


	public static HashMap<String,Integer> createTokenFrequencyMap(String bodyText)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyText.getBytes(StandardCharsets.UTF_8))));
		HashMap<String, Integer> freqMapForFile = new HashMap<String, Integer>();
		StringTokenizer tokens;
		String line;
		HashSet<String> stopWords=buildSetForStopWords();
		while((line=buffReader.readLine())!=null) {
			tokens = new StringTokenizer(line, " \t\n\r\f,.:;?![]{}()|%#$/<>@\\'*_+-=&\"“”~—`'’‘");
			while(tokens.hasMoreTokens()) {
				total_words++;
				String token=tokens.nextToken().toLowerCase();
				//operations for map respective to one file
				if(freqMapForFile.containsKey(token)) {
					int cnt = freqMapForFile.get(token);
					freqMapForFile.put(token, cnt+1);
				}else if(stopWords.contains(token)) {
					//do nothing
				}else {
					freqMapForFile.put(token,1);
					if(invertedIndexCountMap.containsKey(token)) {
						int cnt = invertedIndexCountMap.get(token);
						invertedIndexCountMap.put(token, cnt+1);
					}else {
						invertedIndexCountMap.put(token, 1);
					}
				}

			}
		}
		return freqMapForFile;
	}


	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		File inputFolder = new File(args[0]);
		String outputDir = args[1];
		//	File inputFolder =new File("F:/InformationRetrieval/files");
		//	String outputDir ="F:/InformationRetrieval/output_2";
		File outputFolder = new File(outputDir);


		BM25 bm25 = new BM25();
		File[] files = inputFolder.listFiles();
		bm25.setNumDocs(files.length);
		String tempPath=System.getProperty("user.dir")+"/temp";
		createDirectory(tempPath);
		createDirectory(outputDir);

		//initializeMapsForFiles(files);
		for(int i=0;i<files.length;i++) {
			if(files[i].isFile()) {
				Document doc = Jsoup.parse(files[i], "utf-8");
				doc = new Cleaner(Whitelist.simpleText()).clean(doc);
				if(doc.body()!=null) {
					String text=doc.body().text();
					text=text.replaceAll("[0-9]","");
					text=text.replaceAll("\\u000B","");
					text=text.replaceAll("\\u0001","");
					HashMap<String, Integer> freqMapForFile = createTokenFrequencyMap(text);
					writeTempTokenFile(freqMapForFile,tempPath+"/"+ String.valueOf(i+1) +".properties");
					//BM25scoreForFile(freqMapForFile,invertedIndexCountMap);
				}
			}
		}
		bm25.setAvgDocSize();	
		BM25scoreForFile(tempPath,outputFolder);

		long endTime = System.currentTimeMillis();
		System.out.println("Took "+(endTime - startTime) + " ms"); 
	}

	private void setAvgDocSize() {
		this.avg_doc_size=this.total_words/this.num_docs;
	}

	private static void createDirectory(String tempPath) {
		File temp = new File(tempPath);
		if(temp.exists()) {
			deleteDir(temp);
		}
		if (temp.mkdir()) {
		} else {
			System.out.println("Failed to create directory!");
		}
	}
	
	static void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            if (! Files.isSymbolicLink(f.toPath())) {
	                deleteDir(f);
	            }
	        }
	    }
	    file.delete();
	}
	private static void writeTempTokenFile(HashMap<String,Integer> freqMapForFile, String filePath) {
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


	private static void BM25scoreForFile(String tempPath, File outputFolder) {
		File dir = new File(tempPath);
		File[] propListing = dir.listFiles();
		int fileCounter=0;
		for (File prop : propListing) {
			fileCounter++;
			InputStream propStream = null;
			try {
				propStream = new FileInputStream(prop.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			HashMap<String, Double> scoreMapPerFile = new HashMap<>();
			Properties properties = new Properties();
			try {
				properties.load(propStream);
				for(String key : properties.stringPropertyNames()) {
					String freq = properties.getProperty(key);
					int word_freq = Integer.parseInt(freq);
					double word_freq_sqrt = Math.sqrt(word_freq);
					int docFreq=1;
					if(invertedIndexCountMap.containsKey(key)) {
						docFreq = invertedIndexCountMap.get(key);
					}else {
						continue;
						//	   System.out.println("NOT FOUND"+key);
					}
					double idf = calculateIDF(docFreq);
					double tf = calculateTF(word_freq_sqrt,properties.size());
					double bm25score = tf*idf;
					//System.out.println(key+",TF: "+tf +",IDF:"+idf+"score:" +bm25score);
					scoreMapPerFile.put(key, bm25score);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//write into one file at a time
			writeTokenFile(scoreMapPerFile,outputFolder.toString()+"/"+prop.getName());
			//iterate over properties of prop file
		}

	}
	private static void writeTokenFile(HashMap<String,Double> freqMapForFile, String filePath) {
		Properties properties = new Properties();

		for(Entry<String, Double> entry: freqMapForFile.entrySet()) {
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

	private static double calculateTF(double word_freq, int doc_size) {
		double tf = (word_freq * (k + 1)) / (word_freq + k * (1 - 
				b + b * doc_size / avg_doc_size));
		return tf;
	}

	private static double calculateIDF(int docFreq) {
		BM25 obj = new BM25();
		return Math.log(num_docs/(docFreq+1));
	}

	public void setNumDocs(int num) {
		this.num_docs=num;
	}
	public int getNumDocs() {
		return num_docs;
	}
	private static HashSet<String> buildSetForStopWords() {
		HashSet<String> stopWords = new HashSet<>();
		stopWords.add("a");
		stopWords.add("about");
		stopWords.add("above");
		stopWords.add("according");
		stopWords.add("across");
		stopWords.add("actually");
		stopWords.add("adj");
		stopWords.add("after");
		stopWords.add("afterwards");
		stopWords.add("again");
		stopWords.add("against");
		stopWords.add("all");
		stopWords.add("almost");
		stopWords.add("alone");
		stopWords.add("along");
		stopWords.add("already");
		stopWords.add("also");
		stopWords.add("although");
		stopWords.add("always");
		stopWords.add("among");
		stopWords.add("amongst");
		stopWords.add("an");
		stopWords.add("and");
		stopWords.add("another");
		stopWords.add("any");
		stopWords.add("anybody");
		stopWords.add("anyhow");
		stopWords.add("anyone");
		stopWords.add("anything");
		stopWords.add("anywhere");
		stopWords.add("are");
		stopWords.add("area");
		stopWords.add("areas");
		stopWords.add("aren't");
		stopWords.add("around");
		stopWords.add("as");
		stopWords.add("ask");
		stopWords.add("asked");
		stopWords.add("asking");
		stopWords.add("asks");
		stopWords.add("at");
		stopWords.add("away");
		stopWords.add("b");
		stopWords.add("back");
		stopWords.add("backed");
		stopWords.add("backing");
		stopWords.add("backs");
		stopWords.add("be");
		stopWords.add("became");
		stopWords.add("because");
		stopWords.add("become");
		stopWords.add("becomes");
		stopWords.add("becoming");
		stopWords.add("been");
		stopWords.add("before");
		stopWords.add("beforehand");
		stopWords.add("began");
		stopWords.add("begin");
		stopWords.add("beginning");
		stopWords.add("behind");
		stopWords.add("being");
		stopWords.add("beings");
		stopWords.add("below");
		stopWords.add("beside");
		stopWords.add("besides");
		stopWords.add("best");
		stopWords.add("better");
		stopWords.add("between");
		stopWords.add("beyond");
		stopWords.add("big");
		stopWords.add("billion");
		stopWords.add("both");
		stopWords.add("but");
		stopWords.add("by");
		stopWords.add("c");
		stopWords.add("came");
		stopWords.add("can");
		stopWords.add("can't");
		stopWords.add("cannot");
		stopWords.add("caption");
		stopWords.add("case");
		stopWords.add("cases");
		stopWords.add("certain");
		stopWords.add("certainly");
		stopWords.add("clear");
		stopWords.add("clearly");
		stopWords.add("co");
		stopWords.add("come");
		stopWords.add("could");
		stopWords.add("couldn't");
		stopWords.add("d");
		stopWords.add("did");
		stopWords.add("didn't");
		stopWords.add("differ");
		stopWords.add("different");
		stopWords.add("differently");
		stopWords.add("do");
		stopWords.add("does");
		stopWords.add("doesn't");
		stopWords.add("don't");
		stopWords.add("done");
		stopWords.add("down");
		stopWords.add("downed");
		stopWords.add("downing");
		stopWords.add("downs");
		stopWords.add("during");
		stopWords.add("e");
		stopWords.add("each");
		stopWords.add("early");
		stopWords.add("eg");
		stopWords.add("eight");
		stopWords.add("eighty");
		stopWords.add("either");
		stopWords.add("else");
		stopWords.add("elsewhere");
		stopWords.add("end");
		stopWords.add("ended");
		stopWords.add("ending");
		stopWords.add("ends");
		stopWords.add("enough");
		stopWords.add("etc");
		stopWords.add("even");
		stopWords.add("evenly");
		stopWords.add("ever");
		stopWords.add("every");
		stopWords.add("everybody");
		stopWords.add("everyone");
		stopWords.add("everything");
		stopWords.add("everywhere");
		stopWords.add("except");
		stopWords.add("f");
		stopWords.add("face");
		stopWords.add("faces");
		stopWords.add("fact");
		stopWords.add("facts");
		stopWords.add("far");
		stopWords.add("felt");
		stopWords.add("few");
		stopWords.add("fifty");
		stopWords.add("find");
		stopWords.add("finds");
		stopWords.add("first");
		stopWords.add("five");
		stopWords.add("for");
		stopWords.add("former");
		stopWords.add("formerly");
		stopWords.add("forty");
		stopWords.add("found ");
		stopWords.add("four");
		stopWords.add("from");
		stopWords.add("further");
		stopWords.add("furthered");
		stopWords.add("furthering");
		stopWords.add("furthers");
		stopWords.add("g");
		stopWords.add("gave");
		stopWords.add("general");
		stopWords.add("generally");
		stopWords.add("get");
		stopWords.add("gets");
		stopWords.add("give");
		stopWords.add("given");
		stopWords.add("gives");
		stopWords.add("go");
		stopWords.add("going");
		stopWords.add("good");
		stopWords.add("goods");
		stopWords.add("got");
		stopWords.add("great");
		stopWords.add("greater");
		stopWords.add("greatest");
		stopWords.add("group");
		stopWords.add("grouped");
		stopWords.add("grouping");
		stopWords.add("groups");
		stopWords.add("h");
		stopWords.add("had");
		stopWords.add("has");
		stopWords.add("hasn't");
		stopWords.add("have");
		stopWords.add("haven't");
		stopWords.add("having");
		stopWords.add("he");
		stopWords.add("he'd");
		stopWords.add("he'll");
		stopWords.add("he's");
		stopWords.add("hence");
		stopWords.add("her");
		stopWords.add("here");
		stopWords.add("here's");
		stopWords.add("hereafter");
		stopWords.add("hereby");
		stopWords.add("herein");
		stopWords.add("hereupon");
		stopWords.add("hers");
		stopWords.add("herself");
		stopWords.add("high");
		stopWords.add("higher");
		stopWords.add("highest");
		stopWords.add("him");
		stopWords.add("himself");
		stopWords.add("his");
		stopWords.add("how");
		stopWords.add("however");
		stopWords.add("hundred");
		stopWords.add("i");
		stopWords.add("i'd");
		stopWords.add("i'll");
		stopWords.add("i'm");
		stopWords.add("i've");
		stopWords.add("ie");
		stopWords.add("if");
		stopWords.add("important");
		stopWords.add("in");
		stopWords.add("inc");
		stopWords.add("indeed");
		stopWords.add("instead");
		stopWords.add("interest");
		stopWords.add("interested");
		stopWords.add("interesting");
		stopWords.add("interests");
		stopWords.add("into");
		stopWords.add("is");
		stopWords.add("isn't");
		stopWords.add("it");
		stopWords.add("it's");
		stopWords.add("its");
		stopWords.add("itself");
		stopWords.add("j");
		stopWords.add("just");
		stopWords.add("k");
		stopWords.add("l");
		stopWords.add("large");
		stopWords.add("largely");
		stopWords.add("last");
		stopWords.add("later");
		stopWords.add("latest");
		stopWords.add("latter");
		stopWords.add("latterly");
		stopWords.add("least");
		stopWords.add("less");
		stopWords.add("let");
		stopWords.add("let's");
		stopWords.add("lets");
		stopWords.add("like");
		stopWords.add("likely");
		stopWords.add("long");
		stopWords.add("longer");
		stopWords.add("longest");
		stopWords.add("ltd");
		stopWords.add("m");
		stopWords.add("made");
		stopWords.add("make");
		stopWords.add("makes");
		stopWords.add("making");
		stopWords.add("man");
		stopWords.add("many");
		stopWords.add("may");
		stopWords.add("maybe");
		stopWords.add("me");
		stopWords.add("meantime");
		stopWords.add("meanwhile");
		stopWords.add("member");
		stopWords.add("members");
		stopWords.add("men");
		stopWords.add("might");
		stopWords.add("million");
		stopWords.add("miss");
		stopWords.add("more");
		stopWords.add("moreover");
		stopWords.add("most");
		stopWords.add("mostly");
		stopWords.add("mr");
		stopWords.add("mrs");
		stopWords.add("much");
		stopWords.add("must");
		stopWords.add("my");
		stopWords.add("myself");
		stopWords.add("n");
		stopWords.add("namely");
		stopWords.add("necessary");
		stopWords.add("need");
		stopWords.add("needed");
		stopWords.add("needing");
		stopWords.add("needs");
		stopWords.add("neither");
		stopWords.add("never");
		stopWords.add("nevertheless");
		stopWords.add("new");
		stopWords.add("newer");
		stopWords.add("newest");
		stopWords.add("next");
		stopWords.add("nine");
		stopWords.add("ninety");
		stopWords.add("no");
		stopWords.add("nobody");
		stopWords.add("non");
		stopWords.add("none");
		stopWords.add("nonetheless");
		stopWords.add("noone");
		stopWords.add("nor");
		stopWords.add("not");
		stopWords.add("nothing");
		stopWords.add("now");
		stopWords.add("nowhere");
		stopWords.add("number");
		stopWords.add("numbers");
		stopWords.add("o");
		stopWords.add("of");
		stopWords.add("off");
		stopWords.add("often");
		stopWords.add("old");
		stopWords.add("older");
		stopWords.add("oldest");
		stopWords.add("on");
		stopWords.add("once");
		stopWords.add("one");
		stopWords.add("one's");
		stopWords.add("only");
		stopWords.add("onto");
		stopWords.add("open");
		stopWords.add("opened");
		stopWords.add("opens");
		stopWords.add("or");
		stopWords.add("order");
		stopWords.add("ordered");
		stopWords.add("ordering");
		stopWords.add("orders");
		stopWords.add("other");
		stopWords.add("others");
		stopWords.add("otherwise");
		stopWords.add("our");
		stopWords.add("ours");
		stopWords.add("ourselves");
		stopWords.add("out");
		stopWords.add("over");
		stopWords.add("overall");
		stopWords.add("own");
		stopWords.add("p");
		stopWords.add("part");
		stopWords.add("parted");
		stopWords.add("parting");
		stopWords.add("parts");
		stopWords.add("per");
		stopWords.add("perhaps");
		stopWords.add("place");
		stopWords.add("places");
		stopWords.add("point");
		stopWords.add("pointed");
		stopWords.add("pointing");
		stopWords.add("points");
		stopWords.add("possible");
		stopWords.add("present");
		stopWords.add("presented");
		stopWords.add("presenting");
		stopWords.add("presents");
		stopWords.add("problem");
		stopWords.add("problems");
		stopWords.add("put");
		stopWords.add("puts");
		stopWords.add("q");
		stopWords.add("quite");
		stopWords.add("r");
		stopWords.add("rather");
		stopWords.add("really");
		stopWords.add("recent");
		stopWords.add("recently");
		stopWords.add("right");
		stopWords.add("room");
		stopWords.add("rooms");
		stopWords.add("s");
		stopWords.add("said");
		stopWords.add("same");
		stopWords.add("saw");
		stopWords.add("say");
		stopWords.add("says");
		stopWords.add("second");
		stopWords.add("seconds");
		stopWords.add("see");
		stopWords.add("seem");
		stopWords.add("seemed");
		stopWords.add("seeming");
		stopWords.add("seems");
		stopWords.add("seven");
		stopWords.add("seventy");
		stopWords.add("several");
		stopWords.add("she");
		stopWords.add("she'd");
		stopWords.add("she'll");
		stopWords.add("she's");
		stopWords.add("should");
		stopWords.add("shouldn't");
		stopWords.add("show");
		stopWords.add("showed");
		stopWords.add("showing");
		stopWords.add("shows");
		stopWords.add("sides");
		stopWords.add("since");
		stopWords.add("six");
		stopWords.add("sixty");
		stopWords.add("small");
		stopWords.add("smaller");
		stopWords.add("smallest");
		stopWords.add("so");
		stopWords.add("some");
		stopWords.add("somebody");
		stopWords.add("somehow");
		stopWords.add("someone");
		stopWords.add("something");
		stopWords.add("sometime");
		stopWords.add("sometimes");
		stopWords.add("somewhere");
		stopWords.add("state");
		stopWords.add("states");
		stopWords.add("still");
		stopWords.add("stop");
		stopWords.add("such");
		stopWords.add("sure");
		stopWords.add("t");
		stopWords.add("take");
		stopWords.add("taken");
		stopWords.add("taking");
		stopWords.add("ten");
		stopWords.add("than");
		stopWords.add("that");
		stopWords.add("that'll");
		stopWords.add("that's");
		stopWords.add("that've");
		stopWords.add("the");
		stopWords.add("their");
		stopWords.add("them");
		stopWords.add("themselves");
		stopWords.add("then");
		stopWords.add("thence");
		stopWords.add("there");
		stopWords.add("there'd");
		stopWords.add("there'll");
		stopWords.add("there're");
		stopWords.add("there's");
		stopWords.add("there've");
		stopWords.add("thereafter");
		stopWords.add("thereby");
		stopWords.add("therefore");
		stopWords.add("therein");
		stopWords.add("thereupon");
		stopWords.add("these");
		stopWords.add("they");
		stopWords.add("they'd");
		stopWords.add("they'll");
		stopWords.add("they're");
		stopWords.add("they've");
		stopWords.add("thing");
		stopWords.add("things");
		stopWords.add("think");
		stopWords.add("thinks");
		stopWords.add("thirty");
		stopWords.add("this");
		stopWords.add("those");
		stopWords.add("though");
		stopWords.add("thought");
		stopWords.add("thoughts");
		stopWords.add("thousand");
		stopWords.add("three");
		stopWords.add("through");
		stopWords.add("throughout");
		stopWords.add("thru");
		stopWords.add("thus");
		stopWords.add("to");
		stopWords.add("today");
		stopWords.add("together");
		stopWords.add("too");
		stopWords.add("took");
		stopWords.add("toward");
		stopWords.add("towards");
		stopWords.add("trillion");
		stopWords.add("turn");
		stopWords.add("turned");
		stopWords.add("turning");
		stopWords.add("turns");
		stopWords.add("twenty");
		stopWords.add("two");
		stopWords.add("u");
		stopWords.add("under");
		stopWords.add("unless");
		stopWords.add("unlike");
		stopWords.add("unlikely");
		stopWords.add("until");
		stopWords.add("up");
		stopWords.add("upon");
		stopWords.add("us");
		stopWords.add("use");
		stopWords.add("used");
		stopWords.add("uses");
		stopWords.add("using");
		stopWords.add("v");
		stopWords.add("very");
		stopWords.add("via");
		stopWords.add("w");
		stopWords.add("want");
		stopWords.add("wanted");
		stopWords.add("wanting");
		stopWords.add("wants");
		stopWords.add("was");
		stopWords.add("wasn't");
		stopWords.add("way");
		stopWords.add("ways");
		stopWords.add("we");
		stopWords.add("we'd");
		stopWords.add("we'll");
		stopWords.add("we're");
		stopWords.add("we've");
		stopWords.add("well");
		stopWords.add("wells");
		stopWords.add("were");
		stopWords.add("weren't");
		stopWords.add("what");
		stopWords.add("what'll");
		stopWords.add("what's");
		stopWords.add("what've");
		stopWords.add("whatever");
		stopWords.add("when");
		stopWords.add("whence");
		stopWords.add("whenever");
		stopWords.add("where");
		stopWords.add("where's");
		stopWords.add("whereafter");
		stopWords.add("whereas");
		stopWords.add("whereby");
		stopWords.add("wherein");
		stopWords.add("whereupon");
		stopWords.add("wherever");
		stopWords.add("whether");
		stopWords.add("which");
		stopWords.add("while");
		stopWords.add("whither");
		stopWords.add("who");
		stopWords.add("who'd");
		stopWords.add("who'll");
		stopWords.add("who's");
		stopWords.add("whoever");
		stopWords.add("whole");
		stopWords.add("whom");
		stopWords.add("whomever");
		stopWords.add("whose");
		stopWords.add("why");
		stopWords.add("will");
		stopWords.add("with");
		stopWords.add("within");
		stopWords.add("without");
		stopWords.add("won't");
		stopWords.add("work");
		stopWords.add("worked");
		stopWords.add("working");
		stopWords.add("works");
		stopWords.add("would");
		stopWords.add("wouldn't");
		stopWords.add("x");
		stopWords.add("y");
		stopWords.add("year");
		stopWords.add("years");
		stopWords.add("yes");
		stopWords.add("yet");
		stopWords.add("you");
		stopWords.add("you'd");
		stopWords.add("you'll");
		stopWords.add("you're");
		stopWords.add("you've");
		stopWords.add("young");
		stopWords.add("younger");
		stopWords.add("youngest");
		stopWords.add("your");
		stopWords.add("yours");
		stopWords.add("yourself");
		stopWords.add("yourselves");
		stopWords.add("z");
		return stopWords;
	}
}
