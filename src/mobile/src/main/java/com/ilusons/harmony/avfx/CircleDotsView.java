package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CircleDotsView extends BaseAVFXCanvasView {

	public CircleDotsView(Context context) {
		super(context);
	}

	public CircleDotsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private Paint fadePaint;

	private Dots dots;

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(190, 255, 255, 255));
		fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

		dots = new Dots();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.surfaceChanged(holder, format, width, height);

		if (width <= 0 || height <= 0)
			return;

		try {
			dots.w = width;
			dots.h = height;
			dots.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {
		byte[] fft = null;

		if (data.fData != null) {
			fft = new byte[data.fData.length];
			int n = data.fData.length / 2;
			for (int i = 0; i < n; i++) {
				fft[i + 0] = (byte) (data.fData[i + 0] * 128);
				fft[i + 1] = (byte) (data.fData[i + 1] * 128);
			}
		} else if (data.bData != null) {
			fft = data.bData;
		}

		if (fft != null && fft.length > 0) {
			int dataSize = fft.length / 2 - 1;

			dots.initAudioData();

			for (int i = 0; i < dataSize; i++) {
				float re = fft[2 * i];
				float im = fft[2 * i + 1];
				float sq = re * re + im * im;
				double m = Math.sqrt(sq);
				double f = (double) ((data.sr / 1000 * i) / fft.length);

				dots.updateAudioData(m, f);
			}

			// Draw

			canvas.drawPaint(fadePaint);

			dots.update();

			dots.draw(canvas);

		}
	}

//region FXObject

	public static class FXObject {
		public int w;
		public int h;
		public int sz;
		public float s;
		public double lf;
		public double uf;
		public double f;
		public double m;
		public int c;

		public void init() {
			sz = Math.min(w, h);
			s = Math.min(w, h) / 1.45f;
		}

		public boolean needsInit() {
			return false;
		}

		public void draw(Canvas c) {

		}

		public void update() {

		}

	}

	public static class FXObjectGroup extends FXObject {

		public ArrayList<FXObject> Items;

		@Override
		public void init() {
			if (Items == null)
				Items = new ArrayList<>();
			else
				Items.clear();
		}

		public void initAudioData() {
			m = 0;
			f = 0;
		}

		protected static Map<Integer, Integer> createFrequencyColors(int lc, int uc, int lf, int uf) {
			Map<Integer, Integer> r = new HashMap<>();

			for (int i = lf; i < uf; i++) {
				r.put(i, interpolateColor(lc, uc, (float) i / (float) uf));
			}

			return r;
		}

		private static float interpolate(float a, float b, float proportion) {
			return (a + ((b - a) * proportion));
		}

		private static int interpolateColor(int a, int b, float proportion) {

			if (proportion > 1 || proportion < 0) {
				throw new IllegalArgumentException("proportion must be [0 - 1]");
			}
			float[] hsva = new float[3];
			float[] hsvb = new float[3];
			float[] hsv_output = new float[3];

			Color.colorToHSV(a, hsva);
			Color.colorToHSV(b, hsvb);
			for (int i = 0; i < 3; i++) {
				hsv_output[i] = interpolate(hsva[i], hsvb[i], proportion);
			}

			int alpha_a = Color.alpha(a);
			int alpha_b = Color.alpha(b);
			float alpha_output = interpolate(alpha_a, alpha_b, proportion);

			return Color.HSVToColor((int) alpha_output, hsv_output);
		}

		@Override
		public void draw(Canvas c) {
			for (FXObject item : Items) {
				item.draw(c);
			}
		}

		public void updateAudioData(double mag, double freq) {
			if (mag >= m && !(freq <= lf || freq >= uf)) {
				m = mag;
				f = freq;
			}
		}

		@Override
		public void update() {
			for (FXObject item : Items) {
				if (m > 0 && f > 0) {
					if (item.needsInit())
						item.init();

					item.m = m;
					item.f = f;
				}

				item.update();
			}
		}
	}

//endregion

//region FXObjects

	public class Dots extends FXObjectGroup {

		private Map<Integer, Integer> fc;

		@Override
		public void init() {
			super.init();

			if (fc == null)
				fc = new HashMap<>();
			fc.clear();

			lf = 10;
			uf = 500;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 255, 51, 0),
					Color.argb(255, 0, 153, 255),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 15; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

			lf = 500;
			uf = 2500;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 102, 0, 102),
					Color.argb(255, 255, 0, 0),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 15; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

			lf = 2500;
			uf = 8000;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 255, 0, 102),
					Color.argb(255, 255, 255, 0),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 15; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

			lf = 8000;
			uf = 16000;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 102, 0, 204),
					Color.argb(255, 0, 255, 0),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 15; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

			lf = 10;
			uf = 16000;

		}

		@Override
		public void update() {
			for (FXObject item : Items) {
				if (m > 0 && f > 0) {
					item.c = fc.get((int) Math.min(Math.max(lf, f), uf));
				}
			}

			super.update();
		}

	}

	public class Dot extends FXObject {
		private float x;
		private float y;

		private float vx;
		private float vy;

		private float radius;
		private int radius0;
		private double radiusIncrementer;
		private int radiusExpand;

		private float incrementer;
		private float angleCycle;
		private float baseIncrementer;

		private Paint paint;
		private int alphaDecrease;

		public boolean dotRot;

		@Override
		public void init() {
			super.init();

			x = (float) (w / 2);
			y = (float) (h / 2);

			vx = ((float) ThreadLocalRandom.current().nextInt(360));
			vy = ((float) (ThreadLocalRandom.current().nextInt(60) - 30));

			radius = (s / 2.8f) * 2.0f;
			if (radius > 1.0f) {
				radius0 = ThreadLocalRandom.current().nextInt((int) ((radius * 2.0f) / 100.0f));
			}
			radiusIncrementer = ((double) (s / 2.8f)) * 0.0015d;
			radiusExpand = (int) ((s / 2.8f) - radius);

			incrementer = 0;

			if (dotRot) {
				baseIncrementer = 2.16f;
			} else {
				angleCycle = (float) ((((double) ((vx + incrementer) - 90.0f)) * Math.PI) / 180.0d);
				int r = ThreadLocalRandom.current().nextInt(3) + 1;
				if (r == 1) {
					baseIncrementer = 3.16f;
				} else if (r == 2) {
					baseIncrementer = 2.5f;
				} else {
					baseIncrementer = 2.16f;
				}
			}

			paint = new Paint();
			paint.setColor(c);
			paint.setAntiAlias(true);
			alphaDecrease = 2;
		}

		@Override()
		public void draw(Canvas c) {
			super.draw(c);

			if (dotRot) {
				angleCycle = (float) ((((vx + incrementer) - 90.0f) * Math.PI) / 180.0d);
			}
			x = (float) (w / 2 + ((radiusExpand) * Math.cos(angleCycle)));
			y = (float) (h / 2 + ((radiusExpand) * Math.sin(angleCycle)));

			c.drawCircle(x, y, radius, paint);
		}

		@Override
		public void update() {
			super.update();

			if (paint.getAlpha() >= 240) {
				radiusExpand = (int) (radiusExpand + (((s / 2.3f) - radiusExpand) / 2.0f));
				radius += (radius0 - radius) / 2.0f;
			} else {
				incrementer += this.baseIncrementer;
				radiusExpand = (int) (radiusExpand + Math.pow(1.17d, incrementer));
				radius = (float) (radius + radiusIncrementer);
				alphaDecrease = 5;
			}

			if (paint.getAlpha() >= 2) {
				paint.setAlpha(paint.getAlpha() - alphaDecrease);
			} else {
				paint.setAlpha(0);
			}
		}

		@Override
		public boolean needsInit() {
			boolean r = false;
			if (x - radius <= 0.0f || y - radius <= 0.0f) {
				r = true;
			}
			if (x + radius >= w || y + radius >= h) {
				return true;
			}
			return r;
		}
	}

//endregion

}
