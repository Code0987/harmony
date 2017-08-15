package com.ilusons.harmony.avfx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TimingLogger;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.ref.TimeIt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

public abstract class BaseAVFXCanvasView extends SurfaceView implements SurfaceHolder.Callback {

	@SuppressWarnings("unused")
	private static final String TAG = BaseAVFXCanvasView.class.getSimpleName();
	private static TimeIt TimeIt = new TimeIt();

	private final SurfaceHolder surfaceHolder;
	private AudioDataBuffer.DoubleBufferingManager doubleBufferingManager;
	private int height;
	private int width;
	private Bitmap bitmap;
	private Canvas bitmapCanvas;

	public BaseAVFXCanvasView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		surfaceHolder = getHolder();

		setup();
	}

	public BaseAVFXCanvasView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BaseAVFXCanvasView(Context context) {
		this(context, null, 0);
	}

	public void setup() {
		doubleBufferingManager = new AudioDataBuffer.DoubleBufferingManager();

		setDrawingCacheEnabled(true);

		if (surfaceHolder != null) {
			surfaceHolder.addCallback(this);
			surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
		}

		setZOrderOnTop(true);

		setWillNotDraw(false);
	}

	private void surfaceUpdated() {
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		if (bitmapCanvas == null) {
			bitmapCanvas = new Canvas(bitmap);
		} else {
			bitmapCanvas.setBitmap(bitmap);
		}
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);

		surfaceUpdated();

		setMeasuredDimension(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		this.height = height;
		this.width = width;

		surfaceUpdated();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	public void onPause() {
		synchronized (surfaceHolder) {
			surfaceDestroyed(surfaceHolder);
		}
	}

	public void onResume() {
		synchronized (surfaceHolder) {
			surfaceCreated(surfaceHolder);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		onRenderAudioData(bitmapCanvas, width, height, doubleBufferingManager.getAndSwapBuffer());

		canvas.drawBitmap(bitmap, 0, 0, null);
	}

	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {

	}

	public void updateAudioData(byte[] data, int samplingRate) {
		doubleBufferingManager.update(data, 1, samplingRate);

		invalidate();
	}

	public void updateAudioData(float[] data, int numChannels, int samplingRate) {
		doubleBufferingManager.update(data, numChannels, samplingRate);

		invalidate();
	}

	protected static FloatBuffer allocateNativeFloatBuffer(int size) {
		return ByteBuffer
				.allocateDirect(size * (Float.SIZE / 8))
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
	}

	protected static void converToFloatBuffer(FloatBuffer buffer, float[] array, int n) {
		buffer.clear();
		buffer.limit(n);
		buffer.put(array, 0, n);
		buffer.flip();
	}

}
