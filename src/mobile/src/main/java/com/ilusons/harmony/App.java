package com.ilusons.harmony;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.RealmEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import jonathanfinerty.once.Once;

public class App extends Application {

	// Logger TAG
	private static final String TAG = App.class.getSimpleName();

	// Called when the application is starting, before any other application objects have been created.
	// Overriding this method is totally optional!
	@Override
	public void onCreate() {
		super.onCreate();

		// Memory leak
		try {
			if (LeakCanary.isInAnalyzerProcess(this)) {
				// This process is dedicated to LeakCanary for heap analysis.
				// You should not init your app in this process.
				return;
			}
			LeakCanary.install(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// WTFs
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				Toast.makeText(App.this, "Aw, snap!", Toast.LENGTH_LONG).show();

				Log.wtf(TAG, e);

				Crashlytics.logException(e);
			}
		});

		// Fabric
		final Crashlytics crashlyticsKit = new Crashlytics.Builder()
				.core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
				.build();
		Fabric.with(this, crashlyticsKit);

		// Firebase

		// DB
		RealmEx.init(this);

		Once.initialise(this);

		// Analytics
		Analytics.getInstance().initSettings(this);
		Analytics.getInstance().initLastfm(this);
		Analytics.getInstance().initDC(this);

		// Start scan
		try {
			if (MusicServiceLibraryUpdaterAsyncTask.getScanAutoEnabled(this)) {
				Intent musicServiceIntent = new Intent(this, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
				startService(musicServiceIntent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Ads
		// TODO: Ads later
		// MobileAds.initialize(this, BuildConfig.AD_PUB_ID);

		try {
			AndroidEx.overrideFont(getApplicationContext(), "DEFAULT", "fonts/open-sans.light.ttf");
			AndroidEx.overrideFont(getApplicationContext(), "NORMAL", "fonts/open-sans.light.ttf");
			AndroidEx.overrideFont(getApplicationContext(), "SANS_SERIF", "fonts/open-sans.light.ttf");
			AndroidEx.overrideFont(getApplicationContext(), "SERIF", "fonts/roboto-slab.light.ttf");
		} catch (Exception e) {
			e.printStackTrace();
		}

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
