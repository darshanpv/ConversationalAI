package cto.hmi.processor.nlu.entityparser;


public class OnOffParser extends Parser{

	public OnOffParser() {
		super("sys.onoff");
	}

	@Override
	public ParseResults parse(String utterance) {
		
		ParseResults results = new ParseResults(utterance);
		
		super.match_regex(results,"on", this.type, "ON");
		super.match_regex(results,"off", this.type, "OFF");
	
		return results;
	}

}
