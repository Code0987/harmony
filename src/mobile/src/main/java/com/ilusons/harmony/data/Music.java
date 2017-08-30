package com.ilusons.harmony.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.LyricsEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.SongsEx;
import com.ilusons.harmony.views.LyricsViewFragment;
import com.ilusons.harmony.views.PlaybackUIActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class Music extends RealmObject {

	// Logger TAG
	private static final String TAG = Music.class.getSimpleName();

	public static final String KEY_CACHE_DIR_COVER = "covers";
	public static final String KEY_CACHE_DIR_LYRICS = "lyrics";

	// Basic
	public String Title = "";
	public String Artist = "";
	public String Album = "";
	public Integer Length = -1;
	public Integer Track = -1;
	@PrimaryKey
	public String Path;
	public String Playlists = "";

	// Stats
	public Integer Played = 0;
	public Long TimeLastPlayed = -1L;
	public Long TotalDurationPlayed = 0L;
	public Integer Skipped = 0;
	public Long TimeLastSkipped = -1L;
	public Long TimeAdded = -1L;
	public String Mood = "";

	public Double Score = 0.0;

	@Override
	public boolean equals(Object obj) {
		Music other = (Music) obj;

		if (other == null)
			return false;

		if (Path.equals(other.Path))
			return true;

		return false;
	}

	public void addPlaylist(String playlist) {
		if (!Playlists.contains(playlist))
			Playlists += playlist;
	}

	public void removePlaylist(String playlist) {
		if (Playlists.contains(playlist))
			Playlists = Playlists.replace(playlist, "");
	}

	public boolean isInPlaylist(String playlist) {
		return Playlists.contains(playlist);
	}

	public String getText() {
		return TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title;
	}

	public String getTextExtraOnly(String del) {
		StringBuilder sb = new StringBuilder();

		if (Track > -1) {
			sb.append("#");
			sb.append(Track);
		}
		if (!TextUtils.isEmpty(Album)) {
			if (sb.length() > 0)
				sb.append(del);
			sb.append(Album);
		}
		if (!TextUtils.isEmpty(Artist)) {
			if (sb.length() > 0)
				sb.append(del);
			sb.append(Artist);
		}

		return sb.toString();
	}

	public String getTextExtraOnlySingleLine() {
		return getTextExtraOnly(" • ");
	}

	public String getTextDetailed(String del) {
		StringBuilder sb = new StringBuilder();

		if (Track > -1) {
			sb.append("#");
			sb.append(Track);
			sb.append(del);
		}
		sb.append(Title);
		if (!TextUtils.isEmpty(Artist)) {
			sb.append(del);
			sb.append("\uD83C\uDFA4 ").append(Artist);
		}
		if (!TextUtils.isEmpty(Album)) {
			sb.append(del);
			sb.append(Album);
		}
		if (Length > -1) {
			sb.append(del);
			sb.append("⏳ ");
			sb.append(DurationFormatUtils.formatDuration(Length, "mm:ss", false));
			sb.append("/");
			sb.append(DurationFormatUtils.formatDuration(TotalDurationPlayed, "mm:ss", false));
		}
		sb.append(del);
		sb.append("\uD83C\uDFB5 ").append(Played).append("/").append(Skipped);
		if (TimeAdded > -1) {
			sb.append(del);
			sb.append("\uD83D\uDCC5 ").append(DateFormat.getDateInstance(DateFormat.SHORT).format(TimeAdded));
		}

		return sb.toString();
	}

	public String getTextDetailedSingleLine() {
		return getTextDetailed(" • ");
	}

	public String getTextDetailedMultiLine() {
		return getTextDetailed(System.getProperty("line.separator"));
	}

	//region Cover art

	public Bitmap getCover(final Context context, int size) {
		Bitmap result;

		String key = Path;
		if (size > 0)
			key = key + size;

		// Load from cache
		result = CacheEx.getInstance().getBitmap(key);

		if (result != null)
			return result;

		// File
		File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, Path);

		// Load from cache folder
		if (file.exists())
			result = BitmapFactory.decodeFile(file.getAbsolutePath());

		// Re-sample
		if (result != null) {
			try {
				Bitmap.Config config = result.getConfig();
				if (config == null) {
					config = Bitmap.Config.ARGB_8888;
				}
				result = result.copy(config, false);
			} catch (Exception e) {
				Log.w(TAG, e);
			}

			// Re-size
			if (result != null && size > 0) {
				size = Math.min(size, Math.max(result.getWidth(), result.getHeight()));

				result = Bitmap.createScaledBitmap(result, size, size, true);
			}

			// Put in cache
			CacheEx.getInstance().putBitmap(key, result);
		}

		return result;
	}

	public Bitmap getCover(final Context context) {
		return getCover(context, -1);
	}

	private static WeakReference<PlaybackUIActivity> currentCoverView = null;

	public static PlaybackUIActivity getCurrentCoverView() {
		return currentCoverView.get();
	}

	public static void setCurrentCoverView(PlaybackUIActivity v) {
		currentCoverView = new WeakReference<PlaybackUIActivity>(v);
	}

	private static AsyncTask<Object, Object, Bitmap> getCoverOrDownloadTask = null;

	public static void getCoverOrDownload(final int size, final Music data) {
		if (getCoverOrDownloadTask != null) {
			getCoverOrDownloadTask.cancel(true);
			try {
				getCoverOrDownloadTask.get(1, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			getCoverOrDownloadTask = null;
		}
		getCoverOrDownloadTask = (new AsyncTask<Object, Object, Bitmap>() {
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (getCurrentCoverView() != null)
					getCurrentCoverView().onCoverReloaded(bitmap);
			}

			@Override
			protected Bitmap doInBackground(Object... objects) {
				try {
					if (isCancelled() || getCurrentCoverView() == null)
						throw new CancellationException();

					Bitmap result = data.getCover(getCurrentCoverView(), size);

					// File
					File file = IOEx.getDiskCacheFile(getCurrentCoverView(), KEY_CACHE_DIR_COVER, data.Path);

					if (isCancelled())
						throw new CancellationException();

					// Download and cache to folder then load
					if (result == null) {
						try {
							URL url = new URL(String.format(
									"https://itunes.apple.com/search?term=%s&entity=song&media=music",
									URLEncoder.encode(data.getText(), "UTF-8")));

							Connection connection = Jsoup.connect(url.toExternalForm())
									.timeout(3 * 1000)
									.ignoreContentType(true);

							Document document = connection.get();

							JsonObject response = new JsonParser().parse(document.text()).getAsJsonObject();

							JsonArray results = response.getAsJsonArray("results");

							String downloadUrl = results
									.get(0)
									.getAsJsonObject()
									.get("artworkUrl60")
									.getAsString()
									.replace("60x60bb.jpg", "1000x1000bb.jpg");

							BufferedInputStream in = null;
							FileOutputStream out = null;
							try {
								in = new BufferedInputStream(new URL(downloadUrl).openStream());
								out = new FileOutputStream(file.getAbsoluteFile());

								final byte data[] = new byte[1024];
								int count;
								while ((count = in.read(data, 0, 1024)) != -1) {
									out.write(data, 0, count);
								}
							} finally {
								if (in != null) {
									in.close();
								}
								if (out != null) {
									out.close();
								}
							}

						} catch (Exception e) {
							Log.w(TAG, e);
						}

						if (file.exists())
							result = BitmapFactory.decodeFile(file.getAbsolutePath());

						// Refresh once more
						if (result == null && getCurrentCoverView() != null) {
							data.refresh(getCurrentCoverView());

							result = data.getCover(getCurrentCoverView(), size);
						}

						// Resample
						if (result != null) {
							try {
								Bitmap.Config config = result.getConfig();
								if (config == null) {
									config = Bitmap.Config.ARGB_8888;
								}
								result = result.copy(config, false);
							} catch (Exception e) {
								Log.w(TAG, e);
							}

							// Put in cache
							CacheEx.getInstance().putBitmap(data.Path, result);
						}

					}

					return result;
				} catch (Exception e) {
					Log.w(TAG, e);
				}
				return null;
			}
		});
		getCoverOrDownloadTask.execute();
	}

	public static void putCover(Context context, Music data, Bitmap bmp) {
		IOEx.putBitmapInDiskCache(context, KEY_CACHE_DIR_COVER, data.Path, bmp);
	}

	//endregion

	//region Lyrics

	public String getLyrics(final Context context) {
		String result;

		// Load from cache
		result = (String) CacheEx.getInstance().get(KEY_CACHE_DIR_LYRICS + Path);

		if (result != null)
			return result;

		// File
		File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

		// Load from cache folder
		if (file.exists()) try {
			result = FileUtils.readFileToString(file, Charset.defaultCharset());
		} catch (Exception e) {
			Log.w(TAG, e);
		}

		if (result != null) {
			// Put in cache
			CacheEx.getInstance().put(KEY_CACHE_DIR_LYRICS + Path, result);
		}

		return result;
	}

	public File getLyricsFile(final Context context) {
		File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

		// Load from cache folder
		if (!file.exists()) try {
			file.createNewFile();
		} catch (Exception e) {
			Log.w(TAG, e);
		}

		return file;
	}

	private static WeakReference<LyricsViewFragment> currentLyricsView = null;

	public static LyricsViewFragment getCurrentLyricsView() {
		return currentLyricsView.get();
	}

	public static void setCurrentLyricsView(LyricsViewFragment v) {
		currentLyricsView = new WeakReference<LyricsViewFragment>(v);
	}

	private static AsyncTask<Void, Void, String> getLyricsOrDownloadTask = null;

	@SuppressLint("StaticFieldLeak")
	public static void getLyricsOrDownload(final Music data) {
		if (getLyricsOrDownloadTask != null) {
			getLyricsOrDownloadTask.cancel(true);
			try {
				getLyricsOrDownloadTask.get(1, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			getLyricsOrDownloadTask = null;
		}
		getLyricsOrDownloadTask = (new AsyncTask<Void, Void, String>() {
			@Override
			protected void onPostExecute(String result) {
				if (getCurrentLyricsView() != null)
					getCurrentLyricsView().onReloaded(result);
			}

			@Override
			protected String doInBackground(Void... Voids) {
				try {
					if (isCancelled() || getCurrentLyricsView() == null)
						throw new CancellationException();

					String result = data.getLyrics(getCurrentLyricsView().getActivity());

					if (!TextUtils.isEmpty(result))
						return result;

					// Refresh once more
					if (result == null && getCurrentLyricsView().getActivity() != null) try {
						data.refresh(getCurrentLyricsView().getActivity());

						result = data.getLyrics(getCurrentLyricsView().getActivity());

						if (!TextUtils.isEmpty(result))
							return result;
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Fetch
					try {
						if (isCancelled())
							throw new CancellationException();

						LyricsEx.Lyrics lyrics = LyricsEx.ViewLyricsApi.get(data.Title, data.Artist);

						result = lyrics.Content;

						data.putLyrics(getCurrentLyricsView().getActivity(), result);
					} catch (Exception e) {
						Log.w(TAG, e);
					}

					// Fetch
					if (TextUtils.isEmpty(result))
						try {
							if (isCancelled())
								throw new CancellationException();

							ArrayList<LyricsEx.Lyrics> results = LyricsEx.GeniusApi.get(data.getText());

							if (!(results == null || results.size() == 0)) {
								for (LyricsEx.Lyrics lyrics : results) {

									float t = (1f - (StringUtils.getLevenshteinDistance(lyrics.Title, data.Title) / Math.max(lyrics.Title.length(), data.Title.length())));
									float a = (1f - (StringUtils.getLevenshteinDistance(lyrics.Artist, data.Artist) / Math.max(lyrics.Artist.length(), data.Artist.length())));

									if (t >= 0.8 && a >= 0.8) {
										result = lyrics.Content;
										break;
									}
								}
							}

							if (isCancelled())
								throw new CancellationException();

							data.putLyrics(getCurrentLyricsView().getActivity(), result);
						} catch (Exception e) {
							Log.w(TAG, e);
						}

					if (TextUtils.isEmpty(result)) {
						result = "";

						data.putLyrics(getCurrentLyricsView().getActivity(), "");
					}

					return result;
				} catch (Exception e) {
					Log.w(TAG, e);
				}

				return null;
			}
		});
		getLyricsOrDownloadTask.execute();
	}

	public void putLyrics(Context context, String content) {
		// File
		File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

		try {
			FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	//endregion

	//region Decoding

	public static Music decode(Realm realm, Context context, String path, boolean fastMode, Music oldData) {
		Music data = oldData;

		realm.beginTransaction();
		try {
			// Get data
			if (data == null) {
				RealmResults<Music> realmResults = get(realm, path);
				if (!(realmResults == null || realmResults.size() == 0))
					data = realmResults.get(0);
				else
					data = new Music();

				data.TimeAdded = System.currentTimeMillis();
				data.TimeLastPlayed = System.currentTimeMillis();
				data.TimeLastSkipped = System.currentTimeMillis();
			}

			// HACK: Calling the devil
			System.gc();
			Runtime.getRuntime().gc();

			// Metadata from system

			if (Looper.myLooper() == null) try {
				Looper.prepare(); // HACK
			} catch (Exception e) {
				Log.w(TAG, e);
			}

			if (Looper.myLooper() != null) {

				if (path.toLowerCase().startsWith("content") && path.toLowerCase().contains("audio")) {
					Uri contentUri = Uri.parse(path);
					Cursor cursor = null;

					try {
						String[] projection = {
								MediaStore.Audio.Media.DATA,
								MediaStore.Audio.Media.IS_MUSIC,
								MediaStore.Audio.Media.TITLE,
								MediaStore.Audio.Media.ARTIST,
								MediaStore.Audio.Media.ALBUM,
								MediaStore.Audio.Media.DURATION
						};

						CursorLoader loader = new CursorLoader(context, contentUri, projection, null, null, null);

						cursor = loader.loadInBackground();

						cursor.moveToFirst();

						int isMusic = 1;
						try {
							isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
						} catch (Exception e) {
							// Eat
						}

						if (isMusic != 0) {

							try {
								data.Title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
							} catch (Exception e) {
								// Eat
							}
							try {
								data.Artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
							} catch (Exception e) {
								// Eat
							}
							try {
								data.Album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
							} catch (Exception e) {
								// Eat
							}
							try {
								data.Length = (int) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
							} catch (Exception e) {
								// Eat
							}
							try {
								data.Length = (int) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK));
							} catch (Exception e) {
								// Eat
							}

							path = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))).getPath();

							data.Path = path;

							MediaMetadataRetriever mmr = new MediaMetadataRetriever();
							try {
								mmr.setDataSource(path);

								byte[] cover = mmr.getEmbeddedPicture();
								if (cover != null && cover.length > 0) {
									Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);
									if (bmp != null)
										putCover(context, data, bmp);
								}
							} catch (Exception e) {
								Log.w(TAG, "metadata from system - getEmbeddedPicture", e);
							} finally {
								mmr.release();
							}

						}
					} catch (Exception e) {
						Log.w(TAG, "metadata from system", e);
					} finally {
						if (cursor != null)
							cursor.close();
					}
				}

				if (path.toLowerCase().startsWith("content") && path.toLowerCase().contains("video")) {
					Uri contentUri = Uri.parse(path);
					Cursor cursor = null;

					try {
						String[] projection = {
								MediaStore.Video.Media.DATA,
								MediaStore.Video.Media.TITLE,
								MediaStore.Video.Media.ARTIST,
								MediaStore.Video.Media.ALBUM,
								MediaStore.Video.Media.DURATION
						};

						CursorLoader loader = new CursorLoader(context, contentUri, projection, null, null, null);

						cursor = loader.loadInBackground();

						cursor.moveToFirst();

						try {
							data.Title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
						} catch (Exception e) {
							// Eat
						}
						try {
							data.Artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));
						} catch (Exception e) {
							// Eat
						}
						try {
							data.Album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM));
						} catch (Exception e) {
							// Eat
						}
						try {
							data.Length = (int) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
						} catch (Exception e) {
							// Eat
						}

						path = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))).getPath();

						data.Path = path;

						if (!fastMode) {
							MediaMetadataRetriever mmr = new MediaMetadataRetriever();
							try {
								mmr.setDataSource(path);

								Bitmap bmp = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
								if (bmp != null)
									putCover(context, data, bmp);
							} catch (Exception e) {
								Log.w(TAG, "metadata from system - getEmbeddedPicture", e);
							} finally {
								mmr.release();
							}
						}


					} catch (Exception e) {
						Log.w(TAG, "metadata from system", e);
					} finally {
						if (cursor != null)
							cursor.close();
					}
				}
			}

			// Metadata from tags
			if (!fastMode && !path.toLowerCase().startsWith("content")) {
				File file = new File(path);
				if (file.length() < 100 * 1024 * 1024)
					try {
						AudioFile audioFile = AudioFileIO.read(file);

						Tag tag = audioFile.getTagAndConvertOrCreateAndSetDefault();

						data.Title = tag.getFirst(FieldKey.TITLE);
						data.Artist = tag.getFirst(FieldKey.ARTIST);
						data.Album = tag.getFirst(FieldKey.ALBUM);
						try {
							data.Length = Integer.valueOf(tag.getFirst(FieldKey.LENGTH));
						} catch (Exception e) {
							// Ignore
						}
						try {
							data.Track = Integer.valueOf(tag.getFirst(FieldKey.TRACK));
						} catch (Exception e) {
							// Ignore
						}

						if (data.getCover(context) == null) {
							Artwork artwork = tag.getFirstArtwork();
							if (artwork != null) {
								byte[] cover = artwork.getBinaryData();
								if (cover != null && cover.length > 0) {
									Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);

									if (bmp != null)
										putCover(context, data, bmp);
								}
							}
						}

						// Lyrics
						String lyrics;
						lyrics = tag.getFirst(FieldKey.LYRICS);
						if (!TextUtils.isEmpty(lyrics))
							data.putLyrics(context, lyrics);
						lyrics = tag.getFirst(FieldKey.USER_UNSYNCED_LYRICS);
						if (!TextUtils.isEmpty(lyrics))
							data.putLyrics(context, lyrics);
						lyrics = tag.getFirst(FieldKey.USER_LYRICS);
						if (!TextUtils.isEmpty(lyrics))
							data.putLyrics(context, lyrics);

					} catch (OutOfMemoryError e) {
						Log.wtf(TAG, "OOM", e);
					} catch (Exception e) {
						Log.w(TAG, "metadata from tags", e);
						Log.w(TAG, "file\n" + file);
					}
				else {
					Log.w(TAG, "file\n" + file);
				}
			}

			if (TextUtils.isEmpty(data.Title) || TextUtils.isEmpty(data.Artist)) try {
				ArrayList<String> at = SongsEx.getArtistAndTitle((new File(path)).getName());

				data.Artist = at.get(0);
				data.Title = at.get(1);
			} catch (Exception e) {
				data.Title = (new File(path)).getName().replaceFirst("[.][^.]+$", "");
			}

			Log.d(TAG, "added to library\n" + path);

			// HACK: Calling the devil
			System.gc();
			Runtime.getRuntime().gc();

			data.Path = path;

			realm.copyToRealmOrUpdate(data);

			// Check constraints
			if (!(data.hasAudio() || data.hasVideo())) {
				try {
					data.deleteFromRealm();
				} catch (Exception e) {
					e.printStackTrace();
				}

				data = null;
			}

			realm.commitTransaction();

			if (data != null)
				try {
					RealmResults<Music> realmResults = get(realm, data.Path);
					if (!(realmResults == null || realmResults.size() == 0))
						data = realm.copyFromRealm(realmResults.get(0));
				} catch (Exception e) {
					Log.w(TAG, e);
				}

		} catch (Throwable e) {
			if (realm.isInTransaction()) {
				realm.cancelTransaction();
			} else {
				Log.w(TAG, "Could not cancel transaction, not currently in a transaction.");
			}
			throw e;
		}

		return data;
	}

	public void refresh(Context context) {
		try (Realm realm = getDB()) {
			Music.decode(realm, context, Path, false, this);
		}
	}

	//endregion

	//region Extensions

	private static String[] extensions_audio = new String[]{
			".mp3",
			".m4a",
			".wav",
			".flac",
			".ogg",
			".wma",
			".ape",
			".wv",
			".tta",
			".mpc",
			".aiff",
			".asf",
	};

	public static boolean isAudio(String extension) {
		for (String ext : extensions_audio) {
			if (extension.equalsIgnoreCase(ext))
				return true;
		}
		return false;
	}

	public boolean hasAudio() {
		try {
			return !TextUtils.isEmpty(Path) && isAudio(Path.substring(Path.lastIndexOf(".")));
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	private static String[] extensions_video = new String[]{
			".mp4",
			".m4v",
			".mkv",
			".avi",
			".webm",
			".flv",
	};

	public static boolean isVideo(String extension) {
		for (String ext : extensions_video) {
			if (extension.equalsIgnoreCase(ext))
				return true;
		}
		return false;
	}

	public boolean hasVideo() {
		try {
			return !TextUtils.isEmpty(Path) && isVideo(Path.substring(Path.lastIndexOf(".")));
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}
	//endregion

	//region Playlist

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
			String path = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(audioId)).toString();

			Music m = decode(realm, context, path, true, null);

			if (m != null)
				action.execute(m);
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

	//region DB

	public static Realm getDB() {
		try {
			RealmConfiguration config = new RealmConfiguration.Builder()
					.name("music.realm")
					.deleteRealmIfMigrationNeeded()
					.build();

			return Realm.getInstance(config);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static RealmResults<Music> getAll(Realm realm) {
		RealmResults<Music> result = realm.where(Music.class).findAll();

		return result;
	}

	public static RealmResults<Music> get(Realm realm, String path) {
		RealmResults<Music> result = realm.where(Music.class).equalTo("Path", path).findAll();

		return result;
	}

	public static final String KEY_PLAYLIST_MEDIASTORE = "mediastore";
	public static final String KEY_PLAYLIST_STORAGE = "storage";
	public static final String KEY_PLAYLIST_CURRENT = "current";

	public static RealmResults<Music> getAllInPlaylist(Realm realm, String playlist) {
		RealmResults<Music> result = realm
				.where(Music.class)
				.contains("Playlists", playlist, Case.INSENSITIVE)
				.findAll();

		return result;
	}

	public static Music load(Realm realm, Context context, String path) {
		RealmResults<Music> realmResults = get(realm, path);

		Music m = null;
		if (realmResults == null || realmResults.size() == 0)
			m = decode(realm, context, path, false, null);
		else
			m = realm.copyFromRealm(realmResults.get(0));

		return m;
	}

	public static Music load(Context context, String path) {
		try (Realm realm = getDB()) {
			return load(realm, context, path);
		}
	}

	public static ArrayList<Music> loadAll(Realm realm) {
		ArrayList<Music> result = new ArrayList<>();

		result.addAll(realm.copyFromRealm(getAllInPlaylist(realm, KEY_PLAYLIST_MEDIASTORE)));
		result.addAll(realm.copyFromRealm(getAllInPlaylist(realm, KEY_PLAYLIST_STORAGE)));

		return result;
	}

	public static ArrayList<Music> loadAll() {
		try (Realm realm = getDB()) {
			return loadAll(realm);
		}
	}

	public static ArrayList<Music> loadCurrent(Realm realm) {
		ArrayList<Music> result = new ArrayList<>();

		RealmResults<Music> realmResults = getAllInPlaylist(realm, KEY_PLAYLIST_CURRENT);

		result.addAll(realm.copyFromRealm(realmResults));

		if (result.size() == 0)
			result.addAll(loadAll(realm));

		return result;
	}

	public static ArrayList<Music> loadCurrent() {
		try (Realm realm = getDB()) {
			return loadCurrent(realm);
		}
	}

	public static final String KEY_PLAYLIST_CURRENT_EXP_M3U = "harmony.m3u";

	public static void saveCurrent(Realm realm, Context context, final Collection<Music> data, boolean notify) {
		try {
			try {
				ArrayList<String> items = new ArrayList<>();
				for (Music item : data)
					items.add(item.Path);

				setPlaylistCurrentSortOrder(context, items, notify, notify);
			} catch (Exception e) {
				e.printStackTrace();
			}

			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					for (Music item : data) {
						if (!item.isInPlaylist(KEY_PLAYLIST_CURRENT))
							item.addPlaylist(KEY_PLAYLIST_CURRENT);
					}

					realm.copyToRealmOrUpdate(data);
				}
			});

			String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();

			//noinspection ConstantConditions
			String base = (new File(root)).getAbsolutePath();
			String nl = System.getProperty("line.separator");
			StringBuilder sb = new StringBuilder();
			for (Music music : data) {
				String url = IOEx.getRelativePath(base, music.Path);
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

		if (notify) {
			Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATED);
			LocalBroadcastManager
					.getInstance(context)
					.sendBroadcast(broadcastIntent);

			Intent musicServiceIntent = new Intent(context, MusicService.class);
			musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATED);
			context.startService(musicServiceIntent);
		}
	}

	public static void saveCurrent(Context context, final Collection<Music> data, boolean notify) {
		try (Realm realm = getDB()) {
			saveCurrent(realm, context, data, notify);
		}
	}

	public static void sort(List<Music> data, List<String> keys) {
		if (keys == null)
			return;
		if (keys.size() == 0)
			return;

		Map<String, List<Music>> map = new HashMap<>();
		for (String string : keys)
			map.put(string, new ArrayList<Music>());
		for (Music item : data) {
			List<Music> value = map.get(item.Path);
			if (value != null)
				value.add(item);
		}
		data.clear();
		for (String string : keys)
			for (Music item : map.get(string))
				data.add(item);
	}

	public static final String TAG_SPREF_PLAYLIST_CURRENT_SORT_ORDER = SPrefEx.TAG_SPREF + ".playlist_current_sort_order";

	public static List<String> getPlaylistCurrentSortOrder(Context context) {
		String s = SPrefEx.get(context).getString(TAG_SPREF_PLAYLIST_CURRENT_SORT_ORDER, null);
		if (TextUtils.isEmpty(s))
			return null;
		return new ArrayList<String>(Arrays.asList(s.split(";")));
	}

	public static void setPlaylistCurrentSortOrder(Context context, Collection<String> data, boolean notifyUI, boolean notifyService) {
		StringBuilder value = new StringBuilder();
		for (String item : data)
			value.append(item).append(";");

		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PLAYLIST_CURRENT_SORT_ORDER, value.toString())
				.apply();

		if (notifyUI) {
			Intent broadcastIntent = new Intent(MusicService.ACTION_PLAYLIST_CURRENT_UPDATED);
			LocalBroadcastManager
					.getInstance(context)
					.sendBroadcast(broadcastIntent);
		}

		if (notifyService) {
			Intent musicServiceIntent = new Intent(context, MusicService.class);
			musicServiceIntent.setAction(MusicService.ACTION_PLAYLIST_CURRENT_UPDATED);
			context.startService(musicServiceIntent);
		}
	}

	public static ArrayList<Music> loadCurrentSorted(Realm realm, Context context) {
		ArrayList<Music> data = loadCurrent(realm);

		sort(data, getPlaylistCurrentSortOrder(context));

		return data;
	}

	public static ArrayList<Music> loadCurrentSorted(Context context) {
		ArrayList<Music> data = loadCurrent();

		sort(data, getPlaylistCurrentSortOrder(context));

		return data;
	}

	public static void delete(final MusicService musicService, final Realm realm, final Music data, boolean notify) {
		try {
			if (musicService.getCurrentPlaylistItemMusic().Path.equals(data.Path))
				musicService.next();

			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class)
							.equalTo("Path", data.Path, Case.INSENSITIVE)
							.findAll()
							.deleteAllFromRealm();
				}
			});

			(new File(data.Path)).deleteOnExit();

			List<String> playlistCurrentSortOrder = getPlaylistCurrentSortOrder(musicService);
			if (playlistCurrentSortOrder != null) {
				playlistCurrentSortOrder.remove(data.Path);
				setPlaylistCurrentSortOrder(musicService, playlistCurrentSortOrder, notify, notify);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void delete(final MusicService musicService, final Music data, boolean notify) {
		try (Realm realm = getDB()) {
			delete(musicService, realm, data, notify);
		}
	}

	//endregion

	//region Smart functions

	public static double getScore(Music music) {
		double score;

		int daysSinceLastPlayed = (int) ((System.currentTimeMillis() - music.TimeLastPlayed) / (1000 * 60 * 60 * 24));
		int daysSinceLastSkipped = (int) ((System.currentTimeMillis() - music.TimeLastSkipped) / (1000 * 60 * 60 * 24));
		int daysSinceAdded = (int) ((System.currentTimeMillis() - music.TimeAdded) / (1000 * 60 * 60 * 24));
		int length = music.Length;
		double lengthFixed = Math.round((length + 540) / 4) + Math.round((length * length) / ((length > 3599) ? Math.round((9000 * length) / 3600) : 9000));
		double played = Math.pow(music.Played, 2) * lengthFixed;

		//Big_Berny_Formula_1 = "(10000000 * (7+OptPlayed-(Skip*0.98^(SongLength/60))^1.7)^3 / (10+DaysSinceFirstPlayed)^0.5) / (1+DaysSinceLastPlayed/365)"
		//Big_Berny_Formula_2 = "(10000000 * (7+OptPlayed-(Skip*0.98^(SongLength/60))^1.7)^3 / (10+DaysSinceFirstPlayed)^0.3) / (1+DaysSinceLastPlayed/730)"
		//Big_Berny_Formula_4 = "(10000000 * (7+Played-(Skip*0.98^(SongLength/60))^1.7)^3 / (10+DaysSinceFirstPlayed)^0.5) / (1+DaysSinceLastPlayed/365)"
		//Big_Berny_Formula_5 = "7+OptPlayed-(Skip*0.98^(SongLength/60))"
		//BerniPi_Formula_1 = "(500000000000+10000000000*(Played*0.999^((10+DaysSinceLastPlayed)/(Played/3+1))-Skip^1.7))/((10+DaysSinceFirstPlayed)/(Played^2+1))"
		//score = Int((10000000 * (7 + playedTime + (daysSinceLastSkipped / 365)^1.2 -(skipCount*0.98^(otrackLength/60))^1.7)^3 / (10 + daysSinceImported)^0.5) / ((daysSinceLastPlayed / 365) + 1))
		//score = Int(10000000 + (((playedTime - (skipCount*lengthFixed*0.971^(otrackLength/60)*0.8^(daysSinceLastSkipped / 365)))^3) / (30 + daysSinceImported)^0.5) / ((daysSinceLastPlayed / 365) + 1))

		score = (
				(
						(played -
								(Math.pow(music.Skipped, 2)
										*
										lengthFixed
										*
										Math.pow(0.9, (lengthFixed / 60))
										*
										Math.pow(0.6, (daysSinceLastSkipped / 365))
								)
						)
								/ Math.pow((30 + daysSinceAdded), 0.2)
				) * 100)
				/
				((Math.pow(daysSinceLastPlayed, 1.2) / 730) + 1);

		if (score < 0) {
			score = 0.0;
		}

		Log.d(TAG, "Score generated for [" + music.Path + "], " + score);

		return score;
	}

	//endregion

}
