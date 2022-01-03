package cto.hmi.processor.licensemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.exceptions.LicenseExpiredException;
import cto.hmi.processor.exceptions.LicenseInvalidCompanyException;

public class LicenseProcessor {
	private final static Logger logger = ConvEngineProcessor.getLogger();

	public boolean validate(String company) {

		String propertiesFile = new File(".").getAbsolutePath() + "/res/config/license.properties";
		Properties prop = new Properties();
		InputStream input;
		License license = null;
		license = License.getInstance();

		Date date = new Date();

		try {
			input = new FileInputStream(propertiesFile);
			prop.load(input);

			String data = prop.getProperty("EMAIL").toLowerCase() + ","
			// + prop.getProperty("COMPANY") + ","
					+ company.toLowerCase() + "," + prop.getProperty("LICENSE_TYPE").toLowerCase() + ","
					+ prop.getProperty("EXPIRATION") + "," + prop.getProperty("VERSION");

			license.setProperty("EXPIRATION", prop.getProperty("EXPIRATION"));
			license.setProperty("LICENSE_TYPE", prop.getProperty("LICENSE_TYPE"));
			license.setProperty("COMPANY", prop.getProperty("COMPANY"));
			Boolean isValid;
			isValid = new VerLicenseKey().Verify(data);
			if (isValid) {
				logger.info("found valid data signature");
				license.validateExpiration(date);
				// Send the company as read from DDF file
				license.validateCompany(company);
				logger.info("found valid license");
				return true;
			} else
				logger.severe("***IMPORTANT:no valid license, please contact IT admin");
			return false;
		} catch (IOException | LicenseExpiredException | LicenseInvalidCompanyException e) {
			// TODO Auto-generated catch block
			logger.severe("***IMPORTANT:license invalid or expired !!");
			return false;
		}

	}

	public boolean aboutToExpire() {
		License license = null;
		license = License.getInstance();
		return license.aboutToExpire();
	}
}
