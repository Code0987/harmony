package com.ilusons.harmony.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.ArtworkEx;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.LyricsEx;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

public class Music extends RealmObject {

	// Logger TAG
	private static final String TAG = Music.class.getSimpleName();

	public static final String KEY_CACHE_DIR_COVER = "covers";
	public static final String KEY_CACHE_DIR_LYRICS = "lyrics";

	// Basic
	@PrimaryKey
	private String Id = (UUID.randomUUID().toString());

	public String getId() {
		return Id;
	}

	public void setId(String id) {
		Id = id;
	}

	private String Title = "";

	public String getTitle() {
		return Title;
	}

	public void setTitle(String title) {
		Title = title;
	}

	private String Artist = "";

	public String getArtist() {
		return Artist;
	}

	public void setArtist(String artist) {
		Artist = artist;
	}

	private String Album = "";

	public String getAlbum() {
		return Album;
	}

	public void setAlbum(String album) {
		Album = album;
	}

	private int Length = -1;

	public int getLength() {
		return Length;
	}

	public void setLength(int length) {
		Length = length;
	}

	private int Track = -1;

	public int getTrack() {
		return Track;
	}

	public void setTrack(int track) {
		Track = track;
	}

	private String Path;

	public String getPath() {
		return Path;
	}

	public void setPath(String path) {
		Path = path;
	}

	// Stats
	private int Played = 0;

	public int getPlayed() {
		return Played;
	}

	public void setPlayed(int value) {
		Played = value;
	}

	private long TimeLastPlayed = -1L;

	public long getTimeLastPlayed() {
		return TimeLastPlayed;
	}

	public void setTimeLastPlayed(long value) {
		TimeLastPlayed = value;
	}

	private long TotalDurationPlayed = 0L;

	public long getTotalDurationPlayed() {
		return TotalDurationPlayed;
	}

	public void setTotalDurationPlayed(long value) {
		TotalDurationPlayed = value;
	}

	private int Skipped = 0;

	public int getSkipped() {
		return Played;
	}

	public void setSkipped(int value) {
		Skipped = value;
	}

	private long TimeLastSkipped = -1L;

	public long getTimeLastSkipped() {
		return TimeLastSkipped;
	}

	public void setTimeLastSkipped(long value) {
		TimeLastSkipped = value;
	}

	private long TimeAdded = -1L;

	public long getTimeAdded() {
		return TimeAdded;
	}

	public void setTimeAdded(long value) {
		TimeAdded = value;
	}

	private String Mood = "";

	public String getMood() {
		return Mood;
	}

	public void setMood(String value) {
		Mood = value;
	}

	private double Score = 0.0;

	public double getScore() {
		return Score;
	}

	public void setScore(double value) {
		Score = value;
	}

	private long Timestamp = -1L;

	public long getTimestamp() {
		return Timestamp;
	}

	public void setTimestamp(long value) {
		Timestamp = value;
	}

	private String Genre = "";

	public String getGenre() {
		return Genre;
	}

	public void setGenre(String value) {
		Genre = value;
	}

	private int Year = -1;

	public int getYear() {
		if (Year <= 0)
			return -1;
		return Year;
	}

	public void setYear(int value) {
		if (Year <= 0)
			value = -1;
		Year = value;
	}

	@Override
	public boolean equals(Object obj) {
		Music other = (Music) obj;

		if (other == null)
			return false;

		if (Id.equals(other.Id))
			return true;

		return false;
	}

	public String getText() {
		return TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title;
	}

	public String getTextExtraOnly(String del, int position) {
		StringBuilder sb = new StringBuilder();

		if (position > -1) {
			sb.append("⌖");
			sb.append(position + 1);
		}
		if (Track > -1) {
			if (sb.length() > 0)
				sb.append(del);
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

	public String getTextExtraOnlySingleLine(int position) {
		return getTextExtraOnly(" • ", position);
	}

	public String getTextExtraOnlySingleLine() {
		return getTextExtraOnly(" • ", -1);
	}

	public String getTextDetailed(String del, int position) {
		StringBuilder sb = new StringBuilder();

		if (position > -1) {
			sb.append("⌖");
			sb.append(position + 1);
		}
		if (Track > -1) {
			if (sb.length() > 0)
				sb.append(del);
			sb.append("#");
			sb.append(Track);
		}
		if (sb.length() > 0)
			sb.append(del);
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
			if (TotalDurationPlayed > -1) {
				sb.append("/");
				sb.append(DurationFormatUtils.formatDuration(TotalDurationPlayed, "mm:ss", false));
			}
		}
		if (!TextUtils.isEmpty(Genre)) {
			sb.append(del);
			sb.append("\uD83C\uDFBC ").append(Genre);
		}
		sb.append(del);
		sb.append("\uD83C\uDFB5 ").append(Played).append("/").append(Skipped);
		if (Year > -1) {
			sb.append(del);
			sb.append("\uD83D\uDCC5 ").append(Year);
		}
		/*
		if (TimeAdded > -1) {
			sb.append(del);
			sb.append("\uD83D\uDCC5 ").append(DateFormat.getDateInstance(DateFormat.SHORT).format(TimeAdded));
		}
		*/

		return sb.toString();
	}

	public String getTextDetailedSingleLine(int position) {
		return getTextDetailed(" • ", position);
	}

	public String getTextDetailedSingleLine() {
		return getTextDetailed(" • ", -1);
	}

	public String getTextDetailedMultiLine(int position) {
		return getTextDetailed(System.getProperty("line.separator"), position);
	}

	public String getTextDetailedMultiLine() {
		return getTextDetailed(System.getProperty("line.separator"), -1);
	}

	//region Cover art

	public Bitmap getCover(final Context context, int size) {
		Bitmap result;

		if (size > 600 || size < 0)
			size = 600;

		String key = Path;
		if (size > 0)
			key = key + size;

		// Load from cache
		result = CacheEx.getInstance().getBitmap(key);

		if (result != null)
			return result;

		// File
		try {
			File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, Path);

			// Load from cache folder
			if (file.exists())
				result = BitmapFactory.decodeFile(file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}

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

	private static WeakReference<PlaybackUIActivity> currentCoverView = null;

	public static PlaybackUIActivity getCurrentCoverView() {
		return currentCoverView.get();
	}

	public static void setCurrentCoverView(PlaybackUIActivity v) {
		currentCoverView = new WeakReference<PlaybackUIActivity>(v);
	}

	private static AsyncTask<Object, Object, Bitmap> getCoverOrDownloadTask = null;

	public static void getCoverOrDownload(final int size, final Music data) throws Exception {
		if (getCoverOrDownloadTask != null) {
			getCoverOrDownloadTask.cancel(true);
			try {
				getCoverOrDownloadTask.get(1, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			getCoverOrDownloadTask = null;
		}
		getCoverOrDownloadTask = (new ArtworkEx.ArtworkDownloaderAsyncTask(
				currentCoverView.get(),
				data.getText(),
				ArtworkEx.ArtworkType.Song,
				size,
				data.Path,
				KEY_CACHE_DIR_COVER,
				data.Path,
				new JavaEx.ActionT<Bitmap>() {
					@Override
					public void execute(Bitmap bitmap) {
						if (getCurrentCoverView() != null)
							getCurrentCoverView().onCoverReloaded(bitmap);
					}
				},
				new JavaEx.ActionT<Exception>() {
					@Override
					public void execute(Exception e) {
						Log.w(TAG, e);
					}
				},
				3000,
				false));
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

	public static Music decode(Realm realm, Context context, final String path, final Uri contentUri, boolean fastMode, Music oldData) {
		Music data = oldData;

		try {
			// Get data
			if (data == null) {
				if (path != null) {
					data = realm.where(Music.class).equalTo("Path", path).findFirst();
					if (data != null)
						data = realm.copyFromRealm(data);
				}
				if (data == null)
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

				if (contentUri != null && contentUri.toString().startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
					Cursor cursor = null;

					try {
						String[] projection = {
								MediaStore.Audio.Media.DATA,
								MediaStore.Audio.Media.IS_MUSIC,
								MediaStore.Audio.Media.TITLE,
								MediaStore.Audio.Media.ARTIST,
								MediaStore.Audio.Media.ALBUM,
								MediaStore.Audio.Media.DURATION,
								MediaStore.Audio.Media.YEAR
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
								data.Track = (int) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK));
							} catch (Exception e) {
								// Eat
							}
							try {
								data.Year = (int) cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR));
							} catch (Exception e) {
								// Eat
							}

							data.Path = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))).getPath();
							data.Timestamp = (new File(data.Path)).lastModified();

							MediaMetadataRetriever mmr = new MediaMetadataRetriever();
							try {
								mmr.setDataSource(data.Path);

								try {
									byte[] cover = mmr.getEmbeddedPicture();
									if (cover != null && cover.length > 0) {
										Bitmap bmp = ImageEx.decodeBitmap(cover, 256, 256);
										if (bmp != null)
											putCover(context, data, bmp);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}

								try {
									data.Genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} catch (Exception e) {
								Log.w(TAG, "error on metadata from system", e);
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

				if (contentUri != null && contentUri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) {
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

						data.Path = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))).getPath();
						data.Timestamp = (new File(data.Path)).lastModified();

						if (!fastMode) {
							final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
							try {
								mmr.setDataSource(data.Path);

								try {
									ExecutorService executor = Executors.newCachedThreadPool();
									Callable<Bitmap> task = new Callable<Bitmap>() {
										public Bitmap call() {
											return mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
										}
									};
									Future<Bitmap> future = executor.submit(task);
									try {
										Bitmap bmp = future.get(2500, TimeUnit.MILLISECONDS);
										if (bmp != null)
											putCover(context, data, bmp);
									} catch (Exception e) {
										e.printStackTrace();
									} finally {
										future.cancel(true);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}

								try {
									data.Genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
								} catch (Exception e) {
									e.printStackTrace();
								}

							} catch (Exception e) {
								Log.w(TAG, "error on metadata from system", e);
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
			if (!fastMode && path != null && contentUri == null) {
				File file = new File(path);
				if (file.length() < 100 * 1024 * 1024)
					try {
						AudioFile audioFile = AudioFileIO.read(file);

						Tag tag = audioFile.getTagAndConvertOrCreateAndSetDefault();

						data.Timestamp = file.lastModified();
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
						try {
							data.Year = Integer.valueOf(tag.getFirst(FieldKey.YEAR));
						} catch (Exception e) {
							// Ignore
						}
						try {
							data.Genre = String.valueOf(tag.getFirst(FieldKey.GENRE));
						} catch (Exception e) {
							// Ignore
						}

						data.Path = path;

						if (data.getCover(context, -1) == null) {
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

			if (TextUtils.isEmpty(data.Title)
					|| TextUtils.isEmpty(data.Artist)
					|| data.Artist.contains("unknown"))
				try {
					ArrayList<String> at = SongsEx.getArtistAndTitle((new File(data.Path)).getName());

					data.Artist = at.get(0);
					data.Title = at.get(1);
				} catch (Exception e) {
					if (!TextUtils.isEmpty(path)) {
						data.Path = path;

						data.Title = (new File(data.Path)).getName().replaceFirst("[.][^.]+$", "");
					} else {
						throw new Exception("WTF happened!");
					}
				}

			// Save to db

			realm.beginTransaction();
			realm.copyToRealmOrUpdate(data);
			realm.commitTransaction();

			// Check constraints
			if (!(data.hasAudio() || data.hasVideo())) {
				try {
					realm.beginTransaction();
					data.deleteFromRealm();
					realm.commitTransaction();

					data = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (data != null)
				try {
					Music dbData = realm.where(Music.class).equalTo("Path", data.Path).findFirst();
					if (dbData != null)
						data = realm.copyFromRealm(dbData);
				} catch (Exception e) {
					Log.w(TAG, e);
				}

			// HACK: Calling the devil
			System.gc();
			Runtime.getRuntime().gc();

		} catch (Throwable e) {
			if (realm.isInTransaction()) {
				realm.cancelTransaction();
			} else {
				Log.w(TAG, e);
			}
		}

		if (data != null)
			Log.d(TAG, "decode\n" + data.Path);

		return data;
	}

	public void refresh(Context context) {
		try (Realm realm = DB.getDB()) {
			Music.decode(realm, context, Path, null, false, this);
		}
	}

	//endregion

	//region DB

	public static Music getById(Realm realm, String id) {
		return realm.where(Music.class).equalTo("Id", id).findFirst();
	}

	public static Music getById(String id) {
		try (Realm realm = DB.getDB()) {
			if (realm != null) {
				return realm.copyFromRealm(getById(realm, id));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Collection<Music> loadAll() {
		try {
			try (Realm realm = DB.getDB()) {
				if (realm == null)
					throw new Exception("Realm error.");
				return realm.copyFromRealm(realm.where(Music.class).findAll());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	public static Music load(Realm realm, Context context, String path) {
		Music data = realm.where(Music.class).equalTo("Path", path).findFirst();
		if (data == null)
			data = Music.decode(realm, context, path, null, false, null);
		else
			data = realm.copyFromRealm(data);

		return data;
	}

	public static Music load(Context context, String path) {
		try (Realm realm = DB.getDB()) {
			return load(realm, context, path);
		}
	}

	public static void delete(final MusicService musicService, final Realm realm, final String path, boolean notify) {
		try {
			if (musicService.getMusic().Path.equals(path))
				musicService.next(musicService.isPlaying());

			realm.executeTransaction(new Realm.Transaction() {
				@Override
				public void execute(Realm realm) {
					realm.where(Music.class)
							.equalTo("Path", path, Case.INSENSITIVE)
							.findAll()
							.deleteAllFromRealm();
				}
			});

			(new File(path)).deleteOnExit();

			if (notify) {
				final Context context = musicService;

				Intent broadcastIntent = new Intent(MusicService.ACTION_LIBRARY_UPDATED);
				LocalBroadcastManager
						.getInstance(context)
						.sendBroadcast(broadcastIntent);

				Intent musicServiceIntent = new Intent(context, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATED);
				context.startService(musicServiceIntent);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void delete(final MusicService musicService, final String path, boolean notify) {
		try (Realm realm = DB.getDB()) {
			delete(musicService, realm, path, notify);
		}
	}

	public static long getSize() {
		long r = 0;
		try (Realm realm = DB.getDB()) {
			if (realm != null) {
				r = realm.where(Music.class).count();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
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

	public static boolean isValid(String path) {
		int index = path.lastIndexOf(".");
		if (index > 0) {
			String ext = path.substring(index);
			if (Music.isAudio(ext) || Music.isVideo(ext)) {
				return true;
			}
		}
		if (path.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()) || path.startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString()))
			return true;
		return false;
	}

	//endregion

	//region Smart functions

	public static double getScore(Music music) {
		double score;

		if (music.hasVideo()) // TODO: Currently ignoring videos, fix it.
			return 0;

		int length = music.Length;
		if (length < 1)
			return 0;

		int daysSinceLastPlayed = (int) ((System.currentTimeMillis() - music.TimeLastPlayed) / (1000 * 60 * 60 * 24));
		int daysSinceLastSkipped = (int) ((System.currentTimeMillis() - music.TimeLastSkipped) / (1000 * 60 * 60 * 24));
		int daysSinceAdded = (int) ((System.currentTimeMillis() - music.TimeAdded) / (1000 * 60 * 60 * 24));
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

	private static Music atTopByScore;

	public static Music getAtTopByScore() {
		try {
			if (atTopByScore == null)
				try (Realm realm = DB.getDB()) {
					RealmResults<Music> result = realm
							.where(Music.class)
							.findAllSorted("Score", Sort.DESCENDING);

					if (!(result == null || result.size() == 0))
						atTopByScore = realm.copyFromRealm(result.first());
				}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return atTopByScore;
	}

	public static List<Music> getAllSorted(int count, String field, Sort order) {
		ArrayList<Music> result = new ArrayList<>();

		try {
			try (Realm realm = DB.getDB()) {
				RealmResults<Music> realmResults = realm
						.where(Music.class)
						.findAllSorted(field, order);

				if (!(realmResults.size() == 0)) {
					result.addAll(realm.copyFromRealm(realmResults.subList(0, Math.min(count, realmResults.size()))));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static List<Music> getAllSortedByScore(int count) {
		return getAllSorted(count, "Score", Sort.DESCENDING);
	}

	//endregion

}
