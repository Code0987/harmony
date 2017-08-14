package com.ilusons.harmony.avfx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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

	@Override
	public void setup() {
		super.setup();
	}

	private int color;

	public void setColor(int c) {
		color = c;
	}

	@Override
	protected void onRenderAudioData(Canvas canvas, int width, int height, AudioDataBuffer.Buffer data) {
		super.onRenderAudioData(canvas, width, height, data);


	}
}
