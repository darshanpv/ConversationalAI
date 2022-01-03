package cto.hmi.bot.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopwordProcessor {
	private static Set<String> stopwords = new HashSet<>();
	private static StopwordProcessor instance = null;

	protected StopwordProcessor() {
	}

	public static StopwordProcessor getInstance() {
		if (instance == null) {
			instance = new StopwordProcessor();
			init();
		}
		return instance;
	}

	private static void init() {
		// TODO Auto-generated method stub
		String stopwordFile;
		try {
			stopwordFile = new File(".").getAbsolutePath() + "/res/dictionary/stopwords_en.txt";
			File f = new File(stopwordFile);
			if (!f.exists()) {
				System.out.println("SEVER:No event file exists");
			}
			FileInputStream fstream;
			fstream = new FileInputStream(stopwordFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream,"UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				List<String> listWords = new ArrayList<String>();
				if (line != null) {
					
					listWords = Arrays.asList(line.split(","));
					
				}
				stopwords.addAll(listWords);
			}
			br.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String utterance) {
		String response = "";
		try {
			List<String> listOfStrings = new ArrayList<String>(Arrays.asList(utterance.toLowerCase().split(" ")));
			listOfStrings.removeAll(stopwords);
			response = String.join(" ", listOfStrings);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
}
