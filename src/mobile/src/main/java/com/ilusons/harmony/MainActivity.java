package com.ilusons.harmony;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.ref.ue.RateMe;
import com.ilusons.harmony.ref.ue.Tips;
import com.ilusons.harmony.views.LibraryUIActivity;
import com.ilusons.harmony.views.PlaybackUIActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Intent
		handleIntent(getIntent());

		// Kill self
		finish();

	}

	private void handleIntent(final Intent intent) {
		Log.d(TAG, "handleIntent\n" + intent);

		if (intent.getAction() == null) {
			openLibraryUIActivity(this);
			return;
		}

		if (intent.getAction().equals(Intent.ACTION_VIEW)) {
			String scheme = intent.getScheme();

			if (scheme.equals(ContentResolver.SCHEME_FILE)
					|| scheme.equals(ContentResolver.SCHEME_CONTENT)) {

				try {
					final Uri uri = Uri.parse(StorageEx.getPath(MainActivity.this, intent.getData()));

					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							Intent i = new Intent(MainActivity.this, MusicService.class);

							i.setAction(MusicService.ACTION_OPEN);
							i.putExtra(MusicService.KEY_URI, uri.toString());

							startService(i);

							openPlaybackUIActivity(MainActivity.this);
						}
					}, 350);
				} catch (Exception e) {
					e.printStackTrace();

					openLibraryUIActivity(this);
				}

			} else if (scheme.equals("http")) {
			} else if (scheme.equals("ftp")) {
			} else {
				openLibraryUIActivity(this);
				return;
			}

		}
	}

	public static synchronized Intent getLibraryUIActivityIntent(final Context context) {
		Intent intent = new Intent(context, LibraryUIActivity.class);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static synchronized void openLibraryUIActivity(final Context context) {
		context.startActivity(getLibraryUIActivityIntent(context));
	}

	public static synchronized Intent getPlaybackUIActivityIntent(final Context context) {
		Intent intent = new Intent(context, PlaybackUIActivity.class);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static synchronized void openPlaybackUIActivity(final Context context) {
		context.startActivity(getPlaybackUIActivityIntent(context));
	}

	public static void gotoFeedback(final Context context) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, "[#" + context.getString(R.string.app_name) + " #feedback #android #" + BuildConfig.VERSION_NAME + "]");
		intent.putExtra(Intent.EXTRA_TEXT, "");
		intent.setData(Uri.parse("mailto:"));
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"harmony@ilusons.com", "7b56b759@opayq.com"});
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public static void gotoPlayStore(final Context context) {
		try {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
		} catch (android.content.ActivityNotFoundException anfe) {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
		}
	}

	public static void initRateMe(final WeakReference<FragmentActivity> contextRef) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (contextRef.get() == null)
					return;

				RateMe rateMe = new RateMe(contextRef.get());
				rateMe.setConstraints(
						5,
						2,
						14,
						5);
				rateMe.setListener(new RateMe.Listener() {
					@Override
					public void onPositive(RateMe rateMe, boolean lessRating) {
						if (lessRating) {
							MainActivity.gotoFeedback(rateMe.getActivity());
						} else {
							MainActivity.gotoPlayStore(rateMe.getActivity());
						}
					}

					@Override
					public void onNeutral(RateMe rateMe) {

					}

					@Override
					public void onNegative(RateMe rateMe) {

					}
				});
				rateMe.forceShow();
			}
		}, (int) (1.5 * 60 * 1000));
	}

	public static void initTips(final WeakReference<FragmentActivity> contextRef) {
		if (contextRef.get() == null)
			return;

		ArrayList<String> messages = new ArrayList<>();

		messages.add("You can play videos in background, just open any video.");
		messages.add("Automatic headset play/pause can stopped from settings.");
		messages.add("In default playback ui, you can rotate screen. It's specifically designed for video playback.");
		messages.add("Long press play button to stop/reset.");
		messages.add("Long press jump/open button to open playback ui.");
		messages.add("Long press to open lyrics menu.");
		messages.add("Long press cover art, to fade it.");
		messages.add("Long press visualization button to change styles.");

		Tips tips = new Tips(contextRef.get());

		tips.setMessages(messages);

		tips.run();
	}

}
