package cto.hmi.processor.nlu.soda.classification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.processor.manager.DialogManagerContext;
import cto.hmi.processor.nlu.soda.Soda;
import cto.hmi.processor.nlu.soda.classification.features.ActReqVerbFeature;
import cto.hmi.processor.nlu.soda.classification.features.ConditionalFeature;
import cto.hmi.processor.nlu.soda.classification.features.DummyFeature;
import cto.hmi.processor.nlu.soda.classification.features.Feature;
import cto.hmi.processor.nlu.soda.classification.features.FewWordsFeature;
import cto.hmi.processor.nlu.soda.classification.features.InfProvFromInteractionFeature;
import cto.hmi.processor.nlu.soda.classification.features.InfProvVerbFeature;
import cto.hmi.processor.nlu.soda.classification.features.InfSeekVerbFeature;
import cto.hmi.processor.nlu.soda.classification.features.InterrogativeFeature;
import cto.hmi.processor.nlu.soda.classification.features.NoCueVerbAndNoWhFeature;
import cto.hmi.processor.nlu.soda.classification.features.NoCueVerbFeature;
import cto.hmi.processor.nlu.soda.classification.features.WhWordFeature;
import cto.hmi.processor.utterance.TrainingUtterance;
import cto.hmi.processor.utterance.UserUtterance;

public class SodaRecognizer {

	private static SodaRecognizer instance;
	private MaximumEntropyModel model;

	// private final static Logger logger = ConvEngineProcessor.getLogger();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SodaRecognizer sr = new SodaRecognizer();
		sr.train();
		sr.test_predict();
	}

	private SodaRecognizer() {
	}

	public static SodaRecognizer getInstance() {
		if (instance == null) {
			instance = new SodaRecognizer();
		}
		return instance;
	}

	public boolean isTrained() {
		return (model != null);
	}

	public void train() {
		ArrayList<TrainingUtterance> training_utterances = new ArrayList<TrainingUtterance>();
		ArrayList<String> seekUtterances = new ArrayList<String>();
		ArrayList<String> provUtterances = new ArrayList<String>();
		ArrayList<String> actionUtterances = new ArrayList<String>();

		String sodaFile = new File(".").getAbsolutePath()
				+ "/res/dictionary/soda.json";
		JSONObject sodaObject;
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(sodaFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			String strLine;
			String result = "";
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				result = result + strLine.trim();

			}
			// Close the input stream
			br.close();
			sodaObject = new JSONObject(result);

			JSONObject sodaSeekObject = sodaObject.getJSONArray("tasks")
					.getJSONObject(0);
			JSONObject sodaProvObject = sodaObject.getJSONArray("tasks")
					.getJSONObject(1);
			JSONObject sodaActionObject = sodaObject.getJSONArray("tasks")
					.getJSONObject(2);
			for (int i = 0; i < sodaSeekObject.getJSONArray("utterances")
					.length(); i++) {
				seekUtterances.add(sodaSeekObject.getJSONArray("utterances")
						.getString(i));
			}
			for (int i = 0; i < sodaProvObject.getJSONArray("utterances")
					.length(); i++) {
				provUtterances.add(sodaProvObject.getJSONArray("utterances")
						.getString(i));
			}
			for (int i = 0; i < sodaActionObject.getJSONArray("utterances")
					.length(); i++) {
				actionUtterances.add(sodaActionObject
						.getJSONArray("utterances").getString(i));
			}
		} catch (IOException | JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// get all the seek specific utterances
		for (int i = 0; i < seekUtterances.size(); i++) {
			training_utterances.add(new TrainingUtterance(
					seekUtterances.get(i), Soda.INFORMATION_SEEKING));
		}
		// get all the prov specific utterances
		for (int i = 0; i < provUtterances.size(); i++) {
			training_utterances.add(new TrainingUtterance(
					provUtterances.get(i), Soda.INFORMATION_PROVIDING));
		}
		// get all the action specific utterances
		for (int i = 0; i < actionUtterances.size(); i++) {
			training_utterances.add(new TrainingUtterance(actionUtterances
					.get(i), Soda.ACTION_REQUESTING));
		}
		extractFeatures(training_utterances); // adds features to utterances (by
												// reference); required by
												// MaximumEntropyModel
		model = new MaximumEntropyModel();
		model.train(training_utterances);
	}

	public void predict(UserUtterance utterance, DialogManagerContext context) {
		extractFeature(utterance);
		String act = (model != null) ? model.predict(utterance) : "unknown";
		utterance.setSoda(act);
	}

	public void test_predict() {
		ArrayList<UserUtterance> test_utterances = new ArrayList<UserUtterance>(
				Arrays.asList(new UserUtterance(
						"Could you recommend a hotel for next week"),
						new UserUtterance("Close the window"),
						new UserUtterance("When do you want to come back"),
						new UserUtterance("Can you close the door"),
						new UserUtterance("Paris is nice"), new UserUtterance(
								"Two adults please"), new UserUtterance(
								"I want to have a non smokers room"),
						new UserUtterance("How much would that be"),
						new UserUtterance("Can I have a double room"),
						new UserUtterance("Three persons"), new UserUtterance(
								"London would be great"),
						// new UserUtterance("What about London"),
						new UserUtterance("I really prefer London"),
						// new UserUtterance("What the hell, London it is"),
						// //tricky
						new UserUtterance(
								"I want to know about hotels in London"),// could
																			// also
																			// be
																			// an
																			// answer
																			// to
																			// "What accommodation may I book for you?"
						new UserUtterance("चालू  करो"), new UserUtterance(
								"कंपनी का नाम गूगल है"), new UserUtterance(
								"मुझे टिकट बुक करना है")));

		extractFeatures(test_utterances);
		model.predict(test_utterances);
	}

	public void extractFeature(UserUtterance utterance) {
		ArrayList<UserUtterance> utterances = new ArrayList<UserUtterance>();
		utterances.add(utterance);
		extractFeatures(utterances);
	}

	public void extractFeatures(ArrayList<? extends UserUtterance> utterances) {

		// Select Feature Extractors
		ArrayList<Feature> featureExtractors = new ArrayList<Feature>(
				Arrays.asList(new DummyFeature(), new ActReqVerbFeature(),
						new ConditionalFeature(), new InfSeekVerbFeature(),
						new InfProvVerbFeature(),
						new InfProvFromInteractionFeature(),
						new InterrogativeFeature(), new WhWordFeature(),
						new NoCueVerbFeature(), new NoCueVerbAndNoWhFeature(),
						new FewWordsFeature()));

		// Extract Features
		for (UserUtterance utterance : utterances) {
			for (Feature extractor : featureExtractors) {
				extractor.analyze(utterance.getText().toLowerCase(),
						utterance.getFeatures());
			}
		}

	}

}
