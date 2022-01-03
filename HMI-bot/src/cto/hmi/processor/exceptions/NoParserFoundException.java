package cto.hmi.processor.exceptions;

public class NoParserFoundException extends Exception{

	private static final long serialVersionUID = 1L;

	public NoParserFoundException(String message){
		super("no parser found: "+message);
	}
	
	public NoParserFoundException(){
		super("no parser found");
	}

}
