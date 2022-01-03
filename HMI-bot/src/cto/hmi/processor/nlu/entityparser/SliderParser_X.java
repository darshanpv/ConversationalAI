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

public class SliderParser_X extends Parser {

	public SliderParser_X(String type) {
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
					"Missing slider file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
		}
		ArrayList<String> params = new ArrayList<String>();
		ArrayList<String> items = new ArrayList<String>();
		float min, max;
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(itemFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				if (!strLine.startsWith("#"))
					params.add(strLine);
			}
			// Close the input stream
			br.close();
			min = Float.parseFloat(params.get(0));
			max = Float.parseFloat(params.get(1));

			Pattern p = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
			Matcher m = p.matcher(utterance);
			while (m.find()) {
				float number = Float.parseFloat(m.group());
				if (number >= min && number <= max)
					items.add(m.group());
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// the selection will take only first matching word
		if (items.size() >= 1) {
			int index = utterance.toLowerCase().indexOf(items.get(0).toLowerCase());
			// add category to ITO model
			results.add(new ParseResult(this.name, index, index + items.get(0).length() - 1, items.get(0).toLowerCase(),
					this.type, items.get(0)));
		}

		return results;
	}

}
