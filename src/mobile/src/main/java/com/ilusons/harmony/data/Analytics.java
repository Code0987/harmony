package com.ilusons.harmony.data;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.SecurePreferences;

import org.apache.http.util.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogRecord;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import io.realm.Realm;
import jonathanfinerty.once.Once;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.musicbrainz.android.api.User;
import org.musicbrainz.android.api.data.Artist;
import org.musicbrainz.android.api.data.ArtistSearchResult;
import org.musicbrainz.android.api.data.Recording;
import org.musicbrainz.android.api.data.RecordingInfo;
import org.musicbrainz.android.api.data.Track;
import org.musicbrainz.android.api.handler.RecordingSearchHandler;
import org.musicbrainz.android.api.webservice.MusicBrainzWebClient;

import java.net.URLEncoder;
import java.util.LinkedList;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

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

	//region General

	private final static String KEY_FIRST_RUN_TIMESTAMP = "first_run_timestamp";

	public long getTimeSinceFirstRun() {
		try {
			long currentMillis = System.currentTimeMillis();
			long timeOfAbsoluteFirstLaunch = securePreferences.getLong(KEY_FIRST_RUN_TIMESTAMP, 0);
			if (timeOfAbsoluteFirstLaunch == 0) {
				timeOfAbsoluteFirstLaunch = currentMillis;
				securePreferences.edit().putLong(KEY_FIRST_RUN_TIMESTAMP, timeOfAbsoluteFirstLaunch).apply();
			}
			return (currentMillis - timeOfAbsoluteFirstLaunch);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}


	//endregion

	//region last.fm

	private final static String KEY_LFM_USERNAME = "lfm_username";
	private final static String KEY_LFM_PASSWORD = "lfm_password";

	private Session lfm_session;

	private final static String KEY_LFM_SCACHE = "lfm_scache";

	private List<ScrobbleData> scrobbleCache = new ArrayList<>();

	private List<ScrobbleResult> scrobbleResults = new ArrayList<>();
	private static int callsCount = 0;
	private static long lastCallCountStart = 0;

	public static boolean canCall() {
		boolean r = true;

		long now = System.currentTimeMillis();

		if ((now - lastCallCountStart) > 60 * 1000) {
			callsCount = 0;
			lastCallCountStart = now;
		} else {
			if (callsCount >= 5)
				r = false;
		}

		callsCount++;

		return r;
	}

	public static String getKey() {
		return "7f549d7402bd35d37f3711c40a84ec95";
	}

	public static String getSecret() {
		return "350215664ed8c4b13a27ed56a3da51d5";
	}

	public void initLastfm(Context context) {
		// For api

		Caller.getInstance().setCache(new FileSystemCache(context.getCacheDir()));
		if (!BuildConfig.DEBUG)
			Caller.getInstance().setUserAgent(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_CODE);

		// For scrobbler

		if (securePreferences == null)
			return;

		(new CreateLastfmSession(this)).execute();
	}

	private static class CreateLastfmSession extends AsyncTask<Void, Void, Void> {
		private WeakReference<Analytics> contextRef;

		public CreateLastfmSession(Analytics context) {
			this.contextRef = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			Analytics context = contextRef.get();
			if (context == null)
				return null;

			try {
				String username = context.securePreferences.getString(KEY_LFM_USERNAME, null);
				String password = context.securePreferences.getString(KEY_LFM_PASSWORD, null);

				if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
					context.lfm_session = null;
					return null;
				}

				context.lfm_session = Authenticator.getMobileSession(
						username,
						password,
						getKey(),
						getSecret());
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
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

		(new RunNowPlayingLastfm(this, musicService, data)).execute();

	}

	private static class RunNowPlayingLastfm extends AsyncTask<Void, Void, Void> {
		private WeakReference<Analytics> contextRef;
		private WeakReference<MusicService> musicServiceRef;
		private WeakReference<Music> dataRef;

		public RunNowPlayingLastfm(Analytics context, MusicService musicService, Music data) {
			this.contextRef = new WeakReference<>(context);
			this.musicServiceRef = new WeakReference<>(musicService);
			this.dataRef = new WeakReference<>(data);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				Analytics context = contextRef.get();
				MusicService musicService = musicServiceRef.get();
				Music data = dataRef.get();
				if (context == null)
					return null;
				if (musicService == null)
					return null;
				if (data == null)
					return null;

				if (context.lfm_session == null && !context.isLastfmScrobbledEnabled())
					return null;

				if (context.lfm_session == null) try {
					(new CreateLastfmSession(context)).execute().wait();
				} catch (Exception e) {
					e.printStackTrace();
					if (context.lfm_session == null)
						return null;
				}

				ScrobbleData scrobbleData = new ScrobbleData();
				scrobbleData.setArtist(data.getArtist());
				scrobbleData.setTrack(data.getTitle());
				scrobbleData.setDuration(data.getLength() / 1000);

				ScrobbleResult result = de.umass.lastfm.Track.updateNowPlaying(scrobbleData, context.lfm_session);

				Log.d(TAG, "nowPlayingLastfm" + "\n" + result);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	public boolean canScrobble(MusicService musicService, Music data) {
		boolean duration30s = data.getLength() > 30 * 1000;
		boolean playing = musicService.isPlaying();
		boolean playedHalf = ((float) musicService.getPosition() / (float) musicService.getDuration()) >= 0.5f;
		boolean played4min = musicService.getPosition() >= 4 * 60 * 1000;

		return duration30s && playing && (true || playedHalf || played4min);
	}

	public void scrobbleLastfm(MusicService musicService, Music data) {

		(new RunScrobbleLastfm(this, musicService, data)).execute();

	}

	private static class RunScrobbleLastfm extends AsyncTask<Void, Void, Void> {
		private WeakReference<Analytics> contextRef;
		private WeakReference<MusicService> musicServiceRef;
		private WeakReference<Music> dataRef;

		public RunScrobbleLastfm(Analytics context, MusicService musicService, Music data) {
			this.contextRef = new WeakReference<>(context);
			this.musicServiceRef = new WeakReference<>(musicService);
			this.dataRef = new WeakReference<>(data);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			try {
				Analytics context = contextRef.get();
				MusicService musicService = musicServiceRef.get();
				Music data = dataRef.get();
				if (context == null)
					return null;
				if (musicService == null)
					return null;
				if (data == null)
					return null;

				if (context.lfm_session == null && !context.isLastfmScrobbledEnabled())
					return null;

				if (context.lfm_session == null) try {
					(new CreateLastfmSession(context)).execute().wait();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!context.canScrobble(musicService, data))
					return null;

				ScrobbleData scrobbleData = new ScrobbleData();
				scrobbleData.setArtist(data.getArtist());
				scrobbleData.setTrack(data.getTitle());
				scrobbleData.setDuration(data.getLength() / 1000);
				scrobbleData.setTimestamp((int) (System.currentTimeMillis() / 1000));

				if (context.lfm_session == null) {

					context.scrobbleCache.add(scrobbleData);

					Log.d(TAG, "scrobbleLastfm\ncached" + "\n" + context.scrobbleCache);

				} else {

					if (context.scrobbleCache.size() > 0) {

						context.scrobbleCache.add(scrobbleData);

						List<ScrobbleResult> result = de.umass.lastfm.Track.scrobble(context.scrobbleCache, context.lfm_session);

						context.scrobbleCache.clear();

						context.scrobbleResults.addAll(result);

						Log.d(TAG, "scrobbleLastfm" + "\n" + result);
					} else {

						ScrobbleResult result = de.umass.lastfm.Track.scrobble(scrobbleData, context.lfm_session);

						Log.d(TAG, "scrobbleLastfm" + "\n" + result);

						context.scrobbleResults.add(result);

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	public List<ScrobbleResult> getScrobblerResultsForLastfm() {
		return scrobbleResults;
	}

	//endregion

	//region DC

	public static final String KEY_DC_ENABLED = "dc_enabled";

	public boolean getDCEnabled() {
		try {
			return securePreferences.getBoolean(KEY_DC_ENABLED, true);
		} catch (Exception e) {
			return false;
		}
	}

	public void setDCEnabled(boolean value) {
		try {
			securePreferences
					.edit()
					.putBoolean(KEY_DC_ENABLED, value)
					.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private FirebaseAnalytics firebaseAnalytics;

	public void initDC(Context context) {
		if (getDCEnabled())
			try {
				firebaseAnalytics = FirebaseAnalytics.getInstance(context);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public void logMusicOpened(MusicService musicService, Music data) {
		if (firebaseAnalytics == null)
			return;

		try {
			Bundle bundle = new Bundle();
			bundle.putString("title", data.getTitle());
			bundle.putString("artist", data.getArtist());
			firebaseAnalytics.logEvent("music_opened", bundle);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//endregion

	//region Smart

	public static MusicBrainzWebClient createClient() {
		return new MusicBrainzWebClient(
				new User() {
					@Override
					public String getUsername() {
						return "ikCePnurwp1qxVIjxOXR6Q";
					}

					@Override
					public String getPassword() {
						return "1MFKDP5z_pgZ0Snf3g5CYQ";
					}
				},
				BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_CODE,
				BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_CODE);
	}

	public static Observable<RecordingInfo> findTrackFromQuery(final String query) {
		return Observable.create(new ObservableOnSubscribe<RecordingInfo>() {
			@Override
			public void subscribe(ObservableEmitter<RecordingInfo> oe) throws Exception {
				try {
					MusicBrainzWebClient mbc = createClient();

					for (RecordingInfo item : mbc.searchRecording(URLEncoder.encode(query, "UTF-8")))
						try {
							oe.onNext(item);
						} catch (Exception e) {
							e.printStackTrace();
						}
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<RecordingInfo> findTrackFromTitleArtist(final String title, final String artist) {
		return Observable.create(new ObservableOnSubscribe<RecordingInfo>() {
			@Override
			public void subscribe(ObservableEmitter<RecordingInfo> oe) throws Exception {
				try {
					boolean f = false;

					MusicBrainzWebClient mbc = createClient();

					JaroWinklerDistance sa = new JaroWinklerDistance();

					for (RecordingInfo item : mbc.searchRecording(URLEncoder.encode("recording:" + title + " AND artist:" + artist, "UTF-8")))
						try {
							if (sa.apply(item.getTitle().toLowerCase(), title.toLowerCase()) > 0.85
									&& sa.apply(item.getArtist().getName().toLowerCase(), artist.toLowerCase()) > 0.85) {
								oe.onNext(item);
								f = true;
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					if (!f)
						throw new Exception("Not found");
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Recording> findTrackFromMBID(final String mbid) {
		return Observable.create(new ObservableOnSubscribe<Recording>() {
			@Override
			public void subscribe(ObservableEmitter<Recording> oe) throws Exception {
				try {
					boolean f = false;

					MusicBrainzWebClient mbc = createClient();

					Recording track = mbc.lookupRecording(mbid);
					if (track != null) {
						oe.onNext(track);
						f = true;
					}

					if (!f)
						throw new Exception("Not found");
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Collection<de.umass.lastfm.Track>> findSimilarTracks(final String artist, final String trackOrMbid, final int limit) {
		return Observable.create(new ObservableOnSubscribe<Collection<de.umass.lastfm.Track>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<de.umass.lastfm.Track>> oe) throws Exception {
				try {
					boolean f = false;

					if (canCall()) {
						Collection<de.umass.lastfm.Track> similar = de.umass.lastfm.Track.getSimilar(artist, trackOrMbid, getKey(), limit);
						oe.onNext(similar);
						f = true;
					}

					if (!f)
						throw new Exception("Not found");
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}


	//endregion

}
