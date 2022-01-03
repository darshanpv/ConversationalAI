package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailParser extends Parser {

	public MailParser() {
		super("sys.mail");
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
		// check for mail
		this.match_regex(results,
				"\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
				this.type);
		return results;
	}

}
