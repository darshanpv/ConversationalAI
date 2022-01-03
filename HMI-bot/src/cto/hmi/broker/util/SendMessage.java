package cto.hmi.broker.util;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import cto.hmi.broker.DialogObject;
import cto.hmi.broker.ProducerCreator;
import cto.hmi.processor.ConvEngineProcessor;

public class SendMessage {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public static boolean send(String topicName, String key, DialogObject obj) {
		Producer<String, DialogObject> producer = ProducerCreator.createProducer();
		try {
			final ProducerRecord<String, DialogObject> record = new ProducerRecord<String, DialogObject>(topicName, key,
					obj);
			RecordMetadata metadata = producer.send(record).get();
			logger.info("[BROKER]: message sent with key " + key + " to partition " + metadata.partition()
					+ " with offset " + metadata.offset());
			logger.info("[BROKER]: message detail: " + record.value().toString());
			producer.flush();
		} catch (ExecutionException e) {
			logger.info("[BROKER]: error in sending record");
			System.out.println(e);
			return false;
		} catch (InterruptedException e) {
			logger.info("[BROKER]: error in sending record");
			System.out.println(e);
			return false;
		} finally {
			//since the producer has been made static with single instance
			// producer.close(Duration.ofMillis(5000L));
		}
		return true;
	}
}
