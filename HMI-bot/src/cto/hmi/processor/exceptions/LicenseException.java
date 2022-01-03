package cto.hmi.processor.exceptions;

import cto.hmi.processor.ConvEngineProcessor;

public class LicenseException extends Exception {

	private static final long serialVersionUID = 7895696254570225320L;

	public LicenseException() {
		super("Licensing Exception");
	}

	public LicenseException(String message) {
		super("Licensing Exception :" + message);
		ConvEngineProcessor.getLogger().severe(message);
	}

}
