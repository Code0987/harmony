package com.ilusons.harmony.data;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.ArrayEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class Playlist {

	private static final String TAG = Playlist.class.getSimpleName();

	public static final String KEY_PLAYLIST_ALL = "all";
	public static final String KEY_PLAYLIST_MEDIASTORE = "mediastore";
	public static final String KEY_PLAYLIST_STORAGE = "storage";
	public static final String KEY_PLAYLIST_ONLINE = "online";

	private String Name;

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	private ArrayList<Music> Items = new ArrayList<>();

	public ArrayList<Music> getItems() {
		if (Items == null)
			Items = new ArrayList<>();
		return Items;
	}

	private int ItemIndex = -1;

	public int getItemIndex() {
		return ItemIndex;
	}

	public void setItemIndex(final int i) {
		ItemIndex = i;
		if (ItemIndex < 0 || ItemIndex >= getItems().size())
			ItemIndex = 0;
	}

	private Long LinkedAndroidOSPlaylistId = -1L;

	public Long getLinkedAndroidOSPlaylistId() {
		return LinkedAndroidOSPlaylistId;
	}

	public void setLinkedAndroidOSPlaylistId(final Long id) {
		LinkedAndroidOSPlaylistId = id;
	}

	public void add(Music item) {
		getItems().add(item);
	}

	public void add(Music item, int index) {
		getItems().add(index, item);
	}

	public void addIfNot(Music item) {
		if (!getItems().contains(item))
			getItems().add(item);
	}

	public void addAll(Collection<Music> items) {
		getItems().addAll(items);
	}

	public void remove(Music item) {
		getItems().remove(item);
	}

	public void clear() {
		getItems().clear();
	}

	public void removeAll(Music item) {
		getItems().remove(item);
	}

	public void removeAll(Collection<Music> items) {
		getItems().removeAll(items);
	}

	public void removeAllExceptCurrent() {
		try {
			Music current = getItems().get(ItemIndex);
			getItems().clear();
			if (current != null)
				getItems().add(current);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void moveDown(Music item) {
		int i = getItems().indexOf(item);
		if (i > -1) {
			ArrayEx.move(i, i + 1, getItems());
		}
	}

	public void moveUp(Music item) {
		int i = getItems().indexOf(item);
		if (i > -1) {
			ArrayEx.move(i, i - 1, getItems());
		}
	}

	public void delete(Music item, final MusicService musicService, final LibraryStore store, boolean notify) {
		getItems().remove(item);
		Music.delete(musicService, item.getPath(), notify);
		LibraryStore.get().savePlaylist(this);
	}

	public void delete(Music item, final MusicService musicService, boolean notify) {
		delete(item, musicService, LibraryStore.get(), notify);
	}

	public Music getItem() {
		if (getItemIndex() >= 0 && getItemIndex() < getItems().size())
			return getItems().get(getItemIndex());
		return null;
	}

	public boolean isIn(Music item) {
		return getItems().contains(item);
	}

	public static Collection<Playlist> loadAllPlaylists(LibraryStore store) {
		return store.allPlaylists();
	}

	public static ArrayList<Playlist> loadAllPlaylists() {
		return new ArrayList<>(LibraryStore.get().allPlaylists());
	}

	public static Playlist loadOrCreatePlaylist(LibraryStore store, String name) {
		return LibraryStore.get().getOrCreatePlaylist(name);
	}

	public static Playlist loadOrCreatePlaylist(String name) {
		return LibraryStore.get().getOrCreatePlaylist(name);
	}

	public static Playlist loadOrCreatePlaylist(LibraryStore store, String name, JavaEx.ActionT<Playlist> update) {
		Playlist playlist = LibraryStore.get().getPlaylist(name);
		if (playlist == null) {
			playlist = LibraryStore.get().getOrCreatePlaylist(name);
			if (update != null) {
				update.execute(playlist);
			}
			LibraryStore.get().savePlaylist(playlist);
		}
		return playlist;
	}

	public static Playlist loadOrCreatePlaylist(String name, JavaEx.ActionT<Playlist> update) {
		return loadOrCreatePlaylist(LibraryStore.get(), name, update);
	}

	public static void savePlaylist(LibraryStore store, Playlist playlist) {
		LibraryStore.get().savePlaylist(playlist);
	}

	public static void savePlaylist(Playlist playlist) {
		LibraryStore.get().savePlaylist(playlist);
	}

	public static void add(final Context context, final String playlistName, final Music music, boolean sync) {
		try {
			Playlist playlist = loadOrCreatePlaylist(playlistName);
			playlist.add(music);
			LibraryStore.get().upsert(music);
			LibraryStore.get().savePlaylist(playlist);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (sync) {
			try {
				if (getActivePlaylist(context).equals(playlistName)) {
					Intent musicServiceIntent = new Intent(context, MusicService.class);
					musicServiceIntent.setAction(MusicService.ACTION_PLAYLIST_CHANGED);
					musicServiceIntent.putExtra(MusicService.KEY_PLAYLIST_CHANGED_PLAYLIST, playlistName);
					context.startForegroundService(musicServiceIntent);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static final String TAG_SPREF_PLAYLISTS_ACTIVE = "playlists_active";

	public static String getActivePlaylist(Context context) {
		try {
			String s = SPrefEx.get(context).getString(TAG_SPREF_PLAYLISTS_ACTIVE, null);
			if (TextUtils.isEmpty(s)) {
				return KEY_PLAYLIST_ALL;
			}
			return s;
		} catch (Exception e) {
			e.printStackTrace();
			return KEY_PLAYLIST_ALL;
		}
	}

	public static void setActivePlaylist(Context context, String name, boolean notify) {
		try {
			SPrefEx.get(context)
					.edit()
					.putString(TAG_SPREF_PLAYLISTS_ACTIVE, name)
					.apply();

			if (notify) {
				Intent broadcastIntent = new Intent(MusicService.ACTION_PLAYLIST_CHANGED);
				broadcastIntent.putExtra(MusicService.KEY_PLAYLIST_CHANGED_PLAYLIST, name);
				LocalBroadcastManager
						.getInstance(context)
						.sendBroadcast(broadcastIntent);

				Intent musicServiceIntent = new Intent(context, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_PLAYLIST_CHANGED);
				musicServiceIntent.putExtra(MusicService.KEY_PLAYLIST_CHANGED_PLAYLIST, name);
				context.startForegroundService(musicServiceIntent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setActivePlaylist(
			final Context context,
			final String name,
			final Long playlistId,
			final JavaEx.ActionT<Collection<Music>> onProgress,
			final JavaEx.ActionT<Playlist> onSuccess,
			final JavaEx.ActionT<Exception> onError,
			final boolean notify) {
		try {
			setActivePlaylist(context, name, notify);
			if (onSuccess != null) {
				onSuccess.execute(loadOrCreatePlaylist(name));
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (onError != null)
				onError.execute(e);
		}
	}

	public static void delete(final Context context, final String name, final Long playlistId, final boolean notify) {
		LibraryStore.get().deletePlaylist(name);
		if (getActivePlaylist(context).equalsIgnoreCase(name))
			setActivePlaylist(context, KEY_PLAYLIST_ALL, notify);
	}

	public static Observable<Collection<Pair<String, Uri>>> getAllMediaStoreEntriesForAudio(final Context context) {
		return Observable.create(new ObservableOnSubscribe<Collection<Pair<String, Uri>>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<Pair<String, Uri>>> oe) {
				try {
					oe.onNext(queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.IS_MUSIC + "!= 0"));
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Collection<Pair<String, Uri>>> getAllMediaStoreEntriesForVideo(final Context context) {
		return Observable.create(new ObservableOnSubscribe<Collection<Pair<String, Uri>>>() {
			@Override
			public void subscribe(ObservableEmitter<Collection<Pair<String, Uri>>> oe) {
				try {
					oe.onNext(queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null));
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	private static Collection<Pair<String, Uri>> queryMediaStore(Context context, Uri collection, String selection) {
		ArrayList<Pair<String, Uri>> items = new ArrayList<>();
		String[] projection = new String[]{
				MediaStore.MediaColumns._ID,
				MediaStore.MediaColumns.DISPLAY_NAME
		};
		try (Cursor cursor = context.getContentResolver().query(collection, projection, selection, null, MediaStore.MediaColumns.TITLE + " ASC")) {
			if (cursor != null) {
				int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
				while (cursor.moveToNext()) {
					long id = cursor.getLong(idCol);
					Uri contentUri = ContentUris.withAppendedId(collection, id);
					items.add(Pair.create(contentUri.toString(), contentUri));
				}
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return items;
	}

	public static Observable<Playlist> update(
			final Context context,
			final Playlist playlist,
			final Collection<Pair<String, Uri>> scanItems,
			final boolean removeMissing,
			final boolean fastMode,
			final JavaEx.ActionExT<String> onProgress) {
		return Observable.create(new ObservableOnSubscribe<Playlist>() {
			@Override
			public void subscribe(ObservableEmitter<Playlist> oe) {
				try {
					ArrayList<Music> found = new ArrayList<>();
					for (Pair<String, Uri> pair : scanItems) {
						if (onProgress != null) {
							onProgress.execute(pair.first);
						}
						Music music = Music.load(context, pair.second.toString());
						if (music == null) {
							music = Music.createFromLocal(context, pair.second.toString(), pair.second, fastMode, null);
						}
						if (music != null) {
							music.setPath(pair.second.toString());
							music.setLastPlaybackUrl(pair.second.toString());
							LibraryStore.get().upsert(music);
							found.add(music);
						}
					}
					playlist.clear();
					playlist.addAll(found);
					LibraryStore.get().savePlaylist(playlist);

					Playlist all = loadOrCreatePlaylist(KEY_PLAYLIST_ALL);
					for (Music music : found) {
						all.addIfNot(music);
					}
					LibraryStore.get().savePlaylist(all);

					oe.onNext(playlist);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Playlist> updateForLocations(
			final Context context,
			final Playlist playlist,
			final Collection<String> locations,
			final boolean removeMissing,
			final boolean fastMode,
			final JavaEx.ActionExT<String> onProgress) {
		return Observable.create(new ObservableOnSubscribe<Playlist>() {
			@Override
			public void subscribe(ObservableEmitter<Playlist> oe) {
				try {
					// Extra folders are scanned via SAF in a later pass.
					// Keep the playlist as-is so MediaStore remains the source of truth.
					if (onProgress != null && locations != null) {
						for (String location : locations) {
							onProgress.execute(location);
						}
					}
					LibraryStore.get().savePlaylist(playlist);
					oe.onNext(playlist);
					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	public static Observable<Playlist> updateSmart(final Context context, final Playlist playlist) {
		return Observable.create(new ObservableOnSubscribe<Playlist>() {
			@Override
			public void subscribe(ObservableEmitter<Playlist> oe) {
				LibraryStore.get().savePlaylist(playlist);
				oe.onNext(playlist);
				oe.onComplete();
			}
		});
	}

	private String Query;

	public String getQuery() {
		return Query;
	}

	public void setQuery(String value) {
		Query = value;
	}

	public boolean isSmart() {
		return !TextUtils.isEmpty(getQuery());
	}

	public static List<String> allPlaylist(android.content.ContentResolver cr) {
		return new ArrayList<>();
	}

	public static void allPlaylist(android.content.ContentResolver cr, JavaEx.ActionTU<Long, String> action) {
		if (action == null) {
			return;
		}
	}

	public static void getAllMusicForIds(LibraryStore store, Context context, Collection<String> audioIds, JavaEx.ActionT<Music> action) {
		if (action == null)
			return;
		for (String audioId : audioIds) {
			Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(audioId));
			Music data = Music.createFromLocal(context, contentUri.toString(), contentUri, true, null);
			if (data != null)
				action.execute(data);
		}
	}

	public static void exportM3U(LibraryStore store, Context context, final Collection<Music> data) {
		// SAF export is handled by the playlist UI.
	}

	public static void exportM3U(Context context, final Collection<Music> data) {
		exportM3U(LibraryStore.get(), context, data);
	}
}
