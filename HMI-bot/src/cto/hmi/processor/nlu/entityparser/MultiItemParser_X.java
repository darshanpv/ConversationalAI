package cto.hmi.processor.nlu.entityparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import cto.hmi.processor.exceptions.NoParserFileFoundException;

public class MultiItemParser_X extends Parser {

	public MultiItemParser_X(String type) {
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
					"Missing item file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
		}
		ArrayList<String> items = new ArrayList<String>();
		ArrayList<String> categories = new ArrayList<String>();

		FileInputStream fstream;
		try {
			fstream = new FileInputStream(itemFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// check if it just empty line
				if (!(strLine.trim().length() > 0))
					continue;
				int index = 0;
				String item = "";
				String category = "";
				if (!strLine.startsWith("#") && strLine.contains("="))
					index = strLine.indexOf("=");

				if (index > 0) {
					// populate category as auto and item as car e.g. car=auto
					category = strLine.substring(index + 1, strLine.length()).trim();
					item = strLine.substring(0, index).trim();
				} else {
					// same goes for item and category
					item = strLine.trim();
					category = item;
				}
				if (!item.startsWith("#") && utterance.matches("(?i)^.*?\\b" + item + "\\b.*?")) {
					items.add(item);
					categories.add(category);
				}
			}
			// Close the input stream
			br.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// get total matched result
		int i = items.size() - 1;
		// the selection will take only first matching word
		if (items.size() >= 1) {
			int index1 = utterance.toLowerCase().indexOf(items.get(0).toLowerCase());
			int index2 = utterance.toLowerCase().indexOf(items.get(i).toLowerCase()) + items.get(i).length() - 1;
			// add category to ITO model
			results.add(new ParseResult(this.name, index1, index2, items.get(0).toLowerCase(), this.type,
					StringUtils.join(categories, ',')));
		}

		return results;
	}

}
