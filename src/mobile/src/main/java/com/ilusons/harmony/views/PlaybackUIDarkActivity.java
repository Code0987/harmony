package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.JavaEx;
import com.wang.avi.AVLoadingIndicatorView;

public class PlaybackUIDarkActivity extends BaseUIActivity {

    // Logger TAG
    private static final String TAG = PlaybackUIDarkActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    // UI
    private View root;

    private ImageButton play_pause;
    private ImageButton prev;
    private ImageButton next;
    private ImageButton random;
    private ImageButton stop;

    private LyricsViewFragment lyricsViewFragment;
    private AudioVFXViewFragment audioVFXViewFragment;

    private AVLoadingIndicatorView loadingView;

    private ImageView cover;

    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set view
        setContentView(R.layout.playback_ui_dark_activity);

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

        play_pause = (ImageButton) findViewById(R.id.play_pause);
        prev = (ImageButton) findViewById(R.id.prev);
        next = (ImageButton) findViewById(R.id.next);
        random = (ImageButton) findViewById(R.id.random);
        stop = (ImageButton) findViewById(R.id.stop);

        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null && getMusicService().isPlaying()) {
                    getMusicService().pause();
                } else {
                    getMusicService().play();
                }
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().prev();
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().next();
                }
            }
        });

        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().random();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().stop();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    public void onBackPressed() {
        if (SettingsActivity.getUIPlaybackAutoOpen(this))
            MainActivity.openLibraryUIActivity(this);

        super.onBackPressed();
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
                resetForUriIfNeeded(item);
            }
        });
    }

    @Override
    public void OnMusicServicePlay() {
        super.OnMusicServicePlay();

        play_pause.setImageDrawable(getDrawable(R.drawable.ic_pause_black));

        if (getMusicService() != null)
            resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());
    }

    @Override
    public void OnMusicServicePause() {
        super.OnMusicServicePlay();

        play_pause.setImageDrawable(getDrawable(R.drawable.ic_play_black));

        if (getMusicService() != null)
            resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());
    }

    @Override
    public void OnMusicServiceStop() {
        super.OnMusicServicePlay();

        play_pause.setImageDrawable(getDrawable(R.drawable.ic_play_black));
    }

    @Override
    public void OnMusicServiceOpen(String uri) {
        super.OnMusicServiceOpen(uri);

        resetForUriIfNeeded(uri);
    }

    private String currentUri;

    private void resetForUriIfNeeded(String uri) {
        Log.d(TAG, "resetForUri\n" + uri);

        if (currentUri != null && currentUri.equals(uri))
            return;

        currentUri = uri;

        loadingView.smoothToShow();

        try {
            final Music music = Music.load(this, uri);

            if (music != null) {

                loadingView.smoothToShow();

                Music.getCoverOrDownload(this, cover.getWidth(), music, new JavaEx.ActionT<Bitmap>() {
                    @Override
                    public void execute(Bitmap bitmap) {
                        try {
                            if (bitmap == null)
                                bitmap = CacheEx.getInstance().getBitmap(String.valueOf(R.drawable.logo));

                            if (bitmap == null) {
                                bitmap = ((BitmapDrawable) getDrawable(R.drawable.logo)).getBitmap();

                                CacheEx.getInstance().putBitmap(String.valueOf(R.drawable.logo), bitmap);
                            }
                        } catch (Exception e) {
                            // Eat!
                        }

                        loadingView.smoothToHide();

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

                        Palette palette = Palette.from(bitmap).generate();
                        int color = getApplicationContext().getColor(R.color.accent);
                        int colorBackup = color;
                        color = palette.getVibrantColor(color);
                        if (color == colorBackup)
                            color = palette.getDarkVibrantColor(color);
                        if (color == colorBackup)
                            color = palette.getDarkMutedColor(color);

                        float[] hsl = new float[3];
                        ColorUtils.colorToHSL(color, hsl);
                        hsl[2] = Math.max(hsl[2], hsl[2] + 0.30f); // lum +30%
                        int colorLight = ColorUtils.HSLToColor(hsl);

                        root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 160)));

                        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        seekBar.getThumb().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

                        play_pause.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        play_pause.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        prev.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        next.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        random.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        stop.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

//                        if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
//                            audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.AVFXType.Horizon, color);
//                        }

                        loadingView.smoothToHide();
                    }
                });

                loadingView.smoothToShow();

//                if (!isFinishing()) {
//                    audioVFXViewFragment = AudioVFXViewFragment.create();
//                    getFragmentManager()
//                            .beginTransaction()
//                            .replace(R.id.avfx_layout, audioVFXViewFragment)
//                            .commit();
//                }

                if (lyricsViewFragment != null && lyricsViewFragment.isAdded()) {
                    lyricsViewFragment.reset(music);
                } else if (!isFinishing()) {
                    lyricsViewFragment = LyricsViewFragment.create(music.Path);
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.lyrics_layout, lyricsViewFragment)
                            .commit();
                }

                if (getMusicService() != null)
                    seekBar.setMax(getMusicService().getDuration());

                setupProgressHandler();
            }

        } catch (Exception e) {
            Log.e(TAG, "open file", e);
        }

        if (getMusicService() != null) {
            if (getMusicService().isPlaying())
                OnMusicServicePlay();
            else
                OnMusicServicePause();
        }

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

                    if (lyricsViewFragment != null && lyricsViewFragment.isAdded())
                        lyricsViewFragment.updateScroll(getMusicService().getPosition());

                }

                handler.removeCallbacks(progressHandlerRunnable);
                handler.postDelayed(progressHandlerRunnable, dt);
            }
        };
        handler.postDelayed(progressHandlerRunnable, dt);

    }

}
