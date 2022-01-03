package cto.hmi.processor.licensemanager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.processor.exceptions.LicenseException;
import cto.hmi.processor.exceptions.LicenseExpiredException;
import cto.hmi.processor.exceptions.LicenseInvalidCompanyException;
import cto.hmi.processor.exceptions.LicenseVersionExpiredException;

public class License {

	public static final String COMPANY = "COMPANY";
	public static final String EMAIL = "EMAIL";
	public static final String EXPIRATION = "EXPIRATION";
	public static final String LICENSE_TYPE = "LICENSE_TYPE";
	public static final String TYPE_LIFETIME = "LIFE_TIME";
	public static final String TYPE_SINGLE_VERSION = "SINGLE_TIME";
	public static final String TYPE_TRIAL = "TRIAL";
	public static final String VERSION = "VERSION";
	public static final Integer EXPIRY_ALERT_DAYS = 30;
	private static License instance;
	private Map<String, String> properties;

	private License() {
		this.properties = new HashMap<String, String>();
	}

	public static License getInstance() {

		if (instance == null) {
			instance = new License();
		}
		return instance;
	}

	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(this.properties);
	}

	public Date getExpiration() {
		String value = getProperty(EXPIRATION);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return df.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}

	public void setExpiration(Date expiration) {
		if (expiration == null) {
			setProperty(EXPIRATION, null);
		} else {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			setProperty(EXPIRATION, df.format(expiration));
		}
	}

	public void setProperty(String key, String value) {
		if (value == null) {
			this.properties.remove(key);
		} else {
			this.properties.put(key, value);
		}
	}

	public String getProperty(String key) {
		return this.properties.get(key);
	}

	public void validate(Date currentDate, String currentVersion, String company) throws LicenseException {

		validateExpiration(new Date());

		validateVersion(currentVersion);

		validateCompany(company);

	}

	protected void validateExpiration(Date currentDate) throws LicenseExpiredException {

		// The expiration date doesn't matter for a lifetime version.
		if (getProperty(LICENSE_TYPE).toLowerCase().equals(TYPE_TRIAL.toLowerCase())
				|| getProperty(LICENSE_TYPE).toLowerCase().equals(TYPE_SINGLE_VERSION.toLowerCase())) {
			// System.out.println("Got expiration date: "+ getExpiration());
			// System.out.println("Got current date: "+ currentDate);
			// System.out.println("Is it valid:
			// "+currentDate.after(getExpiration()));
			if (getExpiration() == null || currentDate.after(getExpiration())) {
				throw new LicenseExpiredException();
			}
		}

	}

	protected void validateCompany(String company) throws LicenseInvalidCompanyException {
		if (!getProperty(COMPANY).toLowerCase().equals(company.toLowerCase())) {
			throw new LicenseInvalidCompanyException();
		}

	}

	protected boolean aboutToExpire() {
		long difference = 0;
		if (!getProperty(LICENSE_TYPE).toLowerCase().equals(TYPE_LIFETIME.toLowerCase())) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			try {
				Date d1 = df.parse(df.format(date));
				Date d2 = this.getExpiration();
				difference = (d2.getTime() - d1.getTime()) / 86400000;
				if ((int) difference <= EXPIRY_ALERT_DAYS)
					return true;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	protected void validateVersion(String currentVersion) throws LicenseVersionExpiredException {

		if (TYPE_SINGLE_VERSION.equals(getProperty(LICENSE_TYPE))) {
			if (getProperty(VERSION) == null) {
				throw new LicenseVersionExpiredException();
			}
			Pattern pattern = Pattern.compile(getProperty(VERSION));
			Matcher matcher = pattern.matcher(currentVersion);
			if (!matcher.matches()) {
				throw new LicenseVersionExpiredException();
			}
		}

	}
}
