package cto.hmi.processor.nlu.entityparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.jayway.jsonpath.JsonPath;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.NoParserFileFoundException;

public class UrlListParser_X extends Parser {
	static HttpClient client;
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public UrlListParser_X(String type) {
		super(type);
	}

	@Override
	public ParseResults parse(String utterance) throws NoParserFileFoundException {
		ParseResults results = new ParseResults(utterance);
		String path = new File(".").getAbsolutePath();
		String fname = (this.getType()).split("\\.")[1] + ".txt";
		String PROPERTY_FILE = "/res/entities/" + fname;
		String TEMP_DIR = "/res/temp/";
		String urlMethod = "";
		String itemURL = "";
		String params = "";
		String jPath = "";
		String cacheDays = "";
		String tempDir = path + TEMP_DIR;
		ArrayList<String> items = new ArrayList<String>();
		ArrayList<String> categories = new ArrayList<String>();
		String[] params_arr = null;
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream(path + PROPERTY_FILE);
			prop.load(input);
			urlMethod = prop.getProperty("URL_METHOD").toLowerCase();
			itemURL = prop.getProperty("URL");
			params = prop.getProperty("PARAMS");
			jPath = prop.getProperty("JPATH");
			cacheDays = prop.getProperty("CACHE_DAYS");

			// check if temp folder exist if not create it
			File directory = new File(tempDir);
			if (!directory.exists()) {
				directory.mkdir();
			}

			String tmpFile = tempDir + fname;
			Boolean callURL = true;
			if (new File(tmpFile).exists()) {
				File tFile = new File(tmpFile);
				long diff = new Date().getTime() - tFile.lastModified();
				if (!(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) >= Long.valueOf(cacheDays)))
					callURL = false;
			}
			if (callURL) {
				init();
				ContentResponse response;
				Request request;
				request = client.newRequest(itemURL);
				// choose method
				if (urlMethod.toLowerCase().equals("get")) {
					request.method(HttpMethod.GET);
				} else {
					request.method(HttpMethod.POST);
				}
				// process parameters if defined
				if (params != null) {
					params_arr = params.split("&");
					String[] key_value;
					for (String paramPair : params_arr) {
						key_value = paramPair.split("=");
						if (key_value.length > 1) {
							// check if key_value is referring to ito e.g.
							// city=%getDepartureCity
							String urlParamValue = key_value[1];
							// *****Need fix******
							// urlParamValue =
							// DialogManager.fillUrlParamWithITO(urlParamValue);
							request.param(key_value[0], urlParamValue);
						} else
							request.param(key_value[0], "");
					}
				}
				logger.info("requesting: " + request.getURI() + ", " + request.getParams().toString());
				response = request.send();
				logger.info("HTTP status: " + response.getStatus());
				String body = response.getContentAsString();
				client.stop();

				logger.info("HTTP response: " + body.replace("\r\n", "").trim());
				if (jPath.replaceAll("\\s", "").length() > 0) {
					String jsonExp = jPath;
					items = JsonPath.read(body, jsonExp);
				}
				FileWriter writer = new FileWriter(tmpFile);
				writer.write("##DO NOT REMOVE THIS LINE\n");
				if (items.size() > 0) {
					logger.info("got items: " + items.toString());
					for (String str : items.get(0).split(",")) {
						writer.write(str.trim() + "\n");
					}
				}
				writer.close();
			}
			File f = new File(tmpFile);
			if (!f.exists()) {
				// do something
				throw new NoParserFileFoundException(
						"missing urlList file in temp folder - " + tmpFile.substring((tmpFile.lastIndexOf("/")) + 1));
			}
			items.clear();
			categories.clear();
			FileInputStream fstream;
			fstream = new FileInputStream(tmpFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// check if it just empty line
				if (!(strLine.trim().length() > 0))
					continue;
				int index = 0;
				String item = "";
				String category = "";
				if (!strLine.startsWith("#") && strLine.contains("="))
					index = strLine.indexOf("=");

				if (index > 0) {
					// populate category as auto and item as car e.g. car=auto
					category = strLine.substring(index + 1, strLine.length()).trim();
					item = strLine.substring(0, index).trim();
				} else {
					// same goes for item and category
					item = strLine.trim();
					category = item;
				}
				if (!item.startsWith("#") && utterance.matches("(?i)^.*?\\b" + item + "\\b.*?")) {
					items.add(item);
					categories.add(category);
				}
			}
			// Close the input stream
			br.close();

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return results;
		}

		// the selection will take only first matching word
		if (items.size() >= 1) {
			int index = utterance.toLowerCase().indexOf(items.get(0).toLowerCase());
			// add category to ITO model
			results.add(new ParseResult(this.name, index, index + items.get(0).length() - 1, items.get(0).toLowerCase(),
					this.type, Character.toString(categories.get(0).charAt(0)).toUpperCase()
							+ categories.get(0).toLowerCase().substring(1)));
		}

		return results;
	}

	private static void init() {
		// SslContextFactory sslContextFactory = new SslContextFactory();
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		client = new HttpClient(sslContextFactory);
		try {
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
