package cto.hmi.processor.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.javatuples.Quartet;
import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.bot.util.LogForActivity;
import cto.hmi.bot.util.LogForTraining;
import cto.hmi.bot.util.MaskData;
import cto.hmi.bot.util.ProgressBar;
import cto.hmi.bot.util.Transliteration;
import cto.hmi.broker.DialogObject;
import cto.hmi.broker.MessageListenerService;
import cto.hmi.broker.constants.Params;
import cto.hmi.broker.util.CheckBrokerStatus;
import cto.hmi.broker.util.LookForMessage;
import cto.hmi.broker.util.SendMessage;
import cto.hmi.broker.util.TopicManager;
import cto.hmi.corpus.GetDomainAnswer;
import cto.hmi.nlp.PredictModel;
import cto.hmi.nlp.ProcessClassifierResult;
import cto.hmi.nlp.ProcessMessage;
import cto.hmi.nlp.TrainModel;
import cto.hmi.processor.ConvEngineConfig;
import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.Action;
import cto.hmi.processor.dialogmodel.ActionResultMapping;
import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.dialogmodel.Entity;
import cto.hmi.processor.dialogmodel.Notification;
import cto.hmi.processor.dialogmodel.Task;
import cto.hmi.processor.event.EventManager;
import cto.hmi.processor.exceptions.ProcessingException;
import cto.hmi.processor.exceptions.RuntimeError;
import cto.hmi.processor.licensemanager.LicenseProcessor;
import cto.hmi.processor.manager.DialogManagerContext.UTTERANCE_TYPE;
import cto.hmi.processor.nlu.entityparser.ParseResults;
import cto.hmi.processor.nlu.entityparser.Parsers;
import cto.hmi.processor.nlu.soda.Soda;
import cto.hmi.processor.nlu.soda.classification.SodaRecognizer;
import cto.hmi.processor.ui.UIConsumer;
import cto.hmi.processor.ui.UIConsumer.UIConsumerMessage.Meta;
import cto.hmi.processor.utterance.UserUtterance;

public class DialogManager implements UIConsumer {

	private final static Logger logger = ConvEngineProcessor.getLogger();
	public Map<String, String> entityMapping = new HashMap<String, String>();

	public Triplet<String, String, String> botIntent = new Triplet<String, String, String>("", "", "");
	public Triplet<String, String, String> botActionIntent = new Triplet<String, String, String>("", "", "");
	public Quartet<String, String, String, String> botEntityInAction = new Quartet<String, String, String, String>("",
			"", "", "");
	public LinkedList<Quartet<String, String, String, String>> botEntities = new LinkedList<Quartet<String, String, String, String>>();
	public LinkedList<Quartet<String, String, String, String>> botContexts = new LinkedList<Quartet<String, String, String, String>>();
	public LinkedList<Quartet<String, String, String, String>> botActionEntities = new LinkedList<Quartet<String, String, String, String>>();
	private Task TasktoCancel = new Task();
	private SodaRecognizer sodarec = null;
	private LicenseProcessor licenseProcessor = new LicenseProcessor();
	private static ResourceBundle appMessages;
	private static boolean init = false;
	private boolean followup = false; // move to context?
	private DialogManagerContext context = null;
	private GetDomainAnswer getDomainAnswer;
	private boolean isStartAddedtoStack = false;
	private boolean isTaskExecuted = false;
	private boolean setFollowupFlag = false;
	private boolean isActionFlashed = false;
	private boolean isIntentLookupDone = false;
	private boolean isLicenseValid = false;
	private JSONObject jsonInfo = new JSONObject();
	private boolean USE_NLG = true;
	private boolean USE_GREETINGS = true;
	private boolean SHOW_INTERACTIVE_FORM = false;
	private boolean IGNORE_PREV_TASK = false;
	private boolean MASK_DATA = false;
	private boolean USE_BROKER = false;
	private int MAX_WAIT_TIME = 100; // in Second
	private Float IE_THRESHOLD_SCORE;
	private Float IE_SIMILARITY_INDEX;
	private boolean SHOW_FEEDBACK_ICONS = false;
	private int LIKE_COUNTS = 0;
	private int DISLIKE_COUNTS = 0;
	// global failure attempts
	private int FAILED_ATTEMPTS = 0;
	private int ALLOWED_FAILURE_ATTEMPTS = 5;
	// Entity FAILURES
	private int FAILED_ENTITY_ATTEMPTS = 0;
	private int AllOWED_ENTITY_FAILURE_ATTEMPTS = 2;
	private String CACHE_MESSAGE = "";
	// added to fetch the notification and next best conversations
	private String POLLING_UTTERANCE = "polling-query";
	private String cacheTask = "";
	private Stack<String> lastAccessedTaskStack = new Stack<String>();
	private ArrayList<Notification> notificationList = new ArrayList<Notification>();
	private Map<String, String> roles = new HashMap<String, String>();
	// brokerMessages will be pushed to this bucket as and when they arrive
	public static Map<String, ConsumerRecord<String, DialogObject>> brokerMessages = new HashMap<String, ConsumerRecord<String, DialogObject>>();
	// this flag toggles if intent clarification is required so as to avoid loop
	private HashSet<String> intentSimilarityCheck = new HashSet<String>();
	// entities identified by NER
	public LinkedList<Quartet<String, String, String, String>> NEREntities = new LinkedList<Quartet<String, String, String, String>>();

	private enum dialogState {
		TASK_SWITCH, ENTITY_FOUND, CONTEXT_FILL, ACTION_EXEC
	};

	public DialogManager() throws RuntimeError {
		this(ConvEngineProcessor.getDialog()); // load default dialogue
	}

	public DialogManager(Dialog dialog) throws RuntimeError {
		init();
		if (dialog != null)
			loadDialog(dialog);
		else
			throw new RuntimeError("the dialogue could not be loaded.");

		AllOWED_ENTITY_FAILURE_ATTEMPTS = dialog.getFailureAttempts();
		// set Locale
		switch (dialog.getGlobalLanguage()) {
		case "ar":
			// AE is for UAE
			logger.info("set locale to ar_AE");
			context.setLocale("ar");
			break;
		case "en":
			logger.info("set locale to en_US");
			context.setLocale("en");
			break;
		case "hi":
			logger.info("set locale to hi_IN");
			context.setLocale("hi");
			break;
		case "da":
			logger.info("set locale to da_DK");
			context.setLocale("da");
			break;
		case "nl":
			logger.info("set locale to nl-NL");
			context.setLocale("nl");
			break;
		case "sv":
			logger.info("set locale to sv-SE");
			context.setLocale("sv");
			break;
		case "es":
			logger.info("set locale to es-ES");
			context.setLocale("es");
			break;
		default:
			context.setLocale("en");
			break;
		}
		String resourcesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.RESOURCEBUNDLEFOLDER)
				.substring(8);
		// loading resource bundle
		try {
			File file = new File(resourcesFile);
			URL[] urls = { file.toURI().toURL() };
			ClassLoader loader = new URLClassLoader(urls);

			appMessages = ResourceBundle.getBundle("appMessages", context.getLocale(), loader);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		context.setAdditionalDebugInfo("language", context.getLocale().getLanguage());
		// check if the license is valid
		if (dialog.getCompany() != null) {
			if (licenseProcessor.validate(dialog.getCompany())) {
				isLicenseValid = true;
				logger.info(
						"LICENSE: successfully verified. License issued to -> " + dialog.getCompany().toUpperCase());
				if (licenseProcessor.aboutToExpire())
					logger.severe("***IMPORTANT: LICENSE about to expire, please contact IT admin");

			} else {
				logger.severe("LICENSE: verification unsuccesful");
			}
		}

	}

	public DialogManagerContext getContext() {
		return context;
	}

	private void init() {

		getDomainAnswer = new GetDomainAnswer();
		context = new DialogManagerContext();
		context.setCreatedOn(new Date());
		sodarec = SodaRecognizer.getInstance();
		Properties prop = new Properties();
		String propertiesFile = ConvEngineConfig.getInstance().getProperty(ConvEngineConfig.CONFIGFILE).substring(8);

		try {
			// load all the properties
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);

			USE_NLG = Boolean.parseBoolean(loadProerty(prop, "USE_NLG").toLowerCase());
			USE_GREETINGS = Boolean.parseBoolean(loadProerty(prop, "USE_GREETINGS").toLowerCase());
			SHOW_INTERACTIVE_FORM = Boolean.parseBoolean(loadProerty(prop, "SHOW_INTERACTIVE_FORM").toLowerCase());
			ALLOWED_FAILURE_ATTEMPTS = Integer.parseInt(loadProerty(prop, "ALLOWED_FAILURE_ATTEMPTS"));
			IGNORE_PREV_TASK = Boolean.parseBoolean(loadProerty(prop, "IGNORE_PREV_TASK").toLowerCase());
			MASK_DATA = Boolean.parseBoolean(loadProerty(prop, "MASK_DATA").toLowerCase());
			USE_BROKER = Boolean.parseBoolean(loadProerty(prop, "USE_BROKER").toLowerCase());
			// check if it is set through command line option
			if (ConvEngineProcessor.getUseBrokerFlag())
				USE_BROKER = true;
			MAX_WAIT_TIME = Integer.parseInt(loadProerty(prop, "MAX_WAIT_TIME"));
			IE_THRESHOLD_SCORE = Float.parseFloat(loadProerty(prop, "IE_THRESHOLD_SCORE"));
			IE_SIMILARITY_INDEX = Float.parseFloat(loadProerty(prop, "IE_SIMILARITY_INDEX"));

			// load all the roles from roles.properties
			roles = DialogManagerHelper.findRoleHierarchy();
			input.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.severe("Failed to initialise Dialog Engine, missing bot properties");
			e.printStackTrace();
		}

		if (!init) {
			try {
				// train the soda
				if (!sodarec.isTrained())
					sodarec.train();
				Parsers.init();
				// check the broker topics
				if (USE_BROKER) {
					logger.info(" [BROKER] checking if the topics are existing or needs to be created");
					TopicManager.checkTopics();
				}
				// train the model
				if (!USE_BROKER) {
					DialogObject d0 = new DialogObject();
					logger.info(" [NLP_ENGINE] Important: wait while NLP Engine is training the model...");
					d0 = TrainModel.train(ConvEngineProcessor.getDialog().getName(), context.getLocale().getLanguage());
					ProcessMessage.process("d0-DUMMY", d0);

				} else {
					// send the event and wait till you consume it
					if (!CheckBrokerStatus.getStatus())
						logger.severe("[BROKER] failed to connect to broker server");
					// start message listener service for
					MessageListenerService.start((String) Params.getParam("TOPIC_NLP_TO_BOT"));
					// create the dialog message
					DialogObject co = new DialogObject("TRAIN", ConvEngineProcessor.getDialog().getName(),
							context.getLocale().getLanguage());
					// send message for training with key as d0-DUMMY
					if (!SendMessage.send((String) Params.getParam("TOPIC_BOT_TO_NLP"), "d0-DUMMY", co))
						logger.severe("[BROKER] error while sending message");
					// starting progress bar as training may take time
					ProgressBar.startPB(MAX_WAIT_TIME + 10, "wait while NLP Engine is training the model..");
					// look for messages that or separated
					if (LookForMessage.look(MAX_WAIT_TIME, "d0-DUMMY", "TRAIN_SUCCESS|TRAIN_FAIL")) {
						ProcessMessage.process(brokerMessages.get("d0-DUMMY").key(),
								brokerMessages.get("d0-DUMMY").value());
					} else {
						logger.severe("[BROKER] error while processing message");
					}
					ProgressBar.stopPB();
				}

				init = true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.severe("Failed to initialise Dialog Engine");
				e.printStackTrace();
			}
		}
	}

	// UIConsumer:
	@Override
	public void loadDialog(Dialog dialog) {
		context.setDialog(dialog);
	}

	@Override
	public String getDebugInfo() {
		return getContext().serialize();
	}

	@Override
	public String getDebugInfo(String key) {
		return getContext().getAdditionalDebugInfo(key);
	}

	@Override
	public String getDialogXml() {
		return context.getDialog().toXML();
	}

	@Override
	public String getDialogInfo() {
		String result = "";
		result = getContext().getDialogInfo();
		return result;
	}

	@Override
	public void clearDialogInfo() {
		getContext().clearDialogInfo();
	}

	@Override
	public String getIdentifier() {
		return context.getInstance();
	}

	@Override
	public void setAdditionalDebugInfo(String key, String debugInfo) {
		context.setAdditionalDebugInfo(key, debugInfo);
	}

	@Override
	public Date getLastAccess() {
		return context.getLastAccess();
	}

	// added where entity value is being modified by GROOVY action
	public void setEntityMapping(String entity, String value) {
		if (entityMapping.containsKey(entity))
			entityMapping.put(entity, value);
	}

	// added where entity value is being modified by GROOVY action - this is first
	// read
	public Map<String, String> getEntityMapping() {
		return entityMapping;
	}

	// TODO experimental
	@Override
	public UIConsumerMessage processUtterance(String userUtterance) throws ProcessingException {
		try {
			// added to support handover
			if (FAILED_ATTEMPTS >= ALLOWED_FAILURE_ATTEMPTS) {
				FAILED_ATTEMPTS = 0;
				ArrayList<Task> tsklist = context.getDialog().getTasks();
				for (Task task : tsklist) {
					if (task.getName().equals("handoverTask")) {
						logger.info("switching to handover task as number of failed attempts are more than "
								+ FAILED_ATTEMPTS);
						userUtterance = "#handoverTask";
						break;
					}
				}
			}
			if (userUtterance != null) {
				userUtterance = userUtterance.replaceAll("[\\t\\n\\r]", "");
				// replace unicode | in Devanagri
				userUtterance = userUtterance.replaceAll("\u0964", "");
				userUtterance = userUtterance.trim();
			}
			// touch last access only if it is real utterance
			if (!userUtterance.toLowerCase().equals(POLLING_UTTERANCE))
				context.setLastAccess(new Date());
			// ParseResults results=null;
			UserUtterance answer = null;
			Boolean found = false;
			String IE_Message = "";
			String aiml_message = "";
			String domain_message = "";

			// STEP 1:
			// 1a) INITIALIZE THE DIALOGUE
			if (!context.isStarted()) {
				logger.info("init dialogue");
				context.setStarted(true);
				TasktoCancel.setName("none");
				// get start task and its associated entities
				Task t = context.getDialog().getStartTask();

				if (t == null) {
					// if no start task defined just take the first one
					t = context.getDialog().getFirstTask();
				}
				// create the default entities for storing information
				t = createdefaultEntities(t);

				return initTaskAndGetNextQuestion(t);
			}
			// or 1b) DO NOTHING: if there is no user utterance or utterance
			// with junk character and the dialogue has
			// already been initialized, nothing will/should happen:
			else if (userUtterance == null || userUtterance.length() == 0) {
				logger.info("no utterance, repeating last question");
				// return new UIConsumerMessage("", Meta.UNCHANGED);
				// check if the entity has clarify question
				String message = "";
				if (context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion().length() > 0)
					message = context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion();
				else
					message = context.getHistory().get(context.getHistory().size() - 1).getFallbackQuestion();
				return new UIConsumerMessage(DialogManagerHelper.fillfbqWithEntity(message, entityMapping),
						Meta.REPEATEDQUESTION);
			}
			// if user is trying non alphanumeric character ignore it for en
			else if (context.getLocale().getLanguage().equals("en")
					&& !(userUtterance.replaceAll("\\s+", "").matches(".*[a-zA-Z0-9]+.*"))) {
				logger.info("no meaningful utterance, repeating last question");
				String message = "";
				if (context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion().length() > 0)
					message = context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion();
				else
					message = context.getHistory().get(context.getHistory().size() - 1).getFallbackQuestion();
				return new UIConsumerMessage(DialogManagerHelper.fillfbqWithEntity(message, entityMapping),
						Meta.REPEATEDQUESTION);
			}
			// or 1bb) IF SPACE ENTERED, THEN JUST REPEAT LAST QUESTION
			// (not much of a difference to 1b; no change in dialogue context;
			// however this may be required by a client to (re)display the last
			// system utterance)
			else if (context.getCurrentTask() != null && userUtterance.equals(" ")) {
				logger.info("recognized space, repeating last question");

				return new UIConsumerMessage(DialogManagerHelper.fillfbqWithEntity(
						context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion(), entityMapping),
						Meta.REPEATEDQUESTION);
			}
			// added to support polling..this will ensure notification to pass
			// through to client
			else if (context.getCurrentTask() != null && userUtterance.toLowerCase().equals(POLLING_UTTERANCE)) {
				logger.info("got polling request, ignoring");
				String message = "";
				if (context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion().length() > 0)
					message = context.getHistory().get(context.getHistory().size() - 1).getClarifyQuestion();
				else
					message = context.getHistory().get(context.getHistory().size() - 1).getFallbackQuestion();
				return new UIConsumerMessage(DialogManagerHelper.fillfbqWithEntity(message, entityMapping),
						Meta.POLLING);
			}
			// or 1c) PROCESS USER UTTERANCE
			else if (context.getCurrentTask() != null) {
				context.addUtteranceToHistory(MaskData.mask(userUtterance, MASK_DATA), UTTERANCE_TYPE.USER,
						context.getTaskStack().size());
				answer = new UserUtterance(userUtterance);
				// identify dialog act (sets features and soda by reference),
				// access result: answer.getSoda()
				sodarec.predict(answer, context);
				// context.setQuestionOpen(false); //needs to be set AFTER
				// dialogue act recognition!
				// if the current entity is of open text then we need to set to
				// INFORMATION_PROVIDING
				// to avoid it switching the context
				if (botEntityInAction.getValue2().equals("sys.opentext")) {
					logger.info("SODA Postprocessing: Entity is sys.opentext, setting SODA -> prov");
					answer.setSoda(Soda.INFORMATION_PROVIDING);
				}

				// Parse:
				if ((context.getDialog().isUseSODA() && answer.getSoda().equals(Soda.INFORMATION_PROVIDING))
						|| (!context.getDialog().isUseSODA())) {

					found = lookForAnswers(context.getCurrentQuestion(), context.getCurrentTask().getEntities(),
							answer);

					// SODA Postprocessing:
					if (answer.getSoda().equals(Soda.INFORMATION_PROVIDING) && (!found || !context.isQuestionOpen())) {
						answer.setSoda(Soda.INFORMATION_SEEKING);
						logger.info("SODA Postprocessing: prov -> seek");
					}
				}

				// needs to be set AFTER dialogue act recognition!
				context.setQuestionOpen(false);

				// STEP 2:
				// 2a) IF PARSING SUCCESSFUL, SAVE RESULTS
				// if(results!=null && results.getState()==ParseResults.MATCH){
				if (found) {
					logger.info("interpretation was successful in task: " + context.getCurrentTask().getName());
					// storeResults(context.getCurrentQuestion(), results);

					// follow-up
					if (followup) {
						logger.info("processing followup");
						followup = false;
						// TODO might be better to make a FollowUpentity and check
						// type on the fly in process_utterance()

						// added to handle null pointer exception when user
						// changed the task while the followup question was
						// asked to user
						if (context.getCurrentTask().getFollowup() != null) {
							HashMap<String, String> mapping = context.getCurrentTask().getFollowup().getAnswerMapping();
							String taskToStart = null;
							// added to handle null value
							if (context.getCurrentTask().getFollowup().getEntity().getValue() != null)
								taskToStart = mapping.get(context.getCurrentTask().getFollowup().getEntity().getValue()
										.toString().toUpperCase());
							if (taskToStart != null) {
								// added to support the event task
								if (taskToStart.startsWith("@")) {
									String eName = taskToStart.trim().substring(1, taskToStart.trim().length());
									EventManager eventManager = new EventManager(eName);
									ArrayList<Task> taskArray = new ArrayList<Task>();
									if (eventManager.processEvent() != null)
										taskArray = eventManager.processEvent();
									if (taskArray.size() > 0) {
										// clean up earlier event task that are
										// in task stack this is rare as stack
										// is cleared as and when task are
										// executed
										Iterator<Task> tkStack = context.getDialog().getTasks().iterator();
										while (tkStack.hasNext()) {
											Task t = tkStack.next();
											if (t.getName().startsWith("EVT_")) {
												logger.info("removed prior event triggered task from task stack..:"
														+ t.getName());
												tkStack.remove();
											}
										}
									}
									for (int i = 0; i < taskArray.size(); i++) {
										logger.info("added event triggered task..:" + taskArray.get(i).getName());
										context.getDialog().addTask(taskArray.get(i));

									}
									taskToStart = taskArray.get(0).getName();
								}
								// Added to support cancelTask functionality &
								// is not start
								if (!TasktoCancel.getName().equals("none") && !TasktoCancel.getName().equals("start")) {
									logger.info("..removing task: " + TasktoCancel.getName());
									context.getTaskStack().pop().reset();
									// remove the task from stack
									if (TasktoCancel.getName().equals("start")) {
										logger.info(
												"resetting all Global Entities and context to null as request is from "
														+ TasktoCancel.getName());
										Iterator<Task> tk = context.getTaskStack().iterator();
										while (tk.hasNext()) {
											Task t = tk.next();
											// reset all entities to null and set
											// them
											// unfilled
											Iterator<Entity> it = t.getEntities().iterator();
											while (it.hasNext()) {
												Entity entity = it.next();
												// set all to null except the
												// loginUser, sessionId,
												// lastAccessedEntity and
												// lastAccessedTask

												if (!(entity.getName().equals("loginUser_")
														|| entity.getName().equals("loginRole_")
														|| entity.getName().equals("authToken_")
														|| entity.getName().equals("sessionId_")
														|| entity.getName().equals("lastAccessedEntity_")
														|| entity.getName().equals("lastAccessedTask_"))) {
													entity.setValue(null);
													entity.setUnFilled();
												}
												// IMPOTANT -DO not try removing
												// global
												// variable..it does not work
												// somehow
												// if
												// (entity.getName().endsWith("_"))
												// it.remove();
											}
										}
										// clear all context entities to null except
										// the global ones
										Set<String> keys = entityMapping.keySet();
										for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
											String key = iterator.next();
											if (!(key.equals("loginUser_") || key.equals("loginRole_")
													|| key.equals("authRole_") || key.equals("sessionId_")
													|| key.equals("lastAccessedEntity_")
													|| key.equals("lastAccessedTask_"))) {
												iterator.remove();
											}
										}
									}
									TasktoCancel.setName("none");
								}
								return abortAndStartNewTask(taskToStart, null);
							} else
								return getNextQuestion();
						}
					}
					// //TODO: multiple information in one answer (mixed
					// initiative)
					// //...
					// //beta
					// if(context.getDialog().isAllowOverAnswering()){
					// String processedAnswer=answer.getText();
					// processedAnswer=processedAnswer.replace(results.getFirst().getMatchedSequence(),"");
					// lookForAnswers(context.getCurrentTask().getEntities(), new
					// UserUtterance(processedAnswer));
					// }
					// //...

					if (!context.getCurrentQuestion().isFilled()) {
						logger.info("(detected and realised a correction or answer for a different question)");
						// String message = "OK, I got that. "; // acknowledge
						// that different question has been filled
						String message = appMessages.getString("GOT_MSG");

						String question = context.getLocale().getLanguage().equals("ar")
								? DialogManagerHelper.fillfbqWithEntity(
										context.getCurrentQuestion().getFallbackQuestion(), entityMapping) + message
								: message + DialogManagerHelper.fillfbqWithEntity(
										context.getCurrentQuestion().getFallbackQuestion(), entityMapping);
						context.addUtteranceToHistory(question, UTTERANCE_TYPE.SYSTEM, context.getTaskStack().size());
						context.setQuestionOpen(true);
						return new UIConsumerMessage(question, Meta.QUESTION);
					}
				}
				// or 2b) IF PARSING NOT SUCCESSFUL, CHECK FOR OTHER
				// POSSIBILITIES

				else if (!found) {
					logger.info("could not be interpreted in current task..: " + context.getCurrentTask().getName());
					String message = null;
					boolean found_different_task = false;

					// 2b2) if not successful, check for OTHER tasks
					if ((context.getDialog().isUseSODA() && (answer.getSoda().equals(Soda.INFORMATION_SEEKING)
							|| answer.getSoda().equals(Soda.ACTION_REQUESTING))) || !context.getDialog().isUseSODA()) {
						// allow task switch if it is start or flag is set or
						// utterance
						// begins with # or @ (as we allow task switch with
						// # as
						// it is deliberate)
						if ((context.getDialog().isAllowSwitchTasks()
								|| context.getCurrentTask().getName().equals("start")) || userUtterance.startsWith("#")
								|| userUtterance.startsWith("@")) {

							HashMap<String, String> IEResult = new HashMap<String, String>();
							String iePredictedTask = "NA";
							Boolean tskExist = false;
							// set task if it is coming through interactive mode
							// i.e. utterance starting with #
							if (userUtterance.startsWith("#")) {
								// identify the task
								String tName = userUtterance.trim().substring(1, userUtterance.trim().length());
								tskExist = false;
								ArrayList<Task> tsklist = context.getDialog().getTasks();
								for (Task task : tsklist) {
									if (task.getName().equals(tName)) {
										tskExist = true;
										iePredictedTask = tName;
										break;
									}
								}
							}
							// event based dialogue
							if (userUtterance.startsWith("@")) {
								String eName = userUtterance.trim().substring(1, userUtterance.trim().length());
								EventManager eventManager = new EventManager(eName);
								ArrayList<Task> taskArray = new ArrayList<Task>();
								if (eventManager.processEvent() != null)
									taskArray = eventManager.processEvent();
								if (taskArray.size() > 0) {
									// clean up earlier event task that are in
									// task stack this is rare as
									// stack is cleared as and when task are
									// executed
									Iterator<Task> tkStack = context.getDialog().getTasks().iterator();
									while (tkStack.hasNext()) {
										Task t = tkStack.next();
										if (t.getName().startsWith("EVT_")) {
											logger.info("removed prior event triggered task from task stack...:"
													+ t.getName());
											tkStack.remove();
										}
									}
								}
								for (int i = 0; i < taskArray.size(); i++) {
									logger.info("added event triggered task...:" + taskArray.get(i).getName());
									context.getDialog().addTask(taskArray.get(i));

								}
								// while all tasks are loaded we will start with
								// first task in event.
								if (taskArray.size() > 0) {
									tskExist = true;
									iePredictedTask = taskArray.get(0).getName();
								}
							}
							// process utterance through intent engine if set
							// True
							// provided it is not set through interactive mode
							if (!isIntentLookupDone && iePredictedTask.equals("NA")) {
								IEResult = classifyIntent(context.getDialog().getName(),
										context.getLocale().getLanguage(), userUtterance);
								// added to support role based access to task
								if (IEResult.containsKey("INTENT_FOUND")) {
									String tskName = IEResult.get("INTENT_FOUND");
									// check if user's role is eligible to
									// execute the task
									ArrayList<Task> tsklist = context.getDialog().getTasks();
									for (Task task : tsklist) {
										if (task.getName().equals(tskName) && !task.getRole().isEmpty()
												&& roles.get(task.getRole().toLowerCase()) != null) {
											// compare current role with the
											// allocated role
											try {
												if (!(Integer.parseInt(roles
														.get(this.getDebugInfo("loginRole").toLowerCase())) < Integer
																.parseInt(roles.get(task.getRole().toLowerCase())))) {
													IEResult.remove("INTENT_FOUND");
													IEResult.put("INTENT_ACCESS_DENIED", tskName);
												}
											} catch (NumberFormatException | NullPointerException e) {
												// TODO Auto-generated catch
												// block
												logger.info("check role assigned to user and/or task");
												e.printStackTrace();
											}
										}
									}
								}
								if (IEResult.containsKey("INTENT_FOUND")) {
									String tskName = IEResult.get("INTENT_FOUND");
									tskExist = false;
									ArrayList<Task> tsklist = context.getDialog().getTasks();
									for (Task task : tsklist) {
										if (task.getName().equals(tskName)) {
											tskExist = true;
										}
									}

									if (!tskExist) {

										logger.warning("task identified in intent engine is :" + tskName
												+ " which is missing in dialog definition file.");
										tskName = "NA";
									}

									iePredictedTask = tskName;
								} else if (IEResult.containsKey("INTENT_NOT_FOUND")) {
									iePredictedTask = IEResult.get("INTENT_NOT_FOUND");
									logger.info("intent could not be found in trained model");
									// check if utterance is of generic or one
									// that can be addressed by domain
									aiml_message = "AIML_NO_ANSWER";
									domain_message = "";
									domain_message = getDomainAnswer.get(userUtterance).trim();
									if (USE_GREETINGS)
										// let us try if chat AIML or domain has
										// any answer
										aiml_message = ConvEngineProcessor.aimlProcessor.process(userUtterance).trim();
									// check if DDF is having task "getFAQ" so
									// that we can pass that task provided it is
									// not generic
									if ((aiml_message.contains("AIML_NO_ANSWER") || aiml_message.endsWith("?"))
											&& domain_message.matches("DOMAIN_NO_ANSWER")) {

										Boolean faqTaskExist = false;
										ArrayList<Task> tsklist = context.getDialog().getTasks();
										for (Task task : tsklist) {
											if (task.getName().equals("getFAQ")) {
												faqTaskExist = true;
											}
										}
										if (faqTaskExist) {
											iePredictedTask = "getFAQ";
											logger.info("checking if getFAQ can answer this");
										}
									}

								} else if (IEResult.containsKey("INTENT_CLARIFICATION")) {
									String[] parts = IEResult.get("INTENT_CLARIFICATION").split("\\|");
									if (parts.length == 3)
										IE_Message = context.getLocale().getLanguage().equals("ar")
												? "\'... " + parts[2] + " \'" + appMessages.getString("OR_MSG") + "\' "
														+ parts[1] + " \'" + appMessages.getString("OR_MSG") + "\' "
														+ parts[0] + " \'" + appMessages.getString("CONFIRM_MSG")
												: appMessages.getString("CONFIRM_MSG") + " \'" + parts[0] + "\' "
														+ appMessages.getString("OR_MSG") + " \'" + parts[1] + "\' "
														+ appMessages.getString("OR_MSG") + " \'" + parts[2] + "\'... ";
									else
										IE_Message = context.getLocale().getLanguage().equals("ar")
												? "\'... " + parts[1] + " \'" + appMessages.getString("OR_MSG") + "\' "
														+ parts[0] + " \'" + appMessages.getString("CONFIRM_MSG")
												: appMessages.getString("CONFIRM_MSG") + " \'" + parts[0] + "\' "
														+ appMessages.getString("OR_MSG") + " \'" + parts[1] + "\'... ";
									logger.info("intent clarification required from user");

								} else if (IEResult.containsKey("INTENT_ACCESS_DENIED")) {
									IE_Message = appMessages.getString("ACCESS_DENIED_MSG");
									logger.info("current role does not allow the execution of task:"
											+ IEResult.get("INTENT_ACCESS_DENIED"));
								} else if (IEResult.containsKey("IE_ERROR")) {
									IE_Message = IEResult.get("IE_ERROR");
									logger.severe(IE_Message);
								}
							}
							ArrayList<Task> tasklist = context.getDialog().getTasks();
							for (Task tsk : tasklist) {
								// logger.info("Checking if utterance is part of
								// task: "+
								// tsk.getName());

								if (tsk == context.getCurrentTask())
									continue; // except this task

								if (!iePredictedTask.equals("NA")) {
									tsk = context.getDialog().getTask(iePredictedTask);
									// prevent follow-up-stacking! if the
									// current question is a follow-up question
									// and the user wants to start
									// another task, remove it from stack, also
									// if it has
									// not been answered, i.e. if you ignore a
									// follow-up question, it will not be asked
									// again

									// this line have been commented for ola use
									// case tasks with only followups
									/*
									 * if (followup) { followup = false; context.getTaskStack().pop().reset(); //
									 * remove // current // task // from // stack // and // reset }
									 */

									found_different_task = true;
									logger.info("responsible task is: " + tsk.getName());
									// set the task in likeDislikeObject

									if (!(tsk.getName().equals("start") || tsk.getName().equals("cancelTask")
											|| tsk.getName().equals("handoverTask")
											|| tsk.getName().equals("helpTask"))) {
										DialogManagerHelper.likeDislikeObject.setTask(tsk.getName());
										DialogManagerHelper.likeDislikeObject
												.setUserUtterance(MaskData.mask(userUtterance, MASK_DATA));
									}

									// check if task is already on stack (anti
									// recursion!) i.e. if user goes back (if he
									// does not call a new subdialog but instead
									// calls a previous (existing) dialog, i.e.
									// goes
									// back in history), destroy until desired
									// task
									// is active again
									if (context.getTaskStack().contains(tsk)) {
										Task poppedTask;
										while ((poppedTask = context.getTaskStack().pop()) != tsk) {
											// pop and reset until selected task
											// is
											// active
											logger.info("destroying task- " + poppedTask.getName());
											poppedTask.reset();
										}
									}

									// check if user wants to switch to cancel
									// task
									// and is outside start
									// if so set its name so that it is not none
									// so that in followup if user says
									// YES it is detected in switch task

									if (tsk.getName().matches("cancelTask") || tsk.getName().matches("handoverTask")
											|| tsk.getName().matches("helpTask")) {

										// get the task name to be cancelled
										// into
										// TasktoCancel
										TasktoCancel.setName(context.getCurrentTask().getName());
										logger.info("stored task " + context.getCurrentTask().getName()
												+ " to context as " + tsk.getName() + "  is called");
									}
									// check this task-switch-request for
									// further
									// information
									switchTask(tsk);

									if (context.getDialog().isAllowOverAnswering()) {
										logger.info(
												"check task-switch-request for more information (over answering active)");
										lookForAnswers(tsk.getEntities(), answer);
									}
									break;
								}
							}

						}
					}

					// 2b3) or repeat question (if directed dialogue or
					// alternatives
					// not successful):
					if (!found_different_task) {
						logger.info("could not be interpreted in any task. Try again...");
						// setting up the IE flag again for new search
						isIntentLookupDone = false;

						if (message == null) {
							// need to clear the botAction by sending current
							// task
							processBotResponse(
									Triplet.with(context.getCurrentTask().getName(),
											context.getCurrentTask().getLabel(), context.getCurrentTask().getRole()),
									null, null, dialogState.TASK_SWITCH);
							aiml_message = "AIML_NO_ANSWER";
							if (USE_GREETINGS)
								// let us try if chat AIML or domain has any
								// answer
								aiml_message = ConvEngineProcessor.aimlProcessor.process(userUtterance);
							// if it is retrieved earlier then no need again
							if (domain_message.matches(""))
								domain_message = getDomainAnswer.get(userUtterance);
							// to avoid repeated questions
							if ((aiml_message.contains("AIML_NO_ANSWER"))
									&& domain_message.matches("DOMAIN_NO_ANSWER")) {

								if (IE_Message.equals("")) {
									message = appMessages.getString("SORRY_MSG");
									FAILED_ATTEMPTS++;
									FAILED_ENTITY_ATTEMPTS++;
								} else {
									message = IE_Message;
									IE_Message = "";
								}
								String fbq = DialogManagerHelper.fillfbqWithEntity(
										context.getCurrentQuestion().getFallbackQuestion(), entityMapping);
								String toTrainUtterance = "S: " + MaskData.mask(fbq, MASK_DATA) + " U: "
										+ MaskData.mask(userUtterance, MASK_DATA);
								// log for additional training
								LogForTraining.logData(ConvEngineProcessor.getDefaultDialogPathAndName(),
										this.getDebugInfo("loginUser") + " sessionId:" + this.getDebugInfo("sessionID")
												+ " clientIP:" + this.getDebugInfo("clientIP"),
										context.getCurrentTask().getName(), toTrainUtterance, "FAILURE");

							} else if (!domain_message.matches("DOMAIN_NO_ANSWER")) {
								logger.info("adding the answer as recieved from domain URL- " + domain_message);
								message = domain_message + " ";
								// add like object
								DialogManagerHelper.likeDislikeObject.setTask("DOMAIN_FAQ");
								DialogManagerHelper.likeDislikeObject
										.setUserUtterance(MaskData.mask(userUtterance, MASK_DATA));
								DialogManagerHelper.likeDislikeObject
										.setSystemUtterance(MaskData.mask(message, MASK_DATA));
								SHOW_FEEDBACK_ICONS = true;
							} else if (!aiml_message.contains("AIML_NO_ANSWER"))
								message = aiml_message + " ";

						}
						// added to invoke cancel task if FAILURE_ENT_ATTEMPTS
						// are more than 2
						if (FAILED_ENTITY_ATTEMPTS >= AllOWED_ENTITY_FAILURE_ATTEMPTS
								&& !context.getCurrentTask().getName().equals("start")) {
							UIConsumerMessage answer_msg = null;
							TasktoCancel.setName(context.getCurrentTask().getName());
							logger.info("switching to help task as number of failed Entity attempts are more than "
									+ FAILED_ENTITY_ATTEMPTS);
							FAILED_ENTITY_ATTEMPTS = 0;
							Task helpTask = context.getDialog().getTask("helpTask");
							if (helpTask != null) {
								// make the message null before switching to
								// avoid SORRY_MSG appearing again
								message = "";
								switchTask(helpTask);
							}
							getNextQuestion(answer_msg);
						}
						// nothing could be interpreted
						String reply = "";

						if (!context.getCurrentQuestion().getClarifyQuestion().isEmpty()) {
							// just respond with the clarifyQestion. No need to
							// check "ar" as it is only one string
							reply = context.getLocale().getLanguage().equals("ar")
									? DialogManagerHelper.fillfbqWithEntity(
											context.getCurrentQuestion().getClarifyQuestion(), entityMapping) + " "
											+ message
									: message + " " + DialogManagerHelper.fillfbqWithEntity(
											context.getCurrentQuestion().getClarifyQuestion(), entityMapping);
						} else {
							reply = context.getLocale().getLanguage().equals("ar")
									? DialogManagerHelper.fillfbqWithEntity(
											context.getCurrentQuestion().getFallbackQuestion(), entityMapping) + " "
											+ message
									: message + " " + DialogManagerHelper.fillfbqWithEntity(
											context.getCurrentQuestion().getFallbackQuestion(), entityMapping);
						}

						context.addUtteranceToHistory(reply, UTTERANCE_TYPE.SYSTEM, context.getTaskStack().size());
						context.setQuestionOpen(true);
						return new UIConsumerMessage(reply, Meta.QUESTION);
					}
				} // -- endif 2b (parsing unsuccessful)

				// STEP 3:
				// 3a) IF ALL REQUIRED INFORMATION RETRIEVED, EXECUTE ACTION
				UIConsumerMessage answer_msg = null;
				Action action = context.getCurrentTask().getAction();

				if (context.getCurrentTask().isMandatoryFilled() && (action != null)) {
					// log the executing task to lastAccessedTask_ entity
					addToLastAccessedTaskEntity();
					logger.info("frame filled, executing action");
					// logging to botResponse
					processBotResponse(Triplet.with(context.getCurrentTask().getName(),
							context.getCurrentTask().getLabel(), context.getCurrentTask().getRole()), null, null,
							dialogState.ACTION_EXEC);
					// logging to /log/activity before executing the task
					String actionName = context.getCurrentTask().getAction().toString();
					LogForActivity.logData(ConvEngineProcessor.getDefaultDialogPathAndName(),
							this.getDebugInfo("loginUser") + " sessionId:" + this.getDebugInfo("sessionID")
									+ " clientIP:" + this.getDebugInfo("clientIP"),
							context.getCurrentTask().getName(), actionName.substring(actionName.lastIndexOf(".") + 1));
					String sysAns = processExecutionResult(context.getCurrentTask().execute());

					// check if the execution has failed
					if (sysAns.contains("FAIL_MSG")) {
						sysAns = appMessages.getString("FAIL_MSG");
					}

					// store the system answer to like , dislike object
					if (!(context.getCurrentTask().getName().equals("start")
							|| context.getCurrentTask().getName().equals("cancelTask")
							|| context.getCurrentTask().getName().equals("handoverTask")
							|| context.getCurrentTask().getName().equals("helpTask"))) {
						DialogManagerHelper.likeDislikeObject.setSystemUtterance(MaskData.mask(sysAns, MASK_DATA));
						SHOW_FEEDBACK_ICONS = true;
					}

					// added to return answer in the dialog locale (to check)
					switch (context.getLocale().getLanguage()) {
					case "ar":
						sysAns = Transliteration.EnglishToArabic(sysAns);
						break;
					case "en":
					case "da":
					case "nl":
					case "sv":
					case "es":
						break;
					case "hi":
						sysAns = Transliteration.EnglishToDevnagari(sysAns);
						break;
					default:
						break;
					}
					isTaskExecuted = true;
					// now set the followup flag for its action
					if (context.getCurrentTask().getFollowup() != null)
						setFollowupFlag = true;
					if (action.isReturnAnswer()) {
						// execute will update the msg object of dialogue
						// manager so
						// just get the message object alone
						answer_msg = new UIConsumerMessage(sysAns, Meta.ANSWER);

						// the answer is integrated into the next question
					}
					// TODO beta: task redirection depending on action result
					ActionResultMapping resultMapping;
					if ((resultMapping = action.getFirstMatchingResultMapping()) != null) {
						if (resultMapping.getRedirectToTask() != null
								&& resultMapping.getRedirectToTask().length() > 0) {
							String redirectTask = resultMapping.getRedirectToTask();
							// added to support the event task

							if (redirectTask.startsWith("@")) {
								String eName = redirectTask.trim().substring(1, redirectTask.trim().length());
								EventManager eventManager = new EventManager(eName);
								ArrayList<Task> taskArray = new ArrayList<Task>();
								if (eventManager.processEvent() != null)
									taskArray = eventManager.processEvent();
								if (taskArray.size() > 0) { // clean up earlier
									// event task that
									// are in task stack
									// this is rare as
									// stack is cleared
									// as and when task
									// are executed
									Iterator<Task> tkStack = context.getDialog().getTasks().iterator();
									while (tkStack.hasNext()) {
										Task t = tkStack.next();
										if (t.getName().startsWith("EVT_")) {
											logger.info("removed prior event triggered task from task stack....:"
													+ t.getName());
											tkStack.remove();
										}
									}
								}

								for (int i = 0; i < taskArray.size(); i++) {
									logger.info("added event triggered task....:" + taskArray.get(i).getName());
									context.getDialog().addTask(taskArray.get(i));
								}
								redirectTask = taskArray.get(0).getName();
							}

							return abortAndStartNewTask(redirectTask, answer_msg);
							// TODO maybe this should not necessarily abort the
							// current task?
						}
					}
					// --
				}

				return getNextQuestion(answer_msg);
			} else
				return end();
			// this line should never be reached, else throw exception:
			// throw new
			// ProcessingException("Unexpected dialogue state. This error should
			// never occur!");
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// added to handle null point exception
			return new UIConsumerMessage("", Meta.UNCHANGED);
		}
	}

	// Helpers:
	private ParseResults interpret(Entity ent, String user_answer) {
		ParseResults results = ent.parse(user_answer, true);
		if (results.size() > 0) {
			// check that state is not ERROR
			logger.info(
					"[DIALOG_ENGINE] a parser matched for entity: " + ent.getName() + " result: " + results.toString());
			// reset the entity FAILURE ATTEMPTS as parser matched
			FAILED_ENTITY_ATTEMPTS = 0;
			// added to support the storeCache functionality
			if (ent.isClearCache()) {
				logger.info("clearing CACHE as clearCache is set to true");
				CACHE_MESSAGE = "";
			}
		} else
			;
		// do nothing
		// logger.warning("no parser matched for entity: " + entity.getName());
		return results;
	}

	private void storeResults(Entity ent, ParseResults results) {
		if (results != null && results.size() > 0 && results.getState() == ParseResults.MATCH) {
			ent.setValue(results.getFirst().getResultString()); // TODO what if
																// more than one
																// parse result?
			// Update entityMapping hashMap for context building
			addtoEntityMapping(ent.getName(), results.getFirst().getResultString());
			// logging it to botReponse
			processBotResponse(null, Quartet.with(ent.getName(), ent.getLabel(), ent.getAnswerType(),
					results.getFirst().getResultString()), null, dialogState.ENTITY_FOUND);
			if (results.size() > 1)
				logger.warning("multiple results, only first one will be processed!");
		} else {
			logger.warning("parseResults empty and could not be stored in frame.");
		}
		// add entity to global Info in start task if it ends with "_" & if current
		// task is not start
		if (ent.getName().endsWith("_") && !context.getCurrentTask().getName().equals("start")) {
			logger.warning("storing " + ent.getName() + " to start task as a global Entity");
			context.getTask("start").addEntity(ent);
		}
	}

	private boolean lookForAnswers(ArrayList<Entity> allEntities, UserUtterance answer) {
		return lookForAnswers(null, allEntities, answer);
	}

	/*
	 * check for all/other questions in this task
	 */
	private boolean lookForAnswers(Entity currentEntity, ArrayList<Entity> allEntities, UserUtterance answer) {

		logger.info("looking for answers...");
		boolean found = false;

		String processedAnswer = answer.getText();
		// remove all leading and trailing ";" in case the user answers it
		// through interactive widget
		processedAnswer = processedAnswer.replaceAll("^;+", "");
		// remove all the matched NER parsers from userUtterance before processing
		if (NEREntities.size() > 0) {
			for (int i = 0; i < NEREntities.size(); i++) {
				if (processedAnswer.contains(NEREntities.get(i).getValue2())) {
					processedAnswer = processedAnswer.replaceFirst("(?i)" + NEREntities.get(i).getValue2(), "");
				}
			}
			NEREntities.clear();
		}
		// see if current task has set useCache flag , added flag just to ensure
		// that the same task is not generating the cache information
		if (context.getCurrentTask().getuseCache() && !CACHE_MESSAGE.isEmpty()
				&& !context.getCurrentTask().getName().equals(cacheTask)) {
			logger.info("added CACHE information: " + CACHE_MESSAGE + " to utterance: " + processedAnswer);
			processedAnswer = processedAnswer + " " + CACHE_MESSAGE;
			// to avoid cache being added for every entity iteration refresh the
			// cacheTask
			cacheTask = context.getCurrentTask().getName();
		}

		// change utterance to English for parser
		switch (context.getLocale().getLanguage()) {
		case "ar":
			processedAnswer = Transliteration.ArabicToEnglish(processedAnswer);
			logger.info("transliteration -> " + processedAnswer);
			// REGEX for removing special characters like "'"
			processedAnswer = processedAnswer.replaceAll("[\\']", "");
			break;
		case "en":
			break;
		case "hi":
			processedAnswer = Transliteration.DevanagariToEnglish(processedAnswer);
			logger.info("transliteration -> " + processedAnswer);
			// REGEX for removing special characters like "'"
			processedAnswer = processedAnswer.replaceAll("[\\']", "");
			break;
		case "da": // danish
		case "nl": // dutch
		case "sv": // swedish
		case "es": // sapnish

			processedAnswer = Transliteration.AccentToEnglish(processedAnswer);
		default:
			break;
		}

		if (processedAnswer.length() > 0) {
			ParseResults results = null;

			// first check current entities
			if (currentEntity != null) {

				// first remove entities that are filled by
				results = interpret(currentEntity, processedAnswer);
				if (results != null && results.getState() == ParseResults.MATCH) {
					found = true;
					// added to support the storeCache functionality
					if (currentEntity.isStoreCache()) {
						String val = results.getFirst().getMatchedSequence().toString();
						CACHE_MESSAGE = val;
						cacheTask = context.getCurrentTask().getName();
						logger.info("storing entity information: " + val + " to CACHE");
					}

					if (processedAnswer.toLowerCase()
							.contains(results.getFirst().getMatchedSequence().toString().toLowerCase())) {
						// remove matched parts to prevent multiple processing
						// of same information;
						// otherwise CITY fills both departure and destination
						String matchedString = DialogManagerHelper
								.escapeMetaCharacters(results.getFirst().getMatchedSequence().toString());
						processedAnswer = processedAnswer.replaceFirst("(?i)" + matchedString, "");
					} else {
						// this may be case when you process sys.number.scale
						// where it identifies string as 7000 for 7K and not
						// able to replace
						String newAnswer = processedAnswer.substring(0, results.getFirst().getBeginMatch())
								+ processedAnswer.substring(results.getFirst().getEndMatch());
						processedAnswer = newAnswer;
					}
					storeResults(currentEntity, results);
				}
			}
			boolean overanswering = (found && context.getDialog().isAllowOverAnswering());
			boolean differentquestion_correction = (!found
					&& (context.getDialog().isAllowDifferentQuestion() || context.getDialog().isAllowCorrection()));

			if ((overanswering || differentquestion_correction)) {

				if (overanswering)
					logger.info("(over answering allowed)");
				if (differentquestion_correction)
					logger.info("(different question or correction allowed)");

				// then check all (other) entities
				for (Entity ent : allEntities) {
					if (currentEntity != null && ent == currentEntity)
						continue;
					// if correction not allowed, only use unanswered entities
					if (!context.getDialog().isAllowCorrection()) {
						if (ent.isFilled())
							continue;
					}
					if (ent.getAnswerType().equals("sys.opentext"))
						continue;
					// check if Entity is of type dummy so that it can toggle
					/*
					 * if (entity.getAQD().getType().getAnswerType().equals("dummy") &&
					 * !DUMMY_TOGGLE_FLAG) { logger.
					 * info("found entity to be of dummy type, so ignoring the utterance" );
					 * DUMMY_TOGGLE_FLAG = true; continue; }
					 */
					results = interpret(ent, processedAnswer); // answer.getText()

					if (results.getState() == ParseResults.MATCH) {
						found = true;
						// added to support the storeCache functionality
						if (ent.isStoreCache()) {
							String val = results.getFirst().getMatchedSequence().toString();
							CACHE_MESSAGE = val;
							cacheTask = context.getCurrentTask().getName();
							logger.info("storing entity information: " + val + " to CACHE");
						}

						if (processedAnswer.toLowerCase()
								.contains(results.getFirst().getMatchedSequence().toString().toLowerCase())) {
							// remove matched parts to prevent multiple
							// processing of same information;
							// otherwise CITY fills both departure and
							// destination
							String matchedString = DialogManagerHelper
									.escapeMetaCharacters(results.getFirst().getMatchedSequence().toString());
							processedAnswer = processedAnswer.replaceFirst("(?i)" + matchedString, "");
						} else {
							// this may be case when you process
							// sys.number.scale where it identifies string as
							// 7000 for 7K and not able to replace
							String newAnswer = processedAnswer.substring(0, results.getFirst().getBeginMatch())
									+ processedAnswer.substring(results.getFirst().getEndMatch());
							processedAnswer = newAnswer;
						}
						storeResults(ent, results);
						if (processedAnswer.length() == 0)
							break;
						// break; //abort after first success
					}

				}
			}
		}
		if (!found)
			logger.info("no answers found");
		return found;
	}

	private UIConsumerMessage getNextQuestion() throws ProcessingException {
		return getNextQuestion(null);
	}

	private UIConsumerMessage getNextQuestion(UIConsumerMessage questionPrefix) throws ProcessingException {
		logger.info("getting next question (" + context.getTaskStack().size() + ")...");
		// if more questions in current task
		if (context.getIto_iterator().hasNext()) {
			Entity entity = context.getIto_iterator().next();

			if (entity.isFilled()) {
				logger.info("Entity " + entity.getName() + " already filled");
				// if already answered, get next question
				return getNextQuestion(questionPrefix);
			}
			// beta
			else if (!entity.isRequired()) {
				logger.info("Entity " + entity.getName() + " is optional and thus skipped");
				return getNextQuestion(questionPrefix);
			}
			// ---
			else {
				// resetting this as entity is found

				context.setCurrentQuestion(entity); // point current question to
													// this entity
				String answer_message = (questionPrefix == null) ? "" : (questionPrefix.getSystemUtterance() + " ");
				String question = DialogManagerHelper.fillfbqWithEntity(entity.getFallbackQuestion(), entityMapping); // get
				// question
				// set botEntityinAction to this entity as it is active now
				botEntityInAction = Quartet.with(entity.getName(), entity.getLabel(), entity.getAnswerType(), "");

				// NLG fill
				String NLGFill = "AIML_NO_ANSWER";
				if (USE_NLG) {
					String NLG_RESOURCE = "";
					switch (context.getLocale().getLanguage()) {

					case "en":
						NLG_RESOURCE = "NLGFILLen";
						break;
					case "hi":
						NLG_RESOURCE = "NLGFILLhi";
						break;
					case "da":
						NLG_RESOURCE = "NLGFILLda";
						break;
					case "nl":
						NLG_RESOURCE = "NLGFILLnl";
						break;
					case "sv":
						NLG_RESOURCE = "NLGFILLsv";
						break;
					case "es":
						NLG_RESOURCE = "NLGFILLes";
						break;
					default:
						NLG_RESOURCE = "NLGFILLen";
						break;
					}
					NLGFill = ConvEngineProcessor.aimlProcessor.process(NLG_RESOURCE);
				}
				String utterance = "";
				// if at start
				if (NLGFill.matches("AIML_NO_ANSWER") || context.getCurrentTask().getName().matches("start")
						|| NLGFill.replace(" ", "").matches("")) // if empty

					utterance = context.getLocale().getLanguage().equals("ar") ? question + answer_message
							: answer_message + question;
				else
					utterance = context.getLocale().getLanguage().equals("ar")
							? question + " " + NLGFill + " " + answer_message
							: answer_message + " " + NLGFill + " " + question;
				// answer_message is recorded in different else-branch
				context.addUtteranceToHistory(question, UTTERANCE_TYPE.SYSTEM, context.getTaskStack().size());
				// load the entity to lastAccessedEntity_ only if task is not helpdesk
				// (added to
				// support the volvo use case)
				if (context.getCurrentTask().getEntities("lastAccessedEntity_") != null
						&& !context.getCurrentTask().getName().equals("helpDesk")) {
					context.getCurrentTask().getEntities("lastAccessedEntity_").setValue(entity.getName());
					addtoEntityMapping("lastAccessedEntity_", entity.getName());
				}
				return new UIConsumerMessage(utterance, Meta.QUESTION);
			}
		}
		// if current task has no more questions, get next task from stack
		else if (context.getTaskStack().size() >= 1 && isTaskExecuted && !setFollowupFlag) {
			// other tasks on stack (apart from current task)
			logger.info("no more questions, task " + context.getCurrentTask().getName() + " completed, removing it");
			if (questionPrefix != null)
				context.addUtteranceToHistory(questionPrefix.getSystemUtterance(), UTTERANCE_TYPE.SYSTEM,
						context.getTaskStack().size());
			// remove current (finished task) from stack and
			context.getTaskStack().pop().reset();
			// get next task from stack
			return initTaskAndGetNextQuestion(context.getTaskStack().pop(), questionPrefix);
		}
		// Execute task in case all entities are filled in through
		// context
		else if (context.getTaskStack().size() >= 1 && !isTaskExecuted) {
			logger.info("all entities filled, executing action");
			Action action = context.getCurrentTask().getAction();
			UIConsumerMessage answer_msg = null;
			// logging to /log/activity before executing the task
			String actionName = context.getCurrentTask().getAction().toString();
			LogForActivity.logData(ConvEngineProcessor.getDefaultDialogPathAndName(),
					this.getDebugInfo("loginUser") + " sessionId:" + this.getDebugInfo("sessionID") + " clientIP:"
							+ this.getDebugInfo("clientIP"),
					context.getCurrentTask().getName(), actionName.substring(actionName.lastIndexOf(".") + 1));
			String sysAns = processExecutionResult(context.getCurrentTask().execute());
			// check if it has failed for any reason
			if (sysAns.contains("FAIL_MSG")) {
				sysAns = appMessages.getString("FAIL_MSG");
			}
			// store the system answer to like , dislike object

			if (!(context.getCurrentTask().getName().equals("start")
					|| context.getCurrentTask().getName().equals("cancelTask")
					|| context.getCurrentTask().getName().equals("handoverTask")
					|| context.getCurrentTask().getName().equals("helpTask"))) {
				DialogManagerHelper.likeDislikeObject.setSystemUtterance(MaskData.mask(sysAns, MASK_DATA));
				SHOW_FEEDBACK_ICONS = true;
			}
			// added to return answer in the dialog locale
			switch (context.getLocale().getLanguage()) {
			case "ar":
				sysAns = Transliteration.EnglishToArabic(sysAns);
				break;
			case "en":
			case "da":
			case "nl":
			case "sv":
			case "es":
				break;
			case "hi":
				sysAns = Transliteration.EnglishToDevnagari(sysAns);
				break;

			default:
				break;
			}
			isTaskExecuted = true;
			if (context.getCurrentTask().getFollowup() != null)
				setFollowupFlag = true;
			if (action.isReturnAnswer()) {
				answer_msg = new UIConsumerMessage(sysAns, Meta.ANSWER);
				ActionResultMapping resultMapping;
				if ((resultMapping = action.getFirstMatchingResultMapping()) != null) {
					if (resultMapping.getRedirectToTask() != null && resultMapping.getRedirectToTask().length() > 0) {
						String redirectTask = resultMapping.getRedirectToTask();
						// added to support the event task
						if (redirectTask.startsWith("@")) {
							String eName = redirectTask.trim().substring(1, redirectTask.trim().length());
							EventManager eventManager = new EventManager(eName);
							ArrayList<Task> taskArray = new ArrayList<Task>();
							if (eventManager.processEvent() != null)
								taskArray = eventManager.processEvent();
							if (taskArray.size() > 0) { // clean up earlier
								// event task that
								// are in task stack
								// this is rare as
								// stack is cleared
								// as and when task
								// are executed
								Iterator<Task> tkStack = context.getDialog().getTasks().iterator();
								while (tkStack.hasNext()) {
									Task t = tkStack.next();
									if (t.getName().startsWith("EVT_")) {
										logger.info("removed prior event triggered task from task stack.....:"
												+ t.getName());
										tkStack.remove();
									}
								}
							}
							for (int i = 0; i < taskArray.size(); i++) {
								logger.info("added event triggered task.....:" + taskArray.get(i).getName());
								context.getDialog().addTask(taskArray.get(i));
							}
							redirectTask = taskArray.get(0).getName();
						}

						return abortAndStartNewTask(redirectTask, answer_msg);
					}
				}
			}
			return getNextQuestion(answer_msg);
		}
		// -- beta --
		// if follow up exists:
		else if (context.getCurrentTask().getFollowup() != null
				&& context.getCurrentTask().getFollowup().getEntity() != null
				&& !context.getCurrentTask().getFollowup().getEntity().isFilled()) {
			followup = true;
			setFollowupFlag = false;
			Entity entity = context.getCurrentTask().getFollowup().getEntity();
			botEntityInAction = Quartet.with(entity.getName(), entity.getLabel(), entity.getAnswerType(), "");

			context.setCurrentQuestion(entity);
			String answer_message = (questionPrefix == null) ? "" : (questionPrefix.getSystemUtterance() + " ");
			String question = DialogManagerHelper.fillfbqWithEntity(entity.getFallbackQuestion(), entityMapping); // get
			// question
			String utterance = answer_message + question;

			if (questionPrefix != null)
				context.addUtteranceToHistory(questionPrefix.getSystemUtterance(), UTTERANCE_TYPE.SYSTEM,
						context.getTaskStack().size());
			// answer is recorded in different else-branch
			context.addUtteranceToHistory(question, UTTERANCE_TYPE.SYSTEM, context.getTaskStack().size());
			// load the entity to lastAccessedEntity_ only if task is not helpdesk
			// (added to
			// support the volvo use case)
			if (context.getCurrentTask().getEntities("lastAccessedEntity_") != null
					&& !context.getCurrentTask().getName().equals("helpDesk")) {
				context.getCurrentTask().getEntities("lastAccessedEntity_").setValue(entity.getName());
				addtoEntityMapping("lastAccessedEntity_", entity.getName());
			}
			return new UIConsumerMessage(utterance, Meta.QUESTION);
		}
		// if stack is empty and no more questions available but an answer from
		// the last execution is still pending, return this answer
		else if (questionPrefix != null) {
			if (!context.getCurrentTask().getName().equals("start")) {
				logger.info("...removing task: " + context.getCurrentTask().getName());
				context.getTaskStack().pop().reset(); // remove current
														// (finished
			} // task) from stack
			return questionPrefix;
		}
		// else return restart(); //restart if no more questions available
		else
			return end(); // indicate end of dialogue
	}

	private UIConsumerMessage initTaskAndGetNextQuestion(Task t) throws ProcessingException {
		return initTaskAndGetNextQuestion(t, null);
	}

	private void switchTask(Task t) {
		logger.info("switch task to: " + t.getName() + " (" + context.getTaskStack().size() + ")...");
		// need to reset the cache task flag and TASK_WITH_DUMMY due to task
		// switching
		cacheTask = "";
		// DUMMY_TOGGLE_FLAG = false;
		// store this task into lastAccessesestask stack used to populate
		// lastAccessesTask_ entity
		addToLastAccessedTaskStack(t.getName());
		// start task is always required at the start.
		if (t.getName().equals("start")) {
			// reset the entity as we are not removing start from task stack
			t.getEntities("welcome").setUnFilled();
			isStartAddedtoStack = true;
		}
		// Manual addition is required if user selects to start with other task
		// in flag <start_task_name>
		if (!isStartAddedtoStack) {
			isStartAddedtoStack = true;
			Task tsk = new Task("start");
			// tsk.setLabel("Initial Task");
			// put empty selector to avoid null pointer exception
			// tsk.setSelector(new BagOfWordsTaskSelector(new
			// ArrayList<String>()));
			// added to return answer in the dialog locale
			Entity ent = new Entity("welcome", "How may I help you?", "open_ended");
			// change the fallback question based on language
			switch (context.getDialog().getGlobalLanguage()) {
			case "ar":
				ent.setFallbackQuestion("كيف يمكنني مساعدتك؟");
				break;
			case "en":
				break;
			case "hi":
				ent.setFallbackQuestion("मैं आपकी क्या मदद कर सकता हूँ।");
				break;
			case "da":
				ent.setFallbackQuestion("Hvordan kan jeg hjælpe dig?");
				break;
			case "nl":
				ent.setFallbackQuestion("Hoe kan ik u helpen?");
				break;
			case "sv":
				ent.setFallbackQuestion("Hur kan jag hjälpa dig?");
				break;
			case "es":
				ent.setFallbackQuestion("Como puedo ayudarte?");
				break;
			default:
				break;
			}
			ent.setRequired(true);
			tsk.addEntity(ent);
			tsk = createdefaultEntities(tsk);
			context.getTaskStack().push(tsk);
		}
		// added to take care of EVT task. EVT tasks are removed from stack and
		// hence the global entities are also removed. we need to induct them back
		if (t.getName().startsWith("EVT_")) {
			t = createdefaultEntities(t);
		}
		// CR-101 on 7-Apr-18 ==if user sets the ignore
		// previous task flag in properties file, it
		// should still
		// switch to new task but ignore the previous
		// task==
		if (!t.getName().equals("start")) { // to avoid this being called right
											// at the beginning -> null pointer
			if (context.getCurrentTask() != null) { // null pointer exception
				if (IGNORE_PREV_TASK
						&& !(context.getCurrentTask().getName().equals("start") || t.getName().equals("cancelTask")
								|| t.getName().equals("handoverTask") || t.getName().equals("helpTask"))) {
					Iterator<Task> tkStack = getContext().getTaskStack().iterator();
					while (tkStack.hasNext()) {
						Task tsk = tkStack.next();
						if (tsk.getName().equals(context.getCurrentTask().getName())) {
							logger.info("removed task: " + tsk.getName() + " as IGNORE_PREV_TASK flag is set to true");
							tkStack.remove();
							break;
						}
					}
				}
			}
		}
		// resetting flag to false
		isTaskExecuted = false;
		isIntentLookupDone = false;
		if (!context.getTaskStack().contains(t)) {
			context.getTaskStack().push(t);
		}
		// push Global Entities to current task only if current task is
		// not "start"
		if (!context.getCurrentTask().getName().equals("start")) {
			t = pushGlobalEntities(t);
			ArrayList<Entity> entityList = new ArrayList<Entity>();
			entityList = context.getCurrentTask().getEntities();
			for (int i = 0; i < entityList.size(); i++) {
				// Fill entity from entityMapping if useContext is set to true
				// irrespective of over answering flag is false
				if (entityList.get(i).isUseContext() && entityMapping.containsKey(entityList.get(i).getName())
						&& !entityList.get(i).getName().endsWith("_")) {
					logger.info("filling entity from context map as useContext flag is set to true");
					context.getCurrentTask().getEntities(entityList.get(i).getName())
							.setValue(entityMapping.get(entityList.get(i).getName()));

					// logging to botResponse
					processBotResponse(null, null,
							Quartet.with(entityList.get(i).getName(), entityList.get(i).getLabel(),
									entityList.get(i).getAnswerType(), entityMapping.get(entityList.get(i).getName())),
							dialogState.CONTEXT_FILL);
				}

				// clear the context if the clearContext flag has been set
				if (entityList.get(i).isClearContext() && entityMapping.containsKey(entityList.get(i).getName())
						&& !entityList.get(i).getName().endsWith("_")) {
					logger.info("clearing entity from context map as clearContext flag is set to true");
					entityMapping.remove(entityList.get(i).getName());
				}
				// store to Cache if storeCache has been set and the entityMapping
				// has the entity present
				if (entityList.get(i).isStoreCache() && entityMapping.containsKey(entityList.get(i).getName())
						&& !entityList.get(i).getName().endsWith("_")) {
					CACHE_MESSAGE = entityMapping.get(entityList.get(i).getName());
					cacheTask = context.getCurrentTask().getName();
					logger.info("storing entity information: " + entityMapping.get(entityList.get(i).getName())
							+ " to CACHE");
				}
				// clear Cache if cacheFlag has been set and the entityMapping has
				// the entity present
				if (entityList.get(i).isClearCache() && entityMapping.containsKey(entityList.get(i).getName())
						&& !entityList.get(i).getName().endsWith("_")) {
					logger.info("clearing CACHE as clearCache is set to true");
					CACHE_MESSAGE = "";
				}
			}
			// modify the entities as identified by NLP Engine
			pushNEREntities();
		}
		// logging to botResponse
		processBotResponse(Triplet.with(t.getName(), t.getLabel(), t.getRole()), null, null, dialogState.TASK_SWITCH);
		context.setEntityIterator(t.getEntities().iterator());
	}

	// TODO beta
	private UIConsumerMessage abortAndStartNewTask(String taskName, UIConsumerMessage questionPrefix)
			throws ProcessingException {
		if (questionPrefix != null)
			context.addUtteranceToHistory(questionPrefix.getSystemUtterance(), UTTERANCE_TYPE.SYSTEM,
					context.getTaskStack().size());
		if (!context.getCurrentTask().getName().equals("start")) {
			logger.info(".removing task: " + context.getCurrentTask().getName());

			context.getTaskStack().pop().reset(); // remove current (finished
													// task)
													// from stack only if it is
													// other than start...this
													// was
													// failing when activity is
													// at
													// start and user says exit
		}
		Task newTask = context.getDialog().getTask(taskName);
		return initTaskAndGetNextQuestion(newTask, questionPrefix);
	}

	private UIConsumerMessage initTaskAndGetNextQuestion(Task t, UIConsumerMessage questionPrefix)
			throws ProcessingException {
		switchTask(t);
		// added to check if task uses cache
		if (context.getCurrentTask().getuseCache() && !CACHE_MESSAGE.isEmpty()
				&& !context.getCurrentTask().getName().equals(cacheTask)) {
			UserUtterance cachedMessage = new UserUtterance("");
			// just create a dummy message as we are adding CACHE_MESAAGE in
			// lookForAnswers function
			lookForAnswers(t.getEntities(), cachedMessage);
			// to avoid cache being added for every entity iteration refresh the
			// cacheTask
			cacheTask = context.getCurrentTask().getName();
		}

		// pass any of the dummy information captured to new task
		// added to support the DUMMY entity functionality
		/*
		 * if (!DUMMY_MESSAGE.isEmpty()) {
		 * logger.info("passed on the information from Dummy parser to task: " +
		 * t.getName()); UserUtterance dummyAnswer = new UserUtterance(DUMMY_MESSAGE);
		 * DUMMY_MESSAGE = ""; lookForAnswers(t.getEntities(), dummyAnswer); }
		 */
		return getNextQuestion(questionPrefix);
	}

	// TODO
	@SuppressWarnings("unused")
	private void restartTask() {
		// pop reset switch task
	}

	// TODO still experimental
	private UIConsumerMessage end() throws ProcessingException {
		while (!context.getTaskStack().isEmpty()) {
			context.getTaskStack().pop().reset(); // destroy all tasks
		}
		context.setStarted(false); // allows user to (re)start the dialogue
									// again
		logger.info("end of dialogue");
		String reply = "You are now signed out.Thank you.";
		// or indicate end of dialogue
		switch (context.getDialog().getGlobalLanguage()) {
		case "ar":
			reply = "";
			break;
		case "en":
			break;
		case "hi":
			reply = "अब आप साइन आउट हुए हों। धन्यवाद।";
			break;
		case "da":
			reply = "Du er nu logget ud. Tak skal du have.";
			break;
		case "nl":
			reply = "U bent nu uitgelogd. Dank je.";
			break;
		case "sv":
			reply = "Du är nu utloggad. Tack.";
			break;
		case "es":
			reply = "Usted ahora está desconectado. Gracias.";
			break;
		default:
			break;
		}
		return new UIConsumerMessage(reply, Meta.END_OF_DIALOG);
	}

	// TODO still experimental
	@SuppressWarnings("unused")
	private UIConsumerMessage restart() throws ProcessingException {
		context.setStarted(false);
		return processUtterance(null);
	}

	public void addtoEntityMapping(String key, String value) {
		// TODO Auto-generated method stub
		entityMapping.put(key, value);

	}

	public String getDialogDefinition() {

		return null;
	}

	public boolean isLicenseValid() {
		return isLicenseValid;
	}

	// Get list of all the tasks in form JSON response
	@Override
	public String getTaskInfo() {
		StringWriter out = new StringWriter();
		ArrayList<Entity> entityList = new ArrayList<Entity>();
		List<Task> taskList = new ArrayList<Task>();
		JSONObject infoObject = new JSONObject();
		JSONArray taskArray = new JSONArray();
		if (getContext().getTaskStack() != null)
			taskList = getContext().getTaskStack();
		for (int i = taskList.size() - 1; i >= 0; i--) {
			JSONArray entityArray = new JSONArray();
			entityList = getContext().getTask(taskList.get(i).getName()).getEntities();
			try {
				JSONObject taskObject = new JSONObject();
				taskObject.put("name", taskList.get(i).getName());
				taskObject.put("label", taskList.get(i).getLabel());
				taskObject.put("role", taskList.get(i).getRole());

				if (entityList != null) {
					for (int j = 0; j < entityList.size(); j++) {
						JSONObject entityObject = new JSONObject();
						if (entityList.get(j).getValue() != null) {
							entityObject.put("name", entityList.get(j).getName());
							entityObject.put("label", entityList.get(j).getLabel());
							entityObject.put("type", entityList.get(j).getAnswerType());
							entityObject.put("value", entityList.get(j).getValue());
						} else {
							entityObject.put("name", entityList.get(j).getName());
							entityObject.put("label", entityList.get(j).getLabel());
							entityObject.put("type", entityList.get(j).getAnswerType());
							entityObject.put("value", JSONObject.NULL);
						}
						entityArray.put(entityObject);
					}
					taskObject.put("entities", entityArray);
				}
				taskArray.put(taskObject);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			infoObject.put("tasks", taskArray);
			infoObject.write(out);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out.toString();
	}

	@Override
	public String getDialogFailureInfo() {
		StringWriter out = new StringWriter();
		JSONObject infoObject = new JSONObject();
		try {
			infoObject.put("sessionId", context.getAdditionalDebugInfo("sessionID"));
			infoObject.put("failures", String.valueOf(FAILED_ATTEMPTS));
			infoObject.put("likes", String.valueOf(LIKE_COUNTS));
			infoObject.put("dislikes", String.valueOf(DISLIKE_COUNTS));
			infoObject.write(out);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out.toString();

	}

	@Override
	public String buildResponse(String query, String reply) {

		reply = reply.replaceAll("[\\t\\n\\r]", "");
		reply = reply.trim();

		StringWriter out = new StringWriter();
		JSONObject responseObject = new JSONObject();
		JSONObject resultObject = new JSONObject();
		JSONArray entitiesArray = new JSONArray();
		JSONArray contextsArray = new JSONArray();
		JSONObject actionObject = new JSONObject();
		JSONArray actionEnitiesArray = new JSONArray();

		try {
			// create entities object
			for (int i = 0; i < botEntities.size(); i++) {
				// fill entity Array with objects
				JSONObject entityObject = new JSONObject();
				entityObject.put("name", botEntities.get(i).getValue0());
				entityObject.put("label", botEntities.get(i).getValue1());
				entityObject.put("type", botEntities.get(i).getValue2());
				entityObject.put("value", botEntities.get(i).getValue3());
				entitiesArray.put(entityObject);
				// entityObject.put(entry.getKey(), entry.getValue());
			}
			// create context object
			for (int i = 0; i < botContexts.size(); i++) {
				// fill context entity Array with objects
				JSONObject contextObject = new JSONObject();
				contextObject.put("name", botContexts.get(i).getValue0());
				contextObject.put("label", botContexts.get(i).getValue1());
				contextObject.put("type", botContexts.get(i).getValue2());
				contextObject.put("value", botContexts.get(i).getValue3());
				contextsArray.put(contextObject);
				// contextObject.put(entry.getKey(), entry.getValue());
			}

			// create action object
			for (int i = 0; i < botActionEntities.size(); i++) {
				// fill entity Array with objects
				JSONObject actionEnitiesObject = new JSONObject();
				actionEnitiesObject.put("name", botActionEntities.get(i).getValue0());
				actionEnitiesObject.put("label", botActionEntities.get(i).getValue1());
				actionEnitiesObject.put("type", botActionEntities.get(i).getValue2());
				actionEnitiesObject.put("value", botActionEntities.get(i).getValue3());
				actionEnitiesArray.put(actionEnitiesObject);
				// actionEnitiesObject.put(entry.getKey(), entry.getValue());
			}

			// create a result JSONObject
			resultObject.put("query", query);
			// append systemNotification to reply
			if (query.startsWith("systemNotification"))
				resultObject.put("reply", "systemNotitification:" + reply);
			else
				resultObject.put("reply", reply);

			String speech = reply;

			// segregating reply for TTS speech. getting data from <p> tag
			if (reply.trim().contains("<p>")) {
				Pattern p = Pattern.compile("<p>(.+?)<\\/p>");
				Matcher m = p.matcher(reply);
				while (m.find()) {
					speech = m.group(1);
				}
			}

			// replace all the html tags within p tags
			speech = speech.replaceAll("\\<.*?\\>", "");
			// check if it starts with @ as part of system notification
			if (speech.startsWith("@"))
				speech = "";

			resultObject.put("speech", speech);

			// add intent Object
			JSONObject intentObject = new JSONObject();
			intentObject.put("name", botIntent.getValue0());
			intentObject.put("label", botIntent.getValue1());
			intentObject.put("role", botIntent.getValue2());

			resultObject.put("intent", intentObject);
			JSONObject currentEntityObject = new JSONObject();
			currentEntityObject.put("name", botEntityInAction.getValue0());
			currentEntityObject.put("label", botEntityInAction.getValue1());
			currentEntityObject.put("type", botEntityInAction.getValue2());
			currentEntityObject.put("value", botEntityInAction.getValue3());

			resultObject.put("currentEntity", currentEntityObject);
			resultObject.put("entities", entitiesArray);
			resultObject.put("contexts", contextsArray);

			// creating action object
			intentObject = new JSONObject();
			intentObject.put("name", botActionIntent.getValue0());
			intentObject.put("label", botActionIntent.getValue1());
			intentObject.put("role", botActionIntent.getValue2());

			actionObject.put("intent", intentObject);
			actionObject.put("entities", actionEnitiesArray);

			// create final response
			responseObject.put("user", entityMapping.get("loginUser_"));
			responseObject.put("role", entityMapping.get("loginRole_"));
			responseObject.put("authToken", entityMapping.get("authToken_"));
			responseObject.put("sessionId", context.getAdditionalDebugInfo("sessionID"));
			responseObject.put("timeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			responseObject.put("language", context.getLocale().getLanguage());
			responseObject.put("source", context.getDialog().getName());
			responseObject.put("notifications", String.valueOf(notificationList.size()));
			responseObject.put("result", resultObject);
			responseObject.put("action", actionObject);

			if (SHOW_INTERACTIVE_FORM)
				responseObject.put("iForm", DialogManagerHelper.createiForm(context, botEntityInAction,
						SHOW_FEEDBACK_ICONS, LIKE_COUNTS, DISLIKE_COUNTS));
			SHOW_FEEDBACK_ICONS = false;

			if (jsonInfo.has("message"))
				responseObject.put("message", jsonInfo.get("message"));

			// reset jsonInfo
			jsonInfo.remove("message");
			// Important to set this to true as the action is responded to user
			// as response
			isActionFlashed = true;
			responseObject.write(out);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out.toString();
	}

	private void processBotResponse(Triplet<String, String, String> task,
			Quartet<String, String, String, String> entity, Quartet<String, String, String, String> contextIto,
			dialogState dstate) {

		switch (dstate) {
		case TASK_SWITCH:
			if (!task.equals("start")) {
				botIntent = task;
				botEntityInAction.setAt0("");
				botEntityInAction.setAt1("");
				botEntityInAction.setAt2("");
				botEntityInAction.setAt3("");

				botEntities.clear();
				ArrayList<Entity> entityList = new ArrayList<Entity>();
				entityList = context.getTask(task.getValue0().toString()).getEntities();
				for (int j = 0; j < entityList.size(); j++) {
					if (!entityList.get(j).getName().endsWith("_")) {

						if (entityList.get(j).getValue() != null) {

							botEntities.add(Quartet.with(entityList.get(j).getName(), entityList.get(j).getLabel(),
									entityList.get(j).getAnswerType(), entityList.get(j).getValue().toString()));

						} else

							botEntities.add(Quartet.with(entityList.get(j).getName(), entityList.get(j).getLabel(),
									entityList.get(j).getAnswerType(), ""));
					}
				}
			}
			if (isActionFlashed) {
				botActionIntent = Triplet.with("", "", "");
				botActionEntities.clear();
				isActionFlashed = false;
			}
			break;

		case ENTITY_FOUND:
			for (int i = 0; i < botEntities.size(); i++) {
				if (botEntities.get(i).getValue0().toString().contains(entity.getValue0().toString())) {
					botEntities.remove(i);
					botEntities.add(entity);
				}
			}

			// to ensure that context is retained
			for (int i = 0; i < botContexts.size(); i++) {
				if (botContexts.get(i).getValue0().toString().contains(entity.getValue0().toString())) {
					botContexts.remove(i);
					botContexts.add(entity);
				}

			}
			// required here in case we are retaining action even if task is
			// switched
			if (isActionFlashed) {
				botActionIntent = Triplet.with("", "", "");
				botActionEntities.clear();
				isActionFlashed = false;
			}
			break;

		case CONTEXT_FILL:
			botContexts.add(contextIto);
			botActionIntent = Triplet.with("", "", "");
			botActionEntities.clear();
			break;

		case ACTION_EXEC:
			botActionIntent = task;
			botActionEntities.clear();
			ArrayList<Entity> entityList_1 = new ArrayList<Entity>();
			entityList_1 = context.getTask(task.getValue0().toString()).getEntities();
			for (int j = 0; j < entityList_1.size(); j++) {
				if (!entityList_1.get(j).getName().endsWith("_")) {
					if (entityList_1.get(j).getValue() != null) {
						botActionEntities.add(Quartet.with(entityList_1.get(j).getName(),
								entityList_1.get(j).getLabel(), entityList_1.get(j).getAnswerType(),
								entityList_1.get(j).getValue().toString()));
					} else
						botActionEntities.add(Quartet.with(entityList_1.get(j).getName(),
								entityList_1.get(j).getLabel(), entityList_1.get(j).getAnswerType(), "null"));
				}
			}
			break;
		}
	}

	@Override
	public String processExecutionResult(String response) {
		// TODO Auto-generated method stub
		if (response.startsWith("{")) {
			String chat = "";
			logger.info("processing JSON: " + response);
			try {
				JSONObject json = new JSONObject(response);
				JSONObject message = new JSONObject();
				message.put("message", json);
				// putting message to JSONobject
				jsonInfo = message;
				if (message.getJSONObject("message").has("chat")) {
					chat = message.getJSONObject("message").getString("chat");
				} else {
					logger.info("processing JSON: missing chat object");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return chat;
		} else
			return response;
	}

	HashMap<String, String> classifyIntent(String domain, String locale, String utterance) {
		HashMap<String, String> IEResult = new HashMap<String, String>();
		isIntentLookupDone = true;
		DialogObject d0 = null;
		String ret = "";

		if (!USE_BROKER) {
			d0 = new DialogObject();
			d0 = PredictModel.predict(domain, locale, utterance);
			if (d0 != null)
				ret = d0.getMessage();
			else {
				// generate error response
				IEResult.put("IE_ERROR", "utterance could not be processed by NLP Engine.");
				return IEResult;
			}
		} else {
			// check if broker is up and running
			if (!CheckBrokerStatus.getStatus())
				logger.severe("failed to connect to broker server");
			// create the dialog message for intent prediction
			d0 = new DialogObject("PREDICT", domain, locale, utterance);
			// send message for predicting with key as dialog instance
			if (SendMessage.send((String) Params.getParam("TOPIC_BOT_TO_NLP"), this.getDebugInfo("sessionID"), d0))
				logger.info("message sent successfully");
			else
				logger.severe("[BROKER] failed to send message to broker.");

			if (LookForMessage.look(MAX_WAIT_TIME, this.getDebugInfo("sessionID"), "PREDICT")) {
				ConsumerRecord<String, DialogObject> rec = brokerMessages.get(this.getDebugInfo("sessionID"));
				ProcessMessage.process(rec.key(), rec.value());
				// get the Intent Engine result
				ret = brokerMessages.get(this.getDebugInfo("sessionID")).value().getMessage();
			} else
				logger.severe("[BROKER] failed to recieve message within set time");
		}

		boolean chkSimilarity = true;
		if (intentSimilarityCheck.contains(this.getDebugInfo("sessionID")))
			chkSimilarity = false;

		IEResult = ProcessClassifierResult.processIntent(ret, IE_THRESHOLD_SCORE, IE_SIMILARITY_INDEX, chkSimilarity);
		// set checkSimilarity flag for subsequent check
		if (IEResult.containsKey("INTENT_CLARIFICATION")) {
			intentSimilarityCheck.add(this.getDebugInfo("sessionID"));
		} else {
			// remove it from Hashset as we want to get back to
			// checkSimilairty flag to true
			if (intentSimilarityCheck.contains(this.getDebugInfo("sessionID")))
				intentSimilarityCheck.remove(this.getDebugInfo("sessionID"));
		}

		// load if any entities are extracted from NLP Engine
		NEREntities = ProcessClassifierResult.processEntities(ret);
		if (NEREntities.size() > 0)
			logger.info("[NLP_ENGINE] number of entities identified by NLP engine: " + NEREntities.size());

		logger.info("intent engine post processing result: " + IEResult);
		return IEResult;
	}

	public void registerLikeDislike(String option) {
		if (option.contains("DISLIKE"))
			DISLIKE_COUNTS++;
		else
			LIKE_COUNTS++;
		// TODO Auto-generated method stub
		LogForTraining.logData(ConvEngineProcessor.getDefaultDialogPathAndName(),
				this.getDebugInfo("loginUser") + " sessionId:" + this.getDebugInfo("sessionID") + " clientIP:"
						+ this.getDebugInfo("clientIP"),
				DialogManagerHelper.likeDislikeObject.getTask(),
				"U: " + DialogManagerHelper.likeDislikeObject.getUserUtterance() + " S: "
						+ DialogManagerHelper.likeDislikeObject.getSystemUtterance(),
				option);
	}

	// This function fills url parameters with entity values from entityMapping used
	public String fillUrlParamWithEntity(String url) {
		return DialogManagerHelper.fillUrlParamWithEntity(url, entityMapping);
	}

	// This function adds the ongoing task to lastAccessedTask entity before
	// switching the task
	private void addToLastAccessedTaskEntity() {
		String tName = "NA";
		if (context.getCurrentTask() != null && lastAccessedTaskStack.size() > 2) {
			tName = lastAccessedTaskStack.get(lastAccessedTaskStack.size() - 2);
			if (context.getCurrentTask().getEntities("lastAccessedTask_") != null) {
				context.getCurrentTask().getEntities("lastAccessedTask_").setValue(tName);
			}
		}
		addtoEntityMapping("lastAccessedTask_", tName);
	}

	private void addToLastAccessedTaskStack(String tName) {
		// maximum size of stack that we will keep track
		int maxSize = 5;
		if (lastAccessedTaskStack.size() >= maxSize)
			lastAccessedTaskStack.remove(0);
		lastAccessedTaskStack.push(tName);
	}

	private Task createdefaultEntities(Task t) {
		// setting the loginUser as a global variable
		if (!t.hasEntity("loginUser_")) {
			Entity entityuId = new Entity("loginUser_", "", "unfilled");
			if (entityMapping.containsKey("loginUser_"))
				entityuId.setValue(entityMapping.get("loginUser_"));
			else
				entityuId.setValue("test");
			entityuId.setRequired(false);
			logger.info("created system Entity loginUser_ for task: " + t.getName());
			t.addEntity(entityuId);
		}
		if (!t.hasEntity("loginRole_")) {
			Entity entityrId = new Entity("loginRole_", "", "unfilled");
			if (entityMapping.containsKey("loginRole_"))
				entityrId.setValue(entityMapping.get("loginRole_"));
			else
				entityrId.setValue("admin");
			entityrId.setRequired(false);

			logger.info("created system Entity loginRole_ for task: " + t.getName());
			t.addEntity(entityrId);
		}
		if (!t.hasEntity("authToken_")) {
			Entity entitytId = new Entity("authToken_", "", "unfilled");
			if (entityMapping.containsKey("authToken_"))
				entitytId.setValue(entityMapping.get("authToken_"));
			else
				entitytId.setValue("");
			entitytId.setRequired(false);
			logger.info("created system Entity authToken_ for task: " + t.getName());
			t.addEntity(entitytId);
		}
		// setting the sessionId entity as a global variable
		if (!t.hasEntity("sessionId_")) {
			Entity entitySessionId = new Entity("sessionId_", "", "unfilled");
			if (entityMapping.containsKey("sessionId_"))
				entitySessionId.setValue(entityMapping.get("sessionId_"));
			else
				entitySessionId.setValue("d1-DUMMYSESSION");
			entitySessionId.setRequired(false);
			logger.info("created system Entity sessionId_ for task: " + t.getName());
			t.addEntity(entitySessionId);
		}
		if (!t.hasEntity("lastAccessedEntity_")) {
			// setting the lastAccessedEntity entity as a global variable
			Entity lastAccessedEntity = new Entity("lastAccessedEntity_", "", "unfilled");
			if (entityMapping.containsKey("lastAccessedEntity_"))
				lastAccessedEntity.setValue(entityMapping.get("lastAccessedEntity_"));
			else
				lastAccessedEntity.setValue("NA");
			lastAccessedEntity.setRequired(false);
			logger.info("created system Entity lastAccessedEntity_ for task: " + t.getName());
			t.addEntity(lastAccessedEntity);
		}
		// setting the lastAccessedTask entity as a global variable
		if (!t.hasEntity("lastAccessedTask_")) {
			Entity lastAccessedTask = new Entity("lastAccessedTask_", "", "unfilled");
			if (entityMapping.containsKey("lastAccessedTask_"))
				lastAccessedTask.setValue(entityMapping.get("lastAccessedTask_"));
			else
				lastAccessedTask.setValue("NA");
			lastAccessedTask.setRequired(false);
			logger.info("created system Entity lastAccessedTask_ for task: " + t.getName());
			t.addEntity(lastAccessedTask);
		}
		return t;
	}

	private Task pushGlobalEntities(Task t) {
		ArrayList<Entity> entityList = new ArrayList<Entity>();
		if (context.getTaskStack().size() > 1) // ensure start task in its stack
			entityList = context.getTask("start").getEntities();
		for (int i = 0; i < entityList.size(); i++) {
			if (entityList.get(i).getName().endsWith("_")) {
				String entName = entityList.get(i).getName();
				Object entVal = entityList.get(i).getValue();
				// modify if it is already existing
				if (context.getCurrentTask().hasEntity(entityList.get(i))) {
					// context.getCurrentTask().addEntity(entityList.get(i));
					context.getCurrentTask().getEntities(entName)
							.setValue(entityMapping.get(entityList.get(i).getName()));
					// if the value is null better set the flag as
					// unfilled.
					if (entVal == null)
						context.getCurrentTask().getEntities(entName).setUnFilled();
				} else {
					// add since it is not existing
					context.getCurrentTask().addEntity(entityList.get(i));
					logger.info("adding global Entity " + entityList.get(i).getName() + " to task: " + t.getName());
					context.getCurrentTask().getEntities(entityList.get(i).getName())
							.setValue(entityMapping.get(entityList.get(i).getName()));
				}
			}
		}
		return t;
	}

	private void pushNEREntities() {
		for (int i = 0; i < NEREntities.size(); i++) {
			if (context.getCurrentTask().hasEntity(NEREntities.get(i).getValue0())) {
				context.getCurrentTask().getEntities(NEREntities.get(i).getValue0())
						.setValue(NEREntities.get(i).getValue1());
				String start = NEREntities.get(i).getValue3().split("-")[0];
				String end = NEREntities.get(i).getValue3().split("-")[1];
				logger.info("[NLP_ENGINE] a parser matched for entity: " + NEREntities.get(i).getValue0() + " value: "
						+ NEREntities.get(i).getValue1() + " sequence: " + start + "-" + end + " ("
						+ NEREntities.get(i).getValue2() + ")");
			}
		}
	}

	private String loadProerty(Properties prop, String propertName) {
		String ret = "";
		if (prop.containsKey(propertName)) {
			ret = prop.getProperty(propertName);
		} else
			logger.severe(" missing " + propertName + " propoerty in bot.properties");
		return ret;
	}

	@Override
	public void sendNotification(String id, String message) {
		// TODO Auto-generated method stub
		Notification notification = new Notification(id, message);
		notificationList.add(notification);
	}

	@Override
	public String getNotification() {
		// TODO Auto-generated method stub
		try {
			StringWriter out = new StringWriter();
			JSONObject response = new JSONObject();
			JSONArray notifications = new JSONArray();
			for (int i = 0; i < notificationList.size(); i++) {
				JSONObject notification = new JSONObject();
				notification.put("user", entityMapping.get("loginUser_"));
				notification.put("role", entityMapping.get("loginRole_"));
				notification.put("authToken", entityMapping.get("authToken_"));
				notification.put("sessionId", context.getAdditionalDebugInfo("sessionID"));
				notification.put("timeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				notification.put("language", context.getLocale().getLanguage());
				notification.put("source", context.getDialog().getName());
				notification.put("message", notificationList.get(i).getMessage());
				notifications.put(notification);
			}
			// clear once notifications are read
			notificationList.clear();

			if (notifications.length() >= 1)
				response.put("notifications", notifications);
			else
				response.put("notifications", "NA");

			response.write(out);
			return out.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
