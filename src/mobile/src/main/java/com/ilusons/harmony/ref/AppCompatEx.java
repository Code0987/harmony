package com.ilusons.harmony.ref;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

public class AppCompatEx {

    public static String TAG_SPREF_UI_MODE = "ui_mode";

    public static int getNightMode(AppCompatActivity context) {
        int mode = SPrefEx.get(context).getInt(TAG_SPREF_UI_MODE, AppCompatDelegate.getDefaultNightMode());

        return mode;
    }

    public static void setNightMode(AppCompatActivity context) {
        SPrefEx.get(context).edit().putInt(TAG_SPREF_UI_MODE, AppCompatDelegate.getDefaultNightMode()).apply();
    }

    public static boolean isNightMode(AppCompatActivity context) {
        return getNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void updateNightMode(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        if (mode == AppCompatDelegate.MODE_NIGHT_AUTO)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static int getLocalNightMode(AppCompatActivity context) {
        String tag = TAG_SPREF_UI_MODE + "_" + context.getClass().getSimpleName();
        int mode = SPrefEx.get(context).getInt(tag, AppCompatDelegate.getDefaultNightMode());
        return mode;
    }

    public static void setLocalNightMode(AppCompatActivity context) {
        String tag = TAG_SPREF_UI_MODE + "_" + context.getClass().getSimpleName();
        SPrefEx.get(context).edit().putInt(tag, AppCompatDelegate.getDefaultNightMode()).apply();
    }

    public static boolean isLocalNightMode(AppCompatActivity context) {
        return getLocalNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void updateLocalNightMode(AppCompatActivity context, int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            context.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            context.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        if (mode == AppCompatDelegate.MODE_NIGHT_AUTO)
            context.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            context.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static int toggleNightMode(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO || mode == AppCompatDelegate.MODE_NIGHT_AUTO || mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            return AppCompatDelegate.MODE_NIGHT_YES;
        if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            return AppCompatDelegate.MODE_NIGHT_NO;
        return AppCompatDelegate.getDefaultNightMode();
    }

    public static int toggleNightMode() {
        return toggleNightMode(AppCompatDelegate.getDefaultNightMode());
    }

    public static void applyNightMode(AppCompatActivity context, int mode) {
        updateNightMode(mode);
        context.recreate();
    }

    public static void applyLocalNightMode(AppCompatActivity context, int mode) {
        updateLocalNightMode(context, mode);
        context.recreate();
    }

}
