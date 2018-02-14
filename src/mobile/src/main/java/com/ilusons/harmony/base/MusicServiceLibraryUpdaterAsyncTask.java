package com.ilusons.harmony.base;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.realm.Realm;

public class MusicServiceLibraryUpdaterAsyncTask extends AsyncTask<Void, Boolean, MusicServiceLibraryUpdaterAsyncTask.Result> {

	// Logger TAG
	public static final String TAG = MusicServiceLibraryUpdaterAsyncTask.class.getSimpleName();

	private static final String TAG_SPREF_SCAN_LAST_TS = SPrefEx.TAG_SPREF + ".scan_last_ts";

	// For single task per session
	@SuppressLint("StaticFieldLeak")
	private static MusicServiceLibraryUpdaterAsyncTask instance;

	private WeakReference<Context> contextRef;
	private boolean force;
	private boolean fastMode;

	public MusicServiceLibraryUpdaterAsyncTask(Context c, boolean force, boolean fastMode) {
		contextRef = new WeakReference<>(c);
		this.force = force;
		this.fastMode = fastMode;
	}

	protected Result doInBackground(Void... params) {
		try {
			synchronized (this) {
				Context context = contextRef.get();
				if (context == null)
					throw new Exception("Context lost!");

				// To keep single instance active only
				if (instance != null)
					wait();
				instance = this;

				// Check if really scan is needed
				if (!force) {
					long interval = getScanAutoInterval(context);
					long last = SPrefEx.get(context).getLong(TAG_SPREF_SCAN_LAST_TS, 0);

					long dt = ((last + interval) - System.currentTimeMillis());

					if (dt > 0) {
						throw new Exception("Skipped due to time constraints!");
					}
				}

				// Return
				Result result = new Result();

				// Notification
				setupNotification();

				// Process
				try {
					if (isCancelled())
						throw new Exception("Canceled by user");

					// Record time
					long time = System.currentTimeMillis();

					// Scan media store
					if (getScanMediaStoreEnabled(context)) {
						// Looper needed
						if (Looper.myLooper() == null) try {
							Looper.prepare(); // HACK
						} catch (Exception e) {
							Log.w(TAG, e);
						}

						scanMediaStoreAudio();

						updateNotification("Media store audio scan completed.", true);

						scanMediaStoreVideo();

						updateNotification("Media store video scan completed.", true);

						Log.d(TAG, "Library update from media store took " + (System.currentTimeMillis() - time) + "ms");
					}

					// Scan storage
					scanStorage();

					updateNotification("Storage scan completed.", true);

					Log.d(TAG, "Library update from storage took " + (System.currentTimeMillis() - time) + "ms");

					// Scan all
					scanAll();

					// Scan online
					scanOnline();

					updateNotification("Final scan completed.", true);

					Log.d(TAG, "Library update from all took " + (System.currentTimeMillis() - time) + "ms");

					Log.d(TAG, "Library update took " + (System.currentTimeMillis() - time) + "ms");

				} catch (Exception e) {
					Log.w(TAG, "doInBackground", e);
				}

				SPrefEx.get(context)
						.edit()
						.putLong(TAG_SPREF_SCAN_LAST_TS, System.currentTimeMillis())
						.apply();

				cancelNotification();

				// To keep single instance active only
				notifyAll();

				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();

			instance = null;
		} finally {
			cancelNotification();

			instance = null;
		}

		return new Result();
	}

	private void scanMediaStoreAudio() throws Exception {
		if (isCancelled())
			return;

		final Context context = contextRef.get();
		if (context == null)
			return;

		final Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_MEDIASTORE);

		Playlist.getAllMediaStoreEntriesForAudio(context)
				.flatMap(new Function<Collection<Pair<String, Uri>>, ObservableSource<Playlist>>() {
					@Override
					public ObservableSource<Playlist> apply(Collection<Pair<String, Uri>> newScanItems) throws Exception {
						if (isCancelled())
							throw new Exception("Canceled by user");

						return Playlist.update(
								context,
								playlist,
								newScanItems,
								false,
								fastMode,
								new JavaEx.ActionExT<String>() {
									@Override
									public void execute(String s) throws Exception {
										if (isCancelled())
											throw new Exception("Canceled by user");

										updateNotification(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
									}
								});
					}
				})
				.subscribe(new Consumer<Playlist>() {
					@Override
					public void accept(Playlist playlist) throws Exception {

					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {

					}
				});

		Playlist.savePlaylist(playlist);
	}

	private void scanMediaStoreVideo() throws Exception {
		if (isCancelled())
			return;

		final Context context = contextRef.get();
		if (context == null)
			return;

		final Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_MEDIASTORE);

		Playlist.getAllMediaStoreEntriesForVideo(context)
				.flatMap(new Function<Collection<Pair<String, Uri>>, ObservableSource<Playlist>>() {
					@Override
					public ObservableSource<Playlist> apply(Collection<Pair<String, Uri>> newScanItems) throws Exception {
						if (isCancelled())
							throw new Exception("Canceled by user");

						return Playlist.update(
								context,
								playlist,
								newScanItems,
								false,
								fastMode,
								new JavaEx.ActionExT<String>() {
									@Override
									public void execute(String s) throws Exception {
										if (isCancelled())
											throw new Exception("Canceled by user");

										updateNotification(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
									}
								});
					}
				})
				.subscribe(new Consumer<Playlist>() {
					@Override
					public void accept(Playlist playlist) throws Exception {

					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {

					}
				});

		Playlist.savePlaylist(playlist);
	}

	private void scanStorage() throws Exception {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		final Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_STORAGE);

		Playlist.updateForLocations(
				context,
				playlist,
				getScanLocations(context),
				false,
				fastMode,
				new JavaEx.ActionExT<String>() {
					@Override
					public void execute(String s) throws Exception {
						if (isCancelled())
							throw new Exception("Canceled by user");

						updateNotification(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
					}
				})
				.subscribe(new Consumer<Playlist>() {
					@Override
					public void accept(Playlist playlist) throws Exception {

					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {

					}
				});

		// Save
		Playlist.savePlaylist(playlist);
	}

	private void scanAll() {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		updateNotification("Updating [" + Playlist.KEY_PLAYLIST_ALL + "] ...", true);

		final Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_ALL);

		try (Realm realm = Music.getDB()) {
			if (realm == null)
				return;
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(@NonNull Realm realm) {
					playlist.clear();
					playlist.addAll(realm.where(Music.class).findAll());
					realm.insertOrUpdate(playlist);
				}
			});

			Playlist.update(
					context,
					playlist,
					true,
					new JavaEx.ActionExT<String>() {
						@Override
						public void execute(String s) throws Exception {
							if (isCancelled())
								throw new Exception("Canceled by user");

							updateNotification(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
						}
					})
					.subscribe(new Consumer<Playlist>() {
						@Override
						public void accept(Playlist playlist) throws Exception {

						}
					}, new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {

						}
					});
		}

		Playlist.savePlaylist(playlist);
	}

	private void scanOnline() {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		updateNotification("Updating [" + Playlist.KEY_PLAYLIST_ONLINE + "] ...", true);

		final Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_ONLINE);

		try (Realm realm = Music.getDB()) {
			if (realm == null)
				return;
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(@NonNull Realm realm) {
					playlist.clear();
					for (Music item : realm.where(Music.class).findAll())
						if (!item.isLocal())
							playlist.add(item);
					realm.insertOrUpdate(playlist);
				}
			});

			Playlist.update(
					context,
					playlist,
					true,
					new JavaEx.ActionExT<String>() {
						@Override
						public void execute(String s) throws Exception {
							if (isCancelled())
								throw new Exception("Canceled by user");

							updateNotification(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
						}
					})
					.subscribe(new Consumer<Playlist>() {
						@Override
						public void accept(Playlist playlist) throws Exception {

						}
					}, new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {

						}
					});
		}

		Playlist.savePlaylist(playlist);
	}

	NotificationManager notificationManager = null;
	NotificationCompat.Builder notificationBuilder = null;

	private static final int KEY_NOTIFICATION_ID = 1500;

	private void setupNotification() {
		Context context = contextRef.get();
		if (context == null)
			return;

		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent cancelIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATE_CANCEL);
		PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notificationBuilder = new NotificationCompat.Builder(context)
				.setOngoing(true)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentText("Updating ...")
				.setProgress(100, 0, true)
				.setSmallIcon(R.drawable.ic_arrows_clockwise_dashed)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent);

		notificationManager.notify(KEY_NOTIFICATION_ID, notificationBuilder.build());
	}

	private long lastNotificationUpdateTimestamp = 0;

	private void updateNotification(String msg, boolean force) {
		if (!force && (System.currentTimeMillis() - lastNotificationUpdateTimestamp) < 1000)
			return;
		lastNotificationUpdateTimestamp = System.currentTimeMillis();

		if (notificationManager == null || notificationBuilder == null)
			return;

		notificationBuilder
				.setContentText(msg);

		notificationManager.notify(KEY_NOTIFICATION_ID, notificationBuilder.build());
	}

	private void cancelNotification() {
		if (notificationManager == null || notificationBuilder == null)
			return;

		notificationManager.cancel(KEY_NOTIFICATION_ID);

		notificationBuilder = null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		Context context = contextRef.get();
		if (context == null)
			return;

		Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATE_BEGINS);
		LocalBroadcastManager
				.getInstance(context)
				.sendBroadcast(broadcastIntent);
	}

	@Override
	protected void onPostExecute(Result result) {
		super.onPostExecute(result);

		Context context = contextRef.get();
		if (context == null)
			return;

		if (result == null)
			return;

		Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATED);
		LocalBroadcastManager
				.getInstance(context)
				.sendBroadcast(broadcastIntent);

		Intent musicServiceIntent = new Intent(context, MusicService.class);
		musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATED);
		context.startService(musicServiceIntent);
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();

		Context context = contextRef.get();
		if (context == null)
			return;

		Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATED);
		LocalBroadcastManager
				.getInstance(context)
				.sendBroadcast(broadcastIntent);

		Intent musicServiceIntent = new Intent(context, MusicService.class);
		musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATED);
		context.startService(musicServiceIntent);
	}

	public static class Result {
	}

	//region Settings: Scan automatically

	public static final String TAG_SPREF_LIBRARY_SCAN_AUTO_ENABLED = SPrefEx.TAG_SPREF + ".library_scan_auto_enabled";
	private static final boolean LIBRARY_SCAN_AUTO_ENABLED_DEFAULT = true;

	public static boolean getScanAutoEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_LIBRARY_SCAN_AUTO_ENABLED, LIBRARY_SCAN_AUTO_ENABLED_DEFAULT);
	}

	public static void setScanAutoEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_LIBRARY_SCAN_AUTO_ENABLED, value)
				.apply();
	}

	public static final String TAG_SPREF_LIBRARY_SCAN_AUTO_INTERVAL = SPrefEx.TAG_SPREF + ".library_scan_auto_interval";
	public static final long LIBRARY_SCAN_AUTO_INTERVAL_DEFAULT = 36 * 60 * 60 * 1000;

	public static Long getScanAutoInterval(Context context) {
		return SPrefEx.get(context).getLong(TAG_SPREF_LIBRARY_SCAN_AUTO_INTERVAL, LIBRARY_SCAN_AUTO_INTERVAL_DEFAULT);
	}

	public static void setScanAutoInterval(Context context, Long value) {
		SPrefEx.get(context)
				.edit()
				.putLong(TAG_SPREF_LIBRARY_SCAN_AUTO_INTERVAL, value)
				.apply();
	}

	//endregion

	public static final String TAG_SPREF_LIBRARY_SCAN_LOCATIONS = SPrefEx.TAG_SPREF + ".library_scan_locations";

	public static Set<String> getScanLocations(Context context) {
		Set<String> value = new HashSet<>();
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			value = new ArraySet<>();
		}

		value = SPrefEx.get(context).getStringSet(TAG_SPREF_LIBRARY_SCAN_LOCATIONS, value);

		return value;
	}

	public static void setScanLocations(Context context, Set<String> value) {
		SPrefEx.get(context)
				.edit()
				.putStringSet(TAG_SPREF_LIBRARY_SCAN_LOCATIONS, value)
				.apply();
	}

	public static final String TAG_SPREF_LIBRARY_SCAN_MEDIASTORE_ENABLED = SPrefEx.TAG_SPREF + ".library_scan_mediastore_enabled";
	private static final boolean LIBRARY_SCAN_MEDIASTORE_ENABLED_DEFAULT = true;

	public static boolean getScanMediaStoreEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_LIBRARY_SCAN_MEDIASTORE_ENABLED, LIBRARY_SCAN_MEDIASTORE_ENABLED_DEFAULT);
	}

	public static void setScanMediaStoreEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_LIBRARY_SCAN_MEDIASTORE_ENABLED, value)
				.apply();
	}

	public static final String TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MIN_DURATION = SPrefEx.TAG_SPREF + ".library_scan_constraint_min_duration";
	private static final long LIBRARY_SCAN_CONSTRAINT_MIN_DURATION_DEFAULT = (long) (2.5 * 60 * 1000);

	public static Long getScanConstraintMinDuration(Context context) {
		return SPrefEx.get(context).getLong(TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MIN_DURATION, LIBRARY_SCAN_CONSTRAINT_MIN_DURATION_DEFAULT);
	}

	public static void setScanConstraintMinDuration(Context context, Long value) {
		SPrefEx.get(context)
				.edit()
				.putLong(TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MIN_DURATION, value)
				.apply();
	}


	public static final String TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MAX_DURATION = SPrefEx.TAG_SPREF + ".library_scan_constraint_max_duration";
	private static final long LIBRARY_SCAN_CONSTRAINT_MAX_DURATION_DEFAULT = (long) (15 * 60 * 1000);

	public static Long getScanConstraintMaxDuration(Context context) {
		return SPrefEx.get(context).getLong(TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MAX_DURATION, LIBRARY_SCAN_CONSTRAINT_MAX_DURATION_DEFAULT);
	}

	public static void setScanConstraintMaxDuration(Context context, Long value) {
		SPrefEx.get(context)
				.edit()
				.putLong(TAG_SPREF_LIBRARY_SCAN_CONSTRAINT_MAX_DURATION, value)
				.apply();
	}

}
