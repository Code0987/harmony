package com.ilusons.harmony.ref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeEx {

	private final static String youTubeUrlRegEx = "^(https?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/";
	private final static String[] videoIdRegex = {"\\?vi?=([^&]*)", "watch\\?.*v=([^&]*)", "(?:embed|vi?)/([^/?]*)", "^([A-Za-z0-9\\-]*)"};

	public static String extractVideoIdFromUrl(String url) {
		String youTubeLinkWithoutProtocolAndDomain = youTubeLinkWithoutProtocolAndDomain(url);

		for (String regex : videoIdRegex) {
			Pattern compiledPattern = Pattern.compile(regex);
			Matcher matcher = compiledPattern.matcher(youTubeLinkWithoutProtocolAndDomain);

			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

	private static String youTubeLinkWithoutProtocolAndDomain(String url) {
		Pattern compiledPattern = Pattern.compile(youTubeUrlRegEx);
		Matcher matcher = compiledPattern.matcher(url);

		if (matcher.find()) {
			return url.replace(matcher.group(), "");
		}
		return url;
	}

	public static long getDuration(String ytd) {
		String time = ytd.substring(2);
		long duration = 0L;
		Object[][] indexs = new Object[][]{{"H", 3600}, {"M", 60}, {"S", 1}};
		for (int i = 0; i < indexs.length; i++) {
			int index = time.indexOf((String) indexs[i][0]);
			if (index != -1) {
				String value = time.substring(0, index);
				duration += Integer.parseInt(value) * (int) indexs[i][1] * 1000;
				time = time.substring(value.length() + 1);
			}
		}
		return duration;
	}

}
