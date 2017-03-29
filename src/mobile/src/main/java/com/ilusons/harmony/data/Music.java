package com.ilusons.harmony.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.ID3TagsEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;

public class Music {

    // Logger TAG
    private static final String TAG = Music.class.getSimpleName();

    public static final String KEY_CACHE_DIR_COVER = "covers";

    public String Title = "Untitled";
    public String Artist = "Unknown artist";
    public String Album;
    public String Path;
    public String Lyrics;

    public ID3v2 Tags;

    public String getText() {
        return TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title;
    }

    public Bitmap getCover(final Context context) {
        Bitmap result;

        // Load from cache
        result = CacheEx.getInstance().getBitmap(Path);

        if (result != null)
            return result;

        // File
        File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, Path);

        // Load from cache folder
        if (file.exists())
            result = BitmapFactory.decodeFile(file.getAbsolutePath());

        // Resample
        if (result != null) {
            try {
                Bitmap.Config config = result.getConfig();
                if (config == null) {
                    config = Bitmap.Config.ARGB_8888;
                }
                result = result.copy(config, false);
            } catch (Exception e) {
                Log.w(TAG, e);
            }

            // Put in cache
            CacheEx.getInstance().putBitmap(Path, result);
        }

        return result;
    }

    public static void getCoverOrDownload(final Context context, final Music data, final JavaEx.ActionT<Bitmap> onResult) {
        (new AsyncTask<Object, Object, Bitmap>() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (onResult != null)
                    onResult.execute(bitmap);
            }

            @Override
            protected Bitmap doInBackground(Object... objects) {
                Bitmap result = data.getCover(context);

                // File
                File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, data.Path);

                // Download and cache to folder then load
                if (result == null) {
                    try {
                        URL url = new URL(String.format(
                                "https://itunes.apple.com/search?term=%s+%s&entity=song&media=music",
                                URLEncoder.encode(data.Artist, "UTF-8"),
                                URLEncoder.encode(data.Title, "UTF-8")));

                        Connection connection = Jsoup.connect(url.toExternalForm())
                                .timeout(0)
                                .ignoreContentType(true);

                        Document document = connection.get();

                        JsonObject response = new JsonParser().parse(document.text()).getAsJsonObject();

                        JsonArray results = response.getAsJsonArray("results");

                        String downloadUrl = results
                                .get(0)
                                .getAsJsonObject()
                                .get("artworkUrl60")
                                .getAsString()
                                .replace("60x60bb.jpg", "1000x1000bb.jpg");

                        BufferedInputStream in = null;
                        FileOutputStream out = null;
                        try {
                            in = new BufferedInputStream(new URL(downloadUrl).openStream());
                            out = new FileOutputStream(file.getAbsoluteFile());

                            final byte data[] = new byte[1024];
                            int count;
                            while ((count = in.read(data, 0, 1024)) != -1) {
                                out.write(data, 0, count);
                            }
                        } finally {
                            if (in != null) {
                                in.close();
                            }
                            if (out != null) {
                                out.close();
                            }
                        }

                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }

                    if (file.exists())
                        result = BitmapFactory.decodeFile(file.getAbsolutePath());

                    // Resample
                    if (result != null) {
                        try {
                            Bitmap.Config config = result.getConfig();
                            if (config == null) {
                                config = Bitmap.Config.ARGB_8888;
                            }
                            result = result.copy(config, false);
                        } catch (Exception e) {
                            Log.w(TAG, e);
                        }

                        // Put in cache
                        CacheEx.getInstance().putBitmap(data.Path, result);
                    }

                }

                return result;
            }
        }).execute();
    }

    public static void putCover(Context context, Music data, Bitmap bmp) {
        IOEx.putBitmapInDiskCache(context, KEY_CACHE_DIR_COVER, data.Path, bmp);
    }

    public static Music decodeFromFile(Context context, File file) {
        Music data = new Music();

        data.Path = file.getAbsolutePath();

        try {
            Mp3File mp3file = new Mp3File(file.getAbsoluteFile());

            if (mp3file.hasId3v2Tag()) {
                ID3v2 tags = mp3file.getId3v2Tag();

                data.Title = tags.getTitle();
                data.Artist = tags.getArtist();
                data.Album = tags.getAlbum();
                data.Tags = tags;

                byte[] cover = ID3TagsEx.getCover(tags);
                if (cover != null && cover.length > 0) {
                    Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);

                    if (bmp != null)
                        putCover(context, data, bmp);
                }

                data.Lyrics = ID3TagsEx.getLyrics(tags);

            }

            if (TextUtils.isEmpty(data.Title) && mp3file.hasId3v1Tag()) {
                ID3v1 tags = mp3file.getId3v1Tag();

                data.Title = tags.getTitle();
                data.Artist = tags.getArtist();
                data.Album = tags.getAlbum();
            }

            if (TextUtils.isEmpty(data.Title)) {
                data.Title = file.getName().replaceFirst("[.][^.]+$", "");
            }

        } catch (Exception e) {
            Log.e(TAG, "decode audio data tags", e);

            return null;
        }

        return data;
    }

}
