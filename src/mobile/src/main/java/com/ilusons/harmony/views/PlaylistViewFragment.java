package com.ilusons.harmony.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.support.v7.widget.SearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.ref.ViewEx;
import com.ilusons.harmony.ref.ui.ParallaxImageView;
import com.turingtechnologies.materialscrollbar.ICustomAdapter;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import jonathanfinerty.once.Once;
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator;
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class PlaylistViewFragment extends BaseUIFragment {

	// Logger TAG
	private static final String TAG = PlaylistViewFragment.class.getSimpleName();

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
		View v = inflater.inflate(R.layout.playlist_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		loading = v.findViewById(R.id.loading);

		loading.smoothToShow();

		createItems(v);

		createPlaylists(v);

		setSearchQuery("");

		loading.smoothToHide();

		return v;
	}

	private SearchView searchView;

	@Override
	public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();

		inflater.inflate(R.menu.playlist_view_menu, menu);

		ViewEx.tintMenuIcons(menu, ContextCompat.getColor(getContext(), R.color.textColorPrimary));

		MenuItem search = menu.findItem(R.id.search);
		searchView = (SearchView) search.getActionView();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				if (adapter != null)
					adapter.refresh(query);

				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (adapter != null)
					adapter.refresh(newText);

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

		MenuItem jump = menu.findItem(R.id.jump);
		jump.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					getAdapter().jumpToCurrentlyPlayingItem();

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem refresh = menu.findItem(R.id.refresh);
		refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@SuppressLint("CheckResult")
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					if (getViewPlaylist().isSmart()) {
						loading.smoothToShow();

						info("Smart playlist update started. This can take a while!");

						Playlist.updateSmart(getContext(), getViewPlaylist())
								.subscribeOn(Schedulers.io())
								.observeOn(AndroidSchedulers.mainThread())
								.subscribe(new Consumer<Playlist>() {
									@Override
									public void accept(Playlist playlist) throws Exception {
										setViewPlaylist(playlist);

										info("Smart playlist updated!");

										loading.smoothToHide();
									}
								}, new Consumer<Throwable>() {
									@Override
									public void accept(Throwable throwable) throws Exception {
										info("Smart playlist updated failed :(");

										loading.smoothToHide();
									}
								});
					} else {
						adapter.refresh(getSearchQuery());
					}

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem playlist_settings = menu.findItem(R.id.playlist_settings);
		playlist_settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				try {
					togglePlaylistSettings(getActivity().findViewById(android.R.id.content));

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});

		MenuItem playlist_view_sort_mode = menu.findItem(R.id.playlist_view_sort_mode);
		playlist_view_sort_mode.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				if (menuItem.getActionView() != null)
					menuItem.getActionView().performClick();

				return true;
			}
		});
		Spinner playlist_view_sort_mode_spinner = (Spinner) playlist_view_sort_mode.getActionView();
		playlist_view_sort_mode_spinner.setBackground(null);
		playlist_view_sort_mode_spinner.setGravity(Gravity.CENTER);
		playlist_view_sort_mode_spinner.setDropDownVerticalOffset(AndroidEx.dpToPx(36));
		playlist_view_sort_mode_spinner.setDropDownHorizontalOffset(AndroidEx.dpToPx(360));
		createUISortMode(playlist_view_sort_mode_spinner);

	}

	@SuppressLint("CheckResult")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_EXPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "w");
					FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

					boolean result = true;

					//noinspection ConstantConditions
					String base = (new File(StorageEx.getPath(getContext(), uri))).getParent();
					String nl = System.getProperty("line.separator");
					StringBuilder sb = new StringBuilder();
					for (Music music : getViewPlaylist().getItems()) {
						String url = IOEx.getRelativePath(base, music.getPath());
						sb.append(url).append(nl);
					}

					try {
						IOUtils.write(sb.toString(), fileOutputStream, "utf-8");
					} catch (Exception e) {
						result = false;

						e.printStackTrace();
					}

					if (result)
						info("Current playlist exported!");
					else
						info("Export failed!", true);

					fileOutputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} else if (requestCode == REQUEST_PLAYLIST_ADD_PICK && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				try {
					final ArrayList<android.util.Pair<String, Uri>> newScanItems = new ArrayList<>();

					ClipData clipData = data.getClipData();
					if (clipData != null) {
						for (int i = 0; i < clipData.getItemCount(); i++)
							try {
								String path = StorageEx.getPath(getContext(), clipData.getItemAt(i).getUri());
								if (path != null)
									newScanItems.add(android.util.Pair.create(path, (Uri) null));
							} catch (Exception e) {
								e.printStackTrace();
							}
					} else {
						try {
							String path = StorageEx.getPath(getContext(), data.getData());
							if (path != null)
								newScanItems.add(android.util.Pair.create(path, (Uri) null));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					final Playlist playlist = getViewPlaylist();

					Playlist.update(
							getContext(),
							playlist,
							newScanItems,
							false,
							true,
							new JavaEx.ActionExT<String>() {
								@Override
								public void execute(String s) throws Exception {
									info(playlist.getName() + "@..." + s.substring(Math.max(0, Math.min(s.length() - 34, s.length()))), false);
								}
							})
							.subscribe(new Consumer<Playlist>() {
								@Override
								public void accept(Playlist playlist) throws Exception {
									info("Added all items.");
								}
							}, new Consumer<Throwable>() {
								@Override
								public void accept(Throwable throwable) throws Exception {
									info("Can't add selected items.");
								}
							});

					Playlist.savePlaylist(playlist);

					setViewPlaylist(playlist);

					try {
						if (Playlist.getActivePlaylist(getContext()).equals(playlist.getName())) {
							Intent musicServiceIntent = new Intent(getContext(), MusicService.class);
							musicServiceIntent.setAction(MusicService.ACTION_PLAYLIST_CHANGED);
							musicServiceIntent.putExtra(MusicService.KEY_PLAYLIST_CHANGED_PLAYLIST, playlist.getName());
							getContext().startService(musicServiceIntent);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					info("Playlist updated!");
				} catch (Exception e) {
					e.printStackTrace();

					info("Unable to add selected items to the playlist!");
				}

			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void OnMusicServicePrepared() {
		if (recyclerViewScrollListener != null) try {
			recyclerViewScrollListener.onScrolled(recyclerView, 0, 0);
		} catch (Exception e) {
			// Eat?
		}
	}

	//region Items

	private RecyclerViewAdapter adapter;

	private RecyclerViewExpandableItemManager recyclerViewExpandableItemManager;

	private static final int RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW = 5;

	private RecyclerView recyclerView;

	private RecyclerView.OnScrollListener recyclerViewScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			for (int i = 0; i < recyclerView.getChildCount(); i++) {
				RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

				if (viewHolder instanceof GroupViewHolder) {
					GroupViewHolder vh = (GroupViewHolder) viewHolder;
					if (vh.parallaxCover != null)
						vh.parallaxCover.translate();
				}

				if (viewHolder instanceof ViewHolder) {
					ViewHolder vh = (ViewHolder) viewHolder;
					if (vh.parallaxCover != null)
						vh.parallaxCover.translate();

					if (vh.active_indicator_layout != null)
						adapter.updateActiveIndicator(getMusicService(), vh.active_indicator_layout);
				}
			}
		}
	};

	private FastScrollLayout fastScrollLayout;

	public RecyclerViewAdapter getAdapter() {
		return adapter;
	}

	private void createItems(View v) {

		// Set recycler
		recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()) {
			@Override
			public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
				try {
					super.onLayoutChildren(recycler, state);
				} catch (IndexOutOfBoundsException e) {
					Log.w(TAG, e);
				}
			}
		});
		recyclerView.addOnScrollListener(recyclerViewScrollListener);

		recyclerViewExpandableItemManager = new RecyclerViewExpandableItemManager(null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new RecyclerViewAdapter();
		recyclerView.setAdapter(recyclerViewExpandableItemManager.createWrappedAdapter(adapter));
		((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
		recyclerViewExpandableItemManager.attachRecyclerView(recyclerView);
		recyclerViewExpandableItemManager.setDefaultGroupsExpandedState(true);

		fastScrollLayout = v.findViewById(R.id.fastScrollLayout);
		fastScrollLayout.setRecyclerView(recyclerView);

		// Animations

		VerticalOverScrollBounceEffectDecorator overScroll = new VerticalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView), 2f, 1f, -0.5f);
	}

	public static class FastScrollLayout extends LinearLayout {
		private static final int HANDLE_HIDE_DELAY = 1300;
		private static final int HANDLE_ANIMATION_DURATION = 100;
		private static final int TRACK_SNAP_RANGE = 48;
		private static final String SCALE_X = "scaleX";
		private static final String SCALE_Y = "scaleY";
		private static final String ALPHA = "alpha";

		private View bubble;
		private View handle;
		private TextView text;

		private RecyclerView recyclerView;

		private final HandleHider handleHider = new HandleHider();
		private final ScrollListener scrollListener = new ScrollListener();
		private int width;
		private int height;

		private AnimatorSet currentAnimator = null;

		public FastScrollLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
			initialise(context);
		}

		public FastScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
			initialise(context);
		}

		private void initialise(Context context) {
			setOrientation(HORIZONTAL);
			setClipChildren(false);
			LayoutInflater inflater = LayoutInflater.from(context);
			inflater.inflate(R.layout.playlist_view_fast_scroll, this);
			bubble = findViewById(R.id.bubble);
			handle = findViewById(R.id.handle);
			text = findViewById(R.id.text);

			handle.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					hideHandle();
				}
			});
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			width = w;
			height = h;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if ((width - (bubble.getWidth() + TRACK_SNAP_RANGE)) > event.getX()) {
				return super.onTouchEvent(event);
			}

			if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
				setPosition(event.getY());
				if (currentAnimator != null) {
					currentAnimator.cancel();
				}
				getHandler().removeCallbacks(handleHider);
				if (handle.getVisibility() == INVISIBLE) {
					showHandle();
				}
				setRecyclerViewPosition(event.getY());
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				getHandler().postDelayed(handleHider, HANDLE_HIDE_DELAY);
				return true;
			}
			return super.onTouchEvent(event);
		}

		public void setRecyclerView(RecyclerView recyclerView) {
			this.recyclerView = recyclerView;
			recyclerView.setOnScrollListener(scrollListener);
		}

		private void setRecyclerViewPosition(float y) {
			if (recyclerView != null) {
				int itemCount = recyclerView.getAdapter().getItemCount();
				float proportion;
				if (bubble.getY() == 0) {
					proportion = 0f;
				} else if (bubble.getY() + bubble.getHeight() >= height - TRACK_SNAP_RANGE) {
					proportion = 1f;
				} else {
					proportion = y / (float) height;
				}
				int targetPos = getValueInRange(0, itemCount - 1, (int) (proportion * (float) itemCount));
				recyclerView.scrollToPosition(targetPos);
			}
		}

		private int getValueInRange(int min, int max, int value) {
			int minimum = Math.max(min, value);
			return Math.min(minimum, max);
		}

		private void setPosition(float y) {
			float position = y / height;
			int bubbleHeight = bubble.getHeight();
			bubble.setY(getValueInRange(0, height - bubbleHeight, (int) ((height - bubbleHeight) * position)));
			int handleHeight = handle.getHeight();
			handle.setY(getValueInRange(0, height - handleHeight, (int) ((height - handleHeight) * position)));

			if (recyclerView != null) {
				int n = recyclerView.getChildCount();

				int p = getValueInRange(0, n - 1, (int) ((y / (float) height) * (float) n));

				try {
					Object tag = recyclerView.getLayoutManager().getChildAt(p).getTag();
					if (tag instanceof String)
						text.setText((String) tag);
					else
						text.setText("");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void showHandle() {
			AnimatorSet animatorSet = new AnimatorSet();
			handle.setPivotX(handle.getWidth());
			handle.setPivotY(handle.getHeight());
			handle.setVisibility(VISIBLE);
			Animator growerX = ObjectAnimator.ofFloat(handle, SCALE_X, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
			Animator growerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
			Animator alpha = ObjectAnimator.ofFloat(handle, ALPHA, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
			animatorSet.playTogether(growerX, growerY, alpha);
			animatorSet.start();
		}

		private void hideHandle() {
			currentAnimator = new AnimatorSet();
			handle.setPivotX(handle.getWidth());
			handle.setPivotY(handle.getHeight());
			Animator shrinkerX = ObjectAnimator.ofFloat(handle, SCALE_X, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
			Animator shrinkerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
			Animator alpha = ObjectAnimator.ofFloat(handle, ALPHA, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
			currentAnimator.playTogether(shrinkerX, shrinkerY, alpha);
			currentAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					handle.setVisibility(INVISIBLE);
					currentAnimator = null;
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					super.onAnimationCancel(animation);
					handle.setVisibility(INVISIBLE);
					currentAnimator = null;
				}
			});
			currentAnimator.start();
		}

		private class HandleHider implements Runnable {
			@Override
			public void run() {
				hideHandle();
			}
		}

		private class ScrollListener extends RecyclerView.OnScrollListener {
			@Override
			public void onScrolled(RecyclerView rv, int dx, int dy) {
				View firstVisibleView = recyclerView.getChildAt(0);
				int firstVisiblePosition = recyclerView.getChildPosition(firstVisibleView);
				int visibleRange = recyclerView.getChildCount();
				int lastVisiblePosition = firstVisiblePosition + visibleRange;
				int itemCount = recyclerView.getAdapter().getItemCount();
				int position;
				if (firstVisiblePosition == 0) {
					position = 0;
				} else if (lastVisiblePosition == itemCount - 1) {
					position = itemCount - 1;
				} else {
					position = firstVisiblePosition;
				}
				float proportion = (float) position / (float) itemCount;
				setPosition(height * proportion);
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

	//region View

	public class RecyclerViewAdapter extends AbstractExpandableItemAdapter<GroupViewHolder, ViewHolder> implements ICustomAdapter {

		private final List<Music> data;
		private final List<Pair<String, List<Object>>> dataFiltered;

		private final PlaylistItemUIStyle style;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
			dataFiltered = new ArrayList<>();

			style = getPlaylistItemUIStyle(getContext());

			setHasStableIds(true);
		}

		@Override
		public int getGroupCount() {
			return dataFiltered.size();
		}

		@Override
		public int getChildCount(int groupPosition) {
			return dataFiltered.get(groupPosition).second.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition + childPosition;
		}

		@Override
		public GroupViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			int layoutId = -1;
			switch (style) {
				case Simple:
					layoutId = R.layout.playlist_view_group_simple;
					break;

				case Default:
				default:
					layoutId = R.layout.playlist_view_group_default;
					break;
			}

			View view = inflater.inflate(layoutId, parent, false);

			return new GroupViewHolder(view);
		}

		@Override
		public ViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

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

			View view = inflater.inflate(layoutId, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindGroupViewHolder(GroupViewHolder holder, int groupPosition, int viewType) {
			try {
				final String d = dataFiltered.get(groupPosition).first;

				holder.view.setTag(d);

				holder.title.setText(d);

				final ImageView cover = holder.cover;
				if (cover != null) {
					cover.setImageBitmap(null);
					if (!TextUtils.isEmpty(d) && d.length() > 5) {
						ImageEx.ItunesImageType imageType;
						String q = d;

						switch (getUIGroupMode(getContext())) {
							case Album:
								imageType = ImageEx.ItunesImageType.Album;
								break;
							case Artist:
								imageType = ImageEx.ItunesImageType.Artist;
								break;
							case Genre:
								imageType = ImageEx.ItunesImageType.None;
								break;
							case Year:
								imageType = ImageEx.ItunesImageType.None;
								break;
							case Default:
							default:
								imageType = ImageEx.ItunesImageType.Song;
								break;
						}

						final Context context = holder.view.getContext();

						final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
							@Override
							public void accept(Bitmap bitmap) throws Exception {
								try {
									TransitionDrawable d = new TransitionDrawable(new Drawable[]{
											cover.getDrawable(),
											new BitmapDrawable(context.getResources(), bitmap)
									});

									cover.setImageDrawable(d);

									d.setCrossFadeEnabled(true);
									d.startTransition(200);

									if (cover instanceof ParallaxImageView)
										((ParallaxImageView) cover).translate();
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
						if (imageType != ImageEx.ItunesImageType.None) {
							Music
									.loadLocalOrSearchCoverArtFromItunes(
											context,
											null,
											null,
											q,
											false,
											imageType)
									.observeOn(AndroidSchedulers.mainThread())
									.subscribeOn(Schedulers.computation())
									.subscribe(
											resultConsumer,
											throwableConsumer);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@SuppressLint("StaticFieldLeak")
		@Override
		public void onBindChildViewHolder(final ViewHolder holder, int groupPosition, int childPosition, int viewType) {
			final Object d = dataFiltered.get(groupPosition).second.get(childPosition);

			// Bind data to view here!
			if (d instanceof Music) {

				final Music item = (Music) d;

				holder.view.setTag(item.getText());
				holder.active_indicator_layout.setTag(item.getPath());

				final ImageView cover = holder.cover;
				if (cover != null) {
					cover.setImageBitmap(null);

					final Context context = holder.view.getContext();

					final Consumer<Bitmap> resultConsumer = new Consumer<Bitmap>() {
						@Override
						public void accept(Bitmap bitmap) throws Exception {
							try {
								TransitionDrawable d = new TransitionDrawable(new Drawable[]{
										cover.getDrawable(),
										new BitmapDrawable(context.getResources(), bitmap)
								});

								cover.setImageDrawable(d);

								d.setCrossFadeEnabled(true);
								d.startTransition(200);

								if (cover instanceof ParallaxImageView)
									((ParallaxImageView) cover).translate();
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
											item,
											item.getCoverPath(context),
											item.getText(),
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
									item,
									item.getCoverPath(context),
									item.getText(),
									false,
									ImageEx.ItunesImageType.Song)
							.observeOn(AndroidSchedulers.mainThread())
							.subscribeOn(Schedulers.computation())
							.subscribe(
									resultConsumer,
									throwableConsumerWithRetry);
				}

				updateActiveIndicator(getMusicService(), holder.active_indicator_layout);

				if (holder.title != null)
					holder.title.setText(item.getTitle());

				if (holder.artist != null)
					holder.artist.setText(item.getArtist());

				if (holder.album != null)
					holder.album.setText(item.getAlbum());

				if (holder.info != null) {
					String s;
					try {
						s = item.getTextExtraOnlySingleLine(getMusicService().getPlaylist().getItems().lastIndexOf(item));
					} catch (Exception e) {
						e.printStackTrace();

						s = item.getTextExtraOnlySingleLine();
					}
					holder.info.setText(s);
					holder.info.setHorizontallyScrolling(true);
					holder.info.setSelected(true);
					holder.info.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							TextView tv = (TextView) v;
							if (!hasFocus && tv != null) {
								tv.setSelected(true);
							}
						}
					});
				}

				holder.view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						getMusicService().open(item.getPath());
					}
				});

				holder.view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view) {
						if (viewPlaylist == null) {
							info("Open a playlist into view first!");
							return true;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AppTheme_AlertDialogStyle));
						builder.setTitle("Select the action");
						builder.setItems(new CharSequence[]{
								"Share",
								"Tags",
								"Play next",
								"Play at start",
								"Play at last",
								"Remove",
								"Clear (except current)",
								"Move down",
								"Move up",
								"Delete (physically)",
								"Download"
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int itemIndex) {
								try {
									switch (itemIndex) {
										case 0:
											Intent shareIntent = new Intent();
											shareIntent.setAction(Intent.ACTION_SEND);
											shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTextDetailedMultiLine());
											shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(item.getPath())));
											if (item.getPath().toLowerCase().endsWith("mp3"))
												shareIntent.setType("audio/mp3");
											else if (item.hasVideo())
												shareIntent.setType("video/*");
											else
												shareIntent.setType("audio/*");
											shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
											startActivity(Intent.createChooser(shareIntent, "Share " + item.getText() + " ..."));
											break;
										case 1: {
											final EditText editText = new EditText(getActivity());
											editText.setText(item.getTags());
											(new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AppTheme_AlertDialogStyle))
													.setTitle("Tags")
													.setView(editText)
													.setCancelable(true)
													.setPositiveButton("OK", new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialogInterface, int i) {
															item.setTags(editText.getText().toString().trim());

															item.update();

															setFromPlaylist(-1L, viewPlaylist.getName());

															dialogInterface.dismiss();
														}
													}))
													.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialogInterface, int i) {
															dialogInterface.dismiss();
														}
													})
													.show();
										}
										break;
										case 2:
											viewPlaylist.add(item, viewPlaylist.getItems().lastIndexOf(item) + 1);
											updatePlaylist(viewPlaylist);
											break;
										case 3:
											viewPlaylist.add(item, 0);
											updatePlaylist(viewPlaylist);
											break;
										case 4:
											viewPlaylist.add(item, viewPlaylist.getItems().size());
											updatePlaylist(viewPlaylist);
											break;
										case 5:
											viewPlaylist.remove(item);
											updatePlaylist(viewPlaylist);
											break;
										case 6:
											viewPlaylist.removeAllExceptCurrent();
											updatePlaylist(viewPlaylist);
											break;
										case 7:
											viewPlaylist.moveDown(item);
											updatePlaylist(viewPlaylist);
											break;
										case 8:
											viewPlaylist.moveUp(item);
											updatePlaylist(viewPlaylist);
											break;
										case 9:
											viewPlaylist.delete(item, getMusicService(), true);
											break;
										case 10:
											download(item);
											break;
									}

									adapter.setData(viewPlaylist.getItems());
								} catch (Exception e) {
									Log.w(TAG, e);
								}
							}
						});
						AlertDialog dialog = builder.create();
						dialog.show();

						final String tag_pl_cstm = TAG + ".pl_cstm";
						if (!Once.beenDone(Once.THIS_APP_VERSION, tag_pl_cstm)) {
							(new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AppTheme_AlertDialogStyle))
									.setTitle("Playlist customization")
									.setMessage("Next, Ok, will open a list of actions to allow you to customize current playlist or Now playing playlist. If you have enabled sorting, you won't see changes, just set it to Default/Custom. Example: Add will add the selected music to current playlist from visible playlist.")
									.setCancelable(false)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {
											dialogInterface.dismiss();
										}
									}))
									.show();

							Once.markDone(tag_pl_cstm);
						}

						return true;
					}
				});

			}

		}

		@Override
		public boolean onCheckCanExpandOrCollapseGroup(GroupViewHolder holder, int groupPosition, int x, int y, boolean expand) {
			return true;
		}

		@Override
		public boolean getInitialGroupExpandedState(int groupPosition) {
			final PlaylistViewActivity.PlaylistViewTab playlistViewTab = PlaylistViewActivity.getPlaylistViewTab(getContext());

			boolean r = true;

			switch (playlistViewTab) {
				case Albums:
				case Artists:
					r = false;
					break;
				default:
					r = true;
					break;
			}

			return r;
		}

		public void setData(Collection<Music> d) {
			data.clear();
			data.addAll(d);

			refresh(String.valueOf(getSearchQuery()));
		}

		public void resetData() {
			ArrayList<Music> oldData = new ArrayList<>(data);

			data.clear();
			data.addAll(oldData);

			refresh(String.valueOf(getSearchQuery()));
		}

		public void removeData(Music d) {
			data.remove(d);

			refresh(String.valueOf(getSearchQuery()));
		}

		public void refresh(final CharSequence q) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {

						(getActivity()).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								loading.smoothToShow();
							}
						});

						// Sort
						List<Music> sorted = UISort(data);

						// Group
						Map<String, List<Music>> grouped = UIGroup(sorted);

						// Apply
						dataFiltered.clear();
						for (Map.Entry<String, List<Music>> entry : grouped.entrySet())
							dataFiltered.add(new Pair<String, List<Object>>(entry.getKey(), new ArrayList<Object>(entry.getValue())));

						(getActivity()).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								loading.smoothToHide();

								notifyDataSetChanged();
							}
						});

					} catch (Exception e) {
						Log.w(TAG, e);
					}
				}
			}).start();
		}

		public void bringInToView(int position) {
			try {
				int delta = Math.abs(recyclerView.getScrollY() - recyclerView.getLayoutManager().getChildAt(0).getHeight() * position);

				if (delta < recyclerView.getMeasuredHeight() * 5)
					recyclerView.smoothScrollToPosition(position);
				else
					recyclerView.scrollToPosition(position);

				View v = recyclerView.getLayoutManager().getChildAt(position);

				View active_indicator_layout = v.findViewById(R.id.active_indicator_layout);

				updateActiveIndicator(getMusicService(), active_indicator_layout);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void jumpToCurrentlyPlayingItem() {
			try {
				if (getMusicService().getMusic() == null)
					return;

				int pg = -1;
				int pc = -1;
				String current = getMusicService().getMusic().getPath();
				for (int gi = 0; gi < dataFiltered.size(); gi++) {
					Pair<String, List<Object>> group = dataFiltered.get(gi);
					for (int ci = 0; ci < group.second.size(); ci++) {
						Music child = (Music) group.second.get(ci);
						if (child != null && child.getPath().equals(current)) {
							pg = gi;
							pc = ci;
							break;
						}
					}
				}
				long ppc = RecyclerViewExpandableItemManager.getPackedPositionForChild(pg, pc);
				int k = recyclerViewExpandableItemManager.getFlatPosition(ppc);
				bringInToView(k);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String getCustomStringForElement(int element) {
			try {
				View v = recyclerView.getLayoutManager().getChildAt(element);

				TextView info = v.findViewById(R.id.info);
				return info.getText().toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public String getSectionName(int position) {
			try {
				View v = recyclerView.getLayoutManager().getChildAt(position);

				TextView info = v.findViewById(R.id.info);
				return info.getText().toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public boolean isImageSet(ImageView iv) {
			Drawable drawable = iv.getDrawable();
			BitmapDrawable bitmapDrawable = drawable instanceof BitmapDrawable ? (BitmapDrawable) drawable : null;
			TransitionDrawable transitionDrawable = drawable instanceof TransitionDrawable ? (TransitionDrawable) drawable : null;

			return (bitmapDrawable != null && bitmapDrawable.getBitmap() != null)
					||
					(transitionDrawable != null && transitionDrawable.getDrawable(1) != null);
		}

		private void updateActiveIndicator(final MusicService musicService, final View active_indicator_layout) {
			try {
				if (active_indicator_layout != null && active_indicator_layout.getTag() != null) {
					if (getMusicService().getMusic().getPath().equals(active_indicator_layout.getTag().toString())) {
						active_indicator_layout.setBackground(active_indicator_layout.getContext().getDrawable(R.drawable.bg2));
					} else {
						active_indicator_layout.setBackground(null);
					}
				}
			} catch (Exception e) {
				// Eat?
			}
		}

		private void download(final Music music) {
			final MusicService musicService = getMusicService();
			if (musicService == null)
				return;

			try {
				musicService.download(music, false);
			} catch (Exception e) {
				e.printStackTrace();

				info("Ah! Try again!");
			}
		}

	}

	public class GroupViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public TextView title;
		public ImageView cover;
		public ParallaxImageView parallaxCover;

		public GroupViewHolder(View view) {
			super(view);

			this.view = view;

			title = view.findViewById(R.id.title);
			cover = view.findViewById(R.id.cover);
			if (cover instanceof ParallaxImageView)
				parallaxCover = (ParallaxImageView) cover;

			if (parallaxCover != null) {
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

	public class ViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public View active_indicator_layout;
		public TextView title;
		public TextView album;
		public TextView artist;
		public TextView info;
		public ImageView cover;
		public ParallaxImageView parallaxCover;

		public ViewHolder(View view) {
			super(view);

			this.view = view;

			active_indicator_layout = view.findViewById(R.id.active_indicator_layout);
			title = view.findViewById(R.id.title);
			album = view.findViewById(R.id.album);
			artist = view.findViewById(R.id.artist);
			info = view.findViewById(R.id.info);
			cover = view.findViewById(R.id.cover);
			if (cover instanceof ParallaxImageView)
				parallaxCover = (ParallaxImageView) cover;

			if (parallaxCover != null) {
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

	//endregion

	//region Playlists

	private Playlist viewPlaylist;

	public Playlist getViewPlaylist() {
		return viewPlaylist;
	}

	public void setViewPlaylist(Playlist playlist) {
		boolean set = false;
		if (viewPlaylist == null)
			set = true;

		viewPlaylist = playlist;

		adapter.setData(viewPlaylist.getItems());

		if (set)
			recyclerView.postDelayed(new Runnable() {
				@Override
				public void run() {
					adapter.jumpToCurrentlyPlayingItem();
				}
			}, 1000);
	}

	public void updatePlaylist(Playlist playlist) {
		if (viewPlaylist == null)
			return;

		try {
			Playlist.savePlaylist(playlist);

			if (Playlist.getActivePlaylist(getContext()).equals(playlist.getName())) {
				Intent intent = new Intent(getContext(), MusicService.class);
				intent.setAction(MusicService.ACTION_PLAYLIST_CHANGED);
				intent.putExtra(MusicService.KEY_PLAYLIST_CHANGED_PLAYLIST, playlist.getName());
				getContext().startService(intent);
			}
		} catch (Exception e) {
			// Eat ?
		}
	}

	private void createPlaylists(View v) {

		setFromPlaylist(-1L, Playlist.getActivePlaylist(getContext()));

	}

	private static class SetFromPlaylistAsyncTask extends AsyncTask<Object, Object, Object> {
		private String playlistName;
		private Long playlistId;
		private WeakReference<PlaylistViewFragment> contextRef;

		public SetFromPlaylistAsyncTask(PlaylistViewFragment context, String playlistName, Long playlistId) {
			this.contextRef = new WeakReference<PlaylistViewFragment>(context);
			this.playlistName = playlistName;
			this.playlistId = playlistId;
		}

		@Override
		protected Object doInBackground(Object... objects) {
			final PlaylistViewFragment context = contextRef.get();
			if (context == null)
				return null;

			Playlist.setActivePlaylist(
					context.getContext(),
					playlistName,
					playlistId,
					new JavaEx.ActionT<Collection<Music>>() {
						@Override
						public void execute(Collection<Music> data) {
							if (data.size() % RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW == 0)
								context.adapter.setData(data);
						}
					},
					new JavaEx.ActionT<Playlist>() {
						@Override
						public void execute(final Playlist playlist) {
							try {
								context.viewPlaylist = playlist;

								context.getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										try {
											context.adapter.setData(playlist.getItems());
											context.recyclerView.postDelayed(new Runnable() {
												@Override
												public void run() {
													context.adapter.jumpToCurrentlyPlayingItem();
												}
											}, 1000);
											context.loading.smoothToHide();
											context.info("Loaded playlist!");
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					},
					new JavaEx.ActionT<Exception>() {
						@Override
						public void execute(Exception e) {
							try {
								context.getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										try {
											context.loading.smoothToHide();
											context.info("Playlist load failed!");
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
							} catch (Exception e2) {
								e2.printStackTrace();
							}
						}
					},
					false);

			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			final PlaylistViewFragment context = contextRef.get();
			if (context == null)
				return;

			try {
				context.loading.smoothToShow();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);

			final PlaylistViewFragment context = contextRef.get();
			if (context == null)
				return;

			try {
				context.loading.smoothToHide();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static AsyncTask<Object, Object, Object> setFromPlaylistAsyncTask = null;

	public void setFromPlaylist(Long playlistId, final String playlistName) {
		if (setFromPlaylistAsyncTask != null) {
			setFromPlaylistAsyncTask.cancel(true);
			try {
				setFromPlaylistAsyncTask.get(1, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			setFromPlaylistAsyncTask = null;
		}
		setFromPlaylistAsyncTask = new SetFromPlaylistAsyncTask(this, playlistName, playlistId);
		setFromPlaylistAsyncTask.execute();
	}

	//endregion

	//region Playlist settings

	private static final int REQUEST_EXPORT_LOCATION_PICK_SAF = 59;
	private static final int REQUEST_PLAYLIST_ADD_PICK = 564;

	private PlaylistsRecyclerViewAdapter playlistsRecyclerViewAdapter;

	private void createPlaylistsSettings(View v) {
		v.findViewById(R.id.new_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					final EditText editText = new EditText(getContext());

					new AlertDialog.Builder(getContext())
							.setTitle("Create new playlist")
							.setMessage("Enter name for new playlist ...")
							.setView(editText)
							.setPositiveButton("Create", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									try {
										String name = editText.getText().toString().trim();

										Playlist playlist = Playlist.loadOrCreatePlaylist(name);

										if (playlist != null) {
											Playlist.setActivePlaylist(getContext(), name, true);
											setViewPlaylist(playlist);
											playlistsRecyclerViewAdapter.refresh();
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
			}
		});

		v.findViewById(R.id.add_to_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Intent i = new Intent();
					String[] mimes = new String[]{"audio/*", "video/*"};
					i.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
					i.setType(StringUtils.join(mimes, '|'));
					i.setAction(Intent.ACTION_OPEN_DOCUMENT);
					i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					startActivityForResult(i, REQUEST_PLAYLIST_ADD_PICK);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		v.findViewById(R.id.save_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					Playlist.savePlaylist(getViewPlaylist());

					info("Playlist updated!");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		v.findViewById(R.id.export_playlist).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.putExtra(Intent.EXTRA_TITLE, "Playlist.m3u");
				intent.setType("*/*");
				if (intent.resolveActivity(getContext().getPackageManager()) != null) {
					startActivityForResult(intent, REQUEST_EXPORT_LOCATION_PICK_SAF);
				} else {
					info("SAF not found!");
				}
			}
		});

		// Set playlist(s)
		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(1);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		playlistsRecyclerViewAdapter = new PlaylistsRecyclerViewAdapter();
		recyclerView.setAdapter(playlistsRecyclerViewAdapter);
		playlistsRecyclerViewAdapter.refresh();

	}

	public class PlaylistsRecyclerViewAdapter extends RecyclerView.Adapter<PlaylistsRecyclerViewAdapter.ViewHolder> {

		private final ArrayList<android.util.Pair<Long, String>> data;
		private String dataActive;

		public PlaylistsRecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.playlist_settings_playlist_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final android.util.Pair<Long, String> d = data.get(position);
			final View v = holder.view;

			v.setTag(d.first);

			TextView text = (TextView) v.findViewById(R.id.text);
			text.setText(d.second);

			ImageView menu = v.findViewById(R.id.menu);

			int c;
			if (!TextUtils.isEmpty(dataActive) && dataActive.equals(d.second)) {
				c = ContextCompat.getColor(getContext(), android.R.color.holo_green_light);
			} else {
				c = ContextCompat.getColor(getContext(), R.color.icons);
			}
			text.setTextColor(c);
			menu.setColorFilter(c, PorterDuff.Mode.SRC_ATOP);

			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AppTheme_AlertDialogStyle));
					builder.setTitle("Are you sure?");
					builder.setMessage("This will replace the visible playlist with this one.");
					builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							setFromPlaylist(d.first, d.second);
							refresh();

							dialog.dismiss();
						}
					});
					builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			};
			text.setOnClickListener(onClickListener);

			menu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AppTheme_AlertDialogStyle));
					builder.setTitle("Select the action");
					builder.setItems(new CharSequence[]{
							"Set active",
							"Edit / Open in view",
							"Delete"
					}, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0:
									Playlist.setActivePlaylist(getContext(), d.second, true);
									refresh();
									break;
								case 1:
									setFromPlaylist(d.first, d.second);
									refresh();
									break;
								case 2:
									Playlist.delete(getContext(), d.second, d.first, true);
									refresh();
									break;
							}

							dialog.dismiss();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			});

		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ViewHolder(View view) {
				super(view);

				this.view = view;
			}

		}

		public void setData(Collection<android.util.Pair<Long, String>> d, String active) {
			data.clear();
			data.addAll(d);
			dataActive = active;
			notifyDataSetChanged();
		}

		public void refresh() {
			final ArrayList<android.util.Pair<Long, String>> playlists = new ArrayList<>();
			for (Playlist playlist : Playlist.loadAllPlaylists())
				playlists.add(android.util.Pair.create(playlist.getLinkedAndroidOSPlaylistId(), playlist.getName()));
			Playlist.allPlaylist(getContext().getContentResolver(), new JavaEx.ActionTU<Long, String>() {
				@Override
				public void execute(Long id, String name) {
					android.util.Pair<Long, String> item = new android.util.Pair<Long, String>(id, name);
					if (!playlists.contains(item))
						playlists.add(item);
				}
			});
			setData(playlists, Playlist.getActivePlaylist(getContext()));
		}

	}

	private void togglePlaylistSettings(View ref) {
		try {
			View v = ((LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.playlist_settings, null);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
			builder.setView(v);

			createPlaylistsSettings(v);

			AlertDialog alert = builder.create();

			alert.requestWindowFeature(DialogFragment.STYLE_NO_TITLE);

			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//endregion

	//region Playlist view settings

	//region UI sort mode

	public enum UISortMode {
		Default("Default/Custom"),
		Score("Smart score ▼"),
		LastPlayed("Recently played ▼"),
		Added("Recently added ▼"),
		Track("# Track ▲"),
		Title("Title ▲"),
		Album("Album ▲"),
		Artist("Artist ▲"),
		Played("Times Played ▼"),
		Skipped("Times Skipped ▼"),
		Timestamp("Timestamp ▼"),;

		private String friendlyName;

		UISortMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_SORT_MODE = SPrefEx.TAG_SPREF + ".library_ui_sort_mode";

	public static UISortMode getUISortMode(Context context) {
		return UISortMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_SORT_MODE, String.valueOf(UISortMode.Default)));
	}

	public static void setUISortMode(Context context, UISortMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_SORT_MODE, String.valueOf(value))
				.apply();
	}

	private void createUISortMode(final Spinner spinner) {
		UISortMode[] items = UISortMode.values();

		spinner.setAdapter(new ArrayAdapter<UISortMode>(getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("Sorting: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		UISortMode lastMode = getUISortMode(getContext());
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		spinner.setSelection(i, true);

		spinner.post(new Runnable() {
			public void run() {
				spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUISortMode(getContext(), (UISortMode) adapterView.getItemAtPosition(position));

						adapter.refresh(getSearchQuery());
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public List<Music> UISort(List<Music> data) {
		final UISortMode uiSortMode = getUISortMode(getContext());

		switch (uiSortMode) {
			case Title:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getTitle().compareToIgnoreCase(y.getTitle());
					}
				});
				break;
			case Album:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getAlbum().compareToIgnoreCase(y.getAlbum());
					}
				});
				break;
			case Artist:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return x.getArtist().compareToIgnoreCase(y.getArtist());
					}
				});
				break;
			case Played:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getPlayed(), y.getPlayed());
					}
				});
				Collections.reverse(data);
				break;
			case Skipped:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getSkipped(), y.getSkipped());
					}
				});
				Collections.reverse(data);
				break;
			case Added:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Long.compare(x.getTimeAdded(), y.getTimeAdded());
					}
				});
				Collections.reverse(data);
				break;
			case Score:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Double.compare(x.getScore(), y.getScore());
					}
				});
				Collections.reverse(data);
				break;
			case Track:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Integer.compare(x.getTrack(), y.getTrack());
					}
				});
				break;
			case Timestamp:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Long.compare(x.getTimestamp(), y.getTimestamp());
					}
				});
				Collections.reverse(data);
				break;
			case LastPlayed:
				Collections.sort(data, new Comparator<Music>() {
					@Override
					public int compare(Music x, Music y) {
						return Long.compare(x.getTimeLastPlayed(), y.getTimeLastPlayed());
					}
				});
				Collections.reverse(data);
				break;

			case Default:
			default:
				break;
		}

		return data;
	}

	//endregion

	//region UI group mode

	public enum UIGroupMode {
		Default("Default"),
		Album("Album"),
		Artist("Artist"),
		Genre("Genre"),
		Year("Year"),
		SmartGenre("Smart genre"),;

		private String friendlyName;

		UIGroupMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_GROUP_MODE = SPrefEx.TAG_SPREF + ".library_ui_group_mode";

	public static UIGroupMode getUIGroupMode(Context context) {
		return UIGroupMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(UIGroupMode.Artist)));
	}

	public static void setUIGroupMode(Context context, UIGroupMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiGroupMode_spinner;

	private void createUIGroupMode(View v) {
		uiGroupMode_spinner = (Spinner) v.findViewById(R.id.playlist);

		UIGroupMode[] items = UIGroupMode.values();

		uiGroupMode_spinner.setAdapter(new ArrayAdapter<UIGroupMode>(getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("Grouping: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		UIGroupMode lastMode = getUIGroupMode(getContext());
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiGroupMode_spinner.setSelection(i, true);

		uiGroupMode_spinner.post(new Runnable() {
			public void run() {
				uiGroupMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIGroupMode(getContext(), (UIGroupMode) adapterView.getItemAtPosition(position));

						adapter.refresh(getSearchQuery());
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public Map<String, List<Music>> UIGroup(List<Music> data) {
		Map<String, List<Music>> result = new HashMap<>();

		final PlaylistViewActivity.PlaylistViewTab playlistViewTab = PlaylistViewActivity.getPlaylistViewTab(getContext());

		switch (playlistViewTab) {
			case Albums:
				for (Music d : data) {
					String key = d.getAlbum();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case Artists:
				for (Music d : data) {
					String key = d.getArtist();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
				/*
			case Genre:
				for (Music d : data) {
					String key = d.getGenre();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case Year:
				for (Music d : data) {
					int year = d.getYear();
					String key = year <= 0 ? "*" : String.valueOf(year);
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case SmartGenre:
				for (Music d : data) {
					String key = d.getSmartGenre().getFriendlyName();
					if (TextUtils.isEmpty(key))
						key = "*";
					if (result.containsKey(key)) {
						List<Music> list = result.get(key);
						list.add(d);
					} else {
						List<Music> list = new ArrayList<>();
						list.add(d);
						result.put(key, list);
					}
				}
				result = new TreeMap<>(result);
				break;
			case Default:
				*/
			default:
				result.put("*", data);
				break;
		}

		return result;
	}

	//endregion

	//region Playlist Item UI style

	public enum PlaylistItemUIStyle {
		Default("Default"),
		Simple("Simple"),
		Card1("Card 1"),
		Card2("Card 2"),;

		private String friendlyName;

		PlaylistItemUIStyle(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static PlaylistItemUIStyle getPlaylistItemUIStyle(Context context) {
		try {
			return PlaylistItemUIStyle.valueOf(SPrefEx.get(context).getString(PlaylistItemUIStyle.class.getSimpleName(), String.valueOf(PlaylistItemUIStyle.Card2)));
		} catch (Exception e) {
			e.printStackTrace();

			return PlaylistItemUIStyle.Card2;
		}
	}

	public static void setPlaylistItemUIStyle(Context context, PlaylistItemUIStyle value) {
		SPrefEx.get(context)
				.edit()
				.putString(PlaylistItemUIStyle.class.getSimpleName(), String.valueOf(value))
				.apply();
	}

	private Spinner playlist_item_ui_style_spinner;

	private void createPlaylistItemUIStyle(View v) {
		playlist_item_ui_style_spinner = v.findViewById(R.id.playlist);

		PlaylistItemUIStyle[] items = PlaylistItemUIStyle.values();

		playlist_item_ui_style_spinner.setAdapter(new ArrayAdapter<PlaylistItemUIStyle>(v.getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText(text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText("Item style: " + getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		PlaylistItemUIStyle lastMode = getPlaylistItemUIStyle(v.getContext());
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		playlist_item_ui_style_spinner.setSelection(i, true);

		playlist_item_ui_style_spinner.post(new Runnable() {
			public void run() {
				playlist_item_ui_style_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setPlaylistItemUIStyle(view.getContext().getApplicationContext(), (PlaylistItemUIStyle) adapterView.getItemAtPosition(position));

						info(getString(R.string.will_apply_after_restart));
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	//endregion

	//endregion

	public static PlaylistViewFragment create() {
		PlaylistViewFragment f = new PlaylistViewFragment();
		return f;
	}

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_LIBRARY_UI_SORT_MODE,
			TAG_SPREF_LIBRARY_UI_GROUP_MODE,
			TAG_SPREF_LIBRARY_UI_SORT_MODE
	};

}
