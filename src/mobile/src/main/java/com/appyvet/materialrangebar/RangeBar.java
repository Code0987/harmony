package com.appyvet.materialrangebar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

/** Local stand-in for Material RangeBar. */
public class RangeBar extends SeekBar {
	public interface OnRangeBarChangeListener {
		void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue);
	}

	public RangeBar(Context context) {
		super(context);
	}

	public RangeBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setOnRangeBarChangeListener(OnRangeBarChangeListener listener) {
	}

	public void setRangePinsByValue(float left, float right) {
	}

	public void setTickEnd(float end) {
		setMax((int) end);
	}

	public void setTickStart(float start) {
	}

	public float getLeftPinValue() {
		return 0;
	}

	public float getRightPinValue() {
		return getProgress();
	}

	public int getLeftIndex() {
		return 0;
	}

	public int getRightIndex() {
		return getProgress();
	}

	public float getTickInterval() {
		return 1f;
	}
}
