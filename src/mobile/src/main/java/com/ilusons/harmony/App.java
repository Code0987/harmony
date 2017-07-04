package com.ilusons.harmony;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;

import com.google.android.gms.ads.MobileAds;
import com.ilusons.harmony.base.MusicService;

import jonathanfinerty.once.Once;

public class App extends Application {
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();

        Once.initialise(this);

        // Start scan
        Intent musicServiceIntent = new Intent(this, MusicService.class);
        musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
        startService(musicServiceIntent);

        // Initialize
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