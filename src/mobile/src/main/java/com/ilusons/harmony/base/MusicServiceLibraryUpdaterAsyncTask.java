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
import java.util.ArrayList;
import java.util.Set;

public class MusicServiceLibraryUpdaterAsyncTask extends AsyncTask<Void, Boolean, MusicServiceLibraryUpdaterAsyncTask.Result> {

    // Logger TAG
    private static final String TAG = MusicServiceLibraryUpdaterAsyncTask.class.getSimpleName();

    public static final long SCAN_INTERVAL_FACTOR = 60 * 60 * 1000;

    private static final String TAG_SPREF_SCAN_LAST_TS = SPrefEx.TAG_SPREF + ".scan_last_ts";

    public static final String TAG_SPREF_SCAN_INTERVAL = SPrefEx.TAG_SPREF + ".scan_interval";
    public static final long SCAN_INTERVAL_DEFAULT = 3 * 60 * 60 * 1000;

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

                    ArrayList<Music> data = new ArrayList<>();

                    // Record time
                    long time = System.currentTimeMillis();

                    final Set<String> scanLocations = getScanLocations(context);

                    // Scan media store
                    if (getScanMediaStoreEnabled(context)) {

                        // Load previous data
                        data.clear();
                        data.addAll(Music.loadIndex(context, Music.KEY_CACHE_LIBRARY_MEDIASTORE));
                        scanMediaStoreAudio(data, scanLocations);
                        scanMediaStoreVideo(data, scanLocations);
                        // Save new data
                        Music.saveIndex(context, data, Music.KEY_CACHE_LIBRARY_MEDIASTORE);

                        Log.d(TAG, "Library update from media store took " + (System.currentTimeMillis() - time) + "ms");
                    }

                    // Scan storage

                    // Load previous data
                    data.clear();
                    data.addAll(Music.loadIndex(context, Music.KEY_CACHE_LIBRARY_STORAGE));
                    scanStorage(data, scanLocations);
                    // Save new data
                    Music.saveIndex(context, data, Music.KEY_CACHE_LIBRARY_STORAGE);

                    Log.d(TAG, "Library update from storage took " + (System.currentTimeMillis() - time) + "ms");

                    // Scan current

                    // Load previous data
                    data.clear();
                    data.addAll(Music.loadCurrent(context));
                    scanCurrent(data, scanLocations);
                    // Save new data
                    Music.saveCurrent(context, data, false);

                    Log.d(TAG, "Library update from current took " + (System.currentTimeMillis() - time) + "ms");

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

    private void addFromDirectory(File path, ArrayList<Music> data, final Set<String> scanLocations) {

        if (isCancelled())
            return;

        for (File file : path.listFiles()) {
            if (file.canRead()) {
                String filePath = file.getAbsolutePath();
                if (file.isDirectory()) {
                    addFromDirectory(file, data, scanLocations);
                } else if (filePath.endsWith(".mp3") || filePath.endsWith(".m4a") || filePath.endsWith(".flac") || filePath.endsWith(".mp4")) {
                    add(file.getAbsolutePath(), data, scanLocations);
                }
            }
        }
    }

    private void add(final String path, final ArrayList<Music> data, Set<String> scanLocations) {

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

            // Check constraints
            if (getScanConstraintMinDuration(context) > m.Length)
                return;

            data.add(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMediaStoreAudio(final ArrayList<Music> data, final Set<String> scanLocations) {
        if (isCancelled())
            return;

        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);
        }

        data.removeAll(toRemove);

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

                    // Ignore if already present
                    boolean ignore = false;
                    for (Music item : data) {
                        if ((item).Path.contains(path))
                            ignore = true;
                    }
                    if (ignore)
                        continue;

                    if ((new File(path)).exists()) {
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID)));

                        add(contentUri.toString(), data, scanLocations);
                    }
                }

            }
        }

        if (cursor != null)
            cursor.close();
    }

    private void scanMediaStoreVideo(final ArrayList<Music> data, final Set<String> scanLocations) {
        if (isCancelled())
            return;

        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);
        }

        data.removeAll(toRemove);

        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Video.Media.TITLE + " ASC";

        Cursor cursor = cr.query(uri, null, null, null, sortOrder);
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();

            if (count > 0) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));

                    // Ignore if already present
                    boolean ignore = false;
                    for (Music item : data) {
                        if ((item).Path.contains(path))
                            ignore = true;
                    }
                    if (ignore)
                        continue;

                    if ((new File(path)).exists()) {
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID)));

                        add(contentUri.toString(), data, scanLocations);
                    }
                }

            }
        }

        if (cursor != null)
            cursor.close();
    }

    private void scanStorage(final ArrayList<Music> data, final Set<String> scanLocations) {
        if (isCancelled())
            return;

        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);

            boolean shouldRemove = true;
            for (String scanLocation : scanLocations)
                if (music.Path.contains(scanLocation)) {
                    shouldRemove = false;
                    break;
                }
            if (shouldRemove)
                toRemove.add(music);

        }

        data.removeAll(toRemove);

        for (String location : scanLocations) {
            addFromDirectory(new File(location), data, scanLocations);
        }

    }

    private void scanCurrent(final ArrayList<Music> data, final Set<String> scanLocations) {
        if (isCancelled())
            return;

        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);

            boolean shouldRemove = true;
            for (String scanLocation : scanLocations)
                if (music.Path.contains(scanLocation)) {
                    shouldRemove = false;
                    break;
                }
            if (shouldRemove)
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

    public static final String TAG_SPREF_LIBRARY_SCAN_LOCATIONS = SPrefEx.TAG_SPREF + ".library_scan_locations";

    public static Set<String> getScanLocations(Context context) {
        Set<String> value = new ArraySet<>();

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
