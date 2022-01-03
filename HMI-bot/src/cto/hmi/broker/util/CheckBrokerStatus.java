package cto.hmi.broker.util;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;

import cto.hmi.broker.constants.Params;
import cto.hmi.processor.ConvEngineProcessor;

public class CheckBrokerStatus {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	private static AdminClient client = null;

	public static boolean getStatus() {
		try {
			client = getAdminClient();
			ListTopicsResult topics = client.listTopics();
			Set<String> names = topics.names().get();
			if (names.isEmpty()) {
				logger.severe("[BROKER]:could not connect to broker, exiting");
				return false;
			}
			return true;
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe("[BROKER]: client failed to connect to broker, exiting");
			e.printStackTrace();
		}
		return false;
	}

	private static AdminClient getAdminClient() {
		if (client == null) {
			Properties prop = new Properties();
			prop.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, Params.getParam("KAFKA_BROKERS"));
			prop.put(AdminClientConfig.CLIENT_ID_CONFIG, Params.getParam("CLIENT_ID"));
			prop.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);
			prop.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
			client = AdminClient.create(prop);
			return client;
		}
		return client;
	}
}
