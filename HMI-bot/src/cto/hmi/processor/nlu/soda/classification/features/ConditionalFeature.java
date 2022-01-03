package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class ConditionalFeature extends Feature {

	HashSet<String> condWords=new HashSet<String>(Arrays.asList("could", "should","चाहिए","सकता","सकते"));
	
	public ConditionalFeature() {
		super("conditional");
	}

	@Override
	protected boolean hasFeature(String utterance) {
		for(String condWord : condWords){
			if (utterance.toLowerCase().contains(condWord)) return true;
		}
		return false;
	}

	
}
