package com.ilusons.ref.threading;

import com.ilusons.harmony.ref.threading.CancelableTask;

public class TestCancelableTask extends CancelableTask {

    private boolean mDidRun;
    private Thread mWorkThread;

    @Override
    protected void doWork() {
        mDidRun = true;
        mWorkThread = Thread.currentThread();
    }

    public boolean didRun() {
        return mDidRun;
    }

    public Thread getWorkThread() {
        return mWorkThread;
    }
}
