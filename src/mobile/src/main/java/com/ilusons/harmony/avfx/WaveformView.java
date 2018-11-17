package com.ilusons.harmony.avfx;

import android.content.Context;
import android.util.AttributeSet;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class WaveformView extends BaseAVFXGLView {

    float[] buffer;
    FloatBuffer nativeBuffer;

    FloatColor leftChannelColor;
    FloatColor rightChannelColor;

    int lastCanvasWidth;

    public WaveformView(Context context) {
        super(context);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setup() {
        super.setup();

        leftChannelColor = new FloatColor(1.0f, 1.0f, 1.0f, 1.0f);
        rightChannelColor = new FloatColor(1.0f, 0.0f, 0.0f, 1.0f);
    }

    public void setColor(FloatColor l, FloatColor r) {
        leftChannelColor = l;
        rightChannelColor = r;
    }

    @Override
    protected void onRenderAudioData(GL10 gl, int width, int height, AudioDataBuffer.Buffer data) {
        if (data.fData != null) {
            // NOTE:
            // Increase the SKIP value if the visualization is too heavy
            // on your device

            final float[] waveform = data.fData;
            final int NUM_CHANNELS = 2;
            final int SKIP = Math.max(1, (waveform.length >> 12));
            final int N = waveform.length / (SKIP * NUM_CHANNELS);

            // 2: (x, y)
            final int workBufferSize = N * 2;
            boolean needToUpdateX = false;

            // prepare working buffer
            if (buffer == null || buffer.length < workBufferSize) {
                buffer = new float[workBufferSize];
                nativeBuffer = allocateNativeFloatBuffer(workBufferSize);
                needToUpdateX |= true;
            }

            final float[] points = buffer;
            final FloatBuffer pointsBuffer = nativeBuffer;

            // prepare points info buffer for drawing

            needToUpdateX |= (width != lastCanvasWidth);
            lastCanvasWidth = width;

            if (needToUpdateX) {
                makeXPointPositionData(N, points);
            }

            final float yrange = 1.25f;

            // Left channel
            makeYPointPositionData(waveform, N, SKIP, 0, points);
            converToFloatBuffer(pointsBuffer, points, (2 * N));
            drawWaveForm(gl, width, height, pointsBuffer, N, 0, yrange, leftChannelColor);

            // Right channel
            makeYPointPositionData(waveform, N, SKIP, waveform.length / NUM_CHANNELS, points);
            converToFloatBuffer(pointsBuffer, points, (2 * N));
            drawWaveForm(gl, width, height, pointsBuffer, N, 1, yrange, rightChannelColor);
        }

        if (data.bData != null) {
            // NOTE:
            // Increase the SKIP value if the visualization is too heavy
            // on your device

            final int SKIP = 1;
            final byte[] waveform = data.bData;
            final int N = waveform.length / SKIP;

            // 2: (x, 2)
            final int workBufferSize = N * 2;
            boolean needToUpdateX = false;

            // prepare working buffer
            if (buffer == null || buffer.length < workBufferSize) {
                buffer = new float[workBufferSize];
                nativeBuffer = allocateNativeFloatBuffer(workBufferSize);
                needToUpdateX |= true;
            }

            final float[] points = buffer;
            final FloatBuffer pointsBuffer = nativeBuffer;

            needToUpdateX |= (width != lastCanvasWidth);
            lastCanvasWidth = width;

            if (needToUpdateX) {
                makeXPointPositionData(N, points);
            }

            makeYPointPositionData(waveform, N, SKIP, 0, points);
            converToFloatBuffer(pointsBuffer, points, (2 * N));
            drawWaveForm(gl, width, height, pointsBuffer, N, (0.0f - 1.0f), (255.0f + 1.0f), leftChannelColor);
        }
    }

    private void makeXPointPositionData(int N, float[] points) {
        final float x_diff = 1.0f / (N - 1);
        for (int i = 0; i < N; i++) {
            points[2 * i + 0] = (x_diff * i);
        }
    }

    private static void makeYPointPositionData(float[] waveform, int n, int skip, int offset, float[] points) {
        for (int i = 0; i < n; i++) {
            points[2 * i + 1] = waveform[offset + i * skip];
        }
    }

    private static void drawWaveForm(GL10 gl, int width, int height, FloatBuffer vertices, int n, int vposition, float yrange, FloatColor color) {

        gl.glPushMatrix();

        // viewport
        gl.glViewport(0, (height / 2) * (1 - vposition), width, (height / 2));
        // gl.glViewport(0, 0, width, height);

        // X: [0:1], Y: [-1:+1] (scaled)
        gl.glOrthof(0.0f, 1.0f, -yrange, yrange, -1.0f, 1.0f);

        gl.glColor4f(color.red, color.green, color.blue, color.alpha);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, (2 * Float.SIZE / 8), vertices);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, n);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glPopMatrix();
    }

    private static void makeYPointPositionData(byte[] waveform, int n, int skip, int offset, float[] points) {
        // NOTE: data range = [0:255]
        for (int i = 0; i < n; i++) {
            points[2 * i + 1] = waveform[(offset + i) * skip] & 0xff;
        }
    }

    private static void drawWaveForm(GL10 gl, int width, int height, FloatBuffer vertices, int n, float ymin, float ymax, FloatColor color) {

        gl.glPushMatrix();

        // viewport
        gl.glViewport(0, 0, width, height);

        // X: [0:1], Y: [ymin:ymax]
        gl.glOrthof(0.0f, 1.0f, ymin, ymax, -1.0f, 1.0f);

        gl.glColor4f(color.red, color.green, color.blue, color.alpha);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, (2 * Float.SIZE / 8), vertices);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, n);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glPopMatrix();
    }

}
