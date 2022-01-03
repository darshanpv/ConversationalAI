package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class WhWordFeature extends Feature {

	HashSet<String> whWords = new HashSet<String>(Arrays.asList("where",
			"when", "why", "who", "what", "whom", "how", "कहा", "किधर", "कब",
			"क्यों", "कौन", "किसने","क्या","किसको","किससे","क्या","किस तरह","कैसे"));

	public WhWordFeature() {
		super("WhWord");
	}

	@Override
	protected boolean hasFeature(String utterance) {
		for (String whWord : whWords) {
			if (utterance.toLowerCase().contains(whWord))
				return true;
		}
		return false;
	}

}
