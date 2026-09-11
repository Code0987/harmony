package com.ilusons.harmony;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.DefaultBitmapMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.LibraryStore;

import jonathanfinerty.once.Once;

public class App extends Application {

	// Logger TAG
	private static final String TAG = App.class.getSimpleName();

	// Called when the application is starting, before any other application objects have been created.
	// Overriding this method is totally optional!
	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				Log.wtf(TAG, e);
			}
		});

		LibraryStore.init(this);

		try {
			ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
					.setMainDiskCacheConfig(DiskCacheConfig
							.newBuilder(this)
							.build())
					.build();
			Fresco.initialize(this, config);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Once.initialise(this);

		try {
			Analytics.getInstance().initSettings(this);
			Analytics.getInstance().initLastfm(this);
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
