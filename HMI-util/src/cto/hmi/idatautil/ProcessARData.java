package cto.hmi.idatautil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cto.hmi.bot.util.LoggerUtil;

public class ProcessARData {
	private final static Logger logger = LoggerUtil.getLogger();
	
	public static String GetJSONString(String fileName, String ID, int record) {
		String response = "";
		String recorNumber = "";
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
				// clamp record to minimum
				if (record <= 0)
					record = 1;
				// clamp record to maximum
				if (record >= lines.size())
					record = lines.size();
				// process the requested line
				line = lines.get(record - 1);

				if (record == 1)
					recorNumber = "first";
				else if (record >= lines.size())
					recorNumber = "last";
				else
					recorNumber = String.valueOf(record);

				ArrayList<String> ar = new ArrayList<String>();
				
				String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				for(String t : tokens) {
					ar.add(t.replaceAll("^\"|\"$", "").trim());
		        }
				
				int numberOfOverlayItems = (int) ((ar.size() - 7) / 6);

				// creating JSON record
				JSONArray overlayItems = new JSONArray();
				int pointer = 7;
				for (int n = 0; n < numberOfOverlayItems; n++) {
					JSONObject item_1 = new JSONObject();
					item_1.put("id", ar.get(pointer++));
					item_1.put("imageOrText", ar.get(pointer++));
					item_1.put("position", ar.get(pointer++));
					item_1.put("rotation", ar.get(pointer++));
					item_1.put("scale", ar.get(pointer++));
					item_1.put("color", ar.get(pointer++));
					overlayItems.put(item_1);
				}
				JSONObject panelInfo = new JSONObject();
				panelInfo.put("step", recorNumber);
				panelInfo.put("id", ar.get(0));
				panelInfo.put("label", ar.get(1));
				panelInfo.put("info", ar.get(2));
				panelInfo.put("position", ar.get(3));
				panelInfo.put("rotation", ar.get(4));
				panelInfo.put("scale", ar.get(5));
				panelInfo.put("target", ar.get(6));

				JSONObject ARdata = new JSONObject();
				ARdata.put("panelInfo", panelInfo);
				ARdata.put("overlayItems", overlayItems);

				JSONObject message = new JSONObject();
				message.put("chat", ar.get(2));
				if (lines.size() > 1)
					message.put("type", "ARCardStepGuide");
				else
					message.put("type", "ARCardTrainGuide");
				message.put("data", ARdata);

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

	// this methid can create JSON response for given csv string and type
	// e.g. GetJSONString("GLUCO_USE,\"STEP-5\",\"Your blood meter reading will
	// be available now on screen. You may take expert assistance by selecting
	// that option.\",ID-5,\"Your
	// Reading\",\"4.7,3.72,0\",42,0,\"255,255,0,255\",Glucometer,ID-6,\"https://192.168.0.103:8080/content/arrow_small.png\",\"4.1,2.72,0\",1,270,\"\",Glucometer",
	// "ARCardTrainGuide");
	public static String GetJSONString(String csvData, String type) {
		String response = "";
		ArrayList<String> ar = new ArrayList<String>();
		String[] tokens = csvData.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		for(String t : tokens) {
			ar.add(t.replaceAll("^\"|\"$", "").trim());
        }
		
		int numberOfOverlayItems = (int) ((ar.size() - 7) / 6);
		try {
			// creating JSON record
			JSONArray overlayItems = new JSONArray();
			int pointer = 7;
			for (int n = 0; n < numberOfOverlayItems; n++) {
				JSONObject item_1 = new JSONObject();
				item_1.put("id", ar.get(pointer++));
				item_1.put("imageOrText", ar.get(pointer++));
				item_1.put("position", ar.get(pointer++));
				item_1.put("rotation", ar.get(pointer++));
				item_1.put("scale", ar.get(pointer++));
				item_1.put("color", ar.get(pointer++));
				overlayItems.put(item_1);
			}
			JSONObject panelInfo = new JSONObject();
			panelInfo.put("step", "first");
			panelInfo.put("id", ar.get(0));
			panelInfo.put("label", ar.get(1));
			panelInfo.put("info", ar.get(2));
			panelInfo.put("position", ar.get(3));
			panelInfo.put("rotation", ar.get(4));
			panelInfo.put("scale", ar.get(5));
			panelInfo.put("target", ar.get(6));

			JSONObject ARdata = new JSONObject();
			ARdata.put("panelInfo", panelInfo);
			ARdata.put("overlayItems", overlayItems);

			JSONObject message = new JSONObject();
			message.put("chat", ar.get(2));

			message.put("type", type);
			message.put("data", ARdata);

			JSONObject res = new JSONObject();
			res.put("message", message);
			response = res.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.severe("error while parsing the csv data file. Please check.");
			e.printStackTrace();
		}
		return response;
	}

	public static int NoOfARRecords(String fileName, String ID) {

		int noOfRecords = 0;
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
						ar.add(t.replaceAll("^\"|\"$", ""));
			        }
					
					if (!ar.get(0).equals(ID)) {
						i.remove();
					}
				}

				noOfRecords = lines.size();

			} catch (IOException | IndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				logger.severe("error while parsing the csv data file. Please check.");
				e.printStackTrace();
			}
		}
		return noOfRecords;
	}
}
