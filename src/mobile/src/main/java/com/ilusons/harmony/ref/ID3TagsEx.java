package com.ilusons.harmony.ref;

import android.text.TextUtils;
import android.util.Log;

import com.mpatric.mp3agic.AbstractID3v2Tag;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v2Frame;
import com.mpatric.mp3agic.ID3v2FrameSet;
import com.mpatric.mp3agic.ID3v2PictureFrameData;
import com.mpatric.mp3agic.ID3v2TXXXFrameData;
import com.mpatric.mp3agic.InvalidDataException;

import java.util.Arrays;
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

    public static String cleanLyrics(String s) {
        return ts.matcher(s).replaceAll("");
    }

    public static byte[] getCover(ID3v2 id3v2) { // TODO: Fix this
        Map<String, ID3v2FrameSet> frameSets = id3v2.getFrameSets();

        for(Map.Entry<String, ID3v2FrameSet> frameSet : frameSets.entrySet()) {
            Log.w(TAG, "" + frameSet.getKey() + " --- " + frameSet.getValue());
            for (ID3v2Frame frame : frameSet.getValue().getFrames())
                Log.w(TAG, " --> " + Arrays.toString(frame.getData()));
        }

        byte[] result = id3v2.getAlbumImage();

        if (result != null && result.length > 0)
            return result;

        ID3v2FrameSet frameSet = frameSets.get(AbstractID3v2Tag.ID_IMAGE);
        if (frameSet != null) {
            ID3v2Frame frame = frameSet.getFrames().get(0);
            ID3v2PictureFrameData frameData;
            try {
                frameData = new ID3v2PictureFrameData(id3v2.hasUnsynchronisation(), frame.getData());

                return frameData.getImageData();
            } catch (InvalidDataException e) {
                // do nothing
            }
        }

        return null;
    }

}
