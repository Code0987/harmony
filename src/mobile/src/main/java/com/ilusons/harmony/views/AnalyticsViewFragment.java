package com.ilusons.harmony.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.JavaEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;

import de.umass.lastfm.Track;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
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

	private RecyclerViewAdapter adapter;

	private void createItems(View v) {
		adapter = new RecyclerViewAdapter();
		adapter.setHasStableIds(true);

		swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setColorSchemeColors(
				ContextCompat.getColor(v.getContext(), R.color.accent),
				ContextCompat.getColor(v.getContext(), android.R.color.holo_blue_bright),
				ContextCompat.getColor(v.getContext(), R.color.accent));
		swipeRefreshLayout.setWaveColor(ContextCompat.getColor(v.getContext(), android.R.color.holo_blue_bright));

		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

		GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 16) {
			@Override
			public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
				try {
					super.onLayoutChildren(recycler, state);
				} catch (IndexOutOfBoundsException e) {
					Log.w(TAG, e);
				}
			}
		};
		layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int p) {
				int cs;

				if (p == 0) {
					cs = 16;
				} else if (p == 1) {
					cs = 6;
				} else if (p == 2) {
					cs = 10;
				} else {
					cs = 16;
				}

				return cs;
			}
		});
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setAdapter(adapter);

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
							adapter.clear(Music.class);
							for (Music m : r) {
								adapter.add(m);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable e) {
						try {
							adapter.clear(Collection.class);
							adapter.add(Music.getAllSortedByScore(5));
						} catch (Exception e2) {
							e2.printStackTrace();
						}

						adapter.notifyDataSetChanged();

						swipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onComplete() {
						try {
							adapter.clear(Collection.class);
							adapter.add(Music.getAllSortedByScore(5));
						} catch (Exception e) {
							e.printStackTrace();
						}

						adapter.notifyDataSetChanged();

						swipeRefreshLayout.setRefreshing(false);
					}
				});
	}

	public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private ArrayList<Object> data;

		public static final int ITEM_TYPE_P1 = 1;
		public static final int ITEM_TYPE_C1 = 0;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			if (viewType == ITEM_TYPE_P1) {
				View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.analytics_view_p1_item, parent, false);

				return new P1ViewHolder(v);
			} else if (viewType == ITEM_TYPE_C1) {
				View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.analytics_view_c1_item, parent, false);

				return new C1ViewHolder(v);
			}

			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

			final int itemType = getItemViewType(position);

			try {
				if (itemType == ITEM_TYPE_P1) {
					((P1ViewHolder) holder)
							.bind(position, (List<Music>) data.get(position));
				} else if (itemType == ITEM_TYPE_C1) {
					((C1ViewHolder) holder)
							.bind(position, (Music) data.get(position));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (data.get(position) instanceof List) {
				return ITEM_TYPE_P1;
			} else if (data.get(position) instanceof Music) {
				return ITEM_TYPE_C1;
			}

			return -1;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void add(Object item) {
			data.add(item);

			notifyDataSetChanged();
		}

		public void clear() {
			data.clear();

			notifyDataSetChanged();
		}

		public <T> void clear(Class<T> tClass) {
			final Collection<Object> copy = new ArrayList<>(data);
			for (Object item : copy)
				if (item.getClass().equals(tClass))
					data.remove(item);

			notifyDataSetChanged();
		}

		protected static class P1ViewHolder extends RecyclerView.ViewHolder {
			protected View view;

			protected PieChart pie_chart;

			public P1ViewHolder(View v) {
				super(v);

				view = v;

				pie_chart = v.findViewById(R.id.pie_chart);
			}

			public void bind(int p, Collection<Music> d) {
				final Context context = view.getContext();

				final PieChart chart = pie_chart;

				final int N = 5;

				chart.setUsePercentValues(true);
				chart.getDescription().setEnabled(false);
				chart.setExtraOffsets(3, 3, 3, 3);
				chart.setDragDecelerationFrictionCoef(0.95f);

				chart.setDrawHoleEnabled(true);
				chart.setHoleColor(ContextCompat.getColor(context, R.color.transparent));

				chart.setTransparentCircleColor(ContextCompat.getColor(context, R.color.icons));
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
				for (Music one : d)
					score_total += one.getScore();

				ArrayList<Integer> colors = new ArrayList<Integer>();
				int defaultColor = ContextCompat.getColor(context, R.color.accent);
				ArrayList<PieEntry> data = new ArrayList<>();
				for (Music one : d)
					try {
						Bitmap cover = one.getCover(context, 64);

						data.add(new PieEntry(
								(float) (one.getScore() * 100f / score_total),
								one.getTitle(),
								new BitmapDrawable(context.getResources(), cover),
								one.getPath()));
						int color = defaultColor;
						if (cover != null)
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
		}

		protected static class C1ViewHolder extends RecyclerView.ViewHolder {
			protected View view;

			protected ImageView image;
			protected TextView text1;
			protected TextView text2;

			public C1ViewHolder(View v) {
				super(v);

				view = v;

				image = v.findViewById(R.id.image);
				text1 = v.findViewById(R.id.text1);
				text2 = v.findViewById(R.id.text2);
			}

			@SuppressLint("StaticFieldLeak")
			public void bind(int p, final Music d) {
				final Context context = view.getContext();

				image.setImageBitmap(null);
				final int coverSize = Math.max(image.getWidth(), image.getHeight());
				(new AsyncTask<Void, Void, Bitmap>() {
					@Override
					protected Bitmap doInBackground(Void... voids) {
						try {
							Bitmap bitmap = d.getCover(view.getContext(), coverSize);

							if (bitmap == null) {
								(new ArtworkEx.ArtworkDownloaderAsyncTask(
										context,
										d.getText(),
										ArtworkEx.ArtworkType.Song,
										-1,
										d.getPath(),
										Music.KEY_CACHE_DIR_COVER,
										d.getPath(),
										new JavaEx.ActionT<Bitmap>() {
											@Override
											public void execute(Bitmap r) {
												try {
													if (r == null) {
														(new ArtworkEx.ArtworkDownloaderAsyncTask(
																context,
																d.getArtist(),
																ArtworkEx.ArtworkType.Artist,
																-1,
																d.getPath(),
																Music.KEY_CACHE_DIR_COVER,
																d.getPath(),
																new JavaEx.ActionT<Bitmap>() {
																	@Override
																	public void execute(Bitmap r2) {
																		try {
																			if (r2 == null) {
																				r2 = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.logo)).getBitmap();
																				Music.putCover(context, d, r2);
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

								bitmap = d.getCover(view.getContext(), coverSize);
							}

							return bitmap;
						} catch (Exception e) {
							e.printStackTrace();
						}

						return null;
					}

					@Override
					protected void onPostExecute(Bitmap bitmap) {
						try {
							TransitionDrawable d = new TransitionDrawable(new Drawable[]{
									image.getDrawable(),
									new BitmapDrawable(view.getContext().getResources(), bitmap)
							});

							image.setImageDrawable(d);

							d.setCrossFadeEnabled(true);
							d.startTransition(200);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).execute();

				text1.setText(d.getTitle());
				text2.setText(d.getArtist());

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.shake));

						playItem(view.getContext(), d.getPath());
					}
				});
			}
		}

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
