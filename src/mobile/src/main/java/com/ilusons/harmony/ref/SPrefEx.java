package com.ilusons.harmony.ref;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class SPrefEx {

    // Logger TAG
    private static final String TAG = SPrefEx.class.getSimpleName();

    public static String TAG_SPREF = "spref";
    public static String TAG_SPREF_FIRSTRUN = "first_run";

    public static SharedPreferences get(final Context context) {
        SharedPreferences spref = context.getSharedPreferences(TAG_SPREF, MODE_PRIVATE);
        return spref;
    }

    public static boolean getFirstRun(final Context context) {
        return get(context).getBoolean(TAG_SPREF_FIRSTRUN, false);
    }

    public static void setFirstRun(final Context context, boolean value) {
        SharedPreferences.Editor editor = get(context).edit();
        editor.putBoolean(TAG_SPREF_FIRSTRUN, value);
        editor.apply();
    }

}
