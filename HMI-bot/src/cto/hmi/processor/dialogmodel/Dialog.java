package cto.hmi.processor.dialogmodel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.annotation.XmlRootElement;

import cto.hmi.model.definition.DialogModel;
import cto.hmi.processor.ConvEngineConfig;

@XmlRootElement
public class Dialog extends DialogModel {

	private static ConvEngineConfig config = ConvEngineConfig.getInstance();

	public Dialog() {
		super();
	}

	public Dialog(String name) {
		super(name);
	}

	// Serialization / Deserialization

	public static Dialog loadFromResourcesDir(String filename) {
		return loadFromPath(config.getProperty(ConvEngineConfig.DIALOGUEDIR) + "/" + filename + ".xml");
	}

	public void save() {
		String filename = getName() != null ? getName() + ".xml" : "dialogue.xml";
		saveAs(config.getProperty(ConvEngineConfig.DIALOGUEDIR), filename);
	}

	public void saveUnder(String path) {
		try {
			String filename = getName() != null ? getName() + ".xml" : "dialogue.xml";
			save(new FileOutputStream(path + "/" + filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
