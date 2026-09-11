package me.everything.android.ui.overscroll;

public final class ListenerStubs {
	private ListenerStubs() {
	}

	public static class OverScrollUpdateListenerStub implements IOverScrollUpdateListener {
		@Override
		public void onOverScrollUpdate(IOverScrollDecor decor, int state, float offset) {
		}
	}
}
