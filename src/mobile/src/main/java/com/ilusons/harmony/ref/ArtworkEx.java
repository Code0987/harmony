package com.ilusons.harmony.ref;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ArtworkEx {

	public static final String TAG = ArtworkEx.class.getSimpleName();

	public enum ArtworkType {
		None(""),
		Song("song"),
		Album("album"),
		Artist("musicArtist"),
		Genre("genre");

		private String value;

		ArtworkType(String value) {
			this.value = value;
		}
	}

	public static class ArtworkDownloaderAsyncTask extends AsyncTask<Object, Object, Bitmap> {

		private WeakReference<Context> contextRef;
		private final String query;
		private final ArtworkType artworkType;
		private final int size;
		private String memoryCacheKey;
		private String diskCacheDir;
		private String diskCacheKey;
		private JavaEx.ActionT<Bitmap> onSuccess;
		private JavaEx.ActionT<Exception> onError;
		private int timeout;
		private boolean forceDownload;

		public ArtworkDownloaderAsyncTask(Context context, String query, ArtworkType artworkType, int size, String memoryCacheKey, String diskCacheDir, String diskCacheKey, JavaEx.ActionT<Bitmap> onSuccess, JavaEx.ActionT<Exception> onError, int timeout, boolean forceDownload) {
			this.contextRef = new WeakReference<Context>(context);
			this.query = query;
			this.artworkType = artworkType;
			this.size = size;
			this.memoryCacheKey = memoryCacheKey;
			if (size > 0)
				this.memoryCacheKey = memoryCacheKey + size;
			this.diskCacheDir = diskCacheDir;
			this.diskCacheKey = diskCacheKey;
			this.onSuccess = onSuccess;
			this.onError = onError;
			this.timeout = timeout;
			this.forceDownload = forceDownload;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (onSuccess != null)
				onSuccess.execute(bitmap);
		}

		@Override
		protected Bitmap doInBackground(Object... objects) {
			try {
				if (isCancelled() || contextRef.get() == null)
					throw new CancellationException();

				Bitmap result = null;

				if (!forceDownload)
					result = loadExisting();

				// File
				File file = IOEx.getDiskCacheFile(contextRef.get(), diskCacheDir, diskCacheKey);

				if (isCancelled())
					throw new CancellationException();

				// Download and cache to folder then load
				if (result == null) {
					try {
						URL url = new URL(String.format(
								"https://itunes.apple.com/search?term=%s&entity=song&media=music",
								URLEncoder.encode(query, "UTF-8")));

						Connection connection = Jsoup.connect(url.toExternalForm())
								.timeout(timeout)
								.ignoreContentType(true);

						Document document = connection.get();

						JsonObject response = new JsonParser().parse(document.text()).getAsJsonObject();

						JsonArray results = response.getAsJsonArray("results");

						int s = size;
						if (s < 0)
							s = 600;

						String downloadUrl = results
								.get(0)
								.getAsJsonObject()
								.get("artworkUrl60")
								.getAsString()
								.replace("60x60bb.jpg", s + "x" + s + "bb.jpg");

						BufferedInputStream in = null;
						FileOutputStream out = null;
						try {
							in = new BufferedInputStream(new URL(downloadUrl).openStream());
							out = new FileOutputStream(file.getAbsoluteFile());

							final byte data[] = new byte[1024];
							int count;
							while ((count = in.read(data, 0, 1024)) != -1) {
								out.write(data, 0, count);
							}
						} finally {
							if (in != null) {
								in.close();
							}
							if (out != null) {
								out.close();
							}
						}

					} catch (Exception e) {
						Log.w(TAG, e);
					}

					if (file.exists())
						result = BitmapFactory.decodeFile(file.getAbsolutePath());

					// Resample
					if (result != null) {
						try {
							Bitmap.Config config = result.getConfig();
							if (config == null) {
								config = Bitmap.Config.ARGB_8888;
							}
							result = result.copy(config, false);
						} catch (Exception e) {
							Log.w(TAG, e);
						}

						// Put in cache
						if (memoryCacheKey != null)
							CacheEx.getInstance().putBitmap(memoryCacheKey, result);
					}

				}

				return result;
			} catch (Exception e) {
				Log.w(TAG, e);

				if (onError != null)
					onError.execute(e);
			}
			return null;
		}

		private Bitmap loadExisting() {
			Bitmap result;

			// Load from cache
			result = CacheEx.getInstance().getBitmap(memoryCacheKey);

			if (result != null)
				return result;

			// File
			File file = IOEx.getDiskCacheFile(contextRef.get(), diskCacheDir, diskCacheKey);

			// Load from cache folder
			if (file.exists())
				result = BitmapFactory.decodeFile(file.getAbsolutePath());

			// Re-sample
			if (result != null) {
				try {
					Bitmap.Config config = result.getConfig();
					if (config == null) {
						config = Bitmap.Config.ARGB_8888;
					}
					result = result.copy(config, false);
				} catch (Exception e) {
					Log.w(TAG, e);
				}

				// Re-size
				if (result != null && size > 0) {
					int sz = Math.min(size, Math.max(result.getWidth(), result.getHeight()));

					result = Bitmap.createScaledBitmap(result, sz, sz, true);
				}

				// Put in cache
				CacheEx.getInstance().putBitmap(memoryCacheKey, result);
			}

			return result;
		}

	}

	private static ThreadPoolExecutor artworkDownloaderTaskExecutor = null;

	public static void getArtworkDownloaderTask(Context context, String query, ArtworkType artworkType, int size, String memoryCacheKey, String diskCacheDir, String diskCacheKey, JavaEx.ActionT<Bitmap> onSuccess, JavaEx.ActionT<Exception> onError, int timeout, boolean forceDownload) {
		if (artworkDownloaderTaskExecutor == null) {
			int CORES = Runtime.getRuntime().availableProcessors();

			artworkDownloaderTaskExecutor = new ThreadPoolExecutor(
					3,
					5,
					7L,
					TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>());

			artworkDownloaderTaskExecutor.allowCoreThreadTimeOut(true);
		}

		ArtworkDownloaderAsyncTask artworkDownloaderAsyncTask = (new ArtworkEx.ArtworkDownloaderAsyncTask(
				context,
				query,
				artworkType,
				size,
				memoryCacheKey,
				diskCacheDir,
				diskCacheKey,
				onSuccess,
				onError,
				timeout,
				forceDownload));

		artworkDownloaderAsyncTask.executeOnExecutor(artworkDownloaderTaskExecutor);
	}

}
