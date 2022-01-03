package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordParser extends Parser {

	public PasswordParser() {
		super("sys.password");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		String[] words = utterance.split(" ");
		boolean found = false;
		String password = "";
		for (int i = 0; i < words.length; i++) {
			if (Password_Validation(words[i])) {
				found = true;
				password = words[i];
				break;
			}
		}
		if (found) {
			int index = utterance.indexOf(password);
			results.add(
					new ParseResult(this.name, index, index + password.length() - 1, password, this.type, password));

		}
		return results;
	}

	private static boolean Password_Validation(String password) {

		if (password.length() >= 8) {
			Pattern letter = Pattern.compile("[a-zA-z]");
			Pattern digit = Pattern.compile("[0-9]");
			Pattern special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");

			Matcher hasLetter = letter.matcher(password);
			Matcher hasDigit = digit.matcher(password);
			Matcher hasSpecial = special.matcher(password);

			return hasLetter.find() && hasDigit.find() && hasSpecial.find();

		} else
			return false;

	}

}
