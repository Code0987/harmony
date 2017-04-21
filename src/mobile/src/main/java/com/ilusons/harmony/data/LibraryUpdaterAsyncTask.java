package com.ilusons.harmony.data;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.IOEx;

import java.io.File;
import java.util.ArrayList;

public class LibraryUpdaterAsyncTask extends AsyncTask<Void, Boolean, LibraryUpdaterAsyncTask.Result> {

    // Logger TAG
    private static final String TAG = LibraryUpdaterAsyncTask.class.getSimpleName();

    private static final int KEY_NOTIFICATION_ID = 1500;

    private static final int MIN_SCAN_INTERVAL = 5 * 60 * 1000;

    private static LibraryUpdaterAsyncTask instance;

    private Context context;

    public LibraryUpdaterAsyncTask(Context c) {
        context = c;
    }

    protected Result doInBackground(Void... params) {
        try {
            synchronized (this) {

                // To keep single instance active only
                if (instance != null)
                    wait();
                instance = this;

                // Check permissions
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                    return new Result();
                }

                // Record time
                long time = System.currentTimeMillis();

                // Return
                Result result = new Result();

                // Notification
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                        .setOngoing(true)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText("Scanning for audio and updating database ...")
                        .setProgress(100, 0, true)
                        .setSmallIcon(R.drawable.ic_scan);
                notificationManager.notify(KEY_NOTIFICATION_ID, notificationBuilder.build());

                // Process
                try {
                    ArrayList<Music> data = new ArrayList<>();

                    // Load previous data
                    data.addAll(Music.load(context));

                    // Update
                    update(data);

                    // Scan all storage
                    String externalStorageState = Environment.getExternalStorageState();
                    if ("mounted".equals(externalStorageState) || "mounted_ro".equals(externalStorageState)) {
                        addFromDirectory(Environment.getExternalStorageDirectory(), data);
                    } else {
                        Log.d(TAG, "External/Internal storage is not available.");
                    }

                    for (String path : IOEx.getExternalStorageDirectories(context)) {
                        addFromDirectory(new File(path), data);
                    }

                    // Save new data
                    Music.save(context, data);

                    result.Data.clear();
                    result.Data.addAll(data);
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground", e);
                }

                Log.d(TAG, "Library update from filesystem took " + (System.currentTimeMillis() - time) + "ms");

                notificationManager.cancel(KEY_NOTIFICATION_ID);

                // To keep single instance active only
                notifyAll();
                instance = null;

                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();

            return new Result();
        }
    }

    private void addFromDirectory(File path, ArrayList<Music> data) {
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
        // Ignore if already present
        for (Music item : data) {
            if ((item).Path.equals(path.getAbsolutePath()))
                return;
        }

        try {
            Music m = Music.decodeFromFile(context, path);

            // TODO: make it user editable
            if (m.Tags.getLength() > 2 * 60 * 1000)
                data.add(m);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void update(final ArrayList<Music> data) {
        ArrayList<Music> toRemove = new ArrayList<>();

        for (Music music : data) {
            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music);
        }

        data.removeAll(toRemove);
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
