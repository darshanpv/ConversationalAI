package cto.hmi.processor;

import java.util.regex.Pattern;

public class Test {

	public static void main(String[] args) {
		prg_2();

	}
	
	public static void prg_1() {
		
		String id = "s1-DUMMYSESSION";
		
		if (Pattern.compile("\\d+-").matcher(id).find())
		{
			System.out.println("Found");
			
		}else {
			System.out.println("Not Found");
		}
	}
	
public static void prg_2() {
		
		String id = "s1234-DUMMYSESSION";
		
		if (Pattern.compile("^[sd]?\\d+-[A-Z]{12}").matcher(id).find())
		{
			System.out.println("Found");
			
		}else {
			System.out.println("Not Found");
		}
	}
}
