package com.ilusons.harmony.ref;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IOEx {

    // Logger TAG
    private static final String TAG = IOEx.class.getSimpleName();

    public static String[] getExternalStorageDirectories(Context context) {

        List<String> results = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
            File[] externalDirs = context.getExternalFilesDirs(null);

            for (File file : externalDirs) {
                String path = file.getPath().split("/Android")[0];

                boolean addPath = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addPath = Environment.isExternalStorageRemovable(file);
                } else {
                    addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                }

                if (addPath) {
                    results.add(path);
                }
            }
        }

        if (results.isEmpty()) { //Method 2 for all versions
            // better variation of: http://stackoverflow.com/a/40123073/5002496
            String output = "";
            try {
                final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
                        .redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    output = output + new String(buffer);
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if (!output.trim().isEmpty()) {
                String devicePoints[] = output.split("\n");
                for (String voldPoint : devicePoints) {
                    results.add(voldPoint.split(" ")[2]);
                }
            }
        }

        //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                    Log.d(TAG, results.get(i) + " might not be extSDcard");
                    results.remove(i--);
                }
            }
        } else {
            for (int i = 0; i < results.size(); i++) {
                if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
                    Log.d(TAG, results.get(i) + " might not be extSDcard");
                    results.remove(i--);
                }
            }
        }

        String[] storageDirectories = new String[results.size()];
        for (int i = 0; i < results.size(); ++i) storageDirectories[i] = results.get(i);

        return storageDirectories;
    }

    public static File getDiskCache(Context context, String path, boolean isDir) {
        // Create a path pointing to the system-recommended cache dir for the app, with sub-dir named
        // thumbnails
        File cacheDir = new File(context.getCacheDir(), path);

        File parent = cacheDir;
        if (!isDir)
            parent = cacheDir.getParentFile();

        if (!parent.exists()) {
            if (parent.mkdirs()) {
                Log.d(TAG, "Successfully created dir:" + parent.getName());
            } else {
                Log.d(TAG, "Failed to create the dir:" + parent.getName());
            }
        }

        return cacheDir;
    }

    public static File getDiskCacheFile(Context context, String path) {
        if (context == null)
            return null;

        File cacheFile = getDiskCache(context, path, false);

        return cacheFile;
    }

    public static File getDiskCacheFile(Context context, String dir, String key) {
        // Create a path in that dir for a file, named by the default hash of the url
        File cacheFile = new File(getDiskCache(context, dir, true), "" + key.hashCode());

        return cacheFile;
    }

    public static boolean isInDiskCache(Context context, String dir, String key) {
        File cacheFile = getDiskCacheFile(context, dir, key);

        return cacheFile.exists();
    }

    public static Bitmap getBitmapFromDiskCache(Context context, String dir, String key) {
        File cacheFile = getDiskCacheFile(context, dir, key);

        try {
            // Open input stream to the cache file
            FileInputStream fis = new FileInputStream(cacheFile);
            // Read a bitmap from the file (which presumable contains bitmap in PNG format, since
            // that's how files are created)
            Bitmap local = BitmapFactory.decodeStream(fis);
            return local;
        } catch (Exception e) {
            // Log anything that might go wrong with IO to file
            Log.e(TAG, "Error when loading image from cache. ", e);
        }
        return null;
    }

    public static void putBitmapInDiskCache(Context context, String dir, String key, Bitmap bmp) {
        File cacheFile = getDiskCacheFile(context, dir, key);

        try {
            // Create a file at the file path, and open it for writing obtaining the output stream
            cacheFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(cacheFile);
            // Write the bitmap to the output stream (and thus the file) in PNG format (lossless compression)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            // Flush and close the output stream
            fos.flush();
            fos.close();
        } catch (Exception e) {
            // Log anything that might go wrong with IO to file
            Log.e(TAG, "Error when saving image to cache. ", e);
        }
    }
}

