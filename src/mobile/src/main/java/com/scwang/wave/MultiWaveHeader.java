package com.scwang.wave;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/** Local stand-in for the retired MultiWaveHeader artifact. */
public class MultiWaveHeader extends View {
	public MultiWaveHeader(Context context) {
		super(context);
	}

	public MultiWaveHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MultiWaveHeader(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setVelocity(float v) {
	}

	public void setProgress(float p) {
	}

	public void start() {
	}

	public void stop() {
	}

	public boolean isRunning() {
		return false;
	}

	public void setStartColor(int color) {
	}

	public void setCloseColor(int color) {
	}
}
