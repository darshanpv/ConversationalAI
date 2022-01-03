package cto.hmi.processor.nlu.entityparser;

public class OpenEndedParser extends Parser {

	public OpenEndedParser() {
		super("open_ended");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		if (utterance.toLowerCase().matches("^.*?\\b(bye|exit)\\b.*?")) {
			int index = utterance.toLowerCase().indexOf(utterance.toLowerCase());

			results.add(
					new ParseResult(this.name, index, index + utterance.length() - 1, utterance, this.type, utterance));
		}
		// results.add(new
		// ParseResult(this.name,0,0,"dummy",this.type,"dummy"));
		return results;
	}

}
