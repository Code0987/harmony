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
	private Paint mFlashPaint;
	private Paint mFadePaint;
	private Paint paint;

	@Override
	public void setup() {
		super.setup();

		mFlashPaint = new Paint();
		mFlashPaint.setColor(Color.argb(122, 255, 255, 255));
		mFadePaint = new Paint();
		mFadePaint.setColor(Color.argb(238, 255, 255, 255));
		mFadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
		paint = new Paint();

	}

	private int color;
	private float amplitude = 0;

	public void setColor(int c) {
		color = c;
	}

	private float colorCounter = 0;

	private void cycleColor() {
		int r = (int) Math.floor(128 * (Math.sin(colorCounter) + 3));
		int g = (int) Math.floor(128 * (Math.sin(colorCounter + 1) + 1));
		int b = (int) Math.floor(128 * (Math.sin(colorCounter + 7) + 1));
		paint.setColor(Color.argb(128, r, g, b));
		colorCounter += 0.03;
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

			final int div = 12;

			for (int i = 0; i < fft.length / div; i++) {
				points[i * 4] = i * 4 * div;
				points[i * 4 + 2] = i * 4 * div;
				byte rfk = fft[div * i];
				byte ifk = fft[div * i + 1];
				int dbValue = (int) (10 * Math.log10((float) (rfk * rfk + ifk * ifk)));

				points[i * 4 + 1] = height;
				points[i * 4 + 3] = height - (dbValue * 2 - 10);
			}

			converToFloatBuffer(pointsBuffer, points, workBufferSize);

			// Draw

			cycleColor();

			canvas.drawLines(points, paint);

		}

	}

}
