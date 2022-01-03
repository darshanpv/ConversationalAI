package cto.hmi.processor.nlu.entityparser;

public class DummyParser extends Parser {

	public DummyParser() {
		super("dummy");
	}

	@Override
	public ParseResults parse(String utterance) {

		ParseResults results = new ParseResults(utterance);
		if (utterance.trim().length() > 0 && !utterance.trim().startsWith("#"))
			results.add(new ParseResult(this.name, 0, utterance.length() - 1, utterance, this.type, utterance));
		return results;
	}

}