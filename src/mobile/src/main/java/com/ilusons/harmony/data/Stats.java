package com.ilusons.harmony.data;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.JavaEx;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

// TODO: Upgrade database to optimized ones
public class Stats {

    public static final String KEY_CACHE_STATS = "stats.data";

    public String Path = null;
    public Integer Played = 0;
    public Long LastPlayed = -1L;
    public Integer Skipped = 0;
    public Long TimeAdded = -1L;
    public String Mood = null;

    //region IO

    public static ArrayList<Stats> loadAll(Context context) {
        ArrayList<Stats> result = null;

        try {
            result = (ArrayList<Stats>) CacheEx.getInstance().get(KEY_CACHE_STATS);
            if (result != null)
                return result;
        } catch (Exception e) {
            // Eat
        }

        result = new ArrayList<>();

        File cacheFile = IOEx.getDiskCacheFile(context, KEY_CACHE_STATS);
        if (!cacheFile.exists())
            return result;

        try {
            String json;
            json = FileUtils.readFileToString(cacheFile, "utf-8");

            Gson serializer = getSerializer();

            result.addAll(Arrays.asList(serializer.fromJson(json, Stats[].class)));
        } catch (Exception e) {
            e.printStackTrace();

            return result;
        }

        return result;
    }

    public static void saveAll(Context context, ArrayList<Stats> data) {
        CacheEx.getInstance().put(KEY_CACHE_STATS, data);

        Gson serializer = getSerializer();

        String json = serializer.toJson(data.toArray(), Stats[].class);

        File cacheFile = IOEx.getDiskCacheFile(context, KEY_CACHE_STATS);
        try {
            FileUtils.writeStringToFile(cacheFile, json, "utf-8", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateAll(Context context, JavaEx.ActionT<Stats> action) {
        ArrayList<Stats> data = loadAll(context);
        for (Stats stats : data) {
            action.execute(stats);
        }
        saveAll(context, data);
    }

    public static void updateOrCreate(Context context, String path, JavaEx.ActionT<Stats> action) {
        ArrayList<Stats> data = loadAll(context);
        Stats current = null;
        for (Stats stats : data) {
            if (stats.Path.equalsIgnoreCase(path))
                current = stats;
        }
        if (current == null) {
            current = new Stats();
            current.Path = path;
        }
        action.execute(current);
        saveAll(context, data);
    }

    public static void updateOrCreateAsync(final Context context, final String path, final JavaEx.ActionT<Stats> action) {
        synchronized (KEY_CACHE_STATS) {
            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    updateOrCreate(context, path, action);

                    return null;
                }
            }).execute();
        }
    }

    public static Stats get(Context context, String path) {
        ArrayList<Stats> data = loadAll(context);
        Stats current = null;
        for (Stats stats : data) {
            if (stats.Path.equalsIgnoreCase(path))
                current = stats;
        }
        return current;
    }

    //endregion

    //region Serializer

    public static Gson getSerializer() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Stats.class, new Serializer());
        gsonBuilder.registerTypeAdapter(Stats.class, new Deserializer());

        Gson gson = gsonBuilder.create();

        return gson;
    }

    static class Serializer implements JsonSerializer<Stats> {

        @Override
        public JsonElement serialize(final Stats data, final Type type, final JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            result.add("Path", new JsonPrimitive(data.Path));
            result.add("Played", new JsonPrimitive(data.Played));
            result.add("LastPlayed", new JsonPrimitive(data.LastPlayed));
            result.add("Skipped", new JsonPrimitive(data.Skipped));
            result.add("TimeAdded", new JsonPrimitive(data.TimeAdded));
            result.add("Mood", new JsonPrimitive(data.Mood));

            return result;
        }

    }

    static class Deserializer implements JsonDeserializer<Stats> {

        @Override
        public Stats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Stats result = new Stats();

            JsonObject data = json.getAsJsonObject();

            result.Path = data.get("Path").getAsString();
            result.Played = data.get("Played").getAsInt();
            result.LastPlayed = data.get("LastPlayed").getAsLong();
            result.Skipped = data.get("Skipped").getAsInt();
            result.TimeAdded = data.get("TimeAdded").getAsLong();
            result.Mood = data.get("Mood").getAsString();

            return result;
        }
    }

    //endregion
}
