package com.ilusons.harmony.base;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.lang.ref.WeakReference;

public abstract class BasePlaybackUIActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = BasePlaybackUIActivity.class.getSimpleName();

    // Services
    MusicService musicService;
    boolean isMusicServiceBound = false;
    ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.ServiceBinder binder = (MusicService.ServiceBinder) service;
            musicService = binder.getService();
            isMusicServiceBound = true;

            OnMusicServiceChanged(className, musicService, isMusicServiceBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            isMusicServiceBound = false;

            OnMusicServiceChanged(className, musicService, isMusicServiceBound);
        }
    };

    // Components
    PowerManager.WakeLock wakeLockForScreenOn;

    PlaybackBroadcastReceiver playbackBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Create wake lock
        wakeLockForScreenOn = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, getClass().getName());

        super.onCreate(savedInstanceState);

        // Start service
        startService(new Intent(this, MusicService.class));

        // Broadcast receivers
        playbackBroadcastReceiver = new PlaybackBroadcastReceiver(this);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

        // Release wake lock
        if (wakeLockForScreenOn.isHeld())
            wakeLockForScreenOn.release();

        // Intents
        try {
            unregisterReceiver(playbackBroadcastReceiver);
        } catch (final Throwable e) {
            Log.w(TAG, e);
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Bind service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

        // Acquire wake lock
        wakeLockForScreenOn.acquire();

        // Intents
        final IntentFilter filter = new IntentFilter();

        filter.addAction(MusicService.ACTION_PLAY);
        filter.addAction(MusicService.ACTION_PAUSE);
        filter.addAction(MusicService.ACTION_STOP);

        registerReceiver(playbackBroadcastReceiver, filter);

    }

    @Override
    protected void onStop() {

        super.onStop();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

        // Release wake lock
        wakeLockForScreenOn.release();

    }

    protected MusicService getMusicService() {
        return musicService;
    }

    protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
    }

    private final static class PlaybackBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<BasePlaybackUIActivity> reference;

        public PlaybackBroadcastReceiver(final BasePlaybackUIActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            BaseActivity activity = reference.get();

            if (activity != null) {

                if (action.equals(MusicService.ACTION_PLAY)) {

                }

            }
        }
    }

}
