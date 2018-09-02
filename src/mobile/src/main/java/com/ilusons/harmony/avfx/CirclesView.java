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

import com.ilusons.harmony.ref.AudioEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CirclesView extends BaseAVFXCanvasView {

	public CirclesView(Context context) {
		super(context);
	}

	public CirclesView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private Paint fadePaint;

	private BassFX bassFX;
	private MidFX midFX;
	private HighFX highFX;

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(190, 255, 255, 255));
		fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

		bassFX = new BassFX();
		midFX = new MidFX();
		highFX = new HighFX();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.surfaceChanged(holder, format, width, height);

		if (width <= 0 || height <= 0)
			return;

		try {
			bassFX.w = width;
			bassFX.h = height;
			bassFX.init();

			midFX.w = width;
			midFX.h = height;
			midFX.init();

			highFX.w = width;
			highFX.h = height;
			highFX.init();
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
			bassFX.initAudioData();
			bassFX.updateAudioData(data.sr / 1000, fft);

			midFX.initAudioData();
			midFX.updateAudioData(data.sr / 1000, fft);

			highFX.initAudioData();
			highFX.updateAudioData(data.sr / 1000, fft);

			// Draw

			canvas.drawPaint(fadePaint);

			bassFX.update();

			midFX.update();

			highFX.update();

			bassFX.draw(canvas);

			midFX.draw(canvas);

			highFX.draw(canvas);

		}
	}

//region FXObject

	public static class FXObject {
		public int w;
		public int h;
		public int sz;
		public int s;
		public double lf;
		public double uf;
		public double f;
		public double m;
		public int c;

		public void init() {
			sz = Math.min(w, h);
			s = Math.min(w, h) / 4;
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

		private AudioEx.AudioCalculator audioCalculator;

		@Override
		public void init() {
			if (Items == null)
				Items = new ArrayList<>();
			else
				Items.clear();

			audioCalculator = new AudioEx.AudioCalculator();
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

		public void updateAudioData(int sr, byte[] data) {
			int min = (int) Math.ceil(lf * data.length / (float) sr);
			int max = (int) Math.ceil(uf * data.length / (float) sr);
			audioCalculator.setBytes(Arrays.copyOfRange(data, min, max));
			double f = audioCalculator.getFrequency();
			double m = 1;

			updateAudioData(m, f);
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
				radiusExpand = (int) (radiusExpand + Math.pow(1.05d, incrementer));
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

	public class Arc extends FXObject {
		private int incrementer;

		private RectF bounds = new RectF();
		private float radius;
		private int startRadius;
		private int endRadius;

		private Paint paint;
		private float strokeChange;

		@Override
		public void init() {
			super.init();

			incrementer = 0;

			radius = (s / 3.0f) + ThreadLocalRandom.current().nextInt(Math.max((int) f, 1));
			startRadius = 0;
			endRadius = startRadius + 360;

			paint = new Paint();
			paint.setColor(c);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(sz / 180.0f);
			strokeChange = 0;
		}

		@Override
		public void draw(Canvas c) {
			super.draw(c);

			paint.setStrokeWidth(((float) sz) / strokeChange);

			bounds.set((w / 2) - radius, (h / 2) - radius, (w / 2) + radius, (h / 2) + radius);

			c.drawArc(bounds, startRadius, endRadius, false, paint);
		}

		@Override
		public void update() {
			super.update();

			incrementer = (int) (incrementer + 1.001f);
			radius = (float) (radius + Math.pow(1.03d, incrementer));

			if (paint.getAlpha() >= 100) {
				strokeChange = 15.0f;
			} else {
				strokeChange += (45.0f - strokeChange) / 15.0f;
			}

			if (paint.getAlpha() >= 2.2d) {
				paint.setAlpha((int) (paint.getAlpha() - 2.2d));
			} else {
				paint.setAlpha(0);
			}
		}

		@Override
		public boolean needsInit() {
			if (radius >= s / 4 || paint.getAlpha() <= 10) {
				return true;
			}
			return false;
		}
	}

	public class BassFX extends FXObjectGroup {

		private Map<Integer, Integer> fc;

		@Override
		public void init() {
			super.init();

			if (fc == null)
				fc = new HashMap<>();
			fc.clear();

			lf = 10;
			uf = 2500;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 255, 51, 0),
					Color.argb(255, 255, 181, 0),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 3; i++) {
				Arc arc = new Arc();

				arc.w = w;
				arc.h = h;
				arc.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				arc.init();

				Items.add(arc);
			}

			for (int i = 0; i < 12; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

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

	public class MidFX extends FXObjectGroup {

		private Map<Integer, Integer> fc;

		@Override
		public void init() {
			super.init();

			if (fc == null)
				fc = new HashMap<>();
			fc.clear();

			lf = 2500;
			uf = 10000;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 0, 255, 0),
					Color.argb(255, 0, 140, 204),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 3; i++) {
				Arc arc = new Arc();

				arc.w = w;
				arc.h = h;
				arc.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				arc.init();

				Items.add(arc);
			}

			for (int i = 0; i < 12; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

		}

		@Override
		public void update() {
			if (Items == null)
				return;

			for (FXObject item : Items) {
				if (m > 0 && f > 0) {
					item.c = fc.get((int) Math.min(Math.max(lf, f), uf));
				}
			}

			super.update();
		}

	}

	public class HighFX extends FXObjectGroup {

		private Map<Integer, Integer> fc;

		@Override
		public void init() {
			super.init();

			if (fc == null)
				fc = new HashMap<>();
			fc.clear();

			lf = 10000;
			uf = 20000;

			fc.putAll(createFrequencyColors(
					Color.argb(255, 102, 0, 204),
					Color.argb(255, 0, 0, 204),
					(int) lf,
					(int) uf));

			for (int i = 0; i < 3; i++) {
				Arc arc = new Arc();

				arc.w = w;
				arc.h = h;
				arc.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				arc.init();

				Items.add(arc);
			}

			for (int i = 0; i < 12; i++) {
				Dot dot = new Dot();

				dot.w = w;
				dot.h = h;
				dot.c = fc.get((int) Math.min(Math.max(lf, f), uf));

				dot.init();

				Items.add(dot);
			}

		}

		@Override
		public void update() {
			if (Items == null)
				return;

			for (FXObject item : Items) {
				if (m > 0 && f > 0) {
					item.c = fc.get((int) Math.min(Math.max(lf, f), uf));
				}
			}

			super.update();
		}

	}

//endregion

}
