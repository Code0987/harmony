package com.ilusons.harmony.ref;

import android.util.Log;

import org.acoustid.chromaprint.Chromaprint;

public class FingerprintEx {

	public static final String TAG = FingerprintEx.class.getSimpleName();

	public static String GenerateFingerprint(String path, Long length) {
		Chromaprint chromaprint = null;
		try {
			chromaprint = new Chromaprint();
			MediaEx.MediaDecoder decoder = new MediaEx.MediaDecoder(path);
			int sr = decoder.getSampleRate();
			int n = decoder.getChannels();
			chromaprint.start(sr, n);
			short[] samples;
			int ts = 0;
			while ((samples = decoder.readShortData()) != null) {
				chromaprint.feed(samples);
				ts += samples.length;
				if ((ts / (float) sr) >= (length - 1))
					break;
			}
			chromaprint.finish();

			String fp = chromaprint.getFingerprint();

			Log.d(TAG, "Fingerprint" + "\n" + path + "\n" + fp);

			return fp;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return null;
	}

}
