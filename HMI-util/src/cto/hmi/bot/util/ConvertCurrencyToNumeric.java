package cto.hmi.bot.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertCurrencyToNumeric {
	enum numberType {
		NUMERIC_NUMBER, SCALE_NUMBER, INVALID_NUMBER
	};

	public ConvertCurrencyToNumeric() {
		// TODO Auto-generated constructor stub
	}

	public String convert(String input) {

		String result = "";
		numberType nType;
		input = input.replaceAll(",", "");
		ConvertTextToNumeric convertTextToNumeric = new ConvertTextToNumeric();
		String cInput = convertTextToNumeric.convert(input.toLowerCase()).trim();
		// System.out.println("Converted Intrim part: " + cInput);
		nType = numberType.INVALID_NUMBER;
		Pattern p = Pattern.compile(
				"\\b[+-]?\\d+(\\.)?(\\d+)?(\\s*)?(k|thousand|thousands|l|lac|lacs|lakh|lakhs|m|million|millions)\\b");
		Matcher m = p.matcher(input.toLowerCase());
		if (m.find())
			nType = numberType.SCALE_NUMBER;
		else if (cInput.matches(".*\\d.*"))
			nType = numberType.NUMERIC_NUMBER;
		else
			nType = numberType.INVALID_NUMBER;
		switch (nType) {
		case SCALE_NUMBER:
			String intPart = m.group().replaceAll("[a-z]", "").trim();
			String nonIntPart = m.group().replaceAll("[\\d+,/.]", "").trim();
			String scale = "";
			if (nonIntPart.equals("k") | nonIntPart.equals("thousand") | nonIntPart.equals("thousands"))
				scale = "k";
			if (nonIntPart.equals("l") | nonIntPart.equals("lac") | nonIntPart.equals("lacs")
					| nonIntPart.equals("lakh") | nonIntPart.equals("lakhs"))
				scale = "l";
			if (nonIntPart.equals("m") | nonIntPart.equals("million") | nonIntPart.equals("millions"))
				scale = "m";
			BigDecimal iPart = BigDecimal.valueOf(Double.parseDouble(intPart.trim()));
			BigDecimal num = BigDecimal.valueOf(0);
			switch (scale) {
			case "k":
				num = BigDecimal.valueOf(1000L);
				result = iPart.multiply(num).toString();
				break;
			case "l":
				num = BigDecimal.valueOf(100000L);
				result = iPart.multiply(num).toString();
				break;
			case "m":
				num = BigDecimal.valueOf(1000000L);
				result = iPart.multiply(num).toString();
				break;
			case "b":
				num = BigDecimal.valueOf(1000000000L);
				result = iPart.multiply(num).toString();
				break;
			case "t":
				num = BigDecimal.valueOf(1000000000000L);
				result = iPart.multiply(num).toString();
				break;
			}
			result = result.substring(0, result.length() - 2);
			result = input.toLowerCase().replace(m.group(), result);
			break;
		case NUMERIC_NUMBER:
			result = cInput;
			break;
		case INVALID_NUMBER:
			result = cInput;
			break;
		}
		return result;
	}
}
