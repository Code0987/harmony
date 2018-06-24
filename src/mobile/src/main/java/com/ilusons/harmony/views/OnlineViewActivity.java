package com.ilusons.harmony.views;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
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
import com.ilusons.harmony.base.DrawerArrow;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.wang.avi.AVLoadingIndicatorView;

public class OnlineViewActivity extends BaseUIActivity {

	// Logger TAG
	private static final String TAG = OnlineViewActivity.class.getSimpleName();

	// UI
	private View root;

	private AVLoadingIndicatorView loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.online_view_activity);

		// Hacks
		applyHacksToUI();

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
	public void OnSearchQueryReceived(String query) {
		try {
			if (onlineViewFragment != null && onlineViewFragment.isVisible()) {
				onlineViewFragment.setSearchQuery(query);
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

	public enum Tabs {
		Search("Search"),
		Default("Default"),
		Recommendations("Recommendations"),;

		private String friendlyName;

		Tabs(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	private OnlineViewFragment onlineViewFragment;

	private void createTabs() {
		TabLayout tab_layout = findViewById(R.id.tab_layout);

		for (Tabs tab : Tabs.values())
			tab_layout.addTab(tab_layout.newTab().setText(tab.friendlyName).setTag(tab));

		onlineViewFragment = OnlineViewFragment.create();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_layout, onlineViewFragment)
				.commit();

		tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				Tabs playlistViewTab = (Tabs) tab.getTag();
				if (playlistViewTab == null)
					return;

				switch (playlistViewTab) {
					case Search:
						onlineViewFragment.searchTracks(onlineViewFragment.getSearchQuery().toString());
						break;
					case Recommendations:
						onlineViewFragment.searchRecommendations();
						break;
					case Default:
					default:
						onlineViewFragment.searchDefaultTracks();
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

	}

	//endregion

}
