package cto.hmi.processor.ui;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import cto.hmi.bot.util.LogForDialog;
import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.exceptions.NoParserFileFoundException;
import cto.hmi.processor.exceptions.ProcessingException;
import cto.hmi.processor.exceptions.RuntimeError;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage.Meta;

@Path("/")
public class RESTInterface extends UserInterface {

	private static Server server;
	private static String loginUser = "test";
	private static String loginRole = "admin";
	private static String authToken = "";
	private static String sessionId = "";
	protected static int instanceCounter = 0;
	protected static HashMap<String, UIConsumer> instances = new HashMap<String, UIConsumer>();
	private final static Logger logger = ConvEngineProcessor.getLogger();

	private static boolean isCORS = false;

	private static String ACCESS_CONTROL_ALLOW_ORIGIN = "*";
	private static String ACCESS_CONTROL_ALLOW_CREDENTIALS = "true";
	private static String ACCESS_CONTROL_ALLOW_METHODS = "GET,PUT,POST,DELETE,OPTIONS";
	private static String ACCESS_CONTROL_ALLOW_HEADERS = "X-Requested-With,Authorization,Content-Type,Accept,Origin";
	private static String ACCESS_CONTROL_EXPOSE_HEADERS = "Location";

	private final String API_RESPONSE_OK = "response_ok";
	private final String API_RESPONSE_CREATED = "response_creatred";
	private final String API_RESPONSE_ERROR = "response_serverError";
	private final String API_RESPONSE_EMPTY = "response_noContent";

	// added to support WebSocket
	// protected static Set<Session> wsSessions = new HashSet<Session>();
	protected static Map<Session, String> wsUserSessions = new HashMap<Session, String>();
	static final int LOGOUT_TIME = 20 * 60 * 1000; // 20 min
	static final int MAX_DIALOG_COUNTER = 1000; // max dialog counter after which it will set back to 1

	@Context
	UriInfo uri;
	@Context
	HttpServletRequest request;
	@Context
	HttpHeaders headers;

	public RESTInterface() {
		// if loaded externally (Application Server / war) instead of using
		// main-method in ConvEngineProcessor
		if (!ConvEngineProcessor.isInit()) {
			ConvEngineConfig config = ConvEngineConfig.getInstance();
			String path = "";
			try {
				path = getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
						.resolve("../../../../../../..").getPath().toString(); // Tomcat
				path = path.substring(0, path.length() - 1); // remove last
																// slash
				logger.info("Root Path: " + path);
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.severe("RESTInterface constructor failed: " + ex.getMessage());
			}
			config.setBaseDir("file://" + path);
			ConvEngineProcessor.loadByWar(this);
		}
	}

	// Web Service Methods
	// ===================

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("bot")
	public Response createDefaultBot(@FormParam("user") String user, @FormParam("role") String role,
			@FormParam("authToken") String token, @FormParam("sessionId") String sessId,
			@FormParam("userUtterance") String userUtterance)
			throws URISyntaxException, InstantiationException, IllegalAccessException {
		sessionId = "";
		if (role != null)
			loginRole = role;
		if (token != null)
			authToken = token;
		if (sessId != null)
			sessionId = sessId;
		if (!sessionId.isEmpty())
			if (!validSessionId(sessionId))
				return apiResponse(API_RESPONSE_ERROR,
						"{\"response\":\"Error: failed to create instance due to invalid sessionId\"}");

		if (user.trim().isEmpty() || user == null) {
			return apiResponse(API_RESPONSE_ERROR,
					"{\"response\":\"Error: need valid user for creating bot session\"}");
		} else {
			loginUser = user;
			sessionId = generateDialogID(sessionId);
			if (createDialog(sessionId)) {
				// this is a new dialog session
				// check if there is a user Utterance to be processed
				if (userUtterance == null)
					userUtterance = "";

				if (userUtterance.trim().isEmpty()) {
					return initDialog(sessionId);
				} else {
					// Initialize the dialog with empty string
					process(sessionId, "");
					UIConsumerMessage message = process(sessionId, userUtterance);
					// build bot response
					UIConsumer instance = instances.get(sessionId);
					String jsonBotResponse = instance.buildResponse(userUtterance, message.getSystemUtterance());

					if (message.getMeta() == Meta.UNCHANGED)
						return apiResponse(API_RESPONSE_EMPTY, "");

					else if (!instance.isLicenseValid())
						return apiResponse(API_RESPONSE_OK,
								"{\"response\":\"Error: license either expired or invalid, contact support team.\"}");
					else
						return apiResponse(API_RESPONSE_OK, jsonBotResponse);
				}

			} else {
				// this is existing dialog session
				// process userUtterance
				UIConsumerMessage message = process(sessionId, userUtterance);
				// build bot response
				UIConsumer instance = instances.get(sessionId);
				String jsonBotResponse = instance.buildResponse(userUtterance, message.getSystemUtterance());

				if (message.getMeta() == Meta.UNCHANGED)
					return apiResponse(API_RESPONSE_EMPTY, "");

				else if (!instance.isLicenseValid())
					return apiResponse(API_RESPONSE_OK,
							"{\"response\":\"Error: license either expired or invalid, contact support team.\"}");
				else
					return apiResponse(API_RESPONSE_OK, jsonBotResponse);
			}
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("msgbot")
	public Response createDefaultMessageBot(@QueryParam("user") String user, @QueryParam("role") String role,
			@QueryParam("authToken") String token)
			throws URISyntaxException, InstantiationException, IllegalAccessException {
		if (token != null)
			authToken = token;
		if (role != null)
			loginRole = role;
		if (user != null) {
			loginUser = user;
			if (createMsgDialog(loginUser)) {
				// dialog instance created
				return initMsgDialog(loginUser);
			} else {
				// dialog instance is already existing
				return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error:instance already exists\"}");
			}

		} else {
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error:need user for bot session creation\"}");
		}
	}

	@POST
	@Path("bot/load")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createDialogFromXML(@FormParam("dialogxml") String dialogxml)
			throws URISyntaxException, InstantiationException, IllegalAccessException {
		sessionId = generateDialogID("");
		createDialog(sessionId, dialogxml);
		return initDialog(sessionId);
	}

	@GET
	@Path("msgbot/{sessionId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response exchange_message(@PathParam("sessionId") String sessionId,
			@QueryParam("userMessage") String userMessagee) {
		if (!instances.containsKey(sessionId)) { // check if instance exists
			logger.info("no such instance");
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such Instance\"}");
		} else {
			// process dialogue:
			UIConsumerMessage message = process(sessionId, userMessagee);
			// build bot response
			UIConsumer instance = instances.get(sessionId);
			String jsonBotResponse = instance.buildResponse(userMessagee, message.getSystemUtterance());
			if (message.getMeta() == Meta.UNCHANGED)
				return apiResponse(API_RESPONSE_EMPTY, "");
			else if (!instance.isLicenseValid())
				return apiResponse(API_RESPONSE_OK,
						"{\"response\":\"Error: license either expired or invalid, contact support team.\"}");
			else
				return apiResponse(API_RESPONSE_OK, jsonBotResponse);
		}
	}

	@POST
	@Path("bot/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInstancesInfo() {

		StringWriter out = null;
		try {
			out = new StringWriter();
			JSONObject response = new JSONObject();
			JSONObject result = new JSONObject();

			JSONArray sessionIds = new JSONArray();
			if (server != null)
				result.put("URI", server.getURI().toString());
			else
				result.put("URI", "Not Available");
			result.put("startedOn", ConvEngineProcessor.getStartedOn().toString());
			result.put("domain", ConvEngineProcessor.getDialog().getName());
			result.put("company", ConvEngineProcessor.getDialog().getCompany());
			result.put("uiType", ConvEngineProcessor.getUIType());
			result.put("currentSessions", Integer.toString(instances.size()));
			for (String sid : instances.keySet()) {
				JSONObject session = new JSONObject();
				session.put("id", sid);
				UIConsumer instance = instances.get(sid);
				JSONObject jsonObject = new JSONObject(instance.getDialogFailureInfo());
				session.put("failures", jsonObject.get("failures"));
				session.put("likes", jsonObject.get("likes"));
				session.put("dislikes", jsonObject.get("dislikes"));
				session.put("user", instance.getDebugInfo("loginUser"));
				session.put("role", instance.getDebugInfo("loginRole"));
				session.put("authToken", instance.getDebugInfo("authToken"));
				session.put("lastAcess", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(instance.getLastAccess()));
				sessionIds.put(session);
			}
			result.put("sessionIDs", sessionIds);
			response.put("status", result);
			response.write(out);
		} catch (JSONException e) {
			// TODO: handle exception
			e.printStackTrace();
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: failed to create the response\")}");
		}
		return apiResponse(API_RESPONSE_OK, out.toString());
	}

	// used internally
	@GET
	@Path("bot/{sessionId}/context")
	@Produces(MediaType.APPLICATION_XML)
	public Response getContextInfo(@PathParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		String debugInfo = instance.getDebugInfo();
		if (debugInfo == null || debugInfo.length() == 0) {
			debugInfo = "no debug info";
		} else {
			debugInfo = addXSDReference(debugInfo, "/hmi/context.xsl");
		}
		return apiResponse(API_RESPONSE_OK, debugInfo);
	}

	// used internally
	@GET
	@Path("bot/{sessionId}/xml")
	@Produces(MediaType.APPLICATION_XML)
	public Response getDialogXML(@PathParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		String xml = instance.getDialogXml();
		xml = addXSDReference(xml, "/hmi/dialog.xsl");
		return apiResponse(API_RESPONSE_OK, xml);
	}

	@POST
	@Path("bot/getDDF")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDialogJSON(@FormParam("sessionId") String sessionId) {
		StringWriter out = new StringWriter();
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		try {

			JSONObject body = XML.toJSONObject(instance.getDialogXml());
			JSONObject dialog = body.getJSONObject("n:dialog");
			JSONObject result = new JSONObject();

			JSONArray taskInput = new JSONArray();
			taskInput = dialog.getJSONObject("tasks").getJSONArray("task");

			if (dialog.has("company"))
				result.put("company", dialog.get("company"));
			if (dialog.has("name"))
				result.put("name", dialog.get("name"));
			if (dialog.has("version"))
				result.put("version", dialog.get("version"));
			if (dialog.has("useSODA"))
				result.put("useSODA", dialog.get("useSODA"));
			if (dialog.has("allowSwitchTasks"))
				result.put("allowSwitchTasks", dialog.get("allowSwitchTasks"));
			if (dialog.has("allowOverAnswering"))
				result.put("allowOverAnswering", dialog.get("allowOverAnswering"));
			if (dialog.has("allowDifferentQuestion"))
				result.put("allowDifferentQuestion", dialog.get("allowDifferentQuestion"));
			if (dialog.has("allowCorrection"))
				result.put("allowCorrection", dialog.get("allowCorrection"));
			if (dialog.has("failureAttempts"))
				result.put("failureAttempts", dialog.get("failureAttempts"));
			JSONArray taskInfoArray = new JSONArray();
			for (int i = 0; i < taskInput.length(); i++) {
				JSONObject taskInfo = new JSONObject();
				JSONArray entityInfoArray = new JSONArray();
				JSONArray entities = new JSONArray();
				taskInfo.put("name", taskInput.getJSONObject(i).get("name"));
				if (taskInput.getJSONObject(i).has("label"))
					taskInfo.put("label", taskInput.getJSONObject(i).get("label"));
				if (taskInput.getJSONObject(i).has("role"))
					taskInfo.put("role", taskInput.getJSONObject(i).get("role"));
				// check if entities is array or object
				Object entity;
				if (taskInput.getJSONObject(i).getJSONObject("entities").has("entity")) {
					entity = taskInput.getJSONObject(i).getJSONObject("entities").get("entity");
					if (entity instanceof JSONArray && !entity.toString().equals("{}")) {
						entities = taskInput.getJSONObject(i).getJSONObject("entities").getJSONArray("entity");
						for (int j = 0; j < entities.length(); j++) {
							JSONObject entityInfo = new JSONObject();
							if (entities.getJSONObject(j).has("name")
									&& !entities.getJSONObject(j).get("name").toString().endsWith("_")) {
								entityInfo.put("name", entities.getJSONObject(j).get("name"));
								if (entities.getJSONObject(j).has("label"))
									entityInfo.put("label", entities.getJSONObject(j).get("label"));
								if (entities.getJSONObject(j).has("answerType"))
									entityInfo.put("type", entities.getJSONObject(j).get("answerType"));
							}
							if (!entityInfo.toString().equals("{}"))
								entityInfoArray.put(entityInfo);
						}
					} else if (entity instanceof JSONObject && !entity.toString().equals("{}")) {
						JSONObject entityObject = new JSONObject();
						JSONObject entityInfo = new JSONObject();
						entityObject = (JSONObject) entity;

						if (entityObject.has("name") && !entityObject.get("name").toString().endsWith("_")) {
							entityInfo.put("name", entityObject.get("name"));
							if (entityObject.has("label"))
								entityInfo.put("label", entityObject.get("label"));
							if (entityObject.has("answerType"))
								entityInfo.put("type", entityObject.get("answerType"));
						}
						if (!entityInfo.toString().equals("{}"))
							entityInfoArray.put(entityInfo);

					}
				} else {
					JSONObject entityInfo = new JSONObject();
					entityInfoArray.put(entityInfo);
				}
				taskInfo.put("entities", entityInfoArray);
				taskInfoArray.put(taskInfo);
			}
			result.put("tasks", taskInfoArray);
			result.write(out);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: failed to create the response\"}");
		}
		return apiResponse(API_RESPONSE_OK, out.toString());
	}

	// New Functions
	// ===============
	// For showing task info in browser
	@POST
	@Path("bot/tasks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTaskInfo(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);

		String taskInfo = instance.getTaskInfo();
		if (taskInfo == null || taskInfo.length() == 0) {
			taskInfo = "{\"response\":\"Error: no task info\"}";
		}
		return apiResponse(API_RESPONSE_OK, taskInfo);
	}

	@POST
	@Path("bot/failureInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDialogFailureInfo(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);

		String failureInfo = instance.getDialogFailureInfo();
		if (failureInfo == null || failureInfo.length() == 0) {
			failureInfo = "{\"response\":\"Error: no failure info\"}";
		}
		return apiResponse(API_RESPONSE_OK, failureInfo);
	}

	@POST
	@Path("bot/getDialog")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDialog(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		String dialogData = instance.getDialogInfo();

		return apiResponse(API_RESPONSE_OK, dialogData);
	}

	@POST
	@Path("bot/clearDialog")
	@Produces(MediaType.APPLICATION_JSON)
	public Response clearDialog(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		instance.clearDialogInfo();

		return apiResponse(API_RESPONSE_OK, "{\"response\":\"INFO: cleared dialog history\"}");
	}

	@POST
	@Path("bot/kill")
	@Produces(MediaType.APPLICATION_JSON)
	public Response killInstance(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		logDialog(sessionId);
		instances.remove(sessionId);

		return apiResponse(API_RESPONSE_OK, "{\"response\":\"INFO: removed instance " + sessionId + "\"}");
	}

	@POST
	@Path("bot/like")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logLikeTask(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");
		UIConsumer instance = instances.get(sessionId);
		instance.registerLikeDislike("LIKE");
		return apiResponse(API_RESPONSE_OK, "{\"response\":\"Info: registered like response\"}");
	}

	@POST
	@Path("bot/dislike")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logDislikeTask(@FormParam("sessionId") String sessionId) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");
		UIConsumer instance = instances.get(sessionId);
		instance.registerLikeDislike("DISLIKE");
		return apiResponse(API_RESPONSE_OK, "{\"response\":\"Info: registered dislike response\"}");
	}

	// added to support notification
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("bot/sendNotification")
	public Response sendNotification(@FormParam("sessionId") String userId, @FormParam("message") String message)
			throws URISyntaxException, InstantiationException, IllegalAccessException {
		if (message.length() == 0)
			return apiResponse(API_RESPONSE_OK, "{\"response\":\"Error: no message found, please add message\"}");
		// list of instances to which message need to be sent
		ArrayList<String> userSessions = new ArrayList<String>();
		if (userId.toLowerCase().equals("all")) {
			// add all instances to userInstances
			logger.info("added notification: " + message + ", to all the running sessions");
			instances.forEach((k, v) -> {
				userSessions.add(k);
			});
		} else if (validSessionId(userId)) { // check if it contains numeric
			// userId is sessionId
			userSessions.add(userId);
		} else {
			// userId is user so add all the instances with matching loginUsers
			instances.forEach((k, v) -> {
				UIConsumer instance = instances.get(k);
				if (instance.getDebugInfo("loginUser").equals(userId))
					userSessions.add(k);
			});
		}
		if (!userSessions.isEmpty()) {
			// send notifications
			for (String sessionId : userSessions) {
				boolean messageSent = false;
				UIConsumer instance = instances.get(sessionId);
				// iterate each entry of hashmap for websocket
				for (Entry<Session, String> entry : wsUserSessions.entrySet()) {
					// check if the
					if (entry.getValue().equals(sessionId)) {
						logger.info("found websocket session:" + entry.getValue());
						Session session = entry.getKey();
						if (message.startsWith("@") || message.startsWith("#")) {
							WebSocketMessage.sendMessage(session,
									WebSocketMessage.processMessage(entry.getValue(), message));
						} else {
							WebSocketMessage.sendMessage(session,
									instance.buildResponse("systemNotification", message));
						}
						messageSent = true;
						break;
					}
				} // if message is already sent through websocket ignore
				if (!messageSent) {
					// process dialogue on client side instead on server
					// even if it starts with @
					instance.sendNotification(sessionId, message);
					/*
					 * if (message.startsWith("@")) { // process dialogue: UIConsumerMessage msg =
					 * process(key, message); String jsonBotResponse =
					 * instance.buildResponse(message, msg.getSystemUtterance());
					 * instance.sendNotification(instanceId, jsonBotResponse); } else
					 * instance.sendNotification(instanceId, message);
					 */
				}
			}
			return apiResponse(API_RESPONSE_OK, "{\"response\":\"Info: message recieved\"}");
		} else {
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no users found to send the notification\"}");
		}
	}

	// added to support notification
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("bot/getNotification")
	public Response getNotification(@FormParam("sessionId") String userId)
			throws URISyntaxException, InstantiationException, IllegalAccessException {
		ArrayList<String> userSessions = new ArrayList<String>();
		if (validSessionId(userId))
			userSessions.add(userId);
		else {
			// userId is user so add all the instances with matching loginUsers
			instances.forEach((k, v) -> {
				UIConsumer instance = instances.get(k);
				if (instance.getDebugInfo("loginUser").equals(userId))
					userSessions.add(k);
			});
		}

		if (userSessions.size() > 1)
			logger.info("got multiple notificatons for " + userId + " processing last one");

		String response = null;
		if (!userSessions.isEmpty()) {
			for (String session : userSessions) {
				UIConsumer instance = instances.get(session);
				response = instance.getNotification();
			}
		}
		if (response != null)
			return apiResponse(API_RESPONSE_OK, response);
		else
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such session\"}");
	}

	@POST
	@Path("bot/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@QueryParam("sessionId") String sessionId,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		String user = instance.getDebugInfo("loginUser");
		String name = "";
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = new Date();
			String time = dateFormat.parse(dateFormat.format(date)).toString();
			name = user + "_" + time.replaceAll(" ", "_").replaceAll(":", "_") + "_" + fileDetail.getFileName();
			String uploadedFileLocation = new File(".").getAbsolutePath() + "/res/upload/" + name;

			// save it
			writeToFile(fileInputStream, uploadedFileLocation);
			logger.info("UPLOAD: succesfully uploaded file to upload location");

		} catch (ParseException e) {
			logger.severe("Upload:failed to upload the file");
		}
		return apiResponse(API_RESPONSE_OK, "{\"response\":\"INFO: successfully uploaded the file: " + name + " \"}");
	}

	// added for autocomplete
	@POST
	@Path("autocomplete")
	@Produces(MediaType.APPLICATION_JSON)
	public Response autocomplete(@FormParam("userUtterance") String userUtterance) {
		String response = "";
		String path = new File(".").getAbsolutePath();
		String AUTOCMPLETE_FILE = "/res/autocomplete/autocomplete.py";
		String scriptFile = path + AUTOCMPLETE_FILE;
		try {
			String line;
			StringBuilder everything = new StringBuilder();
			userUtterance = "\"" + userUtterance + "\"";
			ProcessBuilder pb = new ProcessBuilder("python", scriptFile, userUtterance);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			while ((line = in.readLine()) != null) {
				everything.append(line + "\n");
			}
			response = everything.toString().trim();
			/*
			 * String ret = everything.toString().trim(); JSONObject resultObj = new
			 * JSONObject(ret); String list = "\""+resultObj.getString("list")+"\"";
			 * response = "{\"response\":"+ list+ "}";
			 */
			return apiResponse(API_RESPONSE_OK, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: failed to perform auto complete\"}");
		}
	}

	/*
	 * Gets the content from an URL. Used for Google TTS, because we need a client
	 * that does not send the referer.
	 */
	@GET
	@Path("redirect")
	public Response getContentFromUrl(@QueryParam("url") String url_str) {
		try {
			url_str = url_str.replaceAll(" ", "+");
			URL url = new URL(url_str);

			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
			httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
			httpcon.connect();
			InputStream is = httpcon.getInputStream();

			byte[] buffer = new byte[0xFFFF];
			int length;
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			while ((length = is.read(buffer)) != -1) {
				output.write(buffer, 0, length);
			}

			byte[] content = output.toByteArray();
			is.close();
			return apiResponse(API_RESPONSE_OK, new String(content));
		} catch (Exception ex) {
			ex.printStackTrace();
			return apiResponse(API_RESPONSE_EMPTY, "");
		}
	}

	@POST
	@Path("bot/getEntityData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntityData(@FormParam("sessionId") String sessionId) {
		LinkedHashSet<String> itemFiles = new LinkedHashSet<String>();
		if (!instances.containsKey(sessionId))
			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: no such instance\"}");

		UIConsumer instance = instances.get(sessionId);
		StringWriter out = new StringWriter();
		JSONObject result = new JSONObject();
		JSONArray itemInfoArray = new JSONArray();

		try {
			JSONObject body = XML.toJSONObject(instance.getDialogXml());
			JSONObject dialog = body.getJSONObject("n:dialog");
			JSONArray taskInput = new JSONArray();
			taskInput = dialog.getJSONObject("tasks").getJSONArray("task");
			for (int i = 0; i < taskInput.length(); i++) {
				JSONArray entities = new JSONArray();
				Object entity;
				if (taskInput.getJSONObject(i).getJSONObject("entities").has("entity")) {
					entity = taskInput.getJSONObject(i).getJSONObject("entities").get("entity");
					// Entity is Array
					if (entity instanceof JSONArray && !entity.toString().equals("{}")) {
						entities = taskInput.getJSONObject(i).getJSONObject("entities").getJSONArray("entity");
						for (int j = 0; j < entities.length(); j++) {
							// look for other than loginUser_
							if (entities.getJSONObject(j).has("name")
									&& !entities.getJSONObject(j).get("name").toString().endsWith("_")) {

								String entityName = entities.getJSONObject(j).get("answerType").toString().trim();
								if (entityName.startsWith("custom.item"))
									itemFiles.add(entityName.substring(7, entityName.length()));
							}
						}
					} else if (entity instanceof JSONObject && !entity.toString().equals("{}")) {
						JSONObject entityObject = new JSONObject();
						entityObject = (JSONObject) entity;
						if (entityObject.has("name") && !entityObject.get("name").toString().endsWith("_")) {
							String entityName = entityObject.get("answerType").toString().trim();
							if (entityName.startsWith("custom.item"))
								itemFiles.add(entityName.substring(7, entityName.length()));
						}
					}
				}
			}
			// Build response
			if (dialog.has("name"))
				result.put("name", dialog.get("name"));
			if (dialog.has("version"))
				result.put("version", dialog.get("version"));
			Iterator<String> itr = itemFiles.iterator();

			while (itr.hasNext()) {
				LinkedHashSet<String> items = new LinkedHashSet<String>();
				String itemFromIterator = itr.next();
				String itemFile = new File(".").getAbsolutePath() + "/res/entities/" + itemFromIterator + ".txt";
				File f = new File(itemFile);
				FileInputStream fstream;
				if (!f.exists()) {
					// do something
					throw new NoParserFileFoundException(
							"Missing item file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
				}
				fstream = new FileInputStream(itemFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (!strLine.startsWith("#") && strLine.contains("="))
						// add category
						items.add(strLine.substring(strLine.indexOf("=") + 1, strLine.length()).trim());
					else if (!strLine.startsWith("#"))
						// add item
						items.add(strLine);
				}
				br.close();
				// add items to items array
				JSONObject item = new JSONObject();
				JSONArray itemValues = new JSONArray();
				item.put("name", itemFromIterator);
				Iterator<String> itemItr = items.iterator();
				while (itemItr.hasNext()) {
					itemValues.put(itemItr.next());
				}
				item.put("values", itemValues);
				itemInfoArray.put(item);
				items.clear();
			}
			result.put("items", itemInfoArray);
			result.write(out);

		} catch (JSONException | NoParserFileFoundException | IOException e) {

			return apiResponse(API_RESPONSE_ERROR, "{\"response\":\"Error: failed to create the response\"}");
		}

		return apiResponse(API_RESPONSE_OK, out.toString());
	}

	@GET
	@Path("msgbot/hasInstance")
	@Produces(MediaType.APPLICATION_JSON)
	public Response hasInstance(@QueryParam("userID") String userID // dialogue
	) // instance
	{
		if (!instances.containsKey(userID)) { // check if instance exists
			logger.info("no such instance");
			return apiResponse(API_RESPONSE_OK, "{\"response\":\"false\"}");
		} else {
			return apiResponse(API_RESPONSE_OK, "{\"response\":\"true\"}");
		}
	}

	// Convenience functions
	// =====================
	private UIConsumerMessage process(String sessionId, String userUtterance) {

		UIConsumer consumer = instances.get(sessionId);
		UIConsumerMessage message;
		try {
			message = consumer.processUtterance(userUtterance);
		} catch (ProcessingException ex) {
			message = new UIConsumerMessage(ex.getMessage(), Meta.ERROR);
			logger.severe("processing in REST interface failed: " + ex.getMessage());
		}
		return message;
	}

	private boolean createDialog(String sessionId, String dialogxml) {
		if (!instances.containsKey(sessionId)) {
			logger.info("creating a new dialog instance for sessionId: " + sessionId);
			UIConsumer newConsumer;
			try {
				if (dialogxml != null) {
					Dialog d = Dialog.loadFromXml(dialogxml);
					newConsumer = consumerFactory.create(d); // new
																// DialogManager(d);
				} else {
					newConsumer = consumerFactory.create(); // new DialogManager();
				}

				newConsumer.setAdditionalDebugInfo("clientIP", request.getRemoteAddr().toString());
				newConsumer.setAdditionalDebugInfo("userAgent", headers.getRequestHeader("User-Agent").toString());
				newConsumer.setAdditionalDebugInfo("loginUser", loginUser);
				newConsumer.setAdditionalDebugInfo("loginRole", loginRole);
				newConsumer.setAdditionalDebugInfo("authToken", authToken);
				newConsumer.setAdditionalDebugInfo("sessionID", sessionId);

				// adding loginUser and sessionId to context
				newConsumer.addtoEntityMapping("loginUser_", loginUser);
				newConsumer.addtoEntityMapping("loginRole_", loginRole);
				newConsumer.addtoEntityMapping("authToken_", authToken);

				logger.info("added user " + loginUser + " to context");
				logger.info("added role " + loginRole + " to context");
				logger.info("added authToken " + authToken + " to context");
				newConsumer.addtoEntityMapping("sessionId_", sessionId);
				logger.info("added sessionId " + sessionId + " to context");

				instances.put(sessionId, newConsumer);
				ConvEngineProcessor.getLogger().fine("created new instance " + newConsumer.getClass().getName());
			} catch (RuntimeError err) {
				err.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean createMsgDialog(String sessionId) {
		if (!instances.containsKey(sessionId)) {
			UIConsumer newConsumer;
			try {

				newConsumer = consumerFactory.create(); // new DialogManager();

				newConsumer.setAdditionalDebugInfo("clientIP", request.getRemoteAddr().toString());
				newConsumer.setAdditionalDebugInfo("userAgent", headers.getRequestHeader("User-Agent").toString());
				newConsumer.setAdditionalDebugInfo("loginUser", loginUser);
				newConsumer.setAdditionalDebugInfo("loginRole", loginRole);
				newConsumer.setAdditionalDebugInfo("authToken", authToken);
				newConsumer.setAdditionalDebugInfo("sessionID", sessionId);

				// adding loginUser and sessionId to context
				newConsumer.addtoEntityMapping("loginUser_", loginUser);
				newConsumer.addtoEntityMapping("loginRole_", loginRole);
				newConsumer.addtoEntityMapping("authToken_", authToken);
				logger.info("added user " + loginUser + " to context");
				logger.info("added role " + loginRole + " to context");
				newConsumer.addtoEntityMapping("sessionId_", sessionId);
				logger.info("added sessionId " + sessionId + " to context");

				instances.put(sessionId, newConsumer);
				ConvEngineProcessor.getLogger().fine("created new instance " + newConsumer.getClass().getName());
			} catch (RuntimeError err) {
				err.printStackTrace();
			}
			return true;
		}
		return false;
	}

	private boolean createDialog(String sessionId) {
		return createDialog(sessionId, null);
	}

	private String generateDialogID(String sessionId) {
		if (sessionId.isEmpty()) {
			String id = generateNextID() + "-" + RandomStringUtils.randomAlphanumeric(12).toUpperCase();
			return id;
		} else
			return sessionId;
	}

	private static String generateNextID() {
		instanceCounter++;
		if (instanceCounter % 10 == 0)
			cleanUp(); // run clean up every 10 instances
		if (instanceCounter > MAX_DIALOG_COUNTER) // reset to avoid large numbers
			instanceCounter = 0;
		String identifier = "d" + instanceCounter;
		return identifier;
	}

	/**
	 * deletes unused sessions
	 */
	protected static void cleanUp() {

		int threshold_minutes = 10;

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, -threshold_minutes);
		Date threshold = cal.getTime();

		ArrayList<String> deletionCandidates = new ArrayList<String>();
		for (Map.Entry<String, UIConsumer> entry : instances.entrySet()) {
			if (entry.getValue().getLastAccess().before(threshold)) {
				deletionCandidates.add(entry.getKey());
			}
		}

		for (String id : deletionCandidates) {
			logDialog(id);
			instances.remove(id);
		}
	}

	/**
	 * init dialogue, i.e. get first question
	 * 
	 */
	private Response initDialog(String sessionId) throws URISyntaxException {
		UIConsumerMessage message = process(sessionId, "");

		String systemUtterance = message.getSystemUtterance();

		// build bot response
		UIConsumer instance = instances.get(sessionId);
		String jsonBotResponse = instance.buildResponse("", systemUtterance);

		if (message.getMeta() == Meta.UNCHANGED)
			return apiResponse(API_RESPONSE_CREATED, null, "/" + sessionId);

		else
			return apiResponse(API_RESPONSE_CREATED, jsonBotResponse, uri.getBaseUri() + "bot");

	}

	// added for message bot
	private Response initMsgDialog(String sessionId) throws URISyntaxException {
		UIConsumerMessage message = process(sessionId, ""); // empty utterance
															// => init
		String systemUtterance = message.getSystemUtterance();
		// Add instance id to bot response
		// DialogManager.botResponse_header.put("id", instance_id);

		// build bot response
		UIConsumer instance = instances.get(sessionId);
		String jsonBotResponse = instance.buildResponse("", systemUtterance);

		if (message.getMeta() == Meta.UNCHANGED)
			return apiResponse(API_RESPONSE_CREATED, null, "/" + sessionId);

		else
			return apiResponse(API_RESPONSE_CREATED, jsonBotResponse, uri.getBaseUri() + "msgbot");
	}

	private String addXSDReference(String xml, String schemaPath) {
		xml = xml.subSequence(xml.indexOf("\n") + 1, xml.length()).toString();
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<?xml-stylesheet href=\"" + schemaPath
				+ "\" type=\"text/xsl\"?>\r\n" + xml;

		return xml;
	}

	// Interface functions
	// ===================

	@Override
	public void start() {
		try {
			ConvEngineConfig config = ConvEngineConfig.getInstance();

			String propertiesFile = "";
			Properties prop = new Properties();
			propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE).substring(8);
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			boolean isHTTPS = false;

			server = new Server();

			// Web handler to handle index.html
			ResourceHandler webHandler = new ResourceHandler();
			webHandler.setDirectoriesListed(true);
			webHandler.setResourceBase(config.getProperty(ConvEngineConfig.JETTYRESOURCEBASE));
			webHandler.setWelcomeFiles(new String[] { "index.html" });

			// REST API handler to handle bot APIs , multipart media and WebSocket
			ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
			servletHandler.setContextPath(config.getProperty(ConvEngineConfig.JETTYCONTEXTPATH));

			// REST holder to handle REST APIs including multipart
			ResourceConfig resConfig = new ResourceConfig();
			resConfig.register(MultiPartFeature.class);
			resConfig.packages("cto.hmi.processor.ui");
			ServletHolder restServletHolder = new ServletHolder(new ServletContainer(resConfig));
			restServletHolder.setInitParameter(ServerProperties.PROVIDER_PACKAGES, "");
			restServletHolder.setInitOrder(1);
			servletHandler.addServlet(restServletHolder, "/api/v2/*");

			// Websocket holder to handle websocket APIs
			ServletHolder webSocketServletHolder = new ServletHolder(SocketServlet.class);
			servletHandler.addServlet(webSocketServletHolder, "/api/v2/msg/*");
			webSocketServletHolder.setInitOrder(2);

			// collect both the handlers
			HandlerCollection handlers = new HandlerCollection();
			handlers.addHandler(webHandler);
			handlers.addHandler(servletHandler);

			if (prop.getProperty("PROTOCOL") != null)
				if (prop.getProperty("PROTOCOL").toLowerCase().equals("https"))
					isHTTPS = true;

			// check if allow CORS is true
			if (prop.getProperty("ENABLE_CORS") != null)
				if (prop.getProperty("ENABLE_CORS").toLowerCase().equals("true"))
					isCORS = true;

			// check if CORS allowed
			if (isCORS) {
				logger.warning("Cross Origin Resource Sharing(CORS) is set to true. Potential security risk.");
				// load CORS parameters
				ACCESS_CONTROL_ALLOW_ORIGIN = prop.getProperty("ACCESS_CONTROL_ALLOW_ORIGIN");
				ACCESS_CONTROL_ALLOW_CREDENTIALS = prop.getProperty("ACCESS_CONTROL_ALLOW_CREDENTIALS");
				ACCESS_CONTROL_ALLOW_METHODS = prop.getProperty("ACCESS_CONTROL_ALLOW_METHODS");
				ACCESS_CONTROL_ALLOW_HEADERS = prop.getProperty("ACCESS_CONTROL_ALLOW_HEADERS");
				ACCESS_CONTROL_EXPOSE_HEADERS = prop.getProperty("ACCESS_CONTROL_EXPOSE_HEADERS");
			}

			if (isHTTPS) {
				HttpConfiguration https = new HttpConfiguration();
				https.addCustomizer(new SecureRequestCustomizer());

				SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
				// set keystore path
				sslContextFactory.setKeyStorePath(config.getProperty(ConvEngineConfig.JETTYKEYSTOREPATH));
				if (prop.getProperty("JETTY_KS_PASSWORD") != null)
					sslContextFactory.setKeyStorePassword(prop.getProperty("JETTY_KS_PASSWORD"));
				else
					sslContextFactory.setKeyStorePassword(config.getProperty(ConvEngineConfig.JETTYKEYSTOREPASS));
				ServerConnector sslConnector = new ServerConnector(server,
						new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
				sslConnector.setPort(Integer.parseInt(ConvEngineProcessor.getJettyPort()));
				server.setConnectors(new Connector[] { sslConnector });

			} else {
				ServerConnector connector = new ServerConnector(server);
				connector.setPort(Integer.parseInt(ConvEngineProcessor.getJettyPort()));
				server.setConnectors(new Connector[] { connector });
			}

			// add the handlers to server
			server.setHandler(handlers);
			server.start();
			logger.info("REST interface started on " + server.getURI());
			// server.dump(System.err);
			server.join();

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.severe("Error: failed to start Jetty: " + ex.getMessage());

		} finally {
			server.destroy();
		}
	}

	@Override
	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
			server.destroy();
		}
	}

	static void logDialog(String sessionId) {
		try {
			String propertiesFile = "";
			Properties prop = new Properties();
			propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE).substring(8);
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			if (prop.getProperty("LOG_DIALOG").toLowerCase().equals("true")) {
				UIConsumer instance = instances.get(sessionId);
				JSONObject dialogData = new JSONObject(instance.getDialogInfo());
				String domain = ConvEngineProcessor.getDefaultDialogPathAndName();
				String user = dialogData.getString("user");
				String dialog = dialogData.getString("dialog");
				String client_ip = dialogData.getString("clientIP");
				dialog = dialog.replaceAll("(\\t|\\r?\\n)+", " ");
				dialog = dialog.replaceAll("\\{|\\[|\\]|\\}", "");
				LogForDialog.logData(domain, client_ip, user, sessionId, dialog);
			}
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			logger.severe("Failed to log the dialog" + e.getMessage());
			e.printStackTrace();
		}

	}

	public static UIConsumer getInstance(String sessionId) {
		return RESTInterface.instances.get(sessionId);
	}

	Response apiResponse(String type, String responseMessage) {
		return apiResponse(type, responseMessage, null);
	}

	Response apiResponse(String type, String responseMessage, String URIName) {

		switch (type) {
		case API_RESPONSE_OK:
			if (isCORS)
				return Response.ok(responseMessage)
						.header("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN)
						.header("Access-Control-Allow-Credentials", ACCESS_CONTROL_ALLOW_CREDENTIALS)
						.header("Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS)
						.header("Access-Control-Allow-Headers", ACCESS_CONTROL_ALLOW_HEADERS)
						.header("Access-Control-Expose-Headers", ACCESS_CONTROL_EXPOSE_HEADERS).build();
			else
				return Response.ok(responseMessage).build();

		case API_RESPONSE_CREATED:
			try {
				if (isCORS)
					return Response.created(new URI(URIName)).entity(responseMessage)
							.header("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN)
							.header("Access-Control-Allow-Credentials", ACCESS_CONTROL_ALLOW_CREDENTIALS)
							.header("Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS)
							.header("Access-Control-Allow-Headers", ACCESS_CONTROL_ALLOW_HEADERS)
							.header("Access-Control-Expose-Headers", ACCESS_CONTROL_EXPOSE_HEADERS).build();
				else
					return Response.created(new URI(URIName)).entity(responseMessage).build();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		case API_RESPONSE_ERROR:
			if (isCORS)
				return Response.serverError().entity(responseMessage)
						.header("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN)
						.header("Access-Control-Allow-Credentials", ACCESS_CONTROL_ALLOW_CREDENTIALS)
						.header("Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS)
						.header("Access-Control-Allow-Headers", ACCESS_CONTROL_ALLOW_HEADERS)
						.header("Access-Control-Expose-Headers", ACCESS_CONTROL_EXPOSE_HEADERS).build();
			else
				return Response.serverError().entity(responseMessage).build();

		case API_RESPONSE_EMPTY:
			if (isCORS)
				return Response.noContent().entity(responseMessage)
						.header("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN)
						.header("Access-Control-Allow-Credentials", ACCESS_CONTROL_ALLOW_CREDENTIALS)
						.header("Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS)
						.header("Access-Control-Allow-Headers", ACCESS_CONTROL_ALLOW_HEADERS)
						.header("Access-Control-Expose-Headers", ACCESS_CONTROL_EXPOSE_HEADERS).build();
			else
				return Response.noContent().entity(responseMessage).build();

		}
		return null;
	}

	private void writeToFile(InputStream uploadedInputStream, String uploadedFileLocation) {

		try {
			OutputStream out = new FileOutputStream(new File(uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	// validate if session starts with d or s or x followed by - followed by 12
	// digit alphabetic UPPERCASE
	private boolean validSessionId(String sessionId) {
		if (Pattern.compile("^[sxd]?\\d+-[A-Z0-9]{12}").matcher(sessionId).find())
			return true;
		else
			return false;
	}
}
