package cto.hmi.processor.nlu.entityparser;

public class UnfilledParser extends Parser {

	public UnfilledParser() {
		super("unfilled");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		if (utterance.matches("^.*?\\b(!@#$%)\\b.*?")) {
			int index = utterance.toLowerCase().indexOf(utterance.toLowerCase());

			results.add(
					new ParseResult(this.name, index, index + utterance.length() - 1, utterance, this.type, utterance));
		}
		return results;
	}
}
