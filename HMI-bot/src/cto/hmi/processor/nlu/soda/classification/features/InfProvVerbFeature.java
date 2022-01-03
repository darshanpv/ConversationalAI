package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class InfProvVerbFeature extends Feature {

	HashSet<String> verbs = new HashSet<String>(Arrays.asList("want to",
			"need to", "have to", "like to", "चाहता", "जाना", "चाहिए"));

	public InfProvVerbFeature() {
		super("InfProvVerb");
	}

	@Override
	protected boolean hasFeature(String utterance) {
		for (String verb : verbs) {
			if (utterance.toLowerCase().contains(verb))
				return true;
		}
		return false;
	}

}
