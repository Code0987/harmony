package com.ilusons.harmony.ref;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongsEx {

	public static final String TAG = SongsEx.class.getSimpleName();

	private static Pattern SongFileName1 = Pattern.compile(
			"(.*)(\\s?[-]\\s?)(.*)",
			Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	public static ArrayList<String> getArtistAndTitle(String filename) {
		ArrayList<String> result = new ArrayList<>();

		// Remove extension
		filename = filename.replaceFirst("[.][^.]+$", "");

		Matcher m = SongFileName1.matcher(filename);
		while (m.find()) {
			String artist = m.group(1).trim();
			String title = m.group(3).split("_|\\(|:|-|\\[|\\.")[0].trim();
			if (TextUtils.isEmpty(title))
				title = m.group(3).trim();

			// Replace _
			artist = artist.replace("_", " ");
			title = title.replace("_", " ");

			result.add(artist);
			result.add(title);
		}

		return result;
	}

}
