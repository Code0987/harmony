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
import java.util.HashSet;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

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
                try (Realm realm = Music.getDB()) {
                    if (isCancelled())
                        throw new Exception("Canceled by user");

                    ArrayList<Music> data = new ArrayList<>();

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

    private void addFromDirectory(Realm realm, File path, String playlist) {

        if (isCancelled())
            return;

        for (File file : path.listFiles()) {
            if (file.canRead()) {
                String filePath = file.getAbsolutePath();
                if (file.isDirectory()) {
                    addFromDirectory(realm, file, playlist);
                } else {
                    add(realm, file.getAbsolutePath(), playlist);
                }
            }
        }
    }

    private void add(Realm realm, final String path, final String playlist) {

        updateNotification(path);

        if (isCancelled())
            return;

        // Check if correct
        int index = path.lastIndexOf(".");
        if (index > 0) {
            String ext = path.substring(index);
            if (!(Music.isAudio(ext) || Music.isVideo(ext))) {
                return;
            }
        }

        // Ignore if already present
        final RealmResults<Music> result = realm.where(Music.class).equalTo("Path", path).findAll();
        if (result != null && result.size() > 0) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    result.get(0).addPlaylist(playlist);
                    realm.copyToRealmOrUpdate(result.get(0));
                }
            });

            return;
        }

        try {
            final Music m = Music.decode(realm, context, path, fastMode, null);

            if (m == null)
                return;

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    m.addPlaylist(playlist);
                    realm.copyToRealmOrUpdate(m);
                }
            });

            // Check constraints
            if (m.Length > 0 && getScanConstraintMinDuration(context) > m.Length) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.where(Music.class).equalTo("Path", m.Path).findAll().deleteAllFromRealm();
                    }
                });

                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanMediaStoreAudio(Realm realm) {
        if (isCancelled())
            return;

        final ArrayList<String> toRemove = new ArrayList<>();

        for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_MEDIASTORE)) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music.Path);
        }

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
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    if ((new File(path)).exists()) {
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID)));

                        add(realm, contentUri.toString(), Music.KEY_PLAYLIST_MEDIASTORE);
                    }
                }

            }
        }

        if (cursor != null)
            cursor.close();
    }

    private void scanMediaStoreVideo(Realm realm) {
        if (isCancelled())
            return;

        final ArrayList<String> toRemove = new ArrayList<>();

        for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_MEDIASTORE)) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music.Path);
        }

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
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));

                    if ((new File(path)).exists()) {
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID)));

                        add(realm, contentUri.toString(), Music.KEY_PLAYLIST_MEDIASTORE);
                    }
                }

            }
        }

        if (cursor != null)
            cursor.close();
    }

    private void scanStorage(Realm realm) {
        if (isCancelled())
            return;

        final Set<String> scanLocations = getScanLocations(context);

        final ArrayList<String> toRemove = new ArrayList<>();

        for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_STORAGE)) {

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

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Music.class).in("Path", toRemove.toArray(new String[toRemove.size()])).findAll().deleteAllFromRealm();
            }
        });

        for (String location : scanLocations) {
            addFromDirectory(realm, new File(location), Music.KEY_PLAYLIST_STORAGE);
        }

    }

    private void scanCurrent(Realm realm) {
        if (isCancelled())
            return;

        final ArrayList<String> toRemove = new ArrayList<>();

        for (Music music : Music.getAllInPlaylist(realm, Music.KEY_PLAYLIST_CURRENT)) {

            File file = (new File(music.Path));

            if (!file.exists())
                toRemove.add(music.Path);
        }

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
