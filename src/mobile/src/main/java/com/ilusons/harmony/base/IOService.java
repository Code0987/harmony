package com.ilusons.harmony.base;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.IOEx;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Logger;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestInfo;
import com.tonyodev.fetch2rx.RxFetch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class IOService extends Service {

	// Logger TAG
	private static final String TAG = IOService.class.getSimpleName();

	// Keys
	public static final String ACTION_CLOSE = TAG + ".close";

	// Threads
	private Handler handler = new Handler();

	public IOService() {
	}

	@Override
	public void onCreate() {
		createIntent();

		createDownloader();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		destroyIntent();

		destroyDownloader();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			handleIntent(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Service.START_NOT_STICKY;
	}

	//region Binder

	private final IBinder binder = new ServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class ServiceBinder extends Binder {

		public IOService getService() {
			return IOService.this;
		}

	}

	//endregion

	//region Intent

	private BroadcastReceiver intentReceiver;

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
		} else if (action.equals(ACTION_DOWNLOADER_CANCEL)) try {
			int id = intent.getIntExtra(DOWNLOADER_CANCEL_ID, -1);

			cancelDownload(id);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static PendingIntent createIntent(IOService service, Intent intent) {
		intent.setComponent(new ComponentName(service, IOService.class));
		PendingIntent pendingIntent = PendingIntent.getService(service, 0, intent, 0);
		return pendingIntent;
	}

	private static PendingIntent createActionIntent(IOService service, String action) {
		PendingIntent pendingIntent = createIntent(service, new Intent(action));
		return pendingIntent;
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
				Intent cancelIntent = new Intent(IOService.ACTION_DOWNLOADER_CANCEL);
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

			if (Download != null && (Download.getProgress() == 100 || Download.getError() != Error.NONE)) {
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
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			if (audioDownload.AddToDatabase || audioDownload.PlayAfterDownload)
				try (Realm realm = DB.getDB()) {
					if (realm != null) {
						final String file = audioDownload.Download.getFile();
						final Music data = audioDownload.Music;
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								data.setLastPlaybackUrl(file);
								realm.insertOrUpdate(data);
							}
						});
					}
				}

			if (audioDownload.PlayAfterDownload)
				try {
					Intent intent = new Intent(audioDownload.context.getApplicationContext(), MusicService.class);

					intent.setAction(MusicService.ACTION_OPEN);
					intent.putExtra(MusicService.KEY_URI, audioDownload.Music.getPath());

					audioDownload.context.getApplicationContext().startService(intent);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

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

			Toast.makeText(audioDownload.context, "Download FAILED for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onRemoved(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download FAILED for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDeleted(Download download) {
			AudioDownload audioDownload = getAudioDownloadFor(download);
			if (audioDownload == null)
				return;

			audioDownload.updateNotification();

			Toast.makeText(audioDownload.context, "Download FAILED for " + audioDownload.Music.getText() + ".", Toast.LENGTH_SHORT).show();
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
		}
		return fetch;
	}

	public Observable<AudioDownload> download(final AudioDownload audioDownload, Music music, boolean playAfterDownload, boolean addToDatabase) {
		final Context context = this;

		return Observable.create(new ObservableOnSubscribe<AudioDownload>() {
			@SuppressLint("StaticFieldLeak")
			@Override
			public void subscribe(final ObservableEmitter<AudioDownload> oe) throws Exception {
				try {
					AudioDownload lastAudioDownload = getAudioDownloadFor(audioDownload.Music);
					if (lastAudioDownload != null) {
						audioDownloads.remove(lastAudioDownload);
						if (lastAudioDownload.Download != null)
							fetch.remove(lastAudioDownload.Download.getId());
					}

					audioDownloads.add(audioDownload);

					// Delete file from cache
					final File cache = IOEx.getDiskCacheFile(context, "yt_audio", audioDownload.Music.getPath());

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
							Analytics.getYouTubeUrls(context, audioDownload.Music.getText(), 1L)
									.subscribe(new Consumer<Collection<String>>() {
										@Override
										public void accept(Collection<String> r) throws Exception {
											if (r.iterator().hasNext())
												forUrl.setPath(r.iterator().next());
										}
									}, new Consumer<Throwable>() {
										@Override
										public void accept(Throwable throwable) throws Exception {

										}
									});
							audioDownload.Music.setPath(forUrl.getPath());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// Find stream url
					try {
						Analytics.getYouTubeAudioUrl(context, audioDownload.Music.getPath())
								.subscribe(new Consumer<String>() {
									@Override
									public void accept(String r) throws Exception {
										audioDownload.Music.setLastPlaybackUrl(r);
									}
								}, new Consumer<Throwable>() {
									@Override
									public void accept(Throwable throwable) throws Exception {

									}
								});
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Create download
					getDownloader()
							.enqueue(new Request(audioDownload.Music.getLastPlaybackUrl(), cache.getAbsolutePath()))
							.asObservable()
							.subscribe(new Consumer<Download>() {
								@Override
								public void accept(Download download) throws Exception {
									audioDownload.Download = download;

									oe.onNext(audioDownload);
								}
							}, new Consumer<Throwable>() {
								@Override
								public void accept(Throwable throwable) throws Exception {
									oe.onError(throwable);
								}
							});

					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public void download(Music music, boolean playAfterDownload, boolean addToDatabase) {
		final Context context = this;
		final AudioDownload audioDownload = new AudioDownload(context, music, playAfterDownload, addToDatabase);

		download(audioDownload, music, playAfterDownload, addToDatabase)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe();
	}

	public void cancelDownload(int id) {
		AudioDownload audioDownload = null;
		for (AudioDownload item : audioDownloads)
			if (item.Download != null && item.Download.getId() == id) {
				audioDownload = item;
				break;
			}
		if (audioDownload != null) {
			getDownloader().cancel(audioDownload.Download.getId());

			audioDownload.updateNotification();
		}
	}

	//endregion

}
