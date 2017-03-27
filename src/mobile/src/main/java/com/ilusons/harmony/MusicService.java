package com.ilusons.harmony;

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
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;

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

    // Threads
    private Handler handler = new Handler();

    // Binder
    private final IBinder binder = new ServiceBinder();

    // Components
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;

    private ArrayList<String> playlist = new ArrayList<>(25);
    private int playlistPosition = -1;
    private Music currentMusic;

    private BroadcastReceiver intentReceiver;

    private PowerManager.WakeLock wakeLock;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // mMediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        // audioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);


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

        // Attach
        registerReceiver(intentReceiver, filter);

        // Initialize the wake lock
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        setUpMediaSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(intentReceiver);

        wakeLock.release();
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
        return mediaPlayer.getAudioSessionId();
    }

    public void add(String path, int position) {
        playlist.add(position, path);
    }

    public void add(String path) {
        playlist.add(path);
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

    public void next() {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
            else
                playlistPosition += 1;

            stop();
            prepare();
            play();
        }
    }

    public void prev() {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
            else
                playlistPosition -= 1;

            stop();
            prepare();
            play();
        }
    }

    public void random() {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
            else
                playlistPosition = (int) Math.round(Math.random() * playlist.size());

            stop();
            prepare();
            play();
        }
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public ArrayList<String> getPlaylist() {
        return playlist;
    }

    private void prepare() {
        // Fix playlist position
        if (!canPlay())
            return;

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
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d(TAG, "onError");
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

    public void release() {
        if (mediaPlayer == null) return;
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void start() {
        if (mediaPlayer == null) return;
        mediaPlayer.start();

        update();
    }

    public void stop() {
        if (mediaPlayer == null) return;
        mediaPlayer.stop();

        update();

        cancelNotification();
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

    public int getPosition() {
        if (mediaPlayer == null) return -1;
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (mediaPlayer == null) return -1;
        return mediaPlayer.getDuration();
    }

    public void play() {
        // Fix playlist position
        if (!canPlay())
            return;

        synchronized (this) {
            // int status = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            // Log.d(TAG, "Starting playback: audio focus request status = " + status);

            // if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            //    return;

            final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            // audioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName()));

            mediaSession.setActive(true);

            mediaPlayer.start();

            update();
        }
    }

    public void pause() {
        synchronized (this) {
            Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            mediaPlayer.pause();

            update();
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    private static final int NOTIFICATION_ID = 4524;

    private void updateNotification() {

        int bgColor = Color.parseColor("#9e9e9e");
        Bitmap cover = currentMusic.getCover(this);
        if (cover == null)
            cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        else
            bgColor = Palette.from(cover).generate().getVibrantColor(bgColor);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(cover)
                .setContentIntent(contentIntent)
                .setContentTitle(currentMusic.Title)
                .setContentText(currentMusic.getText())
                .setTicker(currentMusic.getText())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(0)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",
                        createActionIntent(this, ACTION_STOP))
                .addAction(android.R.drawable.ic_media_previous,
                        "Previous",
                        createActionIntent(this, ACTION_PREVIOUS))
                .addAction(isPlaying()
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        "Play / Pause",
                        createActionIntent(this, ACTION_TOGGLE_PLAYBACK))
                .addAction(android.R.drawable.ic_media_next,
                        "Next",
                        createActionIntent(this, ACTION_NEXT))
                .addAction(android.R.drawable.ic_media_ff,
                        "Random",
                        createActionIntent(this, ACTION_RANDOM))
                .setProgress(getDuration(), getPosition(), true);

        if (cover != null)
            builder.setColor(bgColor);

        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(2, 4, 0)
                .setCancelButtonIntent(createActionIntent(this, ACTION_STOP))
                .setShowCancelButton(true);
        builder.setStyle(style);

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
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
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
        updateNotification();

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
        }

    }

    public class ServiceBinder extends Binder {

        MusicService getService() {
            return MusicService.this;
        }

    }

    public static PendingIntent createActionIntent(MusicService service, String action) {
        Intent intent = new Intent(action);
        intent.setComponent(new ComponentName(service, MusicService.class));
        PendingIntent pendingIntent = PendingIntent.getService(service, 0, intent, 0);
        return pendingIntent;
    }

}
