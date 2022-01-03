package cto.hmi.processor.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.javatuples.Quartet;
import org.yaml.snakeyaml.Yaml;

import cto.hmi.processor.ConvEngineProcessor;
import cto.hmi.processor.dialogmodel.ActionResultMapping;
import cto.hmi.processor.dialogmodel.Entity;
import cto.hmi.processor.dialogmodel.FollowUp;
import cto.hmi.processor.dialogmodel.Task;
import cto.hmi.processor.dialogmodel.actions.GroovyAction;
import cto.hmi.processor.dialogmodel.actions.HTTPAction;

public class EventManager {
	private final static Logger logger = ConvEngineProcessor.getLogger();
	String eName;

	public EventManager(String eName) {
		this.eName = eName;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<Task> processEvent() {
		Yaml yaml = new Yaml();
		ArrayList<Task> taskArray = new ArrayList<Task>();

		try {

			String eventFile = new File(".").getAbsolutePath() + "/res/events/" + eName + ".yml";
			File f = new File(eventFile);
			if (!f.exists()) {
				logger.severe("No event file exists");
				return null;
			}
			InputStream input = new FileInputStream(eventFile);
			Map<String, Object> eTasks = (Map<String, Object>) yaml.load(input);
			List<Object> taskList = (List<Object>) eTasks.get("tasks");

			// get all the task details and add it to task array
			for (int i = 0; i < taskList.size(); i++) {
				Map<String, ArrayList> tasks = (Map<String, ArrayList>) taskList.get(i);
				Map<String, Object> eTask = (Map<String, Object>) tasks.get("task");

				Task tsk = new Task();
				// set Task attributes from yaml
				tsk.setName(eTask.get("name").toString());
				if (eTask.getOrDefault("label", null) != null)
					tsk.setLabel(eTask.get("label").toString());
				if (eTask.getOrDefault("role", null) != null)
					tsk.setRole(eTask.get("role").toString());
				if (eTask.getOrDefault("useCache", null) != null)
					tsk.setuseCache(Boolean.parseBoolean(eTask.get("useCache").toString()));

				// put empty selector to avoid null pointer exception
				// tsk.setSelector(new BagOfWordsTaskSelector(new
				// ArrayList<String>()));
				// check if entities list exists
				if (eTask.get("entities") instanceof List<?>) {
					// get list of Entities from yaml
					List<Object> entityList = (List<Object>) eTask.get("entities");
					for (int j = 0; j < entityList.size(); j++) {
						Map<String, ArrayList> entities = (Map<String, ArrayList>) entityList.get(j);
						Map<String, Object> eEntity = (Map<String, Object>) entities.get("entity");

						Entity entity = new Entity(eEntity.get("name").toString(),
								eEntity.get("fallbackQuestion").toString(), eEntity.get("answerType").toString());
						if (eEntity.getOrDefault("label", null) != null)
							entity.setLabel(eEntity.get("label").toString());

						if (eEntity.getOrDefault("clarifyQuestion", null) != null)
							entity.setClarifyQuestion(eEntity.get("clarifyQuestion").toString());
						if (eEntity.get("required").toString().toLowerCase().equals("true"))
							entity.setRequired(true);
						if (eEntity.getOrDefault("useContext", null) != null)
							if (eEntity.get("useContext").toString().toLowerCase().equals("true"))
								entity.setUseContext(true);
						if (eEntity.getOrDefault("clearContext", null) != null)
							if (eEntity.get("clearContext").toString().toLowerCase().equals("true"))
								entity.setClearContext(true);
						if (eEntity.getOrDefault("storeCache", null) != null)
							if (eEntity.get("storeCache").toString().toLowerCase().equals("true"))
								entity.setStoreCache(true);
						if (eEntity.getOrDefault("clearCache", null) != null)
							if (eEntity.get("clearCache").toString().toLowerCase().equals("true"))
								entity.setClearCache(true);
						tsk.addEntity(entity);
					}
				}
				Map<String, Object> eAction = (Map<String, Object>) eTask.get("action");
				LinkedList<Quartet<String, String, String, String>> rMappingList = new LinkedList<Quartet<String, String, String, String>>();

				// check if result Mapping present
				if (eAction.getOrDefault("resultMappings", null) != null) {
					// check if list does exist
					if (eAction.get("resultMappings") instanceof List<?>) {
						List<Object> resultMappingList = (List<Object>) eAction.get("resultMappings");
						for (int j = 0; j < resultMappingList.size(); j++) {
							Map<String, ArrayList> maps = (Map<String, ArrayList>) resultMappingList.get(j);
							Map<String, Object> eMap = (Map<String, Object>) maps.get("map");
							String msg = "", toTask = "", varName = "", value = "";
							if (eMap.getOrDefault("message", null) != null)
								msg = eMap.get("message").toString();
							if (eMap.getOrDefault("redirectToTask", null) != null)
								toTask = eMap.get("redirectToTask").toString();
							if (eMap.getOrDefault("resultVarName", null) != null)
								varName = eMap.get("resultVarName").toString();
							if (eMap.getOrDefault("resultValue", null) != null)
								value = eMap.get("resultValue").toString();
							Quartet<String, String, String, String> rMapping = new Quartet<String, String, String, String>(
									"", "", "", "");
							rMapping = Quartet.with(varName, value, msg, toTask);
							rMappingList.add(rMapping);
						}
					}
				}
				// process HTTP action fields
				if (eAction.get("type").toString().toLowerCase().equals("httpaction")) {

					String uTemplate = "";
					if (eAction.getOrDefault("utteranceTemplate", null) != null)
						uTemplate = eAction.get("utteranceTemplate").toString();

					HTTPAction action = new HTTPAction(uTemplate);
					action.setMethod(eAction.get("method").toString());
					action.setUrl(eAction.get("url").toString());

					if (eAction.get("params") != null)
						action.setParams(eAction.get("params").toString());
					else
						action.setParams("");

					if (eAction.get("xpath") != null)
						action.setXpath(eAction.get("xpath").toString());

					if (eAction.get("jpath") != null)
						action.setJpath(eAction.get("jpath").toString());

					// add resultMapping that was processed earlier
					if (rMappingList.size() > 0) {
						for (int k = 0; k < rMappingList.size(); k++) {

							action.addResultMapping(new ActionResultMapping(rMappingList.get(k).getValue0(),
									rMappingList.get(k).getValue1(), rMappingList.get(k).getValue2(),
									rMappingList.get(k).getValue3()));
						}
					}

					tsk.setAction(action);

				}
				// process groovy action fields
				else if (eAction.get("type").toString().toLowerCase().equals("groovyaction")) {
					String uTemplate = "";
					if (eAction.getOrDefault("utteranceTemplate", null) != null)
						uTemplate = eAction.get("utteranceTemplate").toString();

					GroovyAction action = new GroovyAction(uTemplate);
					action.setCode(eAction.getOrDefault("code", "").toString());

					// add resultMapping that was processed earlier
					if (rMappingList.size() > 0) {
						for (int k = 0; k < rMappingList.size(); k++) {

							action.addResultMapping(new ActionResultMapping(rMappingList.get(k).getValue0(),
									rMappingList.get(k).getValue1(), rMappingList.get(k).getValue2(),
									rMappingList.get(k).getValue3()));
						}
					}

					tsk.setAction(action);
				}
				// throw error if none
				else {
					logger.severe("Please check your event configuration.");
					return null;
				}

				// process followup if present
				Map<String, Object> eFollowup = (Map<String, Object>) eTask.get("followup");
				if (eFollowup != null) {
					Map<String, Object> eEntity = (Map<String, Object>) eFollowup.get("entity");
					Entity entity = new Entity(eEntity.get("name").toString(),
							eEntity.get("fallbackQuestion").toString(), eEntity.get("answerType").toString());
					if (eEntity.getOrDefault("label", null) != null)
						entity.setLabel(eEntity.get("label").toString());

					if (eEntity.getOrDefault("clarifyQuestion", null) != null)
						entity.setClarifyQuestion(eEntity.get("clarifyQuestion").toString());
					if (eEntity.get("required").toString().toLowerCase().equals("true"))
						entity.setRequired(true);
					if (eEntity.getOrDefault("useContext", null) != null)
						if (eEntity.get("useContext").toString().toLowerCase().equals("true"))
							entity.setUseContext(true);
					if (eEntity.getOrDefault("clearContext", null) != null)
						if (eEntity.get("clearContext").toString().toLowerCase().equals("true"))
							entity.setClearContext(true);
					if (eEntity.getOrDefault("storeCache", null) != null)
						if (eEntity.get("storeCache").toString().toLowerCase().equals("true"))
							entity.setStoreCache(true);
					if (eEntity.getOrDefault("clearCache", null) != null)
						if (eEntity.get("clearCache").toString().toLowerCase().equals("true"))
							entity.setClearCache(true);

					// get answerMapping fields
					List<Object> mapList = (List<Object>) eFollowup.get("answerMapping");
					HashMap<String, String> answerMapping = new HashMap<String, String>();
					for (int j = 0; j < mapList.size(); j++) {
						Map<String, ArrayList> maps = (Map<String, ArrayList>) mapList.get(j);
						Map<String, Object> eMap = (Map<String, Object>) maps.get("map");
						answerMapping.put(eMap.get("key").toString(), eMap.get("value").toString());

					}
					FollowUp followup = new FollowUp();
					followup.setEntity(entity);
					followup.setAnswerMapping(answerMapping);
					tsk.setFollowup(followup);
				}

				taskArray.add(tsk);
			}

		} catch (FileNotFoundException | NullPointerException e) {
			// TODO Auto-generated catch block
			logger.severe("Please check your event configuration.\n");
			e.printStackTrace();
		}
		return taskArray;

	}

}
