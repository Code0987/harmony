package com.ilusons.harmony.avfx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import java.util.LinkedList;
import java.util.Locale;

public abstract class BaseAVFXCanvasView extends SurfaceView implements SurfaceHolder.Callback {

	@SuppressWarnings("unused")
	private static final String TAG = BaseAVFXCanvasView.class.getSimpleName();

	private UpdaterThread thread;
	private AudioDataBuffer.DoubleBufferingManager doubleBufferingManager;
	protected int height;
	protected int width;
	private Bitmap bitmap;
	private Canvas bitmapCanvas;

	public BaseAVFXCanvasView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);

		SurfaceHolder sh = getHolder();
		if (sh != null) {
			sh.addCallback(this);
			sh.setFormat(PixelFormat.TRANSLUCENT);
		}

		setup();
	}

	public BaseAVFXCanvasView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BaseAVFXCanvasView(Context context) {
		this(context, null, 0);
	}

	public void setup() {
		setWillNotDraw(false);
		setDrawingCacheEnabled(true);
		setZOrderOnTop(true);

		doubleBufferingManager = new AudioDataBuffer.DoubleBufferingManager();
	}

	private void surfaceUpdated() {
		if (width <= 0 || height <= 0)
			return;

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

		if (width <= 0 || height <= 0)
			return;

		surfaceUpdated();

		if (thread == null || !thread.isAlive()) {
			thread = new UpdaterThread(this, holder);
			thread.setRunning(true);
			thread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				Log.d(TAG, "Interrupted Exception", e);
			}
		}
	}

	public void onPause() {
		if (!(thread == null || !thread.isAlive()))
			thread.setRunning(false);
	}

	public void onResume() {
		if (!(thread == null || !thread.isAlive()))
			thread.setRunning(true);
	}

	// private static final TimeIt.FPS FPS = new TimeIt.FPS();

	@Override
	protected void onDraw(Canvas canvas) {
		// Log.d(TAG, "FPS: " + FPS.getFPS());

		super.onDraw(canvas);

		onRenderAudioData(bitmapCanvas, width, height, doubleBufferingManager.getAndSwapBuffer());

		canvas.drawBitmap(bitmap, 0, 0, null);
	}

	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {

	}

	public void updateAudioData(byte[] data, int samplingRate) {
		doubleBufferingManager.update(data, 1, samplingRate);
	}

	public void updateAudioData(float[] data, int numChannels, int samplingRate) {
		doubleBufferingManager.update(data, numChannels, samplingRate);
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

	public static class UpdaterThread extends Thread {

		private final BaseAVFXCanvasView sv;
		private final SurfaceHolder sh;

		private boolean running = false;

		private float frameTime = 1000.0f / 60;

		public UpdaterThread(BaseAVFXCanvasView sv, SurfaceHolder sh) {
			this.sv = sv;
			this.sh = sh;
		}

		@Override
		public void run() {

			Canvas canvas = null;

			while (running) {
				float startTime = System.nanoTime();

				if (!sh.getSurface().isValid())
					continue;

				try {
					canvas = sh.lockCanvas(null);
					synchronized (sh) {
						sv.postInvalidate();
					}
				} finally {
					if (canvas != null) try {
						sh.unlockCanvasAndPost(canvas);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				try {
					Thread.sleep((long) (frameTime - (System.nanoTime() - startTime) / 1000000000.0));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		public void setRunning(boolean b) {
			running = b;
		}

		public void setFrameRate(int rate) {
			frameTime = 1000.0f / rate;
		}

	}

}
