package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Api;
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.musicbrainz.android.api.data.Artist;
import org.musicbrainz.android.api.data.Recording;
import org.musicbrainz.android.api.data.RecordingInfo;
import org.musicbrainz.android.api.data.ReleaseArtist;
import org.musicbrainz.android.api.data.ReleaseInfo;
import org.musicbrainz.android.api.data.Tag;

import java.util.Map;
import java.util.UUID;

import co.mobiwise.materialintro.animation.MaterialIntroListener;
import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.view.MaterialIntroView;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import jonathanfinerty.once.Once;
import jp.wasabeef.blurry.Blurry;

public class PlaybackUIActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = PlaybackUIActivity.class.getSimpleName();

	// UI
	private View root;

	private ImageView bg1;

//	private InterstitialAd iad;

	private ImageButton play_pause_stop;
	private ImageButton prev;
	private ImageButton next;
	private ImageButton shuffle;
	private ImageButton repeat;
	private ImageButton avfx;
	private ImageButton tune;

	private LyricsViewFragment lyricsViewFragment;
	private AudioVFXViewFragment audioVFXViewFragment;

	private AVLoadingIndicatorView loadingView;

	private ImageView cover;
	private VideoView video;
	private View av_layout;

	private int color;
	private int colorLight;

	private SeekBar seekBar;
	private TextView position_start, position_end;

	private View controls_layout;
	private View lyrics_layout;

	private TextView info;

	private Runnable videoSyncTask = new Runnable() {
		@Override
		public void run() {

			if (video != null && video.getVisibility() == View.VISIBLE)
				video.seekTo(getMusicService().getPosition());

			handler.removeCallbacks(videoSyncTask);
			handler.postDelayed(videoSyncTask, 9 * 1000);
		}
	};

	private Runnable updateSystemUITask = new Runnable() {
		@Override
		public void run() {

			View v = getWindow().getDecorView();

			if ((v.getVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
				v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("color", color);
		outState.putInt("colorLight", colorLight);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// State
		if (savedInstanceState != null)
			try {

				color = savedInstanceState.getInt("color");
				colorLight = savedInstanceState.getInt("colorLight");

			} catch (Exception e) {
				Log.w(TAG, e);
			}

		// Set view
		int layoutId = -1;
		switch (SettingsActivity.getPlaybackUIStyle(this)) {
			case PUI2:
				layoutId = R.layout.playback_ui_pui2_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case PUI3:
				layoutId = R.layout.playback_ui_pui3_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case Default:
			default:
				layoutId = R.layout.playback_ui_default_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				break;
		}
		setContentView(layoutId);

		Music.setCurrentCoverView(this);

		final View decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				handler.postDelayed(updateSystemUITask, 500);
			}
		});

		// Set views
		root = findViewById(R.id.root);

		bg1 = (ImageView) findViewById(R.id.bg1);

		loadingView = (AVLoadingIndicatorView) findViewById(R.id.loadingView);

		av_layout = findViewById(R.id.av_layout);
		av_layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleUI();
			}
		});

		controls_layout = findViewById(R.id.controls_layout);
		lyrics_layout = findViewById(R.id.lyrics_layout);

		cover = (ImageView) findViewById(R.id.cover);
		video = (VideoView) findViewById(R.id.video);

		if (getPlaybackUIAVHidden(PlaybackUIActivity.this)) {
			cover.animate().alpha(0.3f).setDuration(666).start();
		} else {
			cover.animate().alpha(1.0f).setDuration(333).start();
		}
		av_layout.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				/*
				if (!MusicService.IsPremium) {
					MusicService.showPremiumFeatureMessage(PlaybackUIActivity.this);

					return false;
				}
				*/

				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle));
				builder.setTitle("Select the action");
				builder.setItems(new CharSequence[]{
						"Fade / Show cover art",
						"Re-download cover art",
						"Search and update details",
						"Fingerprint and update details"
				}, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int itemIndex) {
						try {
							switch (itemIndex) {
								case 0:
									if (!getPlaybackUIAVHidden(PlaybackUIActivity.this)) {
										cover.animate().alpha(0.3f).setDuration(666).start();

										setPlaybackUIAVHidden(PlaybackUIActivity.this, true);
									} else {
										cover.animate().alpha(1.0f).setDuration(333).start();

										setPlaybackUIAVHidden(PlaybackUIActivity.this, false);
									}
									break;
								case 1:
									info("Downloading artwork ...", true);

									Music data = getMusicService().getMusic();
									ArtworkEx.getArtworkDownloaderTask(
											PlaybackUIActivity.this,
											data.getText(),
											ArtworkEx.ArtworkType.Song,
											-1,
											data.getPath(),
											Music.KEY_CACHE_DIR_COVER,
											data.getPath(),
											new JavaEx.ActionT<Bitmap>() {
												@Override
												public void execute(Bitmap bitmap) {
													info("Artwork downloaded!");

													onCoverReloaded(bitmap);
												}
											},
											new JavaEx.ActionT<Exception>() {
												@Override
												public void execute(Exception e) {
													info("Artwork download failed!");

													Log.w(TAG, e);
												}
											},
											1500,
											true);
									break;

								case 2: {
									info("Searching ...", true);

									final Music m = getMusicService().getMusic();

									Analytics.findTrackFromTitleArtist(m.getTitle(), m.getArtist())
											.flatMap(new Function<RecordingInfo, ObservableSource<Recording>>() {
												@Override
												public ObservableSource<Recording> apply(RecordingInfo r) throws Exception {
													return Analytics.findTrackFromMBID(r.getMbid());
												}
											})
											.subscribeOn(Schedulers.io())
											.observeOn(AndroidSchedulers.mainThread())
											.subscribe(new Consumer<Recording>() {
												@Override
												public void accept(final Recording r) throws Exception {
													info("Found something ...");

													final String mbid = r.getMbid();

													final String title = r.getTitle();

													final StringBuilder artist = new StringBuilder();
													for (ReleaseArtist ra : r.getArtists())
														artist.append(ra.getName()).append(",");
													if (artist.length() > 0)
														artist.deleteCharAt(artist.length() - 1);

													final StringBuilder release = new StringBuilder();
													for (ReleaseInfo ri : r.getReleases())
														release.append(ri.getTitle()).append(",");
													if (release.length() > 0)
														release.deleteCharAt(release.length() - 1);

													final StringBuilder tags = new StringBuilder();
													for (Tag tag : r.getTags())
														tags.append(tag.getText()).append(",");
													if (tags.length() > 0)
														tags.deleteCharAt(tags.length() - 1);

													StringBuilder sb = new StringBuilder();

													if (!TextUtils.isEmpty(title))
														sb.append("Title: ").append(title).append(System.lineSeparator());
													if (!TextUtils.isEmpty(artist))
														sb.append("Artist: ").append(artist).append(System.lineSeparator());
													if (!TextUtils.isEmpty(release))
														sb.append("Release: ").append(release).append(System.lineSeparator());
													if (!TextUtils.isEmpty(tags))
														sb.append("Tags: ").append(tags).append(System.lineSeparator());
													if (!TextUtils.isEmpty(mbid))
														sb.append("MBID: ").append(mbid).append(System.lineSeparator());

													(new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle))
															.setTitle("Apply new details?")
															.setMessage(sb.toString())
															.setCancelable(true)
															.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
																@Override
																public void onClick(DialogInterface dialogInterface, int i) {
																	try (Realm realm = DB.getDB()) {
																		if (realm == null)
																			return;

																		realm.executeTransaction(new Realm.Transaction() {
																			@Override
																			public void execute(Realm realm) {
																				if (!TextUtils.isEmpty(mbid))
																					m.setMBID(mbid);
																				if (!TextUtils.isEmpty(title))
																					m.setTitle(title);
																				if (!TextUtils.isEmpty(artist))
																					m.setArtist(artist.toString());
																				if (!TextUtils.isEmpty(release))
																					m.setAlbum(release.toString());
																				if (!TextUtils.isEmpty(tags))
																					m.setTags(StringUtils.join(m.getTags(), tags.toString(), ','));

																				realm.insertOrUpdate(m);
																			}
																		});
																	}

																	resetForUriIfNeeded(m.getPath(), true);

																	info("Music details updated, you may need to restart app to see changes!");
																}
															})
															.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
																@Override
																public void onClick(DialogInterface dialogInterface, int i) {
																	dialogInterface.dismiss();
																}
															}))
															.show();
												}
											}, new Consumer<Throwable>() {
												@Override
												public void accept(Throwable throwable) throws Exception {
													Log.w(TAG, throwable);

													info("Music details were not found over internet or some error occurred.");
												}
											});
								}
								break;
								case 3: {
									info("Generating fingerprint and looking up ...", true);

									final Music m = getMusicService().getMusic();
									Api.lookupAndUpdateMusicData(
											getMusicService(),
											m,
											new JavaEx.ActionT<Map<String, String>>() {
												@Override
												public void execute(Map<String, String> result) {
													info("Music details found!");

													final String title = result.get("title");
													final String artist = result.get("artist");
													final String album = result.get("album");
													final String score = result.get("score");
													final String id = result.get("id");

													if (TextUtils.isEmpty(title)) {
														info("Music details found were not appropriate for use!");

														return;
													}

													(new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle))
															.setTitle("Apply new details?")
															.setMessage("Title: " + title
																	+ "\nArtist: " + artist
																	+ "\nAlbum: " + album
																	+ "\nConfidence: " + ((int) (Double.parseDouble(score) * 100)) + "%")
															.setCancelable(true)
															.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
																@Override
																public void onClick(DialogInterface dialogInterface, int i) {
																	try (Realm realm = DB.getDB()) {
																		if (realm == null)
																			return;

																		realm.executeTransaction(new Realm.Transaction() {
																			@Override
																			public void execute(Realm realm) {
																				m.setTitle(title);
																				m.setArtist(artist);
																				m.setAlbum(album);

																				realm.insertOrUpdate(m);
																			}
																		});
																	}

																	resetForUriIfNeeded(m.getPath(), true);

																	info("Music details updated, you may need to restart app to see changes!");
																}
															})
															.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
																@Override
																public void onClick(DialogInterface dialogInterface, int i) {
																	dialogInterface.dismiss();
																}
															}))
															.show();
												}
											},
											new JavaEx.ActionT<Exception>() {
												@Override
												public void execute(Exception e) {
													info("Music details were not found over internet or some error occurred.");
												}
											});
								}
								break;
							}
						} catch (Exception e) {
							Log.w(TAG, e);
						}
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();

				return true;
			}
		});

		// Video, if loaded is on mute
		if (video != null) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				video.setZOrderOnTop(false);
			} else {
				video.setZOrderOnTop(true);
			}
			video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mediaPlayer) {
					mediaPlayer.setVolume(0, 0);

					if (getMusicService() != null)
						getMusicService().play();

					handler.postDelayed(videoSyncTask, 1000);

					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							toggleUI(true);
						}
					}, 3500);
				}
			});
			video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					video.setVisibility(View.INVISIBLE);

					handler.removeCallbacks(videoSyncTask);

					handler.post(new Runnable() {
						@Override
						public void run() {
							toggleUI(false);
						}
					});
				}
			});
			video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
					video.setVisibility(View.INVISIBLE);

					handler.removeCallbacks(videoSyncTask);

					handler.post(new Runnable() {
						@Override
						public void run() {
							toggleUI(false);
						}
					});

					return false;
				}
			});
		}

		color = ContextCompat.getColor(getApplicationContext(), R.color.accent);
		colorLight = ContextCompat.getColor(getApplicationContext(), R.color.accent);

		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				if (!b) return;

				if (getMusicService() != null) {
					getMusicService().seek(i);

					if (video != null && video.getVisibility() == View.VISIBLE)
						video.seekTo(getMusicService().getPosition());
				}

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		position_start = (TextView) findViewById(R.id.position_start);
		position_end = (TextView) findViewById(R.id.position_end);

		play_pause_stop = (ImageButton) findViewById(R.id.play_pause_stop);
		prev = (ImageButton) findViewById(R.id.prev);
		next = (ImageButton) findViewById(R.id.next);
		shuffle = (ImageButton) findViewById(R.id.shuffle);
		repeat = (ImageButton) findViewById(R.id.repeat);
		avfx = (ImageButton) findViewById(R.id.avfx);
		tune = (ImageButton) findViewById(R.id.tune);

		play_pause_stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null && getMusicService().isPlaying()) {
					getMusicService().pause();
				} else {
					getMusicService().play();
				}
			}
		});
		play_pause_stop.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (getMusicService() != null) {
					getMusicService().stop();

					info("Stopped!");
				}

				return true;
			}
		});

		prev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null) {
					getMusicService().prev();
				}
			}
		});
		prev.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (getMusicService() != null) {
					getMusicService().random();
				}

				return true;
			}
		});

		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null) {
					getMusicService().next();
				}
			}
		});
		next.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (getMusicService() != null) {
					getMusicService().random();
				}

				return true;
			}
		});

		if (MusicService.getPlayerShuffleMusicEnabled(PlaybackUIActivity.this))
			shuffle.setAlpha(0.9f);
		else
			shuffle.setAlpha(0.3f);
		shuffle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null) {
					try {
						boolean value = MusicService.getPlayerShuffleMusicEnabled(PlaybackUIActivity.this);

						value = !value;

						MusicService.setPlayerShuffleMusicEnabled(PlaybackUIActivity.this, value);

						if (value)
							info("Shuffle turned ON");
						else
							info("Shuffle turned OFF");

						if (value)
							shuffle.setAlpha(0.9f);
						else
							shuffle.setAlpha(0.3f);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		shuffle.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (getMusicService() != null) {
					getMusicService().random();
				}

				return true;
			}
		});

		if (MusicService.getPlayerRepeatMusicEnabled(PlaybackUIActivity.this))
			repeat.setAlpha(0.9f);
		else
			repeat.setAlpha(0.3f);
		repeat.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null) {
					boolean value = MusicService.getPlayerRepeatMusicEnabled(PlaybackUIActivity.this);

					value = !value;

					MusicService.setPlayerRepeatMusicEnabled(PlaybackUIActivity.this, value);

					if (value)
						info("Repeat turned ON");
					else
						info("Repeat turned OFF");

					if (value)
						repeat.setAlpha(0.9f);
					else
						repeat.setAlpha(0.3f);
				}
			}
		});

		createAVFX();

		tune.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(PlaybackUIActivity.this, TuneActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		});

		// Info
		info = (TextView) findViewById(R.id.info);

		// Guide
		root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				initHelp();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		handler.removeCallbacks(videoSyncTask);
	}

	@Override
	protected void onResume() {
		super.onResume();

		handler.postDelayed(updateSystemUITask, 500);

		// Restore audio vis.
		updateAVFX();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		MainActivity.openDashboardActivity(this);

		super.onBackPressed();
	}

	@Override
	protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
		super.OnMusicServiceChanged(className, musicService, isBound);

		if (musicService.getMusic() == null)
			return;

		final String path = musicService.getMusic().getPath();

		getWindow().getDecorView().post(new Runnable() {
			@Override
			public void run() {
				resetForUriIfNeeded(path);
			}
		});

		// Restore audio vis.
		updateAVFX();
	}

	@Override
	public void OnMusicServicePlay() {
		super.OnMusicServicePlay();

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_pause_black));

		try {
			if (getMusicService() != null) {
				if (getMusicService().getMusic() == null)
					return;
				final String path = getMusicService().getMusic().getPath();
				resetForUriIfNeeded(path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (video != null && video.getVisibility() == View.VISIBLE)
			video.start();
	}

	@Override
	public void OnMusicServicePause() {
		super.OnMusicServicePlay();

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_play_black));

		try {
			if (getMusicService() != null) {
				if (getMusicService().getMusic() == null)
					return;
				final String path = getMusicService().getMusic().getPath();
				resetForUriIfNeeded(path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (video != null && video.getVisibility() == View.VISIBLE)
			video.pause();
	}

	@Override
	public void OnMusicServiceStop() {
		super.OnMusicServicePlay();

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_play_black));

		if (video != null && video.getVisibility() == View.VISIBLE) {
			video.stopPlayback();
			video.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void OnMusicServiceOpen(String uri) {
		super.OnMusicServiceOpen(uri);

		toggleUI(false);

		resetForUriIfNeeded(uri);
	}

	private boolean isUIHidden = false;

	private void toggleUI(boolean hide) {
		if (hide && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			controls_layout.animate().alpha(0).setDuration(500).start();
			if (lyrics_layout != null)
				lyrics_layout.animate().alpha(0).setDuration(500).start();
			seekBar.animate().alpha(0).setDuration(500).start();

			isUIHidden = true;
		} else {
			controls_layout.animate().alpha(1).setDuration(500).start();
			if (lyrics_layout != null)
				lyrics_layout.animate().alpha(1).setDuration(500).start();
			seekBar.animate().alpha(1).setDuration(500).start();

			isUIHidden = false;
		}

	}

	private void toggleUI() {
		toggleUI(!isUIHidden);
	}

	private String currentUri;

	private void resetForUriIfNeeded(String uri, boolean force) {
		Log.d(TAG, "resetForUri\n" + uri);

		if (!force)
			if (currentUri != null && currentUri.equals(uri))
				return;

		currentUri = uri;

		loadingView.smoothToShow();

		if (video != null && video.getVisibility() == View.VISIBLE) {
			video.stopPlayback();
			video.setVisibility(View.INVISIBLE);
		}

		try {
			final Music music = Music.load(this, uri);

			if (music != null) {

				loadingView.smoothToShow();

				if (info != null) {
					String s;
					try {
						s = music.getTextDetailedMultiLine(getMusicService().getPlaylist().getItemIndex());
					} catch (Exception e) {
						e.printStackTrace();

						s = music.getTextDetailedMultiLine();
					}
					info.setText(s);
				}

				try {
					Music.getCoverOrDownload(cover.getWidth(), music);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Load video
				if (video != null && music.hasVideo()) {
					video.setVisibility(View.VISIBLE);
					video.setVideoPath(music.getPath());
					video.requestFocus();
					video.start();
				}

				if (video != null && music.hasVideo() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					root.setBackground(null);
				}

				loadingView.smoothToHide();

				if (lyrics_layout != null) {

					if (lyricsViewFragment != null && lyricsViewFragment.isAdded()) {
						lyricsViewFragment.reset(music, (long) Math.max(music.getLength(), getMusicService().getDuration()));
					} else if (!isFinishing()) {
						lyricsViewFragment = LyricsViewFragment.create(music.getPath(), (long) Math.max(music.getLength(), getMusicService().getDuration()));
						getFragmentManager()
								.beginTransaction()
								.replace(R.id.lyrics_layout, lyricsViewFragment)
								.commit();
					}
				}

				seekBar.setMax(getMusicService().getDuration());
				position_end.setText(DurationFormatUtils.formatDuration(getMusicService().getDuration(), "mm:ss", false));

				setupProgressHandler();
			}

		} catch (Exception e) {
			Log.e(TAG, "open file", e);
		}

		try {
			if (getMusicService().isPlaying())
				OnMusicServicePlay();
			else
				OnMusicServicePause();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void resetForUriIfNeeded(String uri) {
		if (TextUtils.isEmpty(uri))
			return;

		resetForUriIfNeeded(uri, false);
	}

	public void onCoverReloaded(Bitmap bitmap) {
		try {
			if (bitmap == null)
				bitmap = CacheEx.getInstance().getBitmap(String.valueOf(R.drawable.logo));

			if (bitmap == null) {
				bitmap = ((BitmapDrawable) getDrawable(R.drawable.logo)).getBitmap();

				CacheEx.getInstance().putBitmap(String.valueOf(R.drawable.logo), bitmap);
			}
		} catch (Exception e) {
			// Eat!
		}

		loadingView.smoothToShow();

		if (bitmap == null)
			return;

		// Refresh system bindings
		try {
			Intent musicServiceIntent = new Intent(PlaybackUIActivity.this, MusicService.class);
			musicServiceIntent.setAction(MusicService.ACTION_REFRESH_SYSTEM_BINDINGS);
			startService(musicServiceIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Palette palette = Palette.from(bitmap).generate();
		color = ContextCompat.getColor(getApplicationContext(), R.color.accent);
		int colorBackup = color;
		color = palette.getVibrantColor(color);
		if (color == colorBackup)
			color = palette.getDarkVibrantColor(color);
		if (color == colorBackup)
			color = palette.getDarkMutedColor(color);

		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.30f); // lum +30%
		colorLight = ColorUtils.HSLToColor(hsl);

		seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
		seekBar.getThumb().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

		play_pause_stop.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		play_pause_stop.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		prev.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		next.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		shuffle.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		repeat.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		avfx.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		tune.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

		if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
			audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
		}

		if (!(root.getBackground() == null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 180)));

			if (bg1 != null)
				try {
					Blurry.with(this)
							.radius(25)
							.sampling(1)
							.color(Color.argb(160, 0, 0, 0))
							.animate(450)
							.async()
							.from(bitmap)
							.into(bg1);
				} catch (Exception e) {
					e.printStackTrace();
				}
		} else {
			if (bg1 != null)
				bg1.setImageDrawable(null);
		}

		switch (SettingsActivity.getPlaybackUIStyle(this)) {
			case PUI2:
				Blurry.with(this)
						.radius(25)
						.sampling(1)
						.color(Color.argb(160, 0, 0, 0))
						.async()
						.animate(450)
						.from(bitmap)
						.into(cover);
				break;

			case PUI3:
				if (cover.getDrawable() != null) {
					TransitionDrawable d = new TransitionDrawable(new Drawable[]{
							cover.getDrawable(),
							new BitmapDrawable(getResources(), bitmap)
					});

					cover.setImageDrawable(d);

					d.setCrossFadeEnabled(true);
					d.startTransition(450);
				} else {
					cover.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
				}
				break;

			case Default:
			default:
				if (cover.getDrawable() != null) {
					TransitionDrawable d = new TransitionDrawable(new Drawable[]{
							cover.getDrawable(),
							new BitmapDrawable(getResources(), bitmap)
					});

					cover.setImageDrawable(d);

					d.setCrossFadeEnabled(true);
					d.startTransition(200);
				} else {
					cover.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
				}
				break;
		}

		loadingView.smoothToHide();
	}

	private Runnable progressHandlerRunnable;

	private void setupProgressHandler() {
		if (progressHandlerRunnable != null)
			handler.removeCallbacks(progressHandlerRunnable);

		final int dt = (int) (1000.0 / 24.0);

		progressHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				if (getMusicService() != null && getMusicService().isPlaying()) {

					seekBar.setProgress(getMusicService().getPosition());
					position_start.setText(DurationFormatUtils.formatDuration(getMusicService().getPosition(), "mm:ss", false));

					if (lyricsViewFragment != null && lyricsViewFragment.isAdded())
						lyricsViewFragment.updateScroll(getMusicService().getPosition());
				}

				if (isFinishing())
					return;
				if (isDestroyed())
					return;

				handler.removeCallbacks(progressHandlerRunnable);
				handler.postDelayed(progressHandlerRunnable, dt);
			}
		};
		handler.postDelayed(progressHandlerRunnable, dt);

	}

	//region AVFX

	private View avfx_layout;

	private void createAVFX() {
		avfx_layout = findViewById(R.id.avfx_layout);

		avfx.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (audioVFXViewFragment != null) {
					AudioVFXViewFragment.setAVFXEnabled(PlaybackUIActivity.this, false);
				} else {
					AudioVFXViewFragment.setAVFXEnabled(PlaybackUIActivity.this, true);
				}
				updateAVFX();
			}
		});
		avfx.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {

					AudioVFXViewFragment.setAVFXType(getApplicationContext(), AudioVFXViewFragment.getNextAVFXType(getApplicationContext()));

					audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);

					info("Now using " + AudioVFXViewFragment.getAVFXType(getApplicationContext()) + " fx!");
				}

				return true;
			}
		});
	}

	private void updateAVFX() {
		if (AudioVFXViewFragment.getAVFXEnabled(this)) {
			if (!isFinishing() && audioVFXViewFragment == null) {
				avfx_layout.setVisibility(View.VISIBLE);

				audioVFXViewFragment = AudioVFXViewFragment.create();
				getFragmentManager()
						.beginTransaction()
						.replace(R.id.avfx_layout, audioVFXViewFragment)
						.commit();

				audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
			}
		} else {
			if (audioVFXViewFragment != null) {
				getFragmentManager()
						.beginTransaction()
						.remove(audioVFXViewFragment)
						.commit();

				audioVFXViewFragment = null;

				avfx_layout.setVisibility(View.INVISIBLE);
			}
		}

	}

	//endregion

	//region Help

	private void initHelp() {
		final String tag_guide = TAG + ".guide";

		if (Once.beenDone(Once.THIS_APP_INSTALL, tag_guide))
			return;

		(new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
				.setTitle("Tour?")
				.setMessage("Would you like a short tour, highlighting the basic usage of this screen?")
				.setCancelable(true)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						tour(new MaterialIntroListener() {
							@Override
							public void onUserClicked(String usageId) {
								try {
									Once.markDone(tag_guide);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						try {
							Once.markDone(tag_guide);
						} catch (Exception e) {
							e.printStackTrace();
						}

						dialogInterface.dismiss();
					}
				}))
				.show();

	}

	private void tour(MaterialIntroListener onFinal) {
		final MaterialIntroView.Builder guide_play_pause_stop = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Press to Play/Pause, and long press to Stop current item.")
				.setTarget(play_pause_stop)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_next = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Press to skip to next item in playlist. Long press to skip to random item.")
				.setTarget(next)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_avfx = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Press to enable visualizations. Long press to cycle between various styles.")
				.setTarget(avfx)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_tune = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Press to open Tune view. You can fine tune your sound here!")
				.setTarget(tune)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_lyrics = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("This is lyrics view. Turn on your internet for automatic lyrics. Long press to options menu.")
				.setTarget(lyrics_layout == null ? root : lyrics_layout)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_cover = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Cover art, video, visualizations will be here (in that order)!")
				.setTarget(cover)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_final = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(getApplicationContext(), R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("That's all! Now go play something!")
				.setTarget(play_pause_stop)
				.setUsageId(UUID.randomUUID().toString());

		guide_final.setListener(onFinal);
		guide_cover.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_final.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_lyrics.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_cover.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_tune.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_lyrics.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_avfx.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_tune.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_next.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_avfx.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_play_pause_stop.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_next.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		try {
			guide_play_pause_stop.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//endregion

	public static final String TAG_SPREF_PLAYBACK_UI_AV_HIDDEN = SPrefEx.TAG_SPREF + ".playback_ui_av_hidden";

	public static boolean getPlaybackUIAVHidden(Context context) {
		try {
			return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYBACK_UI_AV_HIDDEN, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void setPlaybackUIAVHidden(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYBACK_UI_AV_HIDDEN, value)
				.apply();
	}

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_PLAYBACK_UI_AV_HIDDEN
	};

}
