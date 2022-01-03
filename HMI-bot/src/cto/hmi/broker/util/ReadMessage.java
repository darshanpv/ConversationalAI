package cto.hmi.broker.util;

import java.time.Duration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import cto.hmi.broker.ConsumerCreator;
import cto.hmi.broker.DialogObject;
import cto.hmi.nlp.ProcessMessage;

public class ReadMessage {

	public static void read(String topicName) {
		Consumer<String, DialogObject> consumer = ConsumerCreator.createConsumer(topicName);
		while (true) {
			final ConsumerRecords<String, DialogObject> consumerRecords = consumer.poll(Duration.ofMillis(100));

			for (ConsumerRecord<String, DialogObject> record : consumerRecords) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						// code goes here.
						ProcessMessage.process(record.key(), record.value());
					}
				}).start();
			}
		}
	}

}
