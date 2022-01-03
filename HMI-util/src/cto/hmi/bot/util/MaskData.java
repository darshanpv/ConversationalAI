package cto.hmi.bot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This utility will mask the data as part of GDPR compliance
 * User need to provide the RegEx pattern those will be masked
 */
public class MaskData {

	public static void main(String[] args) {
		System.out.println(
				MaskData.mask("<![CDATA[<p>I am going to GOA on 12/08/2018</p><p>I am going and is fine</p>]]>", true));
	}

	public static String mask(String utterance, boolean isMaskRequired) {
		if (isMaskRequired) {
			char[] chars = utterance.toCharArray();
			Pattern ptrn = Pattern.compile("([0-9]{1,})");
			Matcher matcher = ptrn.matcher(utterance);
			while (matcher.find()) {
				for (int i = matcher.start(); i < matcher.end(); i++) {
					chars[i] = '?';
				}
			}
			utterance = String.valueOf(chars);
			ptrn = Pattern.compile("([A-Z]{2,})");
			matcher = ptrn.matcher(utterance);
			while (matcher.find()) {
				for (int i = matcher.start(); i < matcher.end(); i++) {
					chars[i] = '?';
				}
			}
			utterance = String.valueOf(chars);
			// remove html tags if any before it is used for logging
			utterance = utterance.replaceAll("\\<.*?\\>", " ").trim();
			// remove the the training closures if any like ]]>
			if (utterance.endsWith(">"))
				utterance = utterance.substring(0, utterance.indexOf("]")).trim();
		}
		return utterance;
	}
}
