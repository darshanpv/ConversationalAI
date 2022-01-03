package cto.hmi.broker.constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;

public class Params {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	private static String KAFKA_BROKERS = "localhost:9092";
	// zookeeper configuration details
	private static String ZOOKEEPER_HOST = "localhost:2181";

	// partition details
	private static int PARTITIONS = 3;
	private static int REPLICATION = 1;
	private static String TOPIC_BOT_TO_NLP = "bot_to_nlp";
	private static String TOPIC_NLP_TO_BOT = "nlp_to_bot";

	// if multiple kafa servers
	private static String CLIENT_ID = "dialog_1";

	private static String GROUP_ID = "dialogEngine_1";
	private static String OFFSET_RESET = "latest";

	private static Integer MAX_POLL_RECORDS = 1;
	private static boolean paramsLoaded = false;

	public static void loadParams() {
		try {
			paramsLoaded = true;
			String propertiesFile = "";
			Properties prop = new Properties();
			propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.BROKERFILE).substring(8);
			InputStream input;
			input = new FileInputStream(propertiesFile);
			prop.load(input);

			ZOOKEEPER_HOST = loadProerty(prop, "ZOOKEEPER_HOST");
			KAFKA_BROKERS = loadProerty(prop, "KAFKA_BROKERS");
			MAX_POLL_RECORDS = Integer.valueOf(loadProerty(prop, "MAX_POLL_RECORDS"));
			OFFSET_RESET = loadProerty(prop, "OFFSET_RESET");
			GROUP_ID = loadProerty(prop, "GROUP_ID");
			CLIENT_ID = loadProerty(prop, "CLIENT_ID");

			PARTITIONS = Integer.valueOf(loadProerty(prop, "PARTITIONS"));
			REPLICATION = Integer.valueOf(loadProerty(prop, "REPLICATION"));

			TOPIC_BOT_TO_NLP = loadProerty(prop, "TOPIC_BOT_TO_NLP");
			TOPIC_NLP_TO_BOT = loadProerty(prop, "TOPIC_NLP_TO_BOT");

			input.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String loadProerty(Properties prop, String propertName) {
		String ret = "";
		if (prop.containsKey(propertName)) {
			ret = prop.getProperty(propertName);
		} else
			logger.severe("Error: missing " + propertName + " propoerty in broker.properties");
		return ret;
	}

	public static Object getParam(String param) {
		// check if params are loaded
		if (!paramsLoaded)
			loadParams();
		
		Object ret = "";
		switch (param) {
		case "ZOOKEEPER_HOST":
			ret = ZOOKEEPER_HOST;
			break;
		case "KAFKA_BROKERS":
			ret = KAFKA_BROKERS;
			break;
		case "MAX_POLL_RECORDS":
			ret = MAX_POLL_RECORDS;
			break;
		case "OFFSET_RESET":
			ret = OFFSET_RESET;
			break;
		case "GROUP_ID":
			ret = GROUP_ID;
			break;
		case "CLIENT_ID":
			ret = CLIENT_ID;
			break;
		case "PARTITIONS":
			ret = PARTITIONS;
			break;
		case "REPLICATION":
			ret = REPLICATION;
			break;
		case "TOPIC_BOT_TO_NLP":
			ret = TOPIC_BOT_TO_NLP;
			break;
		case "TOPIC_NLP_TO_BOT":
			ret = TOPIC_NLP_TO_BOT;
			break;
		}
		return ret;
	}

}
