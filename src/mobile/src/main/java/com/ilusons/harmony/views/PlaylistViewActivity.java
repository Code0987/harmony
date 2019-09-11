package com.ilusons.harmony.views;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.tabs.TabLayout;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.DrawerArrow;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.SPrefEx;
import com.wang.avi.AVLoadingIndicatorView;

import androidx.appcompat.widget.Toolbar;

public class PlaylistViewActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = PlaylistViewActivity.class.getSimpleName();

	// UI
	private View root;

	private AVLoadingIndicatorView loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.playlist_view_activity);

		// Set bar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setHomeAsUpIndicator(new DrawerArrow(this, getSupportActionBar().getThemedContext()));

		// Set views
		root = findViewById(R.id.root);

		loading = findViewById(R.id.loading);

		// Tabs
		createTabs();

		loading.smoothToHide();
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
	public void onBackPressed() {
		MainActivity.openDashboardActivity(this);

		super.onBackPressed();
	}

	@Override
	public void OnMusicServicePrepared() {
		if (playlistViewFragment != null)
			playlistViewFragment.OnMusicServicePrepared();
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
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void OnSearchQueryReceived(String query) {
		try {
			if (playlistViewFragment != null && playlistViewFragment.isVisible()) {
				playlistViewFragment.setSearchQuery(query);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//region Tabs

	public enum PlaylistViewTab {
		Tracks("Tracks"),
		Artists("Artists"),
		Albums("Albums"),;

		private String friendlyName;

		PlaylistViewTab(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_PLAYLIST_VIEW_TAB = "playlist_view_tab";

	public static PlaylistViewTab getPlaylistViewTab(Context context) {
		return PlaylistViewTab.valueOf(SPrefEx.get(context).getString(TAG_SPREF_PLAYLIST_VIEW_TAB, String.valueOf(PlaylistViewTab.Tracks)));
	}

	public static void setPlaylistViewTab(Context context, PlaylistViewTab value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYLIST_VIEW_TAB, String.valueOf(value))
				.apply();
	}

	private TabLayout tab_layout;

	private PlaylistViewFragment playlistViewFragment;

	private void createTabs() {
		tab_layout = findViewById(R.id.tab_layout);

		for (PlaylistViewTab tab : PlaylistViewTab.values()) {
			tab_layout.addTab(tab_layout.newTab().setText(tab.friendlyName).setTag(tab));
		}
		tab_layout.addTab(tab_layout.newTab().setText("Playlists"));

		/*
		final int icon_color = ContextCompat.getColor(this, R.color.icons);
		final int tabs = tab_layout.getTabCount();
		for (int i = 0; i < tabs; i++) {
			TabLayout.Tab tab = tab_layout.getTabAt(i);
			if (tab != null) {
				if (tab.getIcon() != null) {
					tab.getIcon().setColorFilter(icon_color, PorterDuff.Mode.SRC_IN);
				}
			}
		}
		*/

		playlistViewFragment = PlaylistViewFragment.create();

		playlistViewFragment.setPlaylistViewTab(getPlaylistViewTab(this));

		tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				onTab(tab);
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				onTab(tab);
			}
		});

		new Handler().postDelayed(
				() -> {
					final PlaylistViewTab toSelect = getPlaylistViewTab(this);
					final int tabs = tab_layout.getTabCount();
					for (int i = 0; i < tabs; i++) {
						TabLayout.Tab tab = tab_layout.getTabAt(i);
						if (tab != null) {
							if (tab.getTag() != null && ((PlaylistViewTab) tab.getTag()).equals(toSelect)) {
								tab.select();
							}
						}
					}
				}, 333);

	}

	private void onTab(TabLayout.Tab tab) {
		if (tab.getTag() == null) {
			PlaylistSettingsFragment playlistSettingsFragment = PlaylistSettingsFragment.create();

			playlistSettingsFragment.setPlaylistViewFragment(playlistViewFragment);

			getSupportFragmentManager().beginTransaction()
					.replace(R.id.playlist_layout, playlistSettingsFragment)
					.commit();
		} else {
			PlaylistViewTab playlistViewTab = (PlaylistViewTab) tab.getTag();
			if (playlistViewTab == null)
				return;

			setPlaylistViewTab(PlaylistViewActivity.this, playlistViewTab);

			playlistViewFragment.setPlaylistViewTab(playlistViewTab);

			getSupportFragmentManager().beginTransaction()
					.replace(R.id.playlist_layout, playlistViewFragment)
					.commit();

			getSupportFragmentManager().beginTransaction()
					.detach(playlistViewFragment)
					.attach(playlistViewFragment)
					.commit();
		}
	}

	//endregion

}
