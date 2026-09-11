package com.turingtechnologies.materialscrollbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/** Local stand-in for MaterialScrollBar. */
public class MaterialScrollBar extends View {
	public MaterialScrollBar(Context context) {
		super(context);
	}

	public MaterialScrollBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MaterialScrollBar setRecyclerView(androidx.recyclerview.widget.RecyclerView rv) {
		return this;
	}
}
