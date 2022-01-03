package cto.hmi.bot.licensemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class ValidateLicense {
	public static void main(String[] args) {
		String propertiesFile = new File(".").getAbsolutePath()
				+ "/res/config/license.properties";
		Properties prop = new Properties();
		InputStream input;
		License license = null;
		license = License.getInstance();

		Date date = new Date();

		try {
			input = new FileInputStream(propertiesFile);
			prop.load(input);

			String data = prop.getProperty("EMAIL").toLowerCase() + ","
					+ prop.getProperty("COMPANY").toLowerCase() + ","
					+ prop.getProperty("LICENSE_TYPE").toLowerCase() + ","
					+ prop.getProperty("EXPIRATION") + ","
					+ prop.getProperty("VERSION");

			license.setProperty("EXPIRATION", prop.getProperty("EXPIRATION"));
			license.setProperty("LICENSE_TYPE",
					prop.getProperty("LICENSE_TYPE").toLowerCase());
			license.setProperty("COMPANY", prop.getProperty("COMPANY").toLowerCase());

			System.out.println(data);

			Boolean isValid;
			isValid = new VerLicenseKey().Verify(data);
			if (isValid) {
				System.out.println("Found valid data signature");
				license.validateExpiration(date);
				// Send the company as read from DDF file
				license.validateCompany("Connecticus");
				System.out.println("Found valid license");
			} else
				System.out
						.println("SEVERE: No valid license. Please contact IT admin");
		} catch (IOException | LicenseExpiredException
				| LicenseInvalidCompanyException e) {
			// TODO Auto-generated catch block
			System.out.println("SEVERE: License invalid or expired !!");
		}

	}
}
