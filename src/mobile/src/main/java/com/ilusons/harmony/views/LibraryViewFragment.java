package com.ilusons.harmony.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.support.v7.widget.SearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.turingtechnologies.materialscrollbar.ICustomAdapter;
import com.wang.avi.AVLoadingIndicatorView;

import org.w3c.dom.Text;

import java.io.File;
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

import jonathanfinerty.once.Once;

public class LibraryViewFragment extends BaseUIFragment {

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

		createMenu(v);

		createUIFilters(v);
		createUISortMode(v);
		createUIGroupMode(v);
		createUIViewMode(v);
		createUIStyle(v);

		createItems(v);

		createPlaylists(v);

		loading.smoothToHide();

		return v;
	}

	public static LibraryViewFragment create() {
		LibraryViewFragment f = new LibraryViewFragment();
		return f;
	}

	//region Search

	private FloatingActionButton fab_search;
	private View search;
	private ImageButton search_close;
	private SearchView search_view;

	private void createSearch(View v) {
		fab_search = v.findViewById(R.id.fab_search);

		search = v.findViewById(R.id.search);

		search_view = v.findViewById(R.id.search_view);

		fab_search.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (search.getVisibility() != View.VISIBLE) {
					search.setAlpha(0);
					search.setVisibility(View.VISIBLE);

					search.animate().alpha(1).setDuration(283).start();
					search.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_up));

					try {
						search_view.requestFocus();
						new Handler().postDelayed(new Runnable() {
							public void run() {
								try {
									search_view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
									search_view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}, 283);
					} catch (Exception e) {
						e.printStackTrace();
					}

					fab_search.animate().alpha(0).setDuration(163).withEndAction(new Runnable() {
						@Override
						public void run() {
							fab_search.setVisibility(View.GONE);
						}
					}).start();
					fab_menu.animate().alpha(0).setDuration(163).withEndAction(new Runnable() {
						@Override
						public void run() {
							fab_menu.setVisibility(View.GONE);
						}
					}).start();
				} else {
					search.setAlpha(1);
					search.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
						@Override
						public void run() {
							search.setVisibility(View.INVISIBLE);
						}
					}).start();
					search.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_down));

					fab_search.setVisibility(View.VISIBLE);
					fab_search.animate().alpha(1).setDuration(173).start();
					fab_menu.setVisibility(View.VISIBLE);
					fab_menu.animate().alpha(1).setDuration(173).start();
				}
			}
		});

		search_close = v.findViewById(R.id.search_close);

		search_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				search.setAlpha(1);
				search.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
					@Override
					public void run() {
						search.setVisibility(View.INVISIBLE);
					}
				}).start();
				search.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_down));

				fab_search.setVisibility(View.VISIBLE);
				fab_search.animate().alpha(1).setDuration(173).start();
				fab_menu.setVisibility(View.VISIBLE);
				fab_menu.animate().alpha(1).setDuration(173).start();
			}
		});

		search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

	}

	//endregion

	//region Menu

	private FloatingActionButton fab_menu;
	private View menu;
	private ImageButton menu_close;

	private void createMenu(View v) {
		fab_menu = v.findViewById(R.id.fab_menu);

		menu = v.findViewById(R.id.menu);

		fab_menu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (menu.getVisibility() != View.VISIBLE) {
					menu.setAlpha(0);
					menu.setVisibility(View.VISIBLE);
					menu.animate().alpha(1).setDuration(283).start();
					menu.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_up));

					fab_search.animate().alpha(0).setDuration(163).withEndAction(new Runnable() {
						@Override
						public void run() {
							fab_search.setVisibility(View.GONE);
						}
					}).start();
					fab_menu.animate().alpha(0).setDuration(163).withEndAction(new Runnable() {
						@Override
						public void run() {
							fab_menu.setVisibility(View.GONE);
						}
					}).start();
				} else {
					menu.setAlpha(1);
					menu.animate().alpha(0).setDuration(333).withEndAction(new Runnable() {
						@Override
						public void run() {
							menu.setVisibility(View.GONE);
						}
					}).start();
					menu.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_down));

					fab_search.setVisibility(View.VISIBLE);
					fab_search.animate().alpha(1).setDuration(173).start();
					fab_menu.setVisibility(View.VISIBLE);
					fab_menu.animate().alpha(1).setDuration(173).start();
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
						menu.setVisibility(View.GONE);
					}
				}).start();
				menu.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.slide_down));

				fab_search.setVisibility(View.VISIBLE);
				fab_search.animate().alpha(1).setDuration(173).start();
				fab_menu.setVisibility(View.VISIBLE);
				fab_menu.animate().alpha(1).setDuration(173).start();
			}
		});

	}

	//endregion

	//region Items

	private RecyclerViewAdapter adapter;
	private RecyclerViewExpandableItemManager recyclerViewExpandableItemManager;

	private static final int RECYCLER_VIEW_ASSUMED_ITEMS_IN_VIEW = 5;

	private RecyclerView recyclerView;
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

		recyclerViewExpandableItemManager = new RecyclerViewExpandableItemManager(null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new RecyclerViewAdapter();
		recyclerView.setAdapter(recyclerViewExpandableItemManager.createWrappedAdapter(adapter));
		((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
		recyclerViewExpandableItemManager.attachRecyclerView(recyclerView);
		recyclerViewExpandableItemManager.setDefaultGroupsExpandedState(true);

		fastScrollLayout = v.findViewById(R.id.fastScrollLayout);
		fastScrollLayout.setRecyclerView(recyclerView);
	}

	public static class FastScrollLayout extends LinearLayout {
		private static final int HANDLE_HIDE_DELAY = 1300;
		private static final int HANDLE_ANIMATION_DURATION = 100;
		private static final int TRACK_SNAP_RANGE = 15;
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
			inflater.inflate(R.layout.library_view_fast_scroll, this);
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

	private void createPlaylists(View v) {

		UIGroup();

		setFromPlaylist(-1L, Playlist.getActivePlaylist(getContext()));

	}

	private static class SetFromPlaylistAsyncTask extends AsyncTask<Object, Object, Object> {
		private String playlistName;
		private Long playlistId;
		private WeakReference<LibraryViewFragment> contextRef;

		public SetFromPlaylistAsyncTask(LibraryViewFragment context, String playlistName, Long playlistId) {
			this.contextRef = new WeakReference<LibraryViewFragment>(context);
			this.playlistName = playlistName;
			this.playlistId = playlistId;
		}

		@Override
		protected Object doInBackground(Object... objects) {
			final LibraryViewFragment context = contextRef.get();
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

			final LibraryViewFragment context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToShow();
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);

			final LibraryViewFragment context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToHide();
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

	//region View

	public class RecyclerViewAdapter
			extends AbstractExpandableItemAdapter<GroupViewHolder, ViewHolder>
			implements ICustomAdapter {

		private static final int ITEMS_PER_AD = 8;
//		private AdListener lastAdListener = null;

		private final List<Music> data;
		private final List<Pair<String, List<Object>>> dataFiltered;

		private final UIStyle uiStyle;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
			dataFiltered = new ArrayList<>();

			uiStyle = getUIStyle(getContext());

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
			switch (uiStyle) {
				case Card1:
					layoutId = R.layout.library_view_group_card;
					break;

				case Card2:
					layoutId = R.layout.library_view_group_card;
					break;

				case Card3:
					layoutId = R.layout.library_view_group_card;
					break;

				case Card4:
					layoutId = R.layout.library_view_group_card;
					break;

				case Card5:
					layoutId = R.layout.library_view_group_card;
					break;

				case Simple:
					layoutId = R.layout.library_view_group_simple;
					break;

				case Default:
				default:
					layoutId = R.layout.library_view_group_default;
					break;
			}

			View view = inflater.inflate(layoutId, parent, false);

			return new GroupViewHolder(view);
		}

		@Override
		public ViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			int layoutId = -1;
			switch (uiStyle) {
				case Card1:
					layoutId = R.layout.library_view_item_card1;
					break;

				case Card2:
					layoutId = R.layout.library_view_item_card2;
					break;

				case Card3:
					layoutId = R.layout.library_view_item_card3;
					break;

				case Card4:
					layoutId = R.layout.library_view_item_card4;
					break;

				case Card5:
					layoutId = R.layout.library_view_item_card5;
					break;

				case Simple:
					layoutId = R.layout.library_view_item_simple;
					break;

				case Default:
				default:
					layoutId = R.layout.library_view_item_default;
					break;
			}

			View view = inflater.inflate(layoutId, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindGroupViewHolder(GroupViewHolder holder, int groupPosition, int viewType) {
			final String d = dataFiltered.get(groupPosition).first;
			final View v = holder.view;

			v.setTag(d);

			setupLayout(v, groupPosition, true);

			TextView title = ((TextView) v.findViewById(R.id.title));
			title.setText(d);

			try {
				final ImageView cover = (ImageView) v.findViewById(R.id.cover);
				if (cover != null) {
					cover.setImageBitmap(null);
					final int coverSize = Math.max(cover.getWidth(), cover.getHeight());

					if (!TextUtils.isEmpty(d) && d.length() > 5) {
						ArtworkEx.ArtworkType artworkType;
						String q = d;

						switch (getUIGroupMode(getContext())) {
							case Album:
								artworkType = ArtworkEx.ArtworkType.Album;
								break;
							case Artist:
								artworkType = ArtworkEx.ArtworkType.Artist;
								break;
							case Genre:
								artworkType = ArtworkEx.ArtworkType.None;
								break;
							case Year:
								artworkType = ArtworkEx.ArtworkType.None;
								break;
							case Default:
							default:
								artworkType = ArtworkEx.ArtworkType.Song;
								break;
						}

						if (artworkType != ArtworkEx.ArtworkType.None) {
							ArtworkEx.getArtworkDownloaderTask(
									getContext(),
									q,
									artworkType,
									-1,
									d,
									Music.KEY_CACHE_DIR_COVER,
									d,
									new JavaEx.ActionT<Bitmap>() {
										@Override
										public void execute(Bitmap bitmap) {
											cover.setImageBitmap(bitmap);
										}
									},
									new JavaEx.ActionT<Exception>() {
										@Override
										public void execute(Exception e) {
											Log.w(TAG, e);

											cover.setImageDrawable(null);
										}
									},
									1500,
									false);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@SuppressLint("StaticFieldLeak")
		@Override
		public void onBindChildViewHolder(ViewHolder holder, int groupPosition, int childPosition, int viewType) {
			final Object d = dataFiltered.get(groupPosition).second.get(childPosition);
			final View v = holder.view;

			setupLayout(v, childPosition, false);

			// Bind data to view here!
			if (d instanceof Music) {

				final Music item = (Music) d;

				v.setTag(item.getText());

				final View root = v.findViewById(R.id.root);

				final ImageView cover = (ImageView) v.findViewById(R.id.cover);
				if (cover != null) {
					cover.setImageBitmap(null);
					// HACK: This animates as well as reduces load on image view
					final int coverSize = Math.max(cover.getWidth(), cover.getHeight());
					(new AsyncTask<Void, Void, Bitmap>() {
						@Override
						protected Bitmap doInBackground(Void... voids) {
							return item.getCover(getContext(), coverSize);
						}

						@Override
						protected void onPostExecute(Bitmap bitmap) {
							try {
								TransitionDrawable d = new TransitionDrawable(new Drawable[]{
										cover.getDrawable(),
										new BitmapDrawable(getResources(), bitmap)
								});

								cover.setImageDrawable(d);

								d.setCrossFadeEnabled(true);
								d.startTransition(200);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).execute();
				}

				TextView title = (TextView) v.findViewById(R.id.title);
				if (title != null)
					title.setText(item.getTitle());

				TextView artist = (TextView) v.findViewById(R.id.artist);
				if (artist != null)
					artist.setText(item.getArtist());

				TextView album = (TextView) v.findViewById(R.id.album);
				if (album != null)
					album.setText(item.getAlbum());

				TextView info = (TextView) v.findViewById(R.id.info);
				if (info != null) {
					String s;
					try {
						s = item.getTextExtraOnlySingleLine(getMusicService().getPlaylist().getItems().lastIndexOf(item));
					} catch (Exception e) {
						e.printStackTrace();

						s = item.getTextExtraOnlySingleLine();
					}
					info.setText(s);
					info.setHorizontallyScrolling(true);
					info.setSelected(true);
					info.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							TextView tv = (TextView) v;
							if (!hasFocus && tv != null) {
								tv.setSelected(true);
							}
						}
					});
				}

				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent i = new Intent(getContext(), MusicService.class);

						i.setAction(MusicService.ACTION_OPEN);
						i.putExtra(MusicService.KEY_URI, item.getPath());

						getContext().startService(i);

						highlightView(root);
					}
				});

				v.setOnLongClickListener(new View.OnLongClickListener() {
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
								"Add next",
								"Add at start",
								"Add at last",
								"Remove",
								"Clear (except current)",
								"Move down",
								"Move up",
								"Delete (physically)"
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
										case 1:
											viewPlaylist.add(item, viewPlaylist.getItems().lastIndexOf(item) + 1);
											break;
										case 2:
											viewPlaylist.add(item, 0);
											break;
										case 3:
											viewPlaylist.add(item, viewPlaylist.getItems().size());
											break;
										case 4:
											viewPlaylist.remove(item);
											break;
										case 5:
											viewPlaylist.removeAllExceptCurrent();
											break;
										case 6:
											viewPlaylist.moveDown(item);
											break;
										case 7:
											viewPlaylist.moveUp(item);
											break;
										case 8:
											viewPlaylist.delete(item, getMusicService(), true);
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
			final UIGroupMode uiGroupMode = getUIGroupMode(getContext());

			boolean r = true;

			switch (uiGroupMode) {
				case Album:
				case Artist:
				case Year:
				case Genre:
					r = false;
					break;
				case Default:
					r = true;
					break;
			}

			return r;
		}

		public void setData(Collection<Music> d) {
			data.clear();
			data.addAll(d);

			refresh(String.valueOf(search_view.getQuery()));
		}

		public void resetData() {
			ArrayList<Music> oldData = new ArrayList<>(data);

			data.clear();
			data.addAll(oldData);

			refresh(String.valueOf(search_view.getQuery()));
		}

		public void removeData(Music d) {
			data.remove(d);

			refresh(String.valueOf(search_view.getQuery()));
		}

		public void refresh(final String q) {
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

						// Filter
						List<Music> filtered = new ArrayList<>();
						filtered.addAll(data);
						filtered = UIFilter(filtered, q);

						// Sort
						List<Music> sorted = UISort(filtered);

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

		private void setupLayout(View v, int p, boolean header) {
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			if (lp instanceof FlexboxLayoutManager.LayoutParams) {
				FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;

				flexboxLp.setAlignSelf(AlignSelf.FLEX_START);
				flexboxLp.setWrapBefore(false);

				if (header) {
					flexboxLp.setFlexBasisPercent(100);
				} else {
					switch (p) {
						case 0:
							flexboxLp.setFlexBasisPercent(100);
							break;
						case 1:
							flexboxLp.setFlexBasisPercent(50);
							break;
						default:
							flexboxLp.setFlexBasisPercent(25);
							break;
					}
				}
			}
		}

		public void highlightView(View root) {
			try {
				root.clearAnimation();

				AnimatorSet as = new AnimatorSet();
				as.playSequentially(
						ObjectAnimator.ofArgb(root, "backgroundColor", ContextCompat.getColor(getContext(), R.color.transparent), ContextCompat.getColor(getContext(), R.color.accent), ContextCompat.getColor(getContext(), R.color.transparent))
				);
				as.setDuration(733);
				as.setInterpolator(new LinearInterpolator());
				as.setTarget(root);

				as.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void bringInToView(int position) {
			try {
				int delta = Math.abs(recyclerView.getScrollY() - recyclerView.getLayoutManager().getChildAt(0).getHeight() * position);

				if (delta < recyclerView.getMeasuredHeight() * 5)
					recyclerView.smoothScrollToPosition(position);
				else
					recyclerView.scrollToPosition(position);

				View v = recyclerView.getLayoutManager().getChildAt(position);

				highlightView(v);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void jumpToCurrentlyPlayingItem() {
			try {
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

	}

	public class GroupViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public GroupViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	public class ViewHolder extends AbstractExpandableItemViewHolder {
		public View view;

		public ViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	//endregion

	//region UI filters

	public enum UIFilters {
		NoMP3("No MP3"),
		NoM4A("No M4A"),
		NoFLAC("No FLAC"),
		NoOGG("No OGG"),
		NoWAV("No WAV"),
		NoMP4("No MP4"),
		NoM4V("No M4V"),
		NoMKV("No MKV"),
		NoAVI("No AVI"),
		NoWEBM("No WEBM"),;

		private String friendlyName;

		UIFilters(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_FILTERS = SPrefEx.TAG_SPREF + ".library_ui_filters";

	public static Set<UIFilters> getUIFilters(Context context) {
		Set<UIFilters> value = new HashSet<>();

		Set<String> values = new HashSet<>();
		for (String item : SPrefEx.get(context).getStringSet(TAG_SPREF_LIBRARY_UI_FILTERS, values)) {
			value.add(UIFilters.valueOf(item));
		}

		return value;
	}

	public static void setUIFilters(Context context, Set<UIFilters> value) {
		Set<String> values = new HashSet<>();
		for (UIFilters item : value) {
			values.add(String.valueOf(item));
		}

		SPrefEx.get(context)
				.edit()
				.putStringSet(TAG_SPREF_LIBRARY_UI_FILTERS, values)
				.apply();
	}

	private Spinner uiFilters_spinner;

	private void createUIFilters(View v) {
		uiFilters_spinner = (Spinner) v.findViewById(R.id.uiFilters_spinner);

		UIFilters[] items = UIFilters.values();

		uiFilters_spinner.setAdapter(new ArrayAdapter<UIFilters>(getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView v = (CheckedTextView) convertView;

				if (v == null) {
					v = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					v.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					v.setLayoutParams(lp);
					v.setPadding(px, px, px, px);

					v.setText("Filters");
				}

				return v;
			}

			@Override
			public View getDropDownView(final int position, View convertView, ViewGroup parent) {
				RelativeLayout v = (RelativeLayout) convertView;

				if (v == null) {
					v = new RelativeLayout(getContext());
					v.setLayoutParams(new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					));

					Switch sv = new Switch(getContext());
					sv.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					sv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					sv.setLayoutParams(lp);
					sv.setPadding(px, px, px, px);
					sv.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, getResources().getDisplayMetrics()));

					v.addView(sv);

					sv.setText(getItem(position).friendlyName);
					sv.setChecked(getUIFilters(getContext()).contains(getItem(position)));
					sv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
							if (!compoundButton.isShown()) {
								return;
							}

							Set<UIFilters> value = getUIFilters(getContext());

							UIFilters item = getItem(position);
							try {
								if (b)
									value.add(item);
								else
									value.remove(item);
							} catch (Exception e) {
								e.printStackTrace();
							}
							setUIFilters(getContext(), value);

							if (search_view != null)
								adapter.refresh(String.valueOf(search_view.getQuery()));
						}
					});
				}

				((Switch) v.getChildAt(0)).setChecked(getUIFilters(getContext()).contains(getItem(position)));

				return v;
			}
		});

		uiFilters_spinner.post(new Runnable() {
			public void run() {
				uiFilters_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	public synchronized List<Music> UIFilter(Collection<Music> data, String q) {
		ArrayList<Music> result = new ArrayList<>();

		q = q.toLowerCase();

		for (Music m : data) {
			boolean f = true;

			SharedPreferences spref = SPrefEx.get(getContext());

			String ext = m.getPath().substring(m.getPath().lastIndexOf(".")).toLowerCase();
			for (UIFilters uiFilter : getUIFilters(getContext())) {
				boolean uif = uiFilter == UIFilters.NoMP3 && ext.equals(".mp3")
						|| uiFilter == UIFilters.NoM4A && ext.equals(".m4a")
						|| uiFilter == UIFilters.NoFLAC && ext.equals(".flac")
						|| uiFilter == UIFilters.NoOGG && ext.equals(".ogg")
						|| uiFilter == UIFilters.NoWAV && ext.equals(".wav")
						|| uiFilter == UIFilters.NoMP4 && ext.equals(".mp4")
						|| uiFilter == UIFilters.NoMKV && ext.equals(".mkv")
						|| uiFilter == UIFilters.NoAVI && ext.equals(".avi")
						|| uiFilter == UIFilters.NoWEBM && ext.equals(".webm");

				if (uif) {
					f = false;
					break;
				}
			}

			f &= TextUtils.isEmpty(q) || q.length() < 1 ||
					(m.getPath().toLowerCase().contains(q)
							|| m.getTitle().toLowerCase().contains(q)
							|| m.getArtist().toLowerCase().contains(q)
							|| m.getAlbum().toLowerCase().contains(q));

			if (f)
				result.add(m);
		}

		return result;
	}

	//endregion

	//region UI sort mode

	public enum UISortMode {
		Default("Default/Custom"),
		Title("Title ▲"),
		Album("Album ▲"),
		Artist("Artist ▲"),
		Played("Times Played ▼"),
		Skipped("Times Skipped ▼"),
		Added("Added On ▼"),
		Score("Smart score"),
		Track("# Track"),
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

	private Spinner uiSortMode_spinner;

	private void createUISortMode(View v) {
		uiSortMode_spinner = (Spinner) v.findViewById(R.id.uiSortMode_spinner);

		UISortMode[] items = UISortMode.values();

		uiSortMode_spinner.setAdapter(new ArrayAdapter<UISortMode>(getContext(), 0, items) {
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
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
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
		uiSortMode_spinner.setSelection(i, true);

		uiSortMode_spinner.post(new Runnable() {
			public void run() {
				uiSortMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUISortMode(getContext(), (UISortMode) adapterView.getItemAtPosition(position));

						if (search_view != null)
							adapter.refresh(String.valueOf(search_view.getQuery()));
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
		Year("Year"),;

		private String friendlyName;

		UIGroupMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_GROUP_MODE = SPrefEx.TAG_SPREF + ".library_ui_group_mode";

	public static UIGroupMode getUIGroupMode(Context context) {
		return UIGroupMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(UIGroupMode.Default)));
	}

	public static void setUIGroupMode(Context context, UIGroupMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_GROUP_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiGroupMode_spinner;

	private void createUIGroupMode(View v) {
		uiGroupMode_spinner = (Spinner) v.findViewById(R.id.uiGroupMode_spinner);

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
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
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

						if (search_view != null)
							adapter.refresh(String.valueOf(search_view.getQuery()));
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

		final UIGroupMode uiGroupMode = getUIGroupMode(getContext());

		switch (uiGroupMode) {
			case Album:
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
			case Artist:
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
			case Default:
			default:
				result.put("*", data);
				break;
		}

		return result;
	}

	//endregion

	//region UI view mode

	public enum UIViewMode {
		Default("Default"),
		Complex1("Grid of 2"),
		Complex2("Grid of 6|2"),
		Complex3("Flow of 2"),;

		private String friendlyName;

		UIViewMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_LIBRARY_UI_VIEW_MODE = SPrefEx.TAG_SPREF + ".library_ui_view_mode";

	public static UIViewMode getUIViewMode(Context context) {
		return UIViewMode.valueOf(SPrefEx.get(context).getString(TAG_SPREF_LIBRARY_UI_VIEW_MODE, String.valueOf(UIViewMode.Complex1)));
	}

	public static void setUIViewMode(Context context, UIViewMode value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_LIBRARY_UI_VIEW_MODE, String.valueOf(value))
				.apply();
	}

	private Spinner uiViewMode_spinner;

	private void createUIViewMode(View v) {
		uiViewMode_spinner = (Spinner) v.findViewById(R.id.uiViewMode_spinner);

		UIViewMode[] items = UIViewMode.values();

		uiViewMode_spinner.setAdapter(new ArrayAdapter<UIViewMode>(getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("View: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
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
		UIViewMode lastMode = getUIViewMode(getContext());
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiViewMode_spinner.setSelection(i, true);

		uiViewMode_spinner.post(new Runnable() {
			public void run() {
				uiViewMode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIViewMode(getContext(), (UIViewMode) adapterView.getItemAtPosition(position));

						UIGroup();
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	private void UIGroup() {
		final UIViewMode uiViewMode = getUIViewMode(getContext());

		switch (uiViewMode) {
			case Complex1: {
				GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2) {
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
					public int getSpanSize(int position) {
						if (RecyclerViewExpandableItemManager
								.getPackedPositionChild(recyclerViewExpandableItemManager
										.getExpandablePosition(position))
								==
								RecyclerView.NO_POSITION) {
							// group item
							return 2;
						} else {
							// child item
							return 1;
						}
					}
				});
				recyclerView.setLayoutManager(layoutManager);
			}
			break;
			case Complex2: {
				GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 6) {
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
					public int getSpanSize(int position) {
						if (RecyclerViewExpandableItemManager
								.getPackedPositionChild(recyclerViewExpandableItemManager
										.getExpandablePosition(position))
								==
								RecyclerView.NO_POSITION) {
							// group item
							return 6;
						} else {
							// child item
							if (position % 5 == 0)
								return 6;
							return 3;
						}
					}
				});
				recyclerView.setLayoutManager(layoutManager);
			}
			break;
			case Complex3: { // TODO: Fix it
				/*
				FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());

				layoutManager.setFlexDirection(FlexDirection.ROW);
				layoutManager.setFlexWrap(FlexWrap.WRAP);
				layoutManager.setJustifyContent(JustifyContent.FLEX_START);
				layoutManager.setAlignItems(AlignItems.STRETCH);

				recyclerView.setLayoutManager(layoutManager);
				*/

				GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4) {
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
					public int getSpanSize(int position) {
						if (RecyclerViewExpandableItemManager
								.getPackedPositionChild(recyclerViewExpandableItemManager
										.getExpandablePosition(position))
								==
								RecyclerView.NO_POSITION) {
							// group item
							return 4;
						} else {
							// child item
							if (position % 2 == 0 || position % 3 == 0)
								return 2;
							return 4;
						}
					}
				});
				recyclerView.setLayoutManager(layoutManager);
			}
			break;
			case Default:
			default:
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
				break;
		}
	}

	//endregion

	//region UI style

	public enum UIStyle {
		Default("Default"),
		Simple("Simple"),
		Card1("Card 1"),
		Card2("Card 2"),
		Card3("Card 3"),
		Card4("Card 4"),
		Card5("Card 5"),;

		private String friendlyName;

		UIStyle(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_UISTYLE = TAG + ".uistyle";

	public static UIStyle getUIStyle(Context context) {
		try {
			return UIStyle.valueOf(SPrefEx.get(context).getString(TAG_SPREF_UISTYLE, String.valueOf(UIStyle.Card5)));
		} catch (Exception e) {
			e.printStackTrace();

			return UIStyle.Default;
		}
	}

	public static void setUIStyle(Context context, UIStyle value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_UISTYLE, String.valueOf(value))
				.apply();
	}

	private Spinner uiStyle_spinner;

	private void createUIStyle(View v) {
		uiStyle_spinner = (Spinner) v.findViewById(R.id.uiStyle_spinner);

		UIStyle[] items = UIStyle.values();

		uiStyle_spinner.setAdapter(new ArrayAdapter<UIStyle>(getContext(), 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText("Style: " + text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
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
		UIStyle lastMode = getUIStyle(getContext());
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiStyle_spinner.setSelection(i, true);

		uiStyle_spinner.post(new Runnable() {
			public void run() {
				uiStyle_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIStyle(getContext().getApplicationContext(), (UIStyle) adapterView.getItemAtPosition(position));

						recyclerView.getRecycledViewPool().clear();
						adapter.resetData();

						info("UI Style will be completely applied on restart!");
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	//endregion

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_LIBRARY_UI_SORT_MODE,
			TAG_SPREF_LIBRARY_UI_GROUP_MODE,
			TAG_SPREF_LIBRARY_UI_VIEW_MODE,
			TAG_SPREF_LIBRARY_UI_SORT_MODE,
			TAG_SPREF_UISTYLE,
	};

}
