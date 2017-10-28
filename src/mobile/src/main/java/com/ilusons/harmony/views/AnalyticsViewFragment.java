package com.ilusons.harmony.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class AnalyticsViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = AnalyticsViewFragment.class.getSimpleName();

	private View root;

	private AVLoadingIndicatorView loading;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.analytics_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		loading = v.findViewById(R.id.loading);

		loading.smoothToShow();

		// Items
		createItems(v);

		// Charts
		createCharts(v);

		loading.smoothToHide();

		return v;
	}


	//region Charts

	private void createItems(View v) {
		final List<Music> items = Music.getAllSortedByScore(6);

		if (items.size() <= 5)
			return;

		ImageView image_0 = v.findViewById(R.id.image_0);
		ImageView image_1 = v.findViewById(R.id.image_1);
		ImageView image_2 = v.findViewById(R.id.image_2);
		ImageView image_3 = v.findViewById(R.id.image_3);
		ImageView image_4 = v.findViewById(R.id.image_4);
		ImageView image_5 = v.findViewById(R.id.image_5);

		image_0.setImageBitmap(items.get(0).getCover(getContext(), -1));
		image_1.setImageBitmap(items.get(1).getCover(getContext(), -1));
		image_2.setImageBitmap(items.get(2).getCover(getContext(), -1));
		image_3.setImageBitmap(items.get(3).getCover(getContext(), -1));
		image_4.setImageBitmap(items.get(4).getCover(getContext(), -1));
		image_5.setImageBitmap(items.get(5).getCover(getContext(), -1));

		image_0.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(0).getPath());
			}
		});
		image_1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(1).getPath());
			}
		});
		image_2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(2).getPath());
			}
		});
		image_3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(3).getPath());
			}
		});
		image_4.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(4).getPath());
			}
		});
		image_5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playItem(items.get(5).getPath());
			}
		});

	}

	//endregion

	//region Charts

	private void createCharts(View v) {
		PieChart chart = v.findViewById(R.id.analytics_charts_c1);

		final int N = 5;

		chart.setUsePercentValues(true);
		chart.getDescription().setEnabled(false);
		chart.setExtraOffsets(3, 3, 3, 3);
		chart.setDragDecelerationFrictionCoef(0.95f);

		chart.setDrawHoleEnabled(true);
		chart.setHoleColor(ContextCompat.getColor(getContext(), R.color.transparent));

		chart.setTransparentCircleColor(ContextCompat.getColor(getContext(), R.color.translucent_icons));
		chart.setTransparentCircleAlpha(110);

		chart.setHoleRadius(24f);
		chart.setTransparentCircleRadius(27f);

		chart.setRotationAngle(0);
		chart.setRotationEnabled(true);
		chart.setHighlightPerTapEnabled(true);

		chart.setDrawCenterText(true);
		chart.setCenterText("Top " + N);
		chart.setCenterTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
		chart.setCenterTextSize(16f);

		chart.setDrawEntryLabels(true);
		chart.setEntryLabelColor(ContextCompat.getColor(getContext(), R.color.primary_text));
		chart.setEntryLabelTextSize(10f);

		Legend l = chart.getLegend();
		l.setEnabled(false);

		List<Music> allSortedByScore = Music.getAllSortedByScore(N);
		float score_total = 0;
		for (Music one : allSortedByScore)
			score_total += one.getScore();

		ArrayList<Integer> colors = new ArrayList<Integer>();
		int defaultColor = ContextCompat.getColor(getContext(), R.color.accent);
		ArrayList<PieEntry> data = new ArrayList<>();
		for (Music one : allSortedByScore)
			try {
				Bitmap cover = one.getCover(getContext(), 64);

				data.add(new PieEntry(
						(float) (one.getScore() * 100f / score_total),
						one.getTitle(),
						new BitmapDrawable(getResources(), cover),
						one.getPath()));
				int color = defaultColor;
				try {
					Palette palette = Palette.from(cover).generate();
					int colorBackup = color;
					color = palette.getVibrantColor(color);
					if (color == colorBackup)
						color = palette.getDarkVibrantColor(color);
					if (color == colorBackup)
						color = palette.getDarkMutedColor(color);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				colors.add(ColorUtils.setAlphaComponent(color, 160));
			} catch (Exception e) {
				e.printStackTrace();
			}

		PieDataSet dataSet = new PieDataSet(data, "Top " + N);
		dataSet.setSliceSpace(1f);
		dataSet.setSelectionShift(5f);
		dataSet.setHighlightEnabled(true);
		dataSet.setDrawValues(true);
		dataSet.setDrawIcons(true);
		dataSet.setIconsOffset(new MPPointF(0, 42));
		dataSet.setColors(colors);

		PieData chartData = new PieData();
		chartData.addDataSet(dataSet);
		chartData.setDrawValues(true);
		chartData.setValueFormatter(new PercentFormatter());
		chartData.setValueTextSize(10f);
		chartData.setValueTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));

		chart.setData(chartData);
		chart.highlightValues(null);
		chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				playItem((String) e.getData());
			}

			@Override
			public void onNothingSelected() {

			}
		});
		chart.invalidate();

		chart.animateY(3600, Easing.EasingOption.EaseInOutQuad);
		// chart.spin(2000, 0, 360);

	}

	//endregion

	private void playItem(String path) {
		try {
			Intent i = new Intent(getContext(), MusicService.class);

			i.setAction(MusicService.ACTION_OPEN);
			i.putExtra(MusicService.KEY_URI, path);

			getContext().startService(i);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static AnalyticsViewFragment create() {
		AnalyticsViewFragment f = new AnalyticsViewFragment();
		return f;
	}

}
