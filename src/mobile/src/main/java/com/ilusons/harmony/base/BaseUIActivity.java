package com.ilusons.harmony.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public abstract class BaseUIActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = BaseUIActivity.class.getSimpleName();

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

        // Broadcast receivers
        broadcastReceiver = new BaseMediaBroadcastReceiver(this) {

            @Override
            public void OnMusicServicePlay() {
                BaseUIActivity.this.OnMusicServicePlay();
            }

            @Override
            public void OnMusicServicePause() {
                BaseUIActivity.this.OnMusicServicePause();
            }

            @Override
            public void OnMusicServiceStop() {
                BaseUIActivity.this.OnMusicServiceStop();
            }

            @Override
            public void OnMusicServiceOpen(String uri) {
                BaseUIActivity.this.OnMusicServiceOpen(uri);
            }

            @Override
            public void OnMusicServiceLibraryUpdateBegins() {
                BaseUIActivity.this.OnMusicServiceLibraryUpdateBegins();
            }

            @Override
            public void OnMusicServiceLibraryUpdated() {
                BaseUIActivity.this.OnMusicServiceLibraryUpdated();
            }

        };

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

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

        // Release wake lock
        wakeLockForScreenOn.release();

    }

    protected void OnMusicServicePlay() {

    }

    protected void OnMusicServicePause() {

    }

    protected void OnMusicServiceStop() {

    }

    public void OnMusicServiceOpen(String uri) {

    }

    public void OnMusicServiceLibraryUpdateBegins() {

    }

    public void OnMusicServiceLibraryUpdated() {

    }

}
