package cto.hmi.processor.nlu.entityparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.bot.util.StopwordProcessor;
import cto.hmi.corpus.GetLastNameFromCorpus;

public class LastNameParser extends Parser {

	public LastNameParser() {
		super("sys.person.lastname");
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

		String person = "";
		/*
		 * Here are set of rules that we plan to execute
		 */
		/*
		 * 1. We clean up the utterance and load it into Arraylist.
		 * 
		 * Rule-1. Check if user has uttered only one word. we add it to
		 * gazetteer and close. (not required as it will loop)
		 */
		/*
		 * Rule-2.Check if it is more than 2 word and contains word "Last" then
		 * it has come for overwriting. We remove the "Last" word from utterance
		 * and add to gazetteer and close.
		 */
		/*
		 * Rule-3.Check that it contain word "Name" (to ensure that the context
		 * is about person),does not contain word "first" or "last" and has more
		 * than 1 word after clean_up and modified by previous utterance then
		 * user is providing first name and last name. We only add first string
		 * from array to gazetteer and close.
		 */
		/*
		 * 4. We allow the corpus and NER<TBD about NER> to figure out the
		 * matching names
		 */

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
		// Check for LastName:<word> ; format
		if (!matchFound) {
			result.clear();
			if (utterance.trim().toLowerCase().contains("lastname")) {
				Pattern p = Pattern.compile("\\b[Ll]ast[Nn]ame\\s?:(.+?);");
				Matcher m = p.matcher(utterance);
				while (m.find()) {
					result.add(utterance.substring(m.start(1), m.end(1)));
				}
			}
			// clean the array
			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));
			if (result.size() > 0) { // ner has resulted in matching org
				matchFound = true;
				person = result.get(0);
				int index_1 = utterance.toLowerCase().indexOf("lastname");
				int index_2 = utterance.toLowerCase().indexOf(";");
				results.add(new ParseResult(this.name, index_1, index_2, person, this.type, capitalizeWord(person)));
			}
		}
		if (!matchFound) {
			result.clear();
			// Rule-2
			if (splitCleanUtterance.size() > 1 && splitCleanUtterance.contains("last")) {
				splitCleanUtterance.removeAll(Arrays.asList("person", "last", "gentleman"));
				result.add(splitCleanUtterance.get(0));
			}
			// Rule-3
			/*
			 * if ((splitUtterance.contains("name")) &&
			 * (!(splitUtterance.contains("first")) && !(splitUtterance
			 * .contains("last")) && !(splitUtterance.contains("company"))) &&
			 * splitCleanUtterance.size() > 0) {
			 * result.add(splitCleanUtterance.get(0)); }
			 */
			if (decap_utterance.matches(".*\\bname\\b.*")
					&& !decap_utterance.matches("^.*?\\b(first|last|company)\\b.*?")
					&& splitCleanUtterance.size() > 0) {
				result.add(splitCleanUtterance.get(0));
			}
			// clean the arraylist.

			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));

			if (result.size() > 0) { // Either of Rule-2 or Rule-3 is true
				matchFound = true;
				person = result.get(0);
				int index = utterance.toLowerCase().indexOf(person.toLowerCase());
				results.add(new ParseResult(this.name, index, index + person.length() - 1, person, this.type,
						capitalizeWord(person)));
			}
		}

		// check the result in corpus
		if (!matchFound) {
			result.clear();
			GetLastNameFromCorpus corpusGetLastName = new GetLastNameFromCorpus();
			String found = corpusGetLastName.get(utterance);

			for (String s : found.split("\\|")) {
				if (!s.isEmpty() || !s.equals("")) // remove empty strings
					result.add(s);
			}
			// clean the array
			result.removeAll(Collections.singleton(null));
			result.removeAll(Collections.singleton(""));
			result.removeAll(Collections.singleton(" "));

			if (result.size() > 0) { // Last Name found in Corpus
				matchFound = true;
				person = result.get(0);
				int index = utterance.toLowerCase().indexOf(person.toLowerCase());
				results.add(new ParseResult(this.name, index, index + person.length() - 1, person, this.type,
						capitalizeWord(person)));
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
