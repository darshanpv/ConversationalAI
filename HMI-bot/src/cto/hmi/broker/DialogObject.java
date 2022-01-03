package cto.hmi.broker;

public class DialogObject {

	private String messageId = "";
	private String domain = "";
	private String locale = "";
	private String userUtterance = "";
	private String message = "";

	public DialogObject() {

	}

	public DialogObject(String messageId, String domain, String locale, String userUtterance, String message) {
		this.messageId = messageId;
		this.domain = domain;
		this.locale = locale;
		this.userUtterance = userUtterance;
		this.message = message;
	}

	public DialogObject(String messageId, String domain, String locale) {
		this.messageId = messageId;
		this.domain = domain;
		this.locale = locale;
	}

	public DialogObject(String messageId, String domain, String locale, String userUtterance) {
		this.messageId = messageId;
		this.domain = domain;
		this.locale = locale;
		this.userUtterance = userUtterance;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getUserUtterance() {
		return userUtterance;
	}

	public void setUserUtterance(String userUtterance) {
		this.userUtterance = userUtterance;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "{" + "\"messageId\":\"" + messageId + "\"," + "\"domain\":\"" + domain + "\"," + "\"locale\":\""
				+ locale + "\"," + "\"userUtterance\":\"" + userUtterance + "\"," + "\"message\":\"" + message + "\""
				+ "}";

	}

}