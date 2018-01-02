package com.ilusons.harmony.avfx;

import android.content.ClipData;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DotsView extends BaseAVFXCanvasView {

	public DotsView(Context context) {
		super(context);
	}

	public DotsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private Paint fadePaint;

	private ArrayList<Dots> dots;

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(30, 255, 255, 255));
		fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

		dots = new ArrayList<>();
	}

	private final int[] colors = new int[6];

	public void setColor(int color) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[0] += (ThreadLocalRandom.current().nextInt(60 + 1 + 60) - 60);
		hsl[0] = hsl[0] % 360;
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.1f);
		colors[0] = ColorUtils.HSLToColor(hsl);

		for (int n = 1; n < colors.length; n++) {
			int h = (int) (60 + (float) n / 60f);
			ColorUtils.colorToHSL(color, hsl);
			hsl[0] += (ThreadLocalRandom.current().nextInt(h + 1 + h) - h);
			hsl[0] = hsl[0] % 360;
			colors[n] = ColorUtils.HSLToColor(hsl);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.surfaceChanged(holder, format, width, height);

		if (width <= 0 || height <= 0)
			return;

		dots.clear();

		Dots d;

		d = new Dots();
		d.w = width;
		d.h = height;
		d.lf = 10;
		d.uf = 500;
		d.s = 80;
		d.lc = colors[0];
		d.uc = colors[1];
		d.init();
		dots.add(d);

		d = new Dots();
		d.w = width;
		d.h = height;
		d.lf = 2500;
		d.uf = 8000;
		d.s = 80;
		d.lc = colors[2];
		d.uc = colors[3];
		d.init();
		dots.add(d);

		d = new Dots();
		d.w = width;
		d.h = height;
		d.lf = 2500;
		d.uf = 16000;
		d.s = 80;
		d.lc = colors[4];
		d.uc = colors[5];
		d.init();
		dots.add(d);
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

			for (int i = 0; i < dots.size(); i++)
				dots.get(i).initAudioData();

			for (int i = 0; i < dataSize; i++) {
				float re = fft[2 * i];
				float im = fft[2 * i + 1];
				float sq = re * re + im * im;
				double m = Math.sqrt(sq);
				double f = (double) ((data.sr / 1000 * i) / fft.length);

				for (int j = 0; j < dots.size(); j++)
					dots.get(j).updateAudioData(m, f);
			}

			// Draw

			canvas.drawPaint(fadePaint);

			for (int i = 0; i < dots.size(); i++) {
				dots.get(i).update();

				dots.get(i).draw(canvas);
			}

		}
	}

	public static class Dots {
		public int w;
		public int h;
		public double lf;
		public double uf;
		public double s;
		public int lc;
		public int uc;

		public double f;
		public double m;

		public double fdist;

		public ArrayList<Dot> Items;

		public void init() {
			if (Items == null)
				Items = new ArrayList<>();
			else
				Items.clear();

			int r = (int) ((float) h / s);
			int c = (int) ((float) w / s);

			fdist = lf + ((uf - lf) * (1 / (float) (c * r)));

			for (int i = 0; i < r; i++) {
				for (int j = 0; j < c; j++) {
					Dot dot = new Dot();

					int k = c * i + j;

					dot.f = lf + ((uf - lf) * ((float) k / (float) (c * r)));
					dot.r = (float) (s / 8);
					dot.x = (float) (j * s + s / 2);
					dot.y = (float) ((r - i - 1) * s + s / 2);
					dot.c = interpolateColor(lc, uc, (float) (dot.f - lf) / (float) (uf - lf));

					dot.init();

					Items.add(dot);
				}
			}

		}

		public void draw(Canvas c) {
			for (Dot item : Items) {
				item.draw(c);
			}
		}

		public void initAudioData() {
			m = 0;
			f = 0;
		}

		public void updateAudioData(double mag, double freq) {
			if (mag >= m && !(freq <= lf || freq >= uf)) {
				m = mag;
				f = freq;
			}
		}

		public void update() {
			for (Dot item : Items) {
				if (f > 0 && Math.abs(item.f - f) <= fdist) {
					if (item.needsInit())
						item.init();
				}

				item.update();
			}
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

	}

	public static class Dot {
		private float x;
		private float y;
		private double f;

		private float r;

		private Paint paint;
		private int c;
		private int alphaDecrease;

		public void init() {
			paint = new Paint();
			paint.setColor(c);
			paint.setAntiAlias(true);
			alphaDecrease = 12;
		}

		public void draw(Canvas c) {
			c.drawCircle(x, y, r, paint);
		}

		public void update() {
			if (paint.getAlpha() >= 10) {
				paint.setAlpha(paint.getAlpha() - alphaDecrease);
			} else {
				paint.setAlpha(0);
			}
		}

		public boolean needsInit() {
			return paint.getAlpha() <= 10;
		}
	}

}
