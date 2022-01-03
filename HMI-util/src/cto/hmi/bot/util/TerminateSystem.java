package cto.hmi.bot.util;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TerminateSystem {
	private final static Logger logger = LoggerUtil.getLogger();

	public static void terminate(String message) {
		try {
			logger.severe(message);
			logger.severe("terminating the application with exit status 1");
			TimeUnit.SECONDS.sleep(5);
			System.exit(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.severe("terminating the application with exit status 1");
		}

	}
}
