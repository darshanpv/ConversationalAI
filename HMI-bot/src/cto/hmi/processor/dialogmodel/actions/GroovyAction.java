package cto.hmi.processor.dialogmodel.actions;

import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;

import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.model.definition.actions.GroovyActionModel;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Frame;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

@XmlRootElement
public class GroovyAction extends GroovyActionModel {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public GroovyAction() {
		super();
	}

	public GroovyAction(String template) {
		super(template);
	}

	public static void main(String[] args) {
		new GroovyAction().execute(null);
	}

	
	public HashMap<String, String> execute(Frame frame) {

		String result = "FAIL_MSG";

		// Groovy
		try {
			if (code != null && code.length() > 0) {
				Binding binding = new Binding();
				binding.setVariable("executionResults", executionResults);
				binding.setVariable("frame", frame);
				GroovyShell shell = new GroovyShell(binding);
				shell.evaluate(code); // e.g."executionResults.put(\"temperature\",\"6\")"
			}

		} catch (Exception ex) {
			logger.severe("failed to execute the groovy action");
			ex.printStackTrace();
		}
		// let us populate the result with body
		if (executionResults.get("body") != null) {
			String body = executionResults.get("body").toString();
			// remove any spaces before running substring
			body = body.trim().replaceAll("[\\t\\n\\r]", "").replaceAll("(\\s+)?\"message\"(\\s+)?:(\\s+)?\\{",
					"\"message\":{");
			logger.info("result from Groovy call " + body);
			if (body.contains("message")) {
				try {
					JSONObject message = new JSONObject(body);
					result = message.getString("message");
					logger.info("processing only for message json object");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				logger.info("response has no message so sending null");
				result = "";
			}
		}
		result = result.replaceAll("\\s\\[.*?\\]", "");
		executionResults.put("result", result);

		return executionResults;
	}
}
