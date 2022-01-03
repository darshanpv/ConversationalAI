package cto.hmi.processor.exceptions;

import cto.hmi.processor.ConvEngineProcessor;

public class RuntimeError extends Exception {

	private static final long serialVersionUID = 1L;

	public RuntimeError(String message){
		super("runtimeError: "+message);
		ConvEngineProcessor.getLogger().severe(message);
	}
	
	public RuntimeError(){
		super("runtimeError");
	}
}
