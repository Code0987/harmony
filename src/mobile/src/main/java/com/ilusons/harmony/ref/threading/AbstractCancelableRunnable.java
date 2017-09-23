package com.ilusons.harmony.ref.threading;

abstract class AbstractCancelableRunnable implements CancelableRunnable {

	private boolean mCanceled;

	@Override
	public void cancel() {
		mCanceled = true;
	}

	@Override
	public boolean isCanceled() {
		return mCanceled;
	}
}
