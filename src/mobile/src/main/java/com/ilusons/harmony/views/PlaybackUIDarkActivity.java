package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
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

    // Events
    private BaseMediaBroadcastReceiver mediaBroadcastReceiver = new BaseMediaBroadcastReceiver() {
        @Override
        public void open(String uri) {
            resetForUri(uri);
        }
    };

    // UI
    private View root;

    private FloatingActionButton fab;

    private LyricsViewFragment lyricsViewFragment;

    private AVLoadingIndicatorView loadingView;

    private ImageView cover;

    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view
        setContentView(R.layout.playback_ui_dark_activity);

        // Set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setCollapsible(false);

        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setElevation(0);

        // Set views
        root = findViewById(R.id.root);

        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loadingView);

        cover = (ImageView) findViewById(R.id.cover);

        seekBar = (SeekBar) findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (!b) return;

                if (getMusicService() != null) {
                    getMusicService().seek(i);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mediaBroadcastReceiver.unRegister();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
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

        loadingView.show();

        try {
            Music music = Music.decodeFromFile(this, new File(uri));

            if (music != null) {

                Music.getCoverOrDownload(this, music, new JavaEx.ActionT<Bitmap>() {
                    @Override
                    public void execute(Bitmap bitmap) {
                        if (bitmap == null)
                            bitmap = ((BitmapDrawable) getDrawable(R.drawable.logo)).getBitmap();

                        if (bitmap == null)
                            return;

                        // Load cover
                        if (cover.getDrawable() != null) {
                            TransitionDrawable d = new TransitionDrawable(new Drawable[]{
                                    cover.getDrawable(),
                                    new BitmapDrawable(getResources(), bitmap)
                            });

                            cover.setImageDrawable(d);

                            d.setCrossFadeEnabled(true);
                            d.startTransition(200);
                        } else {
                            cover.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        }

                        int color = getApplicationContext().getColor(R.color.accent);
                        color = Palette.from(bitmap).generate().getVibrantColor(color);
                        root.setBackground(new ColorDrawable(color));

                        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);

                        fab.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                });

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

                seekBar.setMax(getMusicService().getDuration());

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

        progressHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (getMusicService() != null && getMusicService().isPlaying()) {

                    seekBar.setProgress(getMusicService().getPosition());

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
