package com.ilusons.harmony.base;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.PlaybackVisualizer;
import com.ilusons.harmony.data.LibraryStore;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.SPrefEx;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import android.net.Uri;

public class MusicService extends MediaSessionService {

	private static final String TAG = MusicService.class.getSimpleName();

	public static final String ACTION_CLOSE = TAG + ".close";
	public static final String ACTION_PREVIOUS = TAG + ".previous";
	public static final String ACTION_NEXT = TAG + ".next";
	public static final String ACTION_PLAY = TAG + ".play";
	public static final String ACTION_PAUSE = TAG + ".pause";
	public static final String ACTION_STOP = TAG + ".stop";
	public static final String ACTION_TOGGLE_PLAYBACK = TAG + ".toggle_playback";
	public static final String ACTION_RANDOM = TAG + ".random";
	public static final String ACTION_OPEN = TAG + ".open";
	public static final String ACTION_OPEN_YTS = TAG + ".open_yts";
	public static final String KEY_URI = "uri";
	public static final String ACTION_PREPARED = TAG + ".prepared";
	public static final String ACTION_LIBRARY_UPDATE = TAG + ".library_update";
	public static final String KEY_LIBRARY_UPDATE_FORCE = "force";
	public static final String KEY_LIBRARY_UPDATE_FASTMODE = "fast_mode";
	public static final String TAG_SPREF_LIBRARY_UPDATE_FASTMODE = SPrefEx.TAG_SPREF + ".library_update_fast_mode";
	public static final boolean LIBRARY_UPDATE_FASTMODE_DEFAULT = false;
	public static final String ACTION_LIBRARY_UPDATE_BEGINS = TAG + ".library_update_begins";
	public static final String ACTION_LIBRARY_UPDATED = TAG + ".library_updated";
	public static final String ACTION_LIBRARY_UPDATE_CANCEL = TAG + ".library_update_cancel";
	public static final String ACTION_REFRESH_SYSTEM_BINDINGS = TAG + ".refresh_system_bindings";
	public static final String ACTION_REFRESH_SFX = TAG + ".sfx";
	public static final String ACTION_SFX_UPDATED = TAG + ".sfx_updated";
	public static final String ACTION_PLAYLIST_CHANGED = TAG + ".playlist_changed";
	public static final String KEY_PLAYLIST_CHANGED_PLAYLIST = "playlist";
	public static final String ACTION_DOWNLOADER_CANCEL = TAG + ".downloader_cancel";
	public static final String DOWNLOADER_CANCEL_ID = "id";
	public static final String PREF_DOWNLOAD_LOCATION = "download_location";
	public static final String TAG_MUSIC_SERVICE_AUTO_DOWNLOAD_ENABLED = "ms_auto_download";
	public static final String TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED = SPrefEx.TAG_SPREF + ".player_repeat_music_enabled";
	public static final String TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED = SPrefEx.TAG_SPREF + ".player_shuffle_music_enabled";
	public static final String TAG_SPREF_PLAYER_TYPE = SPrefEx.TAG_SPREF + ".player_type";
	public static final String TAG_SPREF_PLAYER_EQ = SPrefEx.TAG_SPREF + ".eq";

	public static boolean IsPremium = true;
	public static final String SKU_PREMIUM = "premium";
	public static final String TAG_SPREF_SKU_PREMIUM = SPrefEx.TAG_SPREF + ".sku_premium";
	public static final String LICENSE_BASE64_PUBLIC_KEY = "";

	public static String[] ExportableSPrefKeys = new String[]{};

	private final ServiceBinder binder = new ServiceBinder();
	private ExoPlayer player;
	private MediaSession mediaSession;
	private PlaybackVisualizer playbackVisualizer;
	private Playlist playlist;
	private Music current;
	private boolean prepared;
	private AudioManager audioManager;
	private AudioFocusRequest audioFocusRequest;
	private MusicServiceLibraryUpdaterAsyncTask libraryUpdater;
	private Equalizer equalizer;
	private BassBoost bassBoost;
	private Virtualizer virtualizer;
	private LoudnessEnhancer loudnessEnhancer;

	private final BroadcastReceiver becomingNoisy = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				pause();
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		createNotificationChannel();
		LibraryStore.init(this);
		playlist = Playlist.loadOrCreatePlaylist(Playlist.getActivePlaylist(this));

		playbackVisualizer = new PlaybackVisualizer();
		player = new ExoPlayer.Builder(this).build();
		player.setAudioAttributes(
				new androidx.media3.common.AudioAttributes.Builder()
						.setUsage(C.USAGE_MEDIA)
						.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
						.build(),
				true);
		player.addListener(new Player.Listener() {
			@Override
			public void onPlaybackStateChanged(int playbackState) {
				if (playbackState == Player.STATE_READY) {
					prepared = true;
					broadcast(ACTION_PREPARED);
					attachEffects();
				} else if (playbackState == Player.STATE_ENDED) {
					nextSmart(true);
				}
			}

			@Override
			public void onPlayerError(androidx.media3.common.PlaybackException error) {
				Log.e(TAG, "playback failed", error);
				prepared = false;
				broadcast(ACTION_PREPARED);
			}

			@Override
			public void onIsPlayingChanged(boolean isPlaying) {
				if (isPlaying) {
					playbackVisualizer.start();
					broadcast(ACTION_PLAY);
				} else {
					broadcast(ACTION_PAUSE);
				}
			}
		});

		mediaSession = new MediaSession.Builder(this, player).setId("harmony").build();

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		registerReceiver(becomingNoisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), RECEIVER_NOT_EXPORTED);
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(becomingNoisy);
		} catch (Exception ignored) {
		}
		releaseEffects();
		playbackVisualizer.stop();
		if (mediaSession != null) {
			mediaSession.release();
			mediaSession = null;
		}
		if (player != null) {
			player.release();
			player = null;
		}
		super.onDestroy();
	}

	@Nullable
	@Override
	public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
		return mediaSession;
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent != null && MediaSessionService.SERVICE_INTERFACE.equals(intent.getAction())) {
			return super.onBind(intent);
		}
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction() != null) {
			handleIntent(intent);
		}
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	public class ServiceBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}

	public PlaybackVisualizer getPlaybackVisualizer() {
		return playbackVisualizer;
	}

	public PlaybackVisualizer getVisualizer() {
		return playbackVisualizer;
	}

	public PlaybackVisualizer getVisualizerHQ() {
		return playbackVisualizer;
	}

	public Playlist getPlaylist() {
		if (playlist == null) {
			playlist = Playlist.loadOrCreatePlaylist(Playlist.getActivePlaylist(this));
		}
		return playlist;
	}

	public void setPlaylist(String name) {
		playlist = Playlist.loadOrCreatePlaylist(name);
		broadcast(ACTION_PLAYLIST_CHANGED);
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;
		if (playlist != null) {
			Playlist.savePlaylist(playlist);
		}
		broadcast(ACTION_PLAYLIST_CHANGED);
	}

	public Music getMusic() {
		return current;
	}

	public void refreshMusic() {
		if (current != null) {
			current.refresh(this);
		}
	}

	public int getAudioSessionId() {
		return player == null ? 0 : player.getAudioSessionId();
	}

	public Object getMediaPlayer() {
		return player;
	}

	public boolean canPlay() {
		return current != null && player != null;
	}

	public boolean isPrepared() {
		return prepared;
	}

	public boolean isPlaying() {
		return player != null && player.isPlaying();
	}

	public int getPosition() {
		return player == null ? 0 : (int) player.getCurrentPosition();
	}

	public int getDuration() {
		if (player == null) {
			return current == null ? 0 : Math.max(0, current.getLength());
		}
		long d = player.getDuration();
		return d == C.TIME_UNSET ? 0 : (int) d;
	}

	public void seek(int position) {
		if (player != null) {
			player.seekTo(position);
		}
	}

	public void play() {
		if (player == null) {
			return;
		}
		if (current == null && getPlaylist().getItem() != null) {
			open(getPlaylist().getItem());
		}
		if (requestFocus()) {
			player.play();
		}
	}

	public void pause() {
		if (player != null) {
			player.pause();
		}
	}

	public void stop() {
		if (player != null) {
			player.stop();
			player.clearMediaItems();
		}
		prepared = false;
		playbackVisualizer.stop();
		broadcast(ACTION_STOP);
	}

	public void skip(final int position, boolean autoPlay) {
		Playlist p = getPlaylist();
		if (p.getItems().isEmpty()) {
			return;
		}
		p.setItemIndex(position);
		Playlist.savePlaylist(p);
		Music item = p.getItem();
		if (item != null) {
			open(item);
			if (autoPlay) {
				play();
			}
		}
	}

	public void skip(int position) {
		skip(position, true);
	}

	public void next(boolean autoPlay) {
		Playlist p = getPlaylist();
		skip(p.getItemIndex() + 1, autoPlay);
	}

	public void next() {
		next(true);
	}

	public void prev(boolean autoPlay) {
		Playlist p = getPlaylist();
		skip(p.getItemIndex() - 1, autoPlay);
	}

	public void prev() {
		prev(true);
	}

	public void random(boolean autoPlay) {
		Playlist p = getPlaylist();
		if (p.getItems().isEmpty()) {
			return;
		}
		skip((int) (Math.random() * p.getItems().size()), autoPlay);
	}

	public void random() {
		random(true);
	}

	public void nextSmart(boolean forceNext) {
		if (!forceNext && getPlayerRepeatMusicEnabled(this)) {
			play();
			return;
		}
		if (getPlayerShuffleMusicEnabled(this)) {
			random(true);
		} else {
			next(true);
		}
	}

	public void open(final String musicId) {
		if (TextUtils.isEmpty(musicId)) {
			return;
		}
		Music item = null;
		for (Music music : getPlaylist().getItems()) {
			if (musicId.equalsIgnoreCase(music.getPath())) {
				item = music;
				break;
			}
		}
		if (item == null) {
			item = Music.load(this, musicId);
			if (item != null) {
				getPlaylist().add(item);
				Playlist.add(this, getPlaylist().getName(), item, false);
			}
		}
		if (item != null) {
			open(item);
		}
	}

	public void open(final Music music, final boolean insertOrUpdate) {
		if (insertOrUpdate && music != null) {
			LibraryStore.get().upsert(music);
		}
		open(music);
	}

	public void open(final Music music) {
		if (music == null || player == null) {
			return;
		}
		current = music;
		prepared = false;
		Uri playUri = Music.toPlaybackUri(music);
		if (playUri == null) {
			Log.w(TAG, "no playback uri for " + music.getPath());
			return;
		}
		try {
			player.setMediaItem(MediaItem.fromUri(playUri));
			player.prepare();
			player.play();
			Intent broadcastIntent = new Intent(ACTION_OPEN);
			broadcastIntent.putExtra(KEY_URI, music.getPath());
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	public void openOrDownload(final Music music) {
		open(music, true);
	}

	public void stream(final Music music, final boolean autoPlay) {
		open(music, false);
		if (autoPlay) play();
	}

	public void stream(final Music music) {
		stream(music, true);
	}

	public void download(final Music music, final boolean playAfterDownload) {
		open(music, playAfterDownload);
	}

	public void download(final Music music) {
		download(music, true);
	}

	public void cancelDownload(int id) {
	}

	public void cancelDownload(final Music music) {
	}

	public void yts(final String url) {
	}

	public MusicServiceLibraryUpdaterAsyncTask getLibraryUpdater() {
		return libraryUpdater;
	}

	public void smartTune() {
	}

	public void updateSFX() {
		broadcast(ACTION_SFX_UPDATED);
	}

	public Equalizer getEqualizer() {
		attachEffects();
		return equalizer;
	}

	public void setEqualizer(boolean enabled) {
		attachEffects();
		if (equalizer != null) {
			equalizer.setEnabled(enabled);
		}
	}

	public void saveEqualizer() {
	}

	public BassBoost getBassBoost() {
		attachEffects();
		return bassBoost;
	}

	public void setBassBoost(boolean enabled) {
		attachEffects();
		if (bassBoost != null) {
			bassBoost.setEnabled(enabled);
		}
	}

	public void saveBassBoost() {
	}

	public LoudnessEnhancer getLoudnessEnhancer() {
		attachEffects();
		return loudnessEnhancer;
	}

	public void setLoudnessEnhancer(boolean enabled) {
		attachEffects();
		if (loudnessEnhancer != null) {
			loudnessEnhancer.setEnabled(enabled);
		}
	}

	public void saveLoudnessEnhancer() {
	}

	public Virtualizer getVirtualizer() {
		attachEffects();
		return virtualizer;
	}

	public void setVirtualizer(boolean enabled) {
		attachEffects();
		if (virtualizer != null) {
			virtualizer.setEnabled(enabled);
		}
	}

	public void saveVirtualizer() {
	}

	public Object getPreAmp() {
		return null;
	}

	public void setPreAmp(boolean enabled) {
	}

	public void savePreAmp() {
	}

	public Object getEnvironmentalReverb() {
		return null;
	}

	public void setEnvironmentalReverb(boolean enabled) {
	}

	public void saveEnvironmentalReverb() {
	}

	public Object getPresetReverb() {
		return null;
	}

	public void setPresetReverb(boolean enabled) {
	}

	public void savePresetReverb() {
	}

	public static boolean getPlayerSmartTuneEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(SPrefEx.TAG_SPREF + ".smart_tune", true);
	}

	public static void setPlayerSmartTuneEnabled(Context context, boolean value) {
		SPrefEx.get(context).edit().putBoolean(SPrefEx.TAG_SPREF + ".smart_tune", value).apply();
	}

	public static boolean getPlayerRepeatMusicEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED, false);
	}

	public static void setPlayerRepeatMusicEnabled(Context context, boolean value) {
		SPrefEx.get(context).edit().putBoolean(TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED, value).apply();
	}

	public static boolean getPlayerShuffleMusicEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED, false);
	}

	public static void setPlayerShuffleMusicEnabled(Context context, boolean value) {
		SPrefEx.get(context).edit().putBoolean(TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED, value).apply();
	}

	public enum PlayerType {
		AndroidOS("Android OS / Device Default"),
		AudioTrack("Audio Track (☢)"),
		OpenSL("Open SL (☢)");

		private final String friendlyName;

		PlayerType(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		public String getFriendlyName() {
			return friendlyName;
		}
	}

	public static PlayerType getPlayerType(Context context) {
		return PlayerType.AndroidOS;
	}

	public static void setPlayerType(Context context, PlayerType value) {
		SPrefEx.get(context).edit().putString(TAG_SPREF_PLAYER_TYPE, String.valueOf(value)).apply();
	}

	public static boolean getAutoDownloadEnabled(Context context) {
		return false;
	}

	public static void setAutoDownloadEnabled(Context context, boolean value) {
	}

	public static String getDefaultDownloadLocation(Context context) {
		return context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC).getAbsolutePath();
	}

	public static String getDownloadLocation(Context context) {
		return SPrefEx.get(context).getString(PREF_DOWNLOAD_LOCATION, getDefaultDownloadLocation(context));
	}

	public static void setDownloadLocation(Context context, String value) {
		SPrefEx.get(context).edit().putString(PREF_DOWNLOAD_LOCATION, value).apply();
	}

	public static void startIntentForOpen(final Context context, final String musicId) {
		Intent intent = new Intent(context.getApplicationContext(), MusicService.class);
		intent.setAction(ACTION_OPEN);
		intent.putExtra(KEY_URI, musicId);
		ContextCompat.startForegroundService(context.getApplicationContext(), intent);
	}

	public static void startIntentForOpenYTS(final Context context, final String url) {
		// YouTube extract removed.
	}

	public static boolean verifyDeveloperPayload(Context context, Object p) {
		return true;
	}

	public static String getDeveloperPayload(Context context, String sku) {
		return "";
	}

	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (ACTION_CLOSE.equals(action) || ACTION_STOP.equals(action)) {
			stop();
			stopSelf();
		} else if (ACTION_PREVIOUS.equals(action)) {
			prev();
		} else if (ACTION_NEXT.equals(action)) {
			next();
		} else if (ACTION_PLAY.equals(action)) {
			play();
		} else if (ACTION_PAUSE.equals(action)) {
			pause();
		} else if (ACTION_TOGGLE_PLAYBACK.equals(action)) {
			if (isPlaying()) pause();
			else play();
		} else if (ACTION_RANDOM.equals(action)) {
			random();
		} else if (ACTION_OPEN.equals(action)) {
			open(intent.getStringExtra(KEY_URI));
		} else if (ACTION_LIBRARY_UPDATE.equals(action)) {
			boolean force = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FORCE, false);
			boolean fastMode = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FASTMODE, LIBRARY_UPDATE_FASTMODE_DEFAULT);
			libraryUpdater = new MusicServiceLibraryUpdaterAsyncTask(this, force, fastMode);
			libraryUpdater.execute();
		} else if (ACTION_PLAYLIST_CHANGED.equals(action) || ACTION_LIBRARY_UPDATED.equals(action)) {
			String name = intent.getStringExtra(KEY_PLAYLIST_CHANGED_PLAYLIST);
			if (TextUtils.isEmpty(name)) {
				name = Playlist.getActivePlaylist(this);
			}
			if (!TextUtils.isEmpty(name)) {
				setPlaylist(name);
			}
		} else if (ACTION_LIBRARY_UPDATE_CANCEL.equals(action)) {
			if (libraryUpdater != null) {
				libraryUpdater.cancel(true);
			}
		} else if (ACTION_REFRESH_SFX.equals(action)) {
			updateSFX();
		}
	}

	private void broadcast(String action) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
	}

	private boolean requestFocus() {
		if (audioManager == null) {
			return true;
		}
		if (audioFocusRequest == null) {
			audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
					.setAudioAttributes(new AudioAttributes.Builder()
							.setUsage(AudioAttributes.USAGE_MEDIA)
							.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
							.build())
					.setOnAudioFocusChangeListener(focusChange -> {
						if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
							pause();
						}
					})
					.build();
		}
		return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}

	private void attachEffects() {
		int session = getAudioSessionId();
		if (session == 0) {
			return;
		}
		try {
			if (equalizer == null) {
				equalizer = new Equalizer(0, session);
			}
			if (bassBoost == null) {
				bassBoost = new BassBoost(0, session);
			}
			if (virtualizer == null) {
				virtualizer = new Virtualizer(0, session);
			}
			if (loudnessEnhancer == null) {
				loudnessEnhancer = new LoudnessEnhancer(session);
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void releaseEffects() {
		try {
			if (equalizer != null) equalizer.release();
			if (bassBoost != null) bassBoost.release();
			if (virtualizer != null) virtualizer.release();
			if (loudnessEnhancer != null) loudnessEnhancer.release();
		} catch (Exception ignored) {
		}
		equalizer = null;
		bassBoost = null;
		virtualizer = null;
		loudnessEnhancer = null;
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel music = new NotificationChannel("music", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
			music.setSound(null, null);
			NotificationChannel scan = new NotificationChannel("scan", "Library", NotificationManager.IMPORTANCE_LOW);
			scan.setSound(null, null);
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm != null) {
				nm.createNotificationChannel(music);
				nm.createNotificationChannel(scan);
			}
		}
	}
}
