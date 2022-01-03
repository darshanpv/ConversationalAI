package cto.hmi.processor.ui;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.ProcessingException;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage.Meta;

public class WebSocketMessage {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	protected static void sendMessage(Session session, String msg) {
		try {
			logger.info("sending over websocket to :" + session.getRemoteAddress() + " response: " + msg);
			session.getRemote().sendString(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected static String processMessage(String instanceId, String userUtterance) {
		String resp = "";
		if (!RESTInterface.instances.containsKey(instanceId)) {
			logger.info("no such instance");
			resp = "{\"response\":\"Error: no such Instance\"}";
		} else {
			// process dialogue:
			UIConsumerMessage message = process(instanceId, userUtterance);
			// build bot response
			UIConsumer instance = RESTInterface.instances.get(instanceId);
			resp = instance.buildResponse(userUtterance, message.getSystemUtterance());
			if (!instance.isLicenseValid())
				resp = "{\"response\":\"Error: license either expired or invalid, contact support team.\"}";
		}
		return resp;
	}

	protected static UIConsumerMessage process(String instance_id, String userUtterance) {
		UIConsumer consumer = RESTInterface.instances.get(instance_id);
		UIConsumerMessage message;
		try {
			message = consumer.processUtterance(userUtterance);
		} catch (ProcessingException ex) {
			message = new UIConsumerMessage(ex.getMessage(), Meta.ERROR);
			logger.severe("processing WebSocket interface failed: " + ex.getMessage());
		}
		return message;
	}
}
