package com.ilusons.harmony.base;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.ilusons.harmony.R;

public class BaseActivity extends AppCompatActivity {

    // Logger TAG
    private static final String TAG = BaseActivity.class.getSimpleName();

    // Events
    protected Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Check if all permissions are granted
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this,
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        // info("All needed permissions have been granted :)");
                    }

                    @Override
                    public void onDenied(String permission) {
                        info("Please grant all the required permissions :(");

                        // TODO: Better handle this

                        finish();
                    }
                });

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    /**
     * Shows toast or snack bar as per appropriate
     * Root view should have id of root to show proper snack bar
     *
     * @param s content to show
     */
    public void info(String s) {
        View view = findViewById(R.id.root);

        if (view != null) {
            final Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            if (snackbarView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
                p.setMargins(p.leftMargin,
                        p.topMargin,
                        p.rightMargin,
                        p.bottomMargin);
                snackbarView.requestLayout();
            }
            snackbar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        } else {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }
    }

}
