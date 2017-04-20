package com.ilusons.harmony.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

public abstract class BaseMediaBroadcastReceiver extends BroadcastReceiver {

    // Logger TAG
    private static final String TAG = BaseMediaBroadcastReceiver.class.getSimpleName();

    public static String ACTION_OPEN = TAG + ".open";
    public static String KEY_URI = TAG + ".uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive\n" + intent);

        String action = intent.getAction();

        if (action.equals(ACTION_OPEN) || action.equals(MusicService.ACTION_OPEN)) {
            String uri = intent.getStringExtra(KEY_URI);

            if (!TextUtils.isEmpty(uri))
                open(uri);
        }

        if (action.equals(MusicService.ACTION_LIBRARY_UPDATED)) {
            libraryUpdated();
        }
    }

    LocalBroadcastManager broadcastManager;

    public void register(Context context) {
        broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(ACTION_OPEN);

        intentFilter.addAction(MusicService.ACTION_OPEN);
        intentFilter.addAction(MusicService.ACTION_PLAY);
        intentFilter.addAction(MusicService.ACTION_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STOP);
        intentFilter.addAction(MusicService.ACTION_LIBRARY_UPDATED);

        broadcastManager.registerReceiver(this, intentFilter);
    }

    public void unRegister() {
        broadcastManager.unregisterReceiver(this);
    }

    public void open(String uri) {

    }

    public void libraryUpdated() {

    }

}
