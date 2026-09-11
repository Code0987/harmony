package me.everything.android.ui.overscroll;

public interface IOverScrollState {
	int STATE_IDLE = 0;
	int STATE_DRAG_START_SIDE = 1;
	int STATE_DRAG_END_SIDE = 2;
	int STATE_BOUNCE_BACK = 3;
}
