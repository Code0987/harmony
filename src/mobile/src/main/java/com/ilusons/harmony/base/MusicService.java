package com.ilusons.harmony.base;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;

import java.io.File;
import java.util.ArrayList;

public class MusicService extends Service {

    // Logger TAG
    private static final String TAG = MusicService.class.getSimpleName();

    // Keys
    public static final String ACTION_PREVIOUS = TAG + ".previous";
    public static final String ACTION_NEXT = TAG + ".next";
    public static final String ACTION_PLAY = TAG + ".play";
    public static final String ACTION_PAUSE = TAG + ".pause";
    public static final String ACTION_STOP = TAG + ".stop";
    public static final String ACTION_TOGGLE_PLAYBACK = TAG + ".toggle_playback";
    public static final String ACTION_RANDOM = TAG + ".random";

    public static final String ACTION_OPEN = TAG + ".open";
    public static final String KEY_URI = "uri";

    public static final String ACTION_LIBRARY_UPDATED = TAG + ".library_updated";

    // Threads
    private Handler handler = new Handler();

    // Binder
    private final IBinder binder = new ServiceBinder();

    // Components
    private AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener audioManagerFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            Log.d(TAG, "onAudioFocusChange\n" + focusChange);

            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;

    private BroadcastReceiver intentReceiver;
    private ComponentName headsetMediaButtonIntentReceiverComponent;

    private PowerManager.WakeLock wakeLock;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        headsetMediaButtonIntentReceiverComponent = new ComponentName(getPackageName(), HeadsetMediaButtonIntentReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(headsetMediaButtonIntentReceiverComponent);

        // Intent handler
        intentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                handleIntent(intent);
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_OPEN);
        filter.addAction(ACTION_LIBRARY_UPDATED);

        // Attach
        registerReceiver(intentReceiver, filter);

        // Initialize the wake lock
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        setUpMediaSession();

        // Init loop
        final int dt = 350;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                updateNotification();

                handler.removeCallbacks(this);
                handler.postDelayed(this, dt);
            }
        }, dt);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(intentReceiver);

        wakeLock.release();

        audioManager.abandonAudioFocus(audioManagerFocusListener);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand\n" + intent + "\n" + "flags=" + flags + "\nstartId=" + startId);

        handleIntent(intent);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getAudioSessionId() {
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getAudioSessionId();
    }

    private Music currentMusic;
    private ArrayList<String> playlist = new ArrayList<>(25);
    private int playlistPosition = -1;

    public int add(String path, int position) {
        playlist.add(position, path);

        return position;
    }

    public int add(String path) {
        playlist.add(path);

        return playlist.size() - 1;
    }

    public void clear() {
        stop();

        playlist.clear();
    }

    public void remove(String path) {
        playlist.remove(path);
    }

    public void remove(int position) {
        playlist.remove(position);
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public ArrayList<String> getPlaylist() {
        return playlist;
    }

    public String getCurrentPlaylistItem() {
        if (playlistPosition < 0 || playlistPosition >= playlist.size())
            return null;
        return playlist.get(playlistPosition);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean canPlay() {
        if (playlistPosition < 0 || playlistPosition >= playlist.size())
            playlistPosition = 0;
        if (playlistPosition < playlist.size())
            return true;
        return false;
    }

    public boolean isPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            return true;
        return false;
    }

    public int getPosition() {
        if (mediaPlayer == null)
            return -1;
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (mediaPlayer == null)
            return -1;
        return mediaPlayer.getDuration();
    }

    public void seek(int position) {
        if (mediaPlayer == null) return;
        if (position < 0) {
            position = 0;
        } else if (position > mediaPlayer.getDuration()) {
            position = mediaPlayer.getDuration();
        }
        mediaPlayer.seekTo(position);

        update();
    }

    private void prepare() {
        // Fix playlist position
        if (!canPlay())
            return;

        synchronized (this) {
            String path = playlist.get(playlistPosition);

            // Decode file
            currentMusic = Music.decodeFromFile(this, new File(path));
            if (currentMusic == null)
                return;

            // Setup player
            if (mediaPlayer == null)
                mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.reset();
                mediaPlayer.setOnPreparedListener(null);
                mediaPlayer.setDataSource(path);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.w(TAG, "media init failed", e);
            }
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.d(TAG, "onCompletion");

                    random();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    Log.d(TAG, "onError");

                    random();

                    return false;
                }
            });

            // Update media session
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMusic.Title)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMusic.Artist)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentMusic.Album)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getPlaylistPosition() + 1)
                        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlaylist().size())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentMusic.getCover(this))
                        .build());
            }
        }

    }

    public boolean isPrepared() {
        if (currentMusic == null)
            return false;
        return true;
    }

    public void start() {
        if (mediaPlayer == null)
            return;
        mediaPlayer.start();

        update();
    }

    public void stop() {
        if (mediaPlayer == null)
            return;
        mediaPlayer.stop();

        update();

        cancelNotification();
    }

    public void play() {
        // TODO: Fix playlist position
        if (!canPlay())
            return;

        if (!isPrepared())
            prepare();

        synchronized (this) {
            int status = audioManager.requestAudioFocus(audioManagerFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            Log.d(TAG, "Starting playback: audio focus request status = " + status);

            if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                return;

            final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            audioManager.registerMediaButtonEventReceiver(headsetMediaButtonIntentReceiverComponent);

            mediaSession.setActive(true);

            mediaPlayer.start();

            update();

            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(new Intent(ACTION_PLAY));
        }
    }

    public void pause() {
        if (!isPlaying())
            return;

        synchronized (this) {
            Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            mediaPlayer.pause();

            update();

            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(new Intent(ACTION_PAUSE));
        }
    }

    public void skip(int position) {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            playlistPosition = position;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
        }

        stop();
        prepare();
        play();
    }

    public void next() {
        playlistPosition++;

        skip(playlistPosition);
    }

    public void prev() {
        playlistPosition--;

        skip(playlistPosition);
    }

    public void random() {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
            else
                playlistPosition = (int) Math.round(Math.random() * playlist.size());
        }

        stop();
        prepare();
        play();
    }

    private static final int NOTIFICATION_ID = 4524;

    private android.support.v4.app.NotificationCompat.Builder builder;

    private RemoteViews customNotificationView;

    private void setupNotification() {
        Bitmap cover = currentMusic.getCover(this);
        if (cover == null)
            cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        customNotificationView = new RemoteViews(getPackageName(), R.layout.notification_media_view);
        customNotificationView.setImageViewBitmap(R.id.cover, cover);
        customNotificationView.setTextViewText(R.id.title, currentMusic.Title);
        customNotificationView.setTextViewText(R.id.album, currentMusic.Album);
        customNotificationView.setTextViewText(R.id.artist, currentMusic.Artist);
        customNotificationView.setOnClickPendingIntent(R.id.prev, createActionIntent(this, ACTION_PREVIOUS));
        customNotificationView.setOnClickPendingIntent(R.id.next, createActionIntent(this, ACTION_NEXT));
        customNotificationView.setOnClickPendingIntent(R.id.play_pause, createActionIntent(this, ACTION_TOGGLE_PLAYBACK));
        customNotificationView.setOnClickPendingIntent(R.id.stop, createActionIntent(this, ACTION_STOP));
        customNotificationView.setOnClickPendingIntent(R.id.random, createActionIntent(this, ACTION_RANDOM));
        // TODO: Fix this crashing issue
//        customNotificationView.setImageViewResource(R.id.play_pause, isPlaying()
//                ? android.R.drawable.ic_media_pause
//                : android.R.drawable.ic_media_play);

        builder = new NotificationCompat.Builder(this)
                .setContentTitle(currentMusic.Title)
                .setContentText(currentMusic.Album)
                .setSubText(currentMusic.Artist)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(cover)
                .setColor(getApplicationContext().getColor(R.color.primary))
                .setTicker(currentMusic.getText())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                /*.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",
                        createActionIntent(this, ACTION_STOP))
                .addAction(isPlaying()
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        "Play / Pause",
                        createActionIntent(this, ACTION_TOGGLE_PLAYBACK))
                .addAction(android.R.drawable.ic_media_ff,
                        "Random",
                        createActionIntent(this, ACTION_RANDOM))*/
                .setCustomBigContentView(customNotificationView)
                /*.setStyle(new NotificationCompat.DecoratedMediaCustomViewStyle()
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(createActionIntent(this, ACTION_STOP))
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 0))*/;

        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
    }

    private void updateNotification() {
        if (builder == null)
            return;

        customNotificationView.setProgressBar(R.id.progress, getDuration(), getPosition(), false);

        Notification currentNotification = builder.build();

        if (isPlaying()) {
            startForeground(NOTIFICATION_ID, currentNotification);
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, currentNotification);
        }
    }

    private void cancelNotification() {
        stopForeground(true);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);

        builder = null;
    }

    private void setUpMediaSession() {
        mediaSession = new MediaSessionCompat(this, getString(R.string.app_name));
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onSeekTo(long pos) {
                seek((int) pos);
            }

            @Override
            public void onSkipToNext() {
                next();
            }

            @Override
            public void onSkipToPrevious() {
                prev();
            }

            @Override
            public void onStop() {
                stop();
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
    }

    private void updateMediaSession() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(isPlaying()
                                    ? PlaybackStateCompat.STATE_PLAYING
                                    : PlaybackStateCompat.STATE_PAUSED,
                            getPosition(),
                            1.0f)
                    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_STOP
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .build());
        }

    }

    private void update() {

        // Update media session
        updateMediaSession();

        // Update notification
        setupNotification();

    }

    private void handleIntent(Intent intent) {

        final String action = intent.getAction();

        if (action == null || TextUtils.isEmpty(action))
            return;

        if (action.equals(ACTION_PREVIOUS))
            prev();
        else if (action.equals(ACTION_NEXT))
            next();
        else if (action.equals(ACTION_PLAY))
            play();
        else if (action.equals(ACTION_PAUSE))
            pause();
        else if (action.equals(ACTION_STOP))
            stop();
        else if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
            if (isPlaying())
                pause();
            else
                play();
        } else if (action.equals(ACTION_RANDOM)) {
            random();
        } else if (action.equals(ACTION_OPEN)) {
            String file = intent.getStringExtra(KEY_URI);

            if (TextUtils.isEmpty(file))
                return;

            skip(add(file));

            Intent broadcastIntent = new Intent(ACTION_OPEN);
            broadcastIntent.putExtra(KEY_URI, file);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(broadcastIntent);

        } else if (action.equals(ACTION_LIBRARY_UPDATED)) {
            if (!isPlaying()) {
                stop();
                getPlaylist().clear();
            }
            for (Music music : Music.load(this))
                getPlaylist().add(music.Path);
        }

    }

    public class ServiceBinder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }

    }

    private static PendingIntent createIntent(MusicService service, Intent intent) {
        intent.setComponent(new ComponentName(service, MusicService.class));
        PendingIntent pendingIntent = PendingIntent.getService(service, 0, intent, 0);
        return pendingIntent;
    }

    private static PendingIntent createActionIntent(MusicService service, String action) {
        PendingIntent pendingIntent = createIntent(service, new Intent(action));
        return pendingIntent;
    }

}
