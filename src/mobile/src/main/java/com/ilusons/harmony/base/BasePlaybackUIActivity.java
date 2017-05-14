package com.ilusons.harmony.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import com.ilusons.harmony.R;

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

    // Events
    BaseMediaBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Create wake lock
        wakeLockForScreenOn = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, getClass().getName());

        super.onCreate(savedInstanceState);

        // Start service
        startService(new Intent(this, MusicService.class));

        // Broadcast receivers
        broadcastReceiver = new BaseMediaBroadcastReceiver(this) {

            @Override
            public void OnMusicServicePlay() {
                BasePlaybackUIActivity.this.OnMusicServicePlay();
            }

            @Override
            public void OnMusicServicePause() {
                BasePlaybackUIActivity.this.OnMusicServicePause();
            }

            @Override
            public void OnMusicServiceStop() {
                BasePlaybackUIActivity.this.OnMusicServiceStop();
            }

            @Override
            public void OnMusicServiceOpen(String uri) {
                BasePlaybackUIActivity.this.OnMusicServiceOpen(uri);
            }

            @Override
            public void OnMusicServiceLibraryUpdated() {
                BasePlaybackUIActivity.this.OnMusicServiceLibraryUpdated();
            }

        };

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
            broadcastReceiver.unRegister();
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

        // Events
        broadcastReceiver.register(this);

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

    protected void OnMusicServicePlay() {

    }

    protected void OnMusicServicePause() {

    }

    protected void OnMusicServiceStop() {

    }

    public void OnMusicServiceOpen(String uri) {

    }

    public void OnMusicServiceLibraryUpdated() {

    }

}
