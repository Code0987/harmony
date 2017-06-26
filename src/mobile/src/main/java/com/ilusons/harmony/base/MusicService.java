package com.ilusons.harmony.base;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
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
import com.h6ah4i.android.media.hybrid.HybridMediaPlayerFactory;
import com.h6ah4i.android.media.standard.StandardMediaPlayerFactory;
import com.h6ah4i.android.media.utils.EnvironmentalReverbPresets;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.inappbilling.IabBroadcastReceiver;
import com.ilusons.harmony.ref.inappbilling.IabHelper;
import com.ilusons.harmony.ref.inappbilling.IabResult;
import com.ilusons.harmony.ref.inappbilling.Inventory;
import com.ilusons.harmony.ref.inappbilling.Purchase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    public static final String ACTION_LIBRARY_UPDATE = TAG + ".library_update";
    public static final String KEY_LIBRARY_UPDATE_FORCE = "force";
    public static final String KEY_LIBRARY_UPDATE_FASTMODE = "fast_mode";
    public static final String TAG_SPREF_LIBRARY_UPDATE_FASTMODE = SPrefEx.TAG_SPREF + ".library_update_fast_mode";
    public static final boolean LIBRARY_UPDATE_FASTMODE_DEFAULT = true;
    public static final String ACTION_LIBRARY_UPDATE_BEGINS = TAG + ".library_update_begins";
    public static final String ACTION_LIBRARY_UPDATED = TAG + ".library_updated";
    public static final String ACTION_LIBRARY_UPDATE_CANCEL = TAG + ".library_update_cancel";

    public static final String ACTION_REFRESH_SYSTEM_BINDINGS = TAG + ".refresh_system_bindings";

    // Threads
    private Handler handler = new Handler();

    // Binder
    private final IBinder binder = new ServiceBinder();

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

    // TODO: Add opensl support, flac, m4a, mp3, scrobbler
    IMediaPlayerFactory mediaPlayerFactory;
    IBasicMediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private MediaController mediaController;

    private BroadcastReceiver intentReceiver;
    private ComponentName headsetMediaButtonIntentReceiverComponent;

    private PowerManager.WakeLock wakeLock;

    //region LVL &amp; In-app
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

            Toast.makeText(MusicService.this, "This app is not licensed! Will close now!", Toast.LENGTH_LONG).show();

            System.exit(0);
        }

        public void applicationError(int errorCode) {
            Log.d(TAG, "LVL applicationError\n" + errorCode);

            Toast.makeText(MusicService.this, "Some error occurred while doing something: " + errorCode + "!", Toast.LENGTH_LONG).show();
        }
    };
    private LicenseChecker licenseChecker;

    // IAB
    public static boolean IsPremium = false;

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

            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);

            IsPremium = ((premiumPurchase != null && verifyDeveloperPayload(MusicService.this, premiumPurchase)));

            SPrefEx.get(MusicService.this)
                    .edit()
                    .putBoolean(TAG_SPREF_SKU_PREMIUM, IsPremium)
                    .apply();
        }
    };

    private void initializeLicensing() {
        // LVL
        String deviceId =
                Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        licenseChecker = new LicenseChecker(this, new ServerManagedPolicy(this, new AESObfuscator(SALT, getPackageName(), deviceId)), LICENSE_BASE64_PUBLIC_KEY);
        licenseChecker.checkAccess(licenseCheckerCallback);

        // IAB
        IsPremium = SPrefEx.get(this).getBoolean(TAG_SPREF_SKU_PREMIUM, false);

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
    }

    public static boolean verifyDeveloperPayload(Context context, Purchase p) {
        String payload = p.getDeveloperPayload();

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

        String localPayload = getDeveloperPayload(context, SKU_PREMIUM);

        if (!TextUtils.isEmpty(localPayload))
            if (payload.toLowerCase().equals(localPayload.toLowerCase()))
                return true;

        return false;
    }

    public static String getDeveloperPayload(Context context, String sku) {
        String payload = "";

        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                payload = parts[0];
        }

        if (TextUtils.isEmpty(payload))
            return null;

        payload = payload + ";" + sku;

        return payload;
    }
//endregion

    public MusicService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");

        initializeLicensing();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        headsetMediaButtonIntentReceiverComponent = new ComponentName(getPackageName(), HeadsetMediaButtonIntentReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(headsetMediaButtonIntentReceiverComponent);

        // Intent handler
        intentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Log.d(TAG, "onReceive\n" + intent);

                handleIntent(intent);
            }
        };

        // Attach
        registerReceiver(intentReceiver, getIntentFilter());

        // Initialize the wake lock
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);

        setUpMediaSession();

        // Init loop
        /* TODO: Review this, it's making whole android slow
        final int dt = 1500;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                updateNotification();

                handler.removeCallbacks(this);
                handler.postDelayed(this, dt);
            }
        }, dt);
        */

        setupNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelNotification();

        unregisterReceiver(intentReceiver);

        wakeLock.release();

        audioManager.abandonAudioFocus(audioManagerFocusListener);

        if (licenseChecker != null) {
            licenseChecker.onDestroy();
        }

        if (iabBroadcastReceiver != null) {
            unregisterReceiver(iabBroadcastReceiver);
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

        if (visualizerHQ != null) {
            visualizerHQ.release();
            visualizerHQ = null;
        }

        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }

        if (preAmp != null) {
            preAmp.release();
            preAmp = null;
        }

        if (bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }

        if (loudnessEnhancer != null) {
            loudnessEnhancer.release();
            loudnessEnhancer = null;
        }

        if (virtualizer != null) {
            virtualizer.release();
            virtualizer = null;
        }

        if (environmentalReverb != null) {
            environmentalReverb.release();
            environmentalReverb = null;
        }

        if (presetReverb != null) {
            presetReverb.release();
            presetReverb = null;
        }

        if (mediaPlayerFactory != null) {
            mediaPlayerFactory.release();
            mediaPlayerFactory = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand\n" + intent + "\n" + "flags=" + flags + "\nstartId=" + startId);

        handleIntent(intent);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getAudioSessionId() {
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getAudioSessionId();
    }

    private IVisualizer visualizer;

    public IVisualizer getVisualizer() {
        if (visualizer == null) {
            try {
                visualizer = mediaPlayerFactory.createVisualizer(getAudioSessionId());
            } catch (UnsupportedOperationException e) {
                // the effect is not supported
            } catch (IllegalArgumentException e) {
            }
        }

        return visualizer;
    }

    private IHQVisualizer visualizerHQ;

    public IHQVisualizer getVisualizerHQ() {
        if (visualizerHQ == null) {
            try {
                visualizerHQ = mediaPlayerFactory.createHQVisualizer();
            } catch (UnsupportedOperationException e) {
                // the effect is not supported
            } catch (IllegalArgumentException e) {
            }
        }

        return visualizerHQ;
    }

    private IEqualizer equalizer;

    public IEqualizer getEqualizer() {
        if (equalizer == null)
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
            } catch (Exception e) {
                // Eat?
            }

        return equalizer;
    }

    private IPreAmp preAmp;

    public IPreAmp getPreAmp() {
        if (preAmp == null)
            try {
                preAmp = mediaPlayerFactory.createPreAmp();
            } catch (Exception e) {
                // Eat?
            }

        return preAmp;
    }

    private IBassBoost bassBoost;

    public IBassBoost getBassBoost() {
        if (bassBoost == null)
            try {
                bassBoost = mediaPlayerFactory.createBassBoost(mediaPlayer);
            } catch (Exception e) {
                // Eat?
            }

        return bassBoost;
    }

    private ILoudnessEnhancer loudnessEnhancer;

    public ILoudnessEnhancer getLoudnessEnhancer() {
        if (loudnessEnhancer == null)
            try {
                loudnessEnhancer = mediaPlayerFactory.createLoudnessEnhancer(mediaPlayer);
            } catch (Exception e) {
                // Eat?
            }

        return loudnessEnhancer;
    }

    private IVirtualizer virtualizer;

    public IVirtualizer getVirtualizer() {
        if (virtualizer == null)
            try {
                virtualizer = mediaPlayerFactory.createVirtualizer(mediaPlayer);
            } catch (Exception e) {
                // Eat?
            }

        return virtualizer;
    }

    private IEnvironmentalReverb environmentalReverb;

    public IEnvironmentalReverb getEnvironmentalReverb() {
        if (environmentalReverb == null)
            try {
                environmentalReverb = mediaPlayerFactory.createEnvironmentalReverb();
            } catch (Exception e) {
                // Eat?
            }

        return environmentalReverb;
    }

    private IPresetReverb presetReverb;

    public IPresetReverb getPresetReverb() {
        if (presetReverb == null)
            try {
                presetReverb = mediaPlayerFactory.createPresetReverb();
            } catch (Exception e) {
                // Eat?
            }

        return presetReverb;
    }

    private MusicServiceLibraryUpdaterAsyncTask libraryUpdater = null;
    private Music currentMusic;
    private ArrayList<String> playlist = new ArrayList<>(25);
    private int playlistPosition = -1;

    public int add(String path, int position) {
        if (playlist.contains(path))
            return playlist.indexOf(path);

        playlist.add(position, path);

        return position;
    }

    public int add(String path) {
        if (playlist.contains(path))
            return playlist.indexOf(path);

        playlist.add(path);

        return playlist.size() - 1;
    }

    public void clear() {
        stop();

        playlist.clear();
    }

    public void remove(String path) {
        playlist.remove(path);
    }

    public void remove(int position) {
        playlist.remove(position);
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public ArrayList<String> getPlaylist() {
        return playlist;
    }

    public String getCurrentPlaylistItem() {
        if (playlistPosition < 0 || playlistPosition >= playlist.size())
            return null;
        return playlist.get(playlistPosition);
    }

    public IBasicMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean canPlay() {
        if (playlistPosition < 0 || playlistPosition >= playlist.size())
            playlistPosition = 0;
        if (playlistPosition < playlist.size())
            return true;
        return false;
    }

    public boolean isPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            return true;
        return false;
    }

    public int getPosition() {
        if (mediaPlayer == null)
            return -1;
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (mediaPlayer == null)
            return -1;
        return mediaPlayer.getDuration();
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

    private void prepare(final JavaEx.Action onPrepare) {
        // Fix playlist position
        if (!canPlay())
            return;

        synchronized (this) {
            String path = playlist.get(playlistPosition);

            // Decode file
            currentMusic = Music.load(this, path);
            if (currentMusic == null)
                return;

            // Setup player
            if (mediaPlayerFactory == null)
                switch (getPlayerType(this)) {
                    case OpenSL:
                        mediaPlayerFactory = new HybridMediaPlayerFactory(getApplicationContext());
                        break;
                    case AndroidOS:
                    default:
                        mediaPlayerFactory = new StandardMediaPlayerFactory(getApplicationContext());
                        break;
                }
            if (mediaPlayer == null)
                mediaPlayer = mediaPlayerFactory.createMediaPlayer();

            try {
                mediaPlayer.reset();
                if (onPrepare != null)
                    mediaPlayer.setOnPreparedListener(new IBasicMediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(IBasicMediaPlayer mediaPlayer) {
                            // Enabled effects before playing, if any
                            try {
                                if (getPlayerEQEnabled(MusicService.this))
                                    getEqualizer().setEnabled(true);
                            } catch (Exception e) {
                                Log.w(TAG, e);
                            }

                            try {
                                if (getPlayerPreAmpEnabled(MusicService.this))
                                    getPreAmp().setEnabled(true);
                            } catch (Exception e) {
                                Log.w(TAG, e);
                            }

                            onPrepare.execute();
                        }
                    });
                mediaPlayer.setDataSource(path);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.w(TAG, "media init failed", e);
            }
            mediaPlayer.setOnCompletionListener(new IBasicMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IBasicMediaPlayer mediaPlayer) {
                    Log.d(TAG, "onCompletion");

                    next();
                }
            });
            mediaPlayer.setOnErrorListener(new IBasicMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IBasicMediaPlayer mediaPlayer, int what, int extra) {
                    Log.w(TAG, "onError\nwhat = " + what + "\nextra = " + extra);

                    Toast.makeText(MusicService.this, "There was a problem while playing " + currentMusic.getText() + "!", Toast.LENGTH_LONG).show();

                    random();

                    return false;
                }
            });

            // Update media session
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMusic.Title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMusic.Artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentMusic.Album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getPlaylistPosition() + 1)
                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlaylist().size())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentMusic.getCover(this))
                    .build());

            // Update effects
            if (getPlayerPreAmpEnabled(this)) try {
                IPreAmp preAmp = getPreAmp();
                if (preAmp != null) {
                    preAmp.setProperties(getPlayerPreAmp(this));
                    preAmp.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (preAmp != null) {
                preAmp.setEnabled(false);
                preAmp.release();
                preAmp = null;
            }

            if (getPlayerEQEnabled(this)) try {
                IEqualizer equalizer = getEqualizer();
                if (equalizer != null) {
                    equalizer.setProperties(getPlayerEQ(this));
                    equalizer.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (equalizer != null) {
                equalizer.setEnabled(false);
                equalizer.release();
                equalizer = null;
            }

            if (getPlayerBassBoostEnabled(this)) try {
                IBassBoost bassBoost = getBassBoost();
                if (bassBoost != null) {
                    bassBoost.setProperties(getPlayerBassBoost(this));
                    bassBoost.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (bassBoost != null) {
                bassBoost.setEnabled(false);
                bassBoost.release();
                bassBoost = null;
            }

            if (getPlayerLoudnessEnabled(this)) try {
                ILoudnessEnhancer loudnessEnhancer = getLoudnessEnhancer();
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setProperties(getPlayerLoudness(this));
                    loudnessEnhancer.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (loudnessEnhancer != null) {
                loudnessEnhancer.setEnabled(false);
                loudnessEnhancer.release();
                loudnessEnhancer = null;
            }

            if (getPlayerVirtualizerEnabled(this)) try {
                IVirtualizer virtualizer = getVirtualizer();
                if (virtualizer != null) {
                    virtualizer.setProperties(getPlayerVirtualizer(this));
                    virtualizer.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (virtualizer != null) {
                virtualizer.setEnabled(false);
                virtualizer.release();
                virtualizer = null;
            }

            if (getPlayerReverbPresetEnabled(this)) try {
                IPresetReverb presetReverb = getPresetReverb();
                if (presetReverb != null) {
                    presetReverb.setProperties(getPlayerReverbPreset(this));
                    presetReverb.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (presetReverb != null) {
                presetReverb.setEnabled(false);
                presetReverb.release();
                presetReverb = null;
            }

            if (getPlayerReverbEnvEnabled(this)) try {
                IEnvironmentalReverb environmentalReverb = getEnvironmentalReverb();
                if (environmentalReverb != null) {
                    environmentalReverb.setProperties(getPlayerReverbEnv(this));
                    environmentalReverb.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            else if (environmentalReverb != null) {
                environmentalReverb.setEnabled(false);
                environmentalReverb.release();
                environmentalReverb = null;
            }

        }

    }

    public boolean isPrepared() {
        if (currentMusic == null || mediaPlayer == null)
            return false;
        return true;
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

        currentMusic = null;
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

            mediaPlayer.start();

            update();

            LocalBroadcastManager
                    .getInstance(MusicService.this)
                    .sendBroadcast(new Intent(ACTION_PLAY));
        }
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

    public void skip(int position) {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            playlistPosition = position;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
        }

        prepare(new JavaEx.Action() {
            @Override
            public void execute() {
                play();
            }
        });
    }

    public void next() {
        playlistPosition++;

        skip(playlistPosition);
    }

    public void prev() {
        playlistPosition--;

        skip(playlistPosition);
    }

    public void random() {
        synchronized (this) {
            if (playlist.size() <= 0)
                return;

            if (playlistPosition < 0 || playlistPosition >= playlist.size())
                playlistPosition = 0;
            else
                playlistPosition = (int) Math.round(Math.random() * playlist.size());
        }

        prepare(new JavaEx.Action() {
            @Override
            public void execute() {
                play();
            }
        });
    }

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
                .setColor(getApplicationContext().getColor(R.color.primary))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
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
        if (builder == null)
            return;

        if (currentMusic == null)
            return;

        Bitmap cover = currentMusic.getCover(this, 128);
        if (cover == null)
            cover = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        customNotificationView.setImageViewBitmap(R.id.cover, cover);
        customNotificationView.setTextViewText(R.id.title, currentMusic.Title);
        customNotificationView.setTextViewText(R.id.album, currentMusic.Album);
        customNotificationView.setTextViewText(R.id.artist, currentMusic.Artist);
        customNotificationView.setTextViewText(R.id.info, (getPlaylistPosition() + 1) + "/" + getPlaylist().size());
        customNotificationView.setImageViewResource(R.id.play_pause, isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);

        customNotificationViewS.setImageViewBitmap(R.id.cover, cover);
        customNotificationViewS.setTextViewText(R.id.title, currentMusic.Title);
        customNotificationViewS.setTextViewText(R.id.album, currentMusic.Album);
        customNotificationViewS.setTextViewText(R.id.artist, currentMusic.Artist);
        customNotificationViewS.setImageViewResource(R.id.play_pause, isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);


        builder.setContentTitle(currentMusic.Title)
                .setContentText(currentMusic.Album)
                .setSubText(currentMusic.Artist)
                .setLargeIcon(cover)
                .setColor(getApplicationContext().getColor(R.color.primary))
                .setTicker(currentMusic.getText());

        // TODO: Review this, it's making whole android slow
        // customNotificationView.setProgressBar(R.id.progress, getDuration(), getPosition(), !isPlaying());

        Notification currentNotification = builder.build();

        if (isPlaying()) {
            startForeground(NOTIFICATION_ID, currentNotification);
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, currentNotification);
        }
    }

    private void cancelNotification() {
        stopForeground(true);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    private void setUpMediaSession() {
        mediaSession = new MediaSessionCompat(this, getString(R.string.app_name));
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                // TODO: Check this
                // pause();
            }

            @Override
            public void onPlay() {
                // TODO: Check this
                // play();
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

    private void update() {

        // Update media session
        updateMediaSession();

        // Update notification
        updateNotification();

        // HACK: Calling the devil
        System.gc();
        Runtime.getRuntime().gc();
    }

    private IntentFilter getIntentFilter() {
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

        filter.addAction(Intent.ACTION_HEADSET_PLUG);

        return filter;
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

            skip(add(file));

            Intent broadcastIntent = new Intent(ACTION_OPEN);
            broadcastIntent.putExtra(KEY_URI, file);
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(broadcastIntent);

        } else if (action.equals(ACTION_LIBRARY_UPDATE)) {
            Boolean force = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FORCE, false);
            Boolean fastMode = intent.getBooleanExtra(KEY_LIBRARY_UPDATE_FASTMODE, SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_UPDATE_FASTMODE, LIBRARY_UPDATE_FASTMODE_DEFAULT));

            libraryUpdater = new MusicServiceLibraryUpdaterAsyncTask(this, force, fastMode) {
                @Override
                protected void onPostExecute(Result result) {
                    super.onPostExecute(result);

                    libraryUpdater = null;
                }
            };
            libraryUpdater.execute();

        } else if (action.equals(ACTION_LIBRARY_UPDATED)) {
            if (!isPlaying()) {
                stop();
                getPlaylist().clear();
            }

            for (Music music : MusicServiceLibraryUpdaterAsyncTask.loadIndexAll(this))
                getPlaylist().add(music.Path);

        } else if (action.equals(ACTION_LIBRARY_UPDATE_CANCEL)) {

            if (libraryUpdater != null)
                libraryUpdater.cancel(true);

        } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    pause();
                    break;
                case 1:
                    play();
                    break;
            }

        } else if (action.equals(ACTION_REFRESH_SYSTEM_BINDINGS)) {
            update();
        }

    }

    public class ServiceBinder extends Binder {

        public MusicService getService() {
            return MusicService.this;
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

    public enum PlayerType {
        AndroidOS("Android OS / Device Default"),
        OpenSL("Open SL based (experimental)");

        private String friendlyName;

        PlayerType(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }

    public static final String TAG_SPREF_PLAYER_TYPE = SPrefEx.TAG_SPREF + ".player_type";

    public static PlayerType getPlayerType(Context context) {
        return PlayerType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_PLAYER_TYPE, String.valueOf(PlayerType.AndroidOS)));
    }

    public static void setPlayerType(Context context, PlayerType value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_TYPE, String.valueOf(value))
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_EQ_ENABLED = SPrefEx.TAG_SPREF + ".player_eq_enabled";

    public static boolean getPlayerEQEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_EQ_ENABLED, false);
    }

    public static void setPlayerEQEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_EQ_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_EQ = SPrefEx.TAG_SPREF + ".player_eq";

    public static IEqualizer.Settings getPlayerEQ(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_EQ, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IEqualizer.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IEqualizer.Settings();
    }

    public static void setPlayerEQ(Context context, IEqualizer.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_EQ, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_PREAMP_ENABLED = SPrefEx.TAG_SPREF + ".player_preamp_enabled";

    public static boolean getPlayerPreAmpEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_PREAMP_ENABLED, false);
    }

    public static void setPlayerPreAmpEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_PREAMP_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_PREAMP = SPrefEx.TAG_SPREF + ".player_preamp";

    public static IPreAmp.Settings getPlayerPreAmp(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_PREAMP, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IPreAmp.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IPreAmp.Settings();
    }

    public static void setPlayerPreAmp(Context context, IPreAmp.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_PREAMP, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_BASSBOOST_ENABLED = SPrefEx.TAG_SPREF + ".player_bassboost_enabled";

    public static boolean getPlayerBassBoostEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_BASSBOOST_ENABLED, false);
    }

    public static void setPlayerBassBoostEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_BASSBOOST_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_BASSBOOST = SPrefEx.TAG_SPREF + ".player_bassboost";

    public static IBassBoost.Settings getPlayerBassBoost(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_BASSBOOST, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IBassBoost.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IBassBoost.Settings();
    }

    public static void setPlayerBassBoost(Context context, IBassBoost.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_BASSBOOST, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_LOUDNESS_ENABLED = SPrefEx.TAG_SPREF + ".player_loudness_enabled";

    public static boolean getPlayerLoudnessEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_LOUDNESS_ENABLED, false);
    }

    public static void setPlayerLoudnessEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_LOUDNESS_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_LOUDNESS = SPrefEx.TAG_SPREF + ".player_loudness";

    public static ILoudnessEnhancer.Settings getPlayerLoudness(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_LOUDNESS, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new ILoudnessEnhancer.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ILoudnessEnhancer.Settings();
    }

    public static void setPlayerLoudness(Context context, ILoudnessEnhancer.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_LOUDNESS, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED = SPrefEx.TAG_SPREF + ".player_virtualizer_enabled";

    public static boolean getPlayerVirtualizerEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED, false);
    }

    public static void setPlayerVirtualizerEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_VIRTUALIZER_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_VIRTUALIZER = SPrefEx.TAG_SPREF + ".player_virtualizer";

    public static IVirtualizer.Settings getPlayerVirtualizer(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_VIRTUALIZER, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IVirtualizer.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IVirtualizer.Settings();
    }

    public static void setPlayerVirtualizer(Context context, IVirtualizer.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_VIRTUALIZER, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED = SPrefEx.TAG_SPREF + ".player_reverb_preset_enabled";

    public static boolean getPlayerReverbPresetEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED, false);
    }

    public static void setPlayerReverbPresetEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_REVERB_PRESET_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_REVERB_PRESET = SPrefEx.TAG_SPREF + ".player_reverb_preset";

    public static IPresetReverb.Settings getPlayerReverbPreset(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_REVERB_PRESET, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IPresetReverb.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new IPresetReverb.Settings();
    }

    public static void setPlayerReverbPreset(Context context, IPresetReverb.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_REVERB_PRESET, value.toString())
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_REVERB_ENV_ENABLED = SPrefEx.TAG_SPREF + ".player_reverb_env_enabled";

    public static boolean getPlayerReverbEnvEnabled(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYER_REVERB_ENV_ENABLED, false);
    }

    public static void setPlayerReverbEnvEnabled(Context context, boolean value) {
        SPrefEx.get(context)
                .edit()
                .putBoolean(TAG_SPREF_PLAYER_REVERB_ENV_ENABLED, value)
                .apply();
    }

    public static final String TAG_SPREF_PLAYER_REVERB_ENV = SPrefEx.TAG_SPREF + ".player_reverb_env";

    public static IEnvironmentalReverb.Settings getPlayerReverbEnv(Context context) {
        String valueString = SPrefEx.get(context).getString(TAG_SPREF_PLAYER_REVERB_ENV, "");

        if (!TextUtils.isEmpty(valueString)) try {
            return new IEnvironmentalReverb.Settings(valueString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return EnvironmentalReverbPresets.DEFAULT;
    }

    public static void setPlayerReverbEnv(Context context, IEnvironmentalReverb.Settings value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_PLAYER_REVERB_ENV, value.toString())
                .apply();
    }

}
