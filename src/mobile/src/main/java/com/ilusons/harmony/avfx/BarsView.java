package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class BarsView extends BaseAVFXCanvasView {

	public BarsView(Context context) {
		super(context);
	}

	public BarsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	float[] buffer;
	public FloatBuffer nativeBuffer;

	private int divisions;
	private Paint paint;

	@Override
	public void setup() {
		super.setup();

		divisions = 1;

		paint = new Paint();
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

	}

	public void setDivisions(int n) {
		divisions = n;
	}

	public int getDivisions() {
		return divisions;
	}

	public Paint getPaint() {
		return paint;
	}

	private float paintColorCounter = 0;

	private void cyclePaintColor() {
		int r = (int) Math.floor(128 * (Math.sin(paintColorCounter) + 3));
		int g = (int) Math.floor(128 * (Math.sin(paintColorCounter + 1) + 1));
		int b = (int) Math.floor(128 * (Math.sin(paintColorCounter + 7) + 1));

		paintColorCounter += 0.03;

		int c = Color.rgb(r, g, b);

		//c = blend(paint.getColor(), c, 0.85f);

		paint.setColor(c);
	}

	private int blend(int c1, int c2, float ratio) {
		if (ratio > 1f) ratio = 1f;
		else if (ratio < 0f) ratio = 0f;
		float ir = 1.0f - ratio;

		int a1 = (c1 >> 24 & 0xff);
		int r1 = ((c1 & 0xff0000) >> 16);
		int g1 = ((c1 & 0xff00) >> 8);
		int b1 = (c1 & 0xff);

		int a2 = (c2 >> 24 & 0xff);
		int r2 = ((c2 & 0xff0000) >> 16);
		int g2 = ((c2 & 0xff00) >> 8);
		int b2 = (c2 & 0xff);

		int a = (int) ((a1 * ir) + (a2 * ratio));
		int r = (int) ((r1 * ir) + (r2 * ratio));
		int g = (int) ((g1 * ir) + (g2 * ratio));
		int b = (int) ((b1 * ir) + (b2 * ratio));

		return (a << 24 | r << 16 | g << 8 | b);
	}

	@Override
	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {
		if (data.bData != null) {
			final byte[] fft = data.bData;

			// Setup working buffer
			final int workBufferSize = fft.length * 4;
			if (buffer == null || buffer.length < workBufferSize) {
				buffer = new float[workBufferSize];
				nativeBuffer = allocateNativeFloatBuffer(workBufferSize);
			}
			final float[] points = buffer;
			final FloatBuffer pointsBuffer = nativeBuffer;

			// Calculate points
			final int div = divisions;
			for (int i = 0; i < fft.length / div - 1; i++) {
				points[i * 4] = i * 4 * div;
				points[i * 4 + 2] = i * 4 * div;
				byte rfk = fft[div * i];
				byte ifk = fft[div * i + 1];
				int dbValue = (int) (10 * Math.log10((float) (rfk * rfk + ifk * ifk)));

				points[i * 4 + 1] = height;
				points[i * 4 + 3] = height - (dbValue * 2 - 10) * 4;
			}

			converToFloatBuffer(pointsBuffer, points, workBufferSize);

			// Draw
			cyclePaintColor();

			canvas.drawLines(points, paint);

		}
	}

}
