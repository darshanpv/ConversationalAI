package cto.hmi.processor.nlu.entityparser;

import java.util.HashSet;
import java.util.Set;

import cto.hmi.processor.exceptions.NoParserFileFoundException;
import cto.hmi.processor.exceptions.NoParserFoundException;

public class Parsers {

	private static Set<Parser> active_parsers = new HashSet<Parser>();

	public static void init() {
		// enabled parsers
		if (active_parsers.isEmpty()) {
			active_parsers.add(new YNParser());
			active_parsers.add(new CityParser());
			active_parsers.add(new PersonParser());
			active_parsers.add(new FirstNameParser());
			active_parsers.add(new LastNameParser());
			active_parsers.add(new OrganizationParser());
			active_parsers.add(new DateParser());
			active_parsers.add(new TimeParser());
			active_parsers.add(new ContactParser());
			active_parsers.add(new MailParser());
			active_parsers.add(new PasswordParser());
			active_parsers.add(new OpenEndedParser());
			active_parsers.add(new NumberParser());
			active_parsers.add(new ScalaingNumberParser());
			active_parsers.add(new FloatingNumberParser());
			active_parsers.add(new OnOffParser());
			active_parsers.add(new OpenTextParser());
			active_parsers.add(new QAParser());
			active_parsers.add(new DummyParser());
			active_parsers.add(new UnfilledParser());
		}
	}

	public static Set<Parser> getParserForType(String type) {
		Set<Parser> matchingParsers = new HashSet<Parser>();

		for (Parser parser : active_parsers) {
			if (parser.getType().equals(type)) {
				matchingParsers.add(parser);
			}
		}
		if (type.startsWith("custom.item")) {
			Parser p = new ItemParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.menu")) {
			Parser p = new MenuParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.button")) {
			Parser p = new ButtonParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.pattern")) {
			Parser p = new PatternParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.urlList")) {
			Parser p = new UrlListParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.multiItem")){
			Parser p = new MultiItemParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.multiUrlList")){
			Parser p = new MultiUrlListParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.classifier")){
			Parser p = new ClassifierParser_X(type);
			matchingParsers.add(p);
		}
		else if (type.startsWith("custom.slider")){
			Parser p = new SliderParser_X(type);
			matchingParsers.add(p);
		}

		return matchingParsers;
	}

	public static ParseResults parseExact(String utterance, String type)
			throws NoParserFoundException, NoParserFileFoundException {
		Set<Parser> matchingParsers = getParserForType(type);
		return parse(matchingParsers, utterance);
	}

	public static ParseResults parseWithAllParsers(String utterance)
			throws NoParserFoundException, NoParserFileFoundException {
		return parse(active_parsers, utterance);
	}

	private static ParseResults parse(Set<Parser> parsers, String utterance)
			throws NoParserFoundException, NoParserFileFoundException {

		ParseResults resultList = new ParseResults(utterance);
		if (parsers.size() > 0) {
			for (Parser p : parsers) {
				ParseResults res = p.parse(utterance);
				resultList.addAll(res);
			}
			return resultList;
		} else
			throw new NoParserFoundException();
	}
}
