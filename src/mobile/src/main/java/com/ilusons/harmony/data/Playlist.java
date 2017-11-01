package com.ilusons.harmony.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.ref.ArrayEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class Playlist extends RealmObject {

	// Logger TAG
	private static final String TAG = Playlist.class.getSimpleName();

	public static final String KEY_PLAYLIST_ALL = "all";
	public static final String KEY_PLAYLIST_MEDIASTORE = "mediastore";
	public static final String KEY_PLAYLIST_STORAGE = "storage";

	@PrimaryKey
	private String Name;

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	private RealmList<Music> Items = new RealmList<>();

	public RealmList<Music> getItems() {
		if (Items == null)
			Items = new RealmList<>();
		return Items;
	}

	private int ItemIndex = -1;

	public int getItemIndex() {
		return ItemIndex;
	}

	public void setItemIndex(final int i) {
		ItemIndex = i;
		if (ItemIndex < 0 || ItemIndex >= Items.size())
			ItemIndex = -1;
	}

	private Long LinkedAndroidOSPlaylistId = -1L;

	public Long getLinkedAndroidOSPlaylistId() {
		return LinkedAndroidOSPlaylistId;
	}

	public void setLinkedAndroidOSPlaylistId(final Long id) {
		LinkedAndroidOSPlaylistId = id;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	public void add(Music item) {
		Items.add(item);
	}

	public void add(Music item, int index) {
		Items.add(index, item);
	}

	public void addIfNot(Music item) {
		if (!Items.contains(item))
			Items.add(item);
	}

	public void addAll(Collection<Music> items) {
		Items.addAll(items);
	}

	public void remove(Music item) {
		if (Items.contains(item))
			Items.remove(item);
	}

	public void clear() {
		Items.clear();
	}

	public void removeAll(Music item) {
		ArrayList<Music> toRemove = new ArrayList<>();
		for (Music i : Items) {
			if (i.equals(item))
				toRemove.add(i);
		}
		Items.removeAll(toRemove);
	}

	public void removeAll(Collection<Music> items) {
		Items.removeAll(items);
	}

	public void removeAllExceptCurrent() {
		try {
			Music current = Items.get(ItemIndex);
			Items.clear();
			if (current != null)
				Items.add(current);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void moveDown(Music item) {
		int i = Items.indexOf(item);
		if (i > -1) {
			ArrayEx.move(i, i + 1, Items);
		}
	}

	public void moveUp(Music item) {
		int i = Items.indexOf(item);
		if (i > -1) {
			ArrayEx.move(i, i - 1, Items);
		}
	}

	public void delete(Music item, final MusicService musicService, final Realm realm, boolean notify) {
		if (Items.contains(item))
			Items.remove(item);
		Music.delete(musicService, realm, item.getPath(), true);
	}

	public void delete(Music item, final MusicService musicService, boolean notify) {
		try (Realm realm = DB.getDB()) {
			delete(item, musicService, realm, notify);
		}
	}

	public Music getItem() {
		if (getItemIndex() >= 0 && getItemIndex() < Items.size())
			return Items.get(getItemIndex());
		return null;
	}

	public boolean isIn(Music item) {
		return Items.contains(item);
	}

	public static boolean update(final Realm realm, final Playlist playlist, boolean removeTrace) {
		boolean r = false;
		try {
			final ArrayList<Music> toRemove = new ArrayList<>();

			for (Music item : playlist.Items) {
				File file = (new File(item.getPath()));
				if (!file.exists())
					toRemove.add(item);
			}

			if (toRemove.size() > 0) {
				realm.beginTransaction();
				try {
					playlist.Items.removeAll(toRemove);

					if (removeTrace)
						for (Music item : toRemove)
							try {
								item.deleteFromRealm();
							} catch (Exception e) {
								e.printStackTrace();
							}

					realm.commitTransaction();
				} catch (Throwable e) {
					if (realm.isInTransaction()) {
						realm.cancelTransaction();
					} else {
						Log.w(TAG, e);
					}
					throw e;
				}

				r = true;
			}
		} catch (Exception e) {
			e.printStackTrace();

			r = true;
		}
		return r;
	}

	public static boolean scanNew(final Realm realm, final Context context, final Playlist playlist, final String path, final Uri contentUri, boolean fastMode) {
		try {
			// Check for validity
			if (!Music.isValid(path))
				return false;

			// Ignore if already present
			for (Music item : playlist.getItems())
				if (item.getPath().equals(path))
					return false;

			final Music newData = Music.decode(realm, context, path, contentUri, fastMode, null);

			if (newData == null)
				throw new Exception("Some error while decoding.");

			// Check constraints
			if (newData.getLength() > 0 && MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMinDuration(context) > newData.getLength()) {
				return false;
			}

			// Add it finally
			realm.beginTransaction();
			try {
				playlist.Items.add(newData);

				realm.copyToRealmOrUpdate(newData);

				realm.commitTransaction();
			} catch (Throwable e) {
				if (realm.isInTransaction()) {
					realm.cancelTransaction();
				} else {
					Log.w(TAG, e);
				}
				throw e;
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	public static RealmResults<Playlist> loadAllPlaylists(Realm realm) {
		return realm.where(Playlist.class).findAll();
	}

	public static ArrayList<Playlist> loadAllPlaylists() {
		ArrayList<Playlist> result = new ArrayList<>();

		try (Realm realm = DB.getDB()) {
			if (realm != null)
				result.addAll(realm.copyFromRealm(loadAllPlaylists(realm)));
		}

		return result;
	}

	public static Playlist loadOrCreatePlaylist(Realm realm, String name) {
		Playlist playlist = null;
		try {
			playlist = realm.where(Playlist.class).equalTo("Name", name).findFirst();
			if (playlist == null)
				throw new Exception("Not found. [" + name + "]");
		} catch (Exception e) {
			e.printStackTrace();
			try {
				realm.beginTransaction();

				playlist = realm.createObject(Playlist.class, name);

				realm.commitTransaction();
			} catch (Throwable e2) {
				if (realm.isInTransaction()) {
					realm.cancelTransaction();
				} else {
					Log.w(TAG, e2);
				}
			}
		}
		return playlist;
	}

	public static Playlist loadOrCreatePlaylist(String name) {
		try (Realm realm = DB.getDB()) {
			if (realm != null) {
				return realm.copyFromRealm(loadOrCreatePlaylist(realm, name));
			}
		}
		return null;
	}

	public static void savePlaylist(Realm realm, Playlist playlist) {
		try {
			realm.beginTransaction();

			realm.copyToRealmOrUpdate(playlist);

			realm.commitTransaction();
		} catch (Throwable e) {
			if (realm.isInTransaction()) {
				realm.cancelTransaction();
			} else {
				Log.w(TAG, e);
			}
		}
	}

	public static void savePlaylist(Playlist playlist) {
		try (Realm realm = DB.getDB()) {
			savePlaylist(realm, playlist);
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
				context.startService(musicServiceIntent);
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
			// Load OS playlist
			// Convert it to in-app, then load
			if (!(playlistId <= -1L)) {
				final ArrayList<Music> osDatas = new ArrayList<>();
				final Collection<String> audioIds = getAllAudioIdsInPlaylist(context.getContentResolver(), playlistId);
				try (Realm realm = DB.getDB()) {
					if (realm == null)
						throw new Exception("Realm error.");

					for (String audioId : audioIds) {
						Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(audioId));

						Music data = Music.decode(realm, context, null, contentUri, true, null);

						if (data != null) {
							osDatas.add(data);

							if (onProgress != null)
								onProgress.execute(osDatas);
						}
					}
				}

				Playlist playlist = loadOrCreatePlaylist(name);
				playlist.setLinkedAndroidOSPlaylistId(playlistId);
				playlist.clear();
				playlist.addAll(osDatas);
				savePlaylist(playlist);

				setActivePlaylist(context, name, notify);
				if (onSuccess != null) {
					onSuccess.execute(playlist);
				}
			}

			// Load in-app playlist
			else {
				setActivePlaylist(context, name, notify);
				if (onSuccess != null) {
					onSuccess.execute(loadOrCreatePlaylist(name));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			if (onError != null)
				onError.execute(e);
		}
	}

	public static void delete(
			final Context context,
			final String name,
			final Long playlistId,
			final boolean notify) {
		try {
			if (!(playlistId <= -1L)) {
				deletePlaylist(context.getContentResolver(), playlistId);
			} else {
				try (Realm realm = DB.getDB()) {
					if (realm != null)
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								try {
									Playlist playlist = realm.where(Playlist.class).equalTo("Name", name).findFirst();
									if (playlist != null)
										playlist.deleteFromRealm();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
				}
			}

			if (getActivePlaylist(context).equalsIgnoreCase(name))
				setActivePlaylist(context, KEY_PLAYLIST_ALL, notify);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//region OS

	public static void allPlaylist(ContentResolver cr, final JavaEx.ActionTU<Long, String> action) {
		if (action == null)
			return;

		try {
			Cursor cursor = cr.query(
					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
					new String[]{
							MediaStore.Audio.Playlists._ID,
							MediaStore.Audio.Playlists.NAME
					},
					null,
					null,
					MediaStore.Audio.Playlists.NAME + " ASC");
			if (cursor != null) {
				int count = 0;
				count = cursor.getCount();

				if (count > 0) {
					while (cursor.moveToNext()) {
						Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
						String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME));

						action.execute(id, name);
					}

				}

				cursor.close();
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	public static long createPlaylist(ContentResolver cr, String playlistName) {
		long playlistId = -1;

		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName);
		contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
		contentValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

		Uri uri = cr.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues);

		if (uri != null) {
			Cursor cursor;
			cursor = cr.query(
					uri,
					new String[]{
							MediaStore.Audio.Playlists._ID,
							MediaStore.Audio.Playlists.NAME
					},
					null,
					null,
					null);
			if (cursor != null) {
				playlistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));

				cursor.close();

				Log.d(TAG, "Created playlist [" + playlistName + "], [" + playlistId + "].");
			} else {
				Log.w(TAG, "Creating playlist failed [" + playlistName + "], [" + playlistId + "].");
			}
		}

		return playlistId;
	}

	public static void deletePlaylist(ContentResolver cr, Long playlistId) {
		cr.delete(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
				MediaStore.Audio.Playlists._ID + "=?",
				new String[]{
						Long.toString(playlistId)
				});
	}

	public static void renamePlaylist(ContentResolver cr, Long playlistId, String playlistName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName);

		cr.update(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
				contentValues,
				MediaStore.Audio.Playlists._ID + " =? ",
				new String[]{
						Long.toString(playlistId)
				});
	}

	public static long getPlaylistIdFor(ContentResolver cr, String playlistName, boolean autoCreate) {
		long playlistId = -1;

		Cursor cursor = cr.query(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
				new String[]{
						MediaStore.Audio.Playlists._ID,
						MediaStore.Audio.Playlists.NAME
				},
				null,
				null,
				MediaStore.Audio.Playlists.NAME + " ASC");
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();

			if (count > 0) {
				while (cursor.moveToNext()) {
					long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
					String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME));

					if (name.equalsIgnoreCase(playlistName)) {
						playlistId = id;

						break;
					}
				}

			}

			cursor.close();
		}

		if (autoCreate && playlistId == -1) {
			Log.d(TAG, "Creating playlist [" + playlistName + "], no previous record.");

			playlistId = createPlaylist(cr, playlistName);
		}

		Log.d(TAG, "Playlist [" + playlistName + "], [" + playlistId + "].");


		return playlistId;
	}

	public static Collection<String> getAllAudioIdsInPlaylist(ContentResolver cr, long playlistId) {
		ArrayList<String> result = new ArrayList<>();

		Cursor cursor = cr.query(
				MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
				new String[]{
						MediaStore.Audio.Playlists.Members.AUDIO_ID
				},
				MediaStore.Audio.Media.IS_MUSIC + " != 0 ",
				null,
				null,
				null);
		if (cursor != null) {
			int count = 0;
			count = cursor.getCount();

			if (count > 0) {
				while (cursor.moveToNext()) {
					String audio_id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));

					result.add(audio_id);
				}

			}

			cursor.close();
		}

		return result;
	}

	public static void getAllMusicForIds(Realm realm, Context context, Collection<String> audioIds, JavaEx.ActionT<Music> action) {
		if (action == null)
			return;

		for (String audioId : audioIds) {
			Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(audioId));

			Music data = Music.decode(realm, context, null, contentUri, true, null);

			if (data != null)
				action.execute(data);
		}
	}

	public static void addToPlaylist(ContentResolver cr, long playlistId, Collection<Integer> audioIds) {
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
		Cursor cursor = cr.query(
				uri,
				new String[]{
						MediaStore.Audio.Playlists.Members.PLAY_ORDER
				},
				null,
				null,
				null);
		if (cursor != null) {
			cursor.moveToLast();

			final int base = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER));

			cursor.close();

			int play_order = base;
			for (int audioId : audioIds) {
				ContentValues values = new ContentValues();
				play_order++;
				values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, play_order);
				values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);

				cr.insert(uri, values);
			}
		}
	}

	public static void removeFromPlaylist(ContentResolver cr, long playlistId, Collection<Integer> audioIds) {
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);

		for (int audioId : audioIds)
			cr.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + " = " + audioId, null);
	}

	//endregion

	//region Extra

	public static final String KEY_PLAYLIST_CURRENT_EXP_M3U = "harmony.m3u";

	public static void exportM3U(Realm realm, Context context, final Collection<Music> data) {
		try {
			String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();

			//noinspection ConstantConditions
			String base = (new File(root)).getAbsolutePath();
			String nl = System.getProperty("line.separator");
			StringBuilder sb = new StringBuilder();
			for (Music music : data) {
				String url = IOEx.getRelativePath(base, music.getPath());
				sb.append(url).append(nl);
			}

			File file = new File(root);
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
			file = new File(root + "/" + KEY_PLAYLIST_CURRENT_EXP_M3U);

			FileUtils.writeStringToFile(file, sb.toString(), "utf-8", false);

			MediaScannerConnection.scanFile(context,
					new String[]{
							file.toString()
					},
					null,
					new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) {
							Log.i(TAG, "Scanned " + path + ", uri=" + uri);
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void exportM3U(Context context, final Collection<Music> data) {
		try (Realm realm = DB.getDB()) {
			exportM3U(realm, context, data);
		}
	}

	//endregion

}
