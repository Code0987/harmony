package com.ilusons.harmony.ref;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

public class TimeIt implements AutoCloseable {

	private static final String TAG = TimeIt.class.getSimpleName();

	private String tag = TimeIt.class.getSimpleName();

	private ArrayList<Long> splits = new ArrayList<>();

	public void reset(String tag) {
		splits.clear();

		this.tag = tag;

		split();
	}

	public void split() {
		splits.add(System.nanoTime());
	}

	public String print() {
		split();

		StringBuilder sb = new StringBuilder();

		for (int i = 1; i < splits.size(); i++) {
			long seconds = ((splits.get(i) - splits.get(i - 1)) / 1000) % 60;

			sb.append(tag).append(":\t").append(String.format(Locale.ENGLISH, "0.%d seconds", seconds)).append(System.lineSeparator());
		}

		return sb.toString();
	}

	public void printToLog() {
		Log.d(TAG, print());
	}

	@Override
	public void close() throws Exception {
		printToLog();
	}

	public static <T> T measureFor(String tag, Callable<T> task) {
		T call = null;
		try (TimeIt timeIt = new TimeIt()) {
			timeIt.reset(tag);
			call = task.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return call;
	}

}
