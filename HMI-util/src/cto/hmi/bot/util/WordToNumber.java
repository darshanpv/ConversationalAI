package cto.hmi.bot.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WordToNumber {
	private final static Logger logger = LoggerUtil.getLogger();
	boolean isConverted;
	String number;

	public WordToNumber() {
		// TODO Auto-generated constructor stub
		this.isConverted = false;
		this.number = "";
	}

	public boolean isConverted() {
		return isConverted;
	}

	public void setConverted(boolean isConverted) {
		this.isConverted = isConverted;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public void Convert(String text) {

		String[] units = new String[] { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
				"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
				"nineteen", };

		String[] tens = new String[] { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty",
				"ninety" };

		String[] scales = new String[] { "hundred", "thousand", "lakh", "million", "billion", "trillion" };

		double current = 0;
		double result = 0;
		try {
			Map<String, ScaleIncrementPair> numWord = new LinkedHashMap<>();

			numWord.put("and", new ScaleIncrementPair(1, 0));

			for (int i = 0; i < units.length; i++) {
				numWord.put(units[i], new ScaleIncrementPair(1, i));
			}

			for (int i = 1; i < tens.length; i++) {
				numWord.put(tens[i], new ScaleIncrementPair(1, i * 10));
			}

			for (int i = 0; i < scales.length; i++) {
				switch (i) {
				case 0:
					numWord.put(scales[i], new ScaleIncrementPair(100, 0));
					break;
				case 1:
					numWord.put(scales[i], new ScaleIncrementPair(1000, 0));
					break;
				case 2:
					numWord.put(scales[i], new ScaleIncrementPair(100000, 0));
					break;
				case 3:
					numWord.put(scales[i], new ScaleIncrementPair(Math.pow(10, (2 * 3)), 0));
					break;
				case 4:
					numWord.put(scales[i], new ScaleIncrementPair(Math.pow(10, (3 * 3)), 0));
					break;
				case 5:
					numWord.put(scales[i], new ScaleIncrementPair(Math.pow(10, (4 * 3)), 0));
					break;

				}
			}

			current = 0;
			result = 0;

			for (String word : text.split("[ -]")) {
				ScaleIncrementPair scaleIncrement = numWord.get(word);
				current = current * scaleIncrement.scale + scaleIncrement.increment;
				if (scaleIncrement.scale > 100) {
					result += current;
					current = 0;
				}
			}
			setConverted(true);
			String cNumber = Double.toString(result + current);
			setNumber(cNumber.substring(0, cNumber.length() - 2));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.severe("Error: issue while processing");
			setConverted(false);
			setNumber("0");
		}

	}

}
