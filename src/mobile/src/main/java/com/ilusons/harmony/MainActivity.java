package com.ilusons.harmony;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.views.LibraryUIActivity;
import com.ilusons.harmony.views.PlaybackUIActivity;

public class MainActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Intent
        handleIntent(getIntent());

        // Kill self
        finish();

    }

    private void handleIntent(final Intent intent) {
        Log.d(TAG, "handleIntent\n" + intent);

        if (intent.getAction() == null) {
            openLibraryUIActivity(this);
            return;
        }

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            String scheme = intent.getScheme();

            if (scheme.equals(ContentResolver.SCHEME_FILE)
                    || scheme.equals(ContentResolver.SCHEME_CONTENT)) {

                final Uri uri = Uri.parse(StorageEx.getPath(MainActivity.this, intent.getData()));

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(MainActivity.this, MusicService.class);

                        i.setAction(MusicService.ACTION_OPEN);
                        i.putExtra(MusicService.KEY_URI, uri.toString());

                        startService(i);

                        if (SettingsActivity.getUIPlaybackAutoOpen(MainActivity.this))
                            openPlaybackUIActivity(MainActivity.this);
                    }
                }, 350);

            } else if (scheme.equals("http")) {
            } else if (scheme.equals("ftp")) {
            } else {
                openLibraryUIActivity(this);
                return;
            }

        }
    }

    public static synchronized Intent getLibraryUIActivityIntent(final Context context) {
        Intent intent = new Intent(context, LibraryUIActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    public static synchronized void openLibraryUIActivity(final Context context) {
        context.startActivity(getLibraryUIActivityIntent(context));
    }

    public static synchronized Intent getPlaybackUIActivityIntent(final Context context) {
        Intent intent = new Intent(context, PlaybackUIActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    public static synchronized void openPlaybackUIActivity(final Context context) {
        context.startActivity(getPlaybackUIActivityIntent(context));
    }

}
