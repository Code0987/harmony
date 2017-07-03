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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.SPrefEx;

public class SleepTimerActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = SleepTimerActivity.class.getSimpleName();

    private View root;

    private TextView text;
    private CountDownTimer countDownTimer;
    private ImageButton set_timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set view
        setContentView(R.layout.sleep_timer_activity);

        // Set views
        root = findViewById(R.id.root);

        // Set close
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Set timer
        text = (TextView) findViewById(R.id.text);

        set_timer = (ImageButton) findViewById(R.id.set_timer);

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

                        setTimer(SleepTimerActivity.this, time);

                        updateUI();
                    }
                };
                final HmsPickerBuilder hpb = new HmsPickerBuilder()
                        .setFragmentManager(getSupportFragmentManager())
                        .setStyleResId(R.style.BetterPickersDialogFragment);
                hpb.addHmsPickerDialogHandler(handler);
                hpb.setOnDismissListener(new OnDialogDismissListener() {
                    @Override
                    public void onDialogDismiss(DialogInterface dialoginterface) {
                        hpb.removeHmsPickerDialogHandler(handler);
                    }
                });
                hpb.setTimeInMilliseconds(getSleepTimerTimeLeft(SleepTimerActivity.this));
                hpb.show();

            }
        });

        updateUI();

    }

    private void updateUI() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (getSleepTimerTimeLeft(this) > 0) {

            countDownTimer = new CountDownTimer(getSleepTimerTimeLeft(this), 1000) {
                @Override
                public void onTick(long time) {
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

            set_timer.setImageDrawable(getDrawable(R.drawable.ic_timer_black));
            set_timer.setColorFilter(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);

        } else {

            text.setText(null);

            set_timer.setImageDrawable(getDrawable(R.drawable.ic_timer_off_black));
            set_timer.setColorFilter(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);

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

}
