package com.ilusons.harmony.base;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.MediaController;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.h6ah4i.android.media.IBasicMediaPlayer;
import com.h6ah4i.android.media.IMediaPlayerFactory;
import com.h6ah4i.android.media.audiofx.IBassBoost;
import com.h6ah4i.android.media.audiofx.IEnvironmentalReverb;
import com.h6ah4i.android.media.audiofx.IEqualizer;
import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.ILoudnessEnhancer;
import com.h6ah4i.android.media.audiofx.IPreAmp;
import com.h6ah4i.android.media.audiofx.IPresetReverb;
import com.h6ah4i.android.media.audiofx.IVirtualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.h6ah4i.android.media.utils.EnvironmentalReverbPresets;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.inappbilling.IabBroadcastReceiver;
import com.ilusons.harmony.ref.inappbilling.IabHelper;
import com.ilusons.harmony.ref.inappbilling.IabResult;
import com.ilusons.harmony.ref.inappbilling.Inventory;
import com.ilusons.harmony.ref.inappbilling.Purchase;
import com.ilusons.harmony.sfx.AndroidOSMediaPlayerFactory;
import com.ilusons.harmony.sfx.MediaPlayerFactory;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Logger;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2rx.RxFetch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static android.provider.Settings.Secure;

public class MusicService extends Service {

	// Logger TAG
	private static final String TAG = MusicService.class.getSimpleName();

	// Keys
	public static final String ACTION_CLOSE = TAG + ".close";
	public static final String ACTION_PREVIOUS = TAG + ".previous";
	public static final String ACTION_NEXT = TAG + ".next";
	public static final String ACTION_PLAY = TAG + ".play";
	public static final String ACTION_PAUSE = TAG + ".pause";
	public static final String ACTION_STOP = TAG + ".stop";
	public static final String ACTION_TOGGLE_PLAYBACK = TAG + ".toggle_playback";
	public static final String ACTION_RANDOM = TAG + ".random";

	public static final String ACTION_OPEN = TAG + ".open";
	public static final String KEY_URI = "uri";

	public static final String ACTION_PREPARED = TAG + ".prepared";

	public static final String ACTION_LIBRARY_UPDATE = TAG + ".library_update";
	public static final String KEY_LIBRARY_UPDATE_FORCE = "force";
	public static final String KEY_LIBRARY_UPDATE_FASTMODE = "fast_mode";
	public static final String TAG_SPREF_LIBRARY_UPDATE_FASTMODE = SPrefEx.TAG_SPREF + ".library_update_fast_mode";
	public static final boolean LIBRARY_UPDATE_FASTMODE_DEFAULT = true;
	public static final String ACTION_LIBRARY_UPDATE_BEGINS = TAG + ".library_update_begins";
	public static final String ACTION_LIBRARY_UPDATED = TAG + ".library_updated";
	public static final String ACTION_LIBRARY_UPDATE_CANCEL = TAG + ".library_update_cancel";

	public static final String ACTION_REFRESH_SYSTEM_BINDINGS = TAG + ".refresh_system_bindings";
	public static final String ACTION_REFRESH_SFX = TAG + ".sfx";

	public static final String ACTION_PLAYLIST_CHANGED = TAG + ".playlist_changed";
	public static final String KEY_PLAYLIST_CHANGED_PLAYLIST = "playlist";

	// Components
	private AudioManager audioManager;
	private final AudioManager.OnAudioFocusChangeListener audioManagerFocusListener = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(final int focusChange) {
			Log.d(TAG, "onAudioFocusChange\n" + focusChange);

			switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					// TODO: Check this
					// play();
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					pause();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					pause();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					break;
			}
		}
	};

	private MediaSessionCompat mediaSession;

	private BroadcastReceiver intentReceiver;
	private ComponentName headsetMediaButtonIntentReceiverComponent;

	private PowerManager.WakeLock wakeLock;

	//region LVL & In-app

	public static final String LICENSE_BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl5y7IyWkGhHPEooAL/8dp/vTISO1cZtpZvga6LzLcUoF1TwOyEik0gOeXGwKk/2LrTtUt+3/mvnYUZCTBhkOazRkoLeobBI8Mk+CRjZqyeboIWCsP+KdyZEJy9L/08xCR0VYODwoqTscwVjX/T5JUeM7Z5UNf+frcu7mIQYvhJDbQRuXIyDquNz1PfOMImp3bKYJVH+/5LvqifrbrrhhYedQn1DH64frePPRR+AjM6J1yl229QxN2gQaGs2AcNJHLhaOqYJYWHdwn0d+2VVA4FUeLkaFq7uxmGED4C+5NeGd2nwUl07YOB/s6beWQP+aeiRBGTDhkWrP6HbQ2PJA9wIDAQAB";

	// LVL
	private static final byte[] SALT = new byte[]{
			-46, 65, 30, -128, -103, -57, 74, -64, 51, 88, -95, -45, 77, -117, -36, -113, -11, 32, -64, 89
	};
	private LicenseCheckerCallback licenseCheckerCallback = new LicenseCheckerCallback() {
		public void allow(int policyReason) {
			Log.d(TAG, "LVL allow\n" + policyReason);

		}

		public void dontAllow(int policyReason) {
			Log.d(TAG, "LVL do not allow\n" + policyReason);

		}

		public void applicationError(int errorCode) {
			Log.d(TAG, "LVL applicationError\n" + errorCode);

			Toast.makeText(MusicService.this, "Some error occurred while doing something: " + errorCode + "!", Toast.LENGTH_LONG).show();
		}
	};
	private LicenseChecker licenseChecker;

	// IAB
	public static boolean IsPremium = BuildConfig.DEBUG;

	public static final String SKU_PREMIUM = "premium";

	public static final String TAG_SPREF_SKU_PREMIUM = SPrefEx.TAG_SPREF + ".sku_premium";

	private IabHelper iabHelper;
	private IabBroadcastReceiver iabBroadcastReceiver;
	private IabBroadcastReceiver.IabBroadcastListener iabBroadcastListener = new IabBroadcastReceiver.IabBroadcastListener() {
		@Override
		public void receivedBroadcast() {
			try {
				iabHelper.queryInventoryAsync(gotInventoryListener);
			} catch (IabHelper.IabAsyncInProgressException e) {
				Log.d(TAG, "Error querying inventory. Another async operation in progress.", e);
			}
		}
	};
	private IabHelper.QueryInventoryFinishedListener gotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.\n" + result);

			if (iabHelper == null) return;

			if (result.isFailure()) {
				return;
			}

			try {
				Purchase purchase = inventory.getPurchase(SKU_PREMIUM);

				boolean premium = ((purchase != null && verifyDeveloperPayload(MusicService.this, purchase)));

				if (!IsPremium && premium) try {
					Toast.makeText(MusicService.this, "Thank you for upgrading to premium! All premium features will work correctly after restart!", Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					Log.w(TAG, e);
				}

				IsPremium = BuildConfig.DEBUG || premium;

				SPrefEx.get(MusicService.this)
						.edit()
						.putBoolean(TAG_SPREF_SKU_PREMIUM, IsPremium)
						.apply();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	};

	private void initializeLicensing() {
		// LVL
		String deviceId =
				Secure.getString(getContentResolver(), Secure.ANDROID_ID);

		licenseChecker = new LicenseChecker(this, new ServerManagedPolicy(this, new AESObfuscator(SALT, getPackageName(), deviceId)), LICENSE_BASE64_PUBLIC_KEY);
		licenseChecker.checkAccess(licenseCheckerCallback);

		// IAB
		IsPremium = SPrefEx.get(this).getBoolean(TAG_SPREF_SKU_PREMIUM, BuildConfig.DEBUG);

		if (Analytics.getInstance().getTimeSinceFirstRun() > (3 * 24 * 60 * 60 * 1000)) {

			iabBroadcastReceiver = new IabBroadcastReceiver(iabBroadcastListener);

			iabHelper = new IabHelper(this, LICENSE_BASE64_PUBLIC_KEY);
			if (BuildConfig.DEBUG)
				iabHelper.enableDebugLogging(true, TAG);
			iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					if (!result.isSuccess()) {
						Log.w(TAG, result.toString());

						return;
					}

					if (iabHelper == null) return;

					// Important: Dynamically register for broadcast messages about updated purchases.
					// We register the receiver he re instead of as a <receiver> in the Manifest
					// because we always call getPurchases() at startup, so therefore we can ignore
					// any broadcasts sent while the app isn't running.
					// Note: registering this listener in an Activity is a bad idea, but is done here
					// because this is a SAMPLE. Regardless, the receiver must be registered after
					// IabHelper is setup, but before first call to getPurchases().
					IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
					registerReceiver(iabBroadcastReceiver, broadcastFilter);

					try {
						iabHelper.queryInventoryAsync(gotInventoryListener);
					} catch (IabHelper.IabAsyncInProgressException e) {
						Log.d(TAG, "Error querying inventory. Another async operation in progress.", e);
					}
				}
			});

		} else if (!BuildConfig.DEBUG) {

			IsPremium = true;

		}
	}

	public static boolean verifyDeveloperPayload(Context context, Purchase p) {
		return true;

		// String payload = p.getDeveloperPayload();

        /*
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

		// String localPayload = getDeveloperPayload(context, SKU_PREMIUM);

		//if (!TextUtils.isEmpty(localPayload))
		//    if (payload.toLowerCase().equals(localPayload.toLowerCase()))
		//        return true;

		//return false;
	}

	public static String getDeveloperPayload(Context context, String sku) {
		String payload = "";

		payload = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID) + ";" + sku;

		return payload;
	}

	public static void showPremiumFeatureMessage(Context context) {
		Toast.makeText(context, "It's a premium feature!", Toast.LENGTH_SHORT).show();
	}
//endregion

	public MusicService() {
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate called");

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

		initializeLicensing();

		cancelAllNotification();

		// Load playlist
		getPlaylist();

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		headsetMediaButtonIntentReceiverComponent = new ComponentName(getPackageName(), HeadsetMediaButtonIntentReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(headsetMediaButtonIntentReceiverComponent);

		// Intent handler
		intentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				Log.d(TAG, "onReceive\n" + intent);

				try {
					handleIntent(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		createIntent();

		// Initialize the wake lock
		wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		wakeLock.setReferenceCounted(false);

		setUpMediaSession();

		createDownloader();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		cancelNotification();

		cancelAllNotification();

		destroyIntent();

		destroyDownloader();

		wakeLock.release();

		audioManager.abandonAudioFocus(audioManagerFocusListener);

		if (licenseChecker != null) {
			licenseChecker.onDestroy();
		}

		if (iabBroadcastReceiver != null) try {
			unregisterReceiver(iabBroadcastReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (iabHelper != null) {
			iabHelper.disposeWhenFinished();
			iabHelper = null;
		}

		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}

		if (visualizer != null) {
			visualizer.release();
			visualizer = null;
		}

		cleanVisualizerHQ();

		cleanEqualizer();

		cleanPreAmp();

		cleanBassBoost();

		cleanLoudnessEnhancer();

		cleanVirtualizer();

		cleanEnvironmentalReverb();

		cleanPresetReverb();

		if (mediaPlayerFactory != null) {
			mediaPlayerFactory.release();
			mediaPlayerFactory = null;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand\n" + intent + "\n" + "flags=" + flags + "\nstartId=" + startId);

		try {
			handleIntent(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Service.START_NOT_STICKY;
	}

	//region Binder

	private final IBinder binder = new ServiceBinder();

	public class ServiceBinder extends Binder {

		public MusicService getService() {
			return MusicService.this;
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	//endregion

	//region Visualizer

	private IVisualizer visualizer;

	public IVisualizer getVisualizer() {
		if (visualizer == null) {
			try {
				visualizer = mediaPlayerFactory.createVisualizer(getMediaPlayer());
			} catch (UnsupportedOperationException e) {
				// the effect is not supported
			} catch (IllegalArgumentException e) {
			}
		}

		return visualizer;
	}

	private IHQVisualizer visualizerHQ;

	public IHQVisualizer getVisualizerHQ() {
		if (visualizerHQ != null)
			cleanVisualizerHQ();

		try {
			visualizerHQ = mediaPlayerFactory.createHQVisualizer();
		} catch (UnsupportedOperationException e) {
			// the effect is not supported
		} catch (IllegalArgumentException e) {
		}

		return visualizerHQ;
	}

	private void cleanVisualizerHQ() {
		if (visualizerHQ != null) {
			try {
				visualizerHQ.release();
			} catch (Exception e) {
				e.printStackTrace();
			}

			visualizerHQ = null;
		}
	}

	//endregion

	//region EQ

	private IEqualizer equalizer;

	public IEqualizer getEqualizer() {
		if (getPlayerEQEnabled(this)) try {
			if (equalizer == null) {
				try {
					switch (getPlayerType(this)) {
						case OpenSL:
							equalizer = mediaPlayerFactory.createHQEqualizer();
							break;
						case AndroidOS:
						default:
							equalizer = mediaPlayerFactory.createEqualizer(mediaPlayer);
							break;
					}

					loadEqualizer();
				} catch (Exception e) {
					// Eat?
				}
			}
		} catch (Exception e) {
			Log.w(TAG, e);

			if (equalizer != null)
				equalizer.release();
			equalizer = null;
		}
		else
			cleanEqualizer();

		return equalizer;
	}

	public void setEqualizer(boolean enabled) {
		MusicService.setPlayerEQEnabled(this, enabled);

		getEqualizer();
	}

	private void cleanEqualizer() {
		if (equalizer != null) {
			equalizer.setEnabled(false);
			equalizer.release();
			equalizer = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_EQ_ENABLED = SPrefEx.TAG_SPREF + ".player_eq_enabled";

	private static boolean getPlayerEQEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_EQ_ENABLED, false);
	}

	private static void setPlayerEQEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_EQ_ENABLED, value)
				.apply();
	}

	public void saveEqualizer() {
		if (equalizer != null)
			try {
				MusicService.setPlayerEQ(this, equalizer.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadEqualizer() {
		if (equalizer != null)
			try {
				equalizer.setProperties(getPlayerEQ(this));
				equalizer.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				equalizer.setProperties(new IEqualizer.Settings());
				equalizer.setEnabled(true);
			}
	}

	private static final String TAG_SPREF_PLAYER_EQ = SPrefEx.TAG_SPREF + ".player_eq";

	private static IEqualizer.Settings getPlayerEQ(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_EQ, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IEqualizer.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IEqualizer.Settings();
	}

	private static void setPlayerEQ(Context context, IEqualizer.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_EQ, value.toString())
				.apply();
	}

	//endregion

	//region PreAmp

	private IPreAmp preAmp;

	public IPreAmp getPreAmp() {
		if (getPlayerPreAmpEnabled(this)) try {
			if (preAmp == null) {
				try {
					preAmp = mediaPlayerFactory.createPreAmp();

					loadPreAmp();
				} catch (Exception e) {
					// Eat?
				}
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanPreAmp();

		return preAmp;
	}

	public void setPreAmp(boolean enabled) {
		MusicService.setPlayerPreAmpEnabled(this, enabled);

		getPreAmp();
	}

	private void cleanPreAmp() {
		if (preAmp != null) {
			preAmp.setEnabled(false);
			preAmp.release();
			preAmp = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_PREAMP_ENABLED = SPrefEx.TAG_SPREF + ".player_preamp_enabled";

	private static boolean getPlayerPreAmpEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_PREAMP_ENABLED, false);
	}

	private static void setPlayerPreAmpEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_PREAMP_ENABLED, value)
				.apply();
	}

	public void savePreAmp() {
		if (preAmp != null)
			try {
				MusicService.setPlayerPreAmp(this, preAmp.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadPreAmp() {
		if (preAmp != null)
			try {
				preAmp.setProperties(getPlayerPreAmp(this));
				preAmp.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				preAmp.setProperties(new IPreAmp.Settings());
				preAmp.setEnabled(true);
			}
	}

	private static final String TAG_SPREF_PLAYER_PREAMP = SPrefEx.TAG_SPREF + ".player_preamp";

	private static IPreAmp.Settings getPlayerPreAmp(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_PREAMP, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IPreAmp.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IPreAmp.Settings();
	}

	private static void setPlayerPreAmp(Context context, IPreAmp.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_PREAMP, value.toString())
				.apply();
	}

	//endregion

	//region BassBoost

	private IBassBoost bassBoost;

	public IBassBoost getBassBoost() {
		if (IsPremium && getPlayerBassBoostEnabled(this)) try {
			if (bassBoost == null)
				try {
					bassBoost = mediaPlayerFactory.createBassBoost(mediaPlayer);

					loadBassBoost();
				} catch (Exception e) {
					// Eat?
				}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanBassBoost();

		return bassBoost;
	}

	public void setBassBoost(boolean enabled) {
		MusicService.setPlayerBassBoostEnabled(this, enabled);

		getBassBoost();
	}

	private void cleanBassBoost() {
		if (bassBoost != null) {
			bassBoost.setEnabled(false);
			bassBoost.release();
			bassBoost = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_BASSBOOST_ENABLED = SPrefEx.TAG_SPREF + ".player_bassboost_enabled";

	private static boolean getPlayerBassBoostEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_BASSBOOST_ENABLED, false);
	}

	private static void setPlayerBassBoostEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_BASSBOOST_ENABLED, value)
				.apply();
	}

	public void saveBassBoost() {
		if (bassBoost != null)
			try {
				MusicService.setPlayerBassBoost(this, bassBoost.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadBassBoost() {
		if (bassBoost != null)
			try {
				bassBoost.setProperties(getPlayerBassBoost(this));
				bassBoost.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				bassBoost.setProperties(new IBassBoost.Settings());
				bassBoost.setEnabled(true);
			}
	}

	private static final String TAG_SPREF_PLAYER_BASSBOOST = SPrefEx.TAG_SPREF + ".player_bassboost";

	private static IBassBoost.Settings getPlayerBassBoost(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_BASSBOOST, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IBassBoost.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IBassBoost.Settings();
	}

	private static void setPlayerBassBoost(Context context, IBassBoost.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_BASSBOOST, value.toString())
				.apply();
	}

	//endregion

	//region LoudnessEnhancer

	private ILoudnessEnhancer loudnessEnhancer;

	public ILoudnessEnhancer getLoudnessEnhancer() {
		if (getPlayerLoudnessEnabled(this)) try {
			if (loudnessEnhancer == null)
				try {
					loudnessEnhancer = mediaPlayerFactory.createLoudnessEnhancer(mediaPlayer);

					loadLoudnessEnhancer();
				} catch (Exception e) {
					// Eat?
				}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanLoudnessEnhancer();

		return loudnessEnhancer;
	}

	public void setLoudnessEnhancer(boolean enabled) {
		MusicService.setPlayerLoudnessEnabled(this, enabled);

		getLoudnessEnhancer();
	}

	private void cleanLoudnessEnhancer() {
		if (loudnessEnhancer != null) {
			loudnessEnhancer.setEnabled(false);
			loudnessEnhancer.release();
			loudnessEnhancer = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_LOUDNESS_ENABLED = SPrefEx.TAG_SPREF + ".player_loudness_enabled";

	private static boolean getPlayerLoudnessEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_LOUDNESS_ENABLED, false);
	}

	private static void setPlayerLoudnessEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_LOUDNESS_ENABLED, value)
				.apply();
	}

	private static final String TAG_SPREF_PLAYER_LOUDNESS = SPrefEx.TAG_SPREF + ".player_loudness";

	private static ILoudnessEnhancer.Settings getPlayerLoudness(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_LOUDNESS, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new ILoudnessEnhancer.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ILoudnessEnhancer.Settings();
	}

	private static void setPlayerLoudness(Context context, ILoudnessEnhancer.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_LOUDNESS, value.toString())
				.apply();
	}

	public void saveLoudnessEnhancer() {
		if (loudnessEnhancer != null)
			try {
				MusicService.setPlayerLoudness(this, loudnessEnhancer.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadLoudnessEnhancer() {
		if (loudnessEnhancer != null)
			try {
				loudnessEnhancer.setProperties(getPlayerLoudness(this));
				loudnessEnhancer.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				loudnessEnhancer.setProperties(new ILoudnessEnhancer.Settings());
				loudnessEnhancer.setEnabled(true);
			}
	}

	//endregion

	//region Virtualizer

	private IVirtualizer virtualizer;

	public IVirtualizer getVirtualizer() {
		if (IsPremium && getPlayerVirtualizerEnabled(this)) try {
			if (virtualizer == null)
				try {
					virtualizer = mediaPlayerFactory.createVirtualizer(mediaPlayer);

					loadVirtualizer();
				} catch (Exception e) {
					// Eat?l
				}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanVirtualizer();

		return virtualizer;
	}

	public void setVirtualizer(boolean enabled) {
		MusicService.setPlayerVirtualizerEnabled(this, enabled);

		getVirtualizer();
	}

	private void cleanVirtualizer() {
		if (virtualizer != null) {
			virtualizer.setEnabled(false);
			virtualizer.release();
			virtualizer = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED = SPrefEx.TAG_SPREF + ".player_virtualizer_enabled";

	private static boolean getPlayerVirtualizerEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED, false);
	}

	private static void setPlayerVirtualizerEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED, value)
				.apply();
	}

	private static final String TAG_SPREF_PLAYER_VIRTUALIZER = SPrefEx.TAG_SPREF + ".player_virtualizer";

	private static IVirtualizer.Settings getPlayerVirtualizer(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_VIRTUALIZER, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IVirtualizer.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IVirtualizer.Settings();
	}

	private static void setPlayerVirtualizer(Context context, IVirtualizer.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_VIRTUALIZER, value.toString())
				.apply();
	}

	public void saveVirtualizer() {
		if (virtualizer != null)
			try {
				MusicService.setPlayerVirtualizer(this, virtualizer.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadVirtualizer() {
		if (virtualizer != null)
			try {
				virtualizer.setProperties(getPlayerVirtualizer(this));
				virtualizer.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				virtualizer.setProperties(new IVirtualizer.Settings());
				virtualizer.setEnabled(true);
			}
	}

	//endregion

	//region EnvironmentalReverb

	private IEnvironmentalReverb environmentalReverb;

	public IEnvironmentalReverb getEnvironmentalReverb() {
		if (IsPremium && getPlayerReverbEnvEnabled(this)) try {
			if (environmentalReverb == null)
				try {
					environmentalReverb = mediaPlayerFactory.createEnvironmentalReverb();

					loadEnvironmentalReverb();
				} catch (Exception e) {
					// Eat?
				}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanEnvironmentalReverb();

		return environmentalReverb;
	}

	public void setEnvironmentalReverb(boolean enabled) {
		MusicService.setPlayerReverbEnvEnabled(this, enabled);

		getEnvironmentalReverb();
	}

	private void cleanEnvironmentalReverb() {
		if (environmentalReverb != null) {
			environmentalReverb.setEnabled(false);
			environmentalReverb.release();
			environmentalReverb = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_REVERB_ENV_ENABLED = SPrefEx.TAG_SPREF + ".player_reverb_env_enabled";

	private static boolean getPlayerReverbEnvEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REVERB_ENV_ENABLED, false);
	}

	private static void setPlayerReverbEnvEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_REVERB_ENV_ENABLED, value)
				.apply();
	}

	private static final String TAG_SPREF_PLAYER_REVERB_ENV = SPrefEx.TAG_SPREF + ".player_reverb_env";

	private static IEnvironmentalReverb.Settings getPlayerReverbEnv(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_REVERB_ENV, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IEnvironmentalReverb.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return EnvironmentalReverbPresets.DEFAULT;
	}

	private static void setPlayerReverbEnv(Context context, IEnvironmentalReverb.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_REVERB_ENV, value.toString())
				.apply();
	}

	public void saveEnvironmentalReverb() {
		if (environmentalReverb != null)
			try {
				MusicService.setPlayerReverbEnv(this, environmentalReverb.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadEnvironmentalReverb() {
		if (environmentalReverb != null)
			try {
				environmentalReverb.setProperties(getPlayerReverbEnv(this));
				environmentalReverb.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				environmentalReverb.setProperties(EnvironmentalReverbPresets.DEFAULT);
				environmentalReverb.setEnabled(true);
			}
	}

	//endregion

	//region PresetReverb

	private IPresetReverb presetReverb;

	public IPresetReverb getPresetReverb() {
		if (getPlayerReverbPresetEnabled(this)) try {
			if (presetReverb == null)
				try {
					presetReverb = mediaPlayerFactory.createPresetReverb();

					loadPresetReverb();
				} catch (Exception e) {
					// Eat?
				}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		else
			cleanPresetReverb();

		return presetReverb;
	}

	public void setPresetReverb(boolean enabled) {
		MusicService.setPlayerReverbPresetEnabled(this, enabled);

		getPresetReverb();
	}

	private void cleanPresetReverb() {
		if (presetReverb != null) {
			presetReverb.setEnabled(false);
			presetReverb.release();
			presetReverb = null;
		}
	}

	private static final String TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED = SPrefEx.TAG_SPREF + ".player_reverb_preset_enabled";

	private static boolean getPlayerReverbPresetEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED, false);
	}

	private static void setPlayerReverbPresetEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED, value)
				.apply();
	}

	private static final String TAG_SPREF_PLAYER_REVERB_PRESET = SPrefEx.TAG_SPREF + ".player_reverb_preset";

	private static IPresetReverb.Settings getPlayerReverbPreset(Context context) {
		String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_REVERB_PRESET, "");

		if (!TextUtils.isEmpty(valueString)) try {
			return new IPresetReverb.Settings(valueString);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new IPresetReverb.Settings();
	}

	private static void setPlayerReverbPreset(Context context, IPresetReverb.Settings value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_REVERB_PRESET, value.toString())
				.apply();
	}

	public void savePresetReverb() {
		if (presetReverb != null)
			try {
				MusicService.setPlayerReverbPreset(this, presetReverb.getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void loadPresetReverb() {
		if (presetReverb != null)
			try {
				presetReverb.setProperties(getPlayerReverbPreset(this));
				presetReverb.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
				presetReverb.setProperties(new IPresetReverb.Settings());
				presetReverb.setEnabled(true);
			}
	}

	//endregion

	//region SFX

	public void updateSFX() {
		// Update player effects
		try {
			if (mediaPlayer == null)
				return;

			loadEqualizer();
			getEqualizer();

			loadPreAmp();
			getPreAmp();

			loadBassBoost();
			getBassBoost();

			loadLoudnessEnhancer();
			getLoudnessEnhancer();

			loadVirtualizer();
			getVirtualizer();

			loadEnvironmentalReverb();
			IEnvironmentalReverb environmentalReverb = getEnvironmentalReverb();
			if (environmentalReverb != null) {
				mediaPlayer.attachAuxEffect(getEnvironmentalReverb().getId());
				mediaPlayer.setAuxEffectSendLevel(1f);
			}

			loadPresetReverb();
			IPresetReverb presetReverb = getPresetReverb();
			if (presetReverb != null) {
				mediaPlayer.attachAuxEffect(getPresetReverb().getId());
				mediaPlayer.setAuxEffectSendLevel(1f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//endregion

	//region Playlist

	private static MusicServiceLibraryUpdaterAsyncTask libraryUpdater = null;

	public MusicServiceLibraryUpdaterAsyncTask getLibraryUpdater() {
		return libraryUpdater;
	}

	private Playlist currentPlaylist;

	public Playlist getPlaylist() {
		if (currentPlaylist == null)
			try {
				currentPlaylist = Playlist.loadOrCreatePlaylist(Playlist.getActivePlaylist(this));
			} catch (Exception e) {
				e.printStackTrace();
			}
		return currentPlaylist;
	}

	public void setPlaylist(String playlist) {
		try {
			currentPlaylist = Playlist.loadOrCreatePlaylist(playlist);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPlaylist(Playlist playlist) {
		try {
			currentPlaylist = playlist;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Music currentMusic;

	public Music getMusic() {
		if (currentMusic == null)
			try {
				currentMusic = Music.get(getPlaylist().getItem().getPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		return currentMusic;
	}

	private void setMusic(Music music) {
		currentMusic = music;
	}

	//endregion

	//region Player controls

	IMediaPlayerFactory mediaPlayerFactory;
	IBasicMediaPlayer mediaPlayer;

	public int getAudioSessionId() {
		if (mediaPlayer == null)
			return 0;
		return mediaPlayer.getAudioSessionId();
	}

	public IBasicMediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	public boolean canPlay() {
		return getPlaylist() != null && getPlaylist().getItems().size() >= 0;
	}

	public void seek(int position) {
		if (mediaPlayer == null) return;
		if (position < 0) {
			position = 0;
		} else if (position > mediaPlayer.getDuration()) {
			position = mediaPlayer.getDuration();
		}
		mediaPlayer.seekTo(position);

		update();
	}

	private boolean isPrepared = false;

	public boolean isPrepared() {
		return !(getMusic() == null || mediaPlayer == null || !isPrepared);
	}

	private void prepareA(final JavaEx.Action onPrepare) {
		// Fix playlist position
		if (!canPlay())
			return;

		try {
			// HACK: Calling the devil
			System.gc();
			Runtime.getRuntime().gc();
		} catch (Exception e) {
			Log.wtf(TAG, e);
		}

		synchronized (this) {
			isPrepared = false;

			Music newMusic = null;
			try {
				final String path = getPlaylist().getItem().getPath();
				try (Realm realm = Music.getDB()) {
					if (realm != null) {
						newMusic = realm.copyFromRealm(Music.get(realm, path));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (newMusic == null)
				return;

			if (getMusic().equals(newMusic) && newMusic.isLastPlaybackUrlUpdateNeeded()) {
				Toast.makeText(MusicService.this, newMusic.getText() + " cannot be played. Please restart manually!", Toast.LENGTH_LONG).show();

				return;
			}

			if (newMusic.isLastPlaybackUrlUpdateNeeded()) {
				if (AndroidEx.isNetworkAvailable(this)) {
					switch (getPlayerType(this)) {
						case OpenSL:
							download(newMusic);

							Toast.makeText(MusicService.this, newMusic.getText() + " will be played shortly. Downloading audio!", Toast.LENGTH_LONG).show();
							break;
						case AndroidOS:
						default:
							stream(newMusic);

							Toast.makeText(MusicService.this, newMusic.getText() + " will be played shortly. Streaming audio!", Toast.LENGTH_LONG).show();
							break;
					}
				} else {
					Toast.makeText(MusicService.this, newMusic.getText() + " requires internet! Skipping!", Toast.LENGTH_LONG).show();

					nextSmart(true);
				}
			} else {
				setMusic(newMusic);

				prepareB(onPrepare);
			}
		}
	}

	private boolean mediaPlayerIsInErrorState = false;

	private void prepareB(final JavaEx.Action onPrepare) {
		if (TextUtils.isEmpty(getMusic().getLastPlaybackUrl())) {
			Toast.makeText(MusicService.this, getMusic().getText() + " is not playable!", Toast.LENGTH_LONG).show();

			nextSmart(true);

			return;
		}

		synchronized (this) {
			// Setup player
			if (mediaPlayerFactory == null)
				switch (getPlayerType(this)) {
					case OpenSL:
						mediaPlayerFactory = new MediaPlayerFactory(getApplicationContext());
						break;
					case AndroidOS:
					default:
						mediaPlayerFactory = new AndroidOSMediaPlayerFactory(getApplicationContext());
						break;
				}
			if (mediaPlayer == null)
				mediaPlayer = mediaPlayerFactory.createMediaPlayer();

			try {
				mediaPlayer.reset();
				mediaPlayer.setOnPreparedListener(new IBasicMediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(IBasicMediaPlayer mediaPlayer) {
						isPrepared = true;

						if (onPrepare != null)
							onPrepare.execute();

						LocalBroadcastManager
								.getInstance(MusicService.this)
								.sendBroadcast(new Intent(ACTION_PREPARED));
					}
				});
				mediaPlayer.setDataSource(getMusic().getLastPlaybackUrl());
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mediaPlayer.prepareAsync();
			} catch (Exception e) {
				Log.w(TAG, "media init failed", e);
			}
			mediaPlayer.setOnCompletionListener(new IBasicMediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(IBasicMediaPlayer mediaPlayer) {
					Log.d(TAG, "onCompletion");

					if (getMusic() != null && !getMusic().isLastPlaybackUrlUpdateNeeded()) {
						try (Realm realm = Music.getDB()) {
							realm.executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(Realm realm) {
									getMusic().setTotalDurationPlayed(getMusic().getTotalDurationPlayed() + getPosition());

									realm.insertOrUpdate(getMusic());
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}

						// Scrobbler
						try {
							Analytics.getInstance().scrobbleLastfm(MusicService.this, getMusic());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					if (mediaPlayerIsInErrorState) {
						mediaPlayerIsInErrorState = false;
					} else {
						nextSmart(false);
					}
				}
			});
			mediaPlayer.setOnErrorListener(new IBasicMediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(IBasicMediaPlayer mediaPlayer, int what, int extra) {
					Log.w(TAG, "onError\nwhat = " + what + "\nextra = " + extra);

					isPrepared = false;

					mediaPlayerIsInErrorState = true;

					try {
						Toast.makeText(MusicService.this, "There was a problem while playing [" + getMusic().getPath() + "]!", Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						e.printStackTrace();
					}

					return false;
				}
			});

			// Update media session
			Bitmap cover;
			try {
				cover = getMusic().getCover(this, -1);
				mediaSession.setMetadata(new MediaMetadataCompat.Builder()
						.putString(MediaMetadataCompat.METADATA_KEY_TITLE, getMusic().getTitle())
						.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getMusic().getArtist())
						.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getMusic().getAlbum())
						.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
						.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getPlaylist().getItemIndex() + 1)
						.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlaylist().getItems().size())
						.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
						.build());
			} catch (Exception e) {
				e.printStackTrace();
				mediaSession.setMetadata(new MediaMetadataCompat.Builder()
						.putString(MediaMetadataCompat.METADATA_KEY_TITLE, getMusic().getTitle())
						.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getMusic().getArtist())
						.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getMusic().getAlbum())
						.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
						.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getPlaylist().getItemIndex() + 1)
						.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlaylist().getItems().size())
						.build());
			}
		}
	}

	private void prepare(final JavaEx.Action onPrepare) {
		prepareA(onPrepare);
	}

	public int getPosition() {
		if (!isPrepared() || getMusic() == null || !getMusic().isLocal())
			return -1;
		return mediaPlayer.getCurrentPosition();
	}

	public int getDuration() {
		if (!isPrepared() || getMusic() == null || !getMusic().isLocal())
			return -1;
		return mediaPlayer.getDuration();
	}

	public void stop() {
		if (mediaPlayer == null)
			return;

		Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
		intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);

		mediaPlayer.stop();

		update();

		LocalBroadcastManager
				.getInstance(this)
				.sendBroadcast(new Intent(ACTION_STOP));

		cancelNotification();
	}

	public void play() {
		if (!canPlay())
			return;

		if (!isPrepared()) {
			prepare(new JavaEx.Action() {
				@Override
				public void execute() {
					play();
				}
			});

			return;
		}

		synchronized (this) {
			int status = audioManager.requestAudioFocus(audioManagerFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

			Log.d(TAG, "Starting playback: audio focus request status = " + status);

			if (status != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
				return;

			final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
			intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
			intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
			sendBroadcast(intent);

			audioManager.registerMediaButtonEventReceiver(headsetMediaButtonIntentReceiverComponent);

			mediaSession.setActive(true);

			updateSFX();

			mediaPlayer.start();

			// Signal
			update();

			LocalBroadcastManager
					.getInstance(MusicService.this)
					.sendBroadcast(new Intent(ACTION_PLAY));

			// Update db
			if (getMusic() != null && !getMusic().isLastPlaybackUrlUpdateNeeded())
				try (Realm realm = Music.getDB()) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							getMusic().setPlayed(getMusic().getPlayed() + 1);
							getMusic().setTimeLastPlayed(System.currentTimeMillis());

							getMusic().setScore(Music.getScore(getMusic()));

							realm.insertOrUpdate(getMusic());
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

			// Scrobbler
			try {
				Analytics.getInstance().nowPlayingLastfm(this, getMusic());
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Analytics
			try {
				Analytics.getInstance().logMusicOpened(this, getMusic());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isPlaying() {
		if (mediaPlayer != null && mediaPlayer.isPlaying())
			return true;
		return false;
	}

	public void pause() {
		if (!isPlaying())
			return;

		synchronized (this) {
			Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
			intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
			intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
			sendBroadcast(intent);

			mediaPlayer.pause();

			update();

			LocalBroadcastManager
					.getInstance(this)
					.sendBroadcast(new Intent(ACTION_PAUSE));
		}
	}

	public void skip(final int position, boolean autoPlay) {
		synchronized (this) {
			if (getPlaylist().getItems().size() <= 0)
				return;
		}

		final String playlistName = getPlaylist().getName();

		try (Realm realm = Music.getDB()) {
			if (realm != null) {
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						Playlist playlist = Playlist.loadOrCreatePlaylist(realm, playlistName);
						playlist.setItemIndex(position);

						realm.insertOrUpdate(playlist);

						setPlaylist(realm.copyFromRealm(playlist));
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (getMusic() != null && !getMusic().isLastPlaybackUrlUpdateNeeded()) {
			try (Realm realm = Music.getDB()) {
				if (realm != null) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							if (((float) getPosition() / (float) getDuration()) < 0.65) {
								getMusic().setSkipped(getMusic().getSkipped() + 1);
								getMusic().setTimeLastSkipped(System.currentTimeMillis());
							}
							getMusic().setTotalDurationPlayed(getMusic().getTotalDurationPlayed() + getPosition());

							realm.insertOrUpdate(getMusic());
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Scrobbler
			try {
				Analytics.getInstance().scrobbleLastfm(this, getMusic());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (autoPlay)
			prepare(new JavaEx.Action() {
				@Override
				public void execute() {
					play();
				}
			});
		else
			prepare(null);
	}

	public void skip(int position) {
		skip(position, true);
	}

	public void random(boolean autoPlay) {
		skip((int) Math.round(Math.random() * getPlaylist().getItems().size()), autoPlay);
	}

	public void random() {
		random(true);
	}

	public void next(boolean autoPlay) {
		getPlaylist().setItemIndex(getPlaylist().getItemIndex() + 1);

		if (getPlayerShuffleMusicEnabled(this))
			random(autoPlay);
		else
			skip(getPlaylist().getItemIndex(), autoPlay);
	}

	public void next() {
		next(true);
	}

	public void prev(boolean autoPlay) {
		getPlaylist().setItemIndex(getPlaylist().getItemIndex() - 1);

		if (((float) getPosition() / (float) getDuration()) < 0.55)
			skip(getPlaylist().getItemIndex(), autoPlay);
		else
			seek(0);
	}

	public void prev() {
		prev(true);
	}

	private void nextSmart(boolean forceNext, boolean autoPlay) {
		if (!forceNext && getPlayerRepeatMusicEnabled(this)) {
			play();
		} else {
			if (getPlayerShuffleMusicEnabled(this))
				random(autoPlay);
			else
				next(autoPlay);
		}
	}

	public void nextSmart(boolean forceNext) {
		nextSmart(forceNext, true);
	}

	//endregion

	//region Notification controls

	private static final int NOTIFICATION_ID = 4524;

	private android.support.v4.app.NotificationCompat.Builder builder;

	private RemoteViews customNotificationView;
	private RemoteViews customNotificationViewS;

	private void setupNotification() {
		Intent notificationIntent = MainActivity.getPlaybackUIActivityIntent(this);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		if (customNotificationView == null) {
			customNotificationView = new RemoteViews(getPackageName(), R.layout.notification_media_view);

			customNotificationView.setOnClickPendingIntent(R.id.prev, createActionIntent(this, ACTION_PREVIOUS));
			customNotificationView.setOnClickPendingIntent(R.id.next, createActionIntent(this, ACTION_NEXT));
			customNotificationView.setOnClickPendingIntent(R.id.play_pause, createActionIntent(this, ACTION_TOGGLE_PLAYBACK));
			customNotificationView.setOnClickPendingIntent(R.id.close, createActionIntent(this, ACTION_STOP));
			customNotificationView.setOnClickPendingIntent(R.id.random, createActionIntent(this, ACTION_RANDOM));
		}

		if (customNotificationViewS == null) {
			customNotificationViewS = new RemoteViews(getPackageName(), R.layout.notification_media_view_s);

			customNotificationViewS.setOnClickPendingIntent(R.id.play_pause, createActionIntent(this, ACTION_TOGGLE_PLAYBACK));
			customNotificationViewS.setOnClickPendingIntent(R.id.close, createActionIntent(this, ACTION_STOP));
			customNotificationViewS.setOnClickPendingIntent(R.id.random, createActionIntent(this, ACTION_RANDOM));
		}

		builder = new NotificationCompat.Builder(this)
				.setContentIntent(contentIntent)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
				.setOngoing(false)
				.setDeleteIntent(createActionIntent(this, ACTION_STOP))
				.setOnlyAlertOnce(true)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setAutoCancel(true)
				/*.addAction(android.R.drawable.ic_menu_close_clear_cancel,
				        "Stop",
                        createActionIntent(this, ACTION_STOP))
                .addAction(isPlaying()
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        "Play / Pause",
                        createActionIntent(this, ACTION_TOGGLE_PLAYBACK))
                .addAction(android.R.drawable.ic_media_ff,
                        "Random",
                        createActionIntent(this, ACTION_RANDOM))*/
				.setCustomContentView(customNotificationViewS)
				.setCustomHeadsUpContentView(customNotificationViewS)
				.setCustomBigContentView(customNotificationView)
				/*.setStyle(new NotificationCompat.DecoratedMediaCustomViewStyle()
				        .setShowCancelButton(true)
                        .setCancelButtonIntent(createActionIntent(this, ACTION_STOP))
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1, 2, 0))*/;

		builder.setVisibility(Notification.VISIBILITY_PUBLIC);
	}

	private void updateNotification() {
		try {
			if (builder == null) {
				setupNotification();
			}

			if (getMusic() == null)
				return;

			Bitmap cover = getMusic().getCover(this, 128);
			if (cover == null || cover.getWidth() <= 0 || cover.getHeight() <= 0)
				cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

			customNotificationView.setImageViewBitmap(R.id.cover, cover);
			customNotificationView.setTextViewText(R.id.title, getMusic().getTitle());
			customNotificationView.setTextViewText(R.id.album, getMusic().getAlbum());
			customNotificationView.setTextViewText(R.id.artist, getMusic().getArtist());
			customNotificationView.setTextViewText(R.id.info, (getPlaylist().getItemIndex() + 1) + "/" + getPlaylist().getItems().size());
			customNotificationView.setImageViewResource(R.id.play_pause, isPlaying()
					? android.R.drawable.ic_media_pause
					: android.R.drawable.ic_media_play);

			customNotificationViewS.setImageViewBitmap(R.id.cover, cover);
			customNotificationViewS.setTextViewText(R.id.title, getMusic().getTitle());
			customNotificationViewS.setTextViewText(R.id.album, getMusic().getAlbum());
			customNotificationViewS.setTextViewText(R.id.artist, getMusic().getArtist());
			customNotificationViewS.setImageViewResource(R.id.play_pause, isPlaying()
					? android.R.drawable.ic_media_pause
					: android.R.drawable.ic_media_play);

			builder.setContentTitle(getMusic().getTitle())
					.setContentText(getMusic().getAlbum())
					.setSubText(getMusic().getArtist())
					.setLargeIcon(cover)
					.setColor(ContextCompat.getColor(getApplicationContext(), R.color.primary))
					.setTicker(getMusic().getText())
					.setOngoing(false);

			Notification currentNotification = builder.build();

			currentNotification.flags |= Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

			if (isPlaying()) {
				startForeground(NOTIFICATION_ID, currentNotification);
			} else {
				NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, currentNotification);
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void cancelNotification() {
		stopForeground(true);
		NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
	}

	private void cancelAllNotification() {
		stopForeground(true);
		NotificationManagerCompat.from(this).cancelAll();
	}

	//endregion

	//region MediaSession

	private void setUpMediaSession() {
		mediaSession = new MediaSessionCompat(this, getString(R.string.app_name));
		mediaSession.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public void onPause() {
				pause();
			}

			@Override
			public void onPlay() {
				play();
			}

			@Override
			public void onSeekTo(long pos) {
				seek((int) pos);
			}

			@Override
			public void onSkipToNext() {
				next();
			}

			@Override
			public void onSkipToPrevious() {
				prev();
			}

			@Override
			public void onStop() {
				stop();
			}
		});
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
	}

	private void updateMediaSession() {

		mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
				.setState(isPlaying()
								? PlaybackStateCompat.STATE_PLAYING
								: PlaybackStateCompat.STATE_PAUSED,
						getPosition(),
						1.0f)
				.setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
						| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
						| PlaybackStateCompat.ACTION_PLAY
						| PlaybackStateCompat.ACTION_PAUSE
						| PlaybackStateCompat.ACTION_STOP
						| PlaybackStateCompat.ACTION_PLAY_PAUSE)
				.build());

	}

	//endregion

	//region Update

	private void update() {

		// Update media session
		updateMediaSession();

		// Update notification
		updateNotification();

	}

	//endregion

	//region Open

	public void open(final String musicId) {
		try {
			Music itemToOpen = null;
			for (Music item : getPlaylist().getItems())
				if (item.getPath().equalsIgnoreCase(musicId))
					itemToOpen = item;
			if (itemToOpen != null) {
				skip(getPlaylist().getItems().lastIndexOf(itemToOpen));
			} else {
				itemToOpen = Music.load(this, musicId);
				getPlaylist().add(itemToOpen);
				skip(getPlaylist().getItems().size() - 1);
			}

			Intent broadcastIntent = new Intent(ACTION_OPEN);
			broadcastIntent.putExtra(KEY_URI, musicId);
			LocalBroadcastManager
					.getInstance(this)
					.sendBroadcast(broadcastIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void open(final Music music, final boolean insertOrUpdate) {
		if (insertOrUpdate)
			try (Realm realm = Music.getDB()) {
				if (realm != null) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							realm.insertOrUpdate(music);
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		open(music.getPath());
	}

	public void open(final Music music) {
		open(music, !Music.exists(music.getPath()));
	}

	public static void startIntentForOpen(final Context context, final String musicId) {
		try {
			Intent intent = new Intent(context.getApplicationContext(), MusicService.class);

			intent.setAction(MusicService.ACTION_OPEN);
			intent.putExtra(MusicService.KEY_URI, musicId);

			context.getApplicationContext().startService(intent);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	//endregion

	//region Intent

	private void createIntent() {
		intentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				try {
					handleIntent(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		final IntentFilter filter = new IntentFilter();

		filter.addAction(ACTION_CLOSE);
		filter.addAction(ACTION_PREVIOUS);
		filter.addAction(ACTION_NEXT);
		filter.addAction(ACTION_PLAY);
		filter.addAction(ACTION_PAUSE);
		filter.addAction(ACTION_STOP);
		filter.addAction(ACTION_OPEN);
		filter.addAction(ACTION_LIBRARY_UPDATE);
		filter.addAction(ACTION_LIBRARY_UPDATE_BEGINS);
		filter.addAction(ACTION_LIBRARY_UPDATED);
		filter.addAction(ACTION_LIBRARY_UPDATE_CANCEL);
		filter.addAction(ACTION_PLAYLIST_CHANGED);
		filter.addAction(ACTION_REFRESH_SYSTEM_BINDINGS);
		filter.addAction(ACTION_REFRESH_SFX);

		filter.addAction(Intent.ACTION_HEADSET_PLUG);

		filter.addAction(ACTION_DOWNLOADER_CANCEL);

		registerReceiver(intentReceiver, filter);
	}

	private void destroyIntent() {
		unregisterReceiver(intentReceiver);
	}

	private void handleIntent(Intent intent) {

		final String action = intent.getAction();

		if (action == null || TextUtils.isEmpty(action))
			return;

		if (action.equals(ACTION_CLOSE)) {
			stopSelf();
		} else if (action.equals(ACTION_PREVIOUS))
			prev();
		else if (action.equals(ACTION_NEXT))
			next();
		else if (action.equals(ACTION_PLAY))
			play();
		else if (action.equals(ACTION_PAUSE))
			pause();
		else if (action.equals(ACTION_STOP))
			stop();
		else if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
			if (isPlaying())
				pause();
			else
				play();
		} else if (action.equals(ACTION_RANDOM)) {
			random();
		} else if (action.equals(ACTION_OPEN)) {
			String file = intent.getStringExtra(KEY_URI);

			if (TextUtils.isEmpty(file))
				return;

			open(file);

		} else if (action.equals(ACTION_LIBRARY_UPDATE)) {
			Boolean force = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FORCE, false);
			Boolean fastMode = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FASTMODE, SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_UPDATE_FASTMODE, LIBRARY_UPDATE_FASTMODE_DEFAULT));

			libraryUpdater = new MusicServiceLibraryUpdaterAsyncTask(this, force, fastMode);
			libraryUpdater.execute();

		} else if (action.equals(ACTION_PLAYLIST_CHANGED) || action.equals(ACTION_LIBRARY_UPDATED)) {
			libraryUpdater = null;

			String playlist = null;
			try {
				playlist = intent.getStringExtra(KEY_PLAYLIST_CHANGED_PLAYLIST);
				if (TextUtils.isEmpty(playlist))
					playlist = Playlist.getActivePlaylist(this);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!TextUtils.isEmpty(playlist)) {
				// Load playlist
				setPlaylist(playlist);

				// Check last played
				// play, if it's not currently playing
				if (!isPlaying())
					if (getPlaylist().getItemIndex() > -1) {
						try {
							if (getPlaylist().getItem().isLocal()) {
								if (getPlayerType(this) != PlayerType.OpenSL) {
									prepare(null);
									// update();
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				updateNotification();
			}

		} else if (action.equals(ACTION_LIBRARY_UPDATE_CANCEL)) {

			if (libraryUpdater != null) {
				libraryUpdater.cancel(true);
			}

		} else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

			int state = intent.getIntExtra("state", -1);
			switch (state) {
				case 0:
					pause();
					break;
				case 1:
					if (getPlayerType(this) != PlayerType.OpenSL) {
						play();
					}
					break;
			}

		} else if (action.equals(ACTION_REFRESH_SYSTEM_BINDINGS)) {
			update();
		} else if (action.equals(ACTION_REFRESH_SFX)) {
			updateSFX();
		}

		// Download
		else if (action.equals(ACTION_DOWNLOADER_CANCEL)) {
			try {
				int id = intent.getIntExtra(DOWNLOADER_CANCEL_ID, -1);

				cancelDownload(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static PendingIntent createIntent(MusicService service, Intent intent) {
		intent.setComponent(new ComponentName(service, MusicService.class));
		PendingIntent pendingIntent = PendingIntent.getService(service, 0, intent, 0);
		return pendingIntent;
	}

	private static PendingIntent createActionIntent(MusicService service, String action) {
		PendingIntent pendingIntent = createIntent(service, new Intent(action));
		return pendingIntent;
	}

	//endregion

	//region Stream

	public void stream(final Music music, final boolean autoPlay) {
		Analytics.getYouTubeAudioUrl(this, music.getPath())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeOn(Schedulers.io())
				.subscribe(new Observer<String>() {
					@Override
					public void onSubscribe(Disposable d) {
						if (d.isDisposed())
							return;

						Toast.makeText(MusicService.this, "Audio stream started for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

						updateNotificationForUpdateStreamData(true);
					}

					@Override
					public void onNext(final String r) {
						if (!TextUtils.isEmpty(r)) {
							Toast.makeText(MusicService.this, "Audio streaming for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

							try (Realm realm = Music.getDB()) {
								if (realm != null) {
									realm.executeTransaction(new Realm.Transaction() {
										@Override
										public void execute(@NonNull Realm realm) {
											music.setLastPlaybackUrl(r);

											realm.insertOrUpdate(music);
										}
									});
								}
							}

							if (autoPlay)
								open(music, true);

						} else {
							Toast.makeText(MusicService.this, "Audio stream failed for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();
						}

						updateNotificationForUpdateStreamData(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(MusicService.this, "Audio stream failed for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

						updateNotificationForUpdateStreamData(false);
					}

					@Override
					public void onComplete() {
						updateNotificationForUpdateStreamData(false);
					}
				});
	}

	public void stream(final Music music) {
		stream(music, true);
	}

	private final int NOTIFICATION_ID_STREAM = 1256;
	private NotificationCompat.Builder nb_stream;

	protected void updateNotificationForUpdateStreamData(final boolean isActive) {
		if (nb_stream == null) {
			nb_stream = new NotificationCompat.Builder(this)
					.setContentTitle("Streaming ...")
					.setContentText("Streaming ...")
					.setSmallIcon(R.drawable.ic_cloud_download)
					.setOngoing(true)
					.setProgress(100, 0, true);

			NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_STREAM, nb_stream.build());
		}

		if (!isActive) {
			if (nb_stream == null)
				return;

			NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_STREAM);

			nb_stream = null;
		} else {
			nb_stream.setContentText("Finding available stream ...");

			NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_STREAM, nb_stream.build());
		}

	}

	//endregion

	//region Downloader

	public static final String ACTION_DOWNLOADER_CANCEL = TAG + ".downloader_cancel";
	public static final String DOWNLOADER_CANCEL_ID = "id";

	private ArrayList<AudioDownload> audioDownloads;

	public Collection<AudioDownload> getAudioDownloads() {
		return audioDownloads;
	}

	private AudioDownload getAudioDownloadFor(Download download) {
		AudioDownload audioDownload = null;
		for (AudioDownload item : audioDownloads)
			if (item.Download != null && item.Download.getId() == download.getId()) {
				audioDownload = item;
				audioDownload.Download = download;
				break;
			}
		return audioDownload;
	}

	private AudioDownload getAudioDownloadFor(Music m) {
		AudioDownload audioDownload = null;
		for (AudioDownload item : audioDownloads)
			if (item.Music.getPath().equals(m.getPath())) {
				audioDownload = item;
				break;
			}
		return audioDownload;
	}

	public static class AudioDownload {

		private final Context context;

		public final Music Music;

		public final boolean PlayAfterDownload;
		public final boolean AddToDatabase;

		public Download Download;

		public AudioDownload(final Context context, Music music, boolean playAfterDownload, boolean addToDatabase) {
			this.context = context;

			Music = music;

			PlayAfterDownload = playAfterDownload;
			AddToDatabase = addToDatabase;
		}

		private NotificationCompat.Builder nb;

		protected void updateNotification() {
			if (Download == null)
				return;

			if (nb == null) {
				Intent cancelIntent = new Intent(MusicService.ACTION_DOWNLOADER_CANCEL);
				cancelIntent.putExtra(DOWNLOADER_CANCEL_ID, Download.getId());
				PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);

				nb = new NotificationCompat.Builder(context)
						.setContentTitle("Downloading ...")
						.setContentText("Downloading ...")
						.setSmallIcon(R.drawable.ic_cloud_download)
						.setOngoing(true)
						.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
						.setProgress(100, 0, true);

				NotificationManagerCompat.from(context).notify(Download.getId(), nb.build());
			}

			if (Download != null && (Download.getProgress() == 100)) {
				if (nb == null)
					return;

				NotificationManagerCompat.from(context).cancel(Download.getId());

				nb = null;
			} else {
				nb.setContentText(Download.getProgress() + "% " + Music.getText() + " ...");

				NotificationManagerCompat.from(context).notify(Download.getId(), nb.build());
			}

		}

	}

	private void createDownloader() {
		audioDownloads = new ArrayList<>();
	}

	private void destroyDownloader() {
		if (fetch != null) {
			fetch.removeListener(fetchListener);
			fetch.close();
		}
	}

	private RxFetch fetch;
	private FetchListener fetchListener = new FetchListener() {
		@Override
		public void onQueued(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download queued for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCompleted(Download download) {
			final AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			Observable
					.create(new ObservableOnSubscribe<AudioDownload>() {
						@Override
						public void subscribe(ObservableEmitter<AudioDownload> oe) throws Exception {
							try {
								if (audioDownload.AddToDatabase || audioDownload.PlayAfterDownload)
									try (Realm realm = Music.getDB()) {
										if (realm != null) {
											final String lastPlaybackUrl = audioDownload.Download.getFile();

											realm.executeTransaction(new Realm.Transaction() {
												@Override
												public void execute(@NonNull Realm realm) {
													audioDownload.Music.setLastPlaybackUrl(lastPlaybackUrl);

													realm.insertOrUpdate(audioDownload.Music);
												}
											});
										}
									}

								if (audioDownload.PlayAfterDownload)
									open(audioDownload.Music, true);

								audioDownload.updateNotification();

								oe.onNext(audioDownload);

								oe.onComplete();
							} catch (Exception e) {
								oe.onError(e);
							}
						}
					})
					.observeOn(AndroidSchedulers.mainThread())
					.subscribeOn(AndroidSchedulers.mainThread())
					.subscribe();

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download completed for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onError(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download FAILED for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProgress(Download download, long l, long l1) {

		}

		@Override
		public void onPaused(Download download) {

		}

		@Override
		public void onResumed(Download download) {

		}

		@Override
		public void onCancelled(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download cancelled for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onRemoved(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download removed for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDeleted(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download deleted for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}
	};

	public RxFetch getDownloader() {
		if (fetch == null || fetch.isClosed()) {
			fetch = new RxFetch.Builder(this, TAG)
					.setDownloadConcurrentLimit(2)
					.setGlobalNetworkType(NetworkType.ALL)
					.setProgressReportingInterval(1000)
					.setLogger(new Logger() {
						@Override
						public boolean getEnabled() {
							return true;
						}

						@Override
						public void setEnabled(boolean b) {

						}

						@Override
						public void d(String s) {
							Log.d(TAG, s);
						}

						@Override
						public void d(String s, Throwable throwable) {
							Log.d(TAG, s, throwable);
						}

						@Override
						public void e(String s) {
							Log.e(TAG, s);
						}

						@Override
						public void e(String s, Throwable throwable) {
							Log.e(TAG, s, throwable);
						}
					})
					.enableLogging(true)
					.build();
			fetch.addListener(fetchListener);
			fetch.removeAll();
		}
		return fetch;
	}

	public void download(final Music music, final boolean playAfterDownload, final boolean addToDatabase) {
		final AudioDownload audioDownload = new AudioDownload(this, music, playAfterDownload, addToDatabase);

		Observable
				.create(new ObservableOnSubscribe<AudioDownload>() {
					@Override
					public void subscribe(ObservableEmitter<AudioDownload> oe) throws Exception {
						try {
							if (audioDownload.Music == null)
								throw new Exception();

							AudioDownload lastAudioDownload = getAudioDownloadFor(audioDownload.Music);
							if (lastAudioDownload != null) {
								audioDownloads.remove(lastAudioDownload);
								if (lastAudioDownload.Download != null)
									fetch.remove(lastAudioDownload.Download.getId());
							}

							audioDownloads.add(audioDownload);

							// Delete file from cache
							final File cache = IOEx.getDiskCacheFile(audioDownload.context, "yt_audio", audioDownload.Music.getPath());

							try {
								if (cache.exists())
									//noinspection ResultOfMethodCallIgnored
									cache.delete();
							} catch (Exception e) {
								e.printStackTrace();
							}

							// If url is not of YT, update it
							if (!audioDownload.Music.getPath().toLowerCase().contains("youtube")) {
								try {
									final Music forUrl = audioDownload.Music;
									Analytics.getYouTubeUrls(audioDownload.context, audioDownload.Music.getText(), 1L)
											.subscribe(new Consumer<Collection<String>>() {
												@Override
												public void accept(Collection<String> r) throws Exception {
													if (r.iterator().hasNext())
														forUrl.setPath(r.iterator().next());
												}
											}, new Consumer<Throwable>() {
												@Override
												public void accept(Throwable throwable) throws Exception {
													throwable.printStackTrace();
												}
											});
									audioDownload.Music.setPath(forUrl.getPath());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							// Find stream url
							try {
								Analytics.getYouTubeAudioUrl(audioDownload.context, audioDownload.Music.getPath())
										.observeOn(AndroidSchedulers.mainThread())
										.subscribeOn(Schedulers.io())
										.subscribe(new Consumer<String>() {
											@Override
											public void accept(final String r) throws Exception {
												try (Realm realm = Music.getDB()) {
													if (realm != null) {
														realm.executeTransaction(new Realm.Transaction() {
															@Override
															public void execute(@NonNull Realm realm) {
																audioDownload.Music.setLastPlaybackUrl(r);

																realm.insertOrUpdate(audioDownload.Music);
															}
														});
													}
												}

												createDownload(audioDownload, cache.getPath());
											}
										}, new Consumer<Throwable>() {
											@Override
											public void accept(Throwable throwable) throws Exception {
												throwable.printStackTrace();
											}
										});
							} catch (Exception e) {
								e.printStackTrace();
							}

							oe.onNext(audioDownload);

							oe.onComplete();
						} catch (Exception e) {
							oe.onError(e);
						}
					}
				})
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void download(final Music music) {
		download(music, true, true);
	}

	private void createDownload(final AudioDownload audioDownload, final String toPath) {
		getDownloader()
				.enqueue(new Request(audioDownload.Music.getLastPlaybackUrl(), toPath))
				.asObservable()
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(new Consumer<Download>() {
					@Override
					public void accept(Download download) throws Exception {
						audioDownload.Download = download;
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {

					}
				});
	}

	public void cancelDownload(int id) {
		AudioDownload audioDownload = null;
		for (AudioDownload item : audioDownloads)
			if (item.Download != null && item.Download.getId() == id) {
				audioDownload = item;
				break;
			}
		if (audioDownload != null) {
			getDownloader().remove(audioDownload.Download.getId());
			audioDownloads.remove(audioDownload);

			audioDownload.updateNotification();
		}
	}

	public void cancelDownload(final Music music) {
		AudioDownload audioDownload = null;
		for (AudioDownload item : audioDownloads)
			if (item.Music.equals(music)) {
				audioDownload = item;
				break;
			}
		if (audioDownload != null) {
			getDownloader().remove(audioDownload.Download.getId());
			audioDownloads.remove(audioDownload);

			audioDownload.updateNotification();
		}
	}

	//endregion

	//region Prefs

	public enum PlayerType {
		AndroidOS("Android OS / Device Default"),
		OpenSL("Open SL based (experimental ☢)");

		private String friendlyName;

		PlayerType(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		public String getFriendlyName() {
			return friendlyName;
		}
	}

	public static final String TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED = SPrefEx.TAG_SPREF + ".player_repeat_music_enabled";

	public static boolean getPlayerRepeatMusicEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED, false);
	}

	public static void setPlayerRepeatMusicEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_REPEAT_MUSIC_ENABLED, value)
				.apply();
	}

	public static final String TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED = SPrefEx.TAG_SPREF + ".player_shuffle_music_enabled";

	public static boolean getPlayerShuffleMusicEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED, false);
	}

	public static void setPlayerShuffleMusicEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYER_SHUFFLE_MUSIC_ENABLED, value)
				.apply();
	}

	public static final String TAG_SPREF_PLAYER_TYPE = SPrefEx.TAG_SPREF + ".player_type";

	public static PlayerType getPlayerType(Context context) {
		try {
			return PlayerType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_PLAYER_TYPE, String.valueOf(PlayerType.OpenSL)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return PlayerType.AndroidOS;
	}

	public static void setPlayerType(Context context, PlayerType value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYER_TYPE, String.valueOf(value))
				.apply();
	}

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_PLAYER_EQ,
			TAG_SPREF_PLAYER_EQ_ENABLED,
			TAG_SPREF_PLAYER_PREAMP,
			TAG_SPREF_PLAYER_PREAMP_ENABLED,
			TAG_SPREF_PLAYER_BASSBOOST,
			TAG_SPREF_PLAYER_BASSBOOST_ENABLED,
			TAG_SPREF_PLAYER_LOUDNESS,
			TAG_SPREF_PLAYER_LOUDNESS_ENABLED,
			TAG_SPREF_PLAYER_VIRTUALIZER,
			TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED,
			TAG_SPREF_PLAYER_REVERB_ENV,
			TAG_SPREF_PLAYER_REVERB_ENV_ENABLED,
			TAG_SPREF_PLAYER_REVERB_PRESET,
			TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED,
			TAG_SPREF_PLAYER_TYPE,
	};

	//endregion

}
