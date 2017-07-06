package com.ilusons.harmony;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.ads.MobileAds;
import com.ilusons.harmony.base.MusicService;

import jonathanfinerty.once.Once;

public class App extends Application {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();

        // Fabric
        final Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        final Fabric fabric = new Fabric.Builder(this)
                .kits(crashlyticsKit)
                .debuggable(BuildConfig.DEBUG)
                .build();
        Fabric.with(fabric);

        // Tools
        Once.initialise(this);

        // Start scan
        Intent musicServiceIntent = new Intent(this, MusicService.class);
        musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
        startService(musicServiceIntent);

        // Ads
        MobileAds.initialize(this, BuildConfig.AD_PUB_ID);

    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}