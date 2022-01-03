package cto.hmi.bot.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class LogForActivity {
	public static String LOG_LOCATION = "/res/logs/activity/";

	public static void logData(String domain, String user, String task, String actionType) {

		try {
			DateFormat mm = new SimpleDateFormat("MMM_dd", Locale.US);
			String currentDate = mm.format(new Date());
			String logFileName = new File(".").getAbsolutePath() + LOG_LOCATION + currentDate.substring(0, 3) + "/"
					+ currentDate + "_activity.log";
			File logFile = new File(logFileName);
			if (logFile.exists()) {
				// check if it exists for more than 1 days, which means its a
				// older file and needs to be created fresh
				long lDiff = new Date().getTime() - logFile.lastModified();
				if ((TimeUnit.DAYS.convert(lDiff, TimeUnit.MILLISECONDS)) > 1) {
					// file is older than a day and hence circulate -> need to
					// be deleted
					// and recreated
					logFile.delete();
					logFile.createNewFile();
				}
			} else {
				logFile.createNewFile();
			}

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = new Date();

			// trim the domain to get the name
			String domainName = domain.substring((domain.lastIndexOf("/")) + 1);
			domainName = domainName.substring(0, domainName.length() - 4);
			String data = dateFormat.parse(dateFormat.format(date)) + " domain:" + domainName + " user:" + user
					+ " task:" + task + " TYPE:" + actionType + "\n";
			// check if file exists
			logFile = new File(logFileName);
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8"));
			bw.write(data);
			bw.close();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
