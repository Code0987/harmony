package com.ilusons.harmony.base;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ilusons.harmony.R;

public class BaseActivity extends AppCompatActivity {

    // Logger TAG
    private static final String TAG = BaseActivity.class.getSimpleName();

    // Events
    protected Handler handler = new Handler();

    // Services
    private MusicService musicService;
    private boolean isMusicServiceBound = false;
    ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.ServiceBinder binder = (MusicService.ServiceBinder) service;
            musicService = binder.getService();
            isMusicServiceBound = true;

            OnMusicServiceChanged(className, musicService, isMusicServiceBound);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            isMusicServiceBound = false;

            OnMusicServiceChanged(className, musicService, isMusicServiceBound);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Start service
        startService(new Intent(this, MusicService.class));

        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

    }

    @Override
    protected void onStart() {

        super.onStart();

        // Bind service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {

        super.onStop();

        // Unbind service
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }

    }

    public MusicService getMusicService() {
        return musicService;
    }

    protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {

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
            final Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_INDEFINITE);
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

    /**
     * Shows alert dialog
     */
    public void infoDialog(String content, String title, final DialogInterface.OnClickListener onClickListener) {
        (new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
                .setTitle(title)
                .setMessage(content)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (onClickListener != null)
                            onClickListener.onClick(dialogInterface, i);
                    }
                }))
                .show();
    }

    public void infoDialog(String content) {
        infoDialog(content, "Information", null);
    }

}
