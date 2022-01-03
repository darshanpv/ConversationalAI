package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.bot.util.ConvertTextToNumeric;

public class NumberParser extends Parser {

	public NumberParser() {
		super("sys.number");
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
		//get the converted utterance
		ConvertTextToNumeric convertTextToNumeric = new ConvertTextToNumeric();
		String convertedUtterance = convertTextToNumeric.convert(utterance);
		ParseResults results = new ParseResults(convertedUtterance);
		this.match_regex(results, "\\d+", this.type);
		return results;
	}

}
