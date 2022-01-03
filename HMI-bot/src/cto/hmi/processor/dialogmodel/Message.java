package cto.hmi.processor.dialogmodel;

import cto.hmi.model.definition.MessageModel;

public class Message extends MessageModel {
	private String message;

	public Message() {
		super();
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String value) {
		this.message = value;
	}
}
