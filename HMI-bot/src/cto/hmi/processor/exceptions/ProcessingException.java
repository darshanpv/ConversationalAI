package cto.hmi.processor.exceptions;

import cto.hmi.processor.ConvEngineProcessor;

public class ProcessingException extends Exception {

	private static final long serialVersionUID = 1L;

	public ProcessingException(String message){
		super("processing exception: "+message);
		ConvEngineProcessor.getLogger().severe(message);
	}
	
	public ProcessingException(){
		super("processing exception");
	}
}
