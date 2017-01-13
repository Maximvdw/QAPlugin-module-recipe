package be.maximvdw.qaplugin.modules.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class HtmlUtils {
	/**
	 * User Agent
	 */
	private final static String USER_AGENT = "Mozilla/5.0";

	/**
	 * Get the body contents of an url
	 * 
	 * @param url
	 *            URL Link
	 * @return String with the body
	 * @throws IOException
	 */
	public static String getHtmlSource(String url) throws IOException {
		URL yahoo = new URL(url);
		URLConnection yc = yahoo.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), "UTF-8"));
		String inputLine;
		StringBuilder a = new StringBuilder();
		while ((inputLine = in.readLine()) != null)
			a.append(inputLine + "\n");
		in.close();

		return a.toString();
	}
}
