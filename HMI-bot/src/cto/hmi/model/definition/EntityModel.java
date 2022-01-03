package cto.hmi.model.definition;

import java.util.Random;

import javax.xml.bind.annotation.XmlAttribute;

public abstract class EntityModel {

	// serializable members
	protected String name;
	protected String label = "";
	protected boolean required = true;
	protected String answerType;
	protected String fallbackQuestion;
	protected String clarifyQuestion = "";
	protected boolean useContext = false;
	protected boolean clearContext = false;
	protected boolean storeCache = false;
	protected boolean clearCache = false;

	// Constructors
	public EntityModel() {

	}

	public EntityModel(String name, String fallbackQuestion, String answerType) {
		this.name = name;
		this.fallbackQuestion = fallbackQuestion;
		this.answerType = answerType;
	}

	public EntityModel(String name, String fallbackQuestion, String clarifyQuestion, boolean useContext,
			boolean clearContext, boolean storeCache, boolean clearCache) {
		this.name = name;
		this.fallbackQuestion = fallbackQuestion;
		this.clarifyQuestion = clarifyQuestion;
		this.useContext = useContext;
		this.clearContext = clearContext;
		this.storeCache = storeCache;
		this.clearCache = clearCache;
	}

	// Serialization getter/setter
	@XmlAttribute(name = "name")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute(name = "label")
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setAnswerType(String answerType) {
		this.answerType = answerType;
	}

	public String getAnswerType() {
		return answerType;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isUseContext() {
		return useContext;
	}

	public void setUseContext(boolean useContext) {
		this.useContext = useContext;
	}

	public boolean isClearContext() {
		return clearContext;
	}

	public void setClearContext(boolean clearContext) {
		this.clearContext = clearContext;
	}

	public boolean isStoreCache() {
		return storeCache;
	}

	public void setStoreCache(boolean storeCache) {
		this.storeCache = storeCache;
	}

	public boolean isClearCache() {
		return clearCache;
	}

	public void setClearCache(boolean clearCache) {
		this.clearCache = clearCache;
	}

	public String getFallbackQuestion() {
		String question = "";
		String[] questions = fallbackQuestion.split("\\|");
		if (questions.length > 1) {
			Random ran = new Random();
			int x = ran.nextInt(questions.length);
			question = questions[x];

		} else {
			question = fallbackQuestion;
		}
		return question.trim();
	}

	public void setFallbackQuestion(String fallbackQuestion) {
		this.fallbackQuestion = fallbackQuestion;
	}

	public String getClarifyQuestion() {
		return clarifyQuestion.trim();
	}

	public void setClarifyQuestion(String clarifyQuestion) {
		this.clarifyQuestion = clarifyQuestion;
	}
}
