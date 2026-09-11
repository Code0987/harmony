package at.grabner.circleprogress;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/** Local stand-in for Circle-Progress-View. */
public class CircleProgressView extends ProgressBar {
	public CircleProgressView(Context context) {
		super(context);
	}

	public CircleProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setValue(float value) {
		setProgress((int) value);
	}

	public void setValueAnimated(float value) {
		setValue(value);
	}

	public void setMaxValue(float max) {
		setMax((int) max);
	}

	public void setBarColor(int... colors) {
	}

	public void setText(String text) {
	}

	public void setShowTextWhileSpinning(boolean show) {
	}

	public void spin() {
	}

	public void stopSpinning() {
	}

	public void setUnitVisible(boolean visible) {
	}

	public void setFillCircleColor(int color) {
	}
}
