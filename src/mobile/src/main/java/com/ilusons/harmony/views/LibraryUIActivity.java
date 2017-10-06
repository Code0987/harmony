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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
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

public class LibraryUIActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = LibraryUIActivity.class.getSimpleName();

	// Request codes
	private static final int REQUEST_FILE_PICK = 4684;
	private static final int REQUEST_EXPORT_LOCATION_PICK_SAF = 59;
	private static final int REQUEST_PLAYLIST_ADD_PICK = 564;

	// Data
	RecyclerViewAdapter adapter;
	RecyclerViewExpandableItemManager recyclerViewExpandableItemManager;

	private static final int RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW = 5;

	// UI
	private DrawerLayout drawer_layout;
	private boolean appBarIsExpanded = false;
	private CollapsingToolbarLayout collapse_toolbar;
	private AppBarLayout appBar_layout;
	private View root;
	private AVLoadingIndicatorView loading_view;
	private RecyclerView recyclerView;
	private RecyclerView.OnChildAttachStateChangeListener onChildAttachStateChangeListener;
	private SearchView search_view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// Set view
		setContentView(R.layout.library_ui_activity);

		// Set bar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setCollapsible(false);

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		collapse_toolbar = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
		collapse_toolbar.setEnabled(false);
		collapse_toolbar.setTitle(null);

		final DrawerArrowDrawable homeDrawable = new DrawerArrowDrawable(getSupportActionBar().getThemedContext());
		getSupportActionBar().setHomeAsUpIndicator(homeDrawable);

		appBar_layout = (AppBarLayout) findViewById(R.id.appBar_layout);
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

				if ((collapse_toolbar.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(collapse_toolbar))) {

				} else {

				}
			}
		});
		appBar_layout.setExpanded(appBarIsExpanded, true);
		appBar_layout.animate();

		drawer_layout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer_layout.closeDrawer(GravityCompat.START);
		drawer_layout.closeDrawer(GravityCompat.END);

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

		// Set views
		root = findViewById(R.id.root);

		loading_view = findViewById(R.id.loading_view);

		// Set recycler
		recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		recyclerView.setLayoutManager(new LinearLayoutManager(this) {
			@Override
			public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
				try {
					super.onLayoutChildren(recycler, state);
				} catch (IndexOutOfBoundsException e) {
					Log.w(TAG, e);
				}
			}
		});

		recyclerViewExpandableItemManager = new RecyclerViewExpandableItemManager(null);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new RecyclerViewAdapter();
		recyclerView.setAdapter(recyclerViewExpandableItemManager.createWrappedAdapter(adapter));
		((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
		recyclerViewExpandableItemManager.attachRecyclerView(recyclerView);
		recyclerViewExpandableItemManager.setDefaultGroupsExpandedState(true);

		onChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
			@Override
			public void onChildViewAttachedToWindow(View view) {
				adapter.onGroupItemInView(view);
			}

			@Override
			public void onChildViewDetachedFromWindow(View view) {
				adapter.onGroupItemOutOfView(view);
			}
		};
		recyclerView.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);

		// Set search

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
				Intent musicServiceIntent = new Intent(LibraryUIActivity.this, MusicService.class);
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
				Intent intent = new Intent(LibraryUIActivity.this, PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				drawer_layout.closeDrawer(GravityCompat.START);
			}
		});

		findViewById(R.id.sleep_timer).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (MusicService.IsPremium) {
					Intent intent = new Intent(LibraryUIActivity.this, SleepTimerActivity.class);
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
					Intent intent = new Intent(LibraryUIActivity.this, AnalyticsActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
				} else {
					// TODO: Free for sometime.
					Intent intent = new Intent(LibraryUIActivity.this, AnalyticsActivity.class);
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
				Intent intent = new Intent(LibraryUIActivity.this, SettingsActivity.class);
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

		findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (getMusicService() != null)
					getMusicService().stop();

				System.exit(0);
			}
		});

		search_view = (SearchView) findViewById(R.id.search_view);

		search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				adapter.refresh(query);

				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				adapter.refresh(newText);

				return true;
			}
		});

		// Set collapse items
		createUIFilters();
		createUISortMode();
		createUIGroupMode();
		createUIViewMode();

		// Set right drawer
		drawer_layout.closeDrawer(GravityCompat.END);

		// Playlists
		createPlaylists();

		// Group
		UIGroup();

		// PlaybackUIMini
		createPlaybackUIMini();

		// Feedback
		findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MainActivity.gotoFeedback(LibraryUIActivity.this);
			}
		});

		loading_view.smoothToHide();

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

		// Load playlist
		loadActivePlaylist();
	}

	@Override
	protected void onDestroy() {
		if (onChildAttachStateChangeListener != null)
			recyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);

		super.onDestroy();
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
			// super.onBackPressed();
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
					for (Music music : viewPlaylist.getItems()) {
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
				try (Realm realm = DB.getDB()) {
					if (realm == null)
						return;

					ClipData clipData = data.getClipData();
					for (int i = 0; i < clipData.getItemCount(); i++)
						try {
							String path = StorageEx.getPath(this, clipData.getItemAt(i).getUri());
							if (path != null)
								Playlist.scanNew(realm, this, viewPlaylist, path, null, false);
						} catch (Exception e) {
							e.printStackTrace();
						}

					adapter.setData(viewPlaylist.getItems());

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
		loading_view.smoothToShow();

		info("Library update is in progress!", true);
	}

	@Override
	public void OnMusicServiceLibraryUpdated() {
		loading_view.smoothToHide();

		// Load playlist
		loadActivePlaylist();

		info("Library updated!");

		updatePlaybackUIMini();
	}

	@Override
	public void OnMusicServicePlaylistChanged(String name) {
		try {
			// Refresh list of playlists
			playlistsRecyclerViewAdapter.refresh();

			// Refresh view playlist
			if (viewPlaylist != null) {
				adapter.setData(viewPlaylist.getItems());
			} else {
				if (!TextUtils.isEmpty(name))
					setFromPlaylist(-1L, name);
			}

			// Update mini now playing ui
			updatePlaybackUIMini();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
				adapter.jumpToCurrentlyPlayingItem();
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
				.setTarget(collapse_toolbar)
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

		final MaterialIntroView.Builder guide_recycler = new MaterialIntroView.Builder(this)
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
				.setInfoText("Playlist currently active. Long-press to see sub-menu on item.")
				.setTarget(recyclerView)
				.setUsageId(UUID.randomUUID().toString());

		final MaterialIntroView.Builder guide_search = new MaterialIntroView.Builder(this)
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
				.setInfoText("Here is little search bar, you can use it to find a item. Also, swipe down for some more options!")
				.setTarget(search_view)
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
				.setTarget(collapse_toolbar)
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
		guide_search.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_mini.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		guide_recycler.setListener(new MaterialIntroListener() {
			@Override
			public void onUserClicked(String usageId) {
				try {
					guide_search.show();
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
					guide_recycler.show();
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

	//region Playlists

	private Playlist viewPlaylist;

	private PlaylistsRecyclerViewAdapter playlistsRecyclerViewAdapter;

	private void createPlaylists() {
		findViewById(R.id.load_library).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setFromLibrary();

				drawer_layout.closeDrawer(GravityCompat.END);
			}
		});
		findViewById(R.id.load_library).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				infoDialog("Loads all the library media into view playlist.");
				return true;
			}
		});

		findViewById(R.id.new_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					final EditText editText = new EditText(LibraryUIActivity.this);

					editText.setHint("http://www.librarising.com/astrology/celebs/images2/QR/queenelizabethii.jpg");

					new AlertDialog.Builder(LibraryUIActivity.this)
							.setTitle("Create new playlist")
							.setMessage("Enter name for new playlist ...")
							.setView(editText)
							.setPositiveButton("Create", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									try {
										String name = editText.getText().toString().trim();

										Playlist playlist = Playlist.loadOrCreatePlaylist(name);

										if (playlist != null) {
											Playlist.setActivePlaylist(LibraryUIActivity.this, name, true);
											viewPlaylist = playlist;
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
					Playlist.savePlaylist(viewPlaylist);

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

	private void setFromLibrary() {
		loading_view.smoothToShow();
		if (getMusicService() != null) {
			adapter.setData(Music.loadAll());
		}
		loading_view.smoothToHide();
	}

	private void setFromPlaylist(Long playlistId, final String playlist) {
		loading_view.smoothToShow();

		info("Do not refresh until this playlist is fully loaded!", true);

		Playlist.setActivePlaylist(
				this,
				playlist,
				playlistId,
				new JavaEx.ActionT<Collection<Music>>() {
					@Override
					public void execute(Collection<Music> data) {
						if (data.size() % RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW == 0)
							adapter.setData(data);
					}
				},
				new JavaEx.ActionT<Playlist>() {
					@Override
					public void execute(final Playlist playlist) {
						viewPlaylist = playlist;

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								adapter.setData(playlist.getItems());
							}
						});

						loading_view.smoothToHide();

						info("Loaded playlist!");
					}
				},
				new JavaEx.ActionT<Exception>() {
					@Override
					public void execute(Exception e) {
						loading_view.smoothToHide();

						info("Playlist load failed!");
					}
				},
				false);
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
				c = ContextCompat.getColor(getApplicationContext(), android.R.color.holo_green_light);
			} else {
				c = ContextCompat.getColor(getApplicationContext(), R.color.translucent_icons);
			}
			icon.setColorFilter(c, PorterDuff.Mode.SRC_ATOP);
			text.setTextColor(c);
			menu.setColorFilter(c, PorterDuff.Mode.SRC_ATOP);

			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(LibraryUIActivity.this, R.style.AppTheme_AlertDialogStyle));
					builder.setTitle("Are you sure?");
					builder.setMessage("This will replace the visible playlist with this one.");
					builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							setFromPlaylist(d.first, d.second);
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
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(LibraryUIActivity.this, R.style.AppTheme_AlertDialogStyle));
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
									Playlist.setActivePlaylist(LibraryUIActivity.this, d.second, true);
									refresh();
									break;
								case 1:
									setFromPlaylist(d.first, d.second);
									refresh();
									break;
								case 2:
									Playlist.deletePlaylist(getContentResolver(), d.first);
									removeData(d.first);
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

		public void removeData(final Long id) {
			Pair<Long, String> d = null;
			for (Pair<Long, String> pair : data) {
				if (pair.first.equals(id)) {
					d = pair;
					break;
				}
			}
			if (d != null) {
				data.remove(d);
				notifyDataSetChanged();
			}
		}

		public void refresh() {
			final ArrayList<Pair<Long, String>> playlists = new ArrayList<>();
			for (Playlist playlist : Playlist.loadAllPlaylists())
				playlists.add(Pair.create(-1L, playlist.getName()));
			Playlist.allPlaylist(getContentResolver(), new JavaEx.ActionTU<Long, String>() {
				@Override
				public void execute(Long id, String name) {
					playlists.add(new Pair<Long, String>(id, name));
				}
			});
			setData(playlists, Playlist.getActivePlaylist(getApplicationContext()));
		}

	}

	private void loadActivePlaylist() {
		setFromPlaylist(-1L, Playlist.getActivePlaylist(this));
	}

	//endregion

	//region Library view

	public class RecyclerViewAdapter extends AbstractExpandableItemAdapter<GroupViewHolder, ViewHolder> implements ICustomAdapter {

		private static final String TAG_GROUP = "group";

		private static final int ITEMS_PER_AD = 8;
//		private AdListener lastAdListener = null;

		private final List<Music> data;
		private final List<Pair<String, List<Object>>> dataFiltered;

		private final SettingsActivity.UIStyle uiStyle;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
			dataFiltered = new ArrayList<>();

			uiStyle = SettingsActivity.getUIStyle(getApplicationContext());

			setHasStableIds(true);
		}

		@Override
		public int getGroupCount() {
			return dataFiltered.size();
		}

		@Override
		public int getChildCount(int groupPosition) {
			return dataFiltered.get(groupPosition).second.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition + childPosition;
		}

		@Override
		public GroupViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.library_ui_item_group, parent, false);

			return new GroupViewHolder(view);
		}

		@Override
		public ViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			int layoutId = -1;
			switch (uiStyle) {
				case LUI2:
					layoutId = R.layout.library_ui_lui2_item;
					break;

				case LUI5:
					layoutId = R.layout.library_ui_lui5_item;
					break;

				case LUI11:
					layoutId = R.layout.library_ui_lui11_item;
					break;

				case LUI12:
					layoutId = R.layout.library_ui_lui12_item;
					break;

				case Default:
				default:
					layoutId = R.layout.library_ui_default_item;
					break;
			}

			View view = inflater.inflate(layoutId, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindGroupViewHolder(GroupViewHolder holder, int groupPosition, int viewType) {
			final String d = dataFiltered.get(groupPosition).first;
			final View v = holder.view;

			TextView title = ((TextView) v.findViewById(R.id.title));
			title.setText(d);

			try {
				final ImageView cover = (ImageView) v.findViewById(R.id.cover);
				if (cover != null) {
					cover.setImageBitmap(null);
					final int coverSize = Math.max(cover.getWidth(), cover.getHeight());

					if (!TextUtils.isEmpty(d) && d.length() > 5) {
						ArtworkEx.ArtworkType artworkType;
						switch (getUIGroupMode(LibraryUIActivity.this)) {
							case Album:
								artworkType = ArtworkEx.ArtworkType.Album;
								break;
							case Artist:
								artworkType = ArtworkEx.ArtworkType.Artist;
								break;
							case Default:
							default:
								artworkType = ArtworkEx.ArtworkType.Song;
								break;
						}

						ArtworkEx.getArtworkDownloaderTask(
								LibraryUIActivity.this,
								d,
								artworkType,
								-1,
								d,
								Music.KEY_CACHE_DIR_COVER,
								d,
								new JavaEx.ActionT<Bitmap>() {
									@Override
									public void execute(Bitmap bitmap) {
										TransitionDrawable d = new TransitionDrawable(new Drawable[]{
												cover.getDrawable(),
												new BitmapDrawable(getResources(), bitmap)
										});

										cover.setImageDrawable(d);

										d.setCrossFadeEnabled(true);
										d.startTransition(200);
									}
								},
								new JavaEx.ActionT<Exception>() {
									@Override
									public void execute(Exception e) {
										Log.w(TAG, e);

										cover.setImageDrawable(null);
									}
								},
								1500);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onBindChildViewHolder(ViewHolder holder, int groupPosition, int childPosition, int viewType) {
			final Object d = dataFiltered.get(groupPosition).second.get(childPosition);
			final View v = holder.view;

			// Bind data to view here!

/*
			// TODO: Fix ads later
			if (false && ((BuildConfig.DEBUG || !MusicService.IsPremium) && (d instanceof NativeExpressAdView && lastAdListener == null))) {

				CardView cv = (CardView) v.findViewById(R.id.cardView);

				final NativeExpressAdView adView = (NativeExpressAdView) d;

				try {
					if (adView.getParent() == null) {

						int w = (int) ((cv.getWidth() - cv.getPaddingLeft() - cv.getPaddingRight()) / getResources().getDisplayMetrics().density);
						int h = (int) ((cv.getHeight() - cv.getPaddingTop() - cv.getPaddingBottom()) / getResources().getDisplayMetrics().density);

						if (w > 280 && h > 80) {
							adView.setAdUnitId(BuildConfig.AD_UNIT_ID_NE1);
							adView.setAdSize(new AdSize(w, h));

							cv.addView(adView, new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT));

							lastAdListener = new AdListener() {
								@Override
								public void onAdLoaded() {
									super.onAdLoaded();

									lastAdListener = null;
								}

								@Override
								public void onAdFailedToLoad(int errorCode) {

									lastAdListener = null;
								}
							};
							adView.setAdListener(lastAdListener);

							AdRequest adRequest;
							if (BuildConfig.DEBUG) {
								adRequest = new AdRequest.Builder()
										.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
										.addTestDevice(AndroidEx.getDeviceIdHashed(LibraryUIActivity.this))
										.build();
							} else {
								adRequest = new AdRequest.Builder()
										.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
										.build();
							}

							adView.loadAd(adRequest);

						}
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}

			} else
*/
			if (d instanceof Music) {

				final Music item = (Music) d;

				final View root = v.findViewById(R.id.root);

				final ImageView cover = (ImageView) v.findViewById(R.id.cover);
				if (cover != null) {
					cover.setImageBitmap(null);
					// HACK: This animates as well as reduces load on image view
					final int coverSize = Math.max(cover.getWidth(), cover.getHeight());
					(new AsyncTask<Void, Void, Bitmap>() {
						@Override
						protected Bitmap doInBackground(Void... voids) {
							return item.getCover(LibraryUIActivity.this, coverSize);
						}

						@Override
						protected void onPostExecute(Bitmap bitmap) {
							TransitionDrawable d = new TransitionDrawable(new Drawable[]{
									cover.getDrawable(),
									new BitmapDrawable(getResources(), bitmap)
							});

							cover.setImageDrawable(d);

							d.setCrossFadeEnabled(true);
							d.startTransition(200);
						}
					}).execute();
				}

				TextView title = (TextView) v.findViewById(R.id.title);
				if (title != null)
					title.setText(item.getTitle());

				TextView artist = (TextView) v.findViewById(R.id.artist);
				if (artist != null)
					artist.setText(item.getArtist());

				TextView album = (TextView) v.findViewById(R.id.album);
				if (album != null)
					album.setText(item.getAlbum());

				TextView info = (TextView) v.findViewById(R.id.info);
				if (info != null) {
					String s;
					try {
						s = item.getTextExtraOnlySingleLine(getMusicService().getPlaylist().getItems().lastIndexOf(item));
					} catch (Exception e) {
						e.printStackTrace();

						s = item.getTextExtraOnlySingleLine();
					}
					info.setText(s);
					info.setHorizontallyScrolling(true);
					info.setSelected(true);
					info.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							TextView tv = (TextView) v;
							if (!hasFocus && tv != null) {
								tv.setSelected(true);
							}
						}
					});
				}

				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent i = new Intent(LibraryUIActivity.this, MusicService.class);

						i.setAction(MusicService.ACTION_OPEN);
						i.putExtra(MusicService.KEY_URI, item.getPath());

						startService(i);

						highlightView(root);
					}
				});

				v.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view) {
						if (viewPlaylist == null) {
							info("Open a playlist into view first!");
							return true;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(LibraryUIActivity.this, R.style.AppTheme_AlertDialogStyle));
						builder.setTitle("Select the action");
						builder.setItems(new CharSequence[]{
								"Share",
								"Add next",
								"Add at start",
								"Add at last",
								"Remove",
								"Clear (except current)",
								"Move down",
								"Move up",
								"Delete (physically)"
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int itemIndex) {
								try {
									switch (itemIndex) {
										case 0:
											Intent shareIntent = new Intent();
											shareIntent.setAction(Intent.ACTION_SEND);
											shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTextDetailedMultiLine());
											shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(item.getPath())));
											if (item.getPath().toLowerCase().endsWith("mp3"))
												shareIntent.setType("audio/mp3");
											else if (item.hasVideo())
												shareIntent.setType("video/*");
											else
												shareIntent.setType("audio/*");
											shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
											startActivity(Intent.createChooser(shareIntent, "Share " + item.getText() + " ..."));
											break;
										case 1:
											viewPlaylist.add(item, viewPlaylist.getItems().lastIndexOf(item) + 1);
											break;
										case 2:
											viewPlaylist.add(item, 0);
											playlistsRecyclerViewAdapter.refresh();
											break;
										case 3:
											viewPlaylist.add(item, viewPlaylist.getItems().size());
											break;
										case 4:
											viewPlaylist.remove(item);
											break;
										case 5:
											viewPlaylist.removeAllExceptCurrent();
											break;
										case 6:
											viewPlaylist.moveDown(item);
											break;
										case 7:
											viewPlaylist.moveUp(item);
											break;
										case 8:
											viewPlaylist.delete(item, getMusicService(), true);
											break;
									}

									adapter.setData(viewPlaylist.getItems());
								} catch (Exception e) {
									Log.w(TAG, e);
								}
							}
						});
						AlertDialog dialog = builder.create();
						dialog.show();

						final String tag_pl_cstm = TAG + ".pl_cstm";
						if (!Once.beenDone(Once.THIS_APP_VERSION, tag_pl_cstm)) {
							(new AlertDialog.Builder(new ContextThemeWrapper(LibraryUIActivity.this, R.style.AppTheme_AlertDialogStyle))
									.setTitle("Playlist customization")
									.setMessage("Next, Ok, will open a list of actions to allow you to customize current playlist or Now playing playlist. If you have enabled sorting, you won't see changes, just set it to Default/Custom. Example: Add will add the selected music to current playlist from visible playlist.")
									.setCancelable(false)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {
											dialogInterface.dismiss();
										}
									}))
									.show();

							Once.markDone(tag_pl_cstm);
						}

						return true;
					}
				});

			}

		}

		@Override
		public boolean onCheckCanExpandOrCollapseGroup(GroupViewHolder holder, int groupPosition, int x, int y, boolean expand) {
			return true;
		}

		@Override
		public boolean getInitialGroupExpandedState(int groupPosition) {
			return true;
		}

		public void setData(Collection<Music> d) {
			data.clear();
			data.addAll(d);

			refresh(String.valueOf(search_view.getQuery()));
		}

		public void removeData(Music d) {
			data.remove(d);

			refresh(String.valueOf(search_view.getQuery()));
		}

		public void refresh(final String q) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {

						(LibraryUIActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								loading_view.smoothToShow();
							}
						});

						// Filter
						List<Music> filtered = new ArrayList<>();
						filtered.addAll(data);
						filtered = UIFilter(filtered, q);

						// Sort
						List<Music> sorted = UISort(filtered);

						// Group
						Map<String, List<Music>> grouped = UIGroup(sorted);

						// Apply
						dataFiltered.clear();
						for (Map.Entry<String, List<Music>> entry : grouped.entrySet())
							dataFiltered.add(new Pair<String, List<Object>>(entry.getKey(), new ArrayList<Object>(entry.getValue())));

						// Add ads
						// TODO: Fix ads later
						/*if (false && (BuildConfig.DEBUG || !MusicService.IsPremium)) {
						    final int n = Math.min(dataFiltered.size(), 7);
                            for (int i = 0; i <= n; i += ITEMS_PER_AD)
                                try {
                                    final NativeExpressAdView adView = new NativeExpressAdView(LibraryUIActivity.this);
                                    dataFiltered.add(i, adView);
                                } catch (Exception e) {
                                    Log.w(TAG, e);
                                }
                        }*/

						(LibraryUIActivity.this).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								loading_view.smoothToHide();

								notifyDataSetChanged();
							}
						});

					} catch (Exception e) {
						Log.w(TAG, e);
					}
				}
			}).start();
		}

		public void highlightView(View root) {
			try {
				root.clearAnimation();

				AnimatorSet as = new AnimatorSet();
				as.playSequentially(
						ObjectAnimator.ofArgb(root, "backgroundColor", ContextCompat.getColor(getApplicationContext(), R.color.transparent), ContextCompat.getColor(getApplicationContext(), R.color.accent), ContextCompat.getColor(getApplicationContext(), R.color.transparent))
				);
				as.setDuration(733);
				as.setInterpolator(new LinearInterpolator());
				as.setTarget(root);

				as.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void bringInToView(int position) {
			try {
				recyclerView.smoothScrollToPosition(position);

				View v = recyclerView.getLayoutManager().getChildAt(position);

				highlightView(v);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void jumpToCurrentlyPlayingItem() {
			try {
				int pg = -1;
				int pc = -1;
				String current = getMusicService().getMusic().getPath();
				for (int gi = 0; gi < dataFiltered.size(); gi++) {
					Pair<String, List<Object>> group = dataFiltered.get(gi);
					for (int ci = 0; ci < group.second.size(); ci++) {
						Music child = (Music) group.second.get(ci);
						if (child != null && child.getPath().equals(current)) {
							pg = gi;
							pc = ci;
							break;
						}
					}
				}
				long ppc = RecyclerViewExpandableItemManager.getPackedPositionForChild(pg, pc);
				int k = recyclerViewExpandableItemManager.getFlatPosition(ppc);
				bringInToView(k);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onGroupItemInView(View v) {
			try {
				if (v.isAttachedToWindow() && v.getTag() != null && v.getTag().equals(TAG_GROUP)) {
					ImageView cover = v.findViewById(R.id.cover);
					if (cover != null && !hasNullOrEmptyDrawable(cover)) {
						ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) v.getLayoutParams();
						params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 286, getResources().getDisplayMetrics());
						v.setLayoutParams(params);
						cover.setAlpha(0.85f);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onGroupItemOutOfView(View v) {
			try {
				if (v.isAttachedToWindow() && v.getTag() != null && v.getTag().equals(TAG_GROUP)) {
					ImageView cover = v.findViewById(R.id.cover);
					if (cover != null && !hasNullOrEmptyDrawable(cover)) {
						ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) v.getLayoutParams();
						params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
						v.setLayoutParams(params);
						cover.setAlpha(0.25f);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String getCustomStringForElement(int element) {
			try {
				View v = recyclerView.getLayoutManager().getChildAt(element);

				TextView info = v.findViewById(R.id.info);
				return info.getText().toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public boolean hasNullOrEmptyDrawable(ImageView iv) {
			Drawable drawable = iv.getDrawable();
			BitmapDrawable bitmapDrawable = drawable instanceof BitmapDrawable ? (BitmapDrawable) drawable : null;
			TransitionDrawable transitionDrawable = drawable instanceof TransitionDrawable ? (TransitionDrawable) drawable : null;

			return drawable == null || transitionDrawable == null || (bitmapDrawable == null || bitmapDrawable.getBitmap() == null);
		}
	}

	public class GroupViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public GroupViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	public class ViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public ViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	//endregion

	//region Drawer

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

	//region UI filters

	public enum UIFilters {
		NoMP3("No MP3"),
		NoM4A("No M4A"),
		NoFLAC("No FLAC"),
		NoOGG("No OGG"),
		NoWAV("No WAV"),
		NoMP4("No MP4"),
		NoM4V("No M4V"),
		NoMKV("No MKV"),
		NoAVI("No AVI"),
		NoWEBM("No WEBM"),;

		private String friendlyName;

		UIFilters(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_FILTERS = SPrefEx.TAG_SPREF + ".library_ui_filters";

	public static Set<UIFilters> getUIFilters(Context context) {
		Set<UIFilters> value = new HashSet<>();

		Set<String> values = new HashSet<>();
		for (String item : SPrefEx.get(context).getStringSet(TAG_SPREF_LIBRARY_UI_FILTERS, values)) {
			value.add(UIFilters.valueOf(item));
		}

		return value;
	}

	public static void setUIFilters(Context context, Set<UIFilters> value) {
		Set<String> values = new HashSet<>();
		for (UIFilters item : value) {
			values.add(String.valueOf(item));
		}

		SPrefEx.get(context)
				.edit()
				.putStringSet(TAG_SPREF_LIBRARY_UI_FILTERS, values)
				.apply();
	}

	private Spinner uiFilters_spinner;

	private void createUIFilters() {
		uiFilters_spinner = (Spinner) findViewById(R.id.uiFilters_spinner);

		UIFilters[] items = UIFilters.values();

		uiFilters_spinner.setAdapter(new ArrayAdapter<UIFilters>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView v = (CheckedTextView) convertView;

				if (v == null) {
					v = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					v.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					v.setLayoutParams(lp);
					v.setPadding(px, px, px, px);

					v.setText("Filters");
				}

				return v;
			}

			@Override
			public View getDropDownView(final int position, View convertView, ViewGroup parent) {
				RelativeLayout v = (RelativeLayout) convertView;

				if (v == null) {
					v = new RelativeLayout(getContext());
					v.setLayoutParams(new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					));

					Switch sv = new Switch(getContext());
					sv.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					sv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					sv.setLayoutParams(lp);
					sv.setPadding(px, px, px, px);
					sv.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()));

					v.addView(sv);

					sv.setText(getItem(position).friendlyName);
					sv.setChecked(getUIFilters(getContext()).contains(getItem(position)));
					sv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
							if (!compoundButton.isShown()) {
								return;
							}

							Set<UIFilters> value = getUIFilters(getContext());

							UIFilters item = getItem(position);
							try {
								if (b)
									value.add(item);
								else
									value.remove(item);
							} catch (Exception e) {
								e.printStackTrace();
							}
							setUIFilters(getContext(), value);

							if (search_view != null)
								adapter.refresh(String.valueOf(search_view.getQuery()));
						}
					});
				}

				((Switch) v.getChildAt(0)).setChecked(getUIFilters(getContext()).contains(getItem(position)));

				return v;
			}
		});

		uiFilters_spinner.post(new Runnable() {
			public void run() {
				uiFilters_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public synchronized List<Music> UIFilter(Collection<Music> data, String q) {
		ArrayList<Music> result = new ArrayList<>();

		q = q.toLowerCase();

		for (Music m : data) {
			boolean f = true;

			SharedPreferences spref = SPrefEx.get(LibraryUIActivity.this);

			String ext = m.getPath().substring(m.getPath().lastIndexOf(".")).toLowerCase();
			for (UIFilters uiFilter : getUIFilters(LibraryUIActivity.this)) {
				boolean uif = uiFilter == UIFilters.NoMP3 && ext.equals(".mp3")
						|| uiFilter == UIFilters.NoM4A && ext.equals(".m4a")
						|| uiFilter == UIFilters.NoFLAC && ext.equals(".flac")
						|| uiFilter == UIFilters.NoOGG && ext.equals(".ogg")
						|| uiFilter == UIFilters.NoWAV && ext.equals(".wav")
						|| uiFilter == UIFilters.NoMP4 && ext.equals(".mp4")
						|| uiFilter == UIFilters.NoMKV && ext.equals(".mkv")
						|| uiFilter == UIFilters.NoAVI && ext.equals(".avi")
						|| uiFilter == UIFilters.NoWEBM && ext.equals(".webm");

				if (uif) {
					f = false;
					break;
				}
			}

			f &= TextUtils.isEmpty(q) || q.length() < 1 ||
					(m.getPath().toLowerCase().contains(q)
							|| m.getTitle().toLowerCase().contains(q)
							|| m.getArtist().toLowerCase().contains(q)
							|| m.getAlbum().toLowerCase().contains(q));

			if (f)
				result.add(m);
		}

		return result;
	}

	//endregion

	//region UI sort mode

	public enum UISortMode {
		Default("Default/Custom"),
		Title("Title ▲"),
		Album("Album ▲"),
		Artist("Artist ▲"),
		Played("Times Played ▼"),
		Skipped("Times skipped ▼"),
		Added("Added On ▼"),
		Score("Smart score"),
		Track("# Track"),;

		private String friendlyName;

		UISortMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_SORT_MODE = SPrefEx.TAG_SPREF + ".library_ui_sort_mode";

	public static UISortMode getUISortMode(Context context) {
		return UISortMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_SORT_MODE, String.valueOf(UISortMode.Default)));
	}

	public static void setUISortMode(Context context, UISortMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_SORT_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiSortMode_spinner;

	private void createUISortMode() {
		uiSortMode_spinner = (Spinner) findViewById(R.id.uiSortMode_spinner);

		UISortMode[] items = UISortMode.values();

		uiSortMode_spinner.setAdapter(new ArrayAdapter<UISortMode>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("Sorting: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		UISortMode lastMode = getUISortMode(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiSortMode_spinner.setSelection(i, true);

		uiSortMode_spinner.post(new Runnable() {
			public void run() {
				uiSortMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUISortMode(getApplicationContext(), (UISortMode) adapterView.getItemAtPosition(position));

						if (search_view != null)
							adapter.refresh(String.valueOf(search_view.getQuery()));
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public List<Music> UISort(List<Music> data) {
		final UISortMode uiSortMode = getUISortMode(LibraryUIActivity.this);

		switch (uiSortMode) {
			case Title:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getTitle().compareToIgnoreCase(y.getTitle());
					}
				});
				break;
			case Album:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getAlbum().compareToIgnoreCase(y.getAlbum());
					}
				});
				break;
			case Artist:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getArtist().compareToIgnoreCase(y.getArtist());
					}
				});
				break;
			case Played:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getPlayed(), y.getPlayed());
					}
				});
				Collections.reverse(data);
				break;
			case Skipped:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getSkipped(), y.getSkipped());
					}
				});
				Collections.reverse(data);
				break;
			case Added:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Long.compare(x.getTimeAdded(), y.getTimeAdded());
					}
				});
				Collections.reverse(data);
				break;
			case Score:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Double.compare(x.getScore(), y.getScore());
					}
				});
				Collections.reverse(data);
				break;
			case Track:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getTrack(), y.getTrack());
					}
				});
				break;

			case Default:
			default:
				break;
		}

		return data;
	}

	//endregion

	//region UI group mode

	public enum UIGroupMode {
		Default("Default"),
		Album("Album"),
		Artist("Artist"),;

		private String friendlyName;

		UIGroupMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_GROUP_MODE = SPrefEx.TAG_SPREF + ".library_ui_group_mode";

	public static UIGroupMode getUIGroupMode(Context context) {
		return UIGroupMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(UIGroupMode.Default)));
	}

	public static void setUIGroupMode(Context context, UIGroupMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiGroupMode_spinner;

	private void createUIGroupMode() {
		uiGroupMode_spinner = (Spinner) findViewById(R.id.uiGroupMode_spinner);

		UIGroupMode[] items = UIGroupMode.values();

		uiGroupMode_spinner.setAdapter(new ArrayAdapter<UIGroupMode>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("Grouping: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		UIGroupMode lastMode = getUIGroupMode(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiGroupMode_spinner.setSelection(i, true);

		uiGroupMode_spinner.post(new Runnable() {
			public void run() {
				uiGroupMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIGroupMode(getApplicationContext(), (UIGroupMode) adapterView.getItemAtPosition(position));

						if (search_view != null)
							adapter.refresh(String.valueOf(search_view.getQuery()));
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public Map<String, List<Music>> UIGroup(List<Music> data) {
		Map<String, List<Music>> result = new HashMap<>();

		final UIGroupMode uiGroupMode = getUIGroupMode(LibraryUIActivity.this);

		switch (uiGroupMode) {
			case Album:
				for (Music d : data) {
					String key = d.getAlbum();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case Artist:
				for (Music d : data) {
					String key = d.getArtist();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case Default:
			default:
				result.put("*", data);
				break;
		}

		return result;
	}

	//endregion

	//region UI view mode

	public enum UIViewMode {
		Default("Default"),
		Two("Two"),;

		private String friendlyName;

		UIViewMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_VIEW_MODE = SPrefEx.TAG_SPREF + ".library_ui_view_mode";

	public static UIViewMode getUIViewMode(Context context) {
		return UIViewMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_VIEW_MODE, String.valueOf(UIViewMode.Two)));
	}

	public static void setUIViewMode(Context context, UIViewMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_VIEW_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiViewMode_spinner;

	private void createUIViewMode() {
		uiViewMode_spinner = (Spinner) findViewById(R.id.uiViewMode_spinner);

		UIViewMode[] items = UIViewMode.values();

		uiViewMode_spinner.setAdapter(new ArrayAdapter<UIViewMode>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("View: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		UIViewMode lastMode = getUIViewMode(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiViewMode_spinner.setSelection(i, true);

		uiViewMode_spinner.post(new Runnable() {
			public void run() {
				uiViewMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIViewMode(getApplicationContext(), (UIViewMode) adapterView.getItemAtPosition(position));

						UIGroup();
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	private void UIGroup() {
		final UIViewMode uiViewMode = getUIViewMode(LibraryUIActivity.this);

		switch (uiViewMode) {
			case Two:
				GridLayoutManager layoutManager = new GridLayoutManager(LibraryUIActivity.this, 2) {
					@Override
					public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
						try {
							super.onLayoutChildren(recycler, state);
						} catch (IndexOutOfBoundsException e) {
							Log.w(TAG, e);
						}
					}
				};
				layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
					@Override
					public int getSpanSize(int position) {
						if (RecyclerViewExpandableItemManager
								.getPackedPositionChild(recyclerViewExpandableItemManager
										.getExpandablePosition(position))
								==
								RecyclerView.NO_POSITION) {
							// group item
							return 2;
						} else {
							// child item
							return 1;
						}
					}
				});
				recyclerView.setLayoutManager(layoutManager);
				break;
			case Default:
			default:
				recyclerView.setLayoutManager(new LinearLayoutManager(LibraryUIActivity.this) {
					@Override
					public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
						try {
							super.onLayoutChildren(recycler, state);
						} catch (IndexOutOfBoundsException e) {
							Log.w(TAG, e);
						}
					}
				});
				break;
		}
	}

	//endregion

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_LIBRARY_UI_SORT_MODE,
			TAG_SPREF_LIBRARY_UI_GROUP_MODE,
			TAG_SPREF_LIBRARY_UI_VIEW_MODE,
			TAG_SPREF_LIBRARY_UI_SORT_MODE
	};

}
