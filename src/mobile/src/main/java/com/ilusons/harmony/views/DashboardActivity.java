package com.ilusons.harmony.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.StorageEx;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.turingtechnologies.materialscrollbar.ICustomAdapter;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import co.mobiwise.materialintro.animation.MaterialIntroListener;
import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.view.MaterialIntroView;
import io.realm.Realm;
import jonathanfinerty.once.Once;

// TODO: See below
// https://www.reddit.com/r/androidapps/comments/6lxp6q/do_you_know_any_android_music_player_or_playlist/

public class DashboardActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = DashboardActivity.class.getSimpleName();

	// Request codes
	private static final int REQUEST_FILE_PICK = 4684;
	private static final int REQUEST_EXPORT_LOCATION_PICK_SAF = 59;
	private static final int REQUEST_PLAYLIST_ADD_PICK = 564;

	// UI
	private DrawerLayout drawer_layout;
	private boolean appBarIsExpanded = false;
	private AppBarLayout appBar_layout;
	private View root;
	private AVLoadingIndicatorView loading;

	private LibraryViewFragment libraryViewFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Set view
		setContentView(R.layout.dashboard_activity);

		// Set bar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		final DrawerArrowDrawable homeDrawable = new DrawerArrowDrawable(getSupportActionBar().getThemedContext());
		getSupportActionBar().setHomeAsUpIndicator(homeDrawable);

		appBar_layout = findViewById(R.id.appBar_layout);
		appBar_layout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
			@Override
			public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
				appBarIsExpanded = (verticalOffset == 0);

				float percentage = ((float) Math.abs(verticalOffset) / appBarLayout.getTotalScrollRange());

				if (1.0f - percentage == 1f) {
					homeDrawable.setVerticalMirror(true);
				} else if (1.0f - percentage == 0f) {
					homeDrawable.setVerticalMirror(false);
				}
				homeDrawable.setProgress(1.0f - percentage);
			}
		});
		appBar_layout.setExpanded(appBarIsExpanded, true);
		appBar_layout.animate();

		drawer_layout = findViewById(R.id.drawer_layout);
		drawer_layout.closeDrawer(GravityCompat.START);
		drawer_layout.closeDrawer(GravityCompat.END);

		/*
		ImageButton toolbar_menu = (ImageButton) findViewById(R.id.toolbar_menu);
		toolbar_menu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				view.animate().rotationBy(180).setDuration(330).start();

				if (appBarIsExpanded) {
				}

				appBar_layout.setExpanded(!appBarIsExpanded, true);
			}
		});
		*/

		// Set views
		root = findViewById(R.id.root);

		loading = findViewById(R.id.loading);

		loading.smoothToShow();

		// Drawers
		createDrawers();

		// Tabs
		createTabs();

		// PlaybackUIMini
		createPlaybackUIMini();

		// Playlists
		createPlaylists();

		// Ratings
		MainActivity.initRateMe(new WeakReference<FragmentActivity>(this));

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

		// Debug
		/*
		if (BuildConfig.DEBUG)
			try {
				List<Class<? extends RealmObject>> classes = new ArrayList<>();
				classes.add(Music.class);
				classes.add(Playlist.class);
				new RealmBrowser.Builder(this)
						.add(DB.getDBConfig(), classes)
						.showNotification();
			} catch (Exception e) {
				e.printStackTrace();
			}
		*/

		loading.smoothToHide();

	}

	@Override
	protected void onResume() {
		super.onResume();

		updatePlaybackUIMini();
	}

	@Override
	public void onBackPressed() {
		if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
			drawer_layout.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case android.R.id.home:
				if (appBarIsExpanded) {
					// CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBar_layout.getLayoutParams();
					// lp.height = getResources().getDisplayMetrics().heightPixels / 3;
				}
				appBar_layout.setExpanded(!appBarIsExpanded, true);
				if (!appBarIsExpanded)
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
		} else if (requestCode == REQUEST_EXPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
					FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

					boolean result = true;

					//noinspection ConstantConditions
					String base = (new File(StorageEx.getPath(this, uri))).getParent();
					String nl = System.getProperty("line.separator");
					StringBuilder sb = new StringBuilder();
					for (Music music : libraryViewFragment.getViewPlaylist().getItems()) {
						String url = IOEx.getRelativePath(base, music.getPath());
						sb.append(url).append(nl);
					}

					try {
						IOUtils.write(sb.toString(), fileOutputStream, "utf-8");
					} catch (Exception e) {
						result = false;

						e.printStackTrace();
					}

					if (result)
						info("Current playlist exported!");
					else
						info("Export failed!", true);

					fileOutputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} else if (requestCode == REQUEST_PLAYLIST_ADD_PICK && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				try {
					ClipData clipData = data.getClipData();

					try (Realm realm = DB.getDB()) {
						if (realm == null)
							return;
						for (int i = 0; i < clipData.getItemCount(); i++)
							try {
								String path = StorageEx.getPath(this, clipData.getItemAt(i).getUri());
								if (path != null)
									Playlist.scanNew(realm, this, libraryViewFragment.getViewPlaylist(), path, null, false);
							} catch (Exception e) {
								e.printStackTrace();
							}
						Playlist.savePlaylist(realm, libraryViewFragment.getViewPlaylist());
					}

					libraryViewFragment.setViewPlaylist(libraryViewFragment.getViewPlaylist());

					info("Playlist updated!");
				} catch (Exception e) {
					e.printStackTrace();

					info("Unable to add selected items to the playlist!");
				}

			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
		super.OnMusicServiceChanged(className, musicService, isBound);

		updatePlaybackUIMini();
	}

	@Override
	public void OnMusicServicePlay() {
		super.OnMusicServicePlay();

		if (playbackUIMiniFragment != null)
			playbackUIMiniFragment.onMusicServicePlay();
	}

	@Override
	public void OnMusicServicePause() {
		super.OnMusicServicePause();

		if (playbackUIMiniFragment != null)
			playbackUIMiniFragment.onMusicServicePause();
	}

	@Override
	public void OnMusicServiceStop() {
		super.OnMusicServiceStop();

		if (playbackUIMiniFragment != null)
			playbackUIMiniFragment.onMusicServiceStop();
	}

	@Override
	public void OnMusicServiceOpen(String uri) {
	}

	@Override
	public void OnMusicServicePrepared() {
		updatePlaybackUIMini();
	}

	@Override
	public void OnMusicServiceLibraryUpdateBegins() {
		info("Library update is in progress!", true);

		loading.smoothToShow();
	}

	@Override
	public void OnMusicServiceLibraryUpdated() {
		try {
			// Refresh list of playlists
			playlistsRecyclerViewAdapter.refresh();

			// Refresh view playlist
			if (libraryViewFragment.getViewPlaylist() != null) {
				libraryViewFragment.setViewPlaylist(Playlist.loadOrCreatePlaylist(libraryViewFragment.getViewPlaylist().getName()));
			} else {
				String name = Playlist.getActivePlaylist(this);
				if (!TextUtils.isEmpty(name))
					libraryViewFragment.setFromPlaylist(-1L, name);
			}

			// Update mini now playing ui
			updatePlaybackUIMini();
		} catch (Exception e) {
			e.printStackTrace();
		}

		info("Library updated!");

		updatePlaybackUIMini();

		loading.smoothToHide();
	}

	@Override
	public void OnMusicServicePlaylistChanged(String name) {
		loading.smoothToShow();
		try {
			// Refresh list of playlists
			playlistsRecyclerViewAdapter.refresh();

			// Refresh view playlist
			if (libraryViewFragment.getViewPlaylist().getName().equals(name)) {
				libraryViewFragment.setViewPlaylist(libraryViewFragment.getViewPlaylist());
			} else {
				if (!TextUtils.isEmpty(name))
					libraryViewFragment.setFromPlaylist(-1L, name);
			}

			// Update mini now playing ui
			updatePlaybackUIMini();
		} catch (Exception e) {
			e.printStackTrace();
		}
		loading.smoothToHide();
	}

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
		findViewById(R.id.open).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				infoDialog("Select and play any support media from local storage.");
				return true;
			}
		});

		findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent musicServiceIntent = new Intent(DashboardActivity.this, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
				musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
				startService(musicServiceIntent);

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});
		findViewById(R.id.refresh).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				infoDialog("Re-loads all library media and also scan for new and changed.");
				return true;
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

		findViewById(R.id.sleep_timer).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (MusicService.IsPremium) {
					Intent intent = new Intent(DashboardActivity.this, SleepTimerActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
				} else {
					MusicService.showPremiumFeatureMessage(getApplicationContext());
				}

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.analytics).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (MusicService.IsPremium) {
					Intent intent = new Intent(DashboardActivity.this, AnalyticsViewFragment.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
				} else {
					// TODO: Free for sometime.
					Intent intent = new Intent(DashboardActivity.this, AnalyticsViewFragment.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					// MusicService.showPremiumFeatureMessage(getApplicationContext());
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

		findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null)
					getMusicService().stop();

				System.exit(0);
			}
		});

		drawer_layout.closeDrawer(GravityCompat.END);

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

	private void createTabs() {
		viewPager = findViewById(R.id.viewPager);

		viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(viewPagerAdapter);

		tab_layout = findViewById(R.id.tab_layout);

		tab_layout.post(new Runnable() {
			@Override
			public void run() {
				tab_layout.setupWithViewPager(viewPager, true);
			}
		});

		viewPagerAdapter.add(AnalyticsViewFragment.create(), "Analytics");

		libraryViewFragment = LibraryViewFragment.create();
		viewPagerAdapter.add(libraryViewFragment, "Library");

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

	//region PlaybackUIMini

	private PlaybackUIMiniFragment playbackUIMiniFragment;
	private View playbackUIMini;

	private void createPlaybackUIMini() {
		playbackUIMini = findViewById(R.id.playbackUIMiniFragment);
		playbackUIMiniFragment = PlaybackUIMiniFragment.create();
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.playbackUIMiniFragment, playbackUIMiniFragment)
				.commit();
		playbackUIMiniFragment.setJumpOnClickListener(new WeakReference<View.OnClickListener>(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				libraryViewFragment.getAdapter().jumpToCurrentlyPlayingItem();
			}
		}));
	}

	private void updatePlaybackUIMini() {
		if (playbackUIMiniFragment != null)
			try {
				playbackUIMiniFragment.reset(getMusicService());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	//endregion

	//region Playlists

	private PlaylistsRecyclerViewAdapter playlistsRecyclerViewAdapter;

	private void createPlaylists() {
		findViewById(R.id.new_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					final EditText editText = new EditText(DashboardActivity.this);

					new AlertDialog.Builder(DashboardActivity.this)
							.setTitle("Create new playlist")
							.setMessage("Enter name for new playlist ...")
							.setView(editText)
							.setPositiveButton("Create", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									try {
										String name = editText.getText().toString().trim();

										Playlist playlist = Playlist.loadOrCreatePlaylist(name);

										if (playlist != null) {
											Playlist.setActivePlaylist(DashboardActivity.this, name, true);
											libraryViewFragment.setViewPlaylist(playlist);
											playlistsRecyclerViewAdapter.refresh();
											info("Playlist created!");
										} else
											throw new Exception("Some error.");
									} catch (Exception e) {
										e.printStackTrace();

										info("Playlist creation failed!");
									}
								}
							})
							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
								}
							})
							.show();
				} catch (Exception e) {
					e.printStackTrace();
				}

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});

		findViewById(R.id.add_to_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Intent i = new Intent();
					String[] mimes = new String[]{"audio/*", "video/*"};
					i.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
					i.setType(StringUtils.join(mimes, '|'));
					i.setAction(Intent.ACTION_OPEN_DOCUMENT);
					i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					startActivityForResult(i, REQUEST_PLAYLIST_ADD_PICK);
				} catch (Exception e) {
					e.printStackTrace();
				}

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});

		findViewById(R.id.save_active_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Playlist.savePlaylist(libraryViewFragment.getViewPlaylist());

					info("Playlist updated!");
				} catch (Exception e) {
					e.printStackTrace();
				}

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});

		findViewById(R.id.export_active_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.putExtra(Intent.EXTRA_TITLE, "Playlist.m3u");
				intent.setType("*/*");
				if (intent.resolveActivity(getPackageManager()) != null) {
					startActivityForResult(intent, REQUEST_EXPORT_LOCATION_PICK_SAF);
				} else {
					info("SAF not found!");
				}

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});

		// Set playlist(s)
		RecyclerView playlists_recyclerView = (RecyclerView) findViewById(R.id.playlists_recyclerView);
		playlists_recyclerView.setHasFixedSize(true);
		playlists_recyclerView.setItemViewCacheSize(5);
		playlists_recyclerView.setDrawingCacheEnabled(true);
		playlists_recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		playlistsRecyclerViewAdapter = new PlaylistsRecyclerViewAdapter();
		playlists_recyclerView.setAdapter(playlistsRecyclerViewAdapter);
		playlistsRecyclerViewAdapter.refresh();

	}

	public class PlaylistsRecyclerViewAdapter extends RecyclerView.Adapter<PlaylistsRecyclerViewAdapter.ViewHolder> {

		private final ArrayList<Pair<Long, String>> data;
		private String dataActive;

		public PlaylistsRecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.library_ui_playlist_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final Pair<Long, String> d = data.get(position);
			final View v = holder.view;

			v.setTag(d.first);

			ImageView icon = v.findViewById(R.id.icon);

			TextView text = (TextView) v.findViewById(R.id.text);
			text.setText(d.second);

			ImageView menu = v.findViewById(R.id.menu);

			int c;
			if (!TextUtils.isEmpty(dataActive) && dataActive.equals(d.second)) {
				c = ContextCompat.getColor(DashboardActivity.this, android.R.color.holo_green_light);
			} else {
				c = ContextCompat.getColor(DashboardActivity.this, R.color.translucent_icons);
			}
			icon.setColorFilter(c, PorterDuff.Mode.SRC_ATOP);
			text.setTextColor(c);
			menu.setColorFilter(c, PorterDuff.Mode.SRC_ATOP);

			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(DashboardActivity.this, R.style.AppTheme_AlertDialogStyle));
					builder.setTitle("Are you sure?");
					builder.setMessage("This will replace the visible playlist with this one.");
					builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							libraryViewFragment.setFromPlaylist(d.first, d.second);
							refresh();

							dialog.dismiss();

							drawer_layout.closeDrawer(GravityCompat.END);
						}
					});
					builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();

							drawer_layout.closeDrawer(GravityCompat.END);
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			};
			icon.setOnClickListener(onClickListener);
			text.setOnClickListener(onClickListener);

			menu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(DashboardActivity.this, R.style.AppTheme_AlertDialogStyle));
					builder.setTitle("Select the action");
					builder.setItems(new CharSequence[]{
							"Set active",
							"Edit / Open in view",
							"Delete"
					}, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0:
									Playlist.setActivePlaylist(DashboardActivity.this, d.second, true);
									refresh();
									break;
								case 1:
									libraryViewFragment.setFromPlaylist(d.first, d.second);
									refresh();
									break;
								case 2:
									Playlist.delete(DashboardActivity.this, d.second, d.first, true);
									refresh();
									break;
							}

							dialog.dismiss();

							drawer_layout.closeDrawer(GravityCompat.END);
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			});

		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ViewHolder(View view) {
				super(view);

				this.view = view;
			}

		}

		public void setData(Collection<Pair<Long, String>> d, String active) {
			data.clear();
			data.addAll(d);
			dataActive = active;
			notifyDataSetChanged();
		}

		public void refresh() {
			final ArrayList<Pair<Long, String>> playlists = new ArrayList<>();
			for (Playlist playlist : Playlist.loadAllPlaylists())
				playlists.add(Pair.create(playlist.getLinkedAndroidOSPlaylistId(), playlist.getName()));
			Playlist.allPlaylist(getContentResolver(), new JavaEx.ActionTU<Long, String>() {
				@Override
				public void execute(Long id, String name) {
					Pair<Long, String> item = new Pair<Long, String>(id, name);
					if (!playlists.contains(item))
						playlists.add(item);
				}
			});
			setData(playlists, Playlist.getActivePlaylist(DashboardActivity.this));
		}

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
		final MaterialIntroView.Builder guide_start = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent_accent))
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
				.setTarget(tab_layout)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_ldrawer = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Left drawer.")
				.setTarget(findViewById(R.id.nav_layout))
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_rdrawer = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("Right drawer. Here library and playlist can be managed.")
				.setTarget(findViewById(R.id.nav_layout_right))
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_mini = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("This mini playback ui, for quick view of now playing. Buttons in it have long-press functions.")
				.setTarget(playbackUIMini)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_final = new MaterialIntroView.Builder(this)
				.setMaskColor(ContextCompat.getColor(this, R.color.translucent_accent))
				.setDelayMillis(500)
				.enableFadeAnimation(true)
				.enableDotAnimation(false)
				.setFocusType(Focus.MINIMUM)
				.setFocusGravity(FocusGravity.CENTER)
				.setTargetPadding(32)
				.dismissOnTouch(true)
				.enableIcon(true)
				.performClick(true)
				.setInfoText("That's all! Now go play something (Wait for initial scan...)!")
				.setTarget(tab_layout)
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
		guide_rdrawer.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				drawer_layout.closeDrawer(Gravity.END);

				try {
					guide_mini.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_ldrawer.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				drawer_layout.closeDrawer(Gravity.START);
				drawer_layout.openDrawer(Gravity.END);

				try {
					guide_rdrawer.show();
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

	//endregion

}
