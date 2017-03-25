package com.ilusons.harmony.ref;

import android.text.TextUtils;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v2FrameSet;
import com.mpatric.mp3agic.ID3v2TXXXFrameData;

import java.util.Map;
import java.util.regex.Pattern;

public class ID3TagsEx {

    public static final String TAG = ID3TagsEx.class.getSimpleName();

    public static String getLyrics(ID3v2 id3v2) {
        String value = id3v2.getLyrics();

        if (value != null && !TextUtils.isEmpty(value))
            return value;

        Map<String, ID3v2FrameSet> frameSets = id3v2.getFrameSets();

        ID3v2FrameSet txxx = frameSets.get(ID3v2TXXXFrameData.ID_FIELD);
        if (txxx == null)
            return null;

        ID3v2TXXXFrameData txxxFrameData = ID3v2TXXXFrameData.extract(
                txxx,
                id3v2.hasUnsynchronisation(),
                "UNSYNCED LYRICS");

        if (txxxFrameData != null)
            return txxxFrameData.getValue().toString();

        txxxFrameData = ID3v2TXXXFrameData.extract(
                txxx,
                id3v2.hasUnsynchronisation(),
                "LYRICS");

        if (txxxFrameData != null)
            return txxxFrameData.getValue().toString();

        return null;
    }

    private static Pattern ts = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern lb = Pattern.compile("[\\r\\n]+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public static String cleanLyrics(String s) {
        return lb.matcher(ts.matcher(s).replaceAll("")).replaceAll("\n");
    }
}
