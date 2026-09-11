package com.ilusons.harmony.splash;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

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
		switch (requestCode) {
			case REQUEST_PERMISSIONS:
				if (grantResults.length > 0) {
					boolean result = true;
					for (int gr : grantResults)
						result &= gr == PackageManager.PERMISSION_GRANTED;

					if (result)
						executePermissionsTask();
					else {
						try {
							Toast.makeText(SplashActivity.this, "Please grant all the required permissions :(", Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							Log.wtf(TAG, e);
						}

						(new Handler()).postDelayed(new Runnable() {
							@Override
							public void run() {
								finish();
							}
						}, 5000);
					}
				}
				break;
		}
	}

	private static final int REQUEST_PERMISSIONS = 786;
	private static String[] requiredPermissions() {
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

	private boolean checkPermissions() {
		String audio = Build.VERSION.SDK_INT >= 33
				? android.Manifest.permission.READ_MEDIA_AUDIO
				: android.Manifest.permission.READ_EXTERNAL_STORAGE;
		return ContextCompat.checkSelfPermission(this, audio) == PackageManager.PERMISSION_GRANTED;
	}

	private void requestPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestPermissions(requiredPermissions(), REQUEST_PERMISSIONS);
		} else {
			executePermissionsTask();
		}
	}

	private void executePermissionsTask() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!checkPermissions()) {
				requestPermissions();

				return;
			}
		}

		Intent intent = new Intent(SplashActivity.this, MainActivity.class);

		startActivity(intent);

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				finish();
			}
		}, 777);
	}

}
