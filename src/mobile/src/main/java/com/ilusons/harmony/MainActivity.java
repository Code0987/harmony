package com.ilusons.harmony;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.ilusons.harmony.fx.DbmHandler;
import com.ilusons.harmony.fx.GLAudioVisualizationView;
import com.ilusons.harmony.ref.ID3TagsEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.views.LyricsViewFragment;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

public class MainActivity extends Activity {

    // Logger TAG
    private static final String TAG = MainActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    private static final String FILE_PICK_EXTENSION = ".mp3";

    // Services
    MusicService musicService;
    boolean isMusicServiceBound = false;
    ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.ServiceBinder binder = (MusicService.ServiceBinder) service;
            musicService = binder.getService();
            isMusicServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isMusicServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check if all permissions re granted
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this,
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        info("All needed permissions have been granted :)");
                    }

                    @Override
                    public void onDenied(String permission) {
                        info("Please grant all the required permissions :)");

                        finish();
                    }
                });

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent();
                i.setType("audio/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_FILE_PICK);

                // openFile(Uri.parse("/storage/9016-4EF8/Music/f2k_ows/Amberian Dawn - Magic Forest [Magic Forest].mp3"));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            openFile(Uri.parse(StorageEx.getPath(this, uri)));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    private void parseAction() {
        Intent intent = getIntent();

        Log.d(TAG, "intent:" + intent);

        if (intent.getAction().compareTo(Intent.ACTION_VIEW) == 0) {
            String scheme = intent.getScheme();

            if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                Uri uri = intent.getData();

            } else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                Uri uri = intent.getData();

                openFile(uri);

            } else if (scheme.compareTo("http") == 0) {
            } else if (scheme.compareTo("ftp") == 0) {
            }
        }
    }

    private void openFile(Uri uri) {
        try {
            Mp3File mp3file = new Mp3File(uri.getPath());
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();

                byte[] imageData = id3v2Tag.getAlbumImage();
                if (imageData != null) {
                    String mimeType = id3v2Tag.getAlbumImageMimeType();

                    Bitmap bmp = ImageEx.decodeBitmap(imageData,
                            getWindow().getDecorView().getWidth(),
                            getWindow().getDecorView().getHeight());

                    //if (bmp != null)
                    //    startFX(uri, bmp);
                }

                String lyrics = ID3TagsEx.getLyrics(id3v2Tag);
                if (!TextUtils.isEmpty(lyrics))
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.lyrics_container, LyricsViewFragment.create(lyrics, id3v2Tag.getLength()))
                            .commit();
            }
        } catch (Exception e) {
            Log.e(TAG, "open file", e);
        }
    }

    private void startFX(Uri uri, Bitmap bmp) {
        GLAudioVisualizationView fx = new GLAudioVisualizationView.Builder(this)
                .setBubblesSize(R.dimen.bubble_size)
                .setBubblesRandomizeSize(true)
                .setWavesHeight(R.dimen.wave_height)
                .setWavesFooterHeight(R.dimen.footer_height)
                .setWavesCount(7)
                .setLayersCount(4)
                .setBackgroundColorRes(R.color.av_color_bg)
                .setLayerColors(R.array.av_colors)
                .setBubblesPerLayer(16)
                .build();

        addContentView(fx, new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT));

        musicService.add(uri.getPath());
        musicService.next();

        fx.linkTo(DbmHandler.Factory.newVisualizerHandler(this, musicService.getMediaPlayer().getAudioSessionId()));

        musicService.start();
    }

    public void info(String s) {
        View view = findViewById(R.id.coordinatorLayout);

        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();
            if (snackbarView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
                p.setMargins(p.leftMargin,
                        p.topMargin,
                        p.rightMargin,
                        p.bottomMargin);
                snackbarView.requestLayout();
            }
            snackbar.show();
        } else {
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        }
    }


}
