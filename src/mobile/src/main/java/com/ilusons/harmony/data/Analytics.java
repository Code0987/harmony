package com.ilusons.harmony.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.SecurePreferences;

import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class Analytics {

	// Logger TAG
	private static final String TAG = Analytics.class.getSimpleName();

	private static Analytics instance;

	public static Analytics getInstance() {
		if (instance == null)
			instance = new Analytics();
		return instance;
	}

	//region Settings

	private SecurePreferences securePreferences;
	private final static String securePreferencesFile = "settings_analytics.xml";

	public void initSettings(Context context) {
		try {
			securePreferences = new SecurePreferences(context, context.getPackageName(), securePreferencesFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//endregion

	//region last.fm

	private final static String KEY_LFM_USERNAME = "lfm_username";
	private final static String KEY_LFM_PASSWORD = "lfm_password";

	private Session lfm_session;

	private final static String KEY_LFM_SCACHE = "lfm_scache";

	private List<ScrobbleData> scrobbleCache = new ArrayList<>();

	private List<ScrobbleResult> scrobbleResults = new ArrayList<>();

	public void initLastfm() {
		if (securePreferences == null)
			return;

		try {
			String username = securePreferences.getString(KEY_LFM_USERNAME, null);
			String password = securePreferences.getString(KEY_LFM_PASSWORD, null);

			if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
				return;

			lfm_session = Authenticator.getMobileSession(
					username,
					password,
					"7f549d7402bd35d37f3711c40a84ec95",
					"350215664ed8c4b13a27ed56a3da51d5");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Session getLastfmSession() {
		return lfm_session;
	}

	public boolean isLastfmScrobbledEnabled() {
		String username = securePreferences.getString(KEY_LFM_USERNAME, null);
		String password = securePreferences.getString(KEY_LFM_PASSWORD, null);

		return !(TextUtils.isEmpty(username) || TextUtils.isEmpty(password));
	}

	public void setLastfmCredentials(String username, String password) {
		securePreferences.edit()
				.putString(KEY_LFM_USERNAME, username)
				.putString(KEY_LFM_PASSWORD, password)
				.apply();
	}

	public String getLastfmUsername() {
		return securePreferences.getString(KEY_LFM_USERNAME, null);
	}

	public String getLastfmPassword() {
		return securePreferences.getString(KEY_LFM_PASSWORD, null);
	}

	public void nowPlayingLastfm(MusicService musicService, Music data) {
		if (lfm_session == null && !isLastfmScrobbledEnabled())
			return;

		if (!canScrobble(musicService, data))
			return;

		ScrobbleData scrobbleData = new ScrobbleData();
		scrobbleData.setArtist(data.Artist);
		scrobbleData.setTrack(data.Title);
		scrobbleData.setDuration(data.Length / 1000);

		ScrobbleResult result = Track.updateNowPlaying(scrobbleData, lfm_session);

		Log.d(TAG, "nowPlayingLastfm" + "\n" + result);
	}

	public boolean canScrobble(MusicService musicService, Music data) {
		boolean duration30s = data.Length > 30 * 1000;
		boolean playing = musicService.isPlaying();
		boolean playedHalf = ((float) musicService.getPosition() / (float) musicService.getDuration()) >= 0.5f;
		boolean played4min = musicService.getPosition() >= 4 * 60 * 1000;

		return duration30s && playing && (playedHalf || played4min);
	}

	public void scrobbleLastfm(MusicService musicService, Music data) {
		if (lfm_session == null && !isLastfmScrobbledEnabled())
			return;

		if (!canScrobble(musicService, data))
			return;

		ScrobbleData scrobbleData = new ScrobbleData();
		scrobbleData.setArtist(data.Artist);
		scrobbleData.setTrack(data.Title);
		scrobbleData.setDuration(data.Length / 1000);
		scrobbleData.setTimestamp((int) (System.currentTimeMillis() / 1000));

		if (lfm_session == null) {

			scrobbleCache.add(scrobbleData);

			Log.d(TAG, "scrobbleLastfm\ncached" + "\n" + scrobbleCache);

		} else {

			if (scrobbleCache.size() > 0) {

				scrobbleCache.add(scrobbleData);

				List<ScrobbleResult> result = Track.scrobble(scrobbleCache, lfm_session);

				scrobbleCache.clear();

				scrobbleResults.addAll(result);

				Log.d(TAG, "scrobbleLastfm" + "\n" + result);
			} else {

				ScrobbleResult result = Track.scrobble(scrobbleData, lfm_session);

				Log.d(TAG, "scrobbleLastfm" + "\n" + result);

				scrobbleResults.add(result);

			}

		}

	}

	public List<ScrobbleResult> getScrobblerResultsForLastfm() {
		return scrobbleResults;
	}

	//endregion

}
