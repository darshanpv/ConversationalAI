package cto.hmi.broker.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import cto.hmi.broker.DialogObject;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.manager.DialogManager;

public class LookForMessage {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	private volatile static boolean found = false;

	public static boolean look(int waitTimeInSeconds, String messageKey, String messageId) {

		List<String> mIDs = Arrays.asList(messageId.split("\\|"));
		String mKey = "";
		found = false;

		// delete the broker message with the key if it exists
		if (DialogManager.brokerMessages.containsKey(messageKey))
			DialogManager.brokerMessages.remove(messageKey);
		for (int x = 0; x < (waitTimeInSeconds * 10); x++) {
			if (!found) {
				try {
					for (Map.Entry<String, ConsumerRecord<String, DialogObject>> rec : DialogManager.brokerMessages
							.entrySet()) {
						mKey = rec.getKey();
						// read messageId from Dialog object
						String mId = rec.getValue().value().getMessageId();

						if (mIDs.stream().anyMatch(str -> str.trim().equals(mId)) && mKey.equals(messageKey)) {
							found = true;
							logger.info(
									"[BROKER]: located the message with key: " + messageKey + " and messageId: " + mId);
						}
					}
					TimeUnit.MILLISECONDS.sleep(100);

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.severe("[BROKER]: error: could not look up for message...");
					e.printStackTrace();
				}
			} else
				break; // stop as message is found
		}
		if (found)
			return true;
		else
			return false;
	}
}
