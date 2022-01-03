package cto.hmi.processor.exceptions;

public class NoParserFileFoundException extends Exception{

	private static final long serialVersionUID = 1L;

	public NoParserFileFoundException(String message){
		super("no file found for parsing: "+message);
	}
	
	public NoParserFileFoundException(){
		super("no file found for parsing");
	}

}
