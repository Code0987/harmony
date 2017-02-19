package com.ilusons.harmony;

import android.media.MediaPlayer;

public class Global {

    // Logger TAG
    private static final String TAG = Global.class.getSimpleName();

    private static Global instance;

    public static Global getInstance() {
        if (instance == null)
            instance = new Global();
        return instance;
    }

}
