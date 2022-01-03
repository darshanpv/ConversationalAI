package cto.hmi.bot.licensemanager;

import java.io.Console;
import java.util.Scanner;

public class CreateLicense {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		String lEmail = "";
		String lCompany = "";
		String lType = "";
		String lDate = "";
		String lVersion = "";
		boolean isLtypeCorrect = false;
		boolean isPasswordCorrect = false;
		boolean isDateCorrect = false;
		boolean isVersionCorrect = false;
		License license = null;
		Scanner reader = new Scanner(System.in); // Reading from System.in
		license = License.getInstance();
		Console console = System.console();

		while (!isPasswordCorrect) {
			System.out.println("Enter password: ");
			char[] passString = console.readPassword();
			String passwd = new String(passString);
			if (passwd.equals("bot@PSL123"))
				isPasswordCorrect = true;
			else
				System.out.println("You entered wrong password");
		}
		System.out.println("Enter a email[abc@yourcompany.com]: ");
		lEmail = reader.next();
		System.out.println("Enter a company[xyz]: ");
		lCompany = reader.next();
		while (!isLtypeCorrect) {
			System.out
					.println("Enter license type[1-LIFE_TIME, 2-SINGLE_TIME, 3-TRIAL]: ");
			lType = reader.next();
			if (lType.equals("1") || lType.equals("2") || lType.equals("3")) {
				isLtypeCorrect = true;
				if (lType.equals("1"))
					lType = "LIFE_TIME";
				if (lType.equals("2"))
					lType = "SINGLE_TIME";
				if (lType.equals("3"))
					lType = "TRIAL";
				if (lType.equals("LIFE_TIME")) {
					lDate = "0000-00-00";
				} else {
					while (!isDateCorrect) {
						System.out
								.println("Enter a expiration date[yyyy-mm-dd]: ");
						lDate = reader.next();
						if (lDate
								.matches("^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01])$"))
							isDateCorrect = true;
						if (!isDateCorrect)
							System.out.println("Please enter correct date");
					}
				}
				while (!isVersionCorrect) {
					System.out.println("Enter a version[1.2]: ");
					lVersion = reader.next();
					if (lVersion.matches("^\\d{1}\\.\\d{1}$"))
						isVersionCorrect = true;
					if (!isVersionCorrect)
						System.out.println("Please enter correct version");
				}
			}
			if (!isLtypeCorrect)
				System.out.println("Please enter license type as 1,2 or 3");
		}

		license.setProperty("EMAIL", lEmail.toLowerCase());
		license.setProperty("COMPANY", lCompany.toLowerCase());
		// license.setProperty("LICENSE_TYPE", "LIFE_TIME");
		license.setProperty("LICENSE_TYPE", lType.toLowerCase());
		// license.setProperty("LICENSE_TYPE", "TRIAL");
		// Date format yyyy-MM-dd
		license.setProperty("EXPIRATION", lDate);
		license.setProperty("VERSION", lVersion);

		String data = license.getProperty("EMAIL") + ","
				+ license.getProperty("COMPANY") + ","
				+ license.getProperty("LICENSE_TYPE") + ","
				+ license.getProperty("EXPIRATION") + ","
				+ license.getProperty("VERSION");

		if (new GenLicenseKey().GenKey(data))
			System.out
					.println("LICENSE_MANAGER: License key generated for:\n\n"
							+ "EMAIL=" + license.getProperty("EMAIL") + "\n"
							+ "COMPANY=" + license.getProperty("COMPANY")
							+ "\n" + "LICENSE_TYPE="
							+ license.getProperty("LICENSE_TYPE") + "\n"
							+ "EXPIRATION=" + license.getProperty("EXPIRATION")
							+ "\n" + "VERSION="
							+ license.getProperty("VERSION") + "\n");
	}

}
