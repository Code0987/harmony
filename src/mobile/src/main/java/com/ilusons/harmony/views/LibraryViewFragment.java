package com.ilusons.harmony.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
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
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v7.graphics.Palette;
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
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.support.v7.widget.SearchView;
import android.widget.ImageView;
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
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.turingtechnologies.materialscrollbar.ICustomAdapter;
import com.wang.avi.AVLoadingIndicatorView;

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

		createItems(v);

		createPlaylists(v);

		loading.smoothToHide();

		return v;
	}

	public static LibraryViewFragment create() {
		LibraryViewFragment f = new LibraryViewFragment();
		return f;
	}

	@Override
	public void onDestroyView() {
		if (onChildAttachStateChangeListener != null)
			recyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);

		super.onDestroyView();
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
						getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
	private RecyclerView.OnChildAttachStateChangeListener onChildAttachStateChangeListener;

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

		onChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
			@Override
			public void onChildViewAttachedToWindow(View view) {
				adapter.onGroupItemInView(view);
			}

			@Override
			public void onChildViewDetachedFromWindow(View view) {
				adapter.onGroupItemOutOfView(view);
			}
		};
		recyclerView.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);

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
							context.viewPlaylist = playlist;

							context.getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									context.adapter.setData(playlist.getItems());
									context.recyclerView.postDelayed(new Runnable() {
										@Override
										public void run() {
											context.adapter.jumpToCurrentlyPlayingItem();
										}
									}, 1000);
									context.loading.smoothToHide();
									context.info("Loaded playlist!");
								}
							});
						}
					},
					new JavaEx.ActionT<Exception>() {
						@Override
						public void execute(Exception e) {
							context.getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									context.loading.smoothToHide();
									context.info("Playlist load failed!");
								}
							});
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

		private static final String KEY_GROUP = "group";

		private static final int ITEMS_PER_AD = 8;
//		private AdListener lastAdListener = null;

		private final List<Music> data;
		private final List<Pair<String, List<Object>>> dataFiltered;

		private final SettingsActivity.UIStyle uiStyle;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
			dataFiltered = new ArrayList<>();

			uiStyle = SettingsActivity.getUIStyle(getContext());

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

			View view = inflater.inflate(R.layout.library_ui_item_group, parent, false);

			view.setTag(KEY_GROUP);

			return new GroupViewHolder(view);
		}

		@Override
		public ViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			int layoutId = -1;
			switch (uiStyle) {
				case LUI2:
					layoutId = R.layout.library_ui_lui2_item;
					break;

				case LUI5:
					layoutId = R.layout.library_ui_lui5_item;
					break;

				case LUI11:
					layoutId = R.layout.library_ui_lui11_item;
					break;

				case LUI12:
					layoutId = R.layout.library_ui_lui12_item;
					break;

				case Default:
				default:
					layoutId = R.layout.library_ui_default_item;
					break;
			}

			View view = inflater.inflate(layoutId, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindGroupViewHolder(GroupViewHolder holder, int groupPosition, int viewType) {
			final String d = dataFiltered.get(groupPosition).first;
			final View v = holder.view;

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

											onGroupItemInView(v);
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

		@Override
		public void onBindChildViewHolder(ViewHolder holder, int groupPosition, int childPosition, int viewType) {
			final Object d = dataFiltered.get(groupPosition).second.get(childPosition);
			final View v = holder.view;

			setupLayout(v, childPosition, false);

			// Bind data to view here!

/*
			// TODO: Fix ads later
			if (false && ((BuildConfig.DEBUG || !MusicService.IsPremium) && (d instanceof NativeExpressAdView && lastAdListener == null))) {

				CardView cv = (CardView) v.findViewById(R.id.cardView);

				final NativeExpressAdView adView = (NativeExpressAdView) d;

				try {
					if (adView.getParent() == null) {

						int w = (int) ((cv.getWidth() - cv.getPaddingLeft() - cv.getPaddingRight()) / getResources().getDisplayMetrics().density);
						int h = (int) ((cv.getHeight() - cv.getPaddingTop() - cv.getPaddingBottom()) / getResources().getDisplayMetrics().density);

						if (w > 280 && h > 80) {
							adView.setAdUnitId(BuildConfig.AD_UNIT_ID_NE1);
							adView.setAdSize(new AdSize(w, h));

							cv.addView(adView, new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT));

							lastAdListener = new AdListener() {
								@Override
								public void onAdLoaded() {
									super.onAdLoaded();

									lastAdListener = null;
								}

								@Override
								public void onAdFailedToLoad(int errorCode) {

									lastAdListener = null;
								}
							};
							adView.setAdListener(lastAdListener);

							AdRequest adRequest;
							if (BuildConfig.DEBUG) {
								adRequest = new AdRequest.Builder()
										.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
										.addTestDevice(AndroidEx.getDeviceIdHashed(getContext()))
										.build();
							} else {
								adRequest = new AdRequest.Builder()
										.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
										.build();
							}

							adView.loadAd(adRequest);

						}
					}
				} catch (Exception e) {
					Log.w(TAG, e);
				}

			} else
*/
			if (d instanceof Music) {

				final Music item = (Music) d;

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
							TransitionDrawable d = new TransitionDrawable(new Drawable[]{
									cover.getDrawable(),
									new BitmapDrawable(getResources(), bitmap)
							});

							cover.setImageDrawable(d);

							d.setCrossFadeEnabled(true);
							d.startTransition(200);
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
			return true;
		}

		public void setData(Collection<Music> d) {
			data.clear();
			data.addAll(d);

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

						// Add ads
						// TODO: Fix ads later
						/*if (false && (BuildConfig.DEBUG || !MusicService.IsPremium)) {
						    final int n = Math.min(dataFiltered.size(), 7);
                            for (int i = 0; i <= n; i += ITEMS_PER_AD)
                                try {
                                    final NativeExpressAdView adView = new NativeExpressAdView(getContext());
                                    dataFiltered.add(i, adView);
                                } catch (Exception e) {
                                    Log.w(TAG, e);
                                }
                        }*/

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
				recyclerView.smoothScrollToPosition(position);

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

		public void onGroupItemInView(View v) {
			try {
				if (v.isAttachedToWindow() && v.getTag() != null && v.getTag().toString().equalsIgnoreCase(KEY_GROUP)) {
					ImageView cover = v.findViewById(R.id.cover);
					if (cover != null) {
						if (isImageSet(cover)) {
							ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) v.getLayoutParams();
							params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 286, getResources().getDisplayMetrics());
							v.setLayoutParams(params);
						} else {
							ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) v.getLayoutParams();
							params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
							v.setLayoutParams(params);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onGroupItemOutOfView(View v) {
			try {
				if (v.isAttachedToWindow() && v.getTag() != null && v.getTag().toString().equalsIgnoreCase(KEY_GROUP)) {
					ImageView cover = v.findViewById(R.id.cover);
					if (cover != null && isImageSet(cover)) {
						ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) v.getLayoutParams();
						params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
						v.setLayoutParams(params);
					}
				}
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
			case Complex3: {
				FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());

				layoutManager.setFlexDirection(FlexDirection.ROW);
				layoutManager.setFlexWrap(FlexWrap.WRAP);
				layoutManager.setJustifyContent(JustifyContent.FLEX_START);
				layoutManager.setAlignItems(AlignItems.STRETCH);

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

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_LIBRARY_UI_SORT_MODE,
			TAG_SPREF_LIBRARY_UI_GROUP_MODE,
			TAG_SPREF_LIBRARY_UI_VIEW_MODE,
			TAG_SPREF_LIBRARY_UI_SORT_MODE
	};

}
