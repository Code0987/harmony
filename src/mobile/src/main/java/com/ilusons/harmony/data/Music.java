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
import android.text.TextUtils;
import android.util.Log;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.CacheEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.LyricsEx;
import com.ilusons.harmony.ref.SongsEx;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

import static com.ilusons.harmony.ref.ImageEx.findImageUrlFromItunes;

public class Music extends RealmObject {

	// Logger TAG
	private static final String TAG = Music.class.getSimpleName();

	public static final String KEY_CACHE_DIR_COVER = "covers";
	public static final String KEY_CACHE_DIR_LYRICS = "lyrics";

	// Basic
	@PrimaryKey
	@Index
	private String Path;

	public String getPath() {
		return Path;
	}

	public void setPath(String path) {
		Path = path;
	}

	public String getFilename(String ext) {
		return (TextUtils.isEmpty(Artist) ? Title : Artist + " - " + Title) + ext;
	}

	public boolean isLocal() {
		return (getPath() != null) && !(getPath().toLowerCase().startsWith("http"));
	}

	public String getCoverPath(final Context context) {
		try {
			return IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_COVER, Path).getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String LastPlaybackUrl;

	public String getLastPlaybackUrl() {
		try {
			if (TextUtils.isEmpty(LastPlaybackUrl))
				if (isLocal())
					LastPlaybackUrl = Path;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return LastPlaybackUrl;
	}

	public void setLastPlaybackUrl(String s) {
		LastPlaybackUrl = s;

		setTimeLastPlaybackUrlUpdated(System.currentTimeMillis());
	}

	private long TimeLastPlaybackUrlUpdated = -1L;

	public long getTimeLastPlaybackUrlUpdated() {
		return TimeLastPlaybackUrlUpdated;
	}

	public void setTimeLastPlaybackUrlUpdated(long value) {
		TimeLastPlaybackUrlUpdated = value;
	}

	public boolean isLastPlaybackUrlUpdateNeeded() {
		if (getLastPlaybackUrl() == null)
			return true;
		if (getLastPlaybackUrl().toLowerCase().startsWith("http"))
			if ((System.currentTimeMillis() - getTimeLastPlaybackUrlUpdated()) >= 2 * 60 * 60 * 1000L) // HACK: 2 hrs
				return true;
		return false;
	}

	@Index
	private String Title = "";

	public String getTitle() {
		return Title;
	}

	public void setTitle(String title) {
		Title = title;
	}

	@Index
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

	private String MBID = "";

	public String getMBID() {
		return MBID;
	}

	public void setMBID(String value) {
		MBID = value;
	}

	private String Tags = "";

	public String getTags() {
		return Tags;
	}

	public String[] getTagsCollection() {
		return getTags().split("\\s*,\\s*" /*",[ ]*"*/);
	}

	public void setTags(String value) {
		Tags = value;
	}

	public void setTags(Collection<String> values) {
		Tags = StringUtils.join(values, ",");
	}

	public void updateTags(Collection<String> values) {
		try {
			ArrayList<String> newTags = new ArrayList<>();
			newTags.addAll(Arrays.asList(getTagsCollection()));
			newTags.addAll(values);
			newTags.removeAll(Arrays.asList("", null));
			Set<String> newTagsUnique = new LinkedHashSet<>(newTags);
			Tags = StringUtils.join(newTagsUnique, ",");
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public enum SmartGenre {
		Alternative("Alternative"),
		Metal("Metal"),
		Indie("Indie"),
		Punk("Punk"),
		Rock("Rock"),
		Folk("Folk"),
		Country("Country"),
		Blues("Blues"),
		EDM("EDM"),
		HipHop("HipHop"),
		Jazz("Jazz"),
		RB("R&B"),
		Pop("Pop"),
		None("*");

		private String friendlyName;

		SmartGenre(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		public String getFriendlyName() {
			return friendlyName;
		}
	}

	public SmartGenre getSmartGenre() {
		SmartGenre r = SmartGenre.None;

		try {
			String tags = ((TextUtils.isEmpty(getGenre()) ? "" : getGenre() + ",") + getTags()).toLowerCase();

			if (tags.contains("alt") || tags.contains("alternative"))
				r = SmartGenre.Alternative;

			else if (tags.contains("metal") || tags.contains("metalcore"))
				r = SmartGenre.Metal;

			else if (tags.contains("indie"))
				r = SmartGenre.Indie;

			else if (tags.contains("punk"))
				r = SmartGenre.Punk;

			else if (tags.contains("rock") || tags.contains("hardcore"))
				r = SmartGenre.Rock;

			else if (tags.contains("folk"))
				r = SmartGenre.Folk;

			else if (tags.contains("country"))
				r = SmartGenre.Country;

			else if (tags.contains("blues"))
				r = SmartGenre.Blues;

			else if (tags.contains("electronic") || tags.contains("edm"))
				r = SmartGenre.EDM;

			else if (tags.contains("rap") || tags.contains("hip"))
				r = SmartGenre.HipHop;

			else if (tags.contains("Jazz"))
				r = SmartGenre.Jazz;

			else if (tags.contains("r&b") || tags.contains("soul"))
				r = SmartGenre.RB;

			else if (tags.contains("pop"))
				r = SmartGenre.Pop;

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Log.d(TAG, "smart genre: " + r + " <-> " + getGenre());

		return r;
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

		if (Path != null && other.Path != null && Path.equals(other.Path))
			return true;

		return false;
	}

	public String getText(String del) {
		return TextUtils.isEmpty(Artist) ? Title : Artist + del + Title;
	}

	public String getText() {
		return getText(" - ");
	}

	public String getTextExtraOnly(String del, int position) {
		StringBuilder sb = new StringBuilder();

		if (position > -1) {
			sb.append("⌖");
			sb.append(position + 1);
		}
		if (!isLocal()) {
			sb.append(" \uD83D\uDD17");
			if (isLastPlaybackUrlUpdateNeeded()) {
				sb.append(" \uD83D\uDCF2");
			}
		} else {
			if (!(new File(getPath())).exists()) {
				sb.append(" \uD83D\uDCF5");
			}
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
		if (!isLocal()) {
			sb.append(" \uD83D\uDD17");
			if (isLastPlaybackUrlUpdateNeeded()) {
				sb.append(" \uD83D\uDCE5️");
			}
		} else {
			if (!(new File(getPath())).exists()) {
				sb.append(" \uD83D\uDCF5");
			}
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
			sb.append(Artist);
		}
		if (!TextUtils.isEmpty(Album)) {
			sb.append(del);
			sb.append(Album);
		}
		if (Length > -1) {
			sb.append(del);
			sb.append(DurationFormatUtils.formatDuration(Length, "mm:ss", false));
			if (TotalDurationPlayed > -1) {
				sb.append("/");
				sb.append(DurationFormatUtils.formatDuration(TotalDurationPlayed, "mm:ss", false));
			}
		}
		if (!TextUtils.isEmpty(Genre)) {
			sb.append(del);
			sb.append(getSmartGenre().getFriendlyName());
		}
		if (Year > -1) {
			sb.append(del);
			sb.append(Year);
		}

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
			if (file.exists()) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				options.inDither = true;
				result = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Re-sample
		if (result != null) {
			try {
				Bitmap.Config config = result.getConfig();
				if (config == null) {
					config = Bitmap.Config.RGB_565;
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

	public static void putCover(Context context, Music data, Bitmap bmp) {
		IOEx.putBitmapInDiskCache(context, KEY_CACHE_DIR_COVER, data.Path, bmp);
	}

	public static Observable<Bitmap> loadLocalOrSearchCoverArtFromItunes(final Context context, final Music music, final String uri, final String query, final boolean forceDownload, final ImageEx.ItunesImageType imageType) {
		return Observable.create(new ObservableOnSubscribe<Bitmap>() {
			@Override
			public void subscribe(ObservableEmitter<Bitmap> oe) throws Exception {
				try {
					Uri downloadUri = null;
					File file = null;
					boolean isDownloaded = false;
					try {
						// Direct url
						downloadUri = Uri.parse(uri);
						// If it's a local file
						file = new File(uri);
						if (file.isAbsolute() && file.exists()) {
							downloadUri = Uri.fromFile(file);
						}
					} catch (Exception e) {
						// pass
					}
					// If not local or forced or if local but non-existent
					if (forceDownload || TextUtils.isEmpty(uri) || ((file != null && file.isAbsolute() && !file.exists()))) {
						downloadUri = Uri.parse(findImageUrlFromItunes(query, imageType, 3000, 360));
						isDownloaded = true;
					}

					ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
							.setBitmapConfig(Bitmap.Config.RGB_565)
							.build();

					ImageRequest imageRequest = ImageRequestBuilder
							.newBuilderWithSource(downloadUri)
							.setImageDecodeOptions(decodeOptions)
							.build();

					Bitmap bitmap = null;

					ImagePipeline imagePipeline = Fresco.getImagePipeline();
					DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);
					try (CloseableReference<CloseableImage> closeableImageRef = DataSources.waitForFinalResult(dataSource)) {
						if (closeableImageRef != null && closeableImageRef.get() instanceof CloseableBitmap) {
							bitmap = ((CloseableBitmap) closeableImageRef.get()).getUnderlyingBitmap();

							if (isDownloaded && bitmap != null && music != null) try {
								Music.putCover(context, music, bitmap);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
						dataSource.close();
					}

					oe.onNext(bitmap);

					oe.onComplete();
				} catch (Exception e) {
					oe.onError(e);
				}
			}
		});
	}

	//endregion

	//region Lyrics

	public String getLyrics(final Context context) {
		String result = null;

		// File
		File file = IOEx.getDiskCacheFile(context, KEY_CACHE_DIR_LYRICS, Path);

		// Load from cache folder
		if (file.exists()) try {
			result = FileUtils.readFileToString(file, Charset.defaultCharset());
		} catch (Exception e) {
			Log.w(TAG, e);
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

	private static WeakReference<PlaybackUIActivity> currentLyricsView = null;

	public static PlaybackUIActivity getCurrentLyricsView() {
		return currentLyricsView.get();
	}

	public static void setCurrentLyricsView(PlaybackUIActivity v) {
		currentLyricsView = new WeakReference<PlaybackUIActivity>(v);
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
					getCurrentLyricsView().onLyricsReloaded(result);
			}

			@Override
			protected String doInBackground(Void... Voids) {
				try {
					if (isCancelled() || getCurrentLyricsView() == null)
						throw new CancellationException();

					String result = data.getLyrics(getCurrentLyricsView());

					if (!TextUtils.isEmpty(result))
						return result;

					// Refresh once more
					if (result == null && getCurrentLyricsView() != null) try {
						data.refresh(getCurrentLyricsView());

						result = data.getLyrics(getCurrentLyricsView());

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

						data.putLyrics(getCurrentLyricsView(), result);
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

							data.putLyrics(getCurrentLyricsView(), result);
						} catch (Exception e) {
							Log.w(TAG, e);
						}

					if (TextUtils.isEmpty(result)) {
						result = "";

						data.putLyrics(getCurrentLyricsView(), "");
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

	//region Tags

	public static void getTagsOrDownload(final Music music, final JavaEx.ActionT<Collection<de.umass.lastfm.Tag>> onTags, final JavaEx.ActionT<Throwable> onError) {
		Analytics.getTagsFromLastfm(music)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<Collection<de.umass.lastfm.Tag>>() {
					@Override
					public void accept(final Collection<de.umass.lastfm.Tag> r) throws Exception {
						try (Realm realm = Music.getDB()) {
							if (realm == null)
								return;

							final ArrayList<String> tags = new ArrayList<>();
							List<de.umass.lastfm.Tag> apiTags = new ArrayList<>(r);
							Collections.sort(apiTags, new Comparator<de.umass.lastfm.Tag>() {
								@Override
								public int compare(de.umass.lastfm.Tag l, de.umass.lastfm.Tag r) {
									return Integer.compare(l.getCount(), r.getCount());
								}
							});
							Collections.reverse(apiTags);
							apiTags = apiTags.subList(0, Math.min(10, apiTags.size()));
							for (de.umass.lastfm.Tag tag : apiTags)
								tags.add(tag.getName());

							realm.executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(Realm realm) {
									music.updateTags(tags);

									realm.insertOrUpdate(music);
								}
							});
						}

						if (onTags != null)
							onTags.execute(r);
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {
						Log.w(TAG, throwable);

						if (onError != null)
							onError.execute(throwable);
					}
				});
	}

	//endregion

	//region Decoding

	public static Music createFromLocal(final Context context, final String path, final Uri contentUri, boolean fastMode, final Music oldData) {
		try {
			Music data = oldData;
			if (oldData == null)
				data = new Music();

			data.TimeAdded = System.currentTimeMillis();
			data.TimeLastPlayed = System.currentTimeMillis();
			data.TimeLastSkipped = System.currentTimeMillis();

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
				final MediaMetadataRetriever mmr = new MediaMetadataRetriever();

				tryToFillMetadataFromOS(context, contentUri, data, fastMode, mmr);
			}

			// Metadata from tags
			if (!fastMode && path != null && contentUri == null) {
				tryToFillMetadataFromTags(context, path, data, fastMode);
			}

			// Metadata from file name
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

			return data;
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
		return null;
	}

	private static void tryToFillMetadataFromOS(final Context context, final Uri contentUri, final Music data, final boolean fastMode, final MediaMetadataRetriever mmr) {
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
								Bitmap bmp = future.get(333, TimeUnit.MILLISECONDS);
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

	private static void tryToFillMetadataFromTags(final Context context, final String path, final Music data, final boolean fastMode) {
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
				try {
					data.MBID = String.valueOf(tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID));
				} catch (Exception e) {
					try {
						data.MBID = String.valueOf(tag.getFirst(FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID));
					} catch (Exception e2) {
						try {
							data.MBID = String.valueOf(tag.getFirst(FieldKey.MUSICBRAINZ_RELEASEID));
						} catch (Exception e3) {
							// Ignore
						}
					}
				}
				try {
					data.Tags = String.valueOf(tag.getFirst(FieldKey.TAGS));
				} catch (Exception e) {
					// Ignore
				}

				data.Path = path;

				if (!fastMode) {
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

	//endregion

	//region DB

	private static RealmConfiguration realmConfiguration;

	public static RealmConfiguration getDBConfig() {
		if (realmConfiguration == null) {
			realmConfiguration = new RealmConfiguration.Builder()
					.name("music.realm")
					.deleteRealmIfMigrationNeeded()
					.build();
		}
		return realmConfiguration;
	}

	public static Realm getDB() {
		Realm realm = null;
		try {
			realm = Realm.getInstance(getDBConfig());

			Log.i(TAG, "Realm: " + realm.getPath() + " " + realm.getVersion());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return realm;
	}

	public static Music get(Realm realm, final String path) {
		Music data = null;
		if (realm != null) {
			data = realm.where(Music.class).equalTo("Path", path).findFirst();

		}
		return data;
	}

	public static Music get(String path) {
		try (Realm realm = getDB()) {
			if (realm == null)
				return null;
			return realm.copyFromRealm(get(realm, path));
		}
	}

	public static boolean exists(String path) {
		try (Realm realm = getDB()) {
			if (realm == null)
				return false;
			return realm.where(Music.class).equalTo("Path", path).count() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Music load(Context context, String path) {
		Music data = null;
		try (Realm realm = getDB()) {
			if (realm != null) {
				data = realm.where(Music.class).equalTo("Path", path).findFirst();
				if (data == null) {
					data = Music.createFromLocal(context, path, null, true, null);
					if (data != null) {
						final Music finalData = data;
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								realm.insertOrUpdate(finalData);
							}
						});
					}
				} else {
					data = realm.copyFromRealm(data);
				}
			}
		}
		return data;
	}

	public void update() {
		try (Realm realm = getDB()) {
			if (realm != null) {
				realm.insertOrUpdate(this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void refresh(final Context context) {
		try {
			final Music data = this;
			Music.createFromLocal(context, data.getPath(), null, false, data);
			try (Realm realm = getDB()) {
				if (realm != null) {
					realm.executeTransaction(new Realm.Transaction() {
						@Override
						public void execute(@NonNull Realm realm) {
							realm.insertOrUpdate(data);
						}
					});
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
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
		try (Realm realm = getDB()) {
			delete(musicService, realm, path, notify);
		}
	}

	public static long getSize() {
		long r = 0;
		try (Realm realm = getDB()) {
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

		int length = music.Length;
		if (length < 1 || music.hasVideo())
			length = 3 * 60 * 1000;

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
				try (Realm realm = getDB()) {
					RealmResults<Music> result = realm
							.where(Music.class)
							.sort("Score", Sort.DESCENDING)
							.findAll();

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
			try (Realm realm = getDB()) {
				RealmResults<Music> realmResults = realm
						.where(Music.class)
						.sort(field, order)
						.findAll();

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

	public static List<Music> getAllSortedByTimeLastPlayed(int count) {
		return getAllSorted(count, "TimeLastPlayed", Sort.DESCENDING);
	}

	public static List<Music> getAllSortedByTimeAdded(int count) {
		return getAllSorted(count, "TimeAdded", Sort.DESCENDING);
	}

	//endregion

}
