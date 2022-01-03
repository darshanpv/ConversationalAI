package cto.hmi.processor.nlu.entityparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.bot.util.StopwordProcessor;
import cto.hmi.corpus.GetLocationFromCorpus;
import cto.hmi.ner.NerGetLocation;
import cto.hmi.processor.ConvEngineProcessor;

public class CityParser extends Parser {

	public CityParser() {
		super("sys.location.city");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		utterance = utterance.replace('?', ' ');
		utterance = utterance.replace('!', ' ');
		utterance = utterance.replace(',', ' ');
		// null pointer exception was coming for exude
		if (utterance.equals(null) || utterance.equals(""))
			utterance = " ";

		String decap_utterance = utterance.toLowerCase();
		String cleanUtterance = null;
		Boolean matchFound = false;
		ArrayList<String> result = new ArrayList<String>();
		List<String> splitUtterance = new ArrayList<String>();
		List<String> splitCleanUtterance = new ArrayList<String>();

		String location = "";

		// get cleaned up utterance

		cleanUtterance = StopwordProcessor.getInstance().process(utterance);

		// load the utterance to Array list splitUtterance
		for (String s : decap_utterance.split("\\s+")) {
			splitUtterance.add(s);
		}
		// load the cleaned utterance to Array list cleanSpliUtterance
		for (String s : cleanUtterance.split("\\s+")) {
			splitCleanUtterance.add(s);
		}
		// Rule-1 Check for City:<word> ; format through interactive widget
		if (!matchFound) {
			result.clear();
			if (utterance.trim().toLowerCase().contains("city")) {
				Pattern p = Pattern.compile("\\b[Cc]ity\\s?:(.+?);");
				Matcher m = p.matcher(utterance);
				while (m.find()) {
					result.add(utterance.substring(m.start(1), m.end(1)));
				}
			}
			// clean the arraylist.
			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));
			if (result.size() > 0) { // ner has resulted in matching org
				matchFound = true;
				location = result.get(0);
				int index_1 = utterance.toLowerCase().indexOf("city");
				int index_2 = utterance.toLowerCase().indexOf(";");
				results.add(new ParseResult(this.name, index_1, index_2, location, this.type, capitalizeWord(location)));
			}
		}
		// Rule-2 Check for corpus first
		if (!matchFound) {
			result.clear();
			GetLocationFromCorpus corpusGetLocation = new GetLocationFromCorpus();
			String found = corpusGetLocation.get(utterance);

			for (String s : found.split("\\|")) {
				if (!s.isEmpty() || !s.equals("")) // remove empty strings
					result.add(s);
			}
			// just clean the result arraylist.
			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));

			if (result.size() > 0) { // corpus has matching location
				matchFound = true;
				location = result.get(0);
				int index = utterance.toLowerCase().indexOf(location.toLowerCase());
				results.add(new ParseResult(this.name, index, index + location.length() - 1, location, this.type,
						capitalizeWord(location)));
			}
		}

		// Rule -3 Check for NER if match is not found
		if (!matchFound) {
			result.clear();
			ConvEngineProcessor.nerProcessor.process(utterance);
			String items = NerGetLocation.get();
			for (String s : items.split("\\|")) {
				if (!s.isEmpty() || !s.equals("")) // remove empty strings
					result.add(s);
			}
			if (result.size() > 0) { // ner has resulted in matching org
				matchFound = true;
				location = result.get(0);
				int index = utterance.toLowerCase().indexOf(location.toLowerCase());
				results.add(new ParseResult(this.name, index, index + location.length() - 1, location, this.type,
						capitalizeWord(location)));
			}
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
