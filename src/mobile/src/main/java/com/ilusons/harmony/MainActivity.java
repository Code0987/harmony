package com.ilusons.harmony;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.views.DashboardActivity;
import com.ilusons.harmony.views.IntroActivity;
import com.ilusons.harmony.views.PlaylistViewActivity;
import com.ilusons.harmony.views.RateMe;
import com.ilusons.harmony.views.Tips;
import com.ilusons.harmony.views.PlaybackUIActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import jonathanfinerty.once.Once;

public class MainActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = MainActivity.class.getSimpleName();

	public static final String ACTION_ALERT = TAG + ".alert";
	public static final String ALERT_TITLE = "title";
	public static final String ALERT_CONTENT = "content";
	public static final String ALERT_LINK = "link";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setBackgroundDrawable(null);

		// Intent
		handleIntent(getIntent());
	}

	private void handleIntent(final Intent intent) {
		Log.d(TAG, "handleIntent\n" + intent);

		if (intent.getAction() == null) {
			openDashboardActivity(this);

			// Kill self
			finish();

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

			// Kill self
			finish();

		} else if (intent.getAction().equals(ACTION_ALERT)) {
			String title = intent.getStringExtra(ALERT_TITLE);
			String content = intent.getStringExtra(ALERT_CONTENT);
			String link = intent.getStringExtra(ALERT_LINK);

			showDialog(this, title, content, link, new JavaEx.Action() {
				@Override
				public void execute() {
					// Kill self
					finish();
				}
			});
		}

		// Search

		else if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			Intent queryIntent = new Intent(Intent.ACTION_SEARCH);
			queryIntent.putExtra(SearchManager.QUERY, query);

			LocalBroadcastManager
					.getInstance(this)
					.sendBroadcast(queryIntent);

			// Kill self
			finish();
		}

	}

	public static synchronized Intent getDashboardActivityIntent(final Context context) {
		Intent intent = null;

		if (!Once.beenDone(Once.THIS_APP_INSTALL, IntroActivity.TAG)) {
			intent = new Intent(context, IntroActivity.class);

			Once.markDone(IntroActivity.TAG);
		} else {
			intent = new Intent(context, DashboardActivity.class);
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static synchronized void openDashboardActivity(final Context context) {
		context.startActivity(getDashboardActivityIntent(context));
	}

	public static synchronized Intent getPlaylistViewActivityIntent(final Context context) {
		Intent intent = null;

		if (!Once.beenDone(Once.THIS_APP_INSTALL, IntroActivity.TAG)) {
			intent = new Intent(context, IntroActivity.class);

			Once.markDone(IntroActivity.TAG);
		} else {
			intent = new Intent(context, PlaylistViewActivity.class);
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return intent;
	}

	public static synchronized void openPlaylistViewActivity(final Context context) {
		context.startActivity(getPlaylistViewActivityIntent(context));
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

	public static void initTips(final WeakReference<FragmentActivity> contextRef) {
		if (contextRef.get() == null || contextRef.get().isFinishing() || contextRef.get().isDestroyed())
			return;

		try {
			final Context context = contextRef.get();

			ArrayList<String> messages = new ArrayList<>();

			messages.add(context.getString(R.string.tip_1));
			messages.add(context.getString(R.string.tip_2));
			messages.add(context.getString(R.string.tip_3));
			messages.add(context.getString(R.string.tip_4));
			messages.add(context.getString(R.string.tip_5));
			messages.add(context.getString(R.string.tip_6));
			messages.add(context.getString(R.string.tip_7));
			messages.add(context.getString(R.string.tip_8));
			messages.add(context.getString(R.string.tip_9));
			messages.add(context.getString(R.string.tip_10));
			messages.add(context.getString(R.string.tip_11));
			messages.add(context.getString(R.string.tip_12));
			messages.add(context.getString(R.string.tip_13));
			messages.add(context.getString(R.string.tip_14));
			messages.add(context.getString(R.string.tip_15));
			messages.add(context.getString(R.string.tip_16));
			messages.add(context.getString(R.string.tip_17));
			messages.add(context.getString(R.string.tip_18));
			messages.add(context.getString(R.string.tip_19));
			messages.add(context.getString(R.string.tip_20));
			messages.add(context.getString(R.string.tip_21));
			messages.add(context.getString(R.string.tip_22));
			messages.add(context.getString(R.string.tip_23));

			Tips tips = new Tips(contextRef.get());

			tips.setMessages(messages);

			tips.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showDialog(final Context context, String title, String content, final String link, final JavaEx.Action onFinish) {
		try {
			final AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme_AlertDialogStyle))
					.setTitle(title)
					.setMessage(content)
					.setCancelable(true)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (!TextUtils.isEmpty(link)) {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri.parse(link));
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								context.startActivity(intent);
							}

							dialogInterface.dismiss();

							if (onFinish != null)
								onFinish.execute();
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();

							if (onFinish != null)
								onFinish.execute();
						}
					})
					.create();
			alertDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
