package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.JavaEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.UUID;

import co.mobiwise.materialintro.animation.MaterialIntroListener;
import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.view.MaterialIntroView;
import jonathanfinerty.once.Once;

public class PlaybackUIDarkActivity extends BaseUIActivity {

    // Logger TAG
    private static final String TAG = PlaybackUIDarkActivity.class.getSimpleName();

    // UI
    private View root;

    private ImageButton play_pause_stop;
    private ImageButton prev;
    private ImageButton next;
    private ImageButton shuffle;
    private ImageButton repeat;
    private ImageButton avfx;
    private ImageButton tune;

    private LyricsViewFragment lyricsViewFragment;
    private AudioVFXViewFragment audioVFXViewFragment;

    private AVLoadingIndicatorView loadingView;

    private ImageView cover;
    private VideoView video;

    private int color;
    private int colorLight;

    private SeekBar seekBar;

    private View controls_layout;
    private View lyrics_layout;

    private Runnable videoSyncTask = new Runnable() {
        @Override
        public void run() {

            if (video.getVisibility() == View.VISIBLE)
                video.seekTo(getMusicService().getPosition());

            handler.removeCallbacks(videoSyncTask);
            handler.postDelayed(videoSyncTask, 9 * 1000);
        }
    };

    private Runnable updateSystemUITask = new Runnable() {
        @Override
        public void run() {

            View v = getWindow().getDecorView();

            if ((v.getVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        }
    };

    private final Runnable hideUITask = new Runnable() {
        @Override
        public void run() {
            if (video.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getMusicService() != null && getMusicService().isPlaying()) {
                controls_layout.animate().alpha(0).setDuration(500).start();
                lyrics_layout.animate().alpha(0).setDuration(500).start();
                seekBar.animate().alpha(0).setDuration(500).start();
            }
        }
    };

    private final Runnable showUITask = new Runnable() {
        @Override
        public void run() {
            if (video.getVisibility() == View.VISIBLE) {
                controls_layout.animate().alpha(1).setDuration(500).start();
                lyrics_layout.animate().alpha(1).setDuration(500).start();
                seekBar.animate().alpha(1).setDuration(500).start();
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("color", color);
        outState.putInt("colorLight", colorLight);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // State
        if (savedInstanceState != null)
            try {

                color = savedInstanceState.getInt("color");
                colorLight = savedInstanceState.getInt("colorLight");

            } catch (Exception e) {
                Log.w(TAG, e);
            }

        // Set view
        setContentView(R.layout.playback_ui_dark_activity);

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                handler.postDelayed(updateSystemUITask, 500);
            }
        });

        // Set views
        root = findViewById(R.id.root);

        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loadingView);

        findViewById(R.id.av_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video.getVisibility() == View.VISIBLE) {
                    handler.removeCallbacks(showUITask);
                    handler.post(showUITask);
                    handler.removeCallbacks(hideUITask);
                    handler.postDelayed(hideUITask, 2500);
                }
            }
        });

        controls_layout = findViewById(R.id.controls_layout);
        lyrics_layout = findViewById(R.id.lyrics_layout);

        cover = (ImageView) findViewById(R.id.cover);
        video = (VideoView) findViewById(R.id.video);

        // Video, if loaded is on mute
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            video.setZOrderOnTop(false);
        } else {
            video.setZOrderOnTop(true);
        }
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setVolume(0, 0);

                if (getMusicService() != null)
                    getMusicService().play();

                handler.postDelayed(videoSyncTask, 1000);

                handler.postDelayed(hideUITask, 3500);
            }
        });
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                video.setVisibility(View.INVISIBLE);

                handler.removeCallbacks(videoSyncTask);

                handler.post(showUITask);
            }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                video.setVisibility(View.INVISIBLE);

                handler.removeCallbacks(videoSyncTask);

                handler.post(showUITask);

                return false;
            }
        });

        color = getApplicationContext().getColor(R.color.accent);
        colorLight = getApplicationContext().getColor(R.color.accent);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (!b) return;

                if (getMusicService() != null) {
                    getMusicService().seek(i);

                    if (video.getVisibility() == View.VISIBLE)
                        video.seekTo(getMusicService().getPosition());
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        play_pause_stop = (ImageButton) findViewById(R.id.play_pause_stop);
        prev = (ImageButton) findViewById(R.id.prev);
        next = (ImageButton) findViewById(R.id.next);
        shuffle = (ImageButton) findViewById(R.id.shuffle);
        repeat = (ImageButton) findViewById(R.id.repeat);
        avfx = (ImageButton) findViewById(R.id.avfx);
        tune = (ImageButton) findViewById(R.id.tune);

        play_pause_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null && getMusicService().isPlaying()) {
                    getMusicService().pause();
                } else {
                    getMusicService().play();
                }
            }
        });
        play_pause_stop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().stop();

                    info("Stopped!");
                }

                return true;
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
        prev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().random();
                }

                return true;
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
        next.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().random();
                }

                return true;
            }
        });

        if (MusicService.getPlayerShuffleMusicEnabled(PlaybackUIDarkActivity.this))
            shuffle.setAlpha(0.9f);
        else
            shuffle.setAlpha(0.3f);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    boolean value = MusicService.getPlayerShuffleMusicEnabled(PlaybackUIDarkActivity.this);

                    value = !value;

                    MusicService.setPlayerShuffleMusicEnabled(PlaybackUIDarkActivity.this, value);

                    if (value)
                        info("Shuffle turned ON");
                    else
                        info("Shuffle turned OFF");


                    if (value)
                        shuffle.setAlpha(0.9f);
                    else
                        shuffle.setAlpha(0.3f);
                }
            }
        });
        shuffle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().random();
                }

                return true;
            }
        });

        if (MusicService.getPlayerRepeatMusicEnabled(PlaybackUIDarkActivity.this))
            repeat.setAlpha(0.9f);
        else
            repeat.setAlpha(0.3f);
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    boolean value = MusicService.getPlayerRepeatMusicEnabled(PlaybackUIDarkActivity.this);

                    value = !value;

                    MusicService.setPlayerRepeatMusicEnabled(PlaybackUIDarkActivity.this, value);

                    if (value)
                        info("Repeat turned ON");
                    else
                        info("Repeat turned OFF");

                    if (value)
                        repeat.setAlpha(0.9f);
                    else
                        repeat.setAlpha(0.3f);
                }
            }
        });

        final View avfx_layout = findViewById(R.id.avfx_layout);
        avfx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioVFXViewFragment != null) {
                    getFragmentManager()
                            .beginTransaction()
                            .remove(audioVFXViewFragment)
                            .commit();

                    audioVFXViewFragment = null;

                    avfx_layout.setVisibility(View.INVISIBLE);

                } else if (!isFinishing() && audioVFXViewFragment == null) {
                    avfx_layout.setVisibility(View.VISIBLE);

                    audioVFXViewFragment = AudioVFXViewFragment.create();
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.avfx_layout, audioVFXViewFragment)
                            .commit();

                    audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
                }

                info("Long press to change style!");
            }
        });
        avfx.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
                    if (AudioVFXViewFragment.getAVFXType(getApplicationContext()) == AudioVFXViewFragment.AVFXType.Waveform) {
                        AudioVFXViewFragment.setAVFXType(getApplicationContext(), AudioVFXViewFragment.AVFXType.FFT);
                    } else if (AudioVFXViewFragment.getAVFXType(getApplicationContext()) == AudioVFXViewFragment.AVFXType.FFT) {
                        AudioVFXViewFragment.setAVFXType(getApplicationContext(), AudioVFXViewFragment.AVFXType.Waveform);
                    }

                    audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);

                    info("Now using " + AudioVFXViewFragment.getAVFXType(getApplicationContext()) + " fx!");
                }

                return true;
            }
        });

        tune.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlaybackUIDarkActivity.this, TuneActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        });

        // Guide
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                showGuide();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        handler.postDelayed(updateSystemUITask, 500);
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

        play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_pause_black));

        if (getMusicService() != null)
            resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());

        if (video.getVisibility() == View.VISIBLE)
            video.start();
    }

    @Override
    public void OnMusicServicePause() {
        super.OnMusicServicePlay();

        play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_play_black));

        if (getMusicService() != null)
            resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());

        if (video.getVisibility() == View.VISIBLE)
            video.pause();
    }

    @Override
    public void OnMusicServiceStop() {
        super.OnMusicServicePlay();

        play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_play_black));

        if (video.getVisibility() == View.VISIBLE) {
            video.stopPlayback();
            video.setVisibility(View.INVISIBLE);
        }
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

        if (video.getVisibility() == View.VISIBLE) {
            video.stopPlayback();
            video.setVisibility(View.INVISIBLE);
        }

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

                        // Refresh system bindings
                        Intent musicServiceIntent = new Intent(PlaybackUIDarkActivity.this, MusicService.class);
                        musicServiceIntent.setAction(MusicService.ACTION_REFRESH_SYSTEM_BINDINGS);
                        startService(musicServiceIntent);

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
                        color = getApplicationContext().getColor(R.color.accent);
                        int colorBackup = color;
                        color = palette.getVibrantColor(color);
                        if (color == colorBackup)
                            color = palette.getDarkVibrantColor(color);
                        if (color == colorBackup)
                            color = palette.getDarkMutedColor(color);

                        float[] hsl = new float[3];
                        ColorUtils.colorToHSL(color, hsl);
                        hsl[2] = Math.max(hsl[2], hsl[2] + 0.30f); // lum +30%
                        colorLight = ColorUtils.HSLToColor(hsl);

                        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        seekBar.getThumb().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

                        play_pause_stop.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        play_pause_stop.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                        prev.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        next.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        shuffle.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        repeat.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        avfx.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
                        tune.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

                        if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
                            audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
                        }

                        // Load video
                        if (music.hasVideo()) {
                            video.setVisibility(View.VISIBLE);
                            video.setVideoPath(music.Path);
                            video.requestFocus();
                            video.start();
                        }

                        if (music.hasVideo() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            root.setBackground(null);
                        } else {
                            root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 160)));
                        }

                        loadingView.smoothToHide();
                    }
                });

                loadingView.smoothToShow();

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

    private void showGuide() {
        final String tag_guide = TAG + ".guide";

        if (Once.beenDone(Once.THIS_APP_INSTALL, tag_guide))
            return;

        final MaterialIntroView.Builder guide_play_pause_stop = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("Press to Play/Pause, and long press to Stop current item.")
                .setTarget(play_pause_stop)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_next = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("Press to skip to next item in playlist. Long press to skip to random item.")
                .setTarget(next)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_avfx = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("Press to enable visualizations. Long press to cycle between various styles.")
                .setTarget(avfx)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_tune = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("Press to open Tune view. You can fine tune your sound here!")
                .setTarget(tune)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_lyrics = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("This is lyrics view. Turn on your internet for automatic lyrics. Long press to open editor.")
                .setTarget(lyrics_layout)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_cover = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("Cover art, video, visualizations will be here (in that order)!")
                .setTarget(cover)
                .setUsageId(UUID.randomUUID().toString());

        final MaterialIntroView.Builder guide_final = new MaterialIntroView.Builder(this)
                .setMaskColor(getColor(R.color.translucent_accent))
                .setDelayMillis(500)
                .enableFadeAnimation(true)
                .enableDotAnimation(false)
                .setFocusType(Focus.NORMAL)
                .setFocusGravity(FocusGravity.CENTER)
                .setTargetPadding(32)
                .dismissOnTouch(true)
                .enableIcon(true)
                .performClick(true)
                .setInfoText("That's all! Now go play something!")
                .setTarget(play_pause_stop)
                .setUsageId(UUID.randomUUID().toString());

        guide_final.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                Once.markDone(tag_guide);
            }
        });
        guide_cover.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_final.show();
            }
        });
        guide_lyrics.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_cover.show();
            }
        });
        guide_tune.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_lyrics.show();
            }
        });
        guide_avfx.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_tune.show();
            }
        });
        guide_next.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_avfx.show();
            }
        });
        guide_play_pause_stop.setListener(new MaterialIntroListener() {
            @Override
            public void onUserClicked(String usageId) {
                guide_next.show();
            }
        });
        guide_play_pause_stop.show();

    }

}
