package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class ActReqVerbFeature extends Feature {

	HashSet<String> verbs = new HashSet<String>(Arrays.asList("switch", "turn",
			"close", "open", "बंद", "दूर", "खोलो", "शुरू", "घुमाओ", "चालू"));

	public ActReqVerbFeature() {
		super("ActReqVerb");
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
