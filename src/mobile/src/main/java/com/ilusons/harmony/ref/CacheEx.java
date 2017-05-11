package com.ilusons.harmony.ref;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import java.lang.ref.SoftReference;
import java.util.HashMap;

public class CacheEx {

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
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;

        // Init
        softMemoryCache = new HashMap<>();

        bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public Object get(String key) {
        SoftReference<Object> value = softMemoryCache.get(key);

        if (value != null && value.get() == null)
            softMemoryCache.remove(key);

        if (value != null)
            return value.get();

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
        Bitmap value = bitmapCache.get(key);

        return value;
    }

    public void putBitmap(String key, Bitmap value) {
        if (getBitmap(key) == null) {
            bitmapCache.put(key, value);
        }
    }

}