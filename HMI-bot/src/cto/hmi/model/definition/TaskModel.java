package cto.hmi.model.definition;

import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import cto.hmi.processor.dialogmodel.Action;
import cto.hmi.processor.dialogmodel.Entity;
import cto.hmi.processor.dialogmodel.FollowUp;

@XmlType(propOrder = { "entities", "metaaction", "followup" })
public abstract class TaskModel {

	// serializable members
	protected String name;
	protected String label = "";
	protected String role = "";
	protected boolean useCache = false;
	protected ArrayList<Entity> entities;// must not be null; otherwise unmarshalling fails
	protected MetaActionModel metaAction;
	protected FollowUp followup;

	// Constructors
	public TaskModel() {
		entities = new ArrayList<Entity>();
	}

	public TaskModel(String name) {
		this();
		this.name = name;
	}

	// Serialization getter/setter

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
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

	@XmlAttribute(name = "role")
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@XmlAttribute(name = "useCache")
	public Boolean getuseCache() {
		return useCache;
	}

	public void setuseCache(Boolean flag) {
		this.useCache = flag;
	}

	@XmlElement(name = "action", required = false)
	public MetaActionModel getMetaaction() {
		return metaAction;
	}

	public void setMetaaction(MetaActionModel metaaction) {
		this.metaAction = metaaction;
	}

	@XmlElementWrapper(name = "entities", required = true)
	@XmlElement(name = "entity", required = false)
	public ArrayList<Entity> getEntities() {
		return entities;
	}

	public Entity getEntities(String entity) {
		Iterator<Entity> it = this.entities.iterator();
		while (it.hasNext()) {
			Entity itoList = it.next();
			if (itoList.getName().equals(entity))
				return itoList;
		}
		return null;
	}

	public FollowUp getFollowup() {
		return followup;
	}

	public void setFollowup(FollowUp followup) {
		this.followup = followup;
	}

	// Helpers
	public void addEntity(Entity entity) {
		this.entities.add(entity);
	}

	public boolean hasEntity(Entity entity) {
		boolean flag = false;
		Iterator<Entity> it = this.entities.iterator();
		while (it.hasNext()) {
			Entity itosInTask = it.next();
			if (itosInTask.getName().equals(entity.getName()))
				flag = true;
		}
		return flag;
	}

	public boolean hasEntity(String itoName) {
		boolean flag = false;
		Iterator<Entity> it = this.entities.iterator();
		while (it.hasNext()) {
			Entity itosInTask = it.next();
			if (itosInTask.getName().equals(itoName))
				flag = true;
		}
		return flag;
	}

	// non-serializable
	@XmlTransient
	public Action getAction() {
		// return action;
		if (getMetaaction() != null) {
			return getMetaaction().getAction();
		} else
			return null;
	}

	public void setAction(Action action) {
		MetaActionModel mam = new MetaActionModel();
		mam.setAction(action);
		setMetaaction(mam);
	}

}
