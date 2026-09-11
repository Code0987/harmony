package com.ilusons.harmony.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;

public class SplashActivity extends Activity {

	// Logger TAG
	private static final String TAG = SplashActivity.class.getSimpleName();

	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set splash theme

		// This way (theme defined in styles.xml, with no layout of activity) makes loading faster as styles pre-applied
		// Then we wait while main view is created, finally exiting splash
		setTheme(R.style.SplashTheme);

		super.onCreate(savedInstanceState);

		handler = new Handler();

		executePermissionsTask();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode != REQUEST_PERMISSIONS) {
			return;
		}
		// Audio is the only gate. Notifications may be denied.
		continueToApp();
	}

	private static final int REQUEST_PERMISSIONS = 786;

	private static String[] requestablePermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			return new String[]{
					android.Manifest.permission.READ_MEDIA_AUDIO,
					android.Manifest.permission.POST_NOTIFICATIONS
			};
		}
		return new String[]{
				android.Manifest.permission.READ_EXTERNAL_STORAGE
		};
	}

	private void executePermissionsTask() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestPermissions(requestablePermissions(), REQUEST_PERMISSIONS);
			return;
		}
		continueToApp();
	}

	private void continueToApp() {
		startActivity(new Intent(SplashActivity.this, MainActivity.class));
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		}, 777);
	}

}
