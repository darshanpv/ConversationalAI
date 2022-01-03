package cto.hmi.idatautil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.bot.util.LoggerUtil;

public class ProcessCardData {
	private final static Logger logger = LoggerUtil.getLogger();
	public static String GetJSONString(String fileName, String ID) {

		String response = "";
		String itemFile = new File(".").getAbsolutePath() + "/res/idata/" + fileName;
		File f = new File(itemFile);
		if (!f.exists()) {
			logger.warning("missing item file- " + itemFile.substring((itemFile.lastIndexOf("/")) + 1));
		} else {

			try {
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
				String line = "";
				ArrayList<String> lines = new ArrayList<String>();
				while ((line = br.readLine()) != null) {
					if (!line.isEmpty())
						lines.add(line);
				}
				br.close();

				Iterator<String> i = lines.iterator();
				String str = "";
				while (i.hasNext()) {
					ArrayList<String> ar = new ArrayList<String>();
					str = (String) i.next();

					String[] tokens = str.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
					for(String t : tokens) {
						ar.add(t.replaceAll("^\"|\"$", "").trim());
			        }
					
					if (!ar.get(0).equals(ID)) {
						i.remove();
					}
				}
				if (lines.size() > 1)
					logger.warning("found multiple cards for given ID. Picking the first one.");

				ArrayList<String> ar = new ArrayList<String>();
				String[] tokens = lines.get(0).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				for(String t : tokens) {
					ar.add(t.replaceAll("^\"|\"$", "").trim());
		        }

				// creating JSON record
				JSONObject info = new JSONObject();
				info.put("id", ar.get(0));
				info.put("text", ar.get(2));
				info.put("image", ar.get(3));
				info.put("video", ar.get(4));

				JSONObject data = new JSONObject();
				data.put("info", info);

				JSONObject message = new JSONObject();
				message.put("chat", ar.get(1));
				message.put("type", "iCardTextImageVideo");
				message.put("data", data);

				JSONObject res = new JSONObject();
				res.put("message", message);
				response = res.toString();

			} catch (IOException | IndexOutOfBoundsException | JSONException e) {
				// TODO Auto-generated catch block
				logger.severe("error while parsing the csv data file. Please check.");
				e.printStackTrace();
			}
		}
		return response;
	}

	public static String GetJSONString(String chat, String text, String imgURL, String videoURL) {

		String response = "";
		try {
			// creating JSON record
			JSONObject info = new JSONObject();
			info.put("id", "1");
			info.put("text", text);
			info.put("image", imgURL);
			info.put("video", videoURL);

			JSONObject data = new JSONObject();
			data.put("info", info);

			JSONObject message = new JSONObject();
			message.put("chat", chat);
			message.put("type", "iCardTextImageVideo");
			message.put("data", data);

			JSONObject res = new JSONObject();
			res.put("message", message);
			response = res.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.severe("error while parsing the data file. Please check.");
			e.printStackTrace();
		}

		return response;
	}
}
