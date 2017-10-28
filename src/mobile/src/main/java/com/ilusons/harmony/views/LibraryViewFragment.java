package com.ilusons.harmony.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.widget.ImageButton;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;

public class LibraryViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = LibraryViewFragment.class.getSimpleName();

	private View root;

	private AVLoadingIndicatorView loading;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.library_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		loading = v.findViewById(R.id.loading);

		loading.smoothToShow();

		createSearch(v);

		loading.smoothToHide();

		return v;
	}

	public static LibraryViewFragment create() {
		LibraryViewFragment f = new LibraryViewFragment();
		return f;
	}

	//region Search

	private FloatingActionButton fab_search;
	private Toolbar search_toolbar;
	private ImageButton search_close;

	private void createSearch(View v) {

		fab_search = v.findViewById(R.id.fab_search);

		search_toolbar = v.findViewById(R.id.search_toolbar);

		fab_search.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (search_toolbar.getVisibility() != View.VISIBLE) {
					search_toolbar.setAlpha(0);
					search_toolbar.setVisibility(View.VISIBLE);
					search_toolbar.animate().alpha(1).setDuration(283).start();

					fab_search.animate().alpha(0).setDuration(333).start();
				} else {
					search_toolbar.setAlpha(1);
					search_toolbar.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
						@Override
						public void run() {
							search_toolbar.setVisibility(View.INVISIBLE);
						}
					}).start();

					fab_search.animate().alpha(1).setDuration(283).start();
				}
			}
		});

		search_close = v.findViewById(R.id.search_close);

		search_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				search_toolbar.setAlpha(1);
				search_toolbar.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
					@Override
					public void run() {
						search_toolbar.setVisibility(View.INVISIBLE);
					}
				}).start();

				fab_search.animate().alpha(1).setDuration(283).start();
			}
		});

	}

	//endregion

	//region Menu

	private FloatingActionButton fab_menu;
	private View menu;
	private ImageButton menu_close;

	private void createMenu(View v) {

		fab_menu = v.findViewById(R.id.fab_menu);

		menu = v.findViewById(R.id.toolbar);

		fab_menu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (menu.getVisibility() != View.VISIBLE) {
					menu.setAlpha(0);
					menu.setVisibility(View.VISIBLE);
					menu.animate().alpha(1).setDuration(283).start();

					fab_menu.animate().alpha(0).setDuration(333).start();
				} else {
					menu.setAlpha(1);
					menu.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
						@Override
						public void run() {
							menu.setVisibility(View.INVISIBLE);
						}
					}).start();

					fab_menu.animate().alpha(1).setDuration(283).start();
				}
			}
		});

		menu_close = v.findViewById(R.id.menu_close);

		menu_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				menu.setAlpha(1);
				menu.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
					@Override
					public void run() {
						menu.setVisibility(View.INVISIBLE);
					}
				}).start();

				fab_menu.animate().alpha(1).setDuration(283).start();
			}
		});

	}

	//endregion

}
