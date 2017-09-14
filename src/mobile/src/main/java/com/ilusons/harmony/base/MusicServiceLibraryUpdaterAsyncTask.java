package com.ilusons.harmony.base;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.ArraySet;
import android.util.Log;

import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.SPrefEx;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

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
				try (Realm realm = Music.getDB()) {
					if (isCancelled())
						throw new Exception("Canceled by user");

					// Record time
					long time = System.currentTimeMillis();

					// Scan media store
					if (getScanMediaStoreEnabled(context)) {

						scanMediaStoreAudio(realm);
						scanMediaStoreVideo(realm);

						Log.d(TAG, "Library update from media store took " + (System.currentTimeMillis() - time) + "ms");
					}

					// Scan storage
					scanStorage(realm);

					Log.d(TAG, "Library update from storage took " + (System.currentTimeMillis() - time) + "ms");

					// Scan current
					scanCurrent(realm);

					Log.d(TAG, "Library update from current took " + (System.currentTimeMillis() - time) + "ms");

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

			return new Result();
		} finally {
			instance = null;
		}
	}

	private void addFromDirectory(Realm realm, Context context, File path, String playlist) throws Exception {
		if (isCancelled())
			return;

		for (File file : path.listFiles()) {
			if (isCancelled())
				throw new Exception("Canceled by user");

			if (file.canRead()) {
				if (file.isDirectory()) {
					addFromDirectory(realm, context, file, playlist);
				} else {
					add(realm, context, file.getAbsolutePath(), null, playlist);
				}
			}
		}
	}

	private void add(Realm realm, Context context, final String path, final Uri contentUri, final String playlist) {
		if (isCancelled())
			return;

		updateNotification(path, false);

		// Check if valid
		if (!Music.isValid(path))
			return;

		// Ignore if already present
		final Music oldData = Music.get(realm, path);
		if (oldData != null) {
			if (!oldData.isInPlaylist(playlist)) {
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(Realm realm) {
						oldData.addPlaylist(playlist);
						realm.copyToRealmOrUpdate(oldData);
					}
				});
			}

			return;
		}

		try {
			final Music newData = Music.decode(realm, context, path, contentUri, fastMode, null);

			if (newData == null)
				return;

			// Check constraints
			if (newData.Length > 0 && getScanConstraintMinDuration(context) > newData.Length) {
				return;
			}

			// Save
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					newData.addPlaylist(playlist);
					realm.copyToRealmOrUpdate(newData);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void scanMediaStoreAudio(Realm realm) throws Exception {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		final ArrayList<String> toRemove = new ArrayList<>();

		for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_MEDIASTORE)) {
			File file = (new File(music.Path));
			if (!file.exists())
				toRemove.add(music.Path);
		}

		if (toRemove.size() > 0)
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class).in("Path", toRemove.toArray(new String[toRemove.size()])).findAll().deleteAllFromRealm();
				}
			});

		ContentResolver cr = context.getContentResolver();

		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
		String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

		Cursor cursor = cr.query(uri, null, selection, null, sortOrder);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();

			if (count > 0) {
				while (cursor.moveToNext()) {
					if (isCancelled())
						throw new Exception("Canceled by user");

					String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

					if ((new File(path)).exists()) {
						Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID)));

						add(realm, context, path, contentUri, Music.KEY_PLAYLIST_MEDIASTORE);
					}
				}

			}
		}

		if (cursor != null)
			cursor.close();
	}

	private void scanMediaStoreVideo(Realm realm) throws Exception {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		final ArrayList<String> toRemove = new ArrayList<>();

		for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_MEDIASTORE)) {
			File file = (new File(music.Path));
			if (!file.exists())
				toRemove.add(music.Path);
		}

		if (toRemove.size() > 0)
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class).in("Path", toRemove.toArray(new String[toRemove.size()])).findAll().deleteAllFromRealm();
				}
			});

		ContentResolver cr = context.getContentResolver();

		Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		String sortOrder = MediaStore.Video.Media.TITLE + " ASC";

		Cursor cursor = cr.query(uri, null, null, null, sortOrder);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();

			if (count > 0) {
				while (cursor.moveToNext()) {
					if (isCancelled())
						throw new Exception("Canceled by user");

					String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));

					if ((new File(path)).exists()) {
						Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID)));

						add(realm, context, path, contentUri, Music.KEY_PLAYLIST_MEDIASTORE);
					}
				}

			}
		}

		if (cursor != null)
			cursor.close();
	}

	private void scanStorage(Realm realm) throws Exception {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		final Set<String> scanLocations = getScanLocations(context);

		final ArrayList<String> toRemove = new ArrayList<>();

		for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_STORAGE)) {
			if (isCancelled())
				throw new Exception("Canceled by user");

			File file = (new File(music.Path));

			if (!file.exists())
				toRemove.add(music.Path);

			boolean shouldRemove = true;
			for (String scanLocation : scanLocations)
				if (music.Path.contains(scanLocation)) {
					shouldRemove = false;
					break;
				}
			if (shouldRemove)
				toRemove.add(music.Path);

		}

		if (toRemove.size() > 0)
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class).in("Path", toRemove.toArray(new String[toRemove.size()])).findAll().deleteAllFromRealm();
				}
			});

		for (String location : scanLocations) {
			addFromDirectory(realm, context, new File(location), Music.KEY_PLAYLIST_STORAGE);
		}

	}

	private void scanCurrent(Realm realm) {
		if (isCancelled())
			return;

		Context context = contextRef.get();
		if (context == null)
			return;

		final ArrayList<String> toRemove = new ArrayList<>();

		for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_CURRENT)) {
			File file = (new File(music.Path));
			if (!file.exists())
				toRemove.add(music.Path);
		}

		if (toRemove.size() > 0)
			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class).in("Path", toRemove.toArray(new String[toRemove.size()])).findAll().deleteAllFromRealm();
				}
			});

		Music.saveCurrent(realm, context, Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_CURRENT), false);

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
				.setSmallIcon(R.drawable.ic_scan)
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
	public static final long LIBRARY_SCAN_AUTO_INTERVAL_DEFAULT = 18 * 60 * 60 * 1000;

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

}
