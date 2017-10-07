package com.ilusons.harmony.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.FingerprintEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.MediaEx;

import org.acoustid.chromaprint.Chromaprint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Api {

	// Logger TAG
	private static final String TAG = Api.class.getSimpleName();

	public static class LookupFingerprintDataAsyncTask extends AsyncTask<Object, Object, Map<String, String>> {

		private final String path;
		private final Long length;
		private final JavaEx.ActionT<Map<String, String>> onSuccess;
		private final JavaEx.ActionT<Exception> onError;

		public LookupFingerprintDataAsyncTask(String path, Long length, JavaEx.ActionT<Map<String, String>> onSuccess, JavaEx.ActionT<Exception> onError) {
			this.path = path;
			this.length = length;
			this.onSuccess = onSuccess;
			this.onError = onError;
		}

		@Override
		protected void onPostExecute(Map<String, String> result) {
			if (onSuccess != null)
				onSuccess.execute(result);
		}

		@Override
		protected Map<String, String> doInBackground(Object... objects) {
			Map<String, String> result = new HashMap<>();
			try {
				if (isCancelled())
					throw new CancellationException();

				// Generate fingerprint
				FingerprintEx.GenerateFingerprintAsyncTask gfat = new FingerprintEx.GenerateFingerprintAsyncTask(
						path, null, null);
				String fp = gfat.get(30, TimeUnit.SECONDS);

				// Lookup fingerprint

				/*
				{
				  "status": "ok",
				  "results": [{
				    "score": 1.0,
				    "id": "9ff43b6a-4f16-427c-93c2-92307ca505e0",
				    "recordings": [{
				      "duration": 639,
				      "releasegroups": [{
				        "type": "Album",
				        "id": "ddaa2d4d-314e-3e7c-b1d0-f6d207f5aa2f",
				        "title": "Before the Dawn Heals Us"
				      }],
				      "title": "Lower Your Eyelids to Die With the Sun",
				      "id": "cd2e7c47-16f5-46c6-a37c-a1eb7bf599ff",
				      "artists": [{
				        "id": "6d7b7cd4-254b-4c25-83f6-dd20f98ceacd",
				        "name": "M83"
				      }]
				    }]
				  }]
				}
				*/

				JSONObject lr = lookupFingerprintFromAcoustId(fp, length);

				if (!lr.get("status").toString().equalsIgnoreCase("ok"))
					throw new Exception("Status not Ok.");

				JSONArray lr_results = lr.getJSONArray("results");
				if (lr_results == null || lr_results.length() == 0)
					throw new Exception("No items found.");

				JSONObject lr_results_0 = lr_results.getJSONObject(0);
				String id = lr_results_0.getString("id");
				double score = lr_results_0.getDouble("score");

				JSONArray lr_results_0_recordings = lr_results_0.getJSONArray("recordings");
				JSONObject lr_results_0_recordings_0 = lr_results_0_recordings.getJSONObject(0);
				String title = lr_results_0_recordings_0.getString("title");

				JSONArray lr_results_0_recordings_0_artists = lr_results_0_recordings_0.getJSONArray("artists");
				JSONObject lr_results_0_recordings_0_artists_0 = lr_results_0_recordings_0_artists.getJSONObject(0);
				String artist = lr_results_0_recordings_0_artists_0.getString("name");

				JSONArray lr_results_0_recordings_0_releasegroups = lr_results_0_recordings_0.getJSONArray("releasegroups");
				JSONObject lr_results_0_recordings_0_releasegroups_0 = lr_results_0_recordings_0_releasegroups.getJSONObject(0);
				String album = lr_results_0_recordings_0_releasegroups_0.getString("title");

				result.put("id", id);
				result.put("score", String.valueOf(score));
				result.put("title", title);
				result.put("artist", artist);
				result.put("album", album);

			} catch (Exception e) {
				Log.w(TAG, e);

				if (onError != null)
					onError.execute(e);
			}
			return result;
		}

		public JSONObject lookupFingerprintFromAcoustId(String fingerprint, Long length) {
			String responseData = null;
			URLConnection uc = null;
			BufferedReader br = null;
			try {
				URL url = new URL("https://api.acoustid.org/v2/lookup?format=json&client=iwj8azntPz&duration=" + length + "&fingerprint=" + fingerprint + "&meta=recordings+releasegroups+compress");
				uc = url.openConnection();
				br = new BufferedReader(new InputStreamReader(uc.getInputStream()));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				responseData = sb.toString();

				JSONObject jo = new JSONObject(responseData);

				Log.d(TAG, jo.toString());

				return jo;
			} catch (Exception e) {
				Log.w(TAG, e);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

	}

	public static void lookupAndUpdateMusicData(final MusicService musicService, final Music data, final JavaEx.ActionT<Music> onSuccess, final JavaEx.ActionT<Exception> onError) {
		LookupFingerprintDataAsyncTask asyncTask = new LookupFingerprintDataAsyncTask(
				data.getPath(),
				(long) data.getLength(),
				new JavaEx.ActionT<Map<String, String>>() {
					@Override
					public void execute(final Map<String, String> result) {
						try (Realm realm = DB.getDB()) {
							if (realm == null)
								return;

							realm.executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(Realm realm) {
									data.setTitle(result.get("title"));
									realm.insertOrUpdate(data);
								}
							});
						}

						if (onSuccess != null)
							onSuccess.execute(data);
					}
				},
				new JavaEx.ActionT<Exception>() {
					@Override
					public void execute(Exception e) {
						if (onError != null)
							onError.execute(e);
					}
				});
		asyncTask.execute();
	}

}
