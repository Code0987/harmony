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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.arasthel.spannedgridlayoutmanager.SpanLayoutParams;
import com.arasthel.spannedgridlayoutmanager.SpanSize;
import com.arasthel.spannedgridlayoutmanager.SpannedGridLayoutManager;
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
import com.ilusons.harmony.data.DB;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.JavaEx;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import de.umass.lastfm.Track;
import de.umass.lastfm.cache.Cache;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

public class AnalyticsViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = AnalyticsViewFragment.class.getSimpleName();

	private View root;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.analytics_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		// Items
		createItems(v);

		return v;
	}


	//region Items

	private WaveSwipeRefreshLayout swipeRefreshLayout;

	private FastAdapter adapter;
	private ItemAdapter<P1Item> itemAdapter_p1;
	private ItemAdapter<C1Item> itemAdapter_c1;

	private Animation animation_press;

	private void createItems(View v) {
		animation_press = AnimationUtils.loadAnimation(getContext(), R.anim.shake);

		itemAdapter_p1 = new ItemAdapter<>();
		itemAdapter_c1 = new ItemAdapter<>();

		adapter = FastAdapter.with(Arrays.asList(itemAdapter_p1, itemAdapter_c1));
		adapter.setHasStableIds(true);
		adapter.withSelectable(true);
		adapter.withOnClickListener(new OnClickListener() {
			@Override
			public boolean onClick(@NonNull View v, @NonNull IAdapter adapter, @NonNull IItem item, int position) {
				if (item instanceof C1Item) {
					v.startAnimation(animation_press);

					playItem(v.getContext(), ((C1Item) item).data.getPath());
				}
				return true;
			}
		});

		swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setColorSchemeColors(
				ContextCompat.getColor(v.getContext(), R.color.translucent_accent),
				ContextCompat.getColor(v.getContext(), android.R.color.holo_blue_bright),
				ContextCompat.getColor(v.getContext(), R.color.translucent_accent));
		swipeRefreshLayout.setWaveColor(ContextCompat.getColor(v.getContext(), android.R.color.holo_blue_bright));

		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

		RecyclerView.LayoutManager layoutManager = new SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, 16);
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setAdapter(adapter);

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				int topRowVerticalPosition =
						(recyclerView == null || recyclerView.getChildCount() == 0)
								? 0
								: recyclerView.getChildAt(0).getTop();
				swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
			}

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
			}
		});

		swipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				loadItems();
			}
		});

		loadItems();
	}

	public static class P1Item extends AbstractItem<P1Item, P1Item.ViewHolder> {
		private FastAdapter adapter;
		private Collection<Music> data;

		public P1Item setAdapter(FastAdapter adapter) {
			this.adapter = adapter;

			return this;
		}

		public P1Item setData(Collection<Music> data) {
			this.data = data;

			return this;
		}

		@Override
		public int getType() {
			return R.id.analytics_view_p1_item;
		}

		@Override
		public int getLayoutRes() {
			return R.layout.analytics_view_p1_item;
		}

		@Override
		public void bindView(@NonNull final ViewHolder viewHolder, @NonNull List<Object> payloads) {
			super.bindView(viewHolder, payloads);

			try {
				int p = viewHolder.getLayoutPosition();

				viewHolder.view.setLayoutParams(new SpanLayoutParams(new SpanSize(16, 16)));
			} catch (Exception e) {
				e.printStackTrace();
			}

			final Context context = viewHolder.view.getContext();

			final PieChart chart = viewHolder.pie_chart;

			final int N = 5;

			chart.setUsePercentValues(true);
			chart.getDescription().setEnabled(false);
			chart.setExtraOffsets(3, 3, 3, 3);
			chart.setDragDecelerationFrictionCoef(0.95f);

			chart.setDrawHoleEnabled(true);
			chart.setHoleColor(ContextCompat.getColor(context, R.color.transparent));

			chart.setTransparentCircleColor(ContextCompat.getColor(context, R.color.translucent_icons));
			chart.setTransparentCircleAlpha(110);

			chart.setHoleRadius(24f);
			chart.setTransparentCircleRadius(27f);

			chart.setRotationAngle(0);
			chart.setRotationEnabled(true);
			chart.setHighlightPerTapEnabled(true);

			chart.setDrawCenterText(true);
			chart.setCenterText("Local Top " + N);
			chart.setCenterTextColor(ContextCompat.getColor(context, R.color.primary_text));
			chart.setCenterTextSize(16f);

			chart.setDrawEntryLabels(true);
			chart.setEntryLabelColor(ContextCompat.getColor(context, R.color.primary_text));
			chart.setEntryLabelTextSize(10f);

			Legend l = chart.getLegend();
			l.setEnabled(false);

			float score_total = 0;
			for (Music one : data)
				score_total += one.getScore();

			ArrayList<Integer> colors = new ArrayList<Integer>();
			int defaultColor = ContextCompat.getColor(context, R.color.accent);
			ArrayList<PieEntry> data = new ArrayList<>();
			for (Music one : P1Item.this.data)
				try {
					Bitmap cover = one.getCover(context, 64);

					data.add(new PieEntry(
							(float) (one.getScore() * 100f / score_total),
							one.getTitle(),
							new BitmapDrawable(context.getResources(), cover),
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

			PieDataSet dataSet = new PieDataSet(data, "Local Top " + N);
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
			chartData.setValueTextColor(ContextCompat.getColor(context, R.color.primary_text));

			chart.setData(chartData);
			chart.highlightValues(null);
			chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
				@Override
				public void onValueSelected(Entry e, Highlight h) {
					playItem(context, (String) e.getData());
				}

				@Override
				public void onNothingSelected() {

				}
			});
			chart.invalidate();

			chart.animateY(3600, Easing.EasingOption.EaseInOutQuad);
			// chart.spin(2000, 0, 360);

		}

		@Override
		public void unbindView(ViewHolder holder) {
			super.unbindView(holder);

			holder.pie_chart.clear();
		}

		@Override
		public ViewHolder getViewHolder(View v) {
			return new ViewHolder(v);
		}

		protected static class ViewHolder extends RecyclerView.ViewHolder {
			protected View view;

			protected PieChart pie_chart;

			public ViewHolder(View v) {
				super(v);

				view = v;

				pie_chart = v.findViewById(R.id.pie_chart);
			}
		}
	}

	public static class C1Item extends AbstractItem<C1Item, C1Item.ViewHolder> {
		private FastAdapter adapter;
		private Music data;

		public C1Item setAdapter(FastAdapter adapter) {
			this.adapter = adapter;

			return this;
		}

		public C1Item setData(Music data) {
			this.data = data;

			return this;
		}

		@Override
		public int getType() {
			return R.id.analytics_view_c1_item;
		}

		@Override
		public int getLayoutRes() {
			return R.layout.analytics_view_c1_item;
		}

		@Override
		public void bindView(@NonNull final ViewHolder viewHolder, @NonNull List<Object> payloads) {
			super.bindView(viewHolder, payloads);

			try {
				int p = viewHolder.getLayoutPosition() - 1;

				int cs;
				int rs;

				if (p == 0) {
					cs = 16;
					rs = 10;
				} else if (p == 1 || p == 5) {
					cs = 6;
					rs = 10;
				} else if (p == 2 || p == 3 || p == 4 || p == 6) {
					cs = 10;
					rs = 5;
				} else {
					cs = 8;
					rs = 5;
				}

				viewHolder.view.setLayoutParams(new SpanLayoutParams(new SpanSize(cs, rs)));
			} catch (Exception e) {
				e.printStackTrace();
			}

			final Context context = viewHolder.view.getContext();

			Bitmap bitmap = data.getCover(context, -1);
			if (bitmap != null) {
				viewHolder.image.setImageBitmap(bitmap);
			} else {
				viewHolder.image.setImageDrawable(null);

				try {
					(new ArtworkEx.ArtworkDownloaderAsyncTask(
							context,
							data.getText(),
							ArtworkEx.ArtworkType.Song,
							-1,
							data.getPath(),
							Music.KEY_CACHE_DIR_COVER,
							data.getPath(),
							new JavaEx.ActionT<Bitmap>() {
								@Override
								public void execute(Bitmap bitmap) {
									try {
										if (bitmap == null) {
											(new ArtworkEx.ArtworkDownloaderAsyncTask(
													context,
													data.getArtist(),
													ArtworkEx.ArtworkType.Artist,
													-1,
													data.getPath(),
													Music.KEY_CACHE_DIR_COVER,
													data.getPath(),
													new JavaEx.ActionT<Bitmap>() {
														@Override
														public void execute(Bitmap bitmap) {
															try {
																viewHolder.image.setImageBitmap(bitmap);
															} catch (Exception e) {
																e.printStackTrace();
															}
														}
													},
													new JavaEx.ActionT<Exception>() {
														@Override
														public void execute(Exception e) {
															Log.w(TAG, e);
														}
													},
													3000,
													true))
													.execute();
										} else {
											viewHolder.image.setImageBitmap(bitmap);
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							},
							new JavaEx.ActionT<Exception>() {
								@Override
								public void execute(Exception e) {
									Log.w(TAG, e);
								}
							},
							3000,
							true))
							.execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			viewHolder.text1.setText(data.getTitle());
			viewHolder.text2.setText(data.getArtist());
		}

		@Override
		public void unbindView(ViewHolder holder) {
			super.unbindView(holder);

			holder.image.setImageDrawable(null);
			holder.text1.setText(null);
			holder.text2.setText(null);
		}

		@Override
		public ViewHolder getViewHolder(View v) {
			return new ViewHolder(v);
		}

		protected static class ViewHolder extends RecyclerView.ViewHolder {
			protected View view;

			protected ImageView image;
			protected TextView text1;
			protected TextView text2;

			public ViewHolder(View v) {
				super(v);

				view = v;

				image = v.findViewById(R.id.image);
				text1 = v.findViewById(R.id.text1);
				text2 = v.findViewById(R.id.text2);
			}
		}
	}

	private void loadItems() {
		final int N = 14;
		final Context context = getContext();

		swipeRefreshLayout.setRefreshing(true);

		Analytics.getInstance().getTopTracksForLastfmForApp()
				.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
					@Override
					public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
						return Analytics.convertToLocal(context, tracks, N);
					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<Collection<Music>>() {
					@Override
					public void onSubscribe(Disposable d) {

					}

					@Override
					public void onNext(Collection<Music> r) {
						try {
							itemAdapter_c1.clear();
							for (Music m : r) {
								itemAdapter_c1.add(new C1Item().setAdapter(adapter).setData(m));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable e) {
						swipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onComplete() {
						try {
							itemAdapter_p1.clear();
							itemAdapter_p1.add(new P1Item().setAdapter(adapter).setData(Music.getAllSortedByScore(5)));
						} catch (Exception e) {
							e.printStackTrace();
						}

						swipeRefreshLayout.setRefreshing(false);
					}
				});
	}

	//endregion

	private static void playItem(Context context, String path) {
		try {
			Intent i = new Intent(context, MusicService.class);

			i.setAction(MusicService.ACTION_OPEN);
			i.putExtra(MusicService.KEY_URI, path);

			context.startService(i);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static AnalyticsViewFragment create() {
		AnalyticsViewFragment f = new AnalyticsViewFragment();
		return f;
	}

}
