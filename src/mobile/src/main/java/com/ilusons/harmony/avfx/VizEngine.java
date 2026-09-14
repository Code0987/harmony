package com.ilusons.harmony.avfx;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

/**
 * Draws VFX onto a Bitmap off the UI thread, then swaps it onto an ImageView.
 * The player view hierarchy never contains a custom animated View.
 */
public final class VizEngine {

	private static final String TAG = "HarmonyViz";
	private static final int FRAME_MS = 100;
	private static final int MAX_EDGE = 512;

	public enum Mode {
		RINGS, WAVE, BARS, DOTS
	}

	private final Handler main = new Handler(Looper.getMainLooper());
	private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

	private HandlerThread worker;
	private Handler workerHandler;
	private ImageView target;
	private Bitmap bitmap;
	private Canvas bitmapCanvas;
	private Mode mode = Mode.RINGS;
	private int color = 0xFF80CBC4;
	private float t;
	private boolean running;

	private final Runnable frame = new Runnable() {
		@Override
		public void run() {
			if (!running) {
				return;
			}
			try {
				drawFrame();
			} catch (Throwable e) {
				Log.e(TAG, "drawFrame", e);
			}
			if (running && workerHandler != null) {
				workerHandler.postDelayed(this, FRAME_MS);
			}
		}
	};

	public VizEngine() {
		stroke.setStyle(Paint.Style.STROKE);
		stroke.setStrokeCap(Paint.Cap.ROUND);
		fill.setStyle(Paint.Style.FILL);
	}

	public void attach(ImageView target) {
		this.target = target;
	}

	public void setMode(Mode mode) {
		this.mode = mode == null ? Mode.RINGS : mode;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void start() {
		Log.i(TAG, "start");
		if (running) {
			return;
		}
		running = true;
		if (worker == null) {
			worker = new HandlerThread("harmony-viz");
			worker.start();
			workerHandler = new Handler(worker.getLooper());
		}
		workerHandler.removeCallbacks(frame);
		workerHandler.post(frame);
	}

	public void stop() {
		Log.i(TAG, "stop");
		running = false;
		if (workerHandler != null) {
			workerHandler.removeCallbacks(frame);
		}
	}

	public void release() {
		stop();
		if (worker != null) {
			worker.quitSafely();
			worker = null;
			workerHandler = null;
		}
		bitmap = null;
		bitmapCanvas = null;
		target = null;
	}

	private void drawFrame() {
		final ImageView view = target;
		if (view == null) {
			return;
		}
		int vw = view.getWidth();
		int vh = view.getHeight();
		if (vw < 8 || vh < 8) {
			Log.i(TAG, "skip frame, view size " + vw + "x" + vh);
			return;
		}
		int w = Math.min(MAX_EDGE, vw);
		int h = Math.min(MAX_EDGE, vh);
		if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h) {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			bitmapCanvas = new Canvas(bitmap);
		}
		t += 0.12f;
		bitmap.eraseColor(0x00000000);
		stroke.setColor(color);
		fill.setColor(color);
		float bass = 0.35f + 0.65f * absSin(t * 1.3f);
		float mid = 0.25f + 0.75f * absSin(t * 2.6f + 1.1f);
		float high = 0.20f + 0.80f * absSin(t * 4.4f + 2.2f);
		switch (mode) {
			case WAVE:
				drawWave(bitmapCanvas, w, h);
				break;
			case BARS:
				drawBars(bitmapCanvas, w, h, bass, mid, high);
				break;
			case DOTS:
				drawDots(bitmapCanvas, w, h);
				break;
			case RINGS:
			default:
				drawRings(bitmapCanvas, w, h, bass, mid, high);
				break;
		}
		final Bitmap frameBmp = bitmap;
		main.post(new Runnable() {
			@Override
			public void run() {
				if (running && target != null) {
					target.setImageBitmap(frameBmp);
				}
			}
		});
	}

	private void drawRings(Canvas c, int w, int h, float bass, float mid, float high) {
		float cx = w / 2f;
		float cy = h / 2f;
		float max = Math.min(w, h) * 0.42f;
		stroke.setStrokeWidth(6f);
		c.drawCircle(cx, cy, max * (0.22f + 0.18f * bass), stroke);
		stroke.setStrokeWidth(4f);
		c.drawCircle(cx, cy, max * (0.42f + 0.16f * mid), stroke);
		stroke.setStrokeWidth(3f);
		c.drawCircle(cx, cy, max * (0.62f + 0.14f * high), stroke);
	}

	private void drawWave(Canvas c, int w, int h) {
		stroke.setStrokeWidth(5f);
		float cy = h / 2f;
		float amp = h * 0.22f;
		float prevX = 0f;
		float prevY = cy;
		int n = 32;
		for (int i = 0; i < n; i++) {
			float x = w * (i / (float) (n - 1));
			float y = cy + amp * (0.55f * (float) Math.sin(i * 0.4f + t)
					+ 0.30f * (float) Math.sin(i * 0.9f + t * 1.7f));
			if (i > 0) {
				c.drawLine(prevX, prevY, x, y, stroke);
			}
			prevX = x;
			prevY = y;
		}
	}

	private void drawBars(Canvas c, int w, int h, float bass, float mid, float high) {
		int n = 20;
		float gap = w / (float) n;
		stroke.setStrokeWidth(Math.max(4f, gap * 0.4f));
		float cy = h / 2f;
		float max = h * 0.3f;
		for (int i = 0; i < n; i++) {
			float pos = i / (float) (n - 1);
			float band = pos < 0.33f ? bass : (pos < 0.66f ? mid : high);
			float half = max * (0.25f + 0.75f * band * absSin(t + i * 0.35f));
			float x = gap * (i + 0.5f);
			c.drawLine(x, cy - half, x, cy + half, stroke);
		}
	}

	private void drawDots(Canvas c, int w, int h) {
		int n = 12;
		float gap = w / (float) n;
		float cy = h / 2f;
		for (int i = 0; i < n; i++) {
			float x = gap * (i + 0.5f);
			float y = cy + (h * 0.16f) * (float) Math.sin(t + i * 0.5f);
			c.drawCircle(x, y, 8f + 12f * absSin(t * 1.4f + i), fill);
		}
	}

	private static float absSin(float v) {
		return Math.abs((float) Math.sin(v));
	}
}
