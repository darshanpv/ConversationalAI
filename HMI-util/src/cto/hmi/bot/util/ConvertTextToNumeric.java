package cto.hmi.bot.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertTextToNumeric {

	enum numberType {
		ALL_DIGITS_NUMBER, NUMERIC_NUMBER, WORD_NUMBER, INVALID_NUMBER
	};

	public ConvertTextToNumeric() {
		// TODO Auto-generated constructor stub
	}

	private static numberType nType;

	public String convert(String input) {
		String numberString = "";
		String nonNumberString = "";
		String result = "";
		String[] Numbers = new String[] { "zero", "one", "two", "three",
				"four", "five", "six", "seven", "eight", "nine", "ten",
				"eleven", "twelve", "thirteen", "fourteen", "fifteen",
				"sixteen", "seventeen", "eighteen", "nineteen", "twenty",
				"thirty", "forty", "fifty", "sixty", "seventy", "eighty",
				"ninety", "hundred", "thousand", "lakh", "million", "billion",
				"trillion" };
		String[] ordinal = new String[] { "first", "second", "third", "fifth",
				"eighth", "ninth", "twelfth" };
		String[] digits = new String[] { "zero", "one", "two", "three", "four",
				"five", "six", "seven", "eight", "nine" };
		List<String> digitList = Arrays.asList(digits);
		List<String> list = Arrays.asList(Numbers);
		List<String> ordinalList = Arrays.asList(ordinal);

		boolean doesContainWordNumber = false;
		boolean isAllDigits = true;
		boolean isConversionFound = false;
		boolean gotNumberIndex = false;
		int numberIndex = 0;

		// correct some common mistakes
		input = input.replaceAll(",", "");
		input = input.replaceAll("fourty", "forty");
		input = input.replaceAll("hundreds", "hundred");
		input = input.replaceAll("thousands", "thousand");
		input = input.replaceAll("lac", "lakh");
		input = input.replaceAll("lakhs", "lakh");
		input = input.replaceAll("lacs", "lakh");
		input = input.replaceAll("millions", "million");
		input = input.replaceAll("billions", "billion");
		input = input.replaceAll("trillions", "trillion");

		String inputArray[] = input.split(" ");
		// check if ordinal and replace with number
		for (int i = 0; i < inputArray.length; i++) {
			if (ordinalList.contains(inputArray[i].toLowerCase())) {
				switch (inputArray[i].toLowerCase()) {
				case "first":
					inputArray[i] = "one";
					break;
				case "second":
					inputArray[i] = "two";
					break;
				case "third":
					inputArray[i] = "three";
					break;
				case "fifth":
					inputArray[i] = "five";
					break;
				case "eighth":
					inputArray[i] = "eight";
					break;
				case "ninth":
					inputArray[i] = "nine";
					break;
				}
			} else {
				// convert 'ieth' into number e.g. twentieth twenty
				if (inputArray[i].toLowerCase().endsWith("ieth")) {
					inputArray[i] = inputArray[i].toLowerCase().substring(0,
							inputArray[i].length() - 4)
							+ "y";
				} else {
					// convert 'th' into number e.g. sixth to six
					if (inputArray[i].toLowerCase().endsWith("th")) {
						inputArray[i] = inputArray[i].toLowerCase().substring(
								0, inputArray[i].length() - 2);
					}
				}
			}
		}
		// reconstruct the input with modified elements
		input = Arrays.stream(inputArray).collect(Collectors.joining(" "));
		inputArray = input.split(" ");

		for (int i = 0; i < inputArray.length; i++) {
			// split input into number and nonnumber string
			if (list.contains(inputArray[i].toLowerCase())) {
				numberString = numberString + inputArray[i].toLowerCase() + " ";
				// find index where number is identified
				if (!gotNumberIndex)
					numberIndex = i;
				gotNumberIndex = true;
			} else {
				nonNumberString = nonNumberString + inputArray[i] + " ";
			}
		}

		WordToNumber wordToNumber = new WordToNumber();
		wordToNumber.Convert(numberString.trim());

		nType = numberType.INVALID_NUMBER;
		// check if input contains the word numbers
		for (int i = 0; i < numberString.split(" ").length; i++) {
			if (list.contains(numberString.split(" ")[i]))
				doesContainWordNumber = true;
		}
		// input is already a digit number
		if (input.matches(".*\\d.*") && !doesContainWordNumber) {
			nType = numberType.NUMERIC_NUMBER;
			isConversionFound = true;
		}

		// check if input has all digits e.g. one three four -> 134
		for (int i = 0; i < numberString.split(" ").length; i++) {
			if (!digitList.contains(numberString.split(" ")[i]))
				isAllDigits = false;
		}

		if (isAllDigits) {
			nType = numberType.ALL_DIGITS_NUMBER;
			isConversionFound = true;
		}
		// input contains all the word numbers for conversion
		if (!isConversionFound && !wordToNumber.getNumber().trim().equals("0")
				&& !input.matches(".*\\d.*") && doesContainWordNumber) {
			isConversionFound = true;
			nType = numberType.WORD_NUMBER;
		}

		// sanity check if it conversion is improper
		if ((numberString.contains("hundred") || numberString
				.contains("thousand"))
				&& (Integer.parseInt(wordToNumber.getNumber().trim()) < 99))
			nType = numberType.INVALID_NUMBER;

		switch (nType) {
		case ALL_DIGITS_NUMBER:
			String cNumber = "";
			WordToNumber digitNumber = new WordToNumber();
			for (int i = 0; i < numberString.split(" ").length; i++) {
				digitNumber.Convert(numberString.split(" ")[i]);
				cNumber = cNumber + digitNumber.getNumber();
			}

			if (numberIndex < nonNumberString.split(" ").length) {
				for (int i = 0; i < nonNumberString.split(" ").length; i++) {
					if (i == numberIndex)
						result = result + " " + cNumber.trim();
					result = result + " " + nonNumberString.split(" ")[i];
				}
			} else
				result = nonNumberString + cNumber;
			result = result.replaceAll("and", "").trim();
			break;

		case WORD_NUMBER:
			if (numberIndex < nonNumberString.split(" ").length) {
				for (int i = 0; i < nonNumberString.split(" ").length; i++) {
					if (i == numberIndex)
						result = result + " " + wordToNumber.getNumber().trim();
					result = result + " " + nonNumberString.split(" ")[i];
				}
			} else
				result = nonNumberString + wordToNumber.getNumber().trim();
			result = result.replaceAll("and", "").trim();
			break;

		case NUMERIC_NUMBER:
			result = input;
			break;

		case INVALID_NUMBER:
			result = input.replaceAll("[0-9]", "").trim();
			break;
		}
		return result;

	}
}
