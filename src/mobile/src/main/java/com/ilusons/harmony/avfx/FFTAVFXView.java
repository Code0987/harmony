package com.ilusons.harmony.avfx;

import android.content.Context;
import android.util.AttributeSet;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class FFTAVFXView extends BaseAVFXView {

    float[] buffer;
    public FloatBuffer nativeBuffer;

    FloatColor color;

    int lastCanvasWidth;

    public FFTAVFXView(Context context) {
        super(context);
    }

    public FFTAVFXView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setup() {
        super.setup();

        color = new FloatColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void setColor(FloatColor c) {
        color = c;
    }

    @Override
    protected void onRenderAudioData(GL10 gl, int width, int height, Buffer data) {
        final byte[] fft = data.bData;

        // -2, +2: (DC + Fs/2), /2: (Re + Im)
        final int N = (fft.length - 2) / 2 + 2;

        // 2: (x, y)
        final int bufferSize = N * 2;
        boolean needToUpdateX = false;

        // prepare working buffer
        if (buffer == null || buffer.length < bufferSize) {
            buffer = new float[bufferSize];
            nativeBuffer = allocateNativeFloatBuffer(bufferSize);
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

        final float yrange = 128.0f + 1.0f;

        makeYPointPositionData(fft, N, 0, points);
        converToFloatBuffer(pointsBuffer, points, (2 * N));
        drawFFT(gl, width, height, pointsBuffer, N, yrange, color);
    }

    private void makeXPointPositionData(int N, float[] points) {
        final float x_diff = 1.0f / (N - 1);
        for (int i = 0; i < N; i++) {
            points[2 * i + 0] = (x_diff * i);
        }
    }

    private static void makeYPointPositionData(byte[] fft, int n, int offset, float[] points) {
        // NOTE:
        // mag = sqrt( (re / 128)^2 + (im / 128)^2 )
        // = sqrt( (1 / 128)^2 * (re^2 + im^2) )
        // = (1 / 128) * sqrt(re^2 + im^2)
        //
        // data range = [0:128] (NOTE: L + R mixed gain)

        // DC
        points[2 * 0 + 1] = Math.abs(fft[offset + 0]);

        // f_1 .. f_(N-1)
        for (int i = 1; i < (n - 1); i++) {
            final int re = fft[offset + 2 * i + 0];
            final int im = fft[offset + 2 * i + 1];
            final float y = fastSqrt((re * re) + (im * im));

            points[2 * i + 1] = y;
        }

        // fs / 2
        points[2 * (n - 1) + 1] = Math.abs(fft[offset + 1]);
    }

    private static void drawFFT(GL10 gl, int width, int height, FloatBuffer vertices, int n, float yrange, FloatColor color) {

        gl.glPushMatrix();

        // viewport
        gl.glViewport(0, 0, width, height);

        // X: [0:1], Y: [0:yrange]
        gl.glOrthof(0.0f, 1.0f, 0.0f, yrange, -1.0f, 1.0f);

        gl.glColor4f(color.red, color.green, color.blue, color.alpha);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, (2 * Float.SIZE / 8), vertices);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, n);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glPopMatrix();
    }

    // http://forum.processing.org/topic/super-fast-square-root
    private static final float fastSqrt(float x) {
        return Float.intBitsToFloat(532483686 + (Float.floatToRawIntBits(x) >> 1));
    }
    
}
