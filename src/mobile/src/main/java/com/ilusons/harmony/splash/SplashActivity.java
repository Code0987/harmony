package com.ilusons.harmony.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set splash theme

        // This way (theme defined in styles.xml, with no layout of activity) makes loading faster as styles pre-applied
        // Then we wait while main view is created, finally exiting splash
        setTheme(R.style.SplashTheme);

        super.onCreate(savedInstanceState);

        // Check if all permissions are granted
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
                this,
                new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                        android.Manifest.permission.WAKE_LOCK,
                        android.Manifest.permission.INTERNET,
                        android.Manifest.permission.ACCESS_NETWORK_STATE,
                        android.Manifest.permission.SET_WALLPAPER,
                        android.Manifest.permission.SET_WALLPAPER_HINTS,
                        "com.android.vending.BILLING",
                        "com.android.vending.CHECK_LICENSE"
                },
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        // info("All needed permissions have been granted :)");

                        // Start main
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onDenied(String permission) {
                        Toast.makeText(SplashActivity.this, "Please grant all the required permissions :(", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

}
