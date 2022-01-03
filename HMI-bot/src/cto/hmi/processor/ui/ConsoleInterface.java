package cto.hmi.processor.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.ProcessingException;
import cto.hmi.processor.exceptions.RuntimeError;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage.Meta;

public class ConsoleInterface extends UserInterface {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	private static String loginUser = "test";
	private static String loginRole = "admin";
	private static String authToken = "NzcyNjNhODctZGY3Ny00N2VlLThhYjAtMzg3NjkyOTVkMjIwNzBmZmQyNTAtZGVl_PF84_consumer";
	private static String sessionId = "d1-DUMMYSESSION";
	static UIConsumer consumer;

	private void send(String text) {
		System.out.println(text);
	}

	private String receive() {
		String user_answer = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

			user_answer = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return user_answer;
	}

	@Override
	public void start() {
		try {

			consumer = consumerFactory.create();
			consumer.setAdditionalDebugInfo("loginUser", loginUser);
			consumer.setAdditionalDebugInfo("loginRole", loginRole);
			consumer.setAdditionalDebugInfo("authToken", authToken);
			consumer.setAdditionalDebugInfo("sessionID", sessionId);
			
			consumer.addtoEntityMapping("loginUser_", loginUser);
			consumer.addtoEntityMapping("loginRole_", loginRole);
			consumer.addtoEntityMapping("authToken_", authToken);
			
			logger.info("Added user " + loginUser + " to context");
			logger.info("Added role " + loginRole + " to context");
			logger.info("Added authToken " + authToken + " to context");
			consumer.addtoEntityMapping("sessionId_", sessionId);
			logger.info("Added sessionId " + sessionId + " to context");
			UIConsumerMessage message = consumer.processUtterance("");
			String systemUtterance = message.getSystemUtterance();
			send(systemUtterance);
			String userUtterance;

			while (!(userUtterance = receive()).equals("\r\n")) {
				message = consumer.processUtterance(userUtterance);
				systemUtterance = message.getSystemUtterance();
				if (message.getMeta() == Meta.END_OF_DIALOG)
					break;
				send(systemUtterance);
			}
		} catch (ProcessingException ex) {
			ex.printStackTrace();
		} catch (RuntimeError e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {

	}

	public static UIConsumer getInstance() {
		return consumer;

	}
}
