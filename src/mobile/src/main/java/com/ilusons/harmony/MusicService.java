package com.ilusons.harmony;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.ilusons.harmony.data.Music;

import java.io.File;
import java.util.ArrayList;

public class MusicService extends Service {

    // Logger TAG
    private static final String TAG = MusicService.class.getSimpleName();

    // Binder
    private final IBinder binder = new ServiceBinder();

    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;

    private ArrayList<String> playlist = new ArrayList<>(25);
    private int playlistPosition = -1;
    private Music currentMusic;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // mMediaButtonReceiverComponent = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        // audioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        setUpMediaSession();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand executed");
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
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
    }

    public void release() {
        if (mediaPlayer == null) return;
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void start() {
        if (mediaPlayer == null) return;
        mediaPlayer.start();

        updateNotification();
    }

    public void stop() {
        if (mediaPlayer == null) return;
        mediaPlayer.stop();
    }

    public void seek(int position) {
        if (mediaPlayer == null) return;
        if (position < 0) {
            position = 0;
        } else if (position > mediaPlayer.getDuration()) {
            position = mediaPlayer.getDuration();
        }
        mediaPlayer.seekTo(position);
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

            updateNotification();
        }
    }

    public void pause() {
        synchronized (this) {
            Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(intent);

            mediaPlayer.pause();

            updateNotification();
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    private static final int NOTIFICATION_ID = 4524;

    private Notification buildNotification() {
        final boolean isPlaying = mediaPlayer.isPlaying();

        int playButtonResId = isPlaying
                ? R.drawable.ic_pause_circle_outline_black_36dp : R.drawable.ic_play_circle_outline_black_36dp;


        Bitmap cover = null;
        try {
            cover = BitmapFactory.decodeFile(Music.getCover(this, currentMusic));
        } catch (Exception e) {
            Log.w(TAG, "bitmap decode", e);
        }
        if (cover == null)
            cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(cover)
                // TODO: .setContentIntent(clickIntent)
                .setContentTitle(currentMusic.Title)
                .setContentText(currentMusic.getText())
                /* TODO: .setWhen(0)
                .addAction(R.drawable.ic_skip_previous_white_36dp,
                        "",
                        retrievePlaybackAction(PREVIOUS_ACTION))
                .addAction(playButtonResId, "",
                        retrievePlaybackAction(TOGGLEPAUSE_ACTION))
                .addAction(R.drawable.ic_skip_next_white_36dp,
                        "",
                        retrievePlaybackAction(NEXT_ACTION))*/;

        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2, 3);
        builder.setStyle(style);

        if (cover != null)
            builder.setColor(Palette.from(cover).generate().getVibrantColor(Color.parseColor("#403f4d")));

        return builder.build();
    }

    private void updateNotification() {
        if (mediaPlayer.isPlaying()) {
            startForeground(NOTIFICATION_ID, buildNotification());
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void cancelNotification() {
        stopForeground(true);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    public class ServiceBinder extends Binder {

        MusicService getService() {
            return MusicService.this;
        }

    }

}
