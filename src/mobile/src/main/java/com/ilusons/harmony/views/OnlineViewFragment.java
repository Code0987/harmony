package com.ilusons.harmony.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Online YouTube/free-music tab is disabled in the Android 36 port. */
public class OnlineViewFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		TextView tv = new TextView(getContext());
		tv.setText("Online catalog was removed. Play files from your library.");
		tv.setPadding(48, 48, 48, 48);
		return tv;
	}

	public static OnlineViewFragment create() {
		return new OnlineViewFragment();
	}

	public void setSearchQuery(CharSequence query) {
	}

	public CharSequence getSearchQuery() {
		return "";
	}

	public void searchTracks(String query) {
	}

	public void searchRecommendations() {
	}

	public void searchDefaultTracks() {
	}
}
