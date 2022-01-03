package cto.hmi.processor.nlu.soda.classification.features;

import java.util.Arrays;
import java.util.HashSet;

public class InfProvFromInteractionFeature extends Feature {
	// This type of utterance will come when user wants to fill in interactive
	// form and send the information in one go

	HashSet<String> verbs = new HashSet<String>(Arrays.asList(
			"\\bcity\\s?:(.+?);", "\\bperson\\s?:(.+?);",
			"\\bfirstname\\s?:(.+?);", "\\blastname\\s?:(.+?);",
			"\\borganisation\\s?:(.+?);", "(.*;){2,}"));

	public InfProvFromInteractionFeature() {
		super("InfProvFromInteractionFeature");
	}

	@Override
	protected boolean hasFeature(String utterance) {
		for (String verb : verbs) {
			if (utterance.toLowerCase().matches(verb))
				return true;
		}
		return false;
	}

}
