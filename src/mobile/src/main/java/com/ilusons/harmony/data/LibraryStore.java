package com.ilusons.harmony.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory library used after dropping Realm.
 * Room persistence is added in a later phase; this keeps the existing Music/Playlist API compiling.
 */
public final class LibraryStore {

	private static final LibraryStore INSTANCE = new LibraryStore();

	private final ConcurrentHashMap<String, Music> tracks = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Playlist> playlists = new ConcurrentHashMap<>();

	private LibraryStore() {
	}

	public static LibraryStore get() {
		return INSTANCE;
	}

	public static void init(Context context) {
		// Reserved for Room / disk hydrate.
	}

	public Music getTrack(String path) {
		if (path == null) {
			return null;
		}
		return tracks.get(path);
	}

	public boolean hasTrack(String path) {
		return path != null && tracks.containsKey(path);
	}

	public void upsert(Music music) {
		if (music == null || music.getPath() == null) {
			return;
		}
		tracks.put(music.getPath(), music);
	}

	public void removeTrack(String path) {
		if (path != null) {
			tracks.remove(path);
		}
	}

	public long trackCount() {
		return tracks.size();
	}

	public List<Music> allTracks() {
		return new ArrayList<>(tracks.values());
	}

	public Playlist getPlaylist(String name) {
		if (name == null) {
			return null;
		}
		return playlists.get(name);
	}

	public Playlist getOrCreatePlaylist(String name) {
		if (name == null) {
			name = Playlist.KEY_PLAYLIST_ALL;
		}
		Playlist existing = playlists.get(name);
		if (existing != null) {
			return existing;
		}
		Playlist created = new Playlist();
		created.setName(name);
		playlists.put(name, created);
		return created;
	}

	public void savePlaylist(Playlist playlist) {
		if (playlist == null || playlist.getName() == null) {
			return;
		}
		playlists.put(playlist.getName(), playlist);
	}

	public void deletePlaylist(String name) {
		if (name != null) {
			playlists.remove(name);
		}
	}

	public Collection<Playlist> allPlaylists() {
		return Collections.unmodifiableCollection(playlists.values());
	}
}
