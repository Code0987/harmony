package com.ilusons.harmony.ref;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Permissions {
	private static final String TAG = Permissions.class.getName();

	public static final String KEY_RESULT_RECEIVER = "resultReceiver";
	public static final String KEY_PERMISSIONS = "permissions";
	public static final String KEY_GRANT_RESULTS = "grantResults";
	public static final String KEY_REQUEST_CODE = "requestCode";

	@NonNull
	public static <T extends Context> void requestPermissions(@NonNull final T context, String[] permissions, int requestCode, final OnRequestPermissionsResultCallback callback) {
		ResultReceiver resultReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				String[] outPermissions = resultData.getStringArray(KEY_PERMISSIONS);
				int[] grantResults = resultData.getIntArray(KEY_GRANT_RESULTS);
				callback.onRequestPermissionsResult(resultCode, outPermissions, grantResults);
			}
		};

		Intent permIntent = new Intent(context, PermissionRequestActivity.class);
		permIntent.putExtra(KEY_RESULT_RECEIVER, resultReceiver);
		permIntent.putExtra(KEY_PERMISSIONS, permissions);
		permIntent.putExtra(KEY_REQUEST_CODE, requestCode);
		permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		context.startActivity(permIntent);
	}

	@NonNull
	public static <T extends Activity & OnRequestPermissionsResultCallback> void requestPermission(@NonNull final T context, String permission, int requestCode) {
		if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {
				ActivityCompat.requestPermissions(context, new String[]{ permission }, requestCode);
			}
		}
	}

	@NonNull
	public static <T extends Activity & OnRequestPermissionsResultCallback> void requestPermissions(@NonNull final T context, String[] permission, int requestCode) {
		for (String p : permission) {
			requestPermission(context, p, requestCode);
		}
	}

	@NonNull
	public static synchronized <T extends Context> String[] getManifestPermissions(@NonNull final T context) {
		PackageInfo packageInfo = null;
		List<String> list = new ArrayList<>(1);

		try {
			Log.d(TAG, context.getPackageName());

			packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "A problem occurred when retrieving permissions", e);
		}

		if (packageInfo != null) {
			String[] permissions = packageInfo.requestedPermissions;

			if (permissions != null) {
				for (String perm : permissions) {
					Log.d(TAG, "Manifest contained permission: " + perm);

					list.add(perm);
				}
			}
		}

		return list.toArray(new String[list.size()]);
	}

	@SuppressWarnings("unused")
	public static synchronized boolean hasPermission(@Nullable Context context, @NonNull String permission) {
		return context != null
				&&
			(ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
	}

	@SuppressWarnings("unused")
	public static synchronized boolean hasPermissions(@Nullable Context context, @NonNull String[] permissions) {
		if (context == null) {
			return false;
		}

		boolean does = true;

		for (String p : permissions) {
			does &= hasPermission(context, p);
		}

		return does;
	}

	public static class PermissionRequestActivity extends AppCompatActivity {
		ResultReceiver resultReceiver;
		String[] permissions;
		int requestCode;

		@Override
		public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
			for (int i = 0; i<permissions.length; i++) {
				Log.d(TAG, permissions[i] + "=" + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
			}

			Bundle resultData = new Bundle();
			resultData.putStringArray(KEY_PERMISSIONS, permissions);
			resultData.putIntArray(KEY_GRANT_RESULTS, grantResults);
			resultReceiver.send(requestCode, resultData);
			finish();
		}

		@Override
		protected void onStart() {
			super.onStart();

			resultReceiver = this.getIntent().getParcelableExtra(KEY_RESULT_RECEIVER);
			permissions = this.getIntent().getStringArrayExtra(KEY_PERMISSIONS);
			requestCode = this.getIntent().getIntExtra(KEY_REQUEST_CODE, 0);

			ActivityCompat.requestPermissions(this, permissions, requestCode);
		}
	}

}