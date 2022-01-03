package cto.hmi.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cto.hmi.bot.util.ProgressBar;

public class PingURLs {

	public static boolean isReachable(String URLs, int timeOutSec, String msg) {
		try {
			Map<String, Integer> servers = new HashMap<String, Integer>();

			// split the URLs into server e.g. 192.168.0.16:8092,192.168.0.17:8093
			List<String> addresses = Arrays.asList(URLs.split("\\s*,\\s*"));
			for (String address : addresses) {
				String ip = "";
				int port = 0;
				if (address.startsWith("http")) {
					URL url = new URL(address);
					ip = url.getHost();
					port = url.getPort();

				} else {
					ip = address.split("\\s*:\\s*")[0];
					port = Integer.valueOf(address.split("\\s*:\\s*")[1]);
				}
				servers.put(ip, port);
			}
			ProgressBar.startPB(timeOutSec * servers.size(), msg);
			for (Map.Entry<String, Integer> addr : servers.entrySet()) {
				try (Socket soc = new Socket()) {
					soc.connect(new InetSocketAddress(addr.getKey(), addr.getValue()), timeOutSec * 1000);
				}
			}
			ProgressBar.stopPB();
			return true;
		} catch (IOException ex) {
			ProgressBar.stopPB();
			return false;
		}
	}
}
