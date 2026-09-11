package com.wang.avi;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/** Local stand-in for the retired AVLoadingIndicatorView artifact. */
public class AVLoadingIndicatorView extends ProgressBar {
	public AVLoadingIndicatorView(Context context) {
		super(context);
	}

	public AVLoadingIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AVLoadingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void smoothToShow() {
		setVisibility(VISIBLE);
	}

	public void smoothToHide() {
		setVisibility(GONE);
	}

	public void show() {
		setVisibility(VISIBLE);
	}

	public void hide() {
		setVisibility(GONE);
	}
}
