package cto.hmi.model.definition;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.dialogmodel.Task;

@XmlType(propOrder = { "name", "startTaskName", "globalLanguage", "useSODA", "allowSwitchTasks", "allowOverAnswering",
		"allowDifferentQuestion", "allowCorrection", "failureAttempts", "tasks" })
public abstract class DialogModel {

	public final static String VERSION = "1.0"; // version of the dialogue model, i.e. version of XSD
	private final static Logger logger = ConvEngineProcessor.getLogger();
	// serializable members
	protected String name; // ID
	protected String company;
	protected String version;
	protected String globalLanguage = "en"; // default value
	protected String startTaskName; // task that is used as entry point
	// indirectly specify dialogue strategy (directed, mixed,...):
	protected boolean allowSwitchTasks = true; // allow subdialogues
	protected boolean allowOverAnswering = true; // give more than the
													// information that has been
													// asked for (but at least
													// the current question)
	protected boolean allowDifferentQuestion = true; // ignore current question
														// and answer a
														// different unanswered
														// question
	protected boolean allowCorrection = true; // change a value of an already
												// asked question
	protected int failureAttempts = 2; // number of attempts that will be
										// allowed to user to answer specific
										// ITO

	protected boolean useSODA = true; // make use of dialogue acts
	protected ArrayList<Task> tasks; // every dialogue consists of one or
										// several tasks
	// Constructors
	public DialogModel() {
		tasks = new ArrayList<Task>();
	}

	public DialogModel(String name) {
		this();
		this.name = name;
	}

	// Serialization getter/setter
	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute(name = "company")
	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	@XmlAttribute(name = "version")
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getGlobalLanguage() {
		return globalLanguage;
	}

	public void setGlobalLanguage(String globalLanguage) {
		this.globalLanguage = globalLanguage;
	}

	public boolean isUseSODA() {
		return useSODA;
	}

	public void setUseSODA(boolean useSODA) {
		this.useSODA = useSODA;
	}

	public String getStartTaskName() {
		return startTaskName;
	}

	public void setStartTaskName(String startTaskName) {
		this.startTaskName = startTaskName;
	}

	public boolean isAllowSwitchTasks() {
		return allowSwitchTasks;
	}

	public void setAllowSwitchTasks(boolean switchTasks) {
		this.allowSwitchTasks = switchTasks;
	}

	public boolean isAllowOverAnswering() {
		return allowOverAnswering;
	}

	public void setAllowOverAnswering(boolean overAnswering) {
		this.allowOverAnswering = overAnswering;
	}

	public boolean isAllowDifferentQuestion() {
		return allowDifferentQuestion;
	}

	public void setAllowDifferentQuestion(boolean differentQuestion) {
		this.allowDifferentQuestion = differentQuestion;
	}

	public boolean isAllowCorrection() {
		return allowCorrection;
	}

	public void setAllowCorrection(boolean correction) {
		this.allowCorrection = correction;
	}

	public int getFailureAttempts() {
		return failureAttempts;
	}

	public void setFailureAttempts(int failureAttempts) {
		this.failureAttempts = failureAttempts;
	}

	@XmlElementWrapper(name = "tasks")
	@XmlElement(name = "task")
	public ArrayList<Task> getTasks() {
		return tasks;
	}

	public void setTasks(ArrayList<Task> tasks) {
		this.tasks = tasks;
	}

	// Helpers

	public void addTask(Task task) {
		this.tasks.add(task);
	}

	public void removeTask(String taskName) {
		Iterator<Task> tk = this.tasks.iterator();
		while (tk.hasNext()) {
			Task t = tk.next();
			if (t.getName().equals(taskName))
				tk.remove();
		}
	}

	public Task getTask(String name) {
		for (Task t : tasks) {
			if (t.getName().equals(name))
				return t;
		}
		return null;
	}

	public Task getStartTask() {
		for (Task t : tasks) {
			if (t.getName().equals(this.startTaskName))
				return t;
		}
		return null;
	}

	public Task getFirstTask() {
		return tasks.get(0);
	}

	// Serialization / Deserialization

	protected void save(OutputStream stream) {
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Dialog.class, DialogModel.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.setProperty("jaxb.schemaLocation", "http://cto.net/hmi schema1.xsd");
			m.marshal(this, stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveAs(String path, String filename) {
		try {
			URL p = (path.length() == 0) ? new URL("file", "", filename) : new URL(path + "/" + filename);
			save(new FileOutputStream(p.getPath()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toXML() {
		OutputStream stream = new ByteArrayOutputStream();
		save(stream);
		return stream.toString();
	}

	public static Dialog loadFromPath(String path) {
		JAXBContext context;
		Dialog d = null;
		try {
			logger.info("loading from dialogue from " + path);// addded now
			context = JAXBContext.newInstance(Dialog.class, DialogModel.class);
			Unmarshaller um = context.createUnmarshaller();
			String p = new URL(path).getPath();
			d = (Dialog) um.unmarshal(new java.io.FileInputStream(p));
			logger.info("loaded dialogue from " + path);
		} catch (Exception e) {
			logger.severe("error while loading the dialogue model. Please check the file's syntax and version!");
			e.printStackTrace();
		}
		return d;
	}

	public static Dialog loadFromXml(String xml) {
		JAXBContext context;
		Dialog d = null;
		try {
			context = JAXBContext.newInstance(Dialog.class, DialogModel.class);
			Unmarshaller um = context.createUnmarshaller();
			d = (Dialog) um.unmarshal(new StringReader(xml));
			logger.info("loaded dialogue from XML string (e.g. by user upload)");
		} catch (Exception e) {
			logger.severe("error while loading the dialogue model. Please check the file's syntax and version!");
			e.printStackTrace();
		}
		return d;
	}

	// Schema generation
	public void generateSchema() {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(Dialog.class, DialogModel.class);

			jc.generateSchema(new SchemaOutputResolver() {
				@Override
				public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
					StreamResult result = new StreamResult(new FileWriter(suggestedFileName));
					result.setSystemId(suggestedFileName);
					return result;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}