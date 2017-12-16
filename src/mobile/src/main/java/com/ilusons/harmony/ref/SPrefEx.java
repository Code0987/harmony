package com.ilusons.harmony.ref;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class SPrefEx {

	// Logger TAG
	private static final String TAG = SPrefEx.class.getSimpleName();

	public static String TAG_SPREF = "spref";
	public static String TAG_SPREF_FIRSTRUN = "first_run";

	public static SharedPreferences get(final Context context) {
		SharedPreferences spref = context.getSharedPreferences(TAG_SPREF, MODE_PRIVATE);
		return spref;
	}

	public static boolean getFirstRun(final Context context) {
		return get(context).getBoolean(TAG_SPREF_FIRSTRUN, false);
	}

	public static void setFirstRun(final Context context, boolean value) {
		SharedPreferences.Editor editor = get(context).edit();
		editor.putBoolean(TAG_SPREF_FIRSTRUN, value);
		editor.apply();
	}

	@SuppressWarnings({"unchecked"})
	public static boolean importSPrefs(Context context, InputStream is, Collection<String> keys) {
		boolean r = false;

		try {
			String data = IOUtils.toString(is, StandardCharsets.UTF_8);

			SharedPreferences.Editor prefEdit = SPrefEx.get(context).edit();

			Map<String, ?> entries = (new Gson()).fromJson(data, new TypeToken<Map<String, ?>>() {
			}.getType());
			if (keys != null)
				entries.keySet().retainAll(keys);

			for (Map.Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, (Boolean) v);
				else if (v instanceof Float)
					prefEdit.putFloat(key, (Float) v);
				else if (v instanceof Integer)
					prefEdit.putInt(key, (Integer) v);
				else if (v instanceof Long)
					prefEdit.putLong(key, (Long) v);
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}

			prefEdit.apply();

			r = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return r;
	}

	public static boolean exportSPrefs(Context context, OutputStream os, Uri uri, Collection<String> keys) {
		boolean r = false;

		try {
			SharedPreferences pref = SPrefEx.get(context);

			Map<String, ?> map = pref.getAll();
			map.keySet().retainAll(keys);

			String data = (new Gson()).toJson(map, new TypeToken<Map<String, ?>>() {
			}.getType());

			Log.d(TAG, "Export\n" + data);

			IOUtils.write(data, os, StandardCharsets.UTF_8);

			r = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return r;
	}

}
