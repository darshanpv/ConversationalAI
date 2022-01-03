package cto.hmi.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import cto.hmi.bot.util.LoggerUtil;

public class GetDomainAnswer {
	private final static Logger logger = LoggerUtil.getLogger();
	public static String PROPERTY_FILE = "/res/config/bot.properties";

	public GetDomainAnswer() {
		super();
	}

	public String get(String utterance) {
		Properties prop = new Properties();
		String answer = "DOMAIN_NO_ANSWER";
		Boolean useDomainFlag = false;
		String method = "get";
		String domainURL = "";

		String path = new File(".").getAbsolutePath();
		String propertiesFile = path + PROPERTY_FILE;

		try {
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			useDomainFlag = (prop.getProperty("USE_DOMAIN").toLowerCase()
					.equals("true"));
			method = prop.getProperty("DOMAIN_URL_METHOD").toLowerCase();
			domainURL = prop.getProperty("DOMAIN_URL");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.severe("Error: failed to preocess the domain url." + e);
		}

		if (utterance.split("\\s+").length > 2 && useDomainFlag) {
			HttpResponse response = null;
			if (method.equals("get")) {
				try {

					HttpClient client = new DefaultHttpClient();
					HttpParams httpParams = client.getParams();
					HttpConnectionParams
							.setConnectionTimeout(httpParams, 20000);
					HttpGet request = new HttpGet(domainURL + "?userUtterance="
							+ URLEncoder.encode(utterance, "UTF-8"));
					response = client.execute(request);
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));

					StringBuffer result = new StringBuffer();
					String line = "";
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}
					rd.close();
					int status = response.getStatusLine().getStatusCode();
					if (result.toString().startsWith("{")
							&& (status == 200 || status == 201)) {
						JSONObject json = new JSONObject(result.toString()
								.trim());
						if (json.has("response")) {
							if (!json.getString("response").toLowerCase()
									.equals("na"))
								answer = json.getString("response");
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (method.equals("post")) {
				try {
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost(domainURL + "?userUtterance="
							+ URLEncoder.encode(utterance, "UTF-8"));

					post.setHeader("User-Agent", "Mozilla/5.0");
					response = client.execute(post);
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));
					StringBuffer result = new StringBuffer();
					String line = "";
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}
					rd.close();
					int status = response.getStatusLine().getStatusCode();
					if (result.toString().startsWith("{")
							&& (status == 200 || status == 201)) {
						JSONObject json = new JSONObject(result.toString()
								.trim());
						if (json.has("response")) {
							if (!json.getString("response").toLowerCase()
									.equals("na"))
								answer = json.getString("response");
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.severe("failed to preocess the domain url." + e);
				}
			}
		}

		return answer;
	}
}
