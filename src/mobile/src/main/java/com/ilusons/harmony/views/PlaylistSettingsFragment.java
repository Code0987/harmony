package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.data.Playlist;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.ViewEx;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.Collection;

public class PlaylistSettingsFragment extends BaseUIFragment {

	// Logger TAG
	private static final String TAG = PlaylistSettingsFragment.class.getSimpleName();

	private View root;

	private AVLoadingIndicatorView loading;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.playlist_settings, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		loading = v.findViewById(R.id.loading);

		loading.smoothToShow();

		createPlaylistSettings(v);

		loading.smoothToHide();

		return v;
	}
	@Override
	public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();

		inflater.inflate(R.menu.playlist_settings_menu, menu);

		ViewEx.tintMenuIcons(menu, ContextCompat.getColor(getContext(), R.color.textColorPrimary));

		MenuItem create_new = menu.findItem(R.id.create_new);
		create_new.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
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
											PlaylistRecyclerViewAdapter.refresh();
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

					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		});
	}

	//region Playlist settings

	private PlaylistViewFragment playlistViewFragment;

	public void setPlaylistViewFragment(PlaylistViewFragment instance) {
		playlistViewFragment = instance;
	}

	private PlaylistRecyclerViewAdapter PlaylistRecyclerViewAdapter;

	private void createPlaylistSettings(View v) {
		// Set playlist(s)
		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(1);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		PlaylistRecyclerViewAdapter = new PlaylistRecyclerViewAdapter();
		recyclerView.setAdapter(PlaylistRecyclerViewAdapter);
		PlaylistRecyclerViewAdapter.refresh();

	}

	public class PlaylistRecyclerViewAdapter extends RecyclerView.Adapter<PlaylistRecyclerViewAdapter.ViewHolder> {

		private final ArrayList<android.util.Pair<Long, String>> data;
		private String dataActive;

		public PlaylistRecyclerViewAdapter() {
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
							playlistViewFragment.setFromPlaylist(d.first, d.second);
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
									playlistViewFragment.setFromPlaylist(d.first, d.second);
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

	//endregion

	public static PlaylistSettingsFragment create() {
		PlaylistSettingsFragment f = new PlaylistSettingsFragment();
		return f;
	}

}
