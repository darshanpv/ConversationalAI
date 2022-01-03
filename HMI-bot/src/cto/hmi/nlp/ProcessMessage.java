package cto.hmi.nlp;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONTokener;

import cto.hmi.broker.DialogObject;
import cto.hmi.processor.ConvEngineProcessor;

public class ProcessMessage {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public static void process(String instance, DialogObject dialogObject) {
		// check if dialogInstance has 0 as the dialog number
		int dNumber = 1;

		Pattern p = Pattern.compile("\\d+-");
		Matcher m = p.matcher(instance);
		if (m.find())
			dNumber = Integer.parseInt((String) m.group(0).subSequence(0, m.group(0).length() - 1));
		logdetails(dNumber, dialogObject);

	}

	public static void logdetails(int dNumber, DialogObject dialogObject) {

		String ops = dialogObject.getMessageId();
		String operation = "";
		String opType = "";
		try {
			if (dNumber == 0) {
				operation = "NLP_OPERATION";
				// check if it is related to TRAIN or PREDICT
				if (ops.contains("TRAIN"))
					opType = "TRAIN_NLP";
				else if (ops.contains("PREDICT"))
					opType = "PREDICT_NLP";
			} else
				// ops is related to BOT as sessionId is not starting with d0-
				operation = "BOT_OPERATION";
			switch (operation) {

			case "NLP_OPERATION":

				switch (opType) {
				case "TRAIN_NLP":
					if (ops.contains("TRAIN_SUCCESS"))
						logger.info("[NLP_ENGINE] training completed for domain: " + dialogObject.getDomain()
								+ ", locale: " + dialogObject.getLocale());
					else
						logger.info("[NLP_ENGINE][SEVERE] failed to train for given domain: " + dialogObject.getDomain()
								+ " and locale: " + dialogObject.getLocale() + " ,check domain specific training data");
					logger.info("[NLP_ENGINE] result from NLP Engine: "
							+ new JSONTokener(dialogObject.getMessage()).nextValue().toString());
					break;

				case "PREDICT_NLP":
					logger.info(
							"[NLP_ENGINE] prediction completed for domain: " + dialogObject.getDomain() + ", locale: "
									+ dialogObject.getLocale() + ", utterance: " + dialogObject.getUserUtterance());
					String result = new JSONTokener(dialogObject.getMessage()).nextValue().toString();
					logger.info("[NLP_ENGINE] result from NLP Engine: " + result);
					break;

				default:
					logger.severe("[NLP_ENGINE][SEVERE] could not process the message, please check your domain");
					break;
				}
				break;

			case "BOT_OPERATION":
				// logger.info("[NLP_ENGINE] prediction completed for domain:
				// " + dialogObject.getDomain()
				// + ", locale: " + dialogObject.getLocale() + ", utterance: " +
				// dialogObject.getUserUtterance());
				String result = new JSONTokener(dialogObject.getMessage()).nextValue().toString();
				logger.info("[NLP_ENGINE] result from NLP Engine: " + result);
				break;

			default:
				logger.severe("[NLP_ENGINE][SEVERE] could not process the message, please check the format");
				break;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("[NLP_ENGINE][SEVERE] could not process the message, please check the format");

		}
	}

}
