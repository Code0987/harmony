package com.ilusons.harmony;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.ref.ue.RateMe;
import com.ilusons.harmony.ref.ue.Tips;
import com.ilusons.harmony.views.DashboardActivity;
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
			openDashboardActivity(this);
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

					openDashboardActivity(this);
				}

			} else if (scheme.equals("http")) {
			} else if (scheme.equals("ftp")) {
			} else {
				openDashboardActivity(this);
				return;
			}

		}
	}

	public static synchronized Intent getDashboardActivityIntent(final Context context) {
		Intent intent = new Intent(context, DashboardActivity.class);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static synchronized void openDashboardActivity(final Context context) {
		context.startActivity(getDashboardActivityIntent(context));
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
		try {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "[#" + context.getString(R.string.app_name) + " #feedback #android #" + BuildConfig.VERSION_NAME + "]");
			intent.putExtra(Intent.EXTRA_TEXT, "");
			intent.setData(Uri.parse("mailto:"));
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"harmony@ilusons.com", "7b56b759@opayq.com"});
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();

			Toast.makeText(context, "Unable to open your email handler. Please follow the alternate (Plays store page) instead.", Toast.LENGTH_LONG).show();

			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
		}
	}

	public static void gotoPlayStore(final Context context) {
		try {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
		} catch (android.content.ActivityNotFoundException anfe) {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
		}
	}

	public static void initRateMe(final WeakReference<FragmentActivity> contextRef, final boolean force) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (contextRef.get() == null || contextRef.get().isFinishing() || contextRef.get().isDestroyed())
					return;

				try {
					RateMe rateMe = new RateMe(contextRef.get());
					rateMe.setConstraints(
							5,
							1,
							9,
							3);
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
					if (force)
						rateMe.forceShow();
					else
						rateMe.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, (int) (1.5 * 60 * 1000));
	}

	public static void initTips(final WeakReference<FragmentActivity> contextRef) {
		if (contextRef.get() == null || contextRef.get().isFinishing() || contextRef.get().isDestroyed())
			return;

		try {
			ArrayList<String> messages = new ArrayList<>();

			messages.add("You can play videos in background, just open any video.");
			messages.add("Automatic headset play/pause can be adjusted from settings.");
			messages.add("In default playback ui, you can rotate screen. It's specifically designed for video playback.");
			messages.add("Long press play/pause button to stop/reset.");
			messages.add("Long press skip/next/previous button to skip to random item in playlist.");
			messages.add("Long press jump/open button to open playback ui.");
			messages.add("Long press to open lyrics menu on lyrics view.");
			messages.add("Long press cover art to open menu.");
			messages.add("Long press visualization button to change styles.");
			messages.add("If songs are not shown,\nTap Library -> Save to current playlist\nOr\nLeft drawer -> Refresh\nOr\nLeft drawer -> Settings -> Library -> Add scan location -> (previous step)");
			messages.add("Beta version will have some of the premium features unlocked.");
			messages.add("Visit our YouTube Channel/Playlist (Left drawer -> Help...) for demos and videos.");
			messages.add("Visit our website (Settings -> About ...) for latest updates and more information.");
			messages.add("Almost every button, ui, ... has long-press action. Be sure to find out.");
			messages.add("Headset media button is sometimes supported.\n1 press - play/pause\n2 fast press - next\n3 fast press - previous");
			messages.add("On right drawer, you can also see your playlist from other apps or on device.");
			messages.add("Help us! By creating tour/informative videos. Send us YouTube links, you'll get surprises!");
			messages.add("Be sure to check out Analytics, Playback UI styles, Library UI styles, Audio visualization styles, these are free for limited time!");
			messages.add("You can now share the whole music file  with your friends using any supported app.");
			messages.add("You can now share lyrics with your friends using any supported app.");
			messages.add("We now support audio fingerprinting, i.e. you can get details of audio even if it has no tags.");
			messages.add("You can download cover art again. Long-press cover art on playback ui.");
			messages.add("This is a smart player, it'll learn and improve itself over time, as you use it.");

			Tips tips = new Tips(contextRef.get());

			tips.setMessages(messages);

			tips.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
