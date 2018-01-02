package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
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
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.AndroidTouchEx;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ui.CircleIndicator;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private AVLoadingIndicatorView loading;

	private int color;
	private int colorLight;

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
		int layoutId = -1;
		switch (getPlaybackUIStyle(this)) {
			case P2:
				layoutId = R.layout.playback_ui_p2_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case P3:
				layoutId = R.layout.playback_ui_p3_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case P4:
				layoutId = R.layout.playback_ui_p4_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case P5:
				layoutId = R.layout.playback_ui_p5_activity;
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;

			case P1:
			default:
				layoutId = R.layout.playback_ui_p1_activity;
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

		loading = (AVLoadingIndicatorView) findViewById(R.id.loading);

		createRoot();

		createMetadata();

		createLyrics();

		createControls();

		createAVFX();
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

		resetForUriIfNeeded(uri);
	}

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

				try {
					Music.getCoverOrDownload(cover.getWidth(), music);
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
		color = palette.getVibrantColor(color);
		if (color == colorBackup)
			color = palette.getDarkVibrantColor(color);
		if (color == colorBackup)
			color = palette.getDarkMutedColor(color);

		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = Math.max(hsl[2], hsl[2] + 0.30f); // lum +30%
		colorLight = ColorUtils.HSLToColor(hsl);

		updateControls(color, colorLight);

		if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
			audioVFXViewFragment.reset(getMusicService(), AudioVFXViewFragment.getAVFXType(getApplicationContext()), colorLight);
		}

		if (!(root.getBackground() == null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			root.setBackground(new ColorDrawable(ColorUtils.setAlphaComponent(color, 180)));

			if (bg != null)
				try {
					Blurry.with(this)
							.radius(25)
							.sampling(1)
							.color(Color.argb(160, 0, 0, 0))
							.animate(450)
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

		switch (getPlaybackUIStyle(this)) {
			case P2:
				Blurry.with(this)
						.radius(25)
						.sampling(1)
						.color(Color.argb(160, 0, 0, 0))
						.async()
						.animate(450)
						.from(bitmap)
						.into(cover);
				break;

			case P3:
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

			case P5:
				cover.setImageBitmap(bitmap);
				break;

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

		loading.smoothToHide();
	}

	//endregion

	//region Root

	private View root;

	private ImageView bg;

	private View.OnTouchListener touchListener;

	private void createRoot() {
		root = findViewById(R.id.root);

		bg = (ImageView) findViewById(R.id.bg);

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

		root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				initHelp();
			}
		});

		final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
		if (viewPager != null) {
			viewPager.post(new Runnable() {
				@Override
				public void run() {
					CircleIndicator viewPagerIndicator = (CircleIndicator) findViewById(R.id.viewPagerIndicator);
					viewPagerIndicator.setViewPager(viewPager);
				}
			});
		}
	}

	//endregion

	//region Metadata

	private TextView title;
	private TextView artist;
	private TextView info;
	private com.makeramen.roundedimageview.RoundedImageView cover;
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

		toggleCover(getPlaybackUICoverHidden(this));
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

	private void toggleCover(boolean hide) {
		if (cover != null) {
			if (hide) {
				cover.animate().alpha(0.3f).setDuration(666).start();
				setPlaybackUICoverHidden(PlaybackUIActivity.this, true);
			} else {
				cover.animate().alpha(1).setDuration(333).start();
				setPlaybackUICoverHidden(PlaybackUIActivity.this, false);
			}
		}
	}

	private void toggleCover() {
		toggleCover(!getPlaybackUICoverHidden(this));
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
				getString(R.string.toggle_cover),
				getString(R.string.toggle_video),
				getString(R.string.toggle_lyrics),
				getString(R.string.re_download_cover),
				getString(R.string.re_load_lyrics),
				getString(R.string.edit_lyrics),
				getString(R.string.update_metadata),
				getString(R.string.change_ui),
				getString(R.string.change_fx),
				getString(R.string.tour)
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int itemIndex) {
				try {
					switch (itemIndex) {
						case 0:
							shareLyrics();
							break;
						case 1:
							toggleCover();
							break;
						case 2:
							toggleVideo();
							break;
						case 3:
							toggleLyrics();
							break;
						case 4:
							updateCover();
							break;
						case 5:
							reloadLyrics();
							break;
						case 6:
							editLyrics();
							break;
						case 7:
							lookupAndUpdateDetails();
							break;
						case 8:
							changePlaybackUIStyle();
							break;
						case 9:
							changeAVFXStyle();
							break;
						case 10:
							tour(new MaterialIntroListener() {
								@Override
								public void onUserClicked(String s) {
									info(":)");
								}
							});
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
		info(getString(R.string.downloading_artwork), true);

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
						info(getString(R.string.artwork_downloaded));

						onCoverReloaded(bitmap);
					}
				},
				new JavaEx.ActionT<Exception>() {
					@Override
					public void execute(Exception e) {
						info(getString(R.string.artwork_download_failed));

						Log.w(TAG, e);
					}
				},
				1500,
				true);
	}

	private void editLyrics() {
		if (!MusicService.IsPremium) {
			MusicService.showPremiumFeatureMessage(PlaybackUIActivity.this);

			return;
		}

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
		if (!MusicService.IsPremium) {
			MusicService.showPremiumFeatureMessage(PlaybackUIActivity.this);

			return;
		}

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

	private void changePlaybackUIStyle() {
		final PlaybackUIStyle[] values = PlaybackUIStyle.values();
		CharSequence items[] = new CharSequence[values.length];
		for (int i = 0; i < values.length; i++) {
			items[i] = values[i].friendlyName;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlaybackUIActivity.this, R.style.AppTheme_AlertDialogStyle));
		builder.setTitle(getString(R.string.select_title));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int itemIndex) {
				try {
					setPlaybackUIStyle(PlaybackUIActivity.this, values[itemIndex]);

					info(getString(R.string.applied_on_restart));
				} catch (Exception e) {
					Log.w(TAG, e);

					info(getString(R.string.error));
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
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

		toggleLyrics(getPlaybackUILyricsHidden(PlaybackUIActivity.this));
	}

	private void toggleLyrics(boolean hide) {
		if (lyrics_layout != null) {
			if (hide) {
				lyrics_layout.animate().alpha(0.01f).setDuration(379).start();
				setPlaybackUILyricsHidden(PlaybackUIActivity.this, true);
			} else {
				lyrics_layout.animate().alpha(1).setDuration(333).start();
				setPlaybackUILyricsHidden(PlaybackUIActivity.this, false);
			}
		}
	}

	private void toggleLyrics() {
		toggleLyrics(!getPlaybackUILyricsHidden(this));
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
		if (lyricsScrollBy < 1)
			lyricsScrollBy = 1;

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
		if (p - lyricsLastPScroll > 750) { // Scroll every 3/4 sec
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
				TunePresetsFragment.showAsDialog(view.getContext());
			}
		});
		tune.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				Intent intent = new Intent(PlaybackUIActivity.this, TuneActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				return true;
			}
		});
		tune.setLongClickable(true);

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
		seekBar.setMax(getMusicService().getDuration());
		position_end.setText(DurationFormatUtils.formatDuration(getMusicService().getDuration(), "mm:ss", false));

		if (progressHandlerRunnable != null)
			handler.removeCallbacks(progressHandlerRunnable);

		final int dt = (int) (1000.0 / 24.0);

		progressHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				if (getMusicService() != null && getMusicService().isPlaying()) {

					seekBar.setProgress(getMusicService().getPosition());
					position_start.setText(DurationFormatUtils.formatDuration(getMusicService().getPosition(), "mm:ss", false));

					if (lyrics_layout != null)
						updateLyricsScroll();
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

	private void updateControls(int color, int colorLight) {
		seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
		seekBar.getThumb().setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);

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

		if (more != null) {
			more.setColorFilter(colorLight, PorterDuff.Mode.SRC_IN);
			if (more.getBackground() != null)
				more.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		}
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

	//region Help

	private void initHelp() {
		final String tag_guide = TAG + ".guide";

		if (Once.beenDone(Once.THIS_APP_INSTALL, tag_guide))
			return;

		(new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
				.setTitle(R.string.playback_setup_title)
				.setMessage(R.string.playback_setup_msg)
				.setCancelable(true)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						tour(new MaterialIntroListener() {
							@Override
							public void onUserClicked(String usageId) {
								try {
									tune.performClick();

									info(getString(R.string.playback_setup_tune_choose_preset));
								} catch (Exception e) {
									e.printStackTrace();
								}

								try {
									Once.markDone(tag_guide);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});

						dialogInterface.dismiss();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						try {
							tune.performClick();

							info(getString(R.string.playback_setup_tune_choose_preset));
						} catch (Exception e) {
							e.printStackTrace();
						}

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
				.setInfoText(getString(R.string.playback_setup_msg_1))
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
				.setInfoText(getString(R.string.playback_setup_msg_2))
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
				.setInfoText(getString(R.string.playback_setup_msg_3))
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
				.setInfoText(getString(R.string.playback_setup_msg_4))
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
				.setInfoText(getString(R.string.playback_setup_msg_5))
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
				.setInfoText(getString(R.string.playback_setup_msg_6))
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
				.setInfoText(getString(R.string.playback_setup_msg_7))
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

	//region SPref

	public enum PlaybackUIStyle {
		P1("Cover + Video + Lyrics + FX"),
		P2("Lyrics + FX"),
		P3("Cover + Video + FX"),
		P4("New"),
		P5("New 2");

		private String friendlyName;

		PlaybackUIStyle(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_PLAYBACK_UI_STYLE = "playback_ui_style";

	public static PlaybackUIStyle getPlaybackUIStyle(Context context) {
		try {
			return PlaybackUIStyle.valueOf(SPrefEx.get(context).getString(TAG_SPREF_PLAYBACK_UI_STYLE, String.valueOf(PlaybackUIStyle.P5)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return PlaybackUIStyle.P5;
	}

	public static void setPlaybackUIStyle(Context context, PlaybackUIStyle value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYBACK_UI_STYLE, String.valueOf(value))
				.apply();
	}

	public static final String TAG_SPREF_PLAYBACK_UI_COVER_HIDDEN = "playback_ui_cover_hidden";

	public static boolean getPlaybackUICoverHidden(Context context) {
		try {
			return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYBACK_UI_COVER_HIDDEN, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void setPlaybackUICoverHidden(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYBACK_UI_COVER_HIDDEN, value)
				.apply();
	}

	public static final String TAG_SPREF_PLAYBACK_UI_LYRICS_HIDDEN = "playback_ui_lyrics_hidden";

	public static boolean getPlaybackUILyricsHidden(Context context) {
		try {
			return SPrefEx.get(context).getBoolean(TAG_SPREF_PLAYBACK_UI_LYRICS_HIDDEN, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void setPlaybackUILyricsHidden(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_PLAYBACK_UI_LYRICS_HIDDEN, value)
				.apply();
	}

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
			TAG_SPREF_PLAYBACK_UI_STYLE,
			TAG_SPREF_PLAYBACK_UI_COVER_HIDDEN,
			TAG_SPREF_PLAYBACK_UI_LYRICS_HIDDEN,
			TAG_SPREF_PLAYBACK_UI_VIDEO_HIDDEN
	};

	//endregion

}
