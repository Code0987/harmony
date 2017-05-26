package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class BaseAVFXView extends GLSurfaceView {

    @SuppressWarnings("unused")
    private static final String TAG = BaseAVFXView.class.getSimpleName();

    private DoubleBufferingManager doubleBufferingManager;

    public BaseAVFXView(Context context) {
        super(context);
        setup();
    }

    public BaseAVFXView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public void setup() {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        setRenderer(new VisualizerRenderer(this));
        setRenderMode(RENDERMODE_CONTINUOUSLY);

        setZOrderOnTop(true);

        doubleBufferingManager = new DoubleBufferingManager();
    }

    public void onDrawFrame(GL10 gl, int width, int height) {
        Buffer buffer = doubleBufferingManager.getAndSwapBuffer();

        if (buffer != null && buffer.valid()) {
            // clear background
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            onRenderAudioData(gl, width, height, buffer);
        }
    }

    protected void onRenderAudioData(GL10 gl, int width, int height, Buffer data) {

    }

    public void updateAudioData(byte[] data, int samplingRate) {
        // for normal Visualizer
        doubleBufferingManager.update(data, 1, samplingRate);
    }

    public void updateAudioData(float[] data, int numChannels, int samplingRate) {
        // for HQVisualizer
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

    private static final class VisualizerRenderer implements Renderer {
        private WeakReference<BaseAVFXView> holderViewReference;

        int w;
        int h;

        public VisualizerRenderer(BaseAVFXView holderView) {
            holderViewReference = new WeakReference<>(holderView);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glDisable(GL10.GL_DITHER);
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

            gl.glClearColor(0, 0, 0, 0);
            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_SMOOTH);
            // gl.glEnable(GL10.GL_DEPTH_TEST);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);

            // clear background
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            w = width;
            h = height;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            BaseAVFXView holderView = holderViewReference.get();

            if (holderView != null) {
                holderView.onDrawFrame(gl, w, h);
            }
        }
    }

    public static class FloatColor {
        public float red;
        public float green;
        public float blue;
        public float alpha;

        public FloatColor() {
        }

        public FloatColor(float r, float g, float b, float a) {
            red = r;
            green = g;
            blue = b;
            alpha = a;
        }

        public void set(float r, float g, float b, float a) {
            red = r;
            green = g;
            blue = b;
            alpha = a;
        }
    }

}
