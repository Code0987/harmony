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
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.SPrefEx;

import java.io.File;
import java.util.ArrayList;

public class MusicServiceLibraryUpdaterAsyncTask extends AsyncTask<Void, Boolean, MusicServiceLibraryUpdaterAsyncTask.Result> {

    // Logger TAG
    private static final String TAG = MusicServiceLibraryUpdaterAsyncTask.class.getSimpleName();

    public static final long SCAN_INTERVAL_FACTOR = 60 * 60 * 1000;

    private static final String TAG_SPREF_SCAN_LAST_TS = SPrefEx.TAG_SPREF + ".scan_last_ts";

    private static final String TAG_SPREF_SCAN_LAST_TS_STORAGE = SPrefEx.TAG_SPREF + ".scan_last_ts_storage";

    public static final String TAG_SPREF_SCAN_INTERVAL = SPrefEx.TAG_SPREF + ".scan_interval";
    public static final long SCAN_INTERVAL_DEFAULT = 3 * 60 * 60 * 1000;

    private static final long SCAN_INTERVAL_STORAGE_DEFAULT = 12 * 60 * 60 * 1000;


    // For single task per session
    @SuppressLint("StaticFieldLeak")
    private static MusicServiceLibraryUpdaterAsyncTask instance;

    private Context context;
    private boolean force;
    private boolean fastMode;

    public MusicServiceLibraryUpdaterAsyncTask(Context c, boolean force, boolean fastMode) {
        context = c;
        this.force = force;
        this.fastMode = fastMode;
    }

    protected Result doInBackground(Void... params) {
        try {
            synchronized (this) {

                // Check permissions
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                    return new Result();
                }

                // To keep single instance active only
                if (instance != null)
                    wait();
                instance = this;

                // Check if really scan is needed
                if (!force) {
                    long interval = SPrefEx.get(context).getLong(TAG_SPREF_SCAN_INTERVAL, SCAN_INTERVAL_DEFAULT);
                    long last = SPrefEx.get(context).getLong(TAG_SPREF_SCAN_LAST_TS, 0);

                    long dt = System.currentTimeMillis() - (last + interval);

                    if (dt < 0) {
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

                    Looper.prepare(); // HACK

                    ArrayList<Music> data = new ArrayList<>();

                    // Load previous data
                    data.addAll(Music.load(context));

                    // Update
                    update(data);

                    // Record time
                    long time = System.currentTimeMillis();

                    // Scan media store
                    scanMediaStore(data);

                    Log.d(TAG, "Library update from media store took " + (System.currentTimeMillis() - time) + "ms");

                    // Scan storage
                    scanStorage(data);

                    Log.d(TAG, "Library update from storage took " + (System.currentTimeMillis() - time) + "ms");

                    // Save new data
                    Music.save(context, data);

                    Log.d(TAG, "Library update took " + (System.currentTimeMillis() - time) + "ms");

                    result.Data.clear();
                    result.Data.addAll(data);
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

    private void addFromDirectory(File path, ArrayList<Music> data) {

        if (isCancelled())
            return;

        for (File file : path.listFiles()) {
            if (file.canRead()) {
                String filePath = file.getAbsolutePath();
                if (file.isDirectory()) {
                    addFromDirectory(file, data);
                } else if (filePath.endsWith(".mp3")) {
                    add(file.getAbsolutePath(), data);
                }
            }
        }
    }

    private void add(final String path, final ArrayList<Music> data) {

        updateNotification(path);

        if (isCancelled())
            return;

        // Ignore if already present
        for (Music item : data) {
            if ((item).Path.equals(path))
                return;
        }

        try {
            Music m = Music.decode(context, path, fastMode, null);

            data.add(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMediaStore(final ArrayList<Music> data) {
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
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    File file = new File(path);

                    // Ignore if already present
                    boolean ignore = false;
                    for (Music item : data) {
                        if ((item).Path.contains(path))
                            ignore = true;
                    }
                    if (ignore)
                        continue;

                    if (file.exists()) {
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID)));

                        add(contentUri.toString(), data);
                    }
                }

            }
        }

        if (cursor != null)
            cursor.close();
    }

    private void scanStorage(final ArrayList<Music> data) {
        {
            long interval = SCAN_INTERVAL_STORAGE_DEFAULT;
            long last = SPrefEx.get(context).getLong(TAG_SPREF_SCAN_LAST_TS_STORAGE, 0);

            long dt = System.currentTimeMillis() - (last + interval);

            if (dt < 0) {
                Log.d(TAG, "Storage scan skipped due to time constraints!");

                return;
            }

            SPrefEx.get(context)
                    .edit()
                    .putLong(TAG_SPREF_SCAN_LAST_TS_STORAGE, System.currentTimeMillis())
                    .apply();
        }

        String externalStorageState = Environment.getExternalStorageState();
        if ("mounted".equals(externalStorageState) || "mounted_ro".equals(externalStorageState)) {
            addFromDirectory(Environment.getExternalStorageDirectory(), data);
        } else {
            Log.d(TAG, "External/Internal storage is not available.");
        }

        for (String path : IOEx.getExternalStorageDirectories(context)) {
            addFromDirectory(new File(path), data);
        }
    }

    private void update(final ArrayList<Music> data) {

        if (isCancelled())
            return;

        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);
        }

        data.removeAll(toRemove);
    }

    NotificationManager notificationManager = null;
    NotificationCompat.Builder notificationBuilder = null;

    private static final int KEY_NOTIFICATION_ID = 1500;

    private void setupNotification() {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent cancelIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATE_CANCEL);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                .setOngoing(true)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Updating ...")
                .setProgress(100, 0, true)
                .setSmallIcon(R.drawable.ic_scan)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent);

        notificationManager.notify(KEY_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(String msg) {
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

        Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATE_BEGINS);
        LocalBroadcastManager
                .getInstance(context)
                .sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);

        if (result == null || result.Data == null)
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
        public ArrayList<Music> Data = new ArrayList<>();
    }
}
