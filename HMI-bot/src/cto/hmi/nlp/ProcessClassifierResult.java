package cto.hmi.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.javatuples.Quartet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.processor.ConvEngineProcessor;

public class ProcessClassifierResult {

	private final static Logger logger = ConvEngineProcessor.getLogger();
	private final static float CONFIDENCE_ENTITY = 0.8f;

	public static HashMap<String, String> processIntent(String ret, Float threshold, Float similarityIndex,
			Boolean checkSimilarity) {
		HashMap<String, String> result = new HashMap<String, String>();
		ArrayList<String> intentArray = new ArrayList<String>();
		ArrayList<Float> scoreArray = new ArrayList<Float>();
		ArrayList<String> utteranceArray = new ArrayList<String>();

		try {
			result.clear();
			if (isJSONValid(ret)) {
				JSONObject responseObj = new JSONObject(ret);
				if (responseObj.has("intent_ranking")) {
					JSONArray intentRank = (JSONArray) responseObj.get("intent_ranking");
					for (int i = 0; i < intentRank.length(); i++) {
						JSONObject intentObj = intentRank.getJSONObject(i);
						intentArray.add(intentObj.getString("name"));
						utteranceArray.add(intentObj.getString("utterance"));
						scoreArray.add(Float.valueOf(intentObj.getString("confidence")));
					}
				}
			} else {
				logger.severe("could not process request. NLP engine failed to provide any results");
				result.put("IE_ERROR", "error in intent engine while predicting intent");
				return result;
			}

			if (intentArray.size() < 3 || scoreArray.size() < 3 || utteranceArray.size() < 3) {
				logger.severe("could not process further as NLP engine failed to provide top three results");
				result.put("IE_ERROR", "error in intent engine while predicting intent");
				return result;
			} else {
				Float diff_2 = scoreArray.get(1) - scoreArray.get(2);
				Float diff_1 = scoreArray.get(0) - scoreArray.get(1);
				// this is a case where all three intents qualify for user
				// confirmation

				if (diff_2 <= similarityIndex && scoreArray.get(2) >= threshold && checkSimilarity) {
					checkSimilarity = false;
					result.put("INTENT_CLARIFICATION",
							utteranceArray.get(0) + "|" + utteranceArray.get(1) + "|" + utteranceArray.get(2));
					return result;
				} else if (diff_1 <= similarityIndex && scoreArray.get(1) >= threshold && checkSimilarity) {
					checkSimilarity = false;
					result.put("INTENT_CLARIFICATION", utteranceArray.get(0) + "|" + utteranceArray.get(1));
					return result;
				} else if (scoreArray.get(0) >= threshold) {
					checkSimilarity = true;
					result.put("INTENT_FOUND", intentArray.get(0));
					return result;
				} else {
					result.put("INTENT_NOT_FOUND", "NA");
					return result;
				}
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static LinkedList<Quartet<String, String, String, String>> processEntities(String ret) {
		LinkedList<Quartet<String, String, String, String>> result = new LinkedList<Quartet<String, String, String, String>>();
		String userUtterance = "";
		result.clear();
		try {
			if (isJSONValid(ret)) {
				JSONObject responseObj = new JSONObject(ret);
				// get user Utterance
				if (responseObj.has("text"))
					userUtterance = responseObj.getString("text");

				if (responseObj.has("entities")) {
					JSONArray entities = (JSONArray) responseObj.get("entities");
					for (int i = 0; i < entities.length(); i++) {
						JSONObject entityObject = entities.getJSONObject(i);
						if (entityObject.has("confidence_entity")) {
							if (Float.parseFloat(entityObject.getString("confidence_entity")) > CONFIDENCE_ENTITY) {
								String matched = userUtterance.substring(entityObject.getInt("start"),
										entityObject.getInt("end"));
								result.add(new Quartet<String, String, String, String>(entityObject.getString("entity"),
										entityObject.getString("value"), matched,
										entityObject.getInt("start") + "-" + entityObject.getInt("end")));
							}
						}

					}
				}
			} else {
				logger.severe("could not process request. NLP engine failed to provide any enitities");
				return result;
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	private static boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			// edited, to include @Arthur's comment
			// e.g. in case JSONArray is valid as well...
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}
}
