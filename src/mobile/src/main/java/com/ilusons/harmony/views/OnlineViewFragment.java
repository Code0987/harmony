package com.ilusons.harmony.views;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ViewEx;
import com.ilusons.harmony.ref.ui.ParallaxImageView;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import de.umass.lastfm.Track;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator;
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class OnlineViewFragment extends BaseUIFragment {

	// Logger TAG
	private static final String TAG = OnlineViewFragment.class.getSimpleName();

	private View root;

	private AVLoadingIndicatorView loading;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.online_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		loading = v.findViewById(R.id.loading);

		loading.smoothToShow();

		// Items
		createItems(v);

		loading.smoothToHide();

		v.postDelayed(new Runnable() {
			@Override
			public void run() {
				searchDefaultTracks();
			}
		}, 1500);

		return v;
	}

	private SearchView searchView;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();

		inflater.inflate(R.menu.online_view_menu, menu);

		ViewEx.tintMenuIcons(menu, ContextCompat.getColor(getContext(), R.color.textColorPrimary));

		MenuItem search = menu.findItem(R.id.search);
		searchView = (SearchView) search.getActionView();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				searchTracks(query);

				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {

				return true;
			}
		});
		AndroidEx.trimChildMargins(searchView);
		try {
			SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
			if (searchManager != null) {
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getBaseUIActivity().getComponentName()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			LinearLayout searchEditFrame = searchView.findViewById(R.id.search_edit_frame);
			((LinearLayout.LayoutParams) searchEditFrame.getLayoutParams()).leftMargin = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}

		MenuItem now_playing = menu.findItem(R.id.now_playing);
		now_playing.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				Intent intent = new Intent(getContext(), PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);

				return true;
			}
		});

		MenuItem downloads = menu.findItem(R.id.downloads);
		downloads.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					toggleDownloads();

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem refresh = menu.findItem(R.id.refresh);
		refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					loadOnlinePlaylistTracks(true);

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem my_recommendations = menu.findItem(R.id.my_recommendations);
		my_recommendations.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					searchRecommendations();

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem save_as_smart_playlist = menu.findItem(R.id.save_as_smart_playlist);
		save_as_smart_playlist.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					if (adapter.getAll(Music.class).size() == 0) {
						info("Search something first or maybe try out my recommendations!");

						throw new Exception();
					}

					try {
						final EditText editText = new EditText(getContext());

						new android.app.AlertDialog.Builder(getContext())
								.setTitle("Create new smart playlist")
								.setMessage("Enter name for new smart playlist or old to overwrite ...")
								.setView(editText)
								.setPositiveButton("Create", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										try {
											String name = editText.getText().toString().trim();

											Playlist playlist = Playlist.loadOrCreatePlaylist(name, new JavaEx.ActionT<Playlist>() {
												@Override
												public void execute(Playlist playlist) {
													playlist.addAll(adapter.getAll(Music.class));
													playlist.setQuery(String.valueOf(getSearchQuery()));
												}
											});

											if (playlist != null) {
												Playlist.setActivePlaylist(getContext(), name, true);
												info("Playlist created!");
											} else
												throw new Exception("Some error.");
										} catch (Exception e) {
											e.printStackTrace();

											info("Playlist creation failed!");
										}
									}
								})
								.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
									}
								})
								.show();
					} catch (Exception e) {
						e.printStackTrace();
					}

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

	}

	//region Items

	private RecyclerViewAdapter adapter;

	private RecyclerView.OnScrollListener recyclerViewScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			for (int i = 0; i < recyclerView.getChildCount(); i++) {
				RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
				if (viewHolder instanceof RecyclerViewAdapter.ViewHolder) {
					RecyclerViewAdapter.ViewHolder vh = ((RecyclerViewAdapter.ViewHolder) viewHolder);
					if (vh.parallaxCover != null)
						vh.parallaxCover.translate();
				}
			}
		}
	};

	private void createItems(View v) {
		adapter = new RecyclerViewAdapter(this);
		adapter.setHasStableIds(true);

		final RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 7);

		recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));

		recyclerView.setAdapter(adapter);

		recyclerView.addOnScrollListener(recyclerViewScrollListener);

		// Animations

		VerticalOverScrollBounceEffectDecorator overScroll = new VerticalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView), 1.5f, 1f, -0.5f);
	}

	private void searchTracks(String query) {
		if (TextUtils.isEmpty(query)) {
			searchDefaultTracks();
			return;
		}

		final int N = 50;
		final Context context = getContext();

		if (AndroidEx.isNetworkAvailable(context)) {
			Analytics.findTracks(query, N)
					.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
						@Override
						public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
							return Analytics.convertToLocal(context, tracks, N, false);
						}
					})
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(new Observer<Collection<Music>>() {
						@Override
						public void onSubscribe(Disposable d) {
							loading.smoothToShow();
						}

						@Override
						public void onNext(Collection<Music> r) {
							try {
								adapter.clear();
								for (Music m : r) {
									adapter.add(m);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onError(Throwable e) {
							loadOnlinePlaylistTracks(true);

							loading.smoothToHide();
						}

						@Override
						public void onComplete() {
							loading.smoothToHide();
						}
					});
		} else {
			loadOnlinePlaylistTracks(true);

			info("Turn on your internet for new music.");
		}
	}

	private static final String TAG_SPREF_ONLINE_LAST_DEFAULT_SCAN_TS = ".online_last_default_scan_ts";
	private static final long ONLINE_DEFAULT_SCAN_INTERVAL = 23 * 60 * 60 * 1000;

	private void searchDefaultTracks() {
		try {
			final Context context = getContext();

			boolean updateRequired = true;

			long last = SPrefEx.get(context).getLong(TAG_SPREF_ONLINE_LAST_DEFAULT_SCAN_TS, 0);
			long dt = ((last + ONLINE_DEFAULT_SCAN_INTERVAL) - System.currentTimeMillis());

			if (dt > 0) {
				updateRequired = false;
			}

			updateRequired &= AndroidEx.isNetworkAvailable(context);

			if (updateRequired) {
				loading.smoothToShow();

				info("Getting new tracks. Stand by ...");

				adapter.clear();

				Observer<Collection<Music>> observer = new Observer<Collection<Music>>() {
					@Override
					public void onSubscribe(Disposable d) {
					}

					@Override
					public void onNext(Collection<Music> r) {
						try {
							for (Music m : r) {
								adapter.add(m);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable e) {
						loading.smoothToHide();

						loadOnlinePlaylistTracks(true);
					}

					@Override
					public void onComplete() {
						loading.smoothToHide();

						try {
							SPrefEx.get(context).edit().putLong(TAG_SPREF_ONLINE_LAST_DEFAULT_SCAN_TS, System.currentTimeMillis()).apply();
						} catch (Exception e) {
							e.printStackTrace();
						}

						info("New tracks are available now! More tracks tomorrow :)");
					}
				};

				ArrayList<io.reactivex.Observable<Collection<Track>>> observables = new ArrayList<>();

				try {
					List<Music> topLocalTracks = Music.getAllSortedByScore(10);
					Collections.shuffle(topLocalTracks);
					topLocalTracks = topLocalTracks.subList(0, Math.min(topLocalTracks.size() - 1, 3));
					for (Music music : topLocalTracks) {
						observables.add(Analytics.findSimilarTracks(music.getArtist(), music.getTitle(), 16));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					observables.add(Analytics.getTopTracksForLastfm(getContext()));
					Collections.shuffle(observables);

				} catch (Exception e) {
					e.printStackTrace();
				}

				io.reactivex.Observable
						.concat(observables)
						.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
							@Override
							public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
								return Analytics.convertToLocal(context, tracks, 50, false);
							}
						})
						.observeOn(AndroidSchedulers.mainThread())
						.subscribeOn(Schedulers.io())
						.subscribe(observer);
			} else {
				loadOnlinePlaylistTracks(true);

				info("Turn on your internet for new music. Loading previous tracks.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void searchRecommendations() {
		final int N = 16;
		final Context context = getContext();

		if (AndroidEx.isNetworkAvailable(context)) {
			loading.smoothToShow();

			info("Finding what you may like :) Stand by ...");

			adapter.clear();

			Observer<Collection<Music>> observer = new Observer<Collection<Music>>() {
				@Override
				public void onSubscribe(Disposable d) {
				}

				@Override
				public void onNext(Collection<Music> r) {
					try {
						for (Music m : r) {
							adapter.add(m);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onError(Throwable e) {
					loading.smoothToHide();

					info(":( Please try later. Nothing for you right now!");
				}

				@Override
				public void onComplete() {
					loading.smoothToHide();

					info(":) Your recommendations are ready! Save it, it may change later!");
				}
			};

			ArrayList<io.reactivex.Observable<Collection<Track>>> observables = new ArrayList<>();

			List<Music> topLocalTracks = Music.getAllSortedByScore(10);
			Collections.shuffle(topLocalTracks);
			topLocalTracks = topLocalTracks.subList(0, Math.min(topLocalTracks.size() - 1, 3));
			for (Music music : topLocalTracks) {
				observables.add(Analytics.findSimilarTracks(music.getArtist(), music.getTitle(), N));
			}

			io.reactivex.Observable
					.concat(observables)
					.flatMap(new Function<Collection<Track>, ObservableSource<Collection<Music>>>() {
						@Override
						public ObservableSource<Collection<Music>> apply(Collection<Track> tracks) throws Exception {
							return Analytics.convertToLocal(context, tracks, N, false);
						}
					})
					.observeOn(AndroidSchedulers.mainThread())
					.subscribeOn(Schedulers.io())
					.subscribe(observer);
		} else {
			loadOnlinePlaylistTracks(true);

			info("Turn on your internet please!");
		}
	}

	private void loadOnlinePlaylistTracks(boolean reset) {
		loading.smoothToShow();

		try {
			if (reset)
				adapter.clear(Music.class);

			Playlist playlist = Playlist.loadOrCreatePlaylist(Playlist.KEY_PLAYLIST_ONLINE);
			if (playlist != null) {
				for (Music item : playlist.getItems()) {
					adapter.add(item);
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		adapter.notifyDataSetChanged();

		loading.smoothToHide();
	}

	public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final OnlineViewFragment fragment;

		private final PlaylistViewFragment.PlaylistItemUIStyle style;

		private ArrayList<Object> data;

		public RecyclerViewAdapter(OnlineViewFragment fragment) {
			this.fragment = fragment;

			style = PlaylistViewFragment.getPlaylistItemUIStyle(fragment.getContext());

			data = new ArrayList<>();
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			int layoutId = -1;
			switch (style) {
				case Card1:
					layoutId = R.layout.playlist_view_item_card1;
					break;

				case Card2:
					layoutId = R.layout.playlist_view_item_card2;
					break;

				case Simple:
					layoutId = R.layout.playlist_view_item_simple;
					break;

				case Default:
				default:
					layoutId = R.layout.playlist_view_item_default;
					break;
			}

			View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);

			return new ViewHolder(fragment, v);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			final int itemType = getItemViewType(position);

			try {
				((ViewHolder) holder)
						.bind(position, (Music) data.get(position));
			} catch (Exception e) {
				e.printStackTrace();
			}
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

		public <T> Collection<T> getAll(Class<T> tClass) {
			final Collection<T> r = new ArrayList<>();
			for (Object item : data)
				if (item.getClass().equals(tClass))
					r.add((T) item);
			return r;
		}

		protected static class ViewHolder extends RecyclerView.ViewHolder {
			private final OnlineViewFragment fragment;

			protected View view;

			public ImageView cover;
			public ParallaxImageView parallaxCover;
			protected TextView title;
			protected TextView info;

			public ViewHolder(final OnlineViewFragment fragment, View v) {
				super(v);

				this.fragment = fragment;

				view = v;

				cover = v.findViewById(R.id.cover);
				title = v.findViewById(R.id.title);
				info = v.findViewById(R.id.info);

				if (cover != null) {
					cover.setMaxHeight(AndroidEx.dpToPx(196));
					if (cover instanceof ParallaxImageView) {
						parallaxCover = (ParallaxImageView) cover;
						parallaxCover.setListener(new ParallaxImageView.ParallaxImageListener() {
							@Override
							public int[] getValuesForTranslate() {
								if (itemView.getParent() == null) {
									return null;
								} else {
									int[] itemPosition = new int[2];
									itemView.getLocationOnScreen(itemPosition);

									int[] recyclerPosition = new int[2];
									((RecyclerView) itemView.getParent()).getLocationOnScreen(recyclerPosition);

									return new int[]{
											itemPosition[1],
											((RecyclerView) itemView.getParent()).getMeasuredHeight(),
											recyclerPosition[1]
									};
								}
							}
						});
					}
				}

			}

			@SuppressLint("StaticFieldLeak")
			public void bind(int p, final Music d) {
				final Context context = view.getContext();

				if (cover != null) {
					cover.setImageBitmap(null);
					final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
						@Override
						public void accept(Bitmap bitmap) throws Exception {
							try {
								TransitionDrawable d = new TransitionDrawable(new Drawable[]{
										cover.getDrawable(),
										new BitmapDrawable(view.getContext().getResources(), bitmap)
								});

								cover.setImageDrawable(d);

								d.setCrossFadeEnabled(true);
								d.startTransition(200);

								if (parallaxCover != null)
									parallaxCover.translate();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					final Consumer<Throwable> throwableConsumer = new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {
							// Pass
						}
					};
					final Consumer<Throwable> throwableConsumerWithRetry = new Consumer<Throwable>() {
						@Override
						public void accept(Throwable throwable) throws Exception {
							Music
									.loadLocalOrSearchCoverArtFromItunes(
											context,
											d,
											d.getCoverPath(context),
											d.getText(),
											false,
											ImageEx.ItunesImageType.Artist)
									.observeOn(AndroidSchedulers.mainThread())
									.subscribeOn(Schedulers.computation())
									.subscribe(
											resultConsumer,
											throwableConsumer);
						}
					};

					Music
							.loadLocalOrSearchCoverArtFromItunes(
									context,
									d,
									d.getCoverPath(context),
									d.getText(),
									false,
									ImageEx.ItunesImageType.Song)
							.observeOn(AndroidSchedulers.mainThread())
							.subscribeOn(Schedulers.computation())
							.subscribe(
									resultConsumer,
									throwableConsumerWithRetry);
				}

				title.setText(d.getTitle());
				info.setText(d.getTextExtraOnlySingleLine());

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), R.style.AppTheme_AlertDialogStyle));
						builder.setTitle("Select?");
						builder.setItems(new CharSequence[]{
								"Play now",
								"Play next",
								"Download, then play",
								"Download",
								"Stream",
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int itemIndex) {
								try {
									switch (itemIndex) {
										case 0:
											playNow(d);
											break;
										case 1:
											playNext(d);
											break;
										case 2:
											playAfterDownload(d);
											break;
										case 3:
											download(d);
											break;
										case 4:
											playAfterStream(d);
											break;
									}
								} catch (Exception e) {
									Log.w(TAG, e);
								}
							}
						});
						android.app.AlertDialog dialog = builder.create();
						dialog.show();
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(final View view) {
						view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.shake));

						playNow(d);

						return true;
					}
				});
				view.setLongClickable(true);
			}

			private void playNow(final Music music) {
				final MusicService musicService = fragment.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.open(music);
				} catch (Exception e) {
					e.printStackTrace();

					fragment.info("Ah! Try again!");
				}
			}

			private void playNext(final Music music) {
				final MusicService musicService = fragment.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.getPlaylist().add(music, musicService.getPlaylist().getItemIndex() + 1);

					fragment.info("Added!");
				} catch (Exception e) {
					e.printStackTrace();

					fragment.info("Ah! Try again!");
				}
			}

			private void playAfterStream(final Music music) {
				final MusicService musicService = fragment.getMusicService();
				if (musicService == null)
					return;

				try {
					if (MusicService.getPlayerType(view.getContext()) == MusicService.PlayerType.AndroidOS) {
						musicService.stream(music);
					} else {
						fragment.info("Streaming is only supported in [" + MusicService.PlayerType.AndroidOS.getFriendlyName() + "] player. You can change it from Settings.");
					}
				} catch (Exception e) {
					e.printStackTrace();

					fragment.info("Ah! Try again!");
				}
			}

			private void playAfterDownload(final Music music) {
				final MusicService musicService = fragment.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.download(music);
				} catch (Exception e) {
					e.printStackTrace();

					fragment.info("Ah! Try again!");
				}
			}

			private void download(final Music music) {
				final MusicService musicService = fragment.getMusicService();
				if (musicService == null)
					return;

				try {
					musicService.download(music, false);
				} catch (Exception e) {
					e.printStackTrace();

					fragment.info("Ah! Try again!");
				}
			}

		}

	}

	//endregion

	//region Search

	public void setSearchQuery(CharSequence q) {
		if (TextUtils.isEmpty(q))
			q = "";

		if (searchView != null && !searchView.getQuery().equals(q))
			searchView.setQuery(q, true);
	}

	public CharSequence getSearchQuery() {
		return searchView != null ? searchView.getQuery() : "";
	}

	//endregion

	//region Downloads

	private void toggleDownloads() {
		try {
			if (getMusicService() == null) {
				info("IO service is not running!");
				return;
			}

			View v = ((LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.online_view_downloads, null);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
			builder.setView(v);

			createDownloads(v);

			AlertDialog alert = builder.create();

			alert.requestWindowFeature(DialogFragment.STYLE_NO_TITLE);

			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private DownloadsRecyclerViewAdapter downloadsRecyclerViewAdapter;

	private Runnable downloadsUpdater;

	private void createDownloads(final View v) {
		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(1);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		downloadsRecyclerViewAdapter = new DownloadsRecyclerViewAdapter();
		recyclerView.setAdapter(downloadsRecyclerViewAdapter);

		downloadsUpdater = new Runnable() {
			@Override
			public void run() {
				downloadsRecyclerViewAdapter.refresh();

				if (downloadsUpdater != null)
					v.postDelayed(downloadsUpdater, 2500);
			}
		};
		v.postDelayed(downloadsUpdater, 500);

	}

	public class DownloadsRecyclerViewAdapter extends RecyclerView.Adapter<DownloadsRecyclerViewAdapter.ViewHolder> {

		private final ArrayList<MusicService.AudioDownload> data;

		public DownloadsRecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.online_view_downloads_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final MusicService.AudioDownload d = data.get(position);
			final View v = holder.view;

			holder.progress.setIndeterminate(true);
			holder.text.setText(d.Music.getText());
			if (d.Download != null) {
				holder.progress.setIndeterminate(false);
				holder.progress.setProgress(d.Download.getProgress());
				holder.info.setText((new StringBuilder())
						.append(d.Download.getStatus())
						.append(" ")
						.append(d.Download.getProgress())
						.append("%")
						.toString());
			} else {
				holder.info.setText(R.string.waiting);
			}
			holder.stop.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (getMusicService() == null)
						return;

					if (d.Download == null)
						return;

					getMusicService().cancelDownload(d.Download.getId());
				}
			});

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (d.Download.getProgress() == 100)
						getMusicService().open(d.Music);
					else
						info("Not completed yet!");
				}
			});

		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ProgressBar progress;
			public TextView text;
			public TextView info;
			public ImageView stop;

			public ViewHolder(View v) {
				super(v);

				view = v;

				progress = v.findViewById(R.id.progress);
				text = v.findViewById(R.id.text);
				info = v.findViewById(R.id.info);
				stop = v.findViewById(R.id.stop);
			}
		}

		public void refresh() {
			if (getMusicService() == null)
				return;

			data.clear();

			data.addAll(getMusicService().getAudioDownloads());

			notifyDataSetChanged();
		}

	}

	//endregion

	public static OnlineViewFragment create() {
		OnlineViewFragment f = new OnlineViewFragment();
		return f;
	}

}
