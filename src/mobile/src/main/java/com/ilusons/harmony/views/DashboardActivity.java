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

public class DashboardActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = DashboardActivity.class.getSimpleName();

	// UI
	private DrawerLayout drawer_layout;
	private boolean appBarIsExpanded = false;
	private AppBarLayout appBar_layout;
	private View root;
	private AVLoadingIndicatorView loading;

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
//
//		drawer_layout = findViewById(R.id.drawer_layout);
//		drawer_layout.closeDrawer(GravityCompat.START);
//		drawer_layout.closeDrawer(GravityCompat.END);

		// Set views
		root = findViewById(R.id.root);

		loading = findViewById(R.id.loading);

		loading.smoothToShow();

		// Tabs
		createTabs();

		// PlaybackUIMini
		createPlaybackUIMini();

		loading.smoothToHide();

	}

	@Override
	protected void onResume() {
		super.onResume();

		updatePlaybackUIMini();
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
		// TODO: pass to library/pl view
		info("Library update is in progress!", true);
	}


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

		viewPagerAdapter.add(LibraryViewFragment.create(), "Library");
		viewPagerAdapter.add(AnalyticsViewFragment.create(), "Analytics");

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
				//TODO:adapter.jumpToCurrentlyPlayingItem();
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

}
