package com.ilusons.harmony.views;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
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
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ViewEx;
import com.wang.avi.AVLoadingIndicatorView;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleResult;
import io.realm.Realm;

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

		// Charts
		createCharts();

		// DC
		createDC();

		// last.fm Scrobbler
		createLFM();

		loading.smoothToHide();

	}

	//region Charts

	private void createCharts() {
		PieChart chart = (PieChart) findViewById(R.id.analytics_charts_c1);

		final int N = 5;

		chart.setUsePercentValues(true);
		chart.getDescription().setEnabled(false);
		chart.setExtraOffsets(3, 3, 3, 3);
		chart.setDragDecelerationFrictionCoef(0.95f);

		chart.setDrawHoleEnabled(true);
		chart.setHoleColor(ContextCompat.getColor(this, R.color.transparent));

		chart.setTransparentCircleColor(ContextCompat.getColor(this, R.color.translucent_icons));
		chart.setTransparentCircleAlpha(110);

		chart.setHoleRadius(24f);
		chart.setTransparentCircleRadius(27f);

		chart.setRotationAngle(0);
		chart.setRotationEnabled(true);
		chart.setHighlightPerTapEnabled(true);

		chart.setDrawCenterText(true);
		chart.setCenterText("Top " + N);
		chart.setCenterTextColor(ContextCompat.getColor(this, R.color.primary_text));
		chart.setCenterTextSize(16f);

		chart.setDrawEntryLabels(true);
		chart.setEntryLabelColor(ContextCompat.getColor(this, R.color.primary_text));
		chart.setEntryLabelTextSize(10f);

		Legend l = chart.getLegend();
		l.setEnabled(false);

		List<Music> allSortedByScore = Music.getAllSortedByScore(N);
		float score_total = 0;
		for (Music one : allSortedByScore)
			score_total += one.Score;

		ArrayList<Integer> colors = new ArrayList<Integer>();
		int defaultColor = ContextCompat.getColor(getApplicationContext(), R.color.accent);
		ArrayList<PieEntry> data = new ArrayList<>();
		for (Music one : allSortedByScore)
			try {
				Bitmap cover = one.getCover(this, 64);

				data.add(new PieEntry(
						(float) (one.Score * 100f / score_total),
						one.getText(),
						new BitmapDrawable(getResources(), cover),
						one.Path));
				int color = defaultColor;
				try {
					Palette palette = Palette.from(cover).generate();
					int colorBackup = color;
					color = palette.getVibrantColor(color);
					if (color == colorBackup)
						color = palette.getDarkVibrantColor(color);
					if (color == colorBackup)
						color = palette.getDarkMutedColor(color);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				colors.add(ColorUtils.setAlphaComponent(color, 160));
			} catch (Exception e) {
				e.printStackTrace();
			}

		PieDataSet dataSet = new PieDataSet(data, "Top " + N);
		dataSet.setSliceSpace(1f);
		dataSet.setSelectionShift(5f);
		dataSet.setHighlightEnabled(true);
		dataSet.setDrawValues(true);
		dataSet.setDrawIcons(true);
		dataSet.setIconsOffset(new MPPointF(0, 42));
		dataSet.setColors(colors);

		PieData chartData = new PieData();
		chartData.addDataSet(dataSet);
		chartData.setDrawValues(true);
		chartData.setValueFormatter(new PercentFormatter());
		chartData.setValueTextSize(10f);
		chartData.setValueTextColor(ContextCompat.getColor(this, R.color.primary_text));

		chart.setData(chartData);
		chart.highlightValues(null);
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				try {
					Intent i = new Intent(AnalyticsActivity.this, MusicService.class);

					i.setAction(MusicService.ACTION_OPEN);
					i.putExtra(MusicService.KEY_URI, (String) e.getData());

					startService(i);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			@Override
			public void onNothingSelected() {

			}
		});
		chart.invalidate();

		chart.animateY(3600, Easing.EasingOption.EaseInOutQuad);
		// chart.spin(2000, 0, 360);

	}

	//endregion

	//region DC

	private ImageView analytics_dc_status;

	private void createDC() {
		analytics_dc_status = findViewById(R.id.analytics_dc_status);

		analytics_dc_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (Analytics.getInstance().getDCEnabled()) {
					Analytics.getInstance().setDCEnabled(false);
				} else {
					Analytics.getInstance().setDCEnabled(true);
				}

				updateDCState();
			}
		});

		updateDCState();
	}

	private void updateDCState() {
		if (Analytics.getInstance().getDCEnabled()) {
			analytics_dc_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
			analytics_dc_status.setImageResource(R.drawable.ic_settings_remote_black);
		} else {
			analytics_dc_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
			analytics_dc_status.setImageResource(R.drawable.ic_error_outline_black);
		}
	}

	//endregion

	//region LFM

	private ImageView analytics_lfm_status;
	private EditText analytics_lfm_username_editText;
	private EditText analytics_lfm_password_editText;
	private Button analytics_lfm_save;
	private TextView analytics_lfm_logs;
	private TextInputLayout analytics_lfm_username;
	private TextInputLayout analytics_lfm_password;

	private void createLFM() {
		analytics_lfm_username = findViewById(R.id.analytics_lfm_username);
		analytics_lfm_password = findViewById(R.id.analytics_lfm_password);
		analytics_lfm_save = findViewById(R.id.analytics_lfm_save);

		analytics_lfm_status = findViewById(R.id.analytics_lfm_status);
		analytics_lfm_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Session session = Analytics.getInstance().getLastfmSession();
				if (session != null) {
					analytics_lfm_username_editText.setText("");
					analytics_lfm_password_editText.setText("");
					Analytics.getInstance().setLastfmCredentials("", "");
				}
				updateLFM();
			}
		});

		analytics_lfm_username_editText = findViewById(R.id.analytics_lfm_username_editText);
		analytics_lfm_username_editText.setText(Analytics.getInstance().getLastfmUsername());

		analytics_lfm_password_editText = findViewById(R.id.analytics_lfm_password_editText);
		analytics_lfm_password_editText.setText(Analytics.getInstance().getLastfmPassword());

		analytics_lfm_save = findViewById(R.id.analytics_lfm_save);
		analytics_lfm_save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String username = analytics_lfm_username_editText.getText().toString();
				String password = analytics_lfm_password_editText.getText().toString();

				Analytics.getInstance().setLastfmCredentials(username, password);

				updateLFM();
			}
		});

		analytics_lfm_logs = findViewById(R.id.analytics_lfm_logs);
		StringBuilder sb = new StringBuilder();
		for (ScrobbleResult sr : Analytics.getInstance().getScrobblerResultsForLastfm()) {
			sb.append(sr.toString()).append(System.lineSeparator());
		}
		analytics_lfm_logs.setText(sb.toString());

		updateLFMState();
		Session session = Analytics.getInstance().getLastfmSession();
		if (session == null && Analytics.getInstance().isLastfmScrobbledEnabled()) {
			updateLFM();
		}
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
		if (session == null) {
			analytics_lfm_username.setVisibility(View.VISIBLE);
			analytics_lfm_password.setVisibility(View.VISIBLE);
			analytics_lfm_save.setVisibility(View.VISIBLE);
		} else {
			analytics_lfm_username.setVisibility(View.GONE);
			analytics_lfm_password.setVisibility(View.GONE);
			analytics_lfm_save.setVisibility(View.GONE);
		}
	}

	private static class RefreshLFM extends AsyncTask<Void, Void, Void> {
		private WeakReference<AnalyticsActivity> contextRef;

		public RefreshLFM(AnalyticsActivity context) {
			this.contextRef = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(Void... voids) {

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
