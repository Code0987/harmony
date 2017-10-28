package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ParticlesView extends BaseAVFXCanvasView {

	public ParticlesView(Context context) {
		super(context);
	}

	public ParticlesView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private float[] dbs;
	private float[] amps;
	private float dbMax = 0f;

	float[] buffer;
	public FloatBuffer nativeBuffer;

	private Paint fadePaint;
	private Paint paint;
	private Paint pathPaint;
	private Path path;
	private static final int N = 99;
	private ArrayList<Particle> particles;
	private final int[] colors = new int[N];

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(190, 255, 255, 255));
		fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

		paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1f);
		paint.setMaskFilter(new BlurMaskFilter(0.3f, BlurMaskFilter.Blur.NORMAL));
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));

		pathPaint = new Paint();
		pathPaint.setStyle(Paint.Style.FILL);
		pathPaint.setAntiAlias(true);
		pathPaint.setStrokeWidth(1f);
		pathPaint.setMaskFilter(new BlurMaskFilter(1.3f, BlurMaskFilter.Blur.NORMAL));

		path = new Path();

		particles = new ArrayList<>(N);
		for (int n = 0; n < N; n++) {
			particles.add(new Particle(n));
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.surfaceChanged(holder, format, width, height);

		float x = (float) width / (float) particles.size();
		for (int i = 0; i < particles.size(); i++) {
			Particle p = particles.get(i);

			p.X = x * i;
		}
	}

	public void setColor(int color) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[0] += (ThreadLocalRandom.current().nextInt(30 + 1 + 30) - 30);
		hsl[0] = hsl[0] % 360;
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.1f);
		paint.setColor(ColorUtils.HSLToColor(hsl));

		for (int n = 0; n < N; n++) {
			int h = (int) (15 + (float) n / 15f);
			ColorUtils.colorToHSL(color, hsl);
			hsl[0] += (ThreadLocalRandom.current().nextInt(h + 1 + h) - h);
			hsl[0] = hsl[0] % 360;
			colors[n] = ColorUtils.HSLToColor(hsl);
		}

	}

	@Override
	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {
		if (data.bData != null) {
			byte[] fft = data.bData;

			int dataSize = fft.length / 2 - 1;

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
				if (dbs[i] > dbMax)
					dbMax = dbs[i];

				float k = 1;
				if (i == 0 || i == dataSize - 1) {
					k = 2;
				}

				amps[i] = (float) (k * Math.sqrt(sq) / dataSize);
			}

			// Draw

			canvas.drawPaint(fadePaint);

			for (int i = 0; i < particles.size(); i++) {
				Particle p = particles.get(i);

				p.update();

				p.draw(canvas);
			}
		}
	}

	class Particle {
		public int Index;
		public float R = 5.0f;
		public float X;
		public float Y;
		public float Vx;
		public float Vy;

		public Particle(int n) {
			Index = n;
			X = (float) Math.random() * width;
			Y = (float) Math.random() * height;
			Vx = ((float) Math.random() - 0.5f) * 17;
			Vy = ((float) Math.random() - 0.5f) * 17;
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

				db = db % height;
				if (db < 0) {
					db += height;
				}

				db += 10 * Math.random() * Math.random();

				y = db * (height / dbMax);
			}

			if (Math.abs(y) < 45 || Math.abs(y) > height / 2) {
				X += Vx;
				Y += Vy;
			}

			Y = (height - R - y);

		}

		public void draw(Canvas c) {
			int cb = paint.getColor();
			paint.setColor(colors[Index]);
			c.drawCircle(X + R, Y + R, R, paint);
			paint.setColor(cb);
		}
	}
}
