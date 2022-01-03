package cto.hmi.processor.dialogmodel.actions;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlRootElement;

import cto.hmi.model.definition.actions.JavaActionModel;
import cto.hmi.processor.dialogmodel.Action;
import cto.hmi.processor.dialogmodel.Frame;

@XmlRootElement
public class JavaAction extends JavaActionModel {

	public JavaAction() {
		super();
	}

	public JavaAction(String template) {
		super(template);
	}

	@Override
	public HashMap<String, String> execute(Frame frame) {
		Action action = load();
		return action.execute(frame);
	}

	private Action load() {
		Action extAct = null;
		URLClassLoader cl;
		try {
			URL url = new URL("file", path, "");
			cl = new URLClassLoader(new URL[] { url });
			Class<?> loadedClass = cl.loadClass(className);
			extAct = (Action) loadedClass.getDeclaredConstructor().newInstance();
			cl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return extAct;
	}

}
