package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.audiofx.Visualizer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BasePlaybackUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.VisualizerEx;
import com.wang.avi.AVLoadingIndicatorView;
import com.yalantis.waves.util.Horizon;

import java.util.ArrayList;

public class PlaybackUIDarkActivity extends BasePlaybackUIActivity {

    // Logger TAG
    private static final String TAG = PlaybackUIDarkActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    // UI
    private View root;

    private FloatingActionButton fab;
    private FloatingActionButton fab_prev;
    private FloatingActionButton fab_next;
    private FloatingActionButton fab_random;
    private FloatingActionButton fab_stop;

    private LyricsViewFragment lyricsViewFragment;

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

        // Set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setCollapsible(false);

        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
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
        fab_prev = (FloatingActionButton) findViewById(R.id.fab_prev);
        fab_next = (FloatingActionButton) findViewById(R.id.fab_next);
        fab_random = (FloatingActionButton) findViewById(R.id.fab_random);
        fab_stop = (FloatingActionButton) findViewById(R.id.fab_stop);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null && getMusicService().isPlaying()) {
                    getMusicService().pause();
                } else {
                    getMusicService().play();
                }
            }
        });

        fab_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().prev();
                }
            }
        });

        fab_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().next();
                }
            }
        });

        fab_random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getMusicService() != null) {
                    getMusicService().random();
                }
            }
        });

        fab_stop.setOnClickListener(new View.OnClickListener() {
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

        fab.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause));

        resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());
    }

    @Override
    public void OnMusicServicePause() {
        super.OnMusicServicePlay();

        fab.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));

        resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());
    }

    @Override
    public void OnMusicServiceStop() {
        super.OnMusicServicePlay();

        fab.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));

        resetForUriIfNeeded(getMusicService().getCurrentPlaylistItem());
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

        loadingView.show();

        try {
            Music music = Music.load(this, uri);

            if (music != null) {

                loadingView.show();

                Music.getCoverOrDownload(this, music, new JavaEx.ActionT<Bitmap>() {
                    @Override
                    public void execute(Bitmap bitmap) {
                        if (bitmap == null)
                            bitmap = ((BitmapDrawable) getDrawable(R.drawable.logo)).getBitmap();

                        loadingView.hide();

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

                        root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 127)));

                        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);

                        fab.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        fab_prev.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        fab_next.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        fab_random.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        fab_stop.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

                        // TODO: setupFXHorizon(color);
                        setupFXView(color);

                        loadingView.hide();
                    }
                });

                loadingView.show();

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

        if (getMusicService().isPlaying())
            OnMusicServicePlay();
        else
            OnMusicServicePause();

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

    private Horizon horizon;
    private VisualizerEx horizonVisualizerEx;
    private GLSurfaceView horizonView;

    private void setupFXHorizon(int color) {
        RelativeLayout parent = (RelativeLayout) findViewById(R.id.fxContainer);

        if (horizonView != null)
            parent.removeView(horizonView);

        horizonView = new GLSurfaceView(this);

        horizonView.setZOrderOnTop(true);
        horizonView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        horizonView.getHolder().setFormat(PixelFormat.RGBA_8888);
        horizonView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        parent.addView(horizonView, 0);

        horizon = new Horizon(
                horizonView,
                Color.argb(255, 0, 0, 0),
                44100,
                1,
                16);
        horizon.setMaxVolumeDb(120);

        try {
            horizonVisualizerEx = new VisualizerEx(PlaybackUIDarkActivity.this, getMusicService().getAudioSessionId(), new VisualizerEx.OnFftDataCaptureListener() {
                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    horizon.updateView(fft);
                }
            });
            horizonVisualizerEx.setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private FXView fxView;

    private void setupFXView(int color) {
        RelativeLayout parent = (RelativeLayout) findViewById(R.id.fxContainer);

        if (fxView != null)
            parent.removeView(fxView);

        fxView = new FXView(this, parent.getWidth(), parent.getHeight(), color);

        parent.addView(fxView, 0);
    }

    public class FXView extends SurfaceView implements SurfaceHolder.Callback {

        private final String TAG = FXView.class.getName();

        Context context;

        int color;

        private SurfaceHolder holder;
        private UpdaterThread thread;

        VisualizerEx visualizerEx;
        private float[] dbs;
        private float[] amps;
        private int sr;

        private int height;
        private int width;

        private ArrayList<Particle> particles = new ArrayList<>();

        private Paint paint = new Paint();
        private Path path = new Path();

        public FXView(Context context, int width, int height, int color) {
            super(context);

            this.context = context;

            this.color = color;

            holder = getHolder();
            holder.addCallback(this);
            holder.setFormat(PixelFormat.TRANSPARENT);

            setZOrderOnTop(true);

            this.width = width;
            this.height = height;

            setWillNotDraw(false);

            try {
                visualizerEx = new VisualizerEx(PlaybackUIDarkActivity.this, getMusicService().getAudioSessionId(), new VisualizerEx.OnFftDataCaptureListener() {
                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                        int dataSize = fft.length / 2 - 1;

                        sr = samplingRate;

                        if (dbs == null || dbs.length != dataSize) {
                            dbs = new float[dataSize];
                        }
                        if (amps == null || amps.length != dataSize) {
                            amps = new float[dataSize];
                        }

                        for (int i = 0; i < dataSize; i++) {
                            float re = fft[2 * i];
                            float im = fft[2 * i + 1];
                            float sq = re * re + im * im;

                            dbs[i] = (float) (sq > 0 ? 20 * Math.log10(sq) : 0);

                            float k = 1;
                            if (i == 0 || i == dataSize - 1) {
                                k = 2;
                            }

                            amps[i] = (float) (k * Math.sqrt(sq) / dataSize);
                        }
                    }
                });
                visualizerEx.setEnabled(true);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (thread == null) {
                thread = new UpdaterThread(holder);
                thread.setRunning(true);
                thread.start();
            }

            this.height = height;
            this.width = width;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            thread.setRunning(false);
            while (retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    Log.d(getClass().getSimpleName(), "Interrupted Exception", e);
                }
            }

            if (visualizerEx != null) {
                visualizerEx.release();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Canvas c = canvas;

            // c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY);

            for (int i = 0; i < particles.size(); i++) {

                Particle p = particles.get(i);

                p.draw(c);

                for (int j = particles.size() - 1; j > i; j--) {
                    float d = (float) Math.sqrt(
                            Math.pow(particles.get(i).X - particles.get(j).X, 2)
                                    +
                                    Math.pow(particles.get(i).Y - particles.get(j).Y, 2)
                    );

                    if (d > p.Proximity)
                        continue;

                    paint.reset();
                    paint.setAntiAlias(true);
                    paint.setColor(p.C);
                    paint.setAlpha((int) (((p.Proximity - d) / p.Proximity) * 255));
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setStrokeWidth(1f);

                    path.reset();
                    path.moveTo(particles.get(i).X, particles.get(i).Y);
                    path.lineTo(particles.get(j).X, particles.get(j).Y);
                    path.close();

                    c.drawPath(path, paint);
                }
            }

        }

        public class UpdaterThread extends Thread {
            private final SurfaceHolder holder;

            private boolean running = false;

            public UpdaterThread(SurfaceHolder holder) {
                this.holder = holder;
            }

            @Override
            public void run() {
                Canvas canvas = null;

                while (running) {

                    if (particles.size() < 25) {
                        while (particles.size() < 25)
                            particles.add(new Particle(color));
                    }

                    for (int i = 0; i < particles.size(); i++) {

                        Particle p = particles.get(i);

                        p.update();
                    }

                    try {
                        canvas = holder.lockCanvas(null);
                        synchronized (holder) {
                            postInvalidate();
                        }
                    } finally {
                        if (canvas != null) {
                            holder.unlockCanvasAndPost(canvas);
                        }
                    }
                }

            }

            public void setRunning(boolean b) {
                running = b;
            }
        }

        class Particle {
            public float R = 1.0f;
            public float X;
            public float Y;
            public int C;
            public float Vx;
            public float Vy;
            public float Proximity;

            public Particle(int color) {
                X = (float) Math.random() * width;
                Y = (float) Math.random() * height;
                C = color;
                Vx = ((float) Math.random() - 0.5f) * 5;
                Vy = ((float) Math.random() - 0.5f) * 5;
                Proximity = ((float) Math.random() * 0.22f * (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)));
            }

            public void update() {
                if (X > width + 20 || X < -20) {
                    Vx = -Vx;
                }
                if (Y > height + 20 || Y < -20) {
                    Vy = -Vy;
                }

                float db = 0;
                float amp = 0;
                int k = 0;

                float y = 0;

                if (dbs != null && amps != null) {
                    k = (int) ((X / width) * dbs.length);
                    k = Math.abs(k);
                    k = Math.min(dbs.length - 1, Math.max(0, k));

                    db = dbs[k];
                    amp = amps[k];

                    y = amp * -k * 100;

                    // Log.d(TAG, "fft = " + db + "db, " + amp + "amp" + ". k=" + k + ", y=" + y);
                }

                if (Math.abs(y) < 10) {
                    // X += Vx;
                    Y += Vy;
                } else {
                    // X += Vx;
                    Y = height / 2 - R + y + 50;
                }

            }

            public void draw(Canvas c) {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(C);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(3f);

                c.drawCircle(X + R, Y + R, R, paint);
            }
        }

    }

}
