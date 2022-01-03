package cto.hmi.model.definition;

public abstract class MessageModel {

	// serializable members
	protected String chat;
	protected String info;
	protected String error;

	// Constructors
	public MessageModel() {

	}

	public String getChat() {
		return chat;
	}

	public void setChat(String chat) {
		this.chat = chat;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
