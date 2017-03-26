package com.ilusons.harmony.ref;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class LyricsEx {

    public static final String TAG = LyricsEx.class.getSimpleName();

    public static class Lyrics {

        public String Title;
        public String Artist;
        public String Url;
        public String Content;

    }

    public static class GeniusApi {

        public static ArrayList<Lyrics> get(String query) {
            ArrayList<Lyrics> results = new ArrayList<>();

            query = Normalizer
                    .normalize(query, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            JsonObject response = null;
            try {
                URL url = new URL(String.format("http://api.genius.com/search?q=%s", URLEncoder.encode(query, "UTF-8")));

                Connection connection = Jsoup.connect(url.toExternalForm())
                        .header("Authorization",
                                "Bearer " + "ohI-oJ9_EL7bFnRO4ZyU5bNrNuPfHdEnNNHfagHIlq8kZulJOYTlW3tDqsVoXvwb")
                        .timeout(0)
                        .ignoreContentType(true);

                Document document = connection.get();

                response = new JsonParser().parse(document.text()).getAsJsonObject();

            } catch (JsonSyntaxException e) {
                Log.w(TAG, "searchGetSongUrls", e);
            } catch (IOException e) {
                Log.w(TAG, "searchGetSongUrls", e);
            }

            if (response == null || response.getAsJsonObject("meta").get("status").getAsInt() != 200)
                return results;

            JsonArray hits = response.getAsJsonObject("response").getAsJsonArray("hits");

            int processed = 0;
            while (processed < hits.size()) {

                try {

                    JsonObject song = hits.get(processed).getAsJsonObject().getAsJsonObject("result");

                    Lyrics lyrics = new Lyrics();

                    lyrics.Artist = song.getAsJsonObject("primary_artist").get("name").getAsString();
                    lyrics.Title = song.get("title").getAsString();
                    lyrics.Url = "http://genius.com/songs/" + song.get("id").getAsString();

                    Document lyricsPage;
                    String text;
                    try {

                        lyricsPage = Jsoup.connect(lyrics.Url).get();
                        Elements lyricsDiv = lyricsPage.select(".lyrics");
                        if (lyricsDiv.isEmpty())
                            throw new StringIndexOutOfBoundsException();
                        else
                            text = Jsoup.clean(lyricsDiv.html(), Whitelist.none().addTags("br")).trim();

                    } catch (HttpStatusException e) {
                        Log.w(TAG, e);

                        return null;
                    } catch (IOException | StringIndexOutOfBoundsException e) {
                        Log.w(TAG, e);

                        return null;
                    }

                    Pattern pattern = Pattern.compile("\\[.+\\]");
                    StringBuilder builder = new StringBuilder();
                    for (String line : text.split("<br> ")) {
                        String strippedLine = line.replaceAll("\\s", "");
                        if (!pattern.matcher(strippedLine).matches() && !(strippedLine.isEmpty() && builder.length() == 0))
                            builder.append(line.replaceAll("\\P{Print}", "")).append("<br/>");
                    }
                    if (builder.length() > 5)
                        builder.delete(builder.length() - 5, builder.length());

                    lyrics.Content = Normalizer
                            .normalize(builder.toString(), Normalizer.Form.NFD);

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                processed++;
            }

            return results;
        }

    }

}
