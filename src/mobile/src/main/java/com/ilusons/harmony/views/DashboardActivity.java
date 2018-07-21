package com.ilusons.harmony.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.graphics.Palette;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.DrawerArrow;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.ref.ViewEx;
import com.ilusons.harmony.ref.ui.ParallaxImageView;
import com.scwang.wave.MultiWaveHeader;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Subscription;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import at.grabner.circleprogress.CircleProgressView;
import de.umass.lastfm.Track;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import jonathanfinerty.once.Once;
import me.everything.android.ui.overscroll.HorizontalOverScrollBounceEffectDecorator;
import me.everything.android.ui.overscroll.IOverScrollDecor;
import me.everything.android.ui.overscroll.IOverScrollState;
import me.everything.android.ui.overscroll.ListenerStubs;
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter;

public class DashboardActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = DashboardActivity.class.getSimpleName();

	// Request codes
	private static final int REQUEST_FILE_PICK = 4684;

	// OS
	protected Handler handler = new Handler();

	// UI
	private DrawerLayout drawer_layout;
	private ActionBarDrawerToggle drawer_toggle;
	private AppBarLayout appbar;
	private View metadata_layout;
	private ImageView bg;
	private View root;
	private AVLoadingIndicatorView loading;
	private MultiWaveHeader wave;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Set view
		setContentView(R.layout.dashboard_activity);

		// Hacks
		applyHacksToUI();

		// Set bar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setHomeAsUpIndicator(new com.ilusons.harmony.base.DrawerArrow(this, getSupportActionBar().getThemedContext()));

		drawer_layout = findViewById(R.id.drawer_layout);
		drawer_layout.closeDrawer(GravityCompat.START);

		drawer_toggle = new ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name) {
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				supportInvalidateOptionsMenu();
			}
		};

		drawer_toggle.setDrawerIndicatorEnabled(true);
		drawer_layout.setDrawerListener(drawer_toggle);
		drawer_toggle.syncState();

		appbar = findViewById(R.id.appbar);

		// Set views
		root = findViewById(R.id.root);

		loading = findViewById(R.id.loading);

		wave = findViewById(R.id.wave);

		metadata_layout = findViewById(R.id.metadata_layout);

		bg = findViewById(R.id.bg);

		loading.smoothToShow();

		// Drawers
		createDrawers();

		// Playback
		createPlayback();

		// Recommended
		createRecommended();

		// Recent
		createRecent();

		// Ratings
		root.postDelayed(new Runnable() {
			@Override
			public void run() {
				MainActivity.initRateMe(new WeakReference<FragmentActivity>(DashboardActivity.this), false);
			}
		}, 1500);

		// Help
		root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				showInfo();

				updateRecommended();

				updateRecent();
			}
		});

		// Start scan
		if (!Once.beenDone(Once.THIS_APP_VERSION, MusicServiceLibraryUpdaterAsyncTask.TAG)) {
			Intent musicServiceIntent = new Intent(this, MusicService.class);
			musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
			musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
			startService(musicServiceIntent);
			Once.markDone(MusicServiceLibraryUpdaterAsyncTask.TAG);
		}

		loading.smoothToHide();

	}

	@Override
	protected void onResume() {
		super.onResume();

		root.requestFocus();

		resetPlayback();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		drawer_toggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		drawer_toggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
			drawer_layout.closeDrawer(GravityCompat.START);
		} else {
			drawer_layout.openDrawer(GravityCompat.START);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();

		getMenuInflater().inflate(R.menu.dashboard_menu, menu);

		ViewEx.tintMenuIcons(menu, ContextCompat.getColor(this, R.color.textColorPrimary));

		MenuItem now_playing = menu.findItem(R.id.now_playing);
		now_playing.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				Intent intent = new Intent(DashboardActivity.this, PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				return true;
			}
		});

		MenuItem online = menu.findItem(R.id.online);
		online.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				Intent intent = new Intent(DashboardActivity.this, OnlineViewActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				return true;
			}
		});

		MenuItem playlist = menu.findItem(R.id.playlist);
		playlist.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				Intent intent = new Intent(DashboardActivity.this, PlaylistViewActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult\nrequestCode=" + requestCode + "\nresultCode=" + resultCode + "\ndata=" + data);

		if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK) {
			try {
				Uri uri = Uri.parse(StorageEx.getPath(this, data.getData()));

				Intent i = new Intent(this, MusicService.class);

				i.setAction(MusicService.ACTION_OPEN);
				i.putExtra(MusicService.KEY_URI, uri.toString());

				startService(i);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
		super.OnMusicServiceChanged(className, musicService, isBound);

		resetPlayback();

		if (musicService.getLibraryUpdater() != null && !musicService.getLibraryUpdater().isCancelled()) {
			info("Library update is in progress!", true);

			loading.smoothToShow();
		}
	}

	@Override
	public void OnMusicServicePlay() {
		super.OnMusicServicePlay();

		if (play_pause_stop != null)
			play_pause_stop.setImageResource(R.drawable.ic_music_pause);

		wave.start();
	}

	@Override
	public void OnMusicServicePause() {
		super.OnMusicServicePause();

		if (play_pause_stop != null)
			play_pause_stop.setImageResource(R.drawable.ic_music_play);

		wave.stop();
	}

	@Override
	public void OnMusicServiceStop() {
		super.OnMusicServiceStop();

		if (play_pause_stop != null)
			play_pause_stop.setImageResource(R.drawable.ic_music_stop);

		wave.stop();
	}

	@Override
	public void OnMusicServiceOpen(String uri) {
		resetPlayback();
	}

	@Override
	public void OnMusicServicePrepared() {
		resetPlayback();

		updateRecommended();

		updateRecent();
	}

	@Override
	public void OnMusicServiceLibraryUpdateBegins() {
		info("Library update is in progress!", true);

		loading.smoothToShow();
	}

	@Override
	public void OnMusicServiceLibraryUpdated() {
		loading.smoothToHide();

		try {
			// Update mini now playing ui
			resetPlayback();
		} catch (Exception e) {
			e.printStackTrace();
		}

		info("Library updated!");

		resetPlayback();

		behaviourForAddScanLocationOnEmptyLibrary();

		updateRecommended();

		updateRecent();
	}

	@Override
	public void OnMusicServicePlaylistChanged(String name) {
		try {
			// Update mini now playing ui
			resetPlayback();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//region Other

	private void applyHacksToUI() {
		/*
		View nav_bar_filler = findViewById(R.id.nav_bar_filler);
		if (nav_bar_filler != null) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) nav_bar_filler.getLayoutParams();
			params.bottomMargin = AndroidEx.getNavigationBarSize(this).y;
		}
		*/
	}

	//endregion

	//region Drawers

	private void createDrawers() {

		drawer_layout.closeDrawer(GravityCompat.START);

		findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent();
				String[] mimes = new String[]{"audio/*", "video/*"};
				i.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
				i.setType(StringUtils.join(mimes, '|'));
				i.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(i, REQUEST_FILE_PICK);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.now_playing).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(DashboardActivity.this, PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null) {
					getMusicService().stop();
					getMusicService().stopSelf();
				}

				finish();
			}
		});

		findViewById(R.id.ytpl).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Uri uri = Uri.parse("https://www.youtube.com/playlist?list=PLR6v5-VD7fUJtQepsBTq7Wf44e_z1eM8b");
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(uri);
					i.setClassName("com.google.android.youtube", "com.google.android.youtube.app.froyo.phone.PlaylistViewActivity");
					startActivity(i);
				} catch (Exception e) {
					Log.w(TAG, e);
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=PLR6v5-VD7fUJtQepsBTq7Wf44e_z1eM8b")));
				}
			}
		});

		findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MainActivity.gotoFeedback(DashboardActivity.this);
			}
		});

		findViewById(R.id.intro).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getApplicationContext(), IntroActivity.class);

				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				getApplicationContext().startActivity(intent);

				Toast.makeText(getApplicationContext(), "Dashboard will now close!", Toast.LENGTH_SHORT).show();

				finish();
			}
		});

		findViewById(R.id.timer).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TimerViewFragment.showAsDialog(DashboardActivity.this);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.tune).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TunePresetsFragment.showAsDialog(view.getContext());

				info("Long-press previous button for deep customization.");

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});
		findViewById(R.id.tune).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				Intent intent = new Intent(DashboardActivity.this, TuneActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				drawer_layout.closeDrawer(GravityCompat.START);

				return true;
			}
		});
		findViewById(R.id.tune).setLongClickable(true);

		findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Intent musicServiceIntent = new Intent(getApplicationContext(), MusicService.class);
					musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
					musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
					getApplicationContext().startService(musicServiceIntent);
				} catch (Exception e) {
					e.printStackTrace();
				}

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AboutViewFragment.showAsDialog(DashboardActivity.this);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		drawer_layout.closeDrawer(GravityCompat.START);

	}

	//endregion

	//region Playback

	private CircleProgressView progress;
	private ImageView cover;
	private TextView title;
	private TextView info;

	private ImageView play_pause_stop;

	private void createPlayback() {
		cover = findViewById(R.id.cover);
		title = findViewById(R.id.title);
		info = findViewById(R.id.info);

		cover.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MainActivity.openPlaybackUIActivity(DashboardActivity.this);
			}
		});

		play_pause_stop = findViewById(R.id.play_pause_stop);

		play_pause_stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() == null) return;

				if (getMusicService().isPlaying()) {
					getMusicService().pause();
				} else {
					getMusicService().play();
				}
			}
		});
		play_pause_stop.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (getMusicService() == null) return false;

				if (getMusicService().isPlaying()) {
					getMusicService().stop();
				} else {
					getMusicService().play();
				}

				return true;
			}
		});

		progress = findViewById(R.id.progress);

		final View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(DashboardActivity.this, PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(
						intent,
						ActivityOptionsCompat.makeCustomAnimation(view.getContext(), R.anim.scale_up, R.anim.shake).toBundle());
			}
		};


		title.setOnClickListener(onClickListener);
		info.setOnClickListener(onClickListener);

		// Animations

		final Animation animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
		play_pause_stop.startAnimation(animationSlideDown);
		progress.startAnimation(animationSlideDown);

		final Animation animationSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
		title.startAnimation(animationSlideUp);
		info.startAnimation(animationSlideUp);
	}

	private Runnable progressHandlerRunnable;

	private void setupProgressHandler() {
		if (progressHandlerRunnable != null)
			handler.removeCallbacks(progressHandlerRunnable);

		final int dt = (int) (1000.0 / 16.0);

		progressHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				if (getMusicService() != null && getMusicService().isPlaying()) {
					progress.setValue(getMusicService().getPosition());
				}

				handler.removeCallbacks(progressHandlerRunnable);
				handler.postDelayed(progressHandlerRunnable, dt);
			}
		};
		handler.postDelayed(progressHandlerRunnable, dt);
	}

	public void resetPlayback() {
		if (getMusicService() == null || getMusicService().getMusic() == null)
			return;

		Music m = getMusicService().getMusic();

		title.setText(m.getTitle());
		String s;
		try {
			s = m.getTextExtraOnlySingleLine(getMusicService().getPlaylist().getItemIndex());
		} catch (Exception e) {
			e.printStackTrace();

			s = m.getTextExtraOnlySingleLine();
		}
		info.setText(s);

		progress.setMaxValue(getMusicService().getDuration());

		setupProgressHandler();

		if (getMusicService().isPlaying())
			play_pause_stop.setImageResource(R.drawable.ic_music_pause);

		try {
			final Context context = this;
			final Music music = m;
			final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
				@Override
				public void accept(final Bitmap bitmap) throws Exception {
					try {
						Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
							@SuppressWarnings("ResourceType")
							@Override
							public void onGenerated(@NonNull Palette palette) {
								int vibrantColor = palette.getVibrantColor(R.color.accent);
								int vibrantDarkColor = palette.getDarkVibrantColor(R.color.accent_inverse);

								Drawable drawable = new GradientDrawable(
										GradientDrawable.Orientation.TL_BR,
										new int[]{
												vibrantDarkColor,
												vibrantColor
										});
								drawable = drawable.mutate();
								bg.setImageDrawable(drawable);

								progress.setFillCircleColor(ColorUtils.setAlphaComponent(vibrantDarkColor, 80));

								wave.setStartColor(vibrantColor);

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
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			final Consumer<Throwable> throwableConsumer = new Consumer<Throwable>() {
				@Override
				public void accept(Throwable throwable) throws Exception {
					cover.setImageDrawable(null);
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

			cover.setImageDrawable(null);
		}
	}

	//endregion

	//region Recommended

	private RecyclerViewAdapter adapter_recommended;

	private AVLoadingIndicatorView loading_view_recommended;

	private void createRecommended() {
		adapter_recommended = new RecyclerViewAdapter(this);

		loading_view_recommended = findViewById(R.id.loading_view_recommended);

		RecyclerView recyclerView = findViewById(R.id.recycler_view_recommended);

		recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 3);

		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

		recyclerView.setAdapter(adapter_recommended);

		// Animations
		HorizontalOverScrollBounceEffectDecorator overScroll = new HorizontalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView), 1.5f, 1f, -0.5f);

		overScroll.setOverScrollUpdateListener(new ListenerStubs.OverScrollUpdateListenerStub() {
			@Override
			public void onOverScrollUpdate(IOverScrollDecor decor, int state, float offset) {
				super.onOverScrollUpdate(decor, state, offset);

				if (state == IOverScrollState.STATE_DRAG_START_SIDE && offset > 300)
					updateRecommended();
			}
		});

		updateRecommended();
	}

	private static long lastCallUpdateRecommendedTimestamp = 0;

	public static boolean canCallUpdateRecommended() {
		boolean r = true;

		long now = System.currentTimeMillis();

		if ((now - lastCallUpdateRecommendedTimestamp) > 10 * 1000) {
			lastCallUpdateRecommendedTimestamp = now;
		} else {
			r = false;
		}

		return r;
	}

	private Disposable disposable_recommended = null;

	private void updateRecommended() {
		if (!canCallUpdateRecommended())
			return;

		try {
			final Context context = this;

			if (AndroidEx.hasInternetConnection(context)) {
				loading_view_recommended.smoothToShow();

				Observer<Collection<Music>> observer = new Observer<Collection<Music>>() {
					@Override
					public void onSubscribe(Disposable d) {
						try {
							if (disposable_recommended != null && !disposable_recommended.isDisposed()) {
								disposable_recommended.dispose();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						disposable_recommended = d;
					}

					@Override
					public void onNext(Collection<Music> r) {
						if (r == null || r.isEmpty())
							return;

						try {
							adapter_recommended.clear();
							for (Music m : r) {
								adapter_recommended.add(m);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable e) {
						loading_view_recommended.smoothToHide();

						updateRecommendedOffline();

						disposable_recommended = null;
					}

					@Override
					public void onComplete() {
						loading_view_recommended.smoothToHide();

						updateRecommendedOffline();

						disposable_recommended = null;
					}
				};

				ArrayList<io.reactivex.Observable<Collection<Track>>> observables = new ArrayList<>();

				try {
					for (Music music : Music.getAllSortedByTimeLastPlayed(1)) {
						observables.add(Analytics.findSimilarTracks(music.getArtist(), music.getTitle(), 6));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (observables.size() == 0)
					try {
						observables.add(Analytics.getInstance().getTopTracksForLastfmForApp());
					} catch (Exception e) {
						e.printStackTrace();
					}

				io.reactivex.Observable
						.concat(observables)
						.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
							@Override
							public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
								return Analytics.convertToLocal(context, tracks, 12, true);
							}
						})
						.observeOn(AndroidSchedulers.mainThread())
						.subscribeOn(Schedulers.io())
						.subscribe(observer);
			} else {
				updateRecommendedOffline();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateRecommendedOffline() {
		loading_view_recommended.smoothToShow();

		try {
			Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_ONLINE);
			if (adapter_recommended.getItemCount() < 3 && playlist.getItems().size() > 0)
				adapter_recommended.clear(Music.class);
			if (playlist != null) {
				for (Music item : playlist.getItems()) {
					adapter_recommended.add(item);
				}

				if (playlist.getItems().size() == 0) {
					for (Music item : Music.getAllSortedByTimeAdded(16)) {
						adapter_recommended.add(item);
					}
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		adapter_recommended.notifyDataSetChanged();

		loading_view_recommended.smoothToHide();
	}

	//endregion

	//region Recent

	private RecyclerViewAdapter adapter_recent;

	private AVLoadingIndicatorView loading_view_recent;

	private void createRecent() {
		adapter_recent = new RecyclerViewAdapter(this);

		loading_view_recent = findViewById(R.id.loading_view_recent);

		RecyclerView recyclerView = findViewById(R.id.recycler_view_recent);

		recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 3);

		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

		recyclerView.setAdapter(adapter_recent);

		// Animations
		HorizontalOverScrollBounceEffectDecorator overScroll = new HorizontalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView), 1.5f, 1f, -0.5f);

		overScroll.setOverScrollUpdateListener(new ListenerStubs.OverScrollUpdateListenerStub() {
			@Override
			public void onOverScrollUpdate(IOverScrollDecor decor, int state, float offset) {
				super.onOverScrollUpdate(decor, state, offset);

				if (state == IOverScrollState.STATE_DRAG_START_SIDE && offset > 300)
					updateRecent();
			}
		});

		updateRecent();

	}

	private void updateRecent() {
		loading_view_recent.smoothToShow();

		try {
			adapter_recent.clear();

			ArrayList<Music> items = new ArrayList<>();

			items.addAll(Music.getAllSortedByTimeLastPlayed(6));
			items.addAll(Music.getAllSortedByTimeAdded(6));
			Collections.shuffle(items);

			for (Music item : items) {
				adapter_recent.add(item);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		adapter_recent.notifyDataSetChanged();

		loading_view_recent.smoothToHide();
	}

	//endregion

	//region Recycler view

	public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final DashboardActivity context;

		private ArrayList<Object> data;

		public RecyclerViewAdapter(DashboardActivity context) {
			this.context = context;

			data = new ArrayList<>();
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dashboard_recycler_view_item, parent, false);

			return new ViewHolder(context, v);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			final int itemType = getItemViewType(position);

			try {
				((ViewHolder) holder)
						.bind(position, (Music) data.get(position));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void add(Object item) {
			data.add(item);

			notifyDataSetChanged();
		}

		public void clear() {
			data.clear();

			notifyDataSetChanged();
		}

		public <T> void clear(Class<T> tClass) {
			final Collection<Object> copy = new ArrayList<>(data);
			for (Object item : copy)
				if (item.getClass().equals(tClass))
					data.remove(item);

			notifyDataSetChanged();
		}

		public <T> Collection<T> getAll(Class<T> tClass) {
			final Collection<T> r = new ArrayList<>();
			for (Object item : data)
				if (item.getClass().equals(tClass))
					r.add((T) item);
			return r;
		}

		protected static class ViewHolder extends RecyclerView.ViewHolder {
			private final DashboardActivity context;

			protected View view;

			public ImageView image;
			public ParallaxImageView parallaxImage;
			protected TextView text1;
			protected TextView text2;

			public ViewHolder(final DashboardActivity context, View v) {
				super(v);

				this.context = context;

				view = v;

				image = v.findViewById(R.id.image);
				text1 = v.findViewById(R.id.text1);
				text2 = v.findViewById(R.id.text2);

				if (image != null) {
					image.setMaxHeight(AndroidEx.dpToPx(196));
					if (image instanceof ParallaxImageView) {
						parallaxImage = (ParallaxImageView) image;
						parallaxImage.setListener(new ParallaxImageView.ParallaxImageListener() {
							@Override
							public int[] getValuesForTranslate() {
								if (itemView.getParent() == null) {
									return null;
								} else {
									int[] itemPosition = new int[2];
									itemView.getLocationOnScreen(itemPosition);

									int[] recyclerPosition = new int[2];
									((RecyclerView) itemView.getParent()).getLocationOnScreen(recyclerPosition);

									return new int[]{
											itemPosition[1],
											((RecyclerView) itemView.getParent()).getMeasuredHeight(),
											recyclerPosition[1]
									};
								}
							}
						});
					}
				}

			}

			@SuppressLint({"StaticFieldLeak", "CheckResult"})
			public void bind(int p, final Music d) {
				final Context context = view.getContext();

				if (image != null) {
					image.setImageBitmap(null);
					final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
						@Override
						public void accept(Bitmap bitmap) throws Exception {
							try {
								TransitionDrawable d = new TransitionDrawable(new Drawable[]{
										image.getDrawable(),
										new BitmapDrawable(view.getContext().getResources(), bitmap)
								});

								image.setImageDrawable(d);

								d.setCrossFadeEnabled(true);
								d.startTransition(200);

								if (parallaxImage != null)
									parallaxImage.translate();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					final Consumer<Throwable> throwableConsumer = new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {
							// Pass
						}
					};

					Music
							.loadLocalOrSearchCoverArtFromItunes(
									context,
									d,
									d.getCoverPath(context),
									d.getText(),
									false,
									ImageEx.ItunesImageType.Song)
							.observeOn(AndroidSchedulers.mainThread())
							.subscribeOn(Schedulers.computation())
							.subscribe(
									resultConsumer,
									throwableConsumer);
				}

				text1.setText(d.getTitle());
				text2.setText(d.getArtist());

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.shake));

						playNow(d);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(final View view) {
						android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), R.style.AppTheme_AlertDialogStyle));
						builder.setTitle("Select?");
						builder.setItems(new CharSequence[]{
								"Play now",
								"Play next",
								"Download",
								"Stream",
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int itemIndex) {
								try {
									switch (itemIndex) {
										case 0:
											playNow(d);
											break;
										case 1:
											playNext(d);
											break;
										case 2:
											download(d);
											break;
										case 3:
											playAfterStream(d);
											break;
									}
								} catch (Exception e) {
									Log.w(TAG, e);
								}
							}
						});
						android.app.AlertDialog dialog = builder.create();
						dialog.show();

						return true;
					}
				});
				view.setLongClickable(true);
			}

			private void playNow(final Music music) {
				final MusicService musicService = context.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.open(music);
				} catch (Exception e) {
					e.printStackTrace();

					context.info("Ah! Try again!");
				}
			}

			private void playNext(final Music music) {
				final MusicService musicService = context.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.getPlaylist().add(music, musicService.getPlaylist().getItemIndex() + 1);

					context.info("Added!");
				} catch (Exception e) {
					e.printStackTrace();

					context.info("Ah! Try again!");
				}
			}

			private void playAfterStream(final Music music) {
				final MusicService musicService = context.getMusicService();
				if (musicService == null)
					return;

				try {
					if (MusicService.getPlayerType(view.getContext()) == MusicService.PlayerType.AndroidOS) {
						musicService.stream(music);
					} else {
						context.info("Streaming is only supported in [" + MusicService.PlayerType.AndroidOS.getFriendlyName() + "] player. You can change it from Settings.");
					}
				} catch (Exception e) {
					e.printStackTrace();

					context.info("Ah! Try again!");
				}
			}

			private void download(final Music music) {
				final MusicService musicService = context.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.download(music, false);
				} catch (Exception e) {
					e.printStackTrace();

					context.info("Ah! Try again!");
				}
			}

		}

	}

	//endregion

	//region Other

	private void showInfo() {
		final String tag_release_notes = TAG + ".release_notes";
		if (!BuildConfig.DEBUG && !Once.beenDone(Once.THIS_APP_VERSION, tag_release_notes)) {
			SettingsActivity.showReleaseNotesDialog(this);
			Once.markDone(tag_release_notes);
		} else {
			MainActivity.initTips(new WeakReference<FragmentActivity>(this));
		}
	}

	public boolean behaviourForAddScanLocationOnEmptyLibrary() {
		boolean r = false;

		try {
			try {
				Playlist pl_all = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_ALL);
				if (pl_all != null && pl_all.getItems() != null)
					r = pl_all.getItems().size() <= 3;
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (r) {
				(new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
						.setTitle("No music? No songs?")
						.setMessage("Would you like add a scan location i.e. select your music/songs folder?")
						.setCancelable(true)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								intent.putExtra(SettingsActivity.TAG_BehaviourForAddScanLocationOnEmptyLibrary, true);
								startActivity(intent);

								dialogInterface.dismiss();
							}
						})
						.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
							}
						}))
						.show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return r;
	}

	//endregion

}
