package com.ilusons.harmony.ref;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class AndroidTouchEx {

	public static class OnSwipeTouchListener implements View.OnTouchListener {

		private final GestureDetector gestureDetector = new GestureDetector(new GestureListener());

		public boolean onTouch(final View v, final MotionEvent event) {
			return gestureDetector.onTouchEvent(event);
		}

		private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

			private static final int SWIPE_THRESHOLD = 100;
			private static final int SWIPE_VELOCITY_THRESHOLD = 100;

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				boolean result = false;
				try {
					float diffY = e2.getY() - e1.getY();
					float diffX = e2.getX() - e1.getX();
					if (Math.abs(diffX) > Math.abs(diffY)) {
						if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
							if (diffX > 0) {
								result = onSwipeRight();
							} else {
								result = onSwipeLeft();
							}
						}
					} else {
						if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
							if (diffY > 0) {
								result = onSwipeBottom();
							} else {
								result = onSwipeTop();
							}
						}
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				}
				return result;
			}
		}

		public boolean onSwipeRight() {
			return false;
		}

		public boolean onSwipeLeft() {
			return false;
		}

		public boolean onSwipeTop() {
			return false;
		}

		public boolean onSwipeBottom() {
			return false;
		}

	}

	public static class SwipeEvents implements View.OnTouchListener {
		private SwipeCallback swipeCallback;
		private SwipeSingleCallback swipeSingleCallback;
		private SwipeDirection detectSwipeDirection;

		float x1, x2, y1, y2;
		View view;

		private static SwipeEvents newInstance() {
			return new SwipeEvents();
		}

		public static void detect(View view, SwipeCallback swipeCallback) {
			SwipeEvents evt = SwipeEvents.newInstance();
			evt.swipeCallback = swipeCallback;
			evt.view = view;
			evt.detect();
		}

		public static void detectTop(View view, SwipeSingleCallback swipeSingleCallback) {
			SwipeEvents evt = SwipeEvents.newInstance();
			evt.swipeSingleCallback = swipeSingleCallback;
			evt.view = view;
			evt.detectSingle(SwipeDirection.TOP);
		}

		public static void detectRight(View view, SwipeSingleCallback swipeSingleCallback) {
			SwipeEvents evt = SwipeEvents.newInstance();
			evt.swipeSingleCallback = swipeSingleCallback;
			evt.view = view;
			evt.detectSingle(SwipeDirection.RIGHT);
		}

		public static void detectBottom(View view, SwipeSingleCallback swipeSingleCallback) {
			SwipeEvents evt = SwipeEvents.newInstance();
			evt.swipeSingleCallback = swipeSingleCallback;
			evt.view = view;
			evt.detectSingle(SwipeDirection.BOTTOM);
		}

		public static void detectLeft(View view, SwipeSingleCallback swipeSingleCallback) {
			SwipeEvents evt = SwipeEvents.newInstance();
			evt.swipeSingleCallback = swipeSingleCallback;
			evt.view = view;
			evt.detectSingle(SwipeDirection.LEFT);
		}

		private void detect() {
			view.setOnTouchListener(this);
		}

		private void detectSingle(SwipeDirection direction) {
			this.detectSwipeDirection = direction;
			view.setOnTouchListener(this);
		}

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					x1 = event.getX();
					y1 = event.getY();
					break;
				case MotionEvent.ACTION_UP:
					x2 = event.getX();
					y2 = event.getY();
					SwipeDirection direction = null;

					float xDiff = x2 - x1;
					float yDiff = y2 - y1;
					if (Math.abs(xDiff) > Math.abs(yDiff)) {
						if (x1 < x2) {
							direction = SwipeDirection.RIGHT;
						} else {
							direction = SwipeDirection.LEFT;
						}
					} else {
						if (y1 > y2) {
							direction = SwipeDirection.TOP;
						} else {
							direction = SwipeDirection.BOTTOM;
						}
					}

					// Only trigger the requested event only if there
					if (detectSwipeDirection != null && swipeSingleCallback != null) {
						if (direction == detectSwipeDirection) {
							swipeSingleCallback.onSwipe();
						}
					} else {
						if (direction == SwipeDirection.RIGHT) {
							swipeCallback.onSwipeRight();
						}
						if (direction == SwipeDirection.LEFT) {
							swipeCallback.onSwipeLeft();
						}
						if (direction == SwipeDirection.TOP) {
							swipeCallback.onSwipeTop();
						}
						if (direction == SwipeDirection.BOTTOM) {
							swipeCallback.onSwipeBottom();
						}
					}

					break;
			}
			return false;
		}

	}

	public enum SwipeDirection {
		TOP, RIGHT, BOTTOM, LEFT
	}

	public interface SwipeCallback {
		public void onSwipeTop();

		public void onSwipeRight();

		public void onSwipeBottom();

		public void onSwipeLeft();
	}

	public interface SwipeSingleCallback {
		public void onSwipe();
	}

}
