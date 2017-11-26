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
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.ui.SpannedGridLayoutManager;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.mikepenz.itemanimators.ScaleUpAnimator;
import com.mikepenz.itemanimators.SlideDownAlphaAnimator;
import com.mikepenz.materialize.holder.StringHolder;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.umass.lastfm.Track;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

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
	private RecyclerView.LayoutManager layoutManager_c1;

	private Animation animation_press_c1;

	private void createItems(View v) {
		final List<Music> items = Music.getAllSortedByScore(6);

		if (items.size() <= 5)
			return;

		itemAdapter_c1 = new ItemAdapter<>();
		adapter_c1 = FastAdapter.with(Arrays.asList(itemAdapter_c1));

		animation_press_c1 = AnimationUtils.loadAnimation(getContext(), R.anim.shake);

		adapter_c1.withSelectable(true);
		adapter_c1.withOnClickListener(new OnClickListener<C1Item>() {
			@Override
			public boolean onClick(View v, IAdapter<C1Item> adapter, C1Item item, int position) {
				v.startAnimation(animation_press_c1);

				playItem(item.data.getPath());

				return true;
			}
		});

		recyclerView_c1 = v.findViewById(R.id.recyclerView_c1);
		recyclerView_c1.setHasFixedSize(true);
		recyclerView_c1.setItemViewCacheSize(7);
		recyclerView_c1.setDrawingCacheEnabled(true);
		recyclerView_c1.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		layoutManager_c1 = new SpannedGridLayoutManager(
				new SpannedGridLayoutManager.GridSpanLookup() {
					@Override
					public SpannedGridLayoutManager.SpanInfo getSpanInfo(int p) {
						int cs;
						int rs;

						if (p == 0) {
							cs = 16;
							rs = 8;
						} else if (p == 1) {
							cs = 6;
							rs = 8;
						} else {
							cs = 5;
							rs = 4;
						}

						return new SpannedGridLayoutManager.SpanInfo(cs, rs);
					}
				},
				16,
				1f);

		recyclerView_c1.setLayoutManager(layoutManager_c1);

		recyclerView_c1.setAdapter(adapter_c1);

		recyclerView_c1.setItemAnimator(new SlideDownAlphaAnimator());
		recyclerView_c1.getItemAnimator().setAddDuration(253);
		recyclerView_c1.getItemAnimator().setRemoveDuration(333);

		recyclerView_c1.postDelayed(new Runnable() {
			@Override
			public void run() {
				for (final Music item : items) {
					itemAdapter_c1.add(new C1Item().setAdapter(adapter_c1).setData(item));
				}
			}
		}, 1300);

		Analytics.getInstance().getTopTracksForLastfmForApp()
				.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
					@Override
					public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
						return Analytics.convertToLocal(tracks, 7);
					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<Collection<Music>>() {
					@Override
					public void accept(Collection<Music> r) throws Exception {
						itemAdapter_c1.clear();
						for (Music m : r) {
							itemAdapter_c1.add(new C1Item().setAdapter(adapter_c1).setData(m));
						}
					}
				});
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

			Bitmap bitmap = data.getCover(context, -1);
			if (bitmap != null) {
				viewHolder.image.setImageBitmap(bitmap);
			} else {
				viewHolder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_broken_image_black));
			}
			viewHolder.text.setText(data.getText(System.lineSeparator()));
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

	public static boolean shouldBeVisible() {
		if (!MusicService.IsPremium)
			return false;
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

	public static AnalyticsViewFragment create() {
		AnalyticsViewFragment f = new AnalyticsViewFragment();
		return f;
	}

}
