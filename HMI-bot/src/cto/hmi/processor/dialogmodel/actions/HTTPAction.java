package cto.hmi.processor.dialogmodel.actions;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.jayway.jsonpath.JsonPath;

import cto.hmi.model.definition.actions.HTTPActionModel;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Frame;


@XmlRootElement
public class HTTPAction extends HTTPActionModel {

	private HttpClient client;
	private final static Logger logger = ConvEngineProcessor.getLogger();

	// private static Message Msg = new Message();

	public HTTPAction() {
		super();
		init();
	}

	public HTTPAction(String template) {
		super(template);
		init();
	}

	private void init() {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		// SslContextFactory sslContextFactory = new SslContextFactory();
		client = new HttpClient(sslContextFactory);
		try {
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public HashMap<String, String> execute(Frame frame) {
		String replaced_url = replaceSlotMarkers(url, frame);
		String replaced_params = replaceSlotMarkers(params, frame);
		String[] params_arr = replaced_params.split("&");

		String result = "FAIL_MSG";
		try {
			ContentResponse response;
			Request request;
			// added to check if it's been recursively called and has stopped in
			// earlier call
			if (client.isStopped())
				client.start();
			request = client.newRequest(replaced_url);
			// choose method
			if (method.toLowerCase().equals("get")) {
				request.method(HttpMethod.GET);
			} else {
				request.method(HttpMethod.POST);
			}
			// process parameters
			String[] key_value;
			for (String paramPair : params_arr) {
				key_value = paramPair.split("=");
				if (key_value.length > 1)
					request.param(key_value[0], key_value[1]);
				else
					request.param(key_value[0], "");
			}
			logger.info("requesting: " + request.getURI() + ", " + request.getParams().toString());
			response = request.send();
			logger.info("HTTP status: " + response.getStatus());
			String body = response.getContentAsString();
			client.stop();

			logger.info("HTTP response: " + body.replace("\r\n", "").trim());
			if (xpath != null && xpath.replaceAll("\\s", "").length() > 0) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new InputSource(new StringReader(body)));
				XPathFactory xPathfactory = XPathFactory.newInstance();
				XPath xPath = xPathfactory.newXPath();
				XPathExpression expr = xPath.compile(xpath);
				result = (String) expr.evaluate(doc, XPathConstants.STRING);
			} else if (jpath != null && jpath.replaceAll("\\s", "").length() > 0) {
				String jsonExp = jpath;
				ArrayList<String> items = JsonPath.read(body, jsonExp);
				String data = "";
				if (items.size() >= 1)
					data = items.toString();
				if (!data.equals("")) {
					data = data.trim().replaceAll("[\\t\\n\\r]", "");
					if (data.substring(1, data.length() - 1).startsWith("{")) {
						logger.info("jpath has JSONObject will be further processed for message");
						// result = data.substring(1, data.length()-1);
						JSONObject resultObj = new JSONObject(body);
						String jpathKey = jpath.substring(3, jpath.length());
						result = resultObj.getJSONObject(jpathKey).toString();
					} else
						result = data.substring(2, data.length() - 2);
				} else {
					logger.info("response does not contain the jsonobject you are looking");
				}
			} else {
				result = "could not get xPath and/or jPath";
			}
			// Postprocessing
			result = result.replaceAll("\\s\\(.*?\\)", ""); // remove content in
			result = result.replaceAll("\\s\\[.*?\\]", "");

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		executionResults.put("result", result);
		// executionResults.put("jsonResult", jsonResult);
		return executionResults;
	}
}
