package informationRetrieval;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class createSetFile {

	public static void main(String[] args) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("F:/InformationRetrieval/workspace-IR/assignment1/src/main/java/informationRetrieval/assignment1/stopWords.txt"));
			String line = reader.readLine();
			while (line != null) {
				System.out.println("stopWords.add(\""+line+"\");");
				// read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
