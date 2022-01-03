package cto.hmi.processor.dialogmodel;

import cto.hmi.model.definition.ActionResultMappingModel;

public class ActionResultMapping extends ActionResultMappingModel{

	public ActionResultMapping(){
		super();
	}
	
	public ActionResultMapping(String resultVarName, String resultValue, String message, String redirectToTask){
		super(resultVarName,resultValue,message,redirectToTask);
	}
}
