package com.ilusons.harmony.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import java.io.File;

public class Music {

    // Logger TAG
    private static final String TAG = Music.class.getSimpleName();

    public static final String KEY_CACHE_DIR_COVER = "covers";

    public String Title;
    public String Artist;
    public String Album;
    public String Path;

    public String getText() {
        return TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title;
    }

    public static String getCover(Context context, Music data) {
        File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, data.Path);
        return file.getAbsolutePath();
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

                byte[] cover = tags.getAlbumImage();
                Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);

                if (bmp != null)
                    putCover(context, data, bmp);

            } else if (mp3file.hasId3v1Tag()) {
                ID3v1 tags = mp3file.getId3v1Tag();

                data.Title = tags.getTitle();
                data.Artist = tags.getArtist();
                data.Album = tags.getAlbum();
            } else {
                data.Title = file.getName().replaceFirst("[.][^.]+$", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "decode audio data tags", e);

            return null;
        }

        return data;
    }

}
