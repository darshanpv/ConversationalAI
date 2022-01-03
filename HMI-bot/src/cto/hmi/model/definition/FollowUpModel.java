package cto.hmi.model.definition;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import cto.hmi.model.adapters.MapAdapter;
import cto.hmi.processor.dialogmodel.Entity;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class FollowUpModel {

	protected Entity entity;
	@XmlJavaTypeAdapter(MapAdapter.class) 
	protected HashMap<String, String> answerMapping=new HashMap<String, String>();
	
	public FollowUpModel(){
	}
	
	public Entity getEntity() {
		return entity;
	}
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	
	public HashMap<String, String> getAnswerMapping() {
		return answerMapping;
	}

	public void setAnswerMapping(HashMap<String, String> answerMapping) {
		this.answerMapping = answerMapping;
	}
}
