package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.SurfaceView;

public abstract class BaseAVFXCanvasView extends SurfaceView {

    @SuppressWarnings("unused")
    private static final String TAG = BaseAVFXCanvasView.class.getSimpleName();

    private DoubleBufferingManager doubleBufferingManager;

    public BaseAVFXCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        setup();
    }

    public BaseAVFXCanvasView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseAVFXCanvasView(Context context) {
        this(context, null, 0);
    }

    public void setup() {
        doubleBufferingManager = new DoubleBufferingManager();
    }

    public void updateAudioData(byte[] data, int samplingRate) {
        doubleBufferingManager.update(data, 1, samplingRate);

        invalidate();
    }

    public void updateAudioData(float[] data, int numChannels, int samplingRate) {
        doubleBufferingManager.update(data, numChannels, samplingRate);

        invalidate();
    }

    protected void onRenderAudioData(Canvas canvas, int width, int height, Buffer data) {

    }

    private Canvas canvas;
    private Bitmap canvasBitmap;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (canvasBitmap == null) {
            canvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        }
        if (canvas == null) {
            canvas = new Canvas(canvasBitmap);
        }

        canvas.drawBitmap(canvasBitmap, new Matrix(), null);
    }

    protected static final class Buffer {

        public byte[] bData;
        public float[] fData;
        public int channels;
        public int sr;

        public void update(byte[] data, int numChannels, int samplingRate) {
            if (bData != null && bData.length == data.length) {
                System.arraycopy(data, 0, bData, 0, data.length);
            } else {
                bData = data.clone();
            }
            fData = null;
            channels = numChannels;
            sr = samplingRate;
        }

        public void update(float[] data, int numChannels, int samplingRate) {
            if (fData != null && fData.length == data.length) {
                System.arraycopy(data, 0, fData, 0, data.length);
            } else {
                fData = data.clone();
            }
            bData = null;
            channels = numChannels;
            sr = samplingRate;
        }

        public boolean valid() {
            return (bData != null || fData != null) && channels != 0 && sr != 0;
        }
    }

    protected static final class DoubleBufferingManager {

        private Buffer[] buffers;
        private int index;
        private boolean updated;

        public DoubleBufferingManager() {
            reset();
        }

        public synchronized void reset() {
            if (buffers == null)
                buffers = new Buffer[2];
            buffers[0] = new Buffer();
            buffers[1] = new Buffer();
            index = 0;
            updated = false;
        }

        public synchronized void update(byte[] data, int numChannels, int samplingRate) {
            buffers[index ^ 1].update(data, numChannels, samplingRate);
            updated = true;

            notify();
        }

        public synchronized void update(float[] data, int numChannels, int samplingRate) {
            buffers[index ^ 1].update(data, numChannels, samplingRate);
            updated = true;

            notify();
        }

        public synchronized Buffer getAndSwapBuffer() {
            if (updated) {
                index ^= 1;
                updated = false;
            }

            return buffers[index];
        }
    }

}
