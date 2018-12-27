package com.ilusons.harmony.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.SecurePreferences;
import com.ilusons.harmony.ref.YouTubeEx;

import org.apache.http.util.TextUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import at.huber.youtubeExtractor.Format;
import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Period;
import de.umass.lastfm.Session;
import de.umass.lastfm.Tag;
import de.umass.lastfm.Track;
import de.umass.lastfm.cache.FileSystemCache;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.musicbrainz.android.api.User;
import org.musicbrainz.android.api.data.Recording;
import org.musicbrainz.android.api.data.RecordingInfo;
import org.musicbrainz.android.api.webservice.MusicBrainzWebClient;

import java.net.URLEncoder;

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

	public static String getKey() {
		return "050059635d31b4ce8d4a08384ef832f3";
	}

	public static String getSecret() {
		return "c8bac34a977592eecabc10efe71c5a38";
	}

	//region App

	/*
	last.fm account

	harmony_ilusons
	%"8Gp.
	(% " 8 GOLF park .)
	harmony@ilusons.com

	050059635d31b4ce8d4a08384ef832f3
	c8bac34a977592eecabc10efe71c5a38

	*/

	private static String getLastfmForAppUsername() {
		return "harmony_ilusons";
	}

	private static String getLastfmForAppPassword() {
		return "%\"8Gp.";
	}

	private Session lastfmSessionForApp;

	private Observable<Session> getLastfmSessionForApp() {
		return Observable.create(new ObservableOnSubscribe<Session>() {
			@Override
			public void subscribe(ObservableEmitter<Session> oe) throws Exception {
				try {
					if (lastfmSessionForApp == null) {
						lastfmSessionForApp = Authenticator.getMobileSession(
								getLastfmForAppUsername(),
								getLastfmForAppPassword(),
								getKey(),
								getSecret());
					}

					oe.onNext(lastfmSessionForApp);

					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	private Observable<ScrobbleResult> scrobbleLastfmForApp(final MusicService musicService, final Music data) {
		return getLastfmSessionForApp()
				.flatMap(new Function<Session, ObservableSource<ScrobbleResult>>() {
					@Override
					public ObservableSource<ScrobbleResult> apply(final Session session) throws Exception {
						return Observable.create(new ObservableOnSubscribe<ScrobbleResult>() {
							@Override
							public void subscribe(ObservableEmitter<ScrobbleResult> oe) throws Exception {
								try {
									if (!AndroidEx.hasInternetConnection(musicService))
										throw new Exception("Network not available!");

									if (!canCall())
										throw new Exception("Calls exceeded!");

									if (canScrobble(musicService, data)) {
										ScrobbleData scrobbleData = new ScrobbleData();
										scrobbleData.setArtist(data.getArtist());
										scrobbleData.setTrack(data.getTitle());
										scrobbleData.setDuration(data.getLength() / 1000);
										scrobbleData.setTimestamp((int) (System.currentTimeMillis() / 1000));

										ScrobbleResult result = de.umass.lastfm.Track.scrobble(scrobbleData, session);

										scrobbleResults.add(result);

										oe.onNext(result);
									}

									oe.onComplete();
								} catch (Exception e) {
									oe.onError(e);
								}
							}
						});
					}
				});
	}

	public Observable<Collection<de.umass.lastfm.Track>> getTopTracksForLastfmForApp() {
		return getLastfmSessionForApp()
				.flatMap(new Function<Session, ObservableSource<Collection<de.umass.lastfm.Track>>>() {
					@Override
					public ObservableSource<Collection<de.umass.lastfm.Track>> apply(final Session session) throws Exception {
						return Observable.create(new ObservableOnSubscribe<Collection<de.umass.lastfm.Track>>() {
							@Override
							public void subscribe(ObservableEmitter<Collection<de.umass.lastfm.Track>> oe) throws Exception {
								try {
									if (!canCall())
										throw new Exception("Calls exceeded!");

									Collection<de.umass.lastfm.Track> tracks = new ArrayList<>();

									tracks.addAll(de.umass.lastfm.User.getTopTracks(
											getLastfmForAppUsername(),
											Period.WEEK,
											getKey()));

									if (tracks.size() < 7) {
										tracks.clear();
										tracks.addAll(de.umass.lastfm.User.getTopTracks(
												getLastfmForAppUsername(),
												Period.OVERALL,
												getKey()));
									}

									oe.onNext(tracks);

									oe.onComplete();
								} catch (Exception e) {
									oe.onError(e);
								}
							}
						});
					}
				});
	}

	//endregion

	//region User

	private final static String KEY_LFM_USERNAME = "lfm_username";

	public String getLastfmUsername() {
		return securePreferences.getString(KEY_LFM_USERNAME, null);
	}

	private final static String KEY_LFM_PASSWORD = "lfm_password";

	public String getLastfmPassword() {
		return securePreferences.getString(KEY_LFM_PASSWORD, null);
	}

	private Session lfm_session;

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

	private final static String KEY_LFM_SCACHE = "lfm_scache";
	private List<ScrobbleData> scrobbleCache = new ArrayList<>();
	private List<ScrobbleResult> scrobbleResults = new ArrayList<>();

	public List<ScrobbleResult> getScrobblerResultsForLastfm() {
		return scrobbleResults;
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
					(new CreateLastfmSession(context)).execute().get(1, TimeUnit.SECONDS);
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

	public void nowPlayingLastfm(MusicService musicService, Music data) {

		(new RunNowPlayingLastfm(this, musicService, data)).execute();

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
					(new CreateLastfmSession(context)).execute().get(1, TimeUnit.SECONDS);
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

	public boolean canScrobble(MusicService musicService, Music data) {
		if (!AndroidEx.hasInternetConnection(musicService))
			return false;

		boolean duration30s = data.getLength() > 30 * 1000;
		boolean playing = musicService.isPlaying();
		boolean playedHalf = ((float) musicService.getPosition() / (float) musicService.getDuration()) >= 0.5f;
		boolean played4min = musicService.getPosition() >= 4 * 60 * 1000;

		return duration30s && playing && (true || playedHalf || played4min);
	}

	public void scrobbleLastfm(MusicService musicService, Music data) {

		(new RunScrobbleLastfm(this, musicService, data)).execute();

	}

	//endregion

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
		if (!getDCEnabled())
			return;

		if (firebaseAnalytics != null) {
			try {
				Bundle bundle = new Bundle();
				bundle.putString("title", data.getTitle());
				bundle.putString("artist", data.getArtist());
				firebaseAnalytics.logEvent("music_opened", bundle);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		scrobbleLastfmForApp(musicService, data)
				.subscribeOn(Schedulers.io())
				.subscribe(new Consumer<ScrobbleResult>() {
					@Override
					public void accept(ScrobbleResult r) throws Exception {
						Log.d(TAG, "scrobble" + "\n" + r);
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {
						Log.w(TAG, throwable);
					}
				});
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
					// oe.onError(e);

					oe.onComplete();
				}
			}
		});
	}

	public static Observable<Collection<de.umass.lastfm.Track>> findTracks(final String query, final int limit) {
		return Observable.create(new ObservableOnSubscribe<Collection<de.umass.lastfm.Track>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<de.umass.lastfm.Track>> oe) throws Exception {
				try {
					ArrayList<Track> tracks = new ArrayList<>();

					if (canCall()) {
						Collection<de.umass.lastfm.Track> searched = de.umass.lastfm.Track.search(null, query, limit / 3, getKey());
						tracks.addAll(searched);
					}

					if (canCall()) {
						Collection<de.umass.lastfm.Track> searched = de.umass.lastfm.Tag.getTopTracks(query, limit / 3, getKey());
						tracks.addAll(searched);
					}

					if (canCall()) {
						Collection<de.umass.lastfm.Track> searched = de.umass.lastfm.Artist.getTopTracks(query, limit / 3, getKey());
						tracks.addAll(searched);
					}

					if (tracks.size() == 0)
						throw new Exception("Nothing found!");

					Collections.shuffle(tracks);

					oe.onNext(tracks);

					oe.onComplete();
				} catch (Exception e) {
					// oe.onError(e); // TODO: Handle error

					oe.onComplete();
				}
			}
		});
	}

	public static Observable<Collection<de.umass.lastfm.Track>> getTopTracksForLastfm(final Context context) {
		return Observable.create(new ObservableOnSubscribe<Collection<de.umass.lastfm.Track>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<de.umass.lastfm.Track>> oe) throws Exception {
				try {
					ArrayList<Track> tracks = new ArrayList<>();

					final String country = context.getResources().getConfiguration().locale.getDisplayCountry();

					/*
					if (canCall()) {
						Collection<Track> regionalTracks = de.umass.lastfm.Geo.getTopTracks(country, getKey());
						tracks.addAll(regionalTracks);
					}
					*/

					if (tracks.size() <= 1 && canCall()) {
						PaginatedResult<Track> topTracks = de.umass.lastfm.Chart.getTopTracks(getKey());
						tracks.addAll(topTracks.getPageResults());
					}

					if (tracks.size() == 0)
						throw new Exception("Nothing found!");

					Collections.shuffle(tracks);

					oe.onNext(tracks);

					oe.onComplete();
				} catch (Exception e) {
					// oe.onError(e); TODO: Fix network issue

					oe.onComplete();
				}
			}
		});
	}

	public static Observable<Collection<de.umass.lastfm.Tag>> getTagsFromLastfm(final Music music) {
		return Observable.create(new ObservableOnSubscribe<Collection<de.umass.lastfm.Tag>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<de.umass.lastfm.Tag>> oe) throws Exception {
				try {
					ArrayList<Tag> tags = new ArrayList<>();

					if (canCall()) {
						String titleOrMBID = music.getTitle();
						if (!TextUtils.isEmpty(music.getMBID()))
							titleOrMBID = music.getMBID();

						Collection<Tag> topTags = de.umass.lastfm.Track.getTopTags(music.getArtist(), titleOrMBID, getKey());
						tags.addAll(topTags);
					}

					if (tags.size() <= 1 && canCall()) {
						Collection<Tag> topTags = de.umass.lastfm.Artist.getTopTags(music.getArtist(), getKey());
						tags.addAll(topTags);
					}

					if (tags.size() == 0)
						throw new Exception("Nothing found!");

					oe.onNext(tags);

					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Collection<Music>> convertToLocal(final Context context, final Collection<de.umass.lastfm.Track> tracks, final int limit) {
		return Observable.create(new ObservableOnSubscribe<Collection<Music>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<Music>> oe) throws Exception {
				try {
					final ArrayList<Music> r = new ArrayList<>();

					JaroWinklerDistance sa = new JaroWinklerDistance();

					Collection<Music> local = new ArrayList<>();
					try (Realm realm = Music.getDB()) {
						if (realm != null) {
							local.addAll(realm.copyFromRealm(realm.where(Music.class).findAll()));
						}
					}

					int count = 0;

					for (de.umass.lastfm.Track t : tracks) {
						Music m = null;

						for (Music l : local)
							try {
								if (sa.apply(t.getName(), l.getTitle()) > 0.8
										&& sa.apply(t.getArtist(), l.getArtist()) > 0.8) {
									m = l;
									local.remove(l);
									break;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

						if (m == null) {
							m = new Music();
							m.setTitle(t.getName());
							m.setArtist(t.getArtist());
							m.setAlbum(t.getAlbum());
							m.setMBID(t.getMbid());
							m.setTags(StringUtils.join(t.getTags(), ','));
							m.setLength(t.getDuration());
							m.setPath(t.getUrl());
						}

						r.add(m);

						count++;
						if (count >= limit)
							break;
					}

					oe.onNext(r);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Collection<Music>> convertToLocal(final Context context, final Collection<de.umass.lastfm.Track> tracks, final Collection<Music> old, final int limit) {
		return Observable.create(new ObservableOnSubscribe<Collection<Music>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<Music>> oe) throws Exception {
				try {
					final ArrayList<Music> r = new ArrayList<>();

					r.addAll(old);

					JaroWinklerDistance sa = new JaroWinklerDistance();

					int count = 0;

					for (de.umass.lastfm.Track t : tracks) {
						Music m = null;

						for (Music l : old)
							try {
								if (sa.apply(t.getName(), l.getTitle()) > 0.8
										&& sa.apply(t.getArtist(), l.getArtist()) > 0.8) {
									m = l;
									old.remove(l);
									break;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

						if (m == null) {
							m = new Music();
							m.setTitle(t.getName());
							m.setArtist(t.getArtist());
							m.setAlbum(t.getAlbum());
							m.setMBID(t.getMbid());
							m.setTags(StringUtils.join(t.getTags(), ','));
							m.setLength(t.getDuration());
							m.setPath(t.getUrl());
						}

						r.add(m);

						count++;
						if (count >= limit)
							break;
					}

					oe.onNext(r);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	//region YT

	private static String getYouTubeAPIKey() {
		return "AIzaSyCbI-1cSfDdoMIjXhNKznhxn5L_ZOwtB3A";
	}

	public static Observable<Collection<String>> getYouTubeUrls(final Context context, final String queryTerm, final long limit) {
		return Observable.create(new ObservableOnSubscribe<Collection<String>>() {
			@SuppressLint("StaticFieldLeak")
			@Override
			public void subscribe(ObservableEmitter<Collection<String>> oe) throws Exception {
				try {
					final ArrayList<String> r = new ArrayList<>();

					try {
						// This object is used to make YouTube Data API requests. The last
						// argument is required, but since we don't need anything
						// initialized when the HttpRequest is initialized, we override
						// the interface and provide a no-op function.
						YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
							public void initialize(HttpRequest request) throws IOException {
							}
						}).setApplicationName(context.getString(R.string.app_name)).build();

						// Define the API request for retrieving search results.
						YouTube.Search.List search = youtube.search().list("id,snippet");

						// Set your developer key from the {{ Google Cloud Console }} for
						// non-authenticated requests. See:
						// {{ https://cloud.google.com/console }}
						String apiKey = getYouTubeAPIKey();
						search.setKey(apiKey);
						search.setQ(URLDecoder.decode(queryTerm, "utf-8"));

						// Restrict the search results to only include videos. See:
						// https://developers.google.com/youtube/v3/docs/search/list#type
						search.setType("video");

						// To increase efficiency, only retrieve the fields that the
						// application uses.
						search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
						search.setMaxResults(limit);

						// Call the API and process results.
						SearchListResponse searchResponse = search.execute();
						List<SearchResult> searchResultList = searchResponse.getItems();
						if (searchResultList != null) {
							for (SearchResult sr : searchResultList) {
								r.add("http://" + "youtube.com/watch?v=" + sr.getId().getVideoId());
							}
						}

					} catch (GoogleJsonResponseException e) {
						System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
					} catch (IOException e) {
						System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
					} catch (Throwable t) {
						t.printStackTrace();
					}

					oe.onNext(r);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Video> getYouTubeVideoFromUrl(final Context context, final String url) {
		return Observable.create(new ObservableOnSubscribe<Video>() {
			@SuppressLint("StaticFieldLeak")
			@Override
			public void subscribe(ObservableEmitter<Video> oe) throws Exception {
				try {
					Video r = null;

					try {
						YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
							public void initialize(HttpRequest request) throws IOException {
							}
						}).setApplicationName(context.getString(R.string.app_name)).build();

						YouTube.Videos.List listRequest = youtube.videos().list("statistics");
						listRequest.setKey(getYouTubeAPIKey());
						listRequest.setId(YouTubeEx.extractVideoIdFromUrl(url));
						listRequest.setPart("snippet");
						VideoListResponse listResponse = listRequest.execute();

						r = listResponse.getItems().get(0);
					} catch (IOException e) {
						System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
					} catch (Throwable t) {
						t.printStackTrace();
					}

					oe.onNext(r);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<String> getYouTubeAudioUrl(final Context context, final String watchUrl) {
		return Observable.create(new ObservableOnSubscribe<String>() {
			@SuppressLint("StaticFieldLeak")
			@Override
			public void subscribe(ObservableEmitter<String> oe) throws Exception {
				try {
					if (!watchUrl.toLowerCase().contains("youtube")) {
						oe.onComplete();
						return;
					}

					YouTubeExtractor yte = new YouTubeExtractor(context) {
						@Override
						public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {

						}
					};
					yte.setDefaultHttpProtocol(true);
					yte.setParseDashManifest(true);
					yte.setIncludeWebM(true);

					ArrayList<YtFile> selected = new ArrayList<>();

					SparseArray<YtFile> ytFiles = yte.execute(watchUrl).get(3L, TimeUnit.MINUTES);
					for (int i = 0, itag; i < ytFiles.size(); i++) {
						itag = ytFiles.keyAt(i);
						YtFile ytFile = ytFiles.get(itag);
						Format format = ytFile.getFormat();

						if (format.getAudioBitrate() > 0) {
							selected.add(ytFile);
						}
					}

					Collections.sort(selected, Collections.reverseOrder(new Comparator<YtFile>() {
						@Override
						public int compare(YtFile l, YtFile r) {
							return Integer.compare(l.getFormat().getAudioBitrate(), r.getFormat().getAudioBitrate());
						}
					}));

					if (selected.size() > 0)
						oe.onNext(selected.get(0).getUrl());

					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	//endregion

	//endregion

}
