package cto.hmi.processor.nlu.entityparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.processor.exceptions.NoParserFileFoundException;

public class PatternParser_X extends Parser {

	public PatternParser_X(String type) {
		super(type);
	}

	@Override
	public ParseResults parse(String utterance) throws NoParserFileFoundException {

		ParseResults results = new ParseResults(utterance);
		String fname = (this.getType()).split("\\.")[1] + ".txt";
		String itemFile = new File(".").getAbsolutePath() + "/res/entities/" + fname;
		File f = new File(itemFile);
		if (!f.exists()) {
			// do something
			throw new NoParserFileFoundException(
					"Missing pattern file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
		}
		ArrayList<String> patterns = new ArrayList<String>();
		ArrayList<String> result = new ArrayList<String>();

		FileInputStream fstream;
		try {
			fstream = new FileInputStream(itemFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				patterns.add(strLine);
			}
			// Close the input stream
			br.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// leave first line to avoid encoding issue
		for (int i = 1; i < patterns.size(); i++) {
			Matcher m = Pattern.compile(patterns.get(i)).matcher(utterance);
			while (m.find()) {
				result.add(utterance.substring(m.start(), m.end()));
			}
		}

		if (result.size() > 0) {
			// consider the first one only
			String item = result.get(0);
			int index = utterance.toLowerCase().indexOf(item.toLowerCase());
			results.add(new ParseResult(this.name, index, index + item.length() - 1, item, this.type,
					capitalizeWord(item)));
		}
		return results;
	}

	// to make first letter of each word capital
	public static String capitalizeWord(String str) {
		String words[] = str.split("\\s");
		String capitalizeWord = "";
		for (String w : words) {
			String first = w.substring(0, 1);
			String afterfirst = w.substring(1);
			capitalizeWord += first.toUpperCase() + afterfirst + " ";
		}
		return capitalizeWord.trim();
	}
}
