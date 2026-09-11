package jp.co.recruit_lifestyle.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/** Local stand-in for WaveSwipeRefreshLayout. */
public class WaveSwipeRefreshLayout extends SwipeRefreshLayout {
	public WaveSwipeRefreshLayout(Context context) {
		super(context);
	}

	public WaveSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setColorSchemeColors(int... colors) {
		super.setColorSchemeColors(colors);
	}
}
