package com.ilusons.harmony.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.itemanimators.SlideDownAlphaAnimator;
import com.mikepenz.materialize.holder.StringHolder;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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


	//region Items

	private FastAdapter<C1Item> adapter_c1;
	private ItemAdapter<C1Item> itemAdapter_c1;
	private RecyclerView recyclerView_c1;
	private FlexboxLayoutManager layoutManager_c1;

	private void createItems(View v) {
		final List<Music> items = Music.getAllSortedByScore(6);

		if (items.size() <= 5)
			return;

		itemAdapter_c1 = new ItemAdapter<>();
		adapter_c1 = FastAdapter.with(Arrays.asList(itemAdapter_c1));

		recyclerView_c1 = v.findViewById(R.id.recyclerView_c1);

		layoutManager_c1 = new FlexboxLayoutManager(getContext());

		layoutManager_c1.setFlexDirection(FlexDirection.ROW);
		layoutManager_c1.setFlexWrap(FlexWrap.WRAP);
		layoutManager_c1.setJustifyContent(JustifyContent.FLEX_START);
		layoutManager_c1.setAlignItems(AlignItems.STRETCH);
		layoutManager_c1.setAlignContent(AlignContent.STRETCH);

		recyclerView_c1.setLayoutManager(layoutManager_c1);

		recyclerView_c1.setAdapter(adapter_c1);

		recyclerView_c1.setItemAnimator(new SlideDownAlphaAnimator());
		recyclerView_c1.getItemAnimator().setAddDuration(253);
		recyclerView_c1.getItemAnimator().setRemoveDuration(333);

		for (final Music item : items) {
			recyclerView_c1.postDelayed(new Runnable() {
				@Override
				public void run() {
					itemAdapter_c1.add(new C1Item().setAdapter(adapter_c1).setData(item));
				}
			}, 333);
		}
	}

	public static class C1Item extends AbstractItem<C1Item, C1Item.ViewHolder> {
		private FastAdapter<C1Item> adapter;
		private Music data;

		public C1Item setAdapter(FastAdapter<C1Item> adapter) {
			this.adapter = adapter;

			return this;
		}

		public C1Item setData(Music data) {
			this.data = data;

			return this;
		}

		@Override
		public int getType() {
			return hashCode();
		}

		@Override
		public int getLayoutRes() {
			return R.layout.analytics_view_c1_item;
		}

		@Override
		public void bindView(ViewHolder viewHolder, List<Object> payloads) {
			super.bindView(viewHolder, payloads);

			Context context = viewHolder.view.getContext();

			viewHolder.image.setImageBitmap(data.getCover(context, -1));
			viewHolder.text.setText(data.getText());

			int position = adapter.getPosition(this);

			ViewGroup.LayoutParams lp = viewHolder.view.getLayoutParams();
			if (lp instanceof FlexboxLayoutManager.LayoutParams) {
				FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;

				flexboxLp.setOrder(1);
				flexboxLp.setFlexGrow(1.0f);
				flexboxLp.setFlexShrink(1.0f);
				flexboxLp.setAlignSelf(AlignSelf.FLEX_START);
				flexboxLp.setFlexBasisPercent(-1);
				flexboxLp.setMinWidth(AndroidEx.dpToPx(96));
				flexboxLp.setMinHeight(AndroidEx.dpToPx(96));
				flexboxLp.setWrapBefore(true);

				switch (position) {
					case 0:
						flexboxLp.setFlexGrow(3.0f);
						break;
					case 1:
						flexboxLp.setFlexGrow(2.0f);
						break;
				}

			}
		}

		@Override
		public void unbindView(ViewHolder holder) {
			super.unbindView(holder);

			holder.image.setImageDrawable(null);
			holder.text.setText(null);
		}

		@Override
		public ViewHolder getViewHolder(View v) {
			return new ViewHolder(v);
		}

		protected static class ViewHolder extends RecyclerView.ViewHolder {
			protected View view;

			protected ImageView image;
			protected TextView text;

			public ViewHolder(View v) {
				super(v);

				view = v;

				image = v.findViewById(R.id.image);
				text = v.findViewById(R.id.text);
			}
		}
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

	public static boolean shouldBeVisible() {
		try {
			double sum = 0;
			for (Music item : Music.getAllSortedByScore(6))
				sum += item.getScore();
			return sum >= 7;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
