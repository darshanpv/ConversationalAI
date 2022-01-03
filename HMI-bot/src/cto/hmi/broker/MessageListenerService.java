package cto.hmi.broker;

import java.time.Duration;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.manager.DialogManager;

public class MessageListenerService {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	static Consumer<String, DialogObject> consumer = null;

	public static void start(String topicName) {
		// get only one instance of listener
		if (consumer == null) {
			consumer = ConsumerCreator.createConsumer(topicName);
			// start service thread and exit
			new Thread(new Runnable() {
				@Override
				public void run() {
					ListenerService(topicName, consumer);
				}
			}).start();
		}

	}

	public static void ListenerService(String topicName, Consumer<String, DialogObject> consumer) {
		// run the listener
		boolean flag = true;
		while (true) {
			final ConsumerRecords<String, DialogObject> consumerRecords = consumer.poll(Duration.ofMillis(100));
			//flush all the old records by polling
			if (flag) {
				Set<TopicPartition> assignments = consumer.assignment();
				assignments.forEach(topicPartition -> consumer.seekToEnd(assignments));
				flag = false;
			}
			for (ConsumerRecord<String, DialogObject> record : consumerRecords) {
				logger.info("[BROKER] message recieved with key:" + record.key() + ", partition:" + record.partition()
						+ ", offset:" + record.offset() + ", value:" + record.value().toString());
				DialogManager.brokerMessages.put(record.key(), record);
				consumer.commitSync();
			}
		}
	}

}
