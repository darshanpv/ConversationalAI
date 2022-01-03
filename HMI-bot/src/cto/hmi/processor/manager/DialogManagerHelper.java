package cto.hmi.processor.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.javatuples.Quartet;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jayway.jsonpath.JsonPath;

import cto.hmi.bot.util.Transliteration;
import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Entity;
import cto.hmi.processor.dialogmodel.Task;
import cto.hmi.processor.exceptions.NoParserFileFoundException;


public class DialogManagerHelper {
	static HttpClient client;
	private final static Logger logger = ConvEngineProcessor.getLogger();

	private enum formType {
		TASK_LIST, ENTITY_LIST
	};

	static trainingObject likeDislikeObject = new trainingObject();

	/*
	 * This function will do transliterate based on the language
	 */

	protected static String getTransliteratedvalue(String value, String type, String lang) {

		switch (lang) {
		case "ar":
			switch (type) {
			// Do Transliteration only for these items
			case "sys.location.city":
			case "sys.person":
			case "sys.person.firstname":
			case "sys.person.lastname":
			case "sys.organization":
			case "sys.corpus.qa":
			case "sys.opentext":
			case "customItems":
			case "customMultiItems":
			case "customButtons":
			case "sys.decision":
			case "sys.onoff":
			case "dummy":
				value = Transliteration.EnglishToArabic(value);
				break;

			default:
				break;
			}
			break;
		case "en":
		case "da":
		case "nl":
		case "sv":

			break;
		case "hi":
			switch (type) {
			// Do Transliteration only for these items
			case "sys.location.city":
			case "sys.person":
			case "sys.person.firstname":
			case "sys.person.lastname":
			case "sys.organization":
			case "sys.corpus.qa":
			case "sys.opentext":
			case "customItems":
			case "customMultiItems":
			case "customButtons":
			case "sys.decision":
			case "sys.onoff":
			case "dummy":
				value = Transliteration.EnglishToDevnagari(value);
				break;

			default:
				break;
			}
			break;

		default:
			break;
		}
		return value;
	}

	/*
	 * This function does return the comma separated items from custom items
	 */
	protected static String getEntityItems(String itemType) {

		String fName = itemType.substring(7, itemType.length());
		String res = "";
		LinkedHashSet<String> items = null;
		List<String> sliderItems = null;
		try {
			items = new LinkedHashSet<String>();
			sliderItems = new ArrayList<String>();
			String itemFile = "";
			if (itemType.startsWith("custom.item") || itemType.startsWith("custom.button")
					|| itemType.startsWith("custom.multiItem") || itemType.startsWith("custom.slider")
					|| itemType.startsWith("custom.menu"))
				itemFile = new File(".").getAbsolutePath() + "/res/entities/" + fName + ".txt";
			else if (itemType.startsWith("custom.urlList") || itemType.startsWith("custom.multiUrlList")) {
				itemFile = new File(".").getAbsolutePath() + "/res/temp/" + fName + ".txt";
				// we need to call the url to populate the options in
				// interactive form
				// ignore if it is already available
				File urlFile = new File(itemFile);
				if (!urlFile.exists()) {
					logger.info("fetching items from urlList");
					populateUrlEntity(fName);
				}
			}
			File f = new File(itemFile);
			FileInputStream fstream;
			if (!f.exists()) {
				// do something
				throw new NoParserFileFoundException(
						"Missing entity file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
			} else {
				fstream = new FileInputStream(itemFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (!strLine.startsWith("#") && strLine.contains("="))
						// add category
						items.add(strLine.substring(strLine.indexOf("=") + 1, strLine.length()).trim());
					else if (!strLine.startsWith("#")) {
						// add item
						if (itemType.startsWith("custom.slider")) // slider item
																	// may
																	// contain
																	// the
																	// duplicate
																	// rows
							sliderItems.add(strLine);
						else
							items.add(strLine);
					}
				}
				br.close();
				if (itemType.startsWith("custom.slider"))
					res = String.join(",", sliderItems);
				else
					res = String.join(",", items);
				res = res.trim();
				if (res.endsWith(","))
					res = res.substring(0, res.lastIndexOf(","));
			}
		} catch (IOException | NoParserFileFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return res;
		}
		return res;
	}

	/*
	 * This function fetches urlList
	 */
	private static void populateUrlEntity(String fName) {
		// TODO Auto-generated method stub
		String path = new File(".").getAbsolutePath();
		String fname = fName + ".txt";
		String PROPERTY_FILE = "/res/entities/" + fname;
		String TEMP_DIR = "/res/temp/";
		String urlMethod = "";
		String itemURL = "";
		String params = "";
		String jPath = "";
		String tempDir = path + TEMP_DIR;
		ArrayList<String> items = new ArrayList<String>();
		String[] params_arr = null;

		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream(path + PROPERTY_FILE);
			prop.load(input);
			urlMethod = prop.getProperty("URL_METHOD").toLowerCase();
			itemURL = prop.getProperty("URL");
			params = prop.getProperty("PARAMS");
			jPath = prop.getProperty("JPATH");

			// check if temp folder exist if not create it
			File directory = new File(tempDir);
			if (!directory.exists()) {
				directory.mkdir();
			}
			String tmpFile = tempDir + fname;
			init();
			ContentResponse response;
			Request request;
			request = client.newRequest(itemURL);
			// choose method
			if (urlMethod.toLowerCase().equals("get")) {
				request.method(HttpMethod.GET);
			} else {
				request.method(HttpMethod.POST);
			}
			// process parameters if defined
			if (params != null) {
				params_arr = params.split("&");
				String[] key_value;
				for (String paramPair : params_arr) {
					key_value = paramPair.split("=");
					if (key_value.length > 1) {
						// check if key_value is referring to entity e.g.
						// city=%getDepartureCity
						String urlParamValue = key_value[1];
						// *****Need fix******
						// urlParamValue =
						// DialogManager.fillUrlParamWithEntity(urlParamValue);
						request.param(key_value[0], urlParamValue);
					} else
						request.param(key_value[0], "");
				}
			}
			logger.info("requesting: " + request.getURI() + ", " + request.getParams().toString());
			response = request.send();
			logger.info("HTTP status: " + response.getStatus());
			String body = response.getContentAsString();
			client.stop();
			logger.info("HTTP response: " + body.replace("\r\n", "").trim());

			if (jPath.replaceAll("\\s", "").length() > 0) {
				String jsonExp = jPath;
				items = JsonPath.read(body, jsonExp);
			}
			FileWriter writer = new FileWriter(tmpFile);
			writer.write("##DO NOT REMOVE THIS LINE\n");
			if (items.size() > 0) {
				logger.info("got items: " + items.toString());
				for (String str : items.get(0).split(",")) {
					writer.write(str.trim() + "\n");
				}
			}
			writer.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * This function returns appropriate UI type based on Entity type
	 */
	protected static String getUIType(String type) {

		switch (type) {
		case "sys.location.city":
		case "sys.person":
		case "sys.mail":
		case "sys.person.firstname":
		case "sys.person.lastname":
		case "sys.password":
		case "sys.organization":
		case "sys.corpus.qa":
		case "sys.opentext":
		case "dummy":
		case "customPattern":
			return "text";

		case "sys.contact":
		case "sys.number":
		case "sys.number.scale":
		case "sys.number.float":
			return "number";

		case "sys.temporal.date":
			return "date";

		case "sys.temporal.time":
			return "time";

		case "customItems":
			return "list";

		case "customButtons":
			return "button";

		case "customMultiItems":
			return "multiSelectionList";

		case "sys.decision":
		case "sys.onoff":
			return "radio";

		case "customSlider":
			return "slider";

		default:
			return "text";
		}
	}

	/*
	 * This function builds the iFrom object to be part of JSON response
	 */

	protected static JSONObject createiForm(DialogManagerContext context,
			Quartet<String, String, String, String> botEntityInAction, boolean SHOW_FEEDBACK_ICON, int LIKE_COUNTS,
			int DISLIKE_COUNTS) {
		String lang = context.getLocale().getLanguage();
		JSONObject iFormObject = new JSONObject();
		JSONObject feedbackObject = new JSONObject();
		if (context.getCurrentTask() != null) {
			try {
				formType cType;
				if (context.getCurrentTask().getName().equals("start"))
					cType = formType.TASK_LIST;
				else
					cType = formType.ENTITY_LIST;

				switch (cType) {
				case TASK_LIST:
					// populate iForm

					JSONArray taskArray = new JSONArray();
					int i = 1;
					Iterator<Task> tk = context.getDialog().getTasks().iterator();
					while (tk.hasNext()) {
						Task t = tk.next();
						if (!t.getLabel().equals("") && !(t.getName().equals("getFAQ") || t.getName().startsWith("EVT_")
								|| t.getName().equals("start"))) {
							JSONObject taskObject = new JSONObject();
							taskObject.put("id", Integer.toString(i++));
							taskObject.put("name", t.getName());
							taskObject.put("label", t.getLabel());
							taskObject.put("role", t.getRole());
							taskArray.put(taskObject);
						}

					}
					iFormObject.put("type", "taskList");
					feedbackObject = new JSONObject();
					feedbackObject.put("isVisible", "false");
					feedbackObject.put("likes", String.valueOf(LIKE_COUNTS));
					feedbackObject.put("dislikes", String.valueOf(DISLIKE_COUNTS));

					if (SHOW_FEEDBACK_ICON)
						feedbackObject.put("isVisible", "true");
					iFormObject.put("feedback", feedbackObject);
					iFormObject.put("tasks", taskArray);
					break;
				case ENTITY_LIST:
					feedbackObject = new JSONObject();
					feedbackObject.put("isVisible", "false");
					feedbackObject.put("likes", String.valueOf(LIKE_COUNTS));
					feedbackObject.put("dislikes", String.valueOf(DISLIKE_COUNTS));

					if (SHOW_FEEDBACK_ICON)
						feedbackObject.put("isVisible", "true");
					iFormObject.put("feedback", feedbackObject);

					JSONArray entityArray = new JSONArray();
					int j = 1;
					boolean isFollowupEntity = true;
					Iterator<Entity> it = context.getCurrentTask().getEntities().iterator();
					while (it.hasNext()) {
						Entity entity = it.next();
						if (entity.getName().equals(botEntityInAction.getValue0()))
							isFollowupEntity = false;
						if (entity.getName().endsWith("_"))
							continue;
						JSONObject entityObject = new JSONObject();
						entityObject.put("id", Integer.toString(j++));
						entityObject.put("name", entity.getName());
						entityObject.put("label", entity.getLabel());
						String type = entity.getAnswerType();

						if (type.startsWith("custom.item") || type.startsWith("custom.urlList")
								|| type.startsWith("custom.menu"))
							type = "customItems";
						if (type.startsWith("custom.button"))
							type = "customButtons";
						if (type.startsWith("custom.multiItem") || type.startsWith("custom.multiUrlList"))
							type = "customMultiItems";
						if (type.startsWith("custom.pattern"))
							type = "customPattern";
						if (type.startsWith("custom.slider"))
							type = "customSlider";

						entityObject.put("entityType", type);
						entityObject.put("type", DialogManagerHelper.getUIType(type));
						entityObject.put("elements", "");
						// modify elements for list,radio and checkbox elements
						if (type.equals("customItems") || type.equals("customMultiItems")
								|| type.equals("customButtons") || type.equals("customSlider")) {
							String itemList = DialogManagerHelper.getEntityItems(entity.getAnswerType());
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue(itemList, "customMultiItems", lang));
						} else if (type.equals("sys.decision")) {
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue("yes,no", "sys.decision", lang));
						} else if (type.equals("sys.onoff")) {
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue("on,off", "sys.onoff", lang));
						}
						entityObject.put("value", "");
						// modify value based on entity status
						if (entity.getValue() != null)
							entityObject.put("value",
									DialogManagerHelper.getTransliteratedvalue(entity.getValue().toString(), type, lang));

						if (type.equals("sys.opentext"))
							entityObject.put("isVisible", "false");
						else
							entityObject.put("isVisible", "true");

						if (entity.getName().equals(botEntityInAction.getValue0()))
							entityObject.put("isActive", "true");
						else
							entityObject.put("isActive", "false");

						entityArray.put(entityObject);

					}
					// the entity is from followup and need to be added manually
					if (isFollowupEntity) {
						// we need to remove the earlier
						entityArray = new JSONArray();
						j = 1;
						JSONObject entityObject = new JSONObject();
						entityObject.put("id", Integer.toString(j++));
						entityObject.put("name", botEntityInAction.getValue0());
						entityObject.put("label", botEntityInAction.getValue1());
						String type = botEntityInAction.getValue2();
						if (type.startsWith("custom.item") || type.startsWith("custom.urlList")
								|| type.startsWith("custom.menu"))
							type = "customItems";
						if (type.startsWith("custom.button"))
							type = "customButtons";
						if (type.startsWith("custom.multiItem") || type.startsWith("custom.multiUrlList"))
							type = "customMultiItems";
						if (type.startsWith("custom.pattern"))
							type = "customPattern";
						if (type.startsWith("custom.slider"))
							type = "customSlider";

						entityObject.put("entityType", type);
						entityObject.put("type", DialogManagerHelper.getUIType(type));
						entityObject.put("elements", "");
						// modify elements for list,radio and checkbox elements
						if (type.equals("customItems") || type.equals("customMultiItems")
								|| type.equals("customButtons") || type.equals("customSlider")) {
							String itemList = DialogManagerHelper.getEntityItems(botEntityInAction.getValue2());
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue(itemList, "customMultiItems", lang));
						} else if (type.equals("sys.decision")) {
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue("yes,no", "sys.decision", lang));
						} else if (type.equals("sys.onoff")) {
							entityObject.put("elements",
									DialogManagerHelper.getTransliteratedvalue("on,off", "sys.onoff", lang));
						}
						entityObject.put("value", "");
						// modify value based on entity status
						if (botEntityInAction.getValue3() != null)
							entityObject.put("value", DialogManagerHelper
									.getTransliteratedvalue(botEntityInAction.getValue3(), type, lang));

						entityObject.put("isVisible", "true");
						entityObject.put("isActive", "true");
						entityArray.put(entityObject);
					}

					// check if the current Entity is of open text type then set
					// all
					// the visible parameters to false
					boolean IS_VISIBLE_SET = false;
					if (botEntityInAction.getValue2().equals("sys.opentext")) {
						for (int k = 0; k < entityArray.length(); k++) {
							JSONObject entity = entityArray.getJSONObject(k);
							if (entity.get("entityType").equals("sys.opentext")
									&& entity.get("value").toString().length() < 1 && !IS_VISIBLE_SET) {
								IS_VISIBLE_SET = true;
								entity.put("isVisible", "true");
								continue;

							} else
								entity.put("isVisible", "false");
						}
					}
					// addded menu option for better UI control
					if (context.getCurrentTask().getEntities().get(0).getAnswerType().startsWith("custom.menu"))
						iFormObject.put("type", "menuList");
					else
						iFormObject.put("type", "entityList");
					iFormObject.put("entities", entityArray);
					break;

				default:
					break;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return iFormObject;
	}

	/*
	 * This function fills fallback Question with Entity values from EntityMapping
	 */
	protected static String fillfbqWithEntity(String msg, Map<String, String> entityMapping) {

		for (Map.Entry<String, String> entry : entityMapping.entrySet()) {
			msg = msg.replace("%" + entry.getKey(), entry.getValue());
		}
		return msg;
	}

	/*
	 * This function fills url parameters with Entity values from EntityMapping used in
	 * UrlListParser
	 */
	protected static String fillUrlParamWithEntity(String url, Map<String, String> entityMapping) {

		for (Map.Entry<String, String> entry : entityMapping.entrySet()) {
			url = url.replace("%" + entry.getKey(), entry.getValue());
		}
		return url;
	}

	public static String escapeMetaCharacters(String inputString) {
		// This function escapes the special characters that are used in Regex
		// pattern to avoid "Dangling meta character" error

		final String[] metaCharacters = { "\\", "^", "$", "{", "}", "[", "]", "(", ")", ".", "*", "+", "?", "|", "<",
				">", "-", "&", "%", "=" };

		for (int i = 0; i < metaCharacters.length; i++) {
			if (inputString.contains(metaCharacters[i])) {
				inputString = inputString.replace(metaCharacters[i], "\\" + metaCharacters[i]);
			}
		}
		return inputString;

	}

	public static Map<String, String> findRoleHierarchy() {
		String LEVEL_0 = "";
		String LEVEL_1 = "";
		String LEVEL_2 = "";
		String LEVEL_3 = "";
		String LEVEL_4 = "";
		String LEVEL_5 = "";
		Map<String, String> roles = new HashMap<String, String>();
		Properties prop = new Properties();
		String propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.ROLESFILE).substring(8);
		InputStream input;
		try {
			input = new FileInputStream(propertiesFile);
			prop.load(input);
			if (prop.containsKey("LEVEL_0")) {
				LEVEL_0 = prop.getProperty("LEVEL_0");
				for (int i = 0; i < LEVEL_0.trim().split(",").length; i++) {
					if (!LEVEL_0.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_0.trim().split(",")[i].toLowerCase(), "0");
				}
			}
			if (prop.containsKey("LEVEL_1")) {
				LEVEL_1 = prop.getProperty("LEVEL_1");
				for (int i = 0; i < LEVEL_1.trim().split(",").length; i++) {
					if (!LEVEL_1.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_1.trim().split(",")[i].toLowerCase(), "1");
				}
			}
			if (prop.containsKey("LEVEL_2")) {
				LEVEL_2 = prop.getProperty("LEVEL_2");
				for (int i = 0; i < LEVEL_2.trim().split(",").length; i++) {
					if (!LEVEL_2.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_2.trim().split(",")[i].toLowerCase(), "2");
				}
			}
			if (prop.containsKey("LEVEL_3")) {
				LEVEL_3 = prop.getProperty("LEVEL_3");
				for (int i = 0; i < LEVEL_3.trim().split(",").length; i++) {
					if (!LEVEL_3.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_3.trim().split(",")[i].toLowerCase(), "3");
				}
			}
			if (prop.containsKey("LEVEL_4")) {
				LEVEL_4 = prop.getProperty("LEVEL_4");
				for (int i = 0; i < LEVEL_4.trim().split(",").length; i++) {
					if (!LEVEL_4.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_4.trim().split(",")[i].toLowerCase(), "4");
				}
			}
			if (prop.containsKey("LEVEL_5")) {
				LEVEL_5 = prop.getProperty("LEVEL_5");
				for (int i = 0; i < LEVEL_5.trim().split(",").length; i++) {
					if (!LEVEL_5.trim().split(",")[i].isEmpty())
						roles.put(LEVEL_5.trim().split(",")[i].toLowerCase(), "5");
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.severe("no roles.properties file found");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return roles;
	}

	private static void init() {
		// SslContextFactory sslContextFactory = new SslContextFactory();
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		client = new HttpClient(sslContextFactory);
		try {
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class trainingObject {
	String task, userUtterance, systemUtterance;

	public trainingObject() {
		// TODO Auto-generated constructor stub
		this.task = "";
		this.userUtterance = "";
		this.systemUtterance = "";
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getUserUtterance() {
		return userUtterance;
	}

	public void setUserUtterance(String userUtterance) {
		this.userUtterance = userUtterance;
	}

	public String getSystemUtterance() {
		return systemUtterance;
	}

	public void setSystemUtterance(String systemUtterance) {
		this.systemUtterance = systemUtterance;
	}

}
