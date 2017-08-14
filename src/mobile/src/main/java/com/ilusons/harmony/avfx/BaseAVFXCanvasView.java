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
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class BaseAVFXCanvasView extends SurfaceView implements SurfaceHolder.Callback {

	@SuppressWarnings("unused")
	private static final String TAG = BaseAVFXCanvasView.class.getSimpleName();

	private UpdaterThread thread;
	private final SurfaceHolder surfaceHolder;
	private AudioDataBuffer.DoubleBufferingManager doubleBufferingManager;
	private int height;
	private int width;

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

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (thread == null || !thread.isAlive()) {
			thread = new UpdaterThread(this, holder);
			thread.setRunning(true);
			thread.start();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (thread == null || !thread.isAlive()) {
			thread = new UpdaterThread(this, holder);
			thread.setRunning(true);
			thread.start();
		}

		this.height = height;
		this.width = width;
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

		onRender(canvas, width, height);

		onRenderAudioData(
				canvas,
				width,
				height,
				doubleBufferingManager.getAndSwapBuffer());
	}

	protected void onRender(Canvas canvas, int width, int height) {

	}

	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {

	}

	protected void onUpdate() {

	}

	public void updateAudioData(byte[] data, int samplingRate) {
		doubleBufferingManager.update(data, 1, samplingRate);
	}

	public void updateAudioData(float[] data, int numChannels, int samplingRate) {
		doubleBufferingManager.update(data, numChannels, samplingRate);
	}

	public static class UpdaterThread extends Thread {

		private final BaseAVFXCanvasView sv;
		private final SurfaceHolder sh;

		private boolean running = false;

		private float frameRate = 30;
		private float frameTime = 1000 / frameRate;

		public UpdaterThread(BaseAVFXCanvasView sv, SurfaceHolder sh) {
			this.sv = sv;
			this.sh = sh;
		}

		@Override
		public void run() {

			Canvas canvas = null;

			while (running) {
				float startTime = System.currentTimeMillis();

				if (!sh.getSurface().isValid())
					continue;

				sv.onUpdate();

				try {
					canvas = sh.lockCanvas(null);
					synchronized (sh) {
						sv.postInvalidate();
					}
				} finally {
					if (canvas != null) {
						sh.unlockCanvasAndPost(canvas);
					}
				}

				float endTime = System.currentTimeMillis();
				long deltaTime = (long) (frameTime - (endTime - startTime));
				try {
					Thread.sleep(deltaTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		public void setRunning(boolean b) {
			running = b;
		}

		public void setFrameRate(int rate) {
			frameRate = rate;
		}

	}

}
