package cto.hmi.processor.dialogmodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.model.definition.ActionModel;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.ui.ConsoleInterface;
import cto.hmi.processor.ui.RESTInterface;
import cto.hmi.processor.ui.UIConsumer;


public abstract class Action extends ActionModel {

	private final static Logger logger = ConvEngineProcessor.getLogger();
	// non-serializable features
	protected HashMap<String, String> executionResults = new HashMap<String, String>();

	public Action() {
		super();
	}

	public Action(String utteranceTemplate) {
		super(utteranceTemplate);
	}

	// content
	public abstract HashMap<String, String> execute(Frame frame);

	// fills template with values from frame
	public String executeAndGetAnswer(Task t) {
		UIConsumer instance = null;
		String answer = "";
		String sessionId = "";
		Frame frame = t.toFrame();
		if (frame.get("sessionId_") != null)
			sessionId = (String) frame.get("sessionId_");
		// check if it is a console mode or rest interface mode
		if (!sessionId.equals("d1-DUMMYSESSION")) // rest interface mode
			instance = RESTInterface.getInstance(sessionId);
		else // console mode
			instance = ConsoleInterface.getInstance();

		Map<String, String> executionResults = execute(frame);
		if (isReturnAnswer()) {
			answer = replaceSlotMarkers(utteranceTemplate, frame);
			// answer = replaceSlotMarkersFromITOMapping(answer,
			// instance.getITOMapping());
			answer = replaceExecutionResultMarkers(answer, executionResults);

		}
		// add further message to utterance depending on result (extends
		// utteranceTemplate)
		ActionResultMapping resultMapping = getFirstMatchingResultMapping();
		if (resultMapping != null) {
			String conditionalAnswer;
			conditionalAnswer = resultMapping.getMessage();
			conditionalAnswer = replaceSlotMarkers(conditionalAnswer, frame);
			// conditionalAnswer =
			// replaceSlotMarkersFromITOMapping(conditionalAnswer,
			// instance.getITOMapping());
			conditionalAnswer = replaceExecutionResultMarkers(conditionalAnswer, executionResults);
			//
			answer = answer + " " + conditionalAnswer;
		}
		// see if the hash map contains any ITO key if user wants to change
		// it
		for (Entry<String, String> entry : executionResults.entrySet()) {
			if (instance.getEntityMapping().containsKey(entry.getKey())) {
				logger.info("modifying the ITO: " + entry.getKey() + " to new value:" + entry.getValue());
				instance.setEntityMapping(entry.getKey(), entry.getValue());
			}
			// added to check if there is any global ITO defined
			if (entry.getKey().endsWith("_")) {
				if (instance.getEntityMapping().containsKey(entry.getKey()))
					logger.info("modifying existing global ITO: " + entry.getKey() + " with value: " + entry.getValue());
				else
					logger.info(
							"adding global ITO: " + entry.getKey() + " with value: " + entry.getValue() + " to ITOs");
				instance.addtoEntityMapping(entry.getKey(), entry.getValue());
			}
		}

		return answer;
	}

	// Returns the first ActionResultMapping that matches the values in the
	// executionResults.
	public ActionResultMapping getFirstMatchingResultMapping() {
		if (resultMappingList != null) {
			for (ActionResultMapping resultMapping : resultMappingList) {
				if (executionResults.containsKey(resultMapping.getResultVarName())) {
					if (executionResults.get(resultMapping.getResultVarName()).equals(resultMapping.getResultValue())) {
						return resultMapping;
					}
				}
			}
		}
		return null;
	}

	// marker indicated by hash sign (#), i.e. references to execution results
	protected static String replaceExecutionResultMarkers(String template, Map<String, String> executionResults) {
		String answer = "";
		if (template != null && template.length() > 0) {
			answer = new String(template);

			Pattern pattern = Pattern.compile("#(\\w+)");
			Matcher matcher = pattern.matcher(template);

			String exec_res_name;
			while (matcher.find()) {
				exec_res_name = matcher.group(1);
				if (executionResults.containsKey(exec_res_name)) {
					answer = answer.replaceFirst("#(\\w+)",
							Matcher.quoteReplacement(executionResults.get(exec_res_name)));
				}
			}
		}

		return answer;
	}

	// marker indicated by percentage sign (%), i.e. references to ITOs
	protected static String replaceSlotMarkers(String template, Frame frame) {
		String answer = "";
		if (template != null && template.length() > 0) {
			answer = new String(template);

			Pattern pattern = Pattern.compile("%(\\w+)");
			Matcher matcher = pattern.matcher(template);

			String ito_name;
			while (matcher.find()) {
				ito_name = matcher.group(1);
				if (frame.get(ito_name) != null) {
					answer = answer.replaceFirst("%" + ito_name, frame.get(ito_name).toString());
				}
			}
		}

		return answer;
	}

	// marker indicated by percentage sign (%), i.e. references to ITOs - Not
	// used
	protected static String replaceSlotMarkersFromITOMapping(String answer, Map<String, String> itoMapping) {
		if (answer != null && answer.length() > 0) {
			for (Map.Entry<String, String> entry : itoMapping.entrySet()) {
				answer = answer.replace("%" + entry.getKey(), entry.getValue());
			}
		}
		return answer;
	}
}