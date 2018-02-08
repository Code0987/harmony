package com.ilusons.harmony.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.graphics.Palette;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.AndroidTouchEx;
import com.ilusons.harmony.ref.StorageEx;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import at.grabner.circleprogress.CircleProgressView;
import co.mobiwise.materialintro.animation.MaterialIntroListener;
import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.view.MaterialIntroView;
import jonathanfinerty.once.Once;
import jp.wasabeef.blurry.Blurry;

// TODO: See below
// https://www.reddit.com/r/androidapps/comments/6lxp6q/do_you_know_any_android_music_player_or_playlist/

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
	private CollapsingToolbarLayout collapse_toolbar;
	private View parallax_layout;
	private ImageView parallax_image;
	private ImageView bg;
	private View root;
	private AVLoadingIndicatorView loading;

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

		appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
			@Override
			public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
				boolean expanded = !(Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0);

				onAppBarStateChanged(expanded);
			}
		});

		collapse_toolbar = findViewById(R.id.collapse_toolbar);

		// Set views
		root = findViewById(R.id.root);

		loading = findViewById(R.id.loading);

		parallax_layout = findViewById(R.id.parallax_layout);

		parallax_image = findViewById(R.id.parallax_image);

		bg = findViewById(R.id.bg);

		loading.smoothToShow();

		// Drawers
		createDrawers();

		// Tabs
		createTabs();

		// Playback
		createPlayback();

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

				initHelp();
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
			play_pause_stop.setImageResource(R.drawable.ic_pause_black);
	}

	@Override
	public void OnMusicServicePause() {
		super.OnMusicServicePause();

		if (play_pause_stop != null)
			play_pause_stop.setImageResource(R.drawable.ic_play_black);
	}

	@Override
	public void OnMusicServiceStop() {
		super.OnMusicServiceStop();

		if (play_pause_stop != null)
			play_pause_stop.setImageResource(R.drawable.ic_stop_black);
	}

	@Override
	public void OnMusicServiceOpen(String uri) {
		try {
			Bitmap bitmap = getMusicService().getMusic().getCover(this, -1);

			Blurry.with(this)
					.radius(7)
					.sampling(1)
					.color(Color.argb(100, 0, 0, 0))
					.animate(333)
					.async()
					.from(bitmap)
					.into(parallax_image);

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

					progress.setBarColor(ColorUtils.setAlphaComponent(vibrantColor, 255));
					progress.setFillCircleColor(ColorUtils.setAlphaComponent(vibrantDarkColor, 80));
				}
			});

		} catch (Exception e) {
			e.printStackTrace();

			parallax_image.setImageDrawable(null);
		}
	}

	@Override
	public void OnMusicServicePrepared() {
		resetPlayback();
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
			// Refresh view playlist
			if (playlistViewFragment.getViewPlaylist() != null) {
				playlistViewFragment.setViewPlaylist(Playlist.loadOrCreatePlaylist(playlistViewFragment.getViewPlaylist().getName()));
			} else {
				String name = Playlist.getActivePlaylist(this);
				if (!TextUtils.isEmpty(name))
					playlistViewFragment.setFromPlaylist(-1L, name);
			}

			// Update mini now playing ui
			resetPlayback();
		} catch (Exception e) {
			e.printStackTrace();
		}

		info("Library updated!");

		resetPlayback();

		behaviourForAddScanLocationOnEmptyLibrary();
	}

	@Override
	public void OnMusicServicePlaylistChanged(String name) {
		try {
			// Refresh view playlist
			if (playlistViewFragment.getViewPlaylist().getName().equals(name)) {
				playlistViewFragment.setViewPlaylist(playlistViewFragment.getViewPlaylist());
			} else {
				if (!TextUtils.isEmpty(name))
					playlistViewFragment.setFromPlaylist(-1L, name);
			}

			// Update mini now playing ui
			resetPlayback();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void OnSearchQueryReceived(String query) {
		try {
			if (onlineViewFragment != null && onlineViewFragment.isVisible()) {
				onlineViewFragment.setSearchQuery(query);
			}
			if (playlistViewFragment != null && playlistViewFragment.isVisible()) {
				playlistViewFragment.setSearchQuery(query);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//

	//region Other

	private void applyHacksToUI() {
		View nav_bar_filler = findViewById(R.id.nav_bar_filler);
		if (nav_bar_filler != null) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) nav_bar_filler.getLayoutParams();
			params.bottomMargin = AndroidEx.getNavigationBarSize(this).y;
		}
	}

	private void onAppBarStateChanged(boolean expanded) {
		if (!expanded) {
			if (title != null)
				title.setVisibility(View.INVISIBLE);
			if (info != null)
				info.setVisibility(View.INVISIBLE);
		} else {
			if (title != null)
				title.setVisibility(View.VISIBLE);
			if (info != null)
				info.setVisibility(View.VISIBLE);
		}
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
					i.setClassName("com.google.android.youtube", "com.google.android.youtube.app.froyo.phone.PlaylistActivity");
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

	public class DrawerArrowAnimation {
		public class DrawerArrowDrawableToggle extends DrawerArrowDrawable {
			private final Activity mActivity;

			public DrawerArrowDrawableToggle(Activity activity, Context themedContext) {
				super(themedContext);
				mActivity = activity;
			}

			public void setPosition(float position) {
				if (position == 1f) {
					setVerticalMirror(true);
				} else if (position == 0f) {
					setVerticalMirror(false);
				}
				setProgress(position);
			}

			public float getPosition() {
				return getProgress();
			}
		}

	}

	//endregion

	//region Tabs

	private TabLayout tab_layout;
	private ViewPager viewPager;
	private ViewPagerAdapter viewPagerAdapter;

	private OnlineViewFragment onlineViewFragment;
	private PlaylistViewFragment playlistViewFragment;

	private void createTabs() {
		tab_layout = findViewById(R.id.tab_layout);

		viewPager = findViewById(R.id.viewPager);
		viewPager.setOffscreenPageLimit(3);

		viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(viewPagerAdapter);

		onlineViewFragment = OnlineViewFragment.create();
		viewPagerAdapter.add(onlineViewFragment, "Online");

		playlistViewFragment = PlaylistViewFragment.create();
		viewPagerAdapter.add(playlistViewFragment, "Playlist");

		tab_layout.setupWithViewPager(viewPager, true);

	}

	public static class ViewPagerAdapter extends FragmentStatePagerAdapter {

		public static final String KEY_TITLE = "_title";

		private final List<Fragment> fragments = new ArrayList<>();

		public ViewPagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return fragments.get(position).getArguments().getString(KEY_TITLE);
		}

		public void add(Fragment fragment, String title) {
			Bundle bundle = fragment.getArguments();
			if (bundle == null)
				bundle = new Bundle();
			if (!bundle.containsKey(KEY_TITLE))
				bundle.putString(KEY_TITLE, title);
			fragment.setArguments(bundle);

			fragments.add(fragment);

			notifyDataSetChanged();
		}

		public void remove(Fragment fragment) {
			fragments.remove(fragment);

			notifyDataSetChanged();
		}

		public void clear() {
			fragments.clear();

			notifyDataSetChanged();
		}

	}

	//endregion

	//region Playback

	private CircleProgressView progress;
	private TextView title;
	private TextView info;

	private ImageView play_pause_stop;
	private ImageView random;

	private void createPlayback() {
		title = findViewById(R.id.title);
		info = findViewById(R.id.info);

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

				getMusicService().stop();

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

		View.OnTouchListener touchListener = new AndroidTouchEx.OnSwipeTouchListener() {
			@Override
			public boolean onSwipeLeft() {
				if (getMusicService() != null) {
					getMusicService().prev();

					root.startAnimation(AnimationUtils.loadAnimation(root.getContext(), R.anim.shake));
				}
				return true;
			}

			@Override
			public boolean onSwipeRight() {
				if (getMusicService() != null) {
					getMusicService().next();

					root.startAnimation(AnimationUtils.loadAnimation(root.getContext(), R.anim.shake));
				}
				return true;
			}

			@Override
			public boolean onSwipeTop() {
				onClickListener.onClick(root);

				return true;
			}

			@Override
			public boolean onSwipeBottom() {
				try {
					onBackPressed();
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}
		};

		findViewById(R.id.touch_layout).setOnTouchListener(touchListener);

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
			play_pause_stop.setImageResource(R.drawable.ic_pause_black);

	}

	//endregion

	//region Help

	private void initHelp() {
		final String tag_guide = TAG + ".guide";

		if (Once.beenDone(Once.THIS_APP_INSTALL, tag_guide)) {

			final String tag_release_notes = TAG + ".release_notes";
			if (!BuildConfig.DEBUG && !Once.beenDone(Once.THIS_APP_VERSION, tag_release_notes)) {
				SettingsActivity.showReleaseNotesDialog(this);
				Once.markDone(tag_release_notes);
			} else {

				MainActivity.initTips(new WeakReference<FragmentActivity>(this));

			}

			return;
		}

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

								info("Hey, swipe left to listen the top music this week!");
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

						info("Anyway, swipe left to listen the top music this week!");

						dialogInterface.dismiss();
					}
				}))
				.show();
	}

	private void tour(MaterialIntroListener onFinal) {
		final MaterialIntroView.Builder guide_start = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Welcome! \n\nNow, tap anywhere on blue screen!")
				.setTarget(parallax_layout)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_ldrawer = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Left drawer. So many options here, be sure to checkout all, later.")
				.setTarget(findViewById(R.id.nav_layout))
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_mini = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("This is mini playback ui, for quick view of now playing. Buttons in it have long-press functions too.")
				.setTarget(parallax_layout)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_final = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Now go play something (Wait for initial scan...)!")
				.setTarget(viewPager)
				.setUsageId(UUID.randomUUID().toString());

		guide_final.setListener(onFinal);
		guide_mini.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_final.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_ldrawer.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				drawer_layout.closeDrawer(Gravity.START);

				try {
					guide_mini.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_start.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				drawer_layout.openDrawer(Gravity.START);

				try {
					guide_ldrawer.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		try {
			guide_start.show();
		} catch (Exception e) {
			e.printStackTrace();
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
