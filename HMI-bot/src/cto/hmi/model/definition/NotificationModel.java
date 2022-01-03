package cto.hmi.model.definition;

public abstract class NotificationModel {

	String notificationID = "";
	String notificationMessage = "";

	public NotificationModel() {
		// TODO Auto-generated constructor stub
	}

	public NotificationModel(String id, String message) {
		this.notificationID = id;
		this.notificationMessage = message;
	}

	public String getID() {
		return notificationID;
	}

	public void setID(String notificationID) {
		this.notificationID = notificationID;
	}

	public String getMessage() {
		return notificationMessage;
	}

	public void setMessage(String notificationMessage) {
		this.notificationMessage = notificationMessage;
	}
	
}
