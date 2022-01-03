package cto.hmi.broker;

import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import cto.hmi.broker.constants.Params;
import cto.hmi.broker.util.CustomJsonDeserializer;

public class ConsumerCreator {
	static Consumer<String, DialogObject> consumer = null;

	public static Consumer<String, DialogObject> createConsumer(String topicName) {
		consumer = getConsumer();
		consumer.subscribe(Collections.singletonList(topicName));
		return consumer;
	}

	private static Consumer<String, DialogObject> getConsumer() {
		if (consumer == null) {
			final Properties props = new Properties();
			props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Params.getParam("KAFKA_BROKERS"));
			props.put(ConsumerConfig.GROUP_ID_CONFIG, Params.getParam("GROUP_ID"));
			props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CustomJsonDeserializer.class.getName());
			props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Params.getParam("MAX_POLL_RECORDS"));
			props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, Params.getParam("OFFSET_RESET"));
			consumer = new KafkaConsumer<String, DialogObject>(props);
			return consumer;
		}
		return consumer;
	}
}
