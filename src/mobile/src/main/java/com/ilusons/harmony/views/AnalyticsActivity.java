package com.ilusons.harmony.views;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ViewEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.lang.ref.WeakReference;

import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class AnalyticsActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = AnalyticsActivity.class.getSimpleName();

	private View root;

	private AVLoadingIndicatorView loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Set view
		setContentView(R.layout.analytics_activity);

		// Set views
		root = findViewById(R.id.root);

		loading = (AVLoadingIndicatorView) findViewById(R.id.loading);

		loading.smoothToShow();

		// Set close
		findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});

		// Set views and tabs
		final ViewEx.StaticViewPager viewPager = (ViewEx.StaticViewPager) findViewById(R.id.viewPager);
		TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);

		// last.fm Scrobbler
		createLFM();

		loading.smoothToHide();

	}

	//region LFM

	private ImageView analytics_lfm_status;
	private EditText analytics_lfm_username_editText;
	private EditText analytics_lfm_password_editText;
	private Button analytics_lfms_save;
	private TextView analytics_lfms_logs;

	private void createLFM() {
		analytics_lfm_status = findViewById(R.id.analytics_lfm_status);

		analytics_lfm_username_editText = findViewById(R.id.analytics_lfm_username_editText);
		analytics_lfm_username_editText.setText(Analytics.getInstance().getLastfmUsername());

		analytics_lfm_password_editText = findViewById(R.id.analytics_lfm_password_editText);
		analytics_lfm_password_editText.setText(Analytics.getInstance().getLastfmPassword());

		analytics_lfms_save = findViewById(R.id.analytics_lfms_save);
		analytics_lfms_save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String username = analytics_lfm_username_editText.getText().toString();
				String password = analytics_lfm_password_editText.getText().toString();

				Analytics.getInstance().setLastfmCredentials(username, password);

				updateLFM();
			}
		});

		analytics_lfms_logs = findViewById(R.id.analytics_lfms_logs);
		StringBuilder sb = new StringBuilder();
		for (ScrobbleResult sr : Analytics.getInstance().getScrobblerResultsForLastfm()) {
			sb.append(sr.toString()).append(System.lineSeparator());
		}
		analytics_lfms_logs.setText(sb.toString());

		updateLFMState();
		updateLFM();
	}

	private void updateLFM() {

		(new RefreshLFM(this)).execute();

	}

	private void updateLFMState() {
		Session session = Analytics.getInstance().getLastfmSession();
		if (session == null) {
			analytics_lfm_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
			analytics_lfm_status.setImageResource(R.drawable.ic_error_outline_black);
		} else {
			analytics_lfm_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
			analytics_lfm_status.setImageResource(R.drawable.ic_settings_remote_black);
		}
	}

	private static class RefreshLFM extends AsyncTask<Void, Void, Void> {
		private WeakReference<AnalyticsActivity> contextRef;

		public RefreshLFM(AnalyticsActivity context) {
			this.contextRef = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(Void... voids) {

			Session session = Analytics.getInstance().getLastfmSession();
			if (session == null)
				Analytics.getInstance().initLastfm();

			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			AnalyticsActivity context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToShow();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			AnalyticsActivity context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToHide();

			Session session = Analytics.getInstance().getLastfmSession();
			if (session == null) {
				Toast.makeText(context, "Failed to connect to last.fm!", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "Scrobbler is active now!", Toast.LENGTH_LONG).show();
			}

			context.updateLFMState();
		}
	}

	//endregion

}
