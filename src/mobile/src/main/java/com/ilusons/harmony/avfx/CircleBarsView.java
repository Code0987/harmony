package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CircleBarsView extends BaseAVFXCanvasView {

	public CircleBarsView(Context context) {
		super(context);
	}

	public CircleBarsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private Paint fadePaint;

	private Paint paint;
	private Paint circlePaint;
	private float[] points;
	private int radius;

	@Override
	public void setup() {
		super.setup();

		fadePaint = new Paint();
		fadePaint.setColor(Color.argb(160, 255, 255, 255));
		fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setMaskFilter(new BlurMaskFilter(0.3f, BlurMaskFilter.Blur.NORMAL));
		circlePaint = new Paint();
		circlePaint.setMaskFilter(new BlurMaskFilter(0.3f, BlurMaskFilter.Blur.NORMAL));
		radius = -1;
	}

	public void setColor(int color) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[0] += (ThreadLocalRandom.current().nextInt(30 + 1 + 30) - 30);
		hsl[0] = hsl[0] % 360;
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.1f);
		paint.setColor(ColorUtils.HSLToColor(hsl));
		hsl[0] += (ThreadLocalRandom.current().nextInt(30 + 1 + 30) - 30);
		hsl[0] = hsl[0] % 360;
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.1f);
		circlePaint.setColor(ColorUtils.HSLToColor(hsl));

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.surfaceChanged(holder, format, width, height);

		if (width <= 0 || height <= 0)
			return;
	}

	@Override
	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {
		byte[] bytes = null;

		if (data.fData != null) {
			bytes = new byte[data.fData.length];
			int n = data.fData.length / 2;
			for (int i = 0; i < n; i++) {
				bytes[i + 0] = (byte) (data.fData[i + 0] * 128);
				bytes[i + 1] = (byte) (data.fData[i + 1] * 128);
			}
		} else if (data.bData != null) {
			bytes = data.bData;
		}

		if (bytes != null && bytes.length > 0) {
			canvas.drawPaint(fadePaint);

			if (radius == -1) {
				radius = getHeight() < getWidth() ? getHeight() : getWidth();
				radius = (int) (radius * 0.45 / 2);
				double circumference = 2 * Math.PI * radius;
				paint.setStrokeWidth((float) (circumference / 120));
				circlePaint.setStyle(Paint.Style.STROKE);
				circlePaint.setStrokeWidth(4);
			}
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, circlePaint);

			if (points == null || points.length < bytes.length * 4) {
				points = new float[bytes.length * 4];
			}
			double angle = 0;

			for (int i = 0; i < 120; i++, angle += 3) {
				int x = (int) Math.ceil(i * 8.5);
				int t = ((byte) (-Math.abs(bytes[x]) + 128)) * (canvas.getHeight() / 4) / 128;

				points[i * 4] = (float) (getWidth() / 2
						+ radius
						* Math.cos(Math.toRadians(angle)));

				points[i * 4 + 1] = (float) (getHeight() / 2
						+ radius
						* Math.sin(Math.toRadians(angle)));

				points[i * 4 + 2] = (float) (getWidth() / 2
						+ (radius + t)
						* Math.cos(Math.toRadians(angle)));

				points[i * 4 + 3] = (float) (getHeight() / 2
						+ (radius + t)
						* Math.sin(Math.toRadians(angle)));
			}

			canvas.drawLines(points, paint);

		}
	}

}
