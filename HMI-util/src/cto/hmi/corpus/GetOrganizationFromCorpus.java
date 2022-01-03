package cto.hmi.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GetOrganizationFromCorpus {

	public static String CORPUS = "/res/entities/organization.txt";

	public String get(String utterance) {
		String output="";
		ArrayList<String> items = new ArrayList<String>();

		String path = new File(".").getAbsolutePath();
		String cityCorpus = path + CORPUS;

		FileInputStream fstream;
		try {
			fstream = new FileInputStream(cityCorpus);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				if (utterance.matches("(?i)^.*?\\b" + strLine + "\\b.*?")) {
					items.add(strLine);
				}
			}
			// Close the input stream
			br.close();
			
			// get the name that is max in length if there are multiple results

			/*
			 * for (int i = 0; i < items.size(); i++) { if
			 * (items.get(i).trim().split("\\s+").length > len) { len =
			 * items.get(i).trim().split("").length; cnt = i; } }
			 */

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (int i = 0; i < items.size(); i++) {
			output = output + "|" + items.get(i);
		}
		return output;

	}
}
