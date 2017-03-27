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

        if (action.equals(ACTION_OPEN)) {
            String uri = intent.getStringExtra(KEY_URI);

            if (!TextUtils.isEmpty(uri))
                open(uri);
        }
    }

    LocalBroadcastManager broadcastManager;

    public void register(Context context) {
        broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(ACTION_OPEN);

        broadcastManager.registerReceiver(this, intentFilter);
    }

    public void unRegister() {
        broadcastManager.unregisterReceiver(this);
    }

    public abstract void open(String uri);

}
