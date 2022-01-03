package cto.hmi.bot.util;

import java.io.File;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

public class AIMLProcessor {
	private static AIMLProcessor instance = null;
	static Bot bot = null;
	static Chat chatSession = null;

	protected AIMLProcessor() {
	}

	public static AIMLProcessor getInstance() {
		if (instance == null) {
			instance = new AIMLProcessor();
			init();
		}
		return instance;
	}

	private static void init() {
		// TODO Auto-generated method stub
		String resourcesPath;
		try {
			resourcesPath = new File(".").getAbsolutePath() + "/res";
			MagicBooleans.jp_tokenize = false;
			MagicBooleans.trace_mode = false;
			MagicStrings.default_bot_response = "AIML_NO_ANSWER";
			MagicStrings.error_bot_response = "AIML_ERROR";
			MagicStrings.schedule_error = "AIML_SCHEDULE_ERROR";
			MagicStrings.system_failed = "AIML_SYSTEM_FAILED";
			MagicBooleans.enable_external_sets = false;
			MagicBooleans.enable_external_maps = false;
			org.alicebot.ab.AIMLProcessor.extension = new PCAIMLProcessorExtension(); 

			MagicStrings.root_path = resourcesPath;

			// remove all files from aimlif folders to avoid learning
			File outputFolder = new File(MagicStrings.root_path + "/bots/aimlif");
			final File[] files = outputFolder.listFiles();
			for (File f : files)
				f.delete();

			bot = new Bot("", resourcesPath);
			chatSession = new Chat(bot, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String process(String utterance) {
		String response = "";
		try {
			response = chatSession.multisentenceRespond(utterance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
}
