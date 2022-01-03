package cto.hmi.processor.dialogmodel;

import javax.xml.bind.annotation.XmlTransient;

import cto.hmi.model.definition.EntityModel;
import cto.hmi.processor.nlu.entityparser.ParseResults;
import cto.hmi.processor.nlu.entityparser.Parsers;


public class Entity extends EntityModel {
	// unserializable members
	private boolean filled;
	private Object value;
	// private static Generator generator = Generator.getInstance();
	private String utteranceText;

	public Entity() {
		super();
	}

	public Entity(String name, String fallbackQuestion, String answerType) {
		super(name, fallbackQuestion, answerType);
	}

	public Entity(String name, String fallbackQuestion, String clarifyQuestion, boolean useContext, boolean clearContext,
			boolean storeCahce, boolean clearCache) {
		super(name, fallbackQuestion, clarifyQuestion, useContext, clearContext, storeCahce, clearCache);
	}

	@XmlTransient
	public Boolean isFilled() {
		return this.filled;
	}

	public void setUnFilled() {
		this.filled = false;
	}

	@XmlTransient
	public Object getValue() {
		return this.value;
	}

	@XmlTransient
	public String getUtteranceText() {
		return this.utteranceText;
	}

	public void setValue(Object value) {
		this.value = value;
		this.filled = true;
	}

	public ParseResults parse(String utterance, boolean exact) {
		// may use AQD but don't need to
		String answer_type = this.getAnswerType();
		ParseResults results = null;

		try {
			if (exact)
				results = Parsers.parseExact(utterance, answer_type);
			else
				results = Parsers.parseWithAllParsers(utterance);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return results;
	}
}
