package cto.hmi.bot.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.icu.text.Transliterator;

public class Transliteration {
	static String utterance_file = "/res/utterance.dat";
	static String utterance = "";
	static String option = "";

	public static void main(String[] args) {

		// process command line args
		Options cli_options = new Options();
		cli_options.addOption("h", "help", false, "print this message");

		cli_options.addOption("t", "from_to", true,
				"specify transliteration DEV_TO_ENG,EU_TO_ENG,ARB_TO_ENG e.g. -t DEV_TO_ENG");

		cli_options.addOption("f", "file", true, "specify relative path and file , e.g. -f /res/utterance.dat");

		cli_options.addOption("u", "utterance", true, "sample utterance, e.g. -u \"viajar con clase ejecutiva\"");

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(cli_options, args);

			// Help
			if (cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Transliteration Utility", cli_options, true);
				return;
			}
			// load option
			if (cmd.hasOption("t")) {
				option = cmd.getOptionValue("t");
			}
			// load option
			if (cmd.hasOption("f")) {
				utterance_file = cmd.getOptionValue("f");
			}
			// load option
			if (cmd.hasOption("u")) {
				utterance = cmd.getOptionValue("u");
			}

			String input = "";
			if (cmd.hasOption("f") && cmd.hasOption("t")) {
				input = readFile(utterance_file);
				writeFile(input);
			} else if (cmd.hasOption("u") && cmd.hasOption("t")) {
				String trUtterance = "";
				switch (option) {
				case "DEV_TO_ENG":
					trUtterance = DevanagariToEnglish(utterance);
					break;
				case "EU_TO_ENG":
					trUtterance = AccentToEnglish(utterance);
					break;
				case "ARB_TO_ENG":
					trUtterance = ArabicToEnglish(utterance);
					break;
				case "ENG_TO_DEV":
					trUtterance = EnglishToDevnagari(utterance);
					break;
				case "ENG_TO_ARB":
					trUtterance = EnglishToArabic(utterance);
					break;
				default:
					trUtterance = DevanagariToEnglish(utterance);
					break;
				}
				System.out.println("UTIL:Processed utterance : " + trUtterance);
			} else {
				System.out.println("UTIL: failed to execute please try again..");
			}

		} catch (ParseException e1) {
			System.out.println("UTIL: loading the utiltiy main-method failed. " + e1.getMessage());
			e1.printStackTrace();
		}

		// String gk = "اريد حجز تذكرة";
		// String en = ArabicToEnglish(gk);
		// System.out.println(en);
		// System.out.println(EnglishToArabic(en));
	}

	public static String readFile(String utterance_file) {
		String fileName = new File(".").getAbsolutePath() + utterance_file;

		String Sentence = "";
		String utterance;
		try {
			FileReader fr = new FileReader(fileName);
			System.out.println("UTIL:Reading file " + fileName);
			BufferedReader br = new BufferedReader(fr);
			while ((utterance = br.readLine()) != null) {
				String trUtterance = "";
				switch (option) {
				case "DEV_TO_ENG":
					trUtterance = DevanagariToEnglish(utterance);
					break;
				case "EU_TO_ENG":
					trUtterance = AccentToEnglish(utterance);
					break;
				case "ARB_TO_ENG":
					trUtterance = ArabicToEnglish(utterance);
					break;
				case "ENG_TO_DEV":
					trUtterance = EnglishToDevnagari(utterance);
					break;
				case "ENG_TO_ARB":
					trUtterance = EnglishToArabic(utterance);
					break;
				default:
					trUtterance = DevanagariToEnglish(utterance);
					break;
				}
				// Sentence += line+'\n';
				Sentence += utterance + "|" + trUtterance + '\n';
			}
			br.close();
			System.out.println("UTIL:Processed file " + fileName);
			System.out.println("UTIL:Sending processed file...");
			return Sentence;
		} catch (FileNotFoundException ex) {
			System.out.println("UTIL:Unable to open file '" + fileName + "'");
			return null;
		} catch (IOException ex) {
			System.out.println("UTIL:Error reading file '" + fileName + "'");
			return null;
			// Or we could just do this:
			// ex.printStackTrace();
		}
	}

	public static void writeFile(String in) {
		String output_intent_file = "";
		int index = utterance_file.indexOf(".");
		output_intent_file = utterance_file.substring(0, index) + "_out."
				+ utterance_file.substring(index + 1, utterance_file.length());
		String fileName = new File(".").getAbsolutePath() + output_intent_file;

		try {
			FileWriter fw = new FileWriter(fileName);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(in);
			System.out.println("UTIL:Received " + in.length() + " bytes");
			bw.close();
		} catch (IOException ex) {
			System.out.println("UTIL:Error writing to file '" + fileName + "'");
		}
		System.out.println("UTIL:Created output file " + output_intent_file);
		System.out.println("UTIL:Process completed...");
	}

	public static String DevanagariToEnglish(String devnagari) {
		final String DEV_TO_ENG = "Devanagari-Latin";
		Transliterator toDevnagiri = Transliterator.getInstance(DEV_TO_ENG);
		String latin = toDevnagiri.transliterate(devnagari);
		Transliterator accentsConverter = Transliterator
				.getInstance("Any-Latin; NFD; [:M:] Remove; NFC; [^\\p{ASCII}] Remove");
		String english = accentsConverter.transliterate(latin);
		return english;
	}

	public static String EnglishToDevnagari(String english) {
		final String ENG_TO_DEV = "Latin-Devanagari";
		Transliterator toEnglish = Transliterator.getInstance(ENG_TO_DEV);
		String devnagari = "";
		// check if it has <p> and <a> tags e.g. "<p>कृपया अपने स्थान के पास
		// आपूर्तिकर्ता पर नीचे अधिक जानकारी प्राप्त करें।</p><a
		// href='http://www.reliancedigital.in/'>अधिक विवरण हेतु यहाँ क्लिक
		// करें।</a>"
		if (english.contains("</p>") || english.contains("</a>")) {
			String pTag = "";
			String aTag = "";
			pTag = english.substring(english.indexOf("<p>") + 3, english.indexOf("</p>"));
			Pattern pattern = Pattern.compile("<a[^>]+>(.+?)</a>");
			Matcher matcher = pattern.matcher(english);
			if (matcher.find()) {
				aTag = matcher.group(1);
			}
			if (english.contains("</a>"))
				devnagari = "<p>" + toEnglish.transliterate(pTag) + "</p>"
						+ english.substring(english.indexOf("<a"), english.indexOf("'>") + 2)
						+ toEnglish.transliterate(aTag) + "</a>";
			else
				devnagari = "<p>" + toEnglish.transliterate(pTag) + "</p>";
		} else {
			devnagari = toEnglish.transliterate(english);
		}
		return devnagari;
	}

	public static String AccentToEnglish(String accent) {
		final String TRANSLITERATE_ID = "NFD; Any-Latin;Latin-ASCII; NFC";
		final String NORMALIZE_ID = "NFD; [:Nonspacing Mark:] Remove; NFC";
		Transliterator toEnglish = Transliterator.getInstance(TRANSLITERATE_ID + "; " + NORMALIZE_ID);
		String result = toEnglish.transliterate(accent);
		return result;
	}

	public static String ArabicToEnglish(String arabic) {
		final String ARB_TO_ENG = "Arabic-Latin";
		Transliterator toDevnagiri = Transliterator.getInstance(ARB_TO_ENG);
		String latin = toDevnagiri.transliterate(arabic);
		Transliterator accentsConverter = Transliterator
				.getInstance("Any-Latin; NFD; [:M:] Remove; NFC; [^\\p{ASCII}] Remove");
		String english = accentsConverter.transliterate(latin);
		return english;
	}

	public static String EnglishToArabic(String english) {
		final String ENG_TO_ARB = "Latin-Arabic";
		Transliterator toEnglish = Transliterator.getInstance(ENG_TO_ARB);
		String arabic = "";
		// check if it has <p> and <a> tags
		if (english.contains("</p>") || english.contains("</a>")) {
			String pTag = "";
			String aTag = "";
			pTag = english.substring(english.indexOf("<p>") + 3, english.indexOf("</p>"));
			Pattern pattern = Pattern.compile("<a[^>]+>(.+?)</a>");
			Matcher matcher = pattern.matcher(english);
			if (matcher.find()) {
				aTag = matcher.group(1);
			}
			if (english.contains("</a>"))
				arabic = "<p>" + toEnglish.transliterate(pTag) + "</p>"
						+ english.substring(english.indexOf("<a"), english.indexOf("'>") + 2)
						+ toEnglish.transliterate(aTag) + "</a>";
			else
				arabic = "<p>" + toEnglish.transliterate(pTag) + "</p>";
		} else {
			arabic = toEnglish.transliterate(english);
		}
		return arabic;
	}

}
