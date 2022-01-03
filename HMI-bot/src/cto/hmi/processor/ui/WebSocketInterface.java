package cto.hmi.processor.ui;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.RuntimeError;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage;

@WebSocket(maxIdleTime = RESTInterface.LOGOUT_TIME)
public class WebSocketInterface {
	private Session wsSession;
	private final static Logger logger = ConvEngineProcessor.getLogger();

	@OnWebSocketMessage
	public void onText(Session socketSession, String msg) {
		wsSession = socketSession;
		logger.info("websocket message received:" + msg);
		String path = "";
		if (socketSession.isOpen()) {
			path = socketSession.getUpgradeRequest().getRequestURI().getPath();
			try {
				JSONObject res = new JSONObject(msg);
				String user = "";
				String role = "admin";
				String token = "";
				String sessionId = "";
				String userUtterance = "";

				if (path.contains("msg")) {

					// check if user is unsubscribing to chat
					if (res.has("unsubscribe")) {
						if (res.get("unsubscribe").equals("chat")) {
							WebSocketMessage.sendMessage(socketSession,
									"{\"response\":\"Info: session disconnected.\"}");
							socketSession.disconnect();
						}
					}
					// check if mandatory user field is missing
					else if (res.optString("user", "").isEmpty()) {
						WebSocketMessage.sendMessage(socketSession,
								"{\"response\":\"Error: user parameter missing, cannot create bot session\"}");
					} else {
						role = res.optString("role", role);
						token = res.optString("authToken", token);
						sessionId = res.optString("sessionId", sessionId);
						user = res.optString("user", user);
						userUtterance = res.optString("userUtterance", userUtterance);
						// create session ID if it empty
						if (sessionId.isEmpty()) {
							sessionId = generateDialogID(sessionId);
						}
						// adding the session to wsUserSessions
						RESTInterface.wsUserSessions.put(wsSession, sessionId);
						
						if (validSessionId(sessionId)) {

							if (createDialog(sessionId, user, role, token)) {
								// this is a new dialog session
								if (userUtterance.trim().isEmpty()) {
									WebSocketMessage.sendMessage(socketSession, initDialog(sessionId));
								} else {
									// Initialize the dialog with empty string
									WebSocketMessage.processMessage(sessionId, "");
									WebSocketMessage.sendMessage(socketSession,
											WebSocketMessage.processMessage(sessionId, userUtterance));
								}

							} else {
								// this is a existing dialog session
								WebSocketMessage.sendMessage(socketSession,
										WebSocketMessage.processMessage(sessionId, userUtterance));
							}
						} else {
							// invalid session Id
							WebSocketMessage.sendMessage(socketSession,
									"{\"response\":\"Error: failed to create instance due to invalid sessionId\"}");
						}
					}
				}

			} catch (JSONException | IOException e) {
				logger.info("check your json payload:" + msg);
				WebSocketMessage.sendMessage(socketSession, "{\"response\":\"Error: check your json payload.\"}");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session) throws IOException {
		logger.info("websocket connection established: " + session.getRemoteAddress().getHostString());

	}

	@OnWebSocketClose
	public void onClose(Session session, int status, String reason) {
		// remove the instance
		String instanceId = "";
		if (RESTInterface.wsUserSessions.containsKey(session)) {
			instanceId = RESTInterface.wsUserSessions.get(session);
			logger.info("removing the bot instance with session: " + instanceId);
			if (RESTInterface.instances.containsKey(instanceId))
				RESTInterface.instances.remove(instanceId);
			RESTInterface.wsUserSessions.remove(session);
		}
		logger.info("websocket connection closed: " + session.getRemoteAddress().getHostString());
	}

	@OnWebSocketError
	public void onError(Throwable error) {
		System.out.println(" error!" + error);
	}

	private String generateDialogID(String sessionId) {
		if (sessionId.isEmpty()) {
			String id = generateNextID() + "-" + RandomStringUtils.randomAlphanumeric(12).toUpperCase();
			return id;
		} else
			return sessionId;

	}

	private String generateNextID() {
		RESTInterface.instanceCounter++;
		if (RESTInterface.instanceCounter % 10 == 0)
			RESTInterface.cleanUp(); // run clean up every 10 instances
		if (RESTInterface.instanceCounter > RESTInterface.MAX_DIALOG_COUNTER) // reset to avoid large numbers
			RESTInterface.instanceCounter = 0;
		String identifier = "d" + RESTInterface.instanceCounter;
		return identifier;
	}

	private boolean createDialog(String instance, String user, String role, String token) {
		if (!RESTInterface.instances.containsKey(instance)) {
			logger.info("creating a new dialog instance for sessionId: " + instance);
			UIConsumer newConsumer = null;

			try {
				newConsumer = UserInterface.consumerFactory.create();

				newConsumer.setAdditionalDebugInfo("clientIP", wsSession.getRemoteAddress().getHostString());
				newConsumer.setAdditionalDebugInfo("userAgent", "webSocket");
				newConsumer.setAdditionalDebugInfo("loginUser", user);
				newConsumer.setAdditionalDebugInfo("loginRole", role);
				newConsumer.setAdditionalDebugInfo("authToken", token);
				newConsumer.setAdditionalDebugInfo("sessionID", instance);

				// adding loginUser and sessionId to context
				newConsumer.addtoEntityMapping("loginUser_", user);
				newConsumer.addtoEntityMapping("loginRole_", role);
				newConsumer.addtoEntityMapping("authToken_", token);

				logger.info("added user " + user + " to context");
				logger.info("added role " + role + " to context");
				logger.info("added authToken " + token + " to context");
				newConsumer.addtoEntityMapping("sessionId_", instance);
				logger.info("added sessionId " + instance + " to context");

				RESTInterface.instances.put(instance, newConsumer);
				ConvEngineProcessor.getLogger().fine("created new instance " + newConsumer.getClass().getName());
			} catch (RuntimeError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			return true;
		}

		return false;
	}

	private String initDialog(String instanceId) {
		UIConsumerMessage message = WebSocketMessage.process(instanceId, ""); // empty utterance
		String systemUtterance = message.getSystemUtterance();
		// build bot response
		UIConsumer instance = RESTInterface.instances.get(instanceId);
		String jsonBotResponse = instance.buildResponse("", systemUtterance);
		return jsonBotResponse;
	}

	private boolean validSessionId(String sessionId) {
		if (Pattern.compile("^[sxd]?\\d+-[A-Z0-9]{12}").matcher(sessionId).find())
			return true;
		else
			return false;
	}
}
