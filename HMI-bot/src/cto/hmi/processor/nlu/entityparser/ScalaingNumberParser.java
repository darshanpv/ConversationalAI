package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cto.hmi.bot.util.ConvertCurrencyToNumeric;

public class ScalaingNumberParser extends Parser {
	String original_utterance = "";

	public ScalaingNumberParser() {
		super("sys.number.scale");
	}

	protected void match_regex(ParseResults results, String regex,
			String className) {

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(results.getUtterance());

		while (m.find()) {
			//need to set m.end asper original string
			int newEnd=0;
			Pattern pNew = Pattern.compile(m.group().charAt(0)+"\\w+");
			Matcher mNew = pNew.matcher(original_utterance);
			while (mNew.find())
			{
			    newEnd=mNew.end();
			}
			//results.add(new ParseResult(this.name, m.start(), m.end(), m
			//		.group(), type, m.group()));
			results.add(new ParseResult(this.name, m.start(), newEnd, m
					.group(), type, m.group()));
		}
	}

	@Override
	public ParseResults parse(String utterance) {
		//get the converted utterance
		ConvertCurrencyToNumeric convertCurrencyToNumeric = new ConvertCurrencyToNumeric();
		original_utterance = utterance;
		String convertedUtterance = convertCurrencyToNumeric.convert(utterance);
		ParseResults results = new ParseResults(convertedUtterance);
		this.match_regex(results, "\\d+", this.type);
		return results;
	}

}
