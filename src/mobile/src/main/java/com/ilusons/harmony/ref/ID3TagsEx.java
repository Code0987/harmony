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
            return cleanLyrics(txxxFrameData.getValue().toString());

        return null;
    }

    public static String cleanLyrics(String s) {
        Pattern p = Pattern.compile(
                "\\[(.*?)\\]",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        return p.matcher(s).replaceAll("");
    }
}
