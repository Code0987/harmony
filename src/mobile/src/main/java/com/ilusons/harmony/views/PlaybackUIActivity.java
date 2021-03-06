package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.AndroidTouchEx;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ui.CircleIndicator;
import com.scwang.wave.MultiWaveHeader;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.musicbrainz.android.api.data.Recording;
import org.musicbrainz.android.api.data.RecordingInfo;
import org.musicbrainz.android.api.data.ReleaseArtist;
import org.musicbrainz.android.api.data.ReleaseInfo;
import org.musicbrainz.android.api.data.Tag;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import jp.wasabeef.blurry.Blurry;

public class PlaybackUIActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = PlaybackUIActivity.class.getSimpleName();

	// UI

	private AVLoadingIndicatorView loading;

	private int color;
	private int colorLight;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("color", color);
		outState.putInt("colorLight", colorLight);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// State
		if (savedInstanceState != null) {
			try {
				color = savedInstanceState.getInt("color");
				colorLight = savedInstanceState.getInt("colorLight");
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		} else {
			color = ContextCompat.getColor(getApplicationContext(), R.color.accent);
			colorLight = ContextCompat.getColor(getApplicationContext(), R.color.accent);
		}

		// Set view
		setContentView(R.layout.playback_ui_activity);

		// Hacks
		applyHacksToUI();

		// Set views

		loading = (AVLoadingIndicatorView) findViewById(R.id.loading);

		createRoot();

		createMetadata();

		createLyrics();

		createControls();

		createAVFX();

	}

	@Override
	protected void onStart() {
		super.onStart();

		if (viewPager != null)
			viewPager.addOnPageChangeListener(onPageChangeListener);
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (viewPager != null)
			viewPager.removeOnPageChangeListener(onPageChangeListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		handler.removeCallbacks(videoSyncTask);
	}

	@Override
	protected void onResume() {
		super.onResume();

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

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_music_pause));

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

		wave.start();
	}

	@Override
	public void OnMusicServicePause() {
		super.OnMusicServicePlay();

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_music_play));

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

		wave.stop();
	}

	@Override
	public void OnMusicServiceStop() {
		super.OnMusicServicePlay();

		play_pause_stop.setImageDrawable(getDrawable(R.drawable.ic_music_play));

		if (video != null && video.getVisibility() == View.VISIBLE) {
			video.stopPlayback();
			video.setVisibility(View.INVISIBLE);
		}

		wave.stop();
	}

	@Override
	public void OnMusicServiceOpen(String uri) {
		super.OnMusicServiceOpen(uri);

		resetForUriIfNeeded(uri);
	}

	//region Other

	private void applyHacksToUI() {
		View nav_bar_filler = findViewById(R.id.nav_bar_filler);
		if (nav_bar_filler != null) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) nav_bar_filler.getLayoutParams();
			params.bottomMargin = AndroidEx.getNavigationBarSize(this).y;
		}
	}

	//endregion

	//region Refresh

	private String currentUri;

	private void resetForUriIfNeeded(String uri, boolean force) {
		Log.d(TAG, "resetForUri\n" + uri);

		if (!force)
			if (currentUri != null && currentUri.equals(uri))
				return;

		currentUri = uri;

		loading.smoothToShow();

		if (video != null && video.getVisibility() == View.VISIBLE) {
			video.stopPlayback();
			video.setVisibility(View.INVISIBLE);
		}

		try {
			final Music music = Music.load(this, uri);

			if (music != null) {

				loading.smoothToShow();

				updateMetadata(music);

				// Load cover
				try {
					final Context context = this;
					final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
						@Override
						public void accept(Bitmap bitmap) throws Exception {
							try {
								onCoverReloaded(bitmap);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					final Consumer<Throwable> throwableConsumer = new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {
						}
					};
					final Consumer<Throwable> throwableConsumerWithRetry = new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {
							Music
									.loadLocalOrSearchCoverArtFromItunes(
											context,
											music,
											music.getCoverPath(context),
											music.getText(),
											false,
											ImageEx.ItunesImageType.Artist)
									.observeOn(AndroidSchedulers.mainThread())
									.subscribeOn(Schedulers.computation())
									.subscribe(
											resultConsumer,
											throwableConsumer);
						}
					};
					Music
							.loadLocalOrSearchCoverArtFromItunes(
									context,
									music,
									music.getCoverPath(context),
									music.getText(),
									false,
									ImageEx.ItunesImageType.Song)
							.observeOn(AndroidSchedulers.mainThread())
							.subscribeOn(Schedulers.computation())
							.subscribe(
									resultConsumer,
									throwableConsumerWithRetry);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Load video
				if (video != null && music.hasVideo()) {
					toggleVideo(getPlaybackUIVideoHidden(this));
					video.setVideoPath(music.getPath());
					video.requestFocus();
					video.start();
				}

				if (video != null && music.hasVideo() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					root.setBackground(null);
				}

				loading.smoothToHide();

				resetLyrics();

				updateControls();
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

		loading.smoothToShow();

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
		color = palette.getDarkVibrantColor(color);
		if (color == colorBackup)
			color = palette.getVibrantColor(color);
		if (color == colorBackup)
			color = palette.getDarkMutedColor(color);

		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = Math.min(hsl[2], hsl[2] - 0.15f);
		color = ColorUtils.HSLToColor(hsl);
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.35f);
		hsl[1] = Math.max(hsl[1], hsl[1] + 0.25f);
		colorLight = ColorUtils.HSLToColor(hsl);

		updateControls(color, colorLight);

		if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
			audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
		}

		if (!(root.getBackground() == null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 160)));
			if (bg != null)
				try {
					Blurry.with(this)
							.radius(21)
							.sampling(3)
							.color(ColorUtils.setAlphaComponent(color, 80))
							.animate(763)
							.async()
							.from(bitmap)
							.into(bg);
				} catch (Exception e) {
					e.printStackTrace();
				}
		} else {
			if (bg != null)
				bg.setImageDrawable(null);
		}

		wave.setStartColor(color);

		if (cover.getDrawable() != null) {
			TransitionDrawable d = new TransitionDrawable(new Drawable[]{
					cover.getDrawable(),
					new BitmapDrawable(getResources(), bitmap)
			});

			cover.setImageDrawable(d);

			d.setCrossFadeEnabled(true);
			d.startTransition(763);
		} else {
			cover.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
		}

		loading.smoothToHide();
	}

	//endregion

	//region Root

	private View root;

	private ImageView bg;

	private MultiWaveHeader wave;

	private View.OnTouchListener touchListener;

	private ViewPager viewPager;

	private ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
		@Override
		public void onPageSelected(int position) {
			super.onPageSelected(position);

			updateAVFX();
		}
	};

	private void createRoot() {
		root = findViewById(R.id.root);

		bg = findViewById(R.id.bg);

		wave = findViewById(R.id.wave);

		touchListener = new AndroidTouchEx.OnSwipeTouchListener() {
			@Override
			public boolean onSwipeLeft() {
				if (getMusicService() != null) {
					getMusicService().prev();

					root.startAnimation(AnimationUtils.loadAnimation(PlaybackUIActivity.this, R.anim.shake));

					AndroidEx.vibrate(PlaybackUIActivity.this);

					return true;
				}

				return false;
			}

			@Override
			public boolean onSwipeRight() {
				if (getMusicService() != null) {
					getMusicService().next();

					root.startAnimation(AnimationUtils.loadAnimation(PlaybackUIActivity.this, R.anim.shake));

					AndroidEx.vibrate(PlaybackUIActivity.this);

					return true;
				}

				return false;
			}

			@Override
			public boolean onSwipeTop() {
				if (getMusicService() != null) {
					getMusicService().random();

					root.startAnimation(AnimationUtils.loadAnimation(PlaybackUIActivity.this, R.anim.shake));

					AndroidEx.vibrate(PlaybackUIActivity.this);

					return true;
				}

				return false;
			}

			@Override
			public boolean onSwipeBottom() {
				onBackPressed();

				return false;
			}
		};

		root.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				showOptionsDialog();

				return true;
			}
		});
		root.setLongClickable(true);

		viewPager = findViewById(R.id.viewPager);

		viewPager.post(new Runnable() {
			@Override
			public void run() {
				CircleIndicator viewPagerIndicator = findViewById(R.id.viewPagerIndicator);
				viewPagerIndicator.setViewPager(viewPager);

				try {
					viewPager.setCurrentItem(1);
				} catch (Exception e) {
					// Eat ?
				}
			}
		});
	}

	//endregion

	//region Metadata

	private TextView title;
	private TextView artist;
	private TextView info;
	private ImageView cover;
	private VideoView video;

	private Runnable videoSyncTask = new Runnable() {
		@Override
		public void run() {
			if (video != null && video.getVisibility() == View.VISIBLE)
				video.seekTo(getMusicService().getPosition());

			handler.removeCallbacks(videoSyncTask);
			handler.postDelayed(videoSyncTask, 9 * 1000);
		}
	};

	private void createMetadata() {
		View metadata_layout = findViewById(R.id.metadata_layout);
		if (metadata_layout != null) {
			metadata_layout.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					showOptionsDialog();

					return true;
				}
			});
			metadata_layout.setLongClickable(true);
			metadata_layout.setOnTouchListener(touchListener);
		}

		title = findViewById(R.id.title);
		artist = findViewById(R.id.artist);
		info = findViewById(R.id.info);

		cover = findViewById(R.id.cover);
		video = findViewById(R.id.video);

		toggleVideo(getPlaybackUIVideoHidden(this));

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
							if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
								toggleVideo(false);
								toggleControls(true);
							}
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
							toggleVideo(true);
							toggleControls(false);
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
							toggleVideo(true);
							toggleControls(false);
						}
					});

					return false;
				}
			});
		}

		cover.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				showOptionsDialog();

				return true;
			}
		});
		cover.setLongClickable(true);
		cover.setOnTouchListener(touchListener);
	}

	private void updateMetadata(Music data) {
		if (title != null)
			title.setText(data.getTitle());
		if (artist != null)
			artist.setText(data.getArtist());
		if (info != null) {
			String s;
			try {
				s = data.getTextDetailedMultiLine(getMusicService().getPlaylist().getItemIndex());
			} catch (Exception e) {
				e.printStackTrace();
				s = data.getTextDetailedMultiLine();
			}
			info.setText(s);
		}
	}

	private void toggleVideo(boolean hide) {
		if (video != null) {
			if (hide) {
				video.setVisibility(View.INVISIBLE);
				setPlaybackUIVideoHidden(PlaybackUIActivity.this, true);
			} else {
				video.setVisibility(View.VISIBLE);
				setPlaybackUIVideoHidden(PlaybackUIActivity.this, false);
			}
		}
	}

	private void toggleVideo() {
		toggleVideo(!getPlaybackUIVideoHidden(this));
	}

	//endregion

	//region Options

	private void showOptionsDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle));
		builder.setTitle(getString(R.string.select_title));
		builder.setItems(new CharSequence[]{
				getString(R.string.share_lyrics),
				getString(R.string.toggle_video),
				getString(R.string.re_download_cover),
				getString(R.string.re_load_lyrics),
				getString(R.string.edit_lyrics),
				getString(R.string.update_metadata),
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int itemIndex) {
				try {
					switch (itemIndex) {
						case 0:
							shareLyrics();
							break;
						case 1:
							toggleVideo();
							break;
						case 2:
							updateCover();
							break;
						case 3:
							reloadLyrics();
							break;
						case 4:
							editLyrics();
							break;
						case 5:
							lookupAndUpdateDetails();
							break;
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void shareLyrics() {
		String shareCoverPath = MediaStore.Images.Media.insertImage(getContentResolver(), getMusicService().getMusic().getCover(PlaybackUIActivity.this, -1), getMusicService().getMusic().getText(), null);
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, lyrics.getText().toString());
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(shareCoverPath));
		shareIntent.setType("image/*");
		shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(shareIntent, getString(R.string.share_lyrics_for_) + getMusicService().getMusic().getText()));
	}

	private void updateCover() throws Exception {
		if (!AndroidEx.hasInternetConnection(this)) {
			info(getString(R.string.network_unavailable), true);

			return;
		}

		info(getString(R.string.downloading_artwork), true);

		final Music data = getMusicService().getMusic();

		final Context context = this;

		final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
			@Override
			public void accept(Bitmap bitmap) throws Exception {
				try {
					info(getString(R.string.artwork_downloaded));

					onCoverReloaded(bitmap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		final Consumer<Throwable> throwableConsumer = new Consumer<Throwable>() {
			@Override
			public void accept(Throwable throwable) throws Exception {
				info(getString(R.string.artwork_download_failed));
			}
		};
		final Consumer<Throwable> throwableConsumerWithRetry = new Consumer<Throwable>() {
			@Override
			public void accept(Throwable throwable) throws Exception {
				Music
						.loadLocalOrSearchCoverArtFromItunes(
								context,
								data,
								data.getCoverPath(context),
								data.getText(),
								false,
								ImageEx.ItunesImageType.Artist)
						.observeOn(AndroidSchedulers.mainThread())
						.subscribeOn(Schedulers.computation())
						.subscribe(
								resultConsumer,
								throwableConsumer);
			}
		};

		Music
				.loadLocalOrSearchCoverArtFromItunes(
						context,
						data,
						data.getCoverPath(context),
						data.getText(),
						false,
						ImageEx.ItunesImageType.Song)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeOn(Schedulers.computation())
				.subscribe(
						resultConsumer,
						throwableConsumerWithRetry);

	}

	private void editLyrics() {
		try {
			Context context = PlaybackUIActivity.this;

			File file = getMusicService().getMusic().getLyricsFile(context);
			Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);

			Intent intent = new Intent(Intent.ACTION_EDIT);
			intent.setDataAndType(contentUri, "text/plain");
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

			List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			for (ResolveInfo resolveInfo : resInfoList) {
				String packageName = resolveInfo.activityInfo.packageName;
				context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}

			context.startActivity(Intent.createChooser(intent, getString(R.string.edit_lyrics_for_) + getMusicService().getMusic().getText()));
		} catch (Exception e) {
			e.printStackTrace();

			Toast.makeText(PlaybackUIActivity.this, R.string.install_text_editor, Toast.LENGTH_LONG).show();
		}
	}

	private void lookupAndUpdateDetails() {
		info(getString(R.string.looking_up), true);

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
						info(getString(R.string.found_something));

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
								.setTitle(R.string.apply_new_details_title)
								.setMessage(sb.toString())
								.setCancelable(true)
								.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										try (Realm realm = Music.getDB()) {
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

										info(getString(R.string.details_applied_restart_needed));
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

						info(getString(R.string.details_not_found_internet));
					}
				});
	}

	//endregion

	//region Lyrics

	private ScrollView lyrics_layout;
	private TextView lyrics;

	private void createLyrics() {
		lyrics_layout = findViewById(R.id.lyrics_layout);

		if (lyrics_layout == null)
			return;

		lyrics = findViewById(R.id.lyrics);

		Music.setCurrentLyricsView(this);

		lyrics_layout.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				showOptionsDialog();

				return true;
			}
		});
		lyrics_layout.setLongClickable(true);

		lyrics.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				showOptionsDialog();

				return true;
			}
		});
		lyrics.setLongClickable(true);
	}

	private String lyricsContentFormatted = null;
	private TreeMap<Long, String> LyricsContentProcessed = new TreeMap<>();

	private static Pattern lyrics_ts = Pattern.compile("(\\d+):(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE);
	private static Pattern lyrics_lf = Pattern.compile("((\\[(.*)\\])+)(.*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private boolean lyricsIsContentProcessed = false;
	private float lyricsScrollBy = 1;
	private int lyricsLastPScroll;
	private int lyricsLastP;
	private long lyricsLastTS;
	private int lyricsLastIndex;

	public void resetLyrics() {
		if (lyrics_layout == null)
			return;

		lyricsLastPScroll = 0;
		lyricsLastP = 0;
		lyricsLastTS = 0;
		lyricsLastIndex = -1;

		lyrics_layout.smoothScrollTo(0, 0);

		// If no data loaded return
		if (getMusicService() == null || getMusicService().getMusic() == null)
			return;

		loading.show();

		lyrics.setText(getMusicService().getMusic().getTextDetailedMultiLine());

		// Load lyrics
		String content = getMusicService().getMusic().getLyrics(this);
		// Check if need to download or not
		if (content == null) {
			// If download required, postpone function to later
			reloadLyrics();
			return;
		}

		int lines = content.split(System.getProperty("line.separator")).length + 3;

		// Format content
		String nl = System.getProperty("line.separator");

		LyricsContentProcessed.clear();

		Matcher m = lyrics_lf.matcher(content);
		while (m.find()) {
			String c = m.group(4).trim();

			Matcher mts = lyrics_ts.matcher(m.group(3));
			while (mts.find()) { // Decode multiple time lines

				long ts1 = Long.parseLong(mts.group(1)) * 60000
						+ Long.parseLong(mts.group(2)) * 1000
						+ Long.parseLong(mts.group(3));

				LyricsContentProcessed.put(ts1, c);
			}
		}

		if (LyricsContentProcessed.size() > 0) { // Re-build user friendly lyrics
			StringBuilder sb = new StringBuilder();

			for (TreeMap.Entry entry : LyricsContentProcessed.entrySet()) {
				sb.append(entry.getValue());
				sb.append(System.lineSeparator());
			}

			lyricsContentFormatted = getMusicService().getMusic().getTextDetailedMultiLine() + nl + nl + sb.toString();

			lines = sb.toString().split(System.getProperty("line.separator")).length + 3;
		} else {
			lyricsContentFormatted = getMusicService().getMusic().getTextDetailedMultiLine() + nl + nl + content;
		}

		lyricsScrollBy = ((float) lyrics.getLineHeight() * lines) / ((float) getMusicService().getMusic().getLength() / 1000);
		if (lyricsScrollBy < 1 || lyricsScrollBy > 7)
			lyricsScrollBy = (float) lyrics.getLineHeight() / 2.1f;

		lyrics.setText(lyricsContentFormatted);

		loading.hide();

		lyricsIsContentProcessed = true;
	}

	public void updateLyricsScroll() {
		if (lyrics_layout == null)
			return;

		if (!lyricsIsContentProcessed)
			return;

		if (lyrics == null || lyrics.getLayout() == null)
			return;

		int p = getMusicService().getPosition();

		// Reset if seek-ed back
		if (lyricsLastP > p) {
			lyricsLastTS = 0;
			lyricsLastIndex = -1;
		}
		lyricsLastP = p;

		// For synced
		if (LyricsContentProcessed.size() > 0) {

			long ts = 0L;
			String c = null;

			for (Long k : LyricsContentProcessed.keySet())
				if (k <= p) {
					ts = k;
					c = LyricsContentProcessed.get(k);
				} else break;

			if (ts > lyricsLastTS && c != null) {
				int index = lyricsContentFormatted.indexOf(c, lyricsLastIndex);
				lyricsLastIndex = index;

				int line = lyrics.getLayout().getLineForOffset(index);
				int y = (int) ((line + 0.5) * lyrics.getLineHeight()) + lyrics.getTop();

				lyrics_layout.smoothScrollTo(0, y - lyrics_layout.getHeight() / 2);

				lyricsLastTS = ts;
			}

		}

		// For un-synced (no else to show little scroll always)
		if (p - lyricsLastPScroll > 1000 || p < 0) {
			lyrics_layout.smoothScrollBy(0, Math.round(lyricsScrollBy));
			lyricsLastPScroll = p;
		}

	}

	public void reloadLyrics() {
		loading.smoothToShow();
		try {
			Music.getLyricsOrDownload(getMusicService().getMusic());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onLyricsReloaded(String lyrics) {
		loading.smoothToHide();

		resetLyrics();
	}

	//endregion

	//region AVFX

	private View avfx_layout;

	private AudioVFXViewFragment audioVFXViewFragment;

	private void createAVFX() {
		avfx_layout = findViewById(R.id.avfx_layout);

		avfx.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeAVFXStyle();
			}
		});
	}

	private void updateAVFX() {
		if (viewPager.getCurrentItem() == 0) {
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

	private void changeAVFXStyle() {
		final AudioVFXViewFragment.AVFXType[] values = AudioVFXViewFragment.AVFXType.values();
		CharSequence items[] = new CharSequence[values.length];
		for (int i = 0; i < values.length; i++) {
			items[i] = values[i].friendlyName;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle));
		builder.setTitle(R.string.select_title);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int itemIndex) {
				try {
					AudioVFXViewFragment.setAVFXType(getApplicationContext(), values[itemIndex]);

					audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);

					info(getString(R.string.now_using_) + AudioVFXViewFragment.getAVFXType(getApplicationContext()) + getString(R.string._fx));
				} catch (Exception e) {
					Log.w(TAG, e);

					info(getString(R.string.error));
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	//endregion

	//region Controls

	private View controls_layout;
	private SeekBar seekBar;
	private TextView position_start, position_end;
	private ImageButton play_pause_stop;
	private ImageButton prev;
	private ImageButton next;
	private ImageButton shuffle;
	private ImageButton repeat;
	private ImageButton avfx;
	private ImageButton tune;
	private ImageButton more;

	private void createControls() {
		controls_layout = findViewById(R.id.controls_layout);

		seekBar = findViewById(R.id.seekBar);
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

		position_start = findViewById(R.id.position_start);
		position_end = findViewById(R.id.position_end);

		play_pause_stop = findViewById(R.id.play_pause_stop);
		prev = findViewById(R.id.prev);
		next = findViewById(R.id.next);
		shuffle = findViewById(R.id.shuffle);
		repeat = findViewById(R.id.repeat);
		avfx = findViewById(R.id.avfx);
		tune = findViewById(R.id.tune);
		more = findViewById(R.id.more);

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
				}

				return true;
			}
		});
		play_pause_stop.setLongClickable(true);

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
							info(getString(R.string.shuffle_on));
						else
							info(getString(R.string.shuffle_off));

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
						info(getString(R.string.repeat_on));
					else
						info(getString(R.string.repeat_off));

					if (value)
						repeat.setAlpha(0.9f);
					else
						repeat.setAlpha(0.3f);
				}
			}
		});

		tune.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(PlaybackUIActivity.this, TuneActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		});

		if (more != null)
			more.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showOptionsDialog();
				}
			});

	}

	private Runnable progressHandlerRunnable;

	private void updateControls() {
		if (!getMusicService().getMusic().isLocal() && getMusicService().getMusic().getLength() > 0) {
			position_start.setVisibility(View.GONE);
			position_end.setVisibility(View.GONE);
			seekBar.setVisibility(View.GONE);
		} else {
			position_start.setVisibility(View.VISIBLE);
			position_end.setVisibility(View.VISIBLE);
			seekBar.setVisibility(View.VISIBLE);

			seekBar.setMax(getMusicService().getDuration());
			try {
				position_end.setText(DurationFormatUtils.formatDuration(getMusicService().getDuration(), "mm:ss", false));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		progressHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				if (getMusicService() != null && getMusicService().isPlaying()) {
					if (seekBar.getVisibility() == View.VISIBLE)
						seekBar.setProgress(getMusicService().getPosition());
					if (position_start.getVisibility() == View.VISIBLE) try {
						position_start.setText(DurationFormatUtils.formatDuration(getMusicService().getPosition(), "mm:ss", false));
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (lyrics_layout != null)
						updateLyricsScroll();
				}

				if (isFinishing())
					return;
				if (isDestroyed())
					return;

				handler.removeCallbacks(progressHandlerRunnable);
				handler.postDelayed(progressHandlerRunnable, 1000);
			}
		};
		handler.postDelayed(progressHandlerRunnable, 1000);
	}

	private void updateControls(int color, int colorLight) {
		seekBar.getProgressDrawable().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		seekBar.getThumb().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		position_start.setTextColor(colorLight);
		position_end.setTextColor(colorLight);

		play_pause_stop.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		if (play_pause_stop.getBackground() != null)
			play_pause_stop.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		prev.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		if (prev.getBackground() != null)
			prev.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		next.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		if (next.getBackground() != null)
			next.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		shuffle.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		repeat.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		avfx.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		tune.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
		more.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
	}

	private void toggleControls() {
		if (controls_layout != null) {
			if (controls_layout.getAlpha() > 0.1) {
				controls_layout.animate().alpha(0.01f).setDuration(379).start();
			} else {
				controls_layout.animate().alpha(1).setDuration(333).start();
			}
		}
	}

	private void toggleControls(boolean hide) {
		if (controls_layout != null) {
			if (hide) {
				controls_layout.animate().alpha(0.01f).setDuration(379).start();
			} else {
				controls_layout.animate().alpha(1).setDuration(333).start();
			}
		}
	}

	//endregion

	//region SPref

	public static final String TAG_SPREF_PLAYBACK_UI_VIDEO_HIDDEN = "playback_ui_video_hidden";

	public static boolean getPlaybackUIVideoHidden(Context context) {
		try {
			return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYBACK_UI_VIDEO_HIDDEN, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void setPlaybackUIVideoHidden(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYBACK_UI_VIDEO_HIDDEN, value)
				.apply();
	}

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_PLAYBACK_UI_VIDEO_HIDDEN
	};

	//endregion

}
