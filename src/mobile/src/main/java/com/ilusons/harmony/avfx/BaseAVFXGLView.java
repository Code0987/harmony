package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.ref.TimeIt;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class BaseAVFXGLView extends GLSurfaceView {

    @SuppressWarnings("unused")
    private static final String TAG = BaseAVFXGLView.class.getSimpleName();
    private static TimeIt TimeIt = new TimeIt();

    private AudioDataBuffer.DoubleBufferingManager doubleBufferingManager;

    public BaseAVFXGLView(Context context) {
        super(context);
        setup();
    }

    public BaseAVFXGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public void setup() {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        setRenderer(new VisualizerRenderer(this));
        setRenderMode(RENDERMODE_CONTINUOUSLY);

        setZOrderOnTop(true);

        doubleBufferingManager = new AudioDataBuffer.DoubleBufferingManager();
    }

    public void onDrawFrame(GL10 gl, int width, int height) {
        AudioDataBuffer.Buffer buffer = doubleBufferingManager.getAndSwapBuffer();

        if (buffer != null && buffer.valid()) {
            // clear background
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            onRenderAudioData(gl, width, height, buffer);
        }
    }

    protected void onRenderAudioData(GL10 gl, int width, int height, AudioDataBuffer.Buffer data) {

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

    private static final class VisualizerRenderer implements Renderer {
        private WeakReference<BaseAVFXGLView> holderViewReference;

        int w;
        int h;

        public VisualizerRenderer(BaseAVFXGLView holderView) {
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
            BaseAVFXGLView holderView = holderViewReference.get();

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
