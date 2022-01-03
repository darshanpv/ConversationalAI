package cto.hmi.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;

public class checkServerStatus {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	int MAX_WAIT_TIME = 10; // in Second
	static boolean USE_BROKER = false;
	static String ZOOKEEPER_HOST = "localhost:2181";
	static String KAFKA_BROKERS = "localhost:9092";
	static String NLP_ENGINE_URL = "https://localhost:5050";

	public static boolean check() {

		Properties prop = new Properties();
		String NLP_propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE)
				.substring(8);
		String BROKER_propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.BROKERFILE)
				.substring(8);

		InputStream input;
		try {
			input = new FileInputStream(NLP_propertiesFile);
			prop.load(input);
			USE_BROKER = Boolean.parseBoolean(loadProerty(prop, "USE_BROKER").toLowerCase());
			NLP_ENGINE_URL = loadProerty(prop, "NLP_ENGINE_URL").toLowerCase();
			input = new FileInputStream(BROKER_propertiesFile);
			prop.load(input);
			ZOOKEEPER_HOST = loadProerty(prop, "ZOOKEEPER_HOST").toLowerCase();
			KAFKA_BROKERS = loadProerty(prop, "KAFKA_BROKERS").toLowerCase();
			input.close();
			if (!USE_BROKER)
				pingNLPServer();
			else
				pingBROKERServer();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	private static void pingNLPServer() {
		if (PingURLs.isReachable(NLP_ENGINE_URL, 5, "...checking if NLP_ENGINE is running")) {
			logger.info("NLP_ENGINE up and running.");
		} else {
			logger.severe("NLP_ENGINE not running. Exiting...");
			System.exit(1);
		}
	}

	private static void pingBROKERServer() {
		if (PingURLs.isReachable(ZOOKEEPER_HOST, 5, "...checking if ZOOKEEPER_ENGINE is running")) {
			logger.info("ZOOKEEPER_ENGINE up and running.");
		} else {
			logger.severe("ZOOKEEPER_ENGINE not reachable. exiting...");
			System.exit(1);
		}
		if (PingURLs.isReachable(KAFKA_BROKERS, 5, "...checking if BROKER_ENGINE is running")) {
			logger.info("BROKER_ENGINE up and running.");
		} else {
			logger.severe("BROKER_ENGINE not reachable. exiting...");
			System.exit(1);
		}
	}

	private static String loadProerty(Properties prop, String propertName) {
		String ret = "";
		if (prop.containsKey(propertName)) {
			ret = prop.getProperty(propertName);
		} else
			logger.severe("[SEVERE] missing " + propertName + " propoerty in bot.properties");
		return ret;
	}
}
