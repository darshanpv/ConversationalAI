package cto.hmi.bot.util;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProgressBar {
	private final static Logger logger = LoggerUtil.getLogger();
	private volatile static boolean exit = false;
	private static char[] animationChars = new char[] { '|', '/', '-', '\\' };

	public static void startPB(int Seconds, String message) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (int x = 0; x < (Seconds * 10); x++) {
						if (!exit) {
							String rep1 = new String(new char[x % 20]).replace("\0", "=");
							String rep2 = new String(new char[(20 - x % 20)]).replace("\0", " ");
							System.out.print(" "+ message + rep1 + ">" + rep2 + ":"
									+ animationChars[x % 4] + "\r");
							TimeUnit.MILLISECONDS.sleep(100);
						} else {
							Thread.currentThread().interrupt();
							while (!Thread.currentThread().isInterrupted()) {
								logger.info("timeout, stopped processing...");
							}
						}
					}
					if (!exit) {
						logger.severe(" system failed to process...");
						Thread.currentThread().interrupt();
						while (!Thread.currentThread().isInterrupted()) {
							logger.info("exiting processing");
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.severe("system failed to process...");
				}
			}
		}).start();
	}

	public static void stopPB() {
		exit = true;
	}
}