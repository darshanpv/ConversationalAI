package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactParser extends Parser {

	public ContactParser() {
		super("sys.contact");
	}

	protected void match_regex(ParseResults results, String regex,
			String className) {

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(results.getUtterance());

		while (m.find()) {
			results.add(new ParseResult(this.name, m.start(), m.end(), m
					.group(), type, m.group()));
		}
	}

	@Override
	public ParseResults parse(String utterance) {
		ParseResults results = new ParseResults(utterance);
		// check for time
		this.match_regex(results,
				"\\b(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\\b",
				this.type);
		return results;
	}

}
