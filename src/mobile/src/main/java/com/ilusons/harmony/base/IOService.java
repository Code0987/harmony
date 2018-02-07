package com.ilusons.harmony.base;

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
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.IOEx;
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
		} else if (action.equals(ACTION_UPDATE_STREAM_DATA)) try {
			String id = intent.getStringExtra(UPDATE_STREAM_DATA_MUSIC_ID);

			updateStreamData(id, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		else if (action.equals(ACTION_DOWNLOADER_SCHEDULE_AUDIO)) try {
			String id = intent.getStringExtra(DOWNLOADER_SCHEDULE_MUSIC_ID);

			download(id, true, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		else if (action.equals(ACTION_DOWNLOADER_CANCEL)) try {
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

	//region Stream

	public static final String ACTION_UPDATE_STREAM_DATA = TAG + ".stream_update_data";
	public static final String UPDATE_STREAM_DATA_MUSIC_ID = "id";

	private void updateStreamData(final String musicId, final boolean autoPlay) {
		final Music music = Music.get(musicId);
		if (music == null)
			return;

		Analytics.getYouTubeAudioUrl(this, music.getPath())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeOn(Schedulers.io())
				.subscribe(new Observer<String>() {
					@Override
					public void onSubscribe(Disposable d) {
						if (d.isDisposed())
							return;

						Toast.makeText(IOService.this, "Audio stream started for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

						updateNotificationForUpdateStreamData(true);
					}

					@Override
					public void onNext(final String r) {
						if (!TextUtils.isEmpty(r)) {
							Toast.makeText(IOService.this, "Audio streaming for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

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
								MusicService.startIntentForOpen(IOService.this, music.getPath());

						} else {
							Toast.makeText(IOService.this, "Audio stream failed for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();
						}

						updateNotificationForUpdateStreamData(true);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(IOService.this, "Audio stream failed for [" + music.getText() + "] ...", Toast.LENGTH_LONG).show();

						updateNotificationForUpdateStreamData(false);
					}

					@Override
					public void onComplete() {
						updateNotificationForUpdateStreamData(false);
					}
				});
	}

	private final int NOTIFICATION_ID_UPDATE_STREAM_DATA = 1256;
	private NotificationCompat.Builder nb_update_stream_data;

	protected void updateNotificationForUpdateStreamData(final boolean isActive) {
		if (nb_update_stream_data == null) {
			nb_update_stream_data = new NotificationCompat.Builder(this)
					.setContentTitle("Streaming ...")
					.setContentText("Streaming ...")
					.setSmallIcon(R.drawable.ic_cloud_download)
					.setOngoing(true)
					.setProgress(100, 0, true);

			NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_UPDATE_STREAM_DATA, nb_update_stream_data.build());
		}

		if (isActive) {
			if (nb_update_stream_data == null)
				return;

			NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_UPDATE_STREAM_DATA);

			nb_update_stream_data = null;
		} else {
			nb_update_stream_data.setContentText("Finding available stream ...");

			NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_UPDATE_STREAM_DATA, nb_update_stream_data.build());
		}

	}

	public static void startIntentForUpdateStreamData(final Context context, final String musicId) {
		try {
			Intent intent = new Intent(context.getApplicationContext(), IOService.class);

			intent.setAction(IOService.ACTION_UPDATE_STREAM_DATA);
			intent.putExtra(IOService.UPDATE_STREAM_DATA_MUSIC_ID, musicId);

			context.getApplicationContext().startService(intent);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	//endregion

	//region Downloader

	public static final String ACTION_DOWNLOADER_SCHEDULE_AUDIO = TAG + ".downloader_schedule_audio";
	public static final String DOWNLOADER_SCHEDULE_MUSIC_ID = "id";

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
				try (Realm realm = Music.getDB()) {
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
				MusicService.startIntentForOpen(audioDownload.context, audioDownload.Music.getPath());

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

	private void download(final String musicKey, final boolean playAfterDownload, final boolean addToDatabase) {
		final AudioDownload audioDownload = new AudioDownload(this, Music.get(musicKey), playAfterDownload, addToDatabase);

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

	public static void startIntentForScheduleDownload(final Context context, final String musicId) {
		try {
			Intent intent = new Intent(context.getApplicationContext(), IOService.class);

			intent.setAction(IOService.ACTION_DOWNLOADER_SCHEDULE_AUDIO);
			intent.putExtra(IOService.DOWNLOADER_SCHEDULE_MUSIC_ID, musicId);

			context.getApplicationContext().startService(intent);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	//endregion

}
