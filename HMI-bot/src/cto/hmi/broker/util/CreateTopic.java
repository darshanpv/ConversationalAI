package cto.hmi.broker.util;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;

import cto.hmi.broker.constants.Params;
import cto.hmi.processor.ConvEngineProcessor;

public class CreateTopic {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	static AdminClient admin;
	private static final int DELETE_TIMEOUT_SECONDS = 1;

	public static void create(String topicName, int partitions, int replications) {
		Properties topicConfig = new Properties();
		topicConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, Params.getParam("KAFKA_BROKERS"));
		topicConfig.put(AdminClientConfig.CLIENT_ID_CONFIG, Params.getParam("CLIENT_ID"));
		topicConfig.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);
		topicConfig.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

		admin = AdminClient.create(topicConfig);

		if (hasTopic(topicName)) {
			System.out.println("[BROKER]: deleting the topic: " + topicName);
			try {
				admin.deleteTopics(Collections.singleton(topicName)).all().get(DELETE_TIMEOUT_SECONDS,
						TimeUnit.SECONDS);
				TimeUnit.SECONDS.sleep(2);
				logger.info("[BROKER]: available list of topics after deletion:\n" + topicName);
				admin.listTopics().names().get().forEach(System.out::println);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			// sleep for 5 seconds before creating the new topic
			TimeUnit.SECONDS.sleep(2);
			NewTopic newTopic = new NewTopic(topicName, partitions, (short) replications);
			admin.createTopics(Collections.singleton(newTopic));
			logger.info("[BROKER]: created topic: " + topicName);
			TimeUnit.SECONDS.sleep(2);
			logger.info("[BROKER]: current list of topics:\n" + topicName);
			admin.listTopics().names().get().forEach(System.out::println);
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			admin.close(Duration.ofMillis(5000L));
		}
	}

	public static boolean hasTopic(String topicName) {
		try {
			for (TopicListing topicListing : admin.listTopics().listings().get()) {
				if (topicListing.name().equals(topicName)) {
					return true;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
