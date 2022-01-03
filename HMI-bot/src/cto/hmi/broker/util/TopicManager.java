package cto.hmi.broker.util;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;

import cto.hmi.broker.constants.Params;
import cto.hmi.processor.ConvEngineProcessor;

public class TopicManager {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	static AdminClient admin;
	// private static final int DELETE_TIMEOUT_SECONDS = 1;
	static String KAFKA_BROKERS = "";
	static int PARTITIONS;
	static int REPLICATION;
	static String TOPIC_BOT_TO_NLP = "";
	static String TOPIC_NLP_TO_BOT = "";
	static long RETENTION_MS_CONFIG = 3600000;

	public static void checkTopics() {
		try {

			KAFKA_BROKERS = Params.getParam("KAFKA_BROKERS").toString().toLowerCase();
			PARTITIONS = Integer.valueOf(Params.getParam("PARTITIONS").toString());
			REPLICATION = Integer.valueOf(Params.getParam("REPLICATION").toString());
			TOPIC_BOT_TO_NLP = Params.getParam("TOPIC_BOT_TO_NLP").toString().toLowerCase();
			TOPIC_NLP_TO_BOT = Params.getParam("TOPIC_NLP_TO_BOT").toString().toLowerCase();

			checkTopic(TOPIC_BOT_TO_NLP);
			checkTopic(TOPIC_NLP_TO_BOT);

		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private static void checkTopic(String topicName) {
		try {

			Properties topicConfig = new Properties();

			topicConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
			topicConfig.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 30000);
			topicConfig.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

			admin = AdminClient.create(topicConfig);
			if (hasTopic(topicName)) {
				if (isTopicOK(topicName, PARTITIONS, REPLICATION)) {
					logger.info("[BROKER]:found Topic: " + topicName + " Partition: " + PARTITIONS + " Replica: "
							+ REPLICATION);
				} else {
					logger.info("[BROKER]:could not found Topic: " + topicName + ", with correct configuration");

					if (modifyTopic(topicName, PARTITIONS, REPLICATION))
						logger.info("[BROKER]:modified new Topic: " + topicName + " Partitions: " + PARTITIONS
								+ " Replicas: " + REPLICATION);
					else
						logger.info("[BROKER]:failed to modify new Topic: " + topicName + " Partitions: " + PARTITIONS
								+ " Replicas: " + REPLICATION);

					logger.info("[BROKER]:available list of topics with BROKER_ENGINE:");
					admin.listTopics().names().get().forEach(System.out::println);

				}

			} else {
				if (newTopic(topicName, PARTITIONS, REPLICATION))
					System.out.println("[BROKER]:created new Topic: " + topicName + " Partitions: " + PARTITIONS
							+ " Replicas: " + REPLICATION);
				else
					System.out.println("[BROKER]:failed to create new Topic: " + topicName + " Partitions: "
							+ PARTITIONS + " Replicas: " + REPLICATION);

			}

		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("[BROKER]:failed to check the Topic: " + topicName);
		}
		admin.close(Duration.ofMillis(5000L));

	}

	private static boolean hasTopic(String topicName) {
		try {
			for (TopicListing topicListing : admin.listTopics().listings().get()) {
				if (topicListing.name().equals(topicName)) {
					return true;
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe("[BROKER]:failed to check the Topic: " + topicName);
			e.printStackTrace();
		}
		return false;
	}

	private static Boolean isTopicOK(String topicName, int partitions, int replicas) {
		Map<String, TopicDescription> details = null;
		try {
			DescribeTopicsResult result = admin.describeTopics(Collections.singleton(topicName));
			details = result.all().get();
			String name = details.get(topicName).name();
			int partCount = details.get(topicName).partitions().size();
			int replicaCount = details.get(topicName).partitions().get(0).replicas().size();
			if (name.equals(topicName) && partCount == partitions && replicaCount == replicas)
				return true;
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return false;
	}

	private static Boolean newTopic(String topicName, int partitions, int replicas) {
		try {
			// sleep for 2 seconds before creating the new topic
			TimeUnit.SECONDS.sleep(2);

			NewTopic topic = new NewTopic(topicName, partitions, (short) replicas);
			topic.configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(RETENTION_MS_CONFIG)));
			admin.createTopics(Collections.singleton(topic));

			logger.info("New Topic " + topicName + " details:");
			describeTopic(topicName);

			TimeUnit.SECONDS.sleep(2);
			return true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static Boolean modifyTopic(String topicName, int partitions, int replicas) {
		try {
			// sleep for 2 seconds before creating the new topic
			TimeUnit.SECONDS.sleep(2);

			NewPartitions newPartitions = NewPartitions.increaseTo(partitions);
			Map<String, NewPartitions> map = new HashMap<>();
			map.put(topicName, newPartitions);
			CreatePartitionsResult createPartitionsResult = admin.createPartitions(map);
			createPartitionsResult.all().get();
			// set retention time
			ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
			Collection<AlterConfigOp> configs = Collections.singleton(new AlterConfigOp(
					new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(RETENTION_MS_CONFIG)),
					AlterConfigOp.OpType.SET));
			Map<ConfigResource, Collection<AlterConfigOp>> configMaps = new HashMap<>();
			configMaps.put(configResource, configs);
			AlterConfigsResult result = admin.incrementalAlterConfigs(configMaps);
			result.all().get();

			logger.info("Modified Topic " + topicName + " details:");
			describeTopic(topicName);
			return true;
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static void describeTopic(String topicName) {

		try {
			DescribeTopicsResult result = admin.describeTopics(Collections.singleton(topicName));
			Map<String, TopicDescription> descriptionMap = result.all().get();
			descriptionMap.forEach((key, value) -> logger.info("name: " + key + ", desc: " + value));
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
