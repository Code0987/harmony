package com.ilusons.harmony.ref;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LyricsEx {

    public static final String TAG = LyricsEx.class.getSimpleName();

    public static class Lyrics {

        public String Title;
        public String Artist;
        public String Url;
        public String Content;

    }

    private static Pattern ts = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static String cleanLyrics(String s) {
        return ts.matcher(s).replaceAll("");
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
                        .timeout(3 * 1000)
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
            while (processed < 1 && processed < hits.size()) { // HACK: Check only first result

                try {

                    JsonObject song = hits.get(processed).getAsJsonObject().getAsJsonObject("result");

                    Lyrics lyrics = new Lyrics();

                    lyrics.Artist = song.getAsJsonObject("primary_artist").get("name").getAsString();
                    lyrics.Title = song.get("title").getAsString();
                    lyrics.Url = "http://genius.com/songs/" + song.get("id").getAsString();

                    Document lyricsPage;
                    String text;
                    try {

                        lyricsPage = Jsoup.connect(lyrics.Url).timeout(3 * 1000).get();
                        Elements lyricsDiv = lyricsPage.select(".lyrics");
                        if (lyricsDiv.isEmpty())
                            throw new StringIndexOutOfBoundsException();
                        else
                            text = Jsoup.clean(lyricsDiv.html(), Whitelist.none().addTags("br")).trim();

                    } catch (HttpStatusException e) {
                        return null;
                    } catch (IOException | StringIndexOutOfBoundsException e) {
                        return null;
                    }

                    Pattern pattern = Pattern.compile("\\[.+\\]");
                    StringBuilder builder = new StringBuilder();
                    for (String line : text.split("<br> ")) {
                        String strippedLine = line.replaceAll("\\s", "");
                        if (!pattern.matcher(strippedLine).matches() && !(strippedLine.isEmpty() && builder.length() == 0))
                            builder.append(line.replaceAll("\\P{Print}", "")).append("\n");
                    }
                    if (builder.length() > 5)
                        builder.delete(builder.length() - 5, builder.length());

                    lyrics.Content = Normalizer
                            .normalize(builder.toString(), Normalizer.Form.NFD);

                    results.add(lyrics);

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                processed++;
            }

            return results;
        }

    }

    public static class ViewLyricsApi {

        /*
         * Needed data
         */
        private static final String url = "http://search.crintsoft.com/searchlyrics.htm";
        //ACTUAL: http://search.crintsoft.com/searchlyrics.htm
        //CLASSIC: http://www.ViewLyricsApi.com:1212/searchlyrics.htm

        private static final String clientUserAgent = "MiniLyrics4Android";
        //NORMAL: MiniLyrics <version> for <player>
        //EXAMPLE: MiniLyrics 7.6.44 for Windows Media Player
        //MOBILE: MiniLyrics4Android

        private static final String clientTag = "client=\"ViewLyricsApiOpenSearcher\"";
        //NORMAL: MiniLyrics
        //MOBILE: MiniLyricsForAndroid

        private static final String searchQueryBase = "<?xml version='1.0' encoding='utf-8' ?><searchV1 artist=\"%s\" title=\"%s\" OnlyMatched=\"1\" %s/>";

        private static final String searchQueryPage = " RequestPage='%d'";

        private static final byte[] magickey = "Mlv1clt4.0".getBytes();

        public static Lyrics get(String artist, String title) throws IOException, NoSuchAlgorithmException, SAXException, ParserConfigurationException {
            ArrayList<Lyrics> results =
                    get(
                            String.format(searchQueryBase, artist, title, clientTag +
                                    String.format(searchQueryPage, 0)) // Create XMLQuery String
                    );
            if (results.size() == 0)
                return new Lyrics();

            String url = results.get(0).Url;
            url = url.replace("minilyrics", "ViewLyricsApi");

            int artistDistance = StringUtils.getLevenshteinDistance(results.get(0).Artist, artist);
            int titleDistance = StringUtils.getLevenshteinDistance(results.get(0).Title, title);

            if (url.endsWith("txt") || artistDistance > 6 || titleDistance > 6)
                return new Lyrics();
            Lyrics result = new Lyrics();
            result.Title = (title);
            result.Artist = (artist);
            result.Content = (getUrlAsString(url).replaceAll("(\\[(?=.[a-z]).+\\]|<.+?>|www.*[\\s])", "").replaceAll("[\n\r]", " ").replaceAll("\\[", "\n\\["));

            return result;
        }

        private static ArrayList<Lyrics> get(String searchQuery) throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS).build();

            RequestBody body = RequestBody.create(MediaType.parse("application/text"), assembleQuery(searchQuery.getBytes("UTF-8")));

            Request request = new Request.Builder()
                    .header("User-Agent", clientUserAgent)
                    .post(body)
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            BufferedReader rd = new BufferedReader
                    (new InputStreamReader(response.body().byteStream(), "ISO_8859_1"));

            // Get full result
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = rd.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            String full = builder.toString();

            // Decrypt, parse, store, and return the result list
            return parseResultXML(decryptResultXML(full));
        }

        public static byte[] assembleQuery(byte[] valueBytes) throws NoSuchAlgorithmException, IOException {
            // Create the variable POG to be used in a dirt code
            byte[] pog = new byte[valueBytes.length + magickey.length]; //TODO Give a better name then POG

            // POG = XMLQuery + Magic Key
            System.arraycopy(valueBytes, 0, pog, 0, valueBytes.length);
            System.arraycopy(magickey, 0, pog, valueBytes.length, magickey.length);

            // POG is hashed using MD5
            byte[] pog_md5 = MessageDigest.getInstance("MD5").digest(pog);

            //TODO Thing about using encryption or k as 0...
            // Prepare encryption key
            int j = 0;
            for (byte octet : valueBytes) {
                j += octet;
            }
            int k = (byte) (j / valueBytes.length);

            // Value is encrypted
            for (int m = 0; m < valueBytes.length; m++)
                valueBytes[m] = (byte) (k ^ valueBytes[m]);

            // Prepare result code
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            // Write Header
            result.write(0x02);
            result.write(k);
            result.write(0x04);
            result.write(0x00);
            result.write(0x00);
            result.write(0x00);

            // Write Generated MD5 of POG problaby to be used in a search cache
            result.write(pog_md5);

            // Write encrypted value
            result.write(valueBytes);

            // Return magic encoded query
            return result.toByteArray();
        }

        public static String decryptResultXML(String value) {
            // Get Magic key value
            char magickey = value.charAt(1);

            // Prepare output
            ByteArrayOutputStream neomagic = new ByteArrayOutputStream();

            // Decrypts only the XML
            for (int i = 22; i < value.length(); i++)
                neomagic.write((byte) (value.charAt(i) ^ magickey));

            // Return value
            return neomagic.toString();
        }

        private static String readStrFromAttr(Element elem, String attr, String def) {
            String data = elem.getAttribute(attr);
            try {
                if (data != null && data.length() > 0)
                    return data;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return def;
        }

        public static ArrayList<Lyrics> parseResultXML(String resultXML) throws SAXException, IOException, ParserConfigurationException {
            // Create array for storing the results
            ArrayList<Lyrics> availableLyrics = new ArrayList<>();

            // Parse XML
            ByteArrayInputStream resultBA = new ByteArrayInputStream(resultXML.getBytes("UTF-8"));
            Element resultRootElem = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resultBA).getDocumentElement();

            String server_url = readStrFromAttr(resultRootElem, "server_url", "http://www.ViewLyricsApi.com/");

            NodeList resultItemList = resultRootElem.getElementsByTagName("fileinfo");
            for (int i = 0; i < resultItemList.getLength(); i++) {
                Element itemElem = (Element) resultItemList.item(i);
                Lyrics item = new Lyrics();

                item.Url = (server_url + readStrFromAttr(itemElem, "link", ""));
                item.Artist = (readStrFromAttr(itemElem, "artist", ""));
                item.Title = (readStrFromAttr(itemElem, "title", ""));
                //item.setLyricsFileName(readStrFromAttr(itemElem, "filename", ""));
                //itemInfo.setFType(readIntFromAttr(itemElem, "file_type", 0));
                //itemInfo.setMatchVal(readFloatFromAttr(itemElem, "match_value", 0.0F));
                //itemInfo.setTimeLenght(readIntFromAttr(itemElem, "timelength", 0));


                availableLyrics.add(item);
            }

            return availableLyrics;
        }

        public static String USER_AGENT =
                "Mozilla/5.0 (Linux; U; Android 6.0.1; ko-kr; Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

        public static String getUrlAsString(String paramURL) throws IOException {
            return getUrlAsString(new URL(paramURL));
        }

        public static String getUrlAsString(URL paramURL) throws IOException {
            Request request = new Request.Builder().header("User-Agent", USER_AGENT).url(paramURL).build();
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build();
            Response response = client.newCall(request).execute();

            return response.body().string();
        }

    }

}
