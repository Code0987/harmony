package com.ilusons.harmony.ref.threading;

public interface CancelableRunnable extends Runnable {

	void cancel();

	boolean isCanceled();
}
