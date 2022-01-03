package cto.hmi.nlp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import cto.hmi.broker.DialogObject;
import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;

public class TrainModel {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public static DialogObject train(String domain, String locale) {
		ConvEngineConfig config = ConvEngineConfig.getInstance();
		Properties prop = new Properties();
		String propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE).substring(8);
		String NLPEngineURL;
		HttpClient httpClient = null;
		DialogObject dialogObject = null;

		try {
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			
			NLPEngineURL = prop.getProperty("NLP_ENGINE_URL") + "/train";
			if (NLPEngineURL.contains("https")) {
				SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);
				// set keystore path
				sslContextFactory.setKeyStorePath(config.getProperty(ConvEngineConfig.NLPKEYSTOREPATH));
				if (prop.getProperty("JETTY_KS_PASSWORD") != null) {
					sslContextFactory.setKeyStorePassword(prop.getProperty("JETTY_KS_PASSWORD"));
					sslContextFactory.setKeyManagerPassword(prop.getProperty("JETTY_KS_PASSWORD"));
				} else {
					sslContextFactory.setKeyStorePassword(config.getProperty(ConvEngineConfig.JETTYKEYSTOREPASS));
					sslContextFactory.setKeyManagerPassword(config.getProperty(ConvEngineConfig.JETTYKEYSTOREPASS));
				}
				sslContextFactory.setKeyStoreType("jks");

				httpClient = new HttpClient(sslContextFactory);
			} else
				httpClient = new HttpClient();

			httpClient.setFollowRedirects(false);
			httpClient.start();

			Request request = httpClient.POST(NLPEngineURL);
			request.param("domain", domain);
			request.param("locale", locale);
			ContentResponse response = request.send();

			int status = response.getStatus();
			if (status == 200 || status == 201) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				dialogObject = mapper.readValue(response.getContentAsString(), DialogObject.class);
			}
			httpClient.stop();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.severe("failed to preocess the domain url." + e);
			return null;
		}
		return dialogObject;
	}
}
