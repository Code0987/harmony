package com.ilusons.harmony.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.wang.avi.AVLoadingIndicatorView;

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

		// Hacks
		applyHacksToUI();

		// Set bar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Set views
		root = findViewById(R.id.root);

		loading = (AVLoadingIndicatorView) findViewById(R.id.loading);

		// Tabs
		createTabs();

		// Playlist
		createPlaylist();

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

	//region Other

	private void applyHacksToUI() {
		View nav_bar_filler = findViewById(R.id.nav_bar_filler);
		if (nav_bar_filler != null) {
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) nav_bar_filler.getLayoutParams();
			params.bottomMargin = AndroidEx.getNavigationBarSize(this).y;
		}
	}

	//endregion

	//region Tabs

	public enum PlaylistViewTab {
		Online("Online"),
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

	private void createTabs() {
		tab_layout = findViewById(R.id.tab_layout);

		for (PlaylistViewTab tab : PlaylistViewTab.values())
			tab_layout.addTab(tab_layout.newTab().setText(tab.friendlyName).setTag(tab));

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

	}

	//endregion

	//region Playlist

	private OnlineViewFragment onlineViewFragment;
	private PlaylistViewFragment playlistViewFragment;

	private void createPlaylist() {
		onlineViewFragment = OnlineViewFragment.create();

		playlistViewFragment = PlaylistViewFragment.create();

		tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				PlaylistViewTab playlistViewTab = (PlaylistViewTab) tab.getTag();
				if (playlistViewTab == null)
					return;

				setPlaylistViewTab(PlaylistViewActivity.this, playlistViewTab);

				switch (playlistViewTab) {
					case Online:
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.playlist_layout, onlineViewFragment)
								.commit();
						break;
					case Tracks:
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.playlist_layout, playlistViewFragment)
								.commit();
						break;
					case Artists:
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.playlist_layout, playlistViewFragment)
								.commit();
						break;
					case Albums:
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.playlist_layout, playlistViewFragment)
								.commit();
						break;
				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});

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

	}

	//endregion

}
