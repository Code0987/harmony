package com.ilusons.harmony.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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

        // Start main
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
