package com.ilusons.harmony.base;

import android.app.Activity;
import android.content.Context;

import com.ilusons.harmony.ref.AndroidEx;

import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;

public class DrawerArrow extends DrawerArrowDrawable {
	private final Activity activity;

	public DrawerArrow(Activity activity, Context themedContext) {
		super(themedContext);

		this.activity = activity;

		setBarThickness(AndroidEx.dpToPx(1));
		setArrowShaftLength(AndroidEx.dpToPx(18));
		setArrowHeadLength(AndroidEx.dpToPx(6));
		setBarLength(AndroidEx.dpToPx(18));
		setGapSize(AndroidEx.dpToPx(4));

		setPosition(1f);
	}

	public void setPosition(float position) {
		if (position == 1f) {
			setVerticalMirror(true);
		} else if (position == 0f) {
			setVerticalMirror(false);
		}
		setProgress(position);
	}

	public float getPosition() {
		return getProgress();
	}
}
