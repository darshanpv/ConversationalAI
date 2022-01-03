package cto.hmi.bot.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerUtil {
	private final static Logger logger = Logger.getLogger("hmi_util");
	private static boolean init = false;


	private static void init() {
		init = true;
		// format logging
		logger.setUseParentHandlers(false);
		CustomFormatter fmt = new CustomFormatter();
		Handler ch = new ConsoleHandler();
		ch.setFormatter(fmt);
		logger.addHandler(ch);
		logger.setLevel(Level.INFO);
	}

	public static Logger getLogger() {
		if (!init)
			init();
		return logger;
	}

	public static class CustomFormatter extends Formatter {

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
}
