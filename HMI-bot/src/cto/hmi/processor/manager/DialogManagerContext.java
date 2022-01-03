package cto.hmi.processor.manager;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.processor.contexthelper.HistoryElem;
import cto.hmi.processor.contexthelper.HistoryTree;
import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.dialogmodel.Entity;
import cto.hmi.processor.dialogmodel.Task;

@XmlRootElement
@XmlType(propOrder = { "serializeDialog", "dialogHistory", "serializeTaskStack", "serializeFrameRepresentation",
		"serializeCurrentTask", "questionOpen", "currentQuestionUtterance", "started", "instance", "lastAccess",
		"createdOn", "additionalDebugInfo" })
public class DialogManagerContext {

	// Features
	private Date createdOn;
	private Date lastAccess;
	private HashMap<String, String> additionalDebugInfo = new HashMap<String, String>();
	private Boolean question_open = false;
	private Boolean started = false;
	private ArrayList<Entity> entityHistory = new ArrayList<Entity>();
	private HistoryTree history = new HistoryTree(1);
	private HistoryTree current_node = history;
	private Stack<Task> taskStack = new Stack<Task>();
	private Iterator<Entity> entityIterator = null;
	private Dialog dialog = null;
	private String instance = null;
	// added to store the systeUtterance in JSON format
	private String systemMessage = null;
	private JSONArray jDialog = new JSONArray();
	private Locale dLocale = Locale.getDefault();

	public enum UTTERANCE_TYPE {
		USER, SYSTEM
	};

	// getter
	public Boolean isQuestionOpen() {
		return question_open;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public Date getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(Date lastAccess) {
		this.lastAccess = lastAccess;
	}

	@XmlElement(name = "additionalDebugInfo")
	public String getAdditionalDebugInfo() {
		String info = "";
		for (Map.Entry<String, String> elem : additionalDebugInfo.entrySet()) {
			info += elem.getKey() + ": " + elem.getValue() + "; ";
		}
		return info;
	}

	public Boolean isStarted() {
		return started;
	}

	@XmlElement(name = "currentQuestion")
	private String getCurrentQuestionUtterance() {
		if (question_open) {
			return getCurrentQuestion().getUtteranceText();
		} else
			return "no current question";
	}

	@XmlElement(name = "dialogHistory")
	private HistoryTree getDialogHistory() {
		return history;
	}

	/**
	 * 
	 * @return an XML-compatible list-representation of the frame for pretty
	 *         printing
	 */
	@XmlElementWrapper(name = "frame")
	@XmlElement(name = "entry")
	public List<String> getSerializeFrameRepresentation() {
		ArrayList<String> frame = new ArrayList<String>();
		if (!taskStack.isEmpty()) {
			for (Map.Entry<Object, Object> entry : taskStack.lastElement().toFrame().entrySet()) {
				frame.add(entry.getKey().toString() + ": " + entry.getValue().toString());
			}
		}
		return frame;
		// return task.toFrame(); //does not work on my Windows Setup... On Mac
		// it works fine...
	}

	@XmlElement(name = "currentTask")
	public String getSerializeCurrentTask() {
		if (taskStack.size() > 0)
			return taskStack.lastElement().getName();
		else
			return "none";
	}

	@XmlElementWrapper(name = "taskStack")
	@XmlElement(name = "task")
	public List<String> getSerializeTaskStack() {
		ArrayList<String> stack = new ArrayList<String>();
		for (Task t : taskStack) {
			stack.add(t.getName());
		}
		return stack;
	}

	@XmlElement(name = "dialog")
	public String getSerializeDialog() {
		return dialog.getName();
	}

	@XmlElement(name = "instance")
	public String getInstance() {
		return instance;
	}

	// transient getters

	@XmlTransient
	public Dialog getDialog() {
		return dialog;
	}

	@XmlTransient
	public Entity getCurrentQuestion() {
		if (entityHistory.size() == 0)
			return null;
		else
			return entityHistory.get(entityHistory.size() - 1);
	}

	@XmlTransient
	public Iterator<Entity> getIto_iterator() {
		return entityIterator;
	}

	@XmlTransient
	public Task getCurrentTask() {
		if (taskStack.size() > 0)
			return taskStack.lastElement();
		else
			return null;
	}

	public Task getTask(String task) {
		for (int i = 0; i < (taskStack.size()); i++) {
			if (taskStack.elementAt(i).getName().equals(task))
				return taskStack.elementAt(i);
		}
		return null;
	}

	public String getDialogInfo() {
		try {
			StringWriter out = new StringWriter();
			JSONObject infoObject = new JSONObject();
			infoObject.put("user", getAdditionalDebugInfo("loginUser"));
			infoObject.put("role", getAdditionalDebugInfo("loginRole"));
			infoObject.put("authToken", getAdditionalDebugInfo("authToken"));
			infoObject.put("userAgent", getAdditionalDebugInfo("userAgent"));
			infoObject.put("clientIP", getAdditionalDebugInfo("clientIP"));
			infoObject.put("sessionID", getAdditionalDebugInfo("sessionID"));
			infoObject.put("timeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			infoObject.put("dialog", jDialog);
			infoObject.write(out);
			return out.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "No Dialog";
		}

	}

	public void clearDialogInfo() {
		jDialog = new JSONArray();

	}

	@XmlTransient
	public Stack<Task> getTaskStack() {
		return taskStack;
	}

	@XmlTransient
	public ArrayList<Entity> getHistory() {
		return entityHistory;
	}

	public String getSystemmessage() {
		return systemMessage;
	}

	// do not serialize
	public String getAdditionalDebugInfo(String key) {
		if (additionalDebugInfo.containsKey(key)) {
			return additionalDebugInfo.get(key);
		} else
			return "";
	}

	// setters

	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
		this.instance = String.valueOf(dialog.hashCode());
	}

	public void setStarted(Boolean started) {
		this.started = started;
	}

	public void setQuestionOpen(Boolean question_open) {
		this.question_open = question_open;
	}

	public void setCurrentQuestion(Entity question) {
		if (question != null) {
			setQuestionOpen(true);
			entityHistory.add(question);
		}
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	// public void setAdditionalDebugInfo(String additionalDebugInfo) {
	// this.additionalDebugInfo = additionalDebugInfo;
	// }

	public void setAdditionalDebugInfo(String key, String additionalDebugInfo) {
		this.additionalDebugInfo.put(key, additionalDebugInfo);
	}

	public void addUtteranceToHistory(String utterance, UTTERANCE_TYPE type, int level) {
		// create tree (hierarchical representation according to task level)
		// used as basis for output as HTML list
		if (level == current_node.getLevel()) {
			current_node.addChild(new HistoryTree(new HistoryElem(utterance, level, type), current_node, level));
		} else if (level > current_node.getLevel()) {
			HistoryTree child = new HistoryTree(current_node, level);
			HistoryTree leaf = new HistoryTree(new HistoryElem(utterance, level, type), child, level);
			child.addChild(leaf);
			current_node.addChild(child);
			current_node = child;
		} else {
			current_node = current_node.getParent();
			current_node.addChild(new HistoryTree(new HistoryElem(utterance, level, type), current_node, level));
		}
		// Added to send JSON object through REST API to server gateway
		try {
			JSONObject jUtterance = new JSONObject();
			jUtterance.put((type == UTTERANCE_TYPE.SYSTEM) ? "S" : "U", utterance);
			jDialog.put(jUtterance);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setEntityIterator(Iterator<Entity> entityIterator) {
		this.entityIterator = entityIterator;
	}

	public void setLocale(String langugae) {
		switch (langugae) {
		case "ar":
			dLocale = new Locale.Builder().setLanguage("ar").setRegion("AE").build();
			break;
		case "en":
			dLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
			break;
		case "hi":
			dLocale = new Locale.Builder().setLanguage("hi").setRegion("IN").build();
			break;
		case "da":
			dLocale = new Locale.Builder().setLanguage("da").setRegion("DK").build();
			break;
		case "nl":
			dLocale = new Locale.Builder().setLanguage("nl").setRegion("NL").build();
			break;
		case "sv":
			dLocale = new Locale.Builder().setLanguage("sv").setRegion("SE").build();
			break;
		case "es":
			dLocale = new Locale.Builder().setLanguage("es").setRegion("ES").build();
			break;
		default:
			dLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
			break;
		}

	}

	public Locale getLocale() {

		return dLocale;
	}

	// serialize

	public String serialize() {
		JAXBContext context;
		String result = "";
		try {
			context = JAXBContext.newInstance(DialogManagerContext.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			m.marshal(this, bout);
			result = bout.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void print() {
		System.out.println(serialize());
	}

}
