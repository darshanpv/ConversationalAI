package cto.hmi.processor.nlu.entityparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;

import com.jayway.jsonpath.JsonPath;

import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;

public class QAParser extends Parser {
	Logger logger = ConvEngineProcessor.getLogger();
	public QAParser() {
		super("sys.corpus.qa");
	}

	@Override
	public ParseResults parse(String utterance) {
		// Load the property file and see if the QA check needs to be performed
		Properties prop = new Properties();
		Boolean useQaCheck = false;
		Boolean hasAnswer = true;
		String response = "";
		String propertiesFile = "";
		String qaURL = "";

		propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE).substring(8);
		if (utterance.split("\\s+").length > 2 && (new File(propertiesFile)).exists()) {
			try {
				InputStream input = new FileInputStream(propertiesFile);
				prop.load(input);
				useQaCheck = (prop.getProperty("USE_QACHECK").toLowerCase().equals("true"));
				qaURL = prop.getProperty("QA_URL");
				if (useQaCheck) {
					logger.info("Checking if answer exists in FAQ corpus");
					HttpClient client;
					ContentResponse result;
					Request request;
					client = new HttpClient();
					client.start();
					request = client.newRequest(qaURL);
					request.method(HttpMethod.POST);
					request.param("userUtterance", utterance);
					result = request.send();
					response = result.getContentAsString();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (!response.equals("")) {
				hasAnswer = !JsonPath.read(response.toString(), "response").toString().toLowerCase()
						.equals("false");
			}
		}
		
		ParseResults results = new ParseResults(utterance);

		if (utterance.split("\\s+").length > 2 && hasAnswer)
			results.add(new ParseResult(this.name, 0, utterance.length() - 1,
					utterance, this.type, utterance));
		return results;
	}

}
