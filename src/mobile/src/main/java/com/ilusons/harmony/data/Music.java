package com.ilusons.harmony.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.LyricsEx;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public class Music {

    // Logger TAG
    private static final String TAG = Music.class.getSimpleName();

    public static final String KEY_CACHE_DIR_COVER = "covers";
    public static final String KEY_CACHE_DIR_LYRICS = "lyrics";

    public static final String KEY_CACHE_KEY_LIBRARY = "library.index";

    public String Title = "";
    public String Artist = "";
    public String Album = "";
    public String Path;

    public String getText() {
        return TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title;
    }

    public String getTextDetailed() {
        StringBuilder sb = new StringBuilder();

        String nl = System.getProperty("line.separator");

        sb.append(Title);
        sb.append(nl);
        sb.append(Artist);
        sb.append(nl);
        sb.append(Album);
        sb.append(nl);

        return sb.toString();
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

    public String getLyrics(final Context context) {
        String result;

        // Load from cache
        result = CacheEx.getInstance().get(KEY_CACHE_DIR_LYRICS + Path).toString();

        if (result != null)
            return result;

        // File
        File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

        // Load from cache folder
        if (file.exists()) try {
            result = FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        if (result != null) {
            // Put in cache
            CacheEx.getInstance().put(KEY_CACHE_DIR_LYRICS + Path, result);
        }

        return result;
    }

    public void getLyricsOrDownload(final Context context, final JavaEx.ActionT<String> onResult) {
        (new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPostExecute(String result) {
                if (onResult != null)
                    onResult.execute(result);
            }

            @Override
            protected String doInBackground(Void... Voids) {
                String result = getLyrics(context);

                if (!TextUtils.isEmpty(result))
                    return result;

                ArrayList<LyricsEx.Lyrics> results = LyricsEx.GeniusApi.get(Artist + " " + Title);

                if (!(results == null || results.size() == 0))
                    result = results.get(0).Content;

                putLyrics(context, result);

                return result;
            }
        }).execute();
    }

    public void putLyrics(Context context, String content) {
        // File
        File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

        try {
            FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
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

                // TODO: This tags decoder is inefficient for android, takes too much memory
                if (data.getCover(context) == null) {
                    byte[] cover = tags.getAlbumImage();
                    if (cover != null && cover.length > 0) {
                        Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);

                        if (bmp != null)
                            putCover(context, data, bmp);
                    }
                }

                data.putLyrics(context, LyricsEx.getLyrics(tags));

            }

            if (TextUtils.isEmpty(data.Title) && mp3file.hasId3v1Tag()) {
                ID3v1 tags = mp3file.getId3v1Tag();

                data.Title = tags.getTitle();
                data.Artist = tags.getArtist();
                data.Album = tags.getAlbum();
            }

        } catch (Exception e) {
            Log.w(TAG, "decode audio data tags", e);
        }

        if (TextUtils.isEmpty(data.Title)) {
            data.Title = file.getName().replaceFirst("[.][^.]+$", "");
        }

        return data;
    }

    public static Music load(Context context, String path) {
        ArrayList<Music> all = load(context);

        Music m = null;

        for (Music item : all) {
            if (item.Path.equalsIgnoreCase(path)) {
                m = item;
                break;
            }
        }

        if (m == null)
            m = decodeFromFile(context, new File(path));

        return m;
    }

    public static ArrayList<Music> load(Context context) {
        ArrayList<Music> result = new ArrayList<>();

        File cacheFile = IOEx.getDiskCacheFile(context, KEY_CACHE_KEY_LIBRARY);
        if (!cacheFile.exists())
            return result;

        try {
            String json;
            json = FileUtils.readFileToString(cacheFile, "utf-8");

            Gson serializer = getSerializer();

            result.addAll(Arrays.asList(serializer.fromJson(json, Music[].class)));
        } catch (Exception e) {
            e.printStackTrace();

            return result;
        }

        return result;
    }

    public static void save(Context context, ArrayList<Music> data) {
        Gson serializer = getSerializer();

        String json = serializer.toJson(data.toArray(), Music[].class);

        File cacheFile = IOEx.getDiskCacheFile(context, KEY_CACHE_KEY_LIBRARY);
        try {
            FileUtils.writeStringToFile(cacheFile, json, "utf-8", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reset(Context context) {
        File cacheFile = IOEx.getDiskCacheFile(context, KEY_CACHE_KEY_LIBRARY);
        if (cacheFile.exists())
            cacheFile.delete();
    }

    static Gson getSerializer() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Music.class, new Serializer());
        gsonBuilder.registerTypeAdapter(Music.class, new Deserializer());

        Gson gson = gsonBuilder.create();

        return gson;
    }

    static class Serializer implements JsonSerializer<Music> {

        @Override
        public JsonElement serialize(final Music data, final Type type, final JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            result.add("Title", new JsonPrimitive(data.Title));
            result.add("Artist", new JsonPrimitive(TextUtils.isEmpty(data.Artist) ? "" : data.Artist));
            result.add("Album", new JsonPrimitive(TextUtils.isEmpty(data.Album) ? "" : data.Album));
            result.add("Path", new JsonPrimitive(data.Path));

            return result;
        }

    }

    static class Deserializer implements JsonDeserializer<Music> {

        @Override
        public Music deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Music result = new Music();

            JsonObject data = json.getAsJsonObject();

            result.Title = data.get("Title").getAsString();
            result.Artist = data.get("Artist").getAsString();
            result.Album = data.get("Album").getAsString();
            result.Path = data.get("Path").getAsString();

            return result;
        }
    }

}
