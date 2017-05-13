package com.ilusons.harmony.base;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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

    private static final String TAG_SPREF_SCAN_LAST_TS = SPrefEx.TAG_SPREF + ".scan_last_ts";
    private static final String TAG_SPREF_SCAN_LAST_TS_STORAGE = SPrefEx.TAG_SPREF + ".scan_last_ts_storage";
    // TODO: Create settings ui for this
    private static final long SCAN_INTERVAL = 3 * 60 * 60 * 1000;
    private static final long SCAN_INTERVAL_STORAGE = 12 * 60 * 60 * 1000;

    // For single task per session
    @SuppressLint("StaticFieldLeak")
    private static MusicServiceLibraryUpdaterAsyncTask instance;

    private Context context;
    private boolean force;

    public MusicServiceLibraryUpdaterAsyncTask(Context c, boolean force) {
        context = c;
        this.force = force;
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
                    long interval = SCAN_INTERVAL;
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
                    add(file, data);
                }
            }
        }
    }

    private void add(final File path, final ArrayList<Music> data) {

        updateNotification(path.getName());

        if (isCancelled())
            return;

        // Ignore if already present
        for (Music item : data) {
            if ((item).Path.equals(path.getAbsolutePath()))
                return;
        }

        try {
            Music m = Music.decodeFromFile(context, path);

            data.add(m);

            // HACK: Calling the devil
            System.gc();
            Runtime.getRuntime().gc();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMediaStore(final ArrayList<Music> data) {
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;
        if (cur != null) {
            count = cur.getCount();

            if (count > 0) {
                while (cur.moveToNext()) {
                    String path = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));

                    File file = new File(path);

                    if (file.exists())
                        add(file, data);
                }

            }
        }

        if (cur != null)
            cur.close();
    }

    private void scanStorage(final ArrayList<Music> data) {
        if (!force) {
            long interval = SCAN_INTERVAL_STORAGE;
            long last = SPrefEx.get(context).getLong(TAG_SPREF_SCAN_LAST_TS_STORAGE, 0);

            long dt = System.currentTimeMillis() - (last + interval);

            if (dt < 0) {
                Log.d(TAG, "Storage scan skipped due to time constraints!");

                return;
            }
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
