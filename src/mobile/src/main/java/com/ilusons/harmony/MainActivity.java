package com.ilusons.harmony;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.fx.DbmHandler;
import com.ilusons.harmony.fx.GLAudioVisualizationView;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.views.LyricsViewFragment;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;

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

    PowerManager.WakeLock wakeLockForScreenOn;

    // Events
    private Handler handler = new Handler();

    // UI
    private LyricsViewFragment lyricsViewFragment;

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
                        info("Please grant all the required permissions :(");

                        finish();
                    }
                });

        wakeLockForScreenOn = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, getClass().getName());

        super.onCreate(savedInstanceState);

        // Start service
        startService(new Intent(this, MusicService.class));

        setContentView(R.layout.activity_main);

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {

                getWindow().getDecorView().setBackground(
                        new BitmapDrawable(
                                getResources(),
                                Bitmap.createScaledBitmap(
                                        ((BitmapDrawable) ContextCompat.getDrawable(MainActivity.this, R.drawable.bg1)).getBitmap(),
                                        getWindow().getDecorView().getWidth(),
                                        getWindow().getDecorView().getHeight(),
                                        false)));

            }
        });

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setType("audio/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_FILE_PICK);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

        wakeLockForScreenOn.acquire();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

        wakeLockForScreenOn.release();
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
            Music music = Music.decodeFromFile(this, new File(uri.getPath()));

            if (music != null) {

                Picasso.with(this)
                        .load(new File(Music.getCover(this, music)))
                        .fit()
                        .centerCrop()
                        .into((ImageView) findViewById(R.id.backdrop));

                musicService.add(uri.getPath());
                musicService.next();
                musicService.start();

                ((AVLoadingIndicatorView) findViewById(R.id.loading_view)).show();

                if (lyricsViewFragment != null && lyricsViewFragment.isAdded()) {
                    getFragmentManager()
                            .beginTransaction()
                            .remove(lyricsViewFragment)
                            .commit();
                }

                lyricsViewFragment = LyricsViewFragment.create(music.Title, music.Artist, music.Lyrics);

                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.lyrics_container, lyricsViewFragment)
                        .commit();

                setupProgressHandler();

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

    private Runnable progressHandlerRunnable;

    private void setupProgressHandler() {
        if (progressHandlerRunnable != null)
            handler.removeCallbacks(progressHandlerRunnable);

        final int dt = (int) (1000.0 / 24.0);

        final MediaPlayer mp = musicService.getMediaPlayer();

        final SeekBar seek_bar = (SeekBar) findViewById(R.id.seek_bar);

        seek_bar.setMax(mp.getDuration());

        progressHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mp != null && mp.isPlaying()) {

                    seek_bar.setProgress(mp.getCurrentPosition());

                    float v = (float) mp.getCurrentPosition() / (float) mp.getDuration();

                    if (lyricsViewFragment != null && lyricsViewFragment.isAdded())
                        lyricsViewFragment.updateScroll(v, mp.getCurrentPosition());

                }

                handler.removeCallbacks(progressHandlerRunnable);
                handler.postDelayed(progressHandlerRunnable, dt);
            }
        };
        handler.postDelayed(progressHandlerRunnable, dt);

    }

    public void info(String s) {
        View view = findViewById(R.id.root);

        if (view != null) {
            final Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            if (snackbarView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
                p.setMargins(p.leftMargin,
                        p.topMargin,
                        p.rightMargin,
                        p.bottomMargin);
                snackbarView.requestLayout();
            }
            snackbar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        } else {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }

}
