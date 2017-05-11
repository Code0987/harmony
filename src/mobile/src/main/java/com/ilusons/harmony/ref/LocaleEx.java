package com.ilusons.harmony.ref;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleEx {

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getCurrent(Context context) {
        Resources resources = context.getResources();
        Locale locale =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        ? resources.getConfiguration().getLocales().getFirstMatch(resources.getAssets().getLocales())
                        : resources.getConfiguration().locale;
        return locale;
    }

}
