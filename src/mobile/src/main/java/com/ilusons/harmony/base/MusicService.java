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
import android.media.audiofx.AudioEffect;
import android.os.Binder;
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
import android.widget.Toast;

import com.h6ah4i.android.media.IBasicMediaPlayer;
import com.h6ah4i.android.media.IMediaPlayerFactory;
import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.h6ah4i.android.media.opensl.OpenSLMediaPlayerFactory;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

import java.util.ArrayList;

public class MusicService extends Service {

    // Logger TAG
    private static final String TAG = MusicService.class.getSimpleName();

    // Keys
    public static final String ACTION_CLOSE = TAG + ".close";
    public static final String ACTION_PREVIOUS = TAG + ".previous";
    public static final String ACTION_NEXT = TAG + ".next";
    public static final String ACTION_PLAY = TAG + ".play";
    public static final String ACTION_PAUSE = TAG + ".pause";
    public static final String ACTION_STOP = TAG + ".stop";
    public static final String ACTION_TOGGLE_PLAYBACK = TAG + ".toggle_playback";
    public static final String ACTION_RANDOM = TAG + ".random";

    public static final String ACTION_OPEN = TAG + ".open";
    public static final String KEY_URI = "uri";

    public static final String ACTION_LIBRARY_UPDATE = TAG + ".library_update";
    public static final String KEY_LIBRARY_UPDATE_FORCE = "force";
    public static final String KEY_LIBRARY_UPDATE_FASTMODE = "fast_mode";
    public static final String TAG_SPREF_LIBRARY_UPDATE_FASTMODE = SPrefEx.TAG_SPREF + ".library_update_fast_mode";
    public static final boolean LIBRARY_UPDATE_FASTMODE_DEFAULT = true;
    public static final String ACTION_LIBRARY_UPDATE_BEGINS = TAG + ".library_update_begins";
    public static final String ACTION_LIBRARY_UPDATED = TAG + ".library_updated";
    public static final String ACTION_LIBRARY_UPDATE_CANCEL = TAG + ".library_update_cancel";

    public static final String ACTION_REFRESH_SYSTEM_BINDINGS = TAG + ".refresh_system_bindings";

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
                    // TODO: Check this
                    // play();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    // TODO: Add opensl support, flac, m4a, mp3, scrobbler
    IMediaPlayerFactory mediaPlayerFactory;
    IBasicMediaPlayer mediaPlayer;
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
                Log.d(TAG, "onReceive\n" + intent);

                handleIntent(intent);
            }
        };

        // Attach
        registerReceiver(intentReceiver, getIntentFilter());

        // Initialize the wake lock
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        setUpMediaSession();

        // Init loop
        /* TODO: Review this, it's making whole android slow
        final int dt = 1500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                updateNotification();

                handler.removeCallbacks(this);
                handler.postDelayed(this, dt);
            }
        }, dt);
        */

        setupNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelNotification();

        unregisterReceiver(intentReceiver);

        wakeLock.release();

        audioManager.abandonAudioFocus(audioManagerFocusListener);

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }

        if (visualizerHQ != null) {
            visualizerHQ.release();
            visualizerHQ = null;
        }

        if (mediaPlayerFactory != null) {
            mediaPlayerFactory.release();
            mediaPlayerFactory = null;
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

    private IVisualizer visualizer;

    public IVisualizer getVisualizer() {
        if (visualizer == null) {
            try {
                visualizer = mediaPlayerFactory.createVisualizer(getAudioSessionId());
            } catch (UnsupportedOperationException e) {
                // the effect is not supported
            } catch (IllegalArgumentException e) {
            }
        }

        return visualizer;
    }

    private IHQVisualizer visualizerHQ;

    public IHQVisualizer getVisualizerHQ() {
        if (visualizerHQ == null) {
            try {
                visualizerHQ = mediaPlayerFactory.createHQVisualizer();
            } catch (UnsupportedOperationException e) {
                // the effect is not supported
            } catch (IllegalArgumentException e) {
            }
        }

        return visualizerHQ;
    }

    private MusicServiceLibraryUpdaterAsyncTask libraryUpdater = null;
    private Music currentMusic;
    private ArrayList<String> playlist = new ArrayList<>(25);
    private int playlistPosition = -1;

    public int add(String path, int position) {
        if (playlist.contains(path))
            return playlist.indexOf(path);

        playlist.add(position, path);

        return position;
    }

    public int add(String path) {
        if (playlist.contains(path))
            return playlist.indexOf(path);

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

    public IBasicMediaPlayer getMediaPlayer() {
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

    private void prepare(final JavaEx.Action onPrepare) {
        // Fix playlist position
        if (!canPlay())
            return;

        synchronized (this) {
            String path = playlist.get(playlistPosition);

            // Decode file
            currentMusic = Music.load(this, path);
            if (currentMusic == null)
                return;

            // Setup player
            if (mediaPlayer == null)
                mediaPlayerFactory = new OpenSLMediaPlayerFactory(getApplicationContext());
            mediaPlayer = mediaPlayerFactory.createMediaPlayer();
            try {
                mediaPlayer.reset();
                if (onPrepare != null)
                    mediaPlayer.setOnPreparedListener(new IBasicMediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(IBasicMediaPlayer mediaPlayer) {
                            onPrepare.execute();
                        }
                    });
                mediaPlayer.setDataSource(path);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.w(TAG, "media init failed", e);
            }
            mediaPlayer.setOnCompletionListener(new IBasicMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IBasicMediaPlayer mediaPlayer) {
                    Log.d(TAG, "onCompletion");

                    next();
                }
            });
            mediaPlayer.setOnErrorListener(new IBasicMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IBasicMediaPlayer mediaPlayer, int what, int extra) {
                    Log.w(TAG, "onError\nwhat = " + what + "\nextra = " + extra);

                    Toast.makeText(MusicService.this, "There was a problem while playing " + currentMusic.getText()+"!", Toast.LENGTH_LONG).show();

                    random();

                    return false;
                }
            });

            // Update media session
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

    public boolean isPrepared() {
        if (currentMusic == null || mediaPlayer == null)
            return false;
        return true;
    }

    public void stop() {
        if (mediaPlayer == null)
            return;

        Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(intent);

        mediaPlayer.stop();

        update();

        LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(new Intent(ACTION_STOP));

        cancelNotification();

        currentMusic = null;
    }

    public void play() {
        if (!canPlay())
            return;

        if (!isPrepared()) {
            prepare(new JavaEx.Action() {
                @Override
                public void execute() {
                    play();
                }
            });

            return;
        }

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
                    .getInstance(MusicService.this)
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

        prepare(new JavaEx.Action() {
            @Override
            public void execute() {
                play();
            }
        });
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

        prepare(new JavaEx.Action() {
            @Override
            public void execute() {
                play();
            }
        });
    }

    private static final int NOTIFICATION_ID = 4524;

    private android.support.v4.app.NotificationCompat.Builder builder;

    private RemoteViews customNotificationView;
    private RemoteViews customNotificationViewS;

    private void setupNotification() {
        Intent notificationIntent = MainActivity.getPlaybackUIActivityIntent(this);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (customNotificationView == null) {
            customNotificationView = new RemoteViews(getPackageName(), R.layout.notification_media_view);

            customNotificationView.setOnClickPendingIntent(R.id.prev, createActionIntent(this, ACTION_PREVIOUS));
            customNotificationView.setOnClickPendingIntent(R.id.next, createActionIntent(this, ACTION_NEXT));
            customNotificationView.setOnClickPendingIntent(R.id.play_pause, createActionIntent(this, ACTION_TOGGLE_PLAYBACK));
            customNotificationView.setOnClickPendingIntent(R.id.close, createActionIntent(this, ACTION_STOP));
            customNotificationView.setOnClickPendingIntent(R.id.random, createActionIntent(this, ACTION_RANDOM));
        }

        if (customNotificationViewS == null) {
            customNotificationViewS = new RemoteViews(getPackageName(), R.layout.notification_media_view_s);

            customNotificationViewS.setOnClickPendingIntent(R.id.play_pause, createActionIntent(this, ACTION_TOGGLE_PLAYBACK));
            customNotificationViewS.setOnClickPendingIntent(R.id.close, createActionIntent(this, ACTION_STOP));
            customNotificationViewS.setOnClickPendingIntent(R.id.random, createActionIntent(this, ACTION_RANDOM));
        }

        builder = new NotificationCompat.Builder(this)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(getApplicationContext().getColor(R.color.primary))
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
                .setCustomContentView(customNotificationViewS)
                .setCustomHeadsUpContentView(customNotificationViewS)
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

        Bitmap cover = currentMusic.getCover(this, 128);
        if (cover == null)
            cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        customNotificationView.setImageViewBitmap(R.id.cover, cover);
        customNotificationView.setTextViewText(R.id.title, currentMusic.Title);
        customNotificationView.setTextViewText(R.id.album, currentMusic.Album);
        customNotificationView.setTextViewText(R.id.artist, currentMusic.Artist);
        customNotificationView.setTextViewText(R.id.info, (getPlaylistPosition() + 1) + "/" + getPlaylist().size());
        customNotificationView.setImageViewResource(R.id.play_pause, isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);

        customNotificationViewS.setImageViewBitmap(R.id.cover, cover);
        customNotificationViewS.setTextViewText(R.id.title, currentMusic.Title);
        customNotificationViewS.setTextViewText(R.id.album, currentMusic.Album);
        customNotificationViewS.setTextViewText(R.id.artist, currentMusic.Artist);
        customNotificationViewS.setImageViewResource(R.id.play_pause, isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);


        builder.setContentTitle(currentMusic.Title)
                .setContentText(currentMusic.Album)
                .setSubText(currentMusic.Artist)
                .setLargeIcon(cover)
                .setColor(getApplicationContext().getColor(R.color.primary))
                .setTicker(currentMusic.getText());

        // TODO: Review this, it's making whole android slow
        // customNotificationView.setProgressBar(R.id.progress, getDuration(), getPosition(), !isPlaying());

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
                // TODO: Check this
                // pause();
            }

            @Override
            public void onPlay() {
                // TODO: Check this
                // play();
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

    private void update() {

        // Update media session
        updateMediaSession();

        // Update notification
        updateNotification();

        // HACK: Calling the devil
        System.gc();
        Runtime.getRuntime().gc();
    }

    private IntentFilter getIntentFilter() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(ACTION_CLOSE);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_OPEN);
        filter.addAction(ACTION_LIBRARY_UPDATE);
        filter.addAction(ACTION_LIBRARY_UPDATE_BEGINS);
        filter.addAction(ACTION_LIBRARY_UPDATED);
        filter.addAction(ACTION_LIBRARY_UPDATE_CANCEL);

        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        return filter;
    }

    private void handleIntent(Intent intent) {

        final String action = intent.getAction();

        if (action == null || TextUtils.isEmpty(action))
            return;

        if (action.equals(ACTION_CLOSE)) {
            stopSelf();
        } else if (action.equals(ACTION_PREVIOUS))
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

        } else if (action.equals(ACTION_LIBRARY_UPDATE)) {
            Boolean force = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FORCE, false);
            Boolean fastMode = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FASTMODE, SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_UPDATE_FASTMODE, LIBRARY_UPDATE_FASTMODE_DEFAULT));

            libraryUpdater = new MusicServiceLibraryUpdaterAsyncTask(this, force, fastMode) {
                @Override
                protected void onPostExecute(Result result) {
                    super.onPostExecute(result);

                    libraryUpdater = null;
                }
            };
            libraryUpdater.execute();

        } else if (action.equals(ACTION_LIBRARY_UPDATED)) {
            if (!isPlaying()) {
                stop();
                getPlaylist().clear();
            }

            for (Music music : Music.load(this))
                getPlaylist().add(music.Path);

        } else if (action.equals(ACTION_LIBRARY_UPDATE_CANCEL)) {

            if (libraryUpdater != null)
                libraryUpdater.cancel(true);

        } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    pause();
                    break;
                case 1:
                    play();
                    break;
            }

        } else if (action.equals(ACTION_REFRESH_SYSTEM_BINDINGS)) {
            update();
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
