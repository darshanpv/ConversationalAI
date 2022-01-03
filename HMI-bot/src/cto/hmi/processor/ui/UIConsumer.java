package cto.hmi.processor.ui;

import java.util.Date;
import java.util.Map;

import cto.hmi.processor.dialogmodel.Dialog;
import cto.hmi.processor.exceptions.ProcessingException;

public interface UIConsumer {

	public abstract void loadDialog(Dialog dialog);

	public abstract String getDialogXml();

	public abstract UIConsumerMessage processUtterance(String userUtterance) throws ProcessingException;

	public abstract String getDebugInfo();

	public abstract String getTaskInfo();

	public abstract String getDialogFailureInfo();

	public abstract String buildResponse(String query, String reply);

	public abstract String getDialogInfo();

	public abstract void clearDialogInfo();

	public abstract String getDialogDefinition();

	public abstract String getDebugInfo(String key);

	public abstract void setAdditionalDebugInfo(String key, String debuginfo);

	public abstract Date getLastAccess();

	public abstract String getIdentifier();

	public abstract void addtoEntityMapping(String key, String value);

	public abstract String processExecutionResult(String response);

	public abstract boolean isLicenseValid();

	public abstract void setEntityMapping(String ito, String value);

	public abstract Map<String, String> getEntityMapping();

	public abstract void registerLikeDislike(String option);

	public abstract void sendNotification(String id, String message);

	public abstract String getNotification();

	// public abstract Message getMessage();
	public class UIConsumerMessage {
		private String systemUtterance;
		private Meta meta;

		public enum Meta {
			QUESTION, ANSWER, UNCHANGED, END_OF_DIALOG, ERROR, REPEATEDQUESTION, POLLING
		};

		public UIConsumerMessage(String systemUtterance, Meta meta) {
			this.systemUtterance = systemUtterance;
			this.meta = meta;
		}

		public String getSystemUtterance() {
			return systemUtterance;
		}

		public Meta getMeta() {
			return meta;
		}
	}

}
