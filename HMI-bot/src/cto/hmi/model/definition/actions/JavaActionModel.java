package cto.hmi.model.definition.actions;

import cto.hmi.processor.dialogmodel.Action;

public abstract class JavaActionModel extends Action {

	protected String path;
	protected String className;

	public JavaActionModel() {
		super();
	}

	public JavaActionModel(String template) {
		super(template);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
