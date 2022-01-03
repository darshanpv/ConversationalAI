package cto.hmi.processor.nlu.entityparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONObject;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.NoParserFileFoundException;

public class ClassifierParser_X extends Parser {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public ClassifierParser_X(String type) {
		super(type);
	}

	@Override
	public ParseResults parse(String utterance) throws NoParserFileFoundException {

		ParseResults results = new ParseResults(utterance);
		String path = new File(".").getAbsolutePath();
		String identifiedClass = "";
		Float score = (float) 0.0;
		Float thresholdScore = (float) 0.0;
		String domain = (this.getType()).split("\\.")[1];
		String dataFile = path + "/res/entities/classifier/data/" + domain + ".json";
		File f = new File(dataFile);
		if (!f.exists()) {
			// do something
			throw new NoParserFileFoundException(
					"Missing item file- " + dataFile.substring((dataFile.lastIndexOf("/")) + 1));
		}
		String trainModelFile = path + "/res/entities/classifier/model/" + domain;
		File intentFile = new File(trainModelFile + "_intent.m");
		File svdFile = new File(trainModelFile + "_svd.m");
		File tfidfVecFile = new File(trainModelFile + "_tfidfVec.m");
		File trainLSAFile = new File(trainModelFile + "_trainLSA.m");
		File utteranceFile = new File(trainModelFile + "_utterance.m");
		if (!intentFile.exists() || !svdFile.exists() || !tfidfVecFile.exists() || !trainLSAFile.exists()
				|| !utteranceFile.exists()) {
			logger.info("Missing classifier trained Model for domain "
					+ trainModelFile.substring((trainModelFile.lastIndexOf("/")) + 1) + ". Will train the model");

			try {
				String TRAIN_PYTHON_FILE = "/res/entities/classifier/trainModel.py";
				String trainPythonFile = path + TRAIN_PYTHON_FILE;
				String line;
				StringBuilder everything = new StringBuilder();
				ProcessBuilder pb = new ProcessBuilder("python", trainPythonFile, domain);
				pb.redirectErrorStream(true);
				Process p = pb.start();
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
				while ((line = in.readLine()) != null) {
					everything.append(line + "\n");
				}

				String ret = everything.toString().trim();

				if (ret.trim().isEmpty())
					logger.severe("Someting went wrong in processing your intent data");
				else
					logger.info(ret.trim());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.severe("The model could not be trained.");
			}
		}
		try {
			// read the threshold score from properties file
			String PROPERTY_FILE = "/res/entities/classifier/config/classifier.properties";
			String PREDICT_PYTHON_FILE = "/res/entities/classifier/predictModel.py";
			String predictFile = path + PREDICT_PYTHON_FILE;

			Properties prop = new Properties();
			InputStream input = new FileInputStream(path + PROPERTY_FILE);
			prop.load(input);
			thresholdScore = Float.valueOf(prop.getProperty("THRESHOLD_SCORE"));

			// predict the utterance class
			String line;
			StringBuilder everything = new StringBuilder();
			String userUtterance = utterance;
			ProcessBuilder pb = new ProcessBuilder("python", predictFile, domain, userUtterance);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));

			while ((line = in.readLine()) != null) {

				everything.append(line + "\n");
			}
			String ret = everything.toString().trim();
			logger.info("Classsifier Response: " + ret);
			JSONObject responseObj = new JSONObject(ret);
			if (responseObj.has("class_1"))
				identifiedClass = responseObj.getString("class_1");
			if (responseObj.has("score_1"))
				score = Float.valueOf(responseObj.getString("score_1"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// the selection will take only first matching word
		if (score >= thresholdScore) {
			int index_1 = 0;
			int index_2 = 0;
			if (utterance.contains(";"))
				index_2 = utterance.toLowerCase().indexOf(";");
			else
				index_2 = utterance.length();
			String matchedText = utterance.substring(index_1, index_2);
			// add category to ITO model
			results.add(new ParseResult(this.name, index_1, index_2, matchedText,
					this.type, "Class:"+identifiedClass.trim()+" Utterance:"
							+ matchedText));
		}

		return results;
	}

}
