package com.ilusons.harmony.data;

import android.content.Context;

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
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.JavaEx;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

public class Stats {

    public static final String KEY_CACHE_STATS = "stats.data";

    public Integer TimesPlayed = 0;
    public Long TimeAdded = 0L;

    //region IO

    public static ArrayList<Stats> loadAll(Context context) {
        ArrayList<Stats> result = new ArrayList<>();

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

            result.add("TimesPlayed", new JsonPrimitive(data.TimesPlayed));
            result.add("TimeAdded", new JsonPrimitive(data.TimeAdded));

            return result;
        }

    }

    static class Deserializer implements JsonDeserializer<Stats> {

        @Override
        public Stats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Stats result = new Stats();

            JsonObject data = json.getAsJsonObject();

            result.TimesPlayed = data.get("TimesPlayed").getAsInt();
            result.TimeAdded = data.get("TimeAdded").getAsLong();

            return result;
        }
    }

    //endregion
}
