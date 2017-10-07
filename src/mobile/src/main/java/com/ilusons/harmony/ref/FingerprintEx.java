package com.ilusons.harmony.ref;

import android.os.AsyncTask;
import android.util.Log;

import org.acoustid.chromaprint.Chromaprint;

import java.util.concurrent.CancellationException;

public class FingerprintEx {

	public static final String TAG = FingerprintEx.class.getSimpleName();

	public static class GenerateFingerprintAsyncTask extends AsyncTask<Object, Object, String> {

		private final String path;
		private final JavaEx.ActionT<String> onSuccess;
		private final JavaEx.ActionT<Exception> onError;

		public GenerateFingerprintAsyncTask(String path, JavaEx.ActionT<String> onSuccess, JavaEx.ActionT<Exception> onError) {
			this.path = path;
			this.onSuccess = onSuccess;
			this.onError = onError;
		}

		@Override
		protected void onPostExecute(String result) {
			if (onSuccess != null)
				onSuccess.execute(result);
		}

		@Override
		protected String doInBackground(Object... objects) {
			Chromaprint chromaprint = null;
			try {
				if (isCancelled())
					throw new CancellationException();

				chromaprint = new Chromaprint();
				MediaEx.MediaDecoder decoder = new MediaEx.MediaDecoder(path);
				chromaprint.start(decoder.getSampleRate(), 1);
				short[] samples;
				while ((samples = decoder.readShortData()) != null) {
					chromaprint.feed(samples);
				}
				String fp = chromaprint.getFingerprint();

				if (onSuccess != null)
					onSuccess.execute(fp);

				Log.d(TAG, "Fingerprint" + "\n" + path + "\n" + fp);

				return fp;
			} catch (Exception e) {
				Log.w(TAG, e);

				if (onError != null)
					onError.execute(e);
			} finally {
				try {
					if (chromaprint != null)
						chromaprint.finish();
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
			return null;
		}

	}

}
