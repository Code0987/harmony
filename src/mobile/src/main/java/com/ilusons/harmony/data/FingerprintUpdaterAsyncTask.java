package com.ilusons.harmony.data;

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
import android.util.Pair;
import android.widget.Toast;

import com.ilusons.harmony.R;
import com.ilusons.harmony.ref.SPrefEx;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class FingerprintUpdaterAsyncTask extends AsyncTask<Void, Boolean, Object> {

	// Logger TAG
	public static final String TAG = FingerprintUpdaterAsyncTask.class.getSimpleName();

	public static final String ACTION_UPDATE_START = TAG + "_start";
	public static final String ACTION_UPDATE_COMPLETED = TAG + "_completed";

	public static void broadcastAction(Context context, String action) {
		if (context == null)
			return;

		if (action == null)
			return;

		try {
			Intent broadcastIntent = new Intent(action);
			LocalBroadcastManager
					.getInstance(context)
					.sendBroadcast(broadcastIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("StaticFieldLeak")
	private static FingerprintUpdaterAsyncTask instance;

	public static FingerprintUpdaterAsyncTask getInstance() {
		return instance;
	}

	public static FingerprintUpdaterAsyncTask run(Context context) {
		if (instance != null)
			cancel();
		instance = new FingerprintUpdaterAsyncTask(context);
		instance.execute();
		return instance;
	}

	public static boolean cancel() {
		if (instance == null || instance.isCancelled())
			return true;

		if (instance != null) {
			try {
				Context context = instance.contextRef.get();

				instance.cancelNotification();
				instance.cancel(true);
				instance.get(1, TimeUnit.MILLISECONDS);

				try {
					broadcastAction(context, ACTION_UPDATE_COMPLETED);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				Log.w(TAG, e);
			} finally {
				instance = null;
			}
		}

		return true;
	}

	private WeakReference<Context> contextRef;

	public FingerprintUpdaterAsyncTask(Context c) {
		contextRef = new WeakReference<>(c);
	}

	protected Object doInBackground(Void... params) {
		try {
			broadcastAction(contextRef.get(), ACTION_UPDATE_START);

			synchronized (this) {
				Context context = contextRef.get();
				if (context == null)
					throw new Exception("Context lost!");

				// Notification
				setupNotification();

				// Process
				try {
					if (isCancelled())
						throw new Exception("Canceled by user");

					// Record time
					long time = System.currentTimeMillis();

					// Update
					{
						updateNotification("Preparing to fingerprint ...", true);

						if (isCancelled())
							throw new Exception("Canceled by user");

						Map<String, Pair<String, Long>> musicData = new HashMap<>();
						try (Realm realm = DB.getDB()) {
							if (realm != null) {
								for (Music item : realm.where(Music.class).findAll()) {
									musicData.put(item.getId(), Pair.create(item.getPath(), (long) item.getLength()));
								}
							}
						}

						if (isCancelled())
							throw new Exception("Canceled by user");

						int t = musicData.size();
						int k = 1;

						try (Realm realm = Fingerprint.getDB()) {
							for (Map.Entry<String, Pair<String, Long>> item : musicData.entrySet()) {
								String path = item.getValue().first;
								Long length = item.getValue().second;

								updateNotification("[" + k++ + "/" + t + "] " + (new File(path)).getName(), false);

								Fingerprint.indexIfNot(realm, item.getKey(), path, length);

								if (isCancelled())
									throw new Exception("Canceled by user");
							}
						}

					}

					updateNotification("Indexing completed.", true);

					Log.d(TAG, "Update took " + (System.currentTimeMillis() - time) + "ms");

				} catch (Exception e) {
					Log.w(TAG, "doInBackground", e);
				}

				cancelNotification();

				notifyAll();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cancelNotification();

			broadcastAction(contextRef.get(), ACTION_UPDATE_COMPLETED);
		}

		return null;
	}

	NotificationManager notificationManager = null;
	NotificationCompat.Builder notificationBuilder = null;

	private static final int KEY_NOTIFICATION_ID = TAG.hashCode();

	private void setupNotification() {
		Context context = contextRef.get();
		if (context == null)
			return;

		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationBuilder = new NotificationCompat.Builder(context)
				.setOngoing(true)
				.setContentTitle(context.getString(R.string.app_name) + ": Fingerprint update")
				.setContentText("Updating fingerprints ...")
				.setProgress(100, 0, true)
				.setSmallIcon(R.mipmap.ic_launcher);

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

}
