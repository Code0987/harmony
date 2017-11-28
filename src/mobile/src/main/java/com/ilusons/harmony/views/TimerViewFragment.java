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
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseDialogUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.SPrefEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;

public class TimerViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = TimerViewFragment.class.getSimpleName();

	private View root;

	private TextView text;
	private CountDownTimer countDownTimer;
	private ImageButton set_timer;

	private LottieAnimationView lottieAnimationView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.timer_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		// Set timer
		text = (TextView) v.findViewById(R.id.text);

		set_timer = (ImageButton) v.findViewById(R.id.set_timer);

		set_timer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				final HmsPickerDialogFragment.HmsPickerDialogHandlerV2 handler = new HmsPickerDialogFragment.HmsPickerDialogHandlerV2() {
					@Override
					public void onDialogHmsSet(int reference, boolean isNegative, int hours, int minutes, int seconds) {
						long time = ((((hours * 60L) + minutes) * 60) + seconds) * 1000;
						if (time > 0L) {
							time += System.currentTimeMillis();
						}

						setTimer(getContext(), time);

						updateUI();
					}
				};
				final HmsPickerBuilder hpb = new HmsPickerBuilder()
						.setFragmentManager(getActivity().getSupportFragmentManager())
						.setStyleResId(R.style.BetterPickersDialogFragment);
				hpb.addHmsPickerDialogHandler(handler);
				hpb.setOnDismissListener(new OnDialogDismissListener() {
					@Override
					public void onDialogDismiss(DialogInterface dialoginterface) {
						hpb.removeHmsPickerDialogHandler(handler);
					}
				});
				hpb.setTimeInMilliseconds(getSleepTimerTimeLeft(getContext()));
				hpb.show();

			}
		});

		lottieAnimationView = v.findViewById(R.id.lottieAnimationView);

		updateUI();

		return v;
	}

	private void updateUI() {

		if (countDownTimer != null) {
			countDownTimer.cancel();
			countDownTimer = null;
		}

		if (getSleepTimerTimeLeft(getContext()) > 0) {

			countDownTimer = new CountDownTimer(getSleepTimerTimeLeft(getContext()), 1000) {
				@Override
				public void onTick(long time) {
					time += 1000; // HACK

					int h = (int) (time / 3600000);
					int m = (int) (time - h * 3600000) / 60000;
					int s = (int) (time - h * 3600000 - m * 60000) / 1000;
					String hh = h < 10 ? "0" + h : h + "";
					String mm = m < 10 ? "0" + m : m + "";
					String ss = s < 10 ? "0" + s : s + "";
					text.setText(hh + ":" + mm + ":" + ss);
				}

				@Override
				public void onFinish() {
					updateUI();
				}
			};
			countDownTimer.start();

			text.setText("...");

			set_timer.setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_black));
			set_timer.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);

			lottieAnimationView.pauseAnimation();
			lottieAnimationView.setAnimation("clock.json", LottieAnimationView.CacheStrategy.Weak);
			lottieAnimationView.loop(true);
			lottieAnimationView.setScale(1);
			lottieAnimationView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			lottieAnimationView.clearColorFilters();
			lottieAnimationView.addColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_green_light), PorterDuff.Mode.MULTIPLY));
			lottieAnimationView.playAnimation();

		} else {

			text.setText(null);

			set_timer.setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_off_black));
			set_timer.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);

			lottieAnimationView.pauseAnimation();
			lottieAnimationView.setAnimation("no_notifications!.json", LottieAnimationView.CacheStrategy.Weak);
			lottieAnimationView.loop(true);
			lottieAnimationView.setScale(1);
			lottieAnimationView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			lottieAnimationView.clearColorFilters();
			lottieAnimationView.addColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_red_light), PorterDuff.Mode.MULTIPLY));
			lottieAnimationView.playAnimation();
		}


	}

	public static final String TAG_SPREF_ST_TIME = SPrefEx.TAG_SPREF + ".st_t";

	public static Long getSleepTimerTime(Context context) {
		return SPrefEx.get(context).getLong(TAG_SPREF_ST_TIME, 0);
	}

	public static void setSleepTimerTime(Context context, Long value) {
		SPrefEx.get(context)
				.edit()
				.putLong(TAG_SPREF_ST_TIME, value)
				.apply();
	}

	public static long getSleepTimerTimeLeft(Context context) {
		long dt = getSleepTimerTime(context) - System.currentTimeMillis();

		if (dt < 0)
			dt = 0L;

		return dt;
	}

	public static class WakefulReceiver extends WakefulBroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "WakefulReceiver::onReceive" + System.lineSeparator() + intent);

			WakefulReceiver.completeWakefulIntent(intent);

			// Stop playback
			final Intent intentStop = new Intent(context, MusicService.class);
			intentStop.setAction(MusicService.ACTION_STOP);
			startWakefulService(context, intentStop);

			Toast.makeText(context, "Sleep timer! Stopping playback now, if active!", Toast.LENGTH_LONG).show();

			cancelTimer(context);
		}

	}

	public static class BootReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "BootReceiver::onReceive" + System.lineSeparator() + intent);

			if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

				Long time = getSleepTimerTime(context);

				if (time > 0) {

					AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

					PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, WakefulReceiver.class), 0);

					alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, alarmIntent);

				}

			}

		}

	}

	public static void setTimer(Context context, long time) {
		setSleepTimerTime(context, time);

		if (time <= 0L)
			return;

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(context, WakefulReceiver.class);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, alarmIntent);

		// Enable {@code BootReceiver} to automatically restart when the
		// device is rebooted.
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
	}

	public static void cancelTimer(Context context) {
		setSleepTimerTime(context, 0L);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, WakefulReceiver.class);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmManager.cancel(alarmIntent);

		// Disable {@code BootReceiver} so that it doesn't automatically restart when the device is rebooted.
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	}

	public static void showAsDialog(Context context) {
		if (MusicService.IsPremium) {
			BaseDialogUIActivity.show(context, TimerViewFragment.class, Bundle.EMPTY);
		} else {
			Toast.makeText(context, "Play some music first!", Toast.LENGTH_LONG).show();
		}
	}

	public static boolean shouldBeVisible() {
		return MusicService.IsPremium && Music.getSize() >= 5;
	}

	public static TimerViewFragment create() {
		TimerViewFragment f = new TimerViewFragment();
		return f;
	}

}
