package com.ilusons.harmony.splash;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.ref.permissions.PermissionsManager;
import com.ilusons.harmony.ref.permissions.PermissionsResultAction;

public class SplashActivity extends Activity {

	// Logger TAG
	private static final String TAG = SplashActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set splash theme

		// This way (theme defined in styles.xml, with no layout of activity) makes loading faster as styles pre-applied
		// Then we wait while main view is created, finally exiting splash
		setTheme(R.style.SplashTheme);

		super.onCreate(savedInstanceState);

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
	private static final String[] PERMISSIONS = new String[]{
			android.Manifest.permission.READ_EXTERNAL_STORAGE,
			android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
			android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
			android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
			android.Manifest.permission.WAKE_LOCK,
			android.Manifest.permission.INTERNET,
			android.Manifest.permission.ACCESS_NETWORK_STATE,
			android.Manifest.permission.SET_WALLPAPER,
			android.Manifest.permission.SET_WALLPAPER_HINTS,
			// "com.android.vending.BILLING",
			// "com.android.vending.CHECK_LICENSE"
	};

	private boolean checkPermissions() {
		boolean result = true;

		for (String permission : PERMISSIONS)
			result &= ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;

		return result;
	}

	private void requestPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
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

		finish();
	}

}
