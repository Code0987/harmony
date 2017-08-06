package com.ilusons.harmony.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

public abstract class BaseMediaBroadcastReceiver extends BroadcastReceiver {

    // Logger TAG
    private static final String TAG = BaseMediaBroadcastReceiver.class.getSimpleName();

    private final WeakReference<Context> reference;

    public BaseMediaBroadcastReceiver(final Context ref) {
        reference = new WeakReference<>(ref);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Context ref = reference.get();
        String action = intent.getAction();

        Log.d(TAG, "onReceive\nintent = " + intent + "\nref = ");

        if (ref == null)
            return;

        if (action.equals(MusicService.ACTION_PLAY)) {
            OnMusicServicePlay();
        }

        if (action.equals(MusicService.ACTION_PAUSE)) {
            OnMusicServicePause();
        }

        if (action.equals(MusicService.ACTION_STOP)) {
            OnMusicServiceStop();
        }

        if (action.equals(MusicService.ACTION_OPEN)) {
            String uri = intent.getStringExtra(MusicService.KEY_URI);

            if (!TextUtils.isEmpty(uri))
                OnMusicServiceOpen(uri);
        }

        if (action.equals(MusicService.ACTION_PREPARED)) {
            OnMusicServicePrepared();
        }

        if (action.equals(MusicService.ACTION_LIBRARY_UPDATE_BEGINS)) {
            OnMusicServiceLibraryUpdateBegins();
        }

        if (action.equals(MusicService.ACTION_LIBRARY_UPDATED)) {
            OnMusicServiceLibraryUpdated();
        }
    }

    LocalBroadcastManager broadcastManager;

    public void register(Context context) {
        broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(MusicService.ACTION_PLAY);
        intentFilter.addAction(MusicService.ACTION_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STOP);
        intentFilter.addAction(MusicService.ACTION_OPEN);
        intentFilter.addAction(MusicService.ACTION_PREPARED);
        intentFilter.addAction(MusicService.ACTION_LIBRARY_UPDATE_BEGINS);
        intentFilter.addAction(MusicService.ACTION_LIBRARY_UPDATED);

        broadcastManager.registerReceiver(this, intentFilter);
    }

    public void unRegister() {
        broadcastManager.unregisterReceiver(this);
    }

    protected void OnMusicServicePlay() {

    }

    protected void OnMusicServicePause() {

    }

    protected void OnMusicServiceStop() {

    }

    public void OnMusicServiceOpen(String uri) {

    }

    public void OnMusicServicePrepared() {

    }

    public void OnMusicServiceLibraryUpdateBegins() {

    }

    public void OnMusicServiceLibraryUpdated() {

    }

}
