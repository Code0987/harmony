package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import androidx.core.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ParticlesView extends BaseAVFXCanvasView {

	public ParticlesView(Context context) {
		super(context);
	}

	public ParticlesView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private byte[] waveform;

	private Paint fadePaint;
	private Paint paint;
	private Paint pathPaint;
	private Path path;
	private static final int N = 300;
	private ArrayList<Particle> particles;
	private final int[] colors = new int[N];

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(200, 255, 255, 255));
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
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.15f);
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
		if (data.fData != null) {
			waveform = new byte[data.fData.length];
			int n = data.fData.length / 2;
			for (int i = 0; i < n; i++) {
				waveform[i + 0] = (byte) (data.fData[i + 0] * 128);
				waveform[i + 1] = (byte) (data.fData[i + 1] * 128);
			}
		} else if (data.bData != null) {
			waveform = data.bData;
		}

		if (waveform != null && waveform.length > 0) {
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
		public float R = 4.0f;
		public float X;
		public float Y;
		public float Vx;
		public float Vy;

		public Particle(int n) {
			Index = n;
			X = (float) Math.random() * (float) width;
			Y = (float) Math.random() * (float) height;
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

			float y = 0;

			if (waveform != null) {
				int k = (int) ((X / (float) width) * (float) waveform.length/2);
				k = Math.max(0, Math.min(waveform.length/2 - 1, k));

				y = ((float) Math.abs(waveform[k] & 0xff) / (float) 128) * (height / 2.5f);
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
