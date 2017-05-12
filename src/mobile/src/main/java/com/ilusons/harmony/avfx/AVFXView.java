package com.ilusons.harmony.avfx;


import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.media.audiofx.Visualizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ilusons.harmony.ref.VisualizerEx;

import java.util.ArrayList;

public class AVFXView extends SurfaceView implements SurfaceHolder.Callback {

    private final String TAG = AVFXView.class.getName();

    private Context context;

    private int color;

    private SurfaceHolder holder;
    private UpdaterThread thread;

    VisualizerEx visualizerEx;
    private float[] dbs;
    private float[] amps;
    private int sr;

    private int height;
    private int width;

    private ArrayList<Particle> particles = new ArrayList<>();

    public AVFXView(Context context, int width, int height, int color) {
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

    private Paint paint = new Paint();
    private Path path = new Path();
    private BlurMaskFilter blurFilterDots = new BlurMaskFilter(0.3f, BlurMaskFilter.Blur.NORMAL);
    private BlurMaskFilter blurFilterLines = new BlurMaskFilter(1.3f, BlurMaskFilter.Blur.NORMAL);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Canvas c = canvas;

        // c.drawColor(color.TRANSPARENT, PorterDuff.Mode.OVERLAY);

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
                paint.setColor(color);
                paint.setAlpha((int) (((p.Proximity - d) / p.Proximity) * 255));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(1f);
                paint.setMaskFilter(blurFilterLines);

                path.reset();
                path.moveTo(particles.get(i).X, particles.get(i).Y);
                path.lineTo(particles.get(j).X, particles.get(j).Y);
                path.close();

                c.drawPath(path, paint);
            }
        }

    }

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
                        particles.add(new Particle());
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
        public float Vx;
        public float Vy;
        public float Proximity;

        public Particle() {
            X = (float) Math.random() * width;
            Y = (float) Math.random() * height;
            Vx = ((float) Math.random() - 0.5f) * 5;
            Vy = ((float) Math.random() - 0.5f) * 5;
            Proximity = ((float) Math.random() * 0.24f * (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)));
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
                Y += (-Y + (height / 2 - R + y + 50));
            }

        }

        public void draw(Canvas c) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(3f);
            paint.setMaskFilter(blurFilterDots);

            c.drawCircle(X + R, Y + R, R, paint);
        }
    }

}
