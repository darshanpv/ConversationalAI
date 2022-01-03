package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser extends Parser {

	public TimeParser() {
		super("sys.temporal.time");
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
				"\\b((1[0-2]|0?[1-9])[:.]?([0-5][0-9])?\\s?([AaPp][Mm]))\\b",
				this.type);
		// check for words referencing time
		if (results.isEmpty()) {
			String pattern = "\\b((early morning)|morning|noon|afternoon|evening|(late evening)|night|midnight)\\b";
			int pos;
			String hour = "0 AM";
			Matcher m = Pattern.compile(pattern).matcher(
					utterance.toLowerCase());
			if (m.find()) {
				String time = utterance.toLowerCase().substring(m.start(), m.end());
				switch (time) {
				case "early morning":
					hour = "6 AM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " AM";
					break;
				case "morning":
					hour = "9 AM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " AM";
					break;
				case "noon":
					hour = "12 PM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " PM";
					break;
				case "afternoon":
					hour = "3 PM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " PM";
					break;
				case "evening":
					hour = "6 PM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " PM";
					break;
				case "late evening":
					hour = "9 PM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " PM";
					break;
				case "night":
					hour = "9 PM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " PM";
					break;
				case "midnight":
					hour = "12 AM";
					if (utterance.replaceAll("[^0-9]", "").length() > 0)
						hour = utterance.replaceAll("[^0-9]", "") + " AM";
					break;

				}

				pos = utterance.toLowerCase().indexOf(time);
				results.add(new ParseResult(this.name, pos, pos + time.length()
						- 1, time, type, hour));
			}
		}
		return results;
	}

}
