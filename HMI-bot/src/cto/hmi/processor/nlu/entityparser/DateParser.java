package cto.hmi.processor.nlu.entityparser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser extends Parser {

	public DateParser() {
		super("sys.temporal.date");
	}

	protected void match_regex(ParseResults results, String regex, String className) {

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(results.getUtterance());

		while (m.find()) {
			/*
			 * results.add(new ParseResult(this.name, m.start(), m.end(), m
			 * .group(), type, DateFormat.getDateInstance( DateFormat.SHORT,
			 * Locale.US).parse(m.group())));
			 */
			results.add(new ParseResult(this.name, m.start(), m.end(), m.group(), type, m.group()));
		}
	}

	private void checkForWeekdays(String utterance, ParseResults results) {
		Calendar newDate = Calendar.getInstance();

		ArrayList<String> weekdays = new ArrayList<String>(
				Arrays.asList("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")); // sunday=1
		for (int i = 0; i <= 6; i++) {
			String day = weekdays.get(i);
			if (utterance.toLowerCase().contains(day)) {
				int daysToGo = (7 - Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) + i + 1;
				newDate.add(Calendar.DAY_OF_MONTH, daysToGo);
				int pos = utterance.toLowerCase().indexOf(day);
				results.add(new ParseResult(this.name, pos, pos + day.length() - 1, day, type,
						DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(newDate.getTime())));
			}
		}
	}

	@Override
	public ParseResults parse(String utterance) {
		ParseResults results = new ParseResults(utterance);
		// check for dates MM/DD/YYYY or MM-DD-YYYY format
		this.match_regex(results,
				"\\b(1[012]|0[1-9]|[1-9])([\\/\\-])(0[1-9]|[1-9]|[12]\\d|3[01])\\2((?:19|20)?\\d{2})\\b", this.type);
		// check for MMDDYYYY format
		this.match_regex(results, "\\b(0[1-9]|1[0-2])(0[1-9]|1\\d|2\\d|3[01])(19|20)\\d{2}\\b", this.type);
		//check for YYYY/MM/DD or YYYY-MM-DD format
		this.match_regex(results,
				"\\b((19|2[0-9])[0-9]{2})([\\/\\-])(0[1-9]|1[012])([\\/\\-])(0[1-9]|[12][0-9]|3[01])\\b", this.type);
		// check for words referencing dates
		if (results.isEmpty()) {
			Calendar newDate = Calendar.getInstance();
			String pattern = "\\b(today|tomorrow|yesterday|(day after tomorrow)|(day before yesterday))\\b";
			int pos;
			int daysToGo = 0;
			Matcher m = Pattern.compile(pattern).matcher(utterance.toLowerCase());
			if (m.find()) {
				String day = utterance.toLowerCase().substring(m.start(), m.end());
				switch (day) {

				case "today":
					daysToGo = 0;
					break;

				case "tomorrow":
					daysToGo = 1;
					break;

				case "yesterday":
					daysToGo = -1;
					break;

				case "day after tomorrow":
					daysToGo = 2;
					break;

				case "day before yesterday":
					daysToGo = -2;
					break;

				}
				newDate.add(Calendar.DAY_OF_MONTH, daysToGo);
				pos = utterance.toLowerCase().indexOf(day);
				results.add(new ParseResult(this.name, pos, pos + day.length() - 1, day, type,
						DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(newDate.getTime())));
			} else
				checkForWeekdays(utterance.toLowerCase(), results);
		}
		return results;
	}

}
