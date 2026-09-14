package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lightweight visualizer that never uses SurfaceView or GL.
 * Those paths hang inside ViewPagers on current Android.
 */
public class LiteVfxView extends View {

	public enum Style {
		WAVE, BARS, DOTS, RINGS
	}

	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Runnable frame = new Runnable() {
		@Override
		public void run() {
			if (!attached) {
				return;
			}
			phase += 0.12f;
			invalidate();
			postOnAnimation(this);
		}
	};

	private Style style = Style.BARS;
	private int color = 0xFF80CBC4;
	private float phase;
	private boolean attached;

	public LiteVfxView(Context context) {
		super(context);
		init();
	}

	public LiteVfxView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStyle(Paint.Style.STROKE);
	}

	public void setStyle(Style style) {
		this.style = style == null ? Style.BARS : style;
		invalidate();
	}

	public void setColor(int color) {
		this.color = color;
		paint.setColor(color);
		invalidate();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		attached = true;
		kick();
	}

	@Override
	protected void onDetachedFromWindow() {
		attached = false;
		removeCallbacks(frame);
		super.onDetachedFromWindow();
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (visibility == VISIBLE) {
			kick();
		} else {
			removeCallbacks(frame);
		}
	}

	private void kick() {
		removeCallbacks(frame);
		if (attached && getVisibility() == VISIBLE) {
			postOnAnimation(frame);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}
		paint.setColor(color);
		float cx = w / 2f;
		float cy = h / 2f;

		switch (style) {
			case WAVE:
				drawWave(canvas, w, h, cy);
				break;
			case DOTS:
				drawDots(canvas, w, h, cy);
				break;
			case RINGS:
				drawRings(canvas, cx, cy, Math.min(w, h));
				break;
			case BARS:
			default:
				drawBars(canvas, w, h, cy);
				break;
		}
	}

	private void drawBars(Canvas canvas, int w, int h, float cy) {
		int n = 28;
		float gap = w / (float) n;
		paint.setStrokeWidth(Math.max(6f, gap * 0.45f));
		for (int i = 0; i < n; i++) {
			float amp = 0.25f + 0.75f * absSin(phase + i * 0.35f);
			float x = gap * (i + 0.5f);
			float half = (h * 0.28f) * amp;
			canvas.drawLine(x, cy - half, x, cy + half, paint);
		}
	}

	private void drawWave(Canvas canvas, int w, int h, float cy) {
		paint.setStrokeWidth(6f);
		float prevX = 0;
		float prevY = cy;
		int steps = 48;
		for (int i = 1; i <= steps; i++) {
			float x = w * (i / (float) steps);
			float y = cy + (h * 0.22f) * (float) Math.sin(phase + i * 0.28f);
			canvas.drawLine(prevX, prevY, x, y, paint);
			prevX = x;
			prevY = y;
		}
	}

	private void drawDots(Canvas canvas, int w, int h, float cy) {
		int n = 18;
		float gap = w / (float) n;
		for (int i = 0; i < n; i++) {
			float amp = 0.2f + 0.8f * absSin(phase * 1.2f + i * 0.4f);
			float x = gap * (i + 0.5f);
			float y = cy + (h * 0.18f) * (float) Math.sin(phase + i * 0.5f);
			canvas.drawCircle(x, y, 8f + 14f * amp, paint);
		}
	}

	private void drawRings(Canvas canvas, float cx, float cy, int size) {
		paint.setStrokeWidth(5f);
		float max = size * 0.42f;
		for (int i = 0; i < 5; i++) {
			float amp = 0.45f + 0.55f * absSin(phase * 0.9f + i * 0.7f);
			canvas.drawCircle(cx, cy, max * (0.25f + 0.15f * i) * amp, paint);
		}
	}

	private static float absSin(float v) {
		return Math.abs((float) Math.sin(v));
	}
}
