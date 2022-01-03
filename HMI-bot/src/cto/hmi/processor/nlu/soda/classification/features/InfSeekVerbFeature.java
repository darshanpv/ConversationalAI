package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class InfSeekVerbFeature extends Feature {

	HashSet<String> verbs = new HashSet<String>(Arrays.asList("say me",
			"tell me", "know", "recommend", "बोलो", "बताइये", "कहो", "बताओ"));

	public InfSeekVerbFeature() {
		super("InfSeekVerb");
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
