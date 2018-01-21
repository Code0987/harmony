package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.R;

import java.util.Map;

public class RateMe {

	private static final String TAG = RateMe.class.getSimpleName();

	private static final String DONT_SHOW_AGAIN = TAG + ".DONT_SHOW_AGAIN";
	public static final String TOTAL_LAUNCH_COUNT = TAG + ".TOTAL_LAUNCH_COUNT";
	public static final String TIME_OF_ABSOLUTE_FIRST_LAUNCH = TAG + ".TIME_OF_ABSOLUTE_FIRST_LAUNCH";
	public static final String LAUNCHES_SINCE_LAST_PROMPT = TAG + ".LAUNCHES_SINCE_LAST_PROMPT";
	public static final String TIME_OF_LAST_PROMPT = TAG + ".TIME_OF_LAST_PROMPT";

	private FragmentActivity activity;
	private SharedPreferences spref;

	private int minLaunchesUntilInitialPrompt = 0;
	private int minDaysUntilInitialPrompt = 0;
	private int minLaunchesUntilNextPrompt = 0;
	private int minDaysUntilNextPrompt = 0;
	private Boolean handleCancelAsNeutral = true;
	private Boolean runWithoutPlayStore = false;

	public RateMe(final FragmentActivity fragmentActivity) {
		activity = fragmentActivity;
		spref = activity.getSharedPreferences(TAG, 0);
	}

	public FragmentActivity getActivity() {
		return activity;
	}

	private Listener listener;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	private String title;

	public String getTitle() {
		if (title == null) {
			return "Rate " + getApplicationName();
		} else {
			return title;
		}
	}

	public void setTitle(String s) {
		title = s;
	}

	private String message;

	public String getMessage() {
		if (message == null) {
			return "If you like using "
					+ this.getApplicationName()
					+ ", it would be great"
					+ " if you took a moment to rate it in the Play Store. Thank you!";
		} else {
			return message.replace("%totalLaunchCount%", String.valueOf(spref.getInt(TOTAL_LAUNCH_COUNT, 0)));
		}
	}

	public void setMessage(String s) {
		message = s;
	}

	private String positiveText;

	public String getPositiveText() {
		if (positiveText == null) {
			return "Rate it";
		} else {
			return positiveText;
		}
	}

	public void setPositiveText(String s) {
		positiveText = s;
	}

	private String neutralText;

	public String getNeutralText() {
		if (neutralText == null) {
			return "Not now";
		} else {
			return neutralText;
		}
	}

	public void setNeutralText(String s) {
		neutralText = s;
	}

	private String negativeText;

	public String getNegativeText() {
		if (negativeText == null) {
			return "Never";
		} else {
			return negativeText;
		}
	}

	public void setNegativeText(String s) {
		negativeText = s;
	}

	private int iconRes = 0;

	public int getIconRes() {
		if (iconRes == 0)
			iconRes = R.drawable.logo;
		return iconRes;
	}

	public void setIconRes(int res) {
		iconRes = res;
	}

	private String messageForLowRatings;

	public String getMessageForLowRatings() {
		if (messageForLowRatings == null)
			return "Sorry to hear that! Please tell us why you rated so less, to give us a chance to improve.";
		return messageForLowRatings;
	}

	private String positiveForLowRatings;

	public String getPositiveForLowRatings() {
		if (positiveForLowRatings == null)
			return "Send Feedback";
		return positiveForLowRatings;
	}

	public void setPositiveForLowRatings(String s) {
		positiveForLowRatings = s;
	}

	private float ratings = 5.0f;

	public float getRatings() {
		return ratings;
	}

	public void setRatings(float v) {
		ratings = Math.max(Math.min(v, 5f), 0f);
	}

	private float lowRatings = 3.99f;

	public float getLowRatings() {
		return lowRatings;
	}

	public void setLowRatings(float v) {
		lowRatings = Math.max(Math.min(v, 4.99f), 0.99f);
	}

	/**
	 * Sets requirements for when to prompt the user.
	 *
	 * @param minLaunchesUntilInitialPrompt Minimum of launches before the user is prompted for the first
	 *                                      time. One call of .run() counts as launch.
	 * @param minDaysUntilInitialPrompt     Minimum of days before the user is prompted for the first
	 *                                      time.
	 * @param minLaunchesUntilNextPrompt    Minimum of launches before the user is prompted for each next
	 *                                      time. One call of .run() counts as launch.
	 * @param minDaysUntilNextPrompt        Minimum of days before the user is prompted for each next
	 *                                      time.
	 */
	public void setConstraints(int minLaunchesUntilInitialPrompt,
	                           int minDaysUntilInitialPrompt,
	                           int minLaunchesUntilNextPrompt,
	                           int minDaysUntilNextPrompt) {
		this.minLaunchesUntilInitialPrompt = minLaunchesUntilInitialPrompt;
		this.minDaysUntilInitialPrompt = minDaysUntilInitialPrompt;
		this.minLaunchesUntilNextPrompt = minLaunchesUntilNextPrompt;
		this.minDaysUntilNextPrompt = minDaysUntilNextPrompt;
	}

	/**
	 * @param handleCancelAsNeutral Standard is true. If set to false, a back press (or other
	 *                              things that lead to the dialog being cancelled), will be
	 *                              handled like a negative choice (click on "Never").
	 */
	public void setHandleCancelAsNeutral(Boolean handleCancelAsNeutral) {
		this.handleCancelAsNeutral = handleCancelAsNeutral;
	}

	/**
	 * Standard is false. Whether the run method is executed even if no Play
	 * Store is installed on device.
	 *
	 * @param runWithoutPlayStore
	 */
	public void setRunWithoutPlayStore(Boolean runWithoutPlayStore) {
		runWithoutPlayStore = runWithoutPlayStore;
	}

	public static void reset(Context context) {
		context.getSharedPreferences(TAG, 0).edit().clear().apply();

		Log.d(TAG, "Cleared shared preferences.");
	}

	private void showDialog() {
		if (activity.getSupportFragmentManager().findFragmentByTag(TAG) != null) {
			return;
		}

		Fragment frag = new Fragment();
		frag.setOwner(this);
		frag.show(activity.getSupportFragmentManager(), TAG);
	}

	public void forceShow() {
		showDialog();
	}

	public void run() {
		try {
			if (BuildConfig.DEBUG) {
				Map<String, ?> keys = spref.getAll();
				for (Map.Entry<String, ?> entry : keys.entrySet()) {
					Log.d(TAG, "spref:\t" + entry.getKey() + ": " + entry.getValue().toString());
				}
			}

			if (spref.getBoolean(DONT_SHOW_AGAIN, false)) {
				return;
			}

			if (!isPlayStoreInstalled()) {
				Log.d(TAG, "No Play Store installed on device.");

				if (!runWithoutPlayStore) {
					return;
				}
			}

			Editor editor = spref.edit();

			int totalLaunchCount = spref.getInt(TOTAL_LAUNCH_COUNT, 0) + 1;
			editor.putInt(TOTAL_LAUNCH_COUNT, totalLaunchCount);

			long currentMillis = System.currentTimeMillis();

			long timeOfAbsoluteFirstLaunch = spref.getLong(TIME_OF_ABSOLUTE_FIRST_LAUNCH, 0);
			if (timeOfAbsoluteFirstLaunch == 0) {
				timeOfAbsoluteFirstLaunch = currentMillis;
				editor.putLong(TIME_OF_ABSOLUTE_FIRST_LAUNCH, timeOfAbsoluteFirstLaunch);
			}

			long timeOfLastPrompt = spref.getLong(TIME_OF_LAST_PROMPT, 0);

			int launchesSinceLastPrompt = spref.getInt(LAUNCHES_SINCE_LAST_PROMPT, 0) + 1;
			editor.putInt(LAUNCHES_SINCE_LAST_PROMPT, launchesSinceLastPrompt);

			if (totalLaunchCount >= minLaunchesUntilInitialPrompt
					&& ((currentMillis - timeOfAbsoluteFirstLaunch)) >= (minDaysUntilInitialPrompt * DateUtils.DAY_IN_MILLIS)) {
				if (timeOfLastPrompt == 0 || (launchesSinceLastPrompt >= minLaunchesUntilNextPrompt && ((currentMillis - timeOfLastPrompt) >= (minDaysUntilNextPrompt * DateUtils.DAY_IN_MILLIS)))) {
					editor.putLong(TIME_OF_LAST_PROMPT, currentMillis);
					editor.putInt(LAUNCHES_SINCE_LAST_PROMPT, 0);
					editor.apply();

					showDialog();
				} else {
					editor.commit();
				}
			} else {
				editor.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onCancel() {
		if (handleCancelAsNeutral) {
			onNeutral();
		} else {
			onNegative();
		}
	}

	private void onNegative() {
		if (listener != null)
			try {
				listener.onNegative(this);

				Editor editor = spref.edit();
				editor.putBoolean(DONT_SHOW_AGAIN, true);
				editor.apply();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void onNeutral() {
		if (listener != null)
			try {
				listener.onNeutral(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void onPositive() {
		if (listener != null)
			try {
				listener.onPositive(this, getRatings() <= getLowRatings());

				Editor editor = spref.edit();
				editor.putBoolean(DONT_SHOW_AGAIN, true);
				editor.apply();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/**
	 * @return the application name of the host activity
	 */
	private String getApplicationName() {
		final PackageManager pm = activity.getApplicationContext()
				.getPackageManager();
		ApplicationInfo ai;
		String appName;
		try {
			ai = pm.getApplicationInfo(activity.getPackageName(), 0);
			appName = (String) pm.getApplicationLabel(ai);
		} catch (final NameNotFoundException e) {
			appName = "(unknown)";
		}
		return appName;
	}

	/**
	 * @return Whether Google Play Store is installed on device
	 */
	private Boolean isPlayStoreInstalled() {
		PackageManager pacman = activity.getPackageManager();
		try {
			pacman.getApplicationInfo("com.android.vending", 0);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	public interface Listener {
		void onPositive(RateMe rateMe, boolean lessRating);

		void onNeutral(RateMe rateMe);

		void onNegative(RateMe rateMe);
	}

	public static class Fragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

		private RateMe owner;

		public void setOwner(RateMe owner) {
			this.owner = owner;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Fragment including variables will survive orientation changes
			this.setRetainInstance(true);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View v = inflater.inflate(R.layout.rateme_dialog, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
			builder.setView(v);

			ImageView icon = v.findViewById(R.id.rateme_dialog_icon);
			icon.setImageResource(owner.getIconRes());

			TextView title = v.findViewById(R.id.rateme_dialog_title);
			title.setText(owner.getTitle());

			final TextView message = v.findViewById(R.id.rateme_dialog_message);
			message.setText(owner.getMessage());

			TextView negative = v.findViewById(R.id.rateme_dialog_negative);
			negative.setText(owner.getNegativeText());
			negative.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					owner.onNegative();

					dismiss();
				}
			});

			TextView neutral = v.findViewById(R.id.rateme_dialog_neutral);
			neutral.setText(owner.getNeutralText());
			neutral.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					owner.onNeutral();

					dismiss();
				}
			});

			final TextView positive = v.findViewById(R.id.rateme_dialog_positive);
			positive.setText(owner.getPositiveText());
			positive.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					owner.onPositive();

					dismiss();
				}
			});

			builder.setOnCancelListener(this);

			RatingBar rating_bar = v.findViewById(R.id.rateme_dialog_rating_bar);
			rating_bar.setRating(owner.getRatings());
			rating_bar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
				@Override
				public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
					if (!fromUser)
						return;

					if (rating <= owner.getLowRatings()) {
						message.setText(owner.getMessageForLowRatings());

						positive.setText(owner.getPositiveForLowRatings());
					} else {
						message.setText(owner.getMessage());

						positive.setText(owner.getPositiveText());
					}

					owner.setRatings(rating);
				}
			});

			AlertDialog alert = builder.create();

			alert.requestWindowFeature(DialogFragment.STYLE_NO_TITLE);

			return alert;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			owner.onCancel();
		}

		@Override
		public void onClick(DialogInterface dialog, int choice) {
			switch (choice) {
				case DialogInterface.BUTTON_POSITIVE:
					owner.onPositive();
					break;
				case DialogInterface.BUTTON_NEUTRAL:
					owner.onNeutral();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					owner.onNegative();
					break;
			}
		}

		@Override
		public void onDestroyView() {
			// Work around bug:
			// http://code.google.com/p/android/issues/detail?id=17423
			Dialog dialog = getDialog();

			if ((dialog != null) && getRetainInstance()) {
				dialog.setDismissMessage(null);
			}

			super.onDestroyView();
		}

	}

}
