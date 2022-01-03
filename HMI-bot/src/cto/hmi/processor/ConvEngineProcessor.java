/*
BOT Question Answer System
CTO-2016 
 */

package cto.hmi.processor;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cto.hmi.bot.util.AIMLProcessor;
import cto.hmi.bot.util.StopwordProcessor;
import cto.hmi.ner.NerProcessor;
import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.exceptions.RuntimeError;
import cto.hmi.processor.manager.DialogManager;
import cto.hmi.processor.ui.ConsoleInterface;
import cto.hmi.processor.ui.RESTInterface;
import cto.hmi.processor.ui.UIConsumer;
import cto.hmi.processor.ui.UIConsumerFactory;
import cto.hmi.processor.ui.UserInterface;
import cto.hmi.server.checkServerStatus;

public class ConvEngineProcessor extends UIConsumerFactory {

	private final static Logger logger = Logger.getLogger("hmi_bot");
	private static boolean init = false;
	private static ConvEngineConfig config = ConvEngineConfig.getInstance();
	private static String default_dialog = config.getProperty(ConvEngineConfig.DIALOGUEDIR) + "/" + "dummy.xml"; // default
																													// dialogue
	private static UserInterface ui;
	private static String jettyPort = "8080";
	private static Boolean isUseBroker = false;
	private static Date startedOn;
	public static NerProcessor nerProcessor;
	// public static ExudeData exudeInstance;
	public static StopwordProcessor stopwordProcessor;
	public static AIMLProcessor aimlProcessor;

	/**
	 * @param args
	 */

	public static void main(String[] args) {

		Class<? extends UserInterface> ui_class = ConsoleInterface.class; // default
																			// UI
		String dialog_file = default_dialog; // default dialogue

		// process command line args
		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print this message");

		cli_options.addOption(Option.builder("i").longOpt("interface").desc("select user interface").hasArg(true)
				.argName("console, rest").build());

		cli_options.addOption("f", "file", true, "specify dialogue path and file, e.g. -f /res/dialogue1.xml");
		cli_options.addOption("r", "resource", true, "load dialogue (by name) from resources, e.g. -r dialogue1");
		cli_options.addOption("p", "port", true, "port on which Jetty server runs, e.g. -p 8080");
		cli_options.addOption("b", "broker", false, "use broker services");

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(cli_options, args);

			// Help
			if (cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("bot", cli_options, true);
				return;
			}

			// UI
			if (cmd.hasOption("i")) {
				String interf = cmd.getOptionValue("i");
				if (interf.equals("console"))
					ui_class = ConsoleInterface.class;
				else if (interf.equals("rest"))
					ui_class = RESTInterface.class;
			}

			// load dialogue from path file
			if (cmd.hasOption("f")) {
				dialog_file = "file:///" + cmd.getOptionValue("f");
			}
			// load dialogue from resources
			if (cmd.hasOption("r")) {
				dialog_file = config.getProperty(ConvEngineConfig.DIALOGUEDIR) + "/" + cmd.getOptionValue("r") + ".xml";
			}
			// load dialogue from internal store
			if (cmd.hasOption("p")) {
				jettyPort = cmd.getOptionValue("p");
			}
			// load dialogue from internal store
			if (cmd.hasOption("b")) {
				logger.info("use of broker services enabled");
				isUseBroker = true;
			}

		} catch (ParseException e1) {
			logger.severe("loading by main-method failed. " + e1.getMessage());
			e1.printStackTrace();
		}

		// start HMI with selected UI
		default_dialog = dialog_file;
		ConvEngineProcessor hmi = new ConvEngineProcessor();
		try {
			ui = ui_class.getDeclaredConstructor().newInstance();
			ui.register(hmi);
			ui.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ConvEngineProcessor() {
		if (!init)
			init();
	}

	// for external loading (war)
	public static void loadByWar(UserInterface uinterf) {
		default_dialog = config.getProperty(ConvEngineConfig.DIALOGUEDIR) + "/" + "dummy2.xml";
		ConvEngineProcessor hmi = new ConvEngineProcessor();
		try {
			ui = uinterf;
			ui.register(hmi);
			// ui.start();
		} catch (Exception e) {
			logger.severe("loading by war failed. " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static Dialog getDialog() {
		return Dialog.loadFromPath(default_dialog);
	}

	public static String getDefaultDialogPathAndName() {
		return default_dialog;
	}

	public static String getJettyPort() {
		return jettyPort;
	}

	public static String getUIType() {
		return ui.getClass().getSimpleName();
	}

	public static boolean getUseBrokerFlag() {
		return isUseBroker;
	}

	public static Date getStartedOn() {
		return startedOn;
	}

	public static boolean isInit() {
		return init;
	}

	@Override
	public UIConsumer create() throws RuntimeError {
		return new DialogManager();
	}

	@Override
	public UIConsumer create(Dialog d) throws RuntimeError {
		return new DialogManager(d);
	}

	private void init() {
		init = true;
		startedOn = new Date();
		// format logging
		logger.setUseParentHandlers(false);
		CustomFormatter fmt = new CustomFormatter();
		Handler ch = new ConsoleHandler();
		ch.setFormatter(fmt);
		logger.addHandler(ch);
		logger.setLevel(Level.INFO);

		// ping if the servers are reachable
		pingServer();
		// load the NER classifier
		getNer();
		// load the stopword removal service
		getStopword();
		// load aiml chat Session
		getAIML();
	}

	public static Logger getLogger() {
		return logger;
	}

	public class CustomFormatter extends Formatter {

		public String format(LogRecord record) {

			StringBuffer sb = new StringBuffer();
			sb.append(record.getLevel().getName());
			sb.append(" (");
			sb.append(record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf('.') + 1));
			sb.append("): ");
			sb.append(formatMessage(record));
			sb.append("\n");

			return sb.toString();
		}
	}

	private void pingServer() {
		// TODO Auto-generated method stub
		checkServerStatus.check();
	}

	public void getNer() {
		// Added to initialize the NER classifier
		nerProcessor = new NerProcessor();
		logger.info("NER classifier loaded");
	}

	public void getStopword() {
		// Added to initialize the stopword removal processor
		stopwordProcessor = StopwordProcessor.getInstance();
		logger.info("stopword processor instance created");

	}

	public void getAIML() {
		aimlProcessor = AIMLProcessor.getInstance();
		logger.info("AIML chat session instance created");
	}

}
