package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseMediaBroadcastReceiver;
import com.ilusons.harmony.base.BasePlaybackUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.fx.DbmHandler;
import com.ilusons.harmony.fx.GLAudioVisualizationView;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.StorageEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;

public class PlaybackUIDarkActivity extends BasePlaybackUIActivity {

    // Logger TAG
    private static final String TAG = PlaybackUIDarkActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    private static final String FILE_PICK_EXTENSION = ".mp3";

    // Events
    private BaseMediaBroadcastReceiver mediaBroadcastReceiver = new BaseMediaBroadcastReceiver() {
        @Override
        public void open(String uri) {
            resetForUri(uri);
        }
    };

    // UI
    private LyricsViewFragment lyricsViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playback_ui_dark_activity);

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {

                getWindow().getDecorView().setBackground(
                        new BitmapDrawable(
                                getResources(),
                                Bitmap.createScaledBitmap(
                                        ((BitmapDrawable) ContextCompat.getDrawable(PlaybackUIDarkActivity.this, R.drawable.bg1)).getBitmap(),
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

        // Broadcast receiver
        mediaBroadcastReceiver.register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mediaBroadcastReceiver.unRegister();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult\nrequestCode=" + requestCode + "\nresultCode=" + resultCode + "\ndata=" + data);

        if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK) {
            Uri uri = Uri.parse(StorageEx.getPath(this, data.getData()));

            Intent i = new Intent(this, MusicService.class);

            i.setAction(MusicService.ACTION_OPEN);
            i.putExtra(MusicService.KEY_URI, uri.toString());

            startService(i);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
        super.OnMusicServiceChanged(className, musicService, isBound);

        final String item = musicService.getCurrentPlaylistItem();
        if (item == null || TextUtils.isEmpty(item))
            return;

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                resetForUri(item);
            }
        });
    }

    private void resetForUri(String uri) {
        Log.d(TAG, "resetForUri\n" + uri);

        try {
            Music music = Music.decodeFromFile(this, new File(uri));

            if (music != null) {

                Music.getCoverOrDownload(this, music, new JavaEx.ActionT<Bitmap>() {
                    @Override
                    public void execute(Bitmap bitmap) {
                        ((ImageView) findViewById(R.id.backdrop))
                                .setImageBitmap(bitmap);
                    }
                });

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

        getMusicService().add(uri.getPath());
        getMusicService().next();

        fx.linkTo(DbmHandler.Factory.newVisualizerHandler(this, getMusicService().getAudioSessionId()));

        getMusicService().start();
    }

    private Runnable progressHandlerRunnable;

    private void setupProgressHandler() {
        if (progressHandlerRunnable != null)
            handler.removeCallbacks(progressHandlerRunnable);

        final int dt = (int) (1000.0 / 24.0);

        final SeekBar seek_bar = (SeekBar) findViewById(R.id.seek_bar);

        seek_bar.setMax(getMusicService().getDuration());

        progressHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (getMusicService() != null && getMusicService().isPlaying()) {

                    seek_bar.setProgress(getMusicService().getPosition());

                    float v = (float) getMusicService().getPosition() / (float) getMusicService().getDuration();

                    if (lyricsViewFragment != null && lyricsViewFragment.isAdded())
                        lyricsViewFragment.updateScroll(v, getMusicService().getPosition());

                }

                handler.removeCallbacks(progressHandlerRunnable);
                handler.postDelayed(progressHandlerRunnable, dt);
            }
        };
        handler.postDelayed(progressHandlerRunnable, dt);

    }

}
