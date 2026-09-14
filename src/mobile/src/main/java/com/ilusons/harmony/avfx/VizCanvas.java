package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

/**
 * Now-playing visualizer drawn from scratch.
 * Math is the old idea (time-domain wave + 3 frequency bands as energy).
 * No SurfaceView, GL, render thread, or self-starting animation.
 */
public class VizCanvas extends View {

	public enum Mode {
		WAVE, BARS, RINGS, DOTS
	}

	private static final int FRAME_MS = 50;
	private static final int BARS = 24;
	private static final int WAVE_POINTS = 36;

	private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Handler frames = new Handler(Looper.getMainLooper());
	private final float[] waveY = new float[WAVE_POINTS];
	private final float[] barH = new float[BARS];

	private final Runnable tick = new Runnable() {
		@Override
		public void run() {
			if (!running) {
				return;
			}
			t += 0.08f;
			rebuildSignal();
			invalidate();
			frames.postDelayed(this, FRAME_MS);
		}
	};

	private Mode mode = Mode.RINGS;
	private int color = 0xFF80CBC4;
	private float t;
	private float bass;
	private float mid;
	private float high;
	private boolean running;

	public VizCanvas(Context context) {
		super(context);
		init();
	}

	public VizCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		stroke.setStyle(Paint.Style.STROKE);
		stroke.setStrokeCap(Paint.Cap.ROUND);
		stroke.setStrokeJoin(Paint.Join.ROUND);
		fill.setStyle(Paint.Style.FILL);
	}

	public void setMode(Mode mode) {
		this.mode = mode == null ? Mode.RINGS : mode;
		if (running) {
			invalidate();
		}
	}

	public void setColor(int color) {
		this.color = color;
		stroke.setColor(color);
		fill.setColor(color);
		if (running) {
			invalidate();
		}
	}

	/** Must be called by the host. Never starts itself. */
	public void setRunning(boolean run) {
		if (running == run) {
			return;
		}
		running = run;
		frames.removeCallbacks(tick);
		if (running) {
			frames.post(tick);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		setRunning(false);
		super.onDetachedFromWindow();
	}

	private void rebuildSignal() {
		// Three bands, same split the old Circles VFX used: bass / mid / high.
		bass = 0.35f + 0.65f * absSin(t * 1.3f);
		mid = 0.25f + 0.75f * absSin(t * 2.6f + 1.1f);
		high = 0.20f + 0.80f * absSin(t * 4.4f + 2.2f);

		for (int i = 0; i < WAVE_POINTS; i++) {
			float x = i / (float) (WAVE_POINTS - 1);
			waveY[i] = 0.55f * (float) Math.sin(x * Math.PI * 4.0 + t)
					+ 0.30f * bass * (float) Math.sin(x * Math.PI * 8.0 + t * 1.7f)
					+ 0.15f * high * (float) Math.sin(x * Math.PI * 16.0 + t * 2.3f);
		}

		for (int i = 0; i < BARS; i++) {
			float pos = i / (float) (BARS - 1);
			float band = pos < 0.33f ? bass : (pos < 0.66f ? mid : high);
			barH[i] = 0.2f + 0.8f * band * absSin(t * 1.8f + i * 0.4f);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}
		stroke.setColor(color);
		fill.setColor(color);
		switch (mode) {
			case WAVE:
				drawWave(canvas, w, h);
				break;
			case BARS:
				drawBars(canvas, w, h);
				break;
			case DOTS:
				drawDots(canvas, w, h);
				break;
			case RINGS:
			default:
				drawRings(canvas, w, h);
				break;
		}
	}

	private void drawWave(Canvas canvas, int w, int h) {
		stroke.setStrokeWidth(6f);
		float cy = h * 0.5f;
		float amp = h * 0.22f;
		float prevX = 0f;
		float prevY = cy;
		for (int i = 0; i < WAVE_POINTS; i++) {
			float x = w * (i / (float) (WAVE_POINTS - 1));
			float y = cy + waveY[i] * amp;
			if (i > 0) {
				canvas.drawLine(prevX, prevY, x, y, stroke);
			}
			prevX = x;
			prevY = y;
		}
	}

	private void drawBars(Canvas canvas, int w, int h) {
		float gap = w / (float) BARS;
		stroke.setStrokeWidth(Math.max(5f, gap * 0.42f));
		float cy = h * 0.5f;
		float max = h * 0.32f;
		for (int i = 0; i < BARS; i++) {
			float x = gap * (i + 0.5f);
			float half = max * barH[i];
			canvas.drawLine(x, cy - half, x, cy + half, stroke);
		}
	}

	private void drawRings(Canvas canvas, int w, int h) {
		float cx = w * 0.5f;
		float cy = h * 0.5f;
		float max = Math.min(w, h) * 0.42f;
		stroke.setStrokeWidth(6f);
		canvas.drawCircle(cx, cy, max * (0.22f + 0.18f * bass), stroke);
		stroke.setStrokeWidth(4f);
		canvas.drawCircle(cx, cy, max * (0.42f + 0.16f * mid), stroke);
		stroke.setStrokeWidth(3f);
		canvas.drawCircle(cx, cy, max * (0.62f + 0.14f * high), stroke);
	}

	private void drawDots(Canvas canvas, int w, int h) {
		int n = 14;
		float gap = w / (float) n;
		float cy = h * 0.5f;
		for (int i = 0; i < n; i++) {
			float x = gap * (i + 0.5f);
			float y = cy + (h * 0.16f) * (float) Math.sin(t + i * 0.55f);
			float r = 7f + 16f * barH[Math.min(i, BARS - 1)];
			canvas.drawCircle(x, y, r, fill);
		}
	}

	private static float absSin(float v) {
		return Math.abs((float) Math.sin(v));
	}
}
