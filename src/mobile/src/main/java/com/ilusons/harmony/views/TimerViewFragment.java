package com.ilusons.harmony.views;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.legacy.content.WakefulBroadcastReceiver;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ui.timedurationpicker.TimeDurationPicker;
import com.ilusons.harmony.ref.ui.timedurationpicker.TimeDurationPickerDialog;

import org.apache.commons.lang3.StringUtils;

public class TimerViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = TimerViewFragment.class.getSimpleName();

	private View root;

	private TextView text;
	private CountDownTimer countDownTimer;
	private LottieAnimationView set_timer;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.timer_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		// Set timer
		text = v.findViewById(R.id.text);

		set_timer = v.findViewById(R.id.set_timer);

		set_timer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				(new TimeDurationPickerDialog(getContext(), new TimeDurationPickerDialog.OnDurationSetListener() {
					@Override
					public void onDurationSet(TimeDurationPicker view, long duration) {
						if (duration != 0) {
							setTimer(getContext(), System.currentTimeMillis() + duration);

							updateUI();
						} else {
							cancelTimer(getContext());

							updateUI();
						}
					}
				}, getSleepTimerTimeLeft(getContext()))).show();
			}
		});

		updateUI();

		return v;
	}

	private void updateUI() {
		try {
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

				set_timer.pauseAnimation();
				set_timer.setAnimation("clock.json", LottieAnimationView.CacheStrategy.Weak);
				set_timer.loop(true);
				set_timer.setScale(1);
				set_timer.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				set_timer.clearColorFilters();
				set_timer.addColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_green_light), PorterDuff.Mode.MULTIPLY));
				set_timer.playAnimation();

			} else {

				text.setText("Tap above");

				set_timer.pauseAnimation();
				set_timer.setAnimation("no_notifications!.json", LottieAnimationView.CacheStrategy.Weak);
				set_timer.loop(true);
				set_timer.setScale(1);
				set_timer.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				set_timer.clearColorFilters();
				set_timer.addColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_red_light), PorterDuff.Mode.MULTIPLY));
				set_timer.playAnimation();
			}
		} catch (Exception e) {
			// Eat ?
		}
	}

	//region Notification

	public static final String ACTION_VIEW_TIMER = TAG + "_view_timer";
	private static int NOTIFICATION_ID_VIEW_TIMER = 23453;

	private static void sendNotification(Context context, int id, String title, String content) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.setAction(ACTION_VIEW_TIMER);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		final String NOTIFICATION_CHANNEL = "Timer";

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			try {
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				if (notificationManager != null) {
					notificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL, StringUtils.capitalize(NOTIFICATION_CHANNEL), NotificationManager.IMPORTANCE_DEFAULT));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
				.setSmallIcon(R.drawable.ic_music_bell)
				.setContentTitle(title)
				.setContentText(content)
				.setAutoCancel(false)
				.setColor(ContextCompat.getColor(context, R.color.accent))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				.setStyle(new NotificationCompat.BigTextStyle()
						.setBigContentTitle(title)
						.setSummaryText(content)
						.bigText(content));

		if (pendingIntent != null)
			notificationBuilder.setContentIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify(id, notificationBuilder.build());
		}
	}

	private static void cancelNotification(Context context, int id) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.cancel(id);
		}
	}

	//endregion

	public static final String TAG_SPREF_ST_TIME = SPrefEx.TAG_SPREF + ".st_t";

	public static Long getSleepTimerTime(Context context) {
		try {
			return SPrefEx.get(context).getLong(TAG_SPREF_ST_TIME, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}

	public static void setSleepTimerTime(Context context, Long value) {
		try {
			SPrefEx.get(context)
					.edit()
					.putLong(TAG_SPREF_ST_TIME, value)
					.apply();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

		sendNotification(context, NOTIFICATION_ID_VIEW_TIMER, "Timer", "Tap here to open timer.");
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

		cancelNotification(context, NOTIFICATION_ID_VIEW_TIMER);
	}

	public static void showAsDialog(Context context) {
		FragmentDialogActivity.show(context, TimerViewFragment.class, Bundle.EMPTY);
	}

	public static TimerViewFragment create() {
		TimerViewFragment f = new TimerViewFragment();
		return f;
	}

}
