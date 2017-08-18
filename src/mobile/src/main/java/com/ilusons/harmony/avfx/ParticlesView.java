package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ParticlesView extends BaseAVFXCanvasView {

	public ParticlesView(Context context) {
		super(context);
	}

	public ParticlesView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private float[] dbs;
	private float[] amps;

	float[] buffer;
	public FloatBuffer nativeBuffer;

	private Paint fadePaint;
	private Paint paint;
	private Paint pathPaint;
	private Path path;
	private static final int N = 55;
	private ArrayList<Particle> particles;

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(230, 255, 255, 255));
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
		while (particles.size() < N)
			particles.add(new Particle());

	}

	public void setColor(int color) {
		paint.setColor(color);
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

				for (int j = particles.size() - 1; j > i; j--) {
					float d = (float) Math.sqrt(
							Math.pow(particles.get(i).X - particles.get(j).X, 2)
									+
									Math.pow(particles.get(i).Y - particles.get(j).Y, 2)
					);

					if (d > p.Proximity)
						continue;

					pathPaint.setAlpha((int) (((p.Proximity - d) / p.Proximity) * 255));

					path.reset();
					path.moveTo(particles.get(i).X, particles.get(i).Y);
					path.lineTo(particles.get(j).X, particles.get(j).Y);
					path.close();

					canvas.drawPath(path, pathPaint);
				}
			}
		}
	}

	class Particle {
		public float R = 3.0f;
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

				y = amp * 10000;
			}

			if (Math.abs(y) < 1) {
				X += Vx;
				Y += Vy;
			} else {
				X += Vx;
				Y += ((height - R - y) - Y);
			}

		}

		public void draw(Canvas c) {
			c.drawCircle(X + R, Y + R, R, paint);
		}
	}
}
