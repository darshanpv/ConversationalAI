package cto.hmi.model.definition;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;

import cto.hmi.processor.dialogmodel.ActionResultMapping;
import cto.hmi.processor.dialogmodel.actions.GroovyAction;
import cto.hmi.processor.dialogmodel.actions.HTTPAction;
import cto.hmi.processor.dialogmodel.actions.JavaAction;

@XmlSeeAlso({ JavaAction.class, GroovyAction.class, HTTPAction.class })
public abstract class ActionModel {

	// serializable features
	protected boolean returnAnswer = true;
	protected String utteranceTemplate = ""; // e.g. "The temperature in %getWeatherCity is #temperature!"
	protected ArrayList<ActionResultMapping> resultMappingList;

	// Constructors
	public ActionModel() {

	}

	public ActionModel(String utteranceTemplate) {
		this();
		this.utteranceTemplate = utteranceTemplate;
	}

	// Serialization getters/setters
	public boolean isReturnAnswer() {
		return returnAnswer;
	}

	public void setReturnAnswer(boolean returnAnswer) {
		this.returnAnswer = returnAnswer;
	}

	public String getUtteranceTemplate() {
		return utteranceTemplate;
	}

	public void setUtteranceTemplate(String utteranceTemplate) {
		this.utteranceTemplate = utteranceTemplate;
	}

	@XmlElementWrapper(name = "resultMappings")
	@XmlElement(name = "resultMapping")
	public ArrayList<ActionResultMapping> getResultMappingList() {
		return resultMappingList;
	}

	public void setResultMappingList(ArrayList<ActionResultMapping> resultMappingList) {
		this.resultMappingList = resultMappingList;
	}

	public void addResultMapping(ActionResultMapping resultMapping) {
		if (this.resultMappingList == null)
			this.resultMappingList = new ArrayList<ActionResultMapping>();
		this.resultMappingList.add(resultMapping);
	}

}
