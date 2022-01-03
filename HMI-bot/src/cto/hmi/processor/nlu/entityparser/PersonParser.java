package cto.hmi.processor.nlu.entityparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.ner.NerGetPerson;
import cto.hmi.processor.ConvEngineProcessor;

public class PersonParser extends Parser {

	public PersonParser() {
		super("sys.person");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		utterance = utterance.replace('?', ' ');
		utterance = utterance.replace('!', ' ');
		utterance = utterance.replace(',', ' ');

		String decap_utterance = utterance.toLowerCase();
		String person = "";
		Boolean matchFound = false;
		ArrayList<String> result = new ArrayList<String>();

		// Rule -1 Check for City:<word> & format
		if (!matchFound) {
			result.clear();
			if (utterance.trim().toLowerCase().contains("person")) {
				Pattern p = Pattern.compile("\\b[Pp]erson\\s?:(.+?);");
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
				person = result.get(0);
				// capitalizedPerson = Character.toString(person.charAt(0)).toUpperCase()
				// + person.toLowerCase().substring(1);
				int index_1 = utterance.toLowerCase().indexOf("person");
				int index_2 = utterance.toLowerCase().indexOf(";");
				results.add(new ParseResult(this.name, index_1, index_2, person, this.type, capitalizeWord(person)));
			}
		}
		// Rule -2 Check for if NER match is found
		if (!matchFound) {
			result.clear();
			// Adding NER extracted from utterance to above Array list
			ConvEngineProcessor.nerProcessor.process(utterance);
			String items = "";
			items = NerGetPerson.get();
			if (items != null && !items.toString().isEmpty()) {
				for (String s : items.split("\\|")) {
					result.add(s.trim().toLowerCase());
				}
			}
			// clean the arraylist.
			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));

			ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(decap_utterance.split(" ")));// tokenize
			tokens.retainAll(result);

			String value = "";
			if (tokens.size() > 0) {
				matchFound = true;
				for (String person1 : tokens) {
					// capitalizedPerson = Character.toString(person1.charAt(0)).toUpperCase() +
					// person1.substring(1);
					if (utterance.contains(person1)) {
						value = person1;
					}
					int index = utterance.indexOf(value);
					results.add(new ParseResult(this.name, index, index + value.length() - 1, value, this.type,
							capitalizeWord(person1)));
				}
			}
		}

		// Rule -3 Get the first two words starting with Capital letters e.g. John Doe
		if (!matchFound) {
			result.clear();
			Pattern p = Pattern.compile("\\b[A-Z][a-zA-Z]*(?:\\s+[A-Z][a-zA-Z]*)\\b");
			Matcher m = p.matcher(utterance);
			while (m.find()) {
				result.add(utterance.substring(m.start(), m.end()));
			}
			if (result.size() > 0) {
				matchFound = true;
				person = result.get(0);
				int index_1 = utterance.toLowerCase().indexOf("person");
				int index_2 = utterance.toLowerCase().indexOf(";");
				results.add(new ParseResult(this.name, index_1, index_2, person, this.type, capitalizeWord(person)));
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
