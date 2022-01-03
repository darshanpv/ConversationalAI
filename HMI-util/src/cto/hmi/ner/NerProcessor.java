package cto.hmi.ner;

import java.io.File;
import java.io.IOException;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class NerProcessor {
	static AbstractSequenceClassifier<CoreLabel> classifier;
	static String nerOutput = "";

	public NerProcessor() {
		init();
	}

	private void init() {
		// TODO Auto-generated method stub
		String path = new File(".").getAbsolutePath();
		String serializedClassifier = path + "/res/classifiers/english.all.3class.distsim.crf.ser.gz";
		try {
			classifier = CRFClassifier.getClassifier(serializedClassifier);
		} catch (ClassCastException | ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void process(String utterance) {
		nerOutput = classifier.classifyWithInlineXML(utterance);
		nerOutput = "<main> <data>" + nerOutput + "</data></main>";
	}

}
