package com.ilusons.harmony.ref;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import java.lang.ref.SoftReference;
import java.util.HashMap;

public class CacheEx {

	// Logger TAG
	private static final String TAG = CacheEx.class.getSimpleName();

	private static CacheEx instance;

	public static CacheEx getInstance() {
		if (instance == null) {
			instance = new CacheEx();
		}
		return instance;
	}

	private static HashMap<Object, SoftReference<Object>> softMemoryCache;

	private static LruCache<String, Bitmap> bitmapCache;

	private CacheEx() {
		// Get the Max available memory
		int maxMemory = (int) Runtime.getRuntime().maxMemory() / 1024;
		int cacheSize = maxMemory / 16; // HACK: Works so far!

		Log.d(TAG, "maxMemory = " + maxMemory + "\ncachesize = " + cacheSize);

		// Init
		softMemoryCache = new HashMap<>();

		bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value == null ? 0 : value.getRowBytes() * value.getHeight() / 1024;
			}
		};
	}

	public Object get(String key) {
		SoftReference<Object> value = softMemoryCache.get(key);

		if (value != null && value.get() == null)
			softMemoryCache.remove(key);

		if (value != null)
			try {
				return value.get();
			} catch (Exception e) {
				// Eat
			}

		return null;
	}

	public void put(String key, SoftReference<Object> value) {
		if (get(key) == null) {
			softMemoryCache.put(key, value);
		}
	}

	public void put(String key, Object value) {
		put(key, new SoftReference<>(value));
	}

	public Bitmap getBitmap(String key) {
		try {
			Bitmap value = bitmapCache.get(key);
			if (value == null || value.isRecycled())
				return null;

			return value;
		} catch (Exception e) {
			// Eat
		}
		return null;
	}

	public void putBitmap(String key, Bitmap value) {
		if (getBitmap(key) == null) {
			bitmapCache.put(key, value);
		}

		if (bitmapCache.size() >= bitmapCache.maxSize() * 0.75)
			try {
				// HACK: Calling the devil
				System.gc();
				Runtime.getRuntime().gc();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}
