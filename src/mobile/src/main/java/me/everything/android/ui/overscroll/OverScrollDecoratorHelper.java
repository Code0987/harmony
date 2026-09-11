package me.everything.android.ui.overscroll;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/** Local stand-in for overscroll-decor. */
public final class OverScrollDecoratorHelper {
	private OverScrollDecoratorHelper() {
	}

	public static Object setUpOverScroll(RecyclerView rv, int orientation) {
		return null;
	}

	public static Object setUpStaticOverScroll(View view, int orientation) {
		return null;
	}

	public static final int ORIENTATION_VERTICAL = 0;
	public static final int ORIENTATION_HORIZONTAL = 1;
}
