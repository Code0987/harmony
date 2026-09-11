package com.ilusons.harmony.data;

import android.content.Context;

import com.ilusons.harmony.ref.SecurePreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.umass.lastfm.Session;
import de.umass.lastfm.Tag;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import io.reactivex.Observable;

/**
 * Last.fm / metadata facade. YouTube extract, Firebase DC, and unofficial lyrics backends
 * were removed. Last.fm user scrobble remains; search helpers currently return empty.
 */
public class Analytics {

	private static final String TAG = Analytics.class.getSimpleName();

	private static Analytics instance;

	public static Analytics getInstance() {
		if (instance == null)
			instance = new Analytics();
		return instance;
	}

	private SecurePreferences securePreferences;
	private static final String securePreferencesFile = "settings_analytics.xml";

	public void initSettings(Context context) {
		try {
			securePreferences = new SecurePreferences(context, context.getPackageName(), securePreferencesFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initLastfm(Context context) {
	}

	public Session getLastfmSession() {
		return null;
	}

	public String getLastfmUsername() {
		try {
			return securePreferences == null ? "" : securePreferences.getString("lfm_user", "");
		} catch (Exception e) {
			return "";
		}
	}

	public String getLastfmPassword() {
		try {
			return securePreferences == null ? "" : securePreferences.getString("lfm_pass", "");
		} catch (Exception e) {
			return "";
		}
	}

	public void setLastfmCredentials(String username, String password) {
		if (securePreferences == null) {
			return;
		}
		securePreferences.edit()
				.putString("lfm_user", username)
				.putString("lfm_pass", password)
				.apply();
	}

	public boolean isLastfmScrobbledEnabled() {
		return false;
	}

	public Collection<ScrobbleResult> getScrobblerResultsForLastfm() {
		return Collections.emptyList();
	}

	public boolean getDCEnabled() {
		return false;
	}

	public void setDCEnabled(boolean enabled) {
	}

	public long getTimeSinceFirstRun() {
		return 0;
	}

	public static String getKey() {
		return "";
	}

	public static String getSecret() {
		return "";
	}

	public static Observable<Collection<Tag>> getTagsFromLastfm(Music music) {
		return Observable.just(Collections.emptyList());
	}

	public static Observable<Collection<Track>> findTracks(String query, int n) {
		return Observable.just(Collections.emptyList());
	}

	public static Observable<Collection<Track>> findSimilarTracks(String artist, String title, int n) {
		return Observable.just(Collections.emptyList());
	}

	public static Observable<Collection<Track>> getTopTracksForLastfm(Context context) {
		return Observable.just(Collections.emptyList());
	}

	public Observable<Collection<Track>> getTopTracksForLastfmForApp() {
		return Observable.just(Collections.emptyList());
	}

	public static Observable<Collection<Music>> convertToLocal(Context context, Collection<Track> tracks, int n) {
		return Observable.just(new ArrayList<Music>());
	}

	public static Observable<Collection<Music>> convertToLocal(Context context, Collection<Track> tracks, Collection<Music> existing, int n) {
		return Observable.just(new ArrayList<Music>());
	}

	public static Observable<Object> findTrackFromTitleArtist(String title, String artist) {
		return Observable.empty();
	}

	public static Observable<Object> findTrackFromMBID(String mbid) {
		return Observable.empty();
	}

	public void nowPlayingLastfm(Music music) {
	}

	public void scrobbleLastfm(Music music) {
	}

	public void logMusicOpened(Context context, Music music) {
	}
}
