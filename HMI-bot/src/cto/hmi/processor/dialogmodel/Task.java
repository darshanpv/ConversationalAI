package cto.hmi.processor.dialogmodel;

import java.util.HashMap;
import java.util.Map;

import cto.hmi.model.definition.TaskModel;
import cto.hmi.processor.ui.ConsoleInterface;
import cto.hmi.processor.ui.RESTInterface;
import cto.hmi.processor.ui.UIConsumer;


public class Task extends TaskModel {

	public Task() {
		super();
	}

	public Task(String name) {
		super(name);
	}

	public Boolean isAllFilled() {
		for (Entity entity : this.getEntities()) {
			if (!entity.isFilled())
				return false;
		}
		return true;
	}

	public Boolean isMandatoryFilled() {
		for (Entity entity : this.getEntities()) {
			if (!entity.isFilled() && entity.isRequired())
				return false;
		}
		return true;
	}

	public String execute() {
		return getAction().executeAndGetAnswer(this);
	}

	public Frame toFrame() {
		Frame frame = new Frame();
		for (Entity entity : this.getEntities()) {
			if (entity.isFilled()) {
				frame.put(entity.getName(), entity.getValue());
			}
		}
		//this part adds if we have any global ITOs from ITO mapping to be added to frame
		// 1. you need to get the instance first
		UIConsumer instance = null;
		String sessionId = "";
		if (frame.get("sessionId_") != null)
			sessionId = (String) frame.get("sessionId_");
		// check if it is a console mode or rest interface mode
		if (!sessionId.equals("d1-DUMMYSESSION")) // rest interface mode
			instance = RESTInterface.getInstance(sessionId);
		else // console mode
			instance = ConsoleInterface.getInstance();
		// 2. get ITOMapping hashmap
		Map<String, String> entityMap = new HashMap<String, String>();
		entityMap = instance.getEntityMapping();
		//3. add the global ITOs to frame for filling the execution and get answer
		for (Map.Entry<String, String> entry : entityMap.entrySet()) {
			if (!frame.containsKey(entry.getKey()) && entry.getKey().endsWith("_")) {
				frame.put(entry.getKey(), entry.getValue());
			}
		}
		return frame;
	}

	public void reset() {
		for (Entity entity : this.getEntities()) {
			// added to avoid setting null value for global itos
			if (!entity.getName().endsWith("_")) {
				entity.setValue(null);
				entity.setUnFilled(); // need to be executed after setValue!
			}
		}
		if (followup != null) {
			Entity fupENtity;
			if ((fupENtity = followup.getEntity()) != null) {
				fupENtity.setValue(null);
				fupENtity.setUnFilled();
			}
		}
	}
}
