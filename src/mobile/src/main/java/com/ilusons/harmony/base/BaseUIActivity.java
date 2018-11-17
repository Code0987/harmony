package com.ilusons.harmony.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ilusons.harmony.R;

public abstract class BaseUIActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = BaseUIActivity.class.getSimpleName();

	// Components
	PowerManager.WakeLock wakeLockForScreenOn;

	// Events
	BaseMediaBroadcastReceiver broadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Create wake lock
		try {
			wakeLockForScreenOn = ((PowerManager) getSystemService(Context.POWER_SERVICE))
					.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onCreate(savedInstanceState);

		// Broadcast receivers
		broadcastReceiver = new BaseMediaBroadcastReceiver(this) {

			@Override
			public void OnMusicServicePlay() {
				BaseUIActivity.this.OnMusicServicePlay();
			}

			@Override
			public void OnMusicServicePause() {
				BaseUIActivity.this.OnMusicServicePause();
			}

			@Override
			public void OnMusicServiceStop() {
				BaseUIActivity.this.OnMusicServiceStop();
			}

			@Override
			public void OnMusicServiceOpen(String uri) {
				BaseUIActivity.this.OnMusicServiceOpen(uri);
			}

			@Override
			public void OnMusicServicePrepared() {
				BaseUIActivity.this.OnMusicServicePrepared();
			}

			@Override
			public void OnMusicServiceLibraryUpdateBegins() {
				BaseUIActivity.this.OnMusicServiceLibraryUpdateBegins();
			}

			@Override
			public void OnMusicServiceLibraryUpdated() {
				BaseUIActivity.this.OnMusicServiceLibraryUpdated();
			}

			@Override
			public void OnMusicServicePlaylistChanged(String name) {
				BaseUIActivity.this.OnMusicServicePlaylistChanged(name);
			}

			@Override
			public void OnSearchQueryReceived(String query) {
				BaseUIActivity.this.OnSearchQueryReceived(query);
			}

			@Override
			public void OnMusicServiceSFXUpdated() {
				BaseUIActivity.this.OnMusicServiceSFXUpdated();
			}
		};

	}

	@Override
	protected void onDestroy() {

		super.onDestroy();

		// Release wake lock
		try {
			if (wakeLockForScreenOn.isHeld())
				wakeLockForScreenOn.release();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Intents
		try {
			broadcastReceiver.unRegister();
		} catch (final Throwable e) {
			Log.w(TAG, e);
		}
	}

	@Override
	protected void onStart() {

		super.onStart();

		// Bind service
		Intent intent = new Intent(this, MusicService.class);
		bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

		// Acquire wake lock
		try {
			wakeLockForScreenOn.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Events
		broadcastReceiver.register(this);

	}

	@Override
	protected void onStop() {

		super.onStop();

		// Release wake lock
		try {
			if (wakeLockForScreenOn.isHeld())
				wakeLockForScreenOn.release();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void OnMusicServicePlay() {

	}

	protected void OnMusicServicePause() {

	}

	protected void OnMusicServiceStop() {

	}

	public void OnMusicServiceOpen(String uri) {

	}

	protected void OnMusicServicePrepared() {

	}

	public void OnMusicServiceLibraryUpdateBegins() {

	}

	public void OnMusicServiceLibraryUpdated() {

	}

	public void OnMusicServicePlaylistChanged(String name) {

	}

	public void OnSearchQueryReceived(String query) {

	}

	public void OnMusicServiceSFXUpdated(){

	}

}
