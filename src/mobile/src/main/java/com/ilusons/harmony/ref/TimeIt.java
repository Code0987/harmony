package com.ilusons.harmony.ref;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.Callable;

public class TimeIt implements AutoCloseable {

	private static final String TAG = TimeIt.class.getSimpleName();

	private String tag = TimeIt.class.getSimpleName();

	private ArrayList<Long> splits = new ArrayList<>();
	private ArrayList<String> splitTags = new ArrayList<>();

	public void reset(String tag) {
		splits.clear();

		this.tag = tag;

		split("[Initial]");
	}

	public void split(String tag) {
		splits.add(System.nanoTime());
		splitTags.add(tag);
	}

	public String print() {
		split("[Final]");

		StringBuilder sb = new StringBuilder();

		for (int i = 1; i < splits.size(); i++) {
			long seconds = ((splits.get(i) - splits.get(i - 1)) / 1000) % 60;

			sb.append(tag)
					.append(":\t")
					.append(String.format(Locale.ENGLISH, "0.%d seconds", seconds))
					.append("\t")
					.append(splitTags.get(i))
					.append(System.lineSeparator());
		}

		return sb.toString();
	}

	public void printToLog() {
		Log.d(TAG, print());
	}

	@Override
	public void close() {
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

	public static class FPS {

		private LinkedList<Long> fpsTimes = new LinkedList<Long>() {{
			add(System.nanoTime());
		}};

		public double getFPS() {
			long lastTime = System.nanoTime();
			double difference = (lastTime - fpsTimes.getFirst()) / 1000000000.0;
			fpsTimes.addLast(lastTime);
			int size = fpsTimes.size();
			if (size > 10) {
				fpsTimes.removeFirst();
			}
			return difference > 0 ? fpsTimes.size() / difference : 0.0;
		}

	}

}
