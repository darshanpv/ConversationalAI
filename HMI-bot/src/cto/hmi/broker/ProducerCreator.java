package cto.hmi.broker;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import cto.hmi.broker.constants.Params;
import cto.hmi.broker.util.CustomJsonSerializer;
import cto.hmi.broker.util.CustomPartitioner;

public class ProducerCreator {
	static KafkaProducer<String, DialogObject> producer = null;

	public static Producer<String, DialogObject> createProducer() {
		producer = getProducer();
		return producer;
	}

	private static KafkaProducer<String, DialogObject> getProducer() {
		if (producer == null) {
			Properties props = new Properties();
			props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Params.getParam("KAFKA_BROKERS"));
			props.put(ProducerConfig.CLIENT_ID_CONFIG, Params.getParam("CLIENT_ID"));
			props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CustomJsonSerializer.class.getName());
			props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());
			props.put("linger.ms", 1000);
			props.put("retries", 1);
			producer = new KafkaProducer<String, DialogObject>(props);
			return producer;
		}
		return producer;
	}
}
