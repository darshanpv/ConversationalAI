package cto.hmi.processor.nlu.entityparser;

public class OpenTextParser extends Parser {

	public OpenTextParser() {
		super("sys.opentext");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		if (utterance.trim().length() > 0 && !utterance.trim().startsWith("#"))
			results.add(new ParseResult(this.name, 0, utterance.length() - 1, utterance, this.type, utterance));
		return results;
	}

}
