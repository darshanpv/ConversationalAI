package cto.hmi.processor.nlu.entityparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cto.hmi.processor.exceptions.NoParserFileFoundException;


public abstract class Parser {
	
	protected String name; //the java class name
	protected String type; //the type (i.e. class) that the parser parses, e.g. fact.temporal.date

	public Parser(String type){
		this.name=this.getClass().getSimpleName();
		this.type=type;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void setType(String type){
		this.type=type;
	}

	protected void match_regex(ParseResults results, String regex, String className, String classValue){

		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(results.getUtterance());
		
		 while(m.find()){
			 results.add(new ParseResult(this.name, m.start(), m.end(), m.group(0), type, classValue));
		 }
	}
	
	public abstract ParseResults parse(String utterance) throws NoParserFileFoundException;
}
