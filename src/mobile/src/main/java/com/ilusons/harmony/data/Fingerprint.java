package com.ilusons.harmony.data;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.ilusons.harmony.ref.MediaEx;

import org.acoustid.chromaprint.Chromaprint;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;
import io.realm.log.RealmLog;

public class Fingerprint extends RealmObject {

	public static final String TAG = Fingerprint.class.getSimpleName();

	//region DB Fields

	@PrimaryKey
	private String Id;

	public String getId() {
		return Id;
	}

	public void setId(String value) {
		Id = value;
	}

	private String RawFingerprint;

	public String getRawFingerprint() {
		return RawFingerprint;
	}

	public void setRawFingerprint(String value) {
		RawFingerprint = value;
	}

	//endregion

	//region DB

	public static RealmConfiguration getDBConfig() {
		return new RealmConfiguration.Builder()
				.name("fingerprint.realm")
				.deleteRealmIfMigrationNeeded()
				.build();
	}

	public static Realm getDB() {
		try {
			return Realm.getInstance(getDBConfig());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//endregion

	//region Generator

	public static String GenerateFingerprint(String path, Long length) {
		Chromaprint chromaprint = null;
		try {
			chromaprint = new Chromaprint();
			MediaEx.MediaDecoder decoder = new MediaEx.MediaDecoder(path);
			int sr = decoder.getSampleRate();
			int n = decoder.getChannels();
			chromaprint.start(sr, n);
			short[] samples;
			int ts = 0;
			while ((samples = decoder.readShortData()) != null) {
				chromaprint.feed(samples);
				ts += samples.length;
				if ((ts / (float) sr) >= (length - 1))
					break;
			}
			chromaprint.finish();

			String fp = chromaprint.getFingerprint();

			Log.d(TAG, "Fingerprint" + "\n" + path + "\n" + fp);

			return fp;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return null;
	}

	public static int[] GenerateRawFingerprint(String path, Long length) {
		Chromaprint chromaprint = null;
		try {
			chromaprint = new Chromaprint();
			MediaEx.MediaDecoder decoder = new MediaEx.MediaDecoder(path);
			int sr = decoder.getSampleRate();
			int n = decoder.getChannels();
			chromaprint.start(sr, n);
			short[] samples;
			int ts = 0;
			while ((samples = decoder.readShortData()) != null) {
				chromaprint.feed(samples);
				ts += samples.length;
				if ((ts / (float) sr) >= (length - 1))
					break;
			}
			chromaprint.finish();

			int[] fp = chromaprint.getRawFingerprint();

			Log.d(TAG, "Fingerprint" + "\n" + path + "\n" + Arrays.toString(fp));

			return fp;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return null;
	}

	public static String GenerateFingerprint(byte[] samples, int n, int sr) {
		Chromaprint chromaprint = null;
		try {
			chromaprint = new Chromaprint();
			chromaprint.start(sr, n);
			short[] s = new short[samples.length / 2];
			ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s);
			chromaprint.feed(s);
			chromaprint.finish();

			String fp = chromaprint.getFingerprint();

			Log.d(TAG, "Fingerprint" + "\n" + fp);

			return fp;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return null;
	}

	public static int[] GenerateRawFingerprint(byte[] samples, int n, int sr) {
		Chromaprint chromaprint = null;
		try {
			chromaprint = new Chromaprint();
			chromaprint.start(sr, n);
			short[] s = new short[samples.length / 2];
			ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s);
			chromaprint.feed(s);
			chromaprint.finish();

			int[] fp = chromaprint.getRawFingerprint();

			Log.d(TAG, "Fingerprint" + "\n" + Arrays.toString(fp));

			return fp;
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		return null;
	}

	//endregion

	//region Indexer

	public static Fingerprint index(Realm realm, final String id, final String path, final Long length) {
		Fingerprint r = null;
		try {
			int[] rawFingerprint = GenerateRawFingerprint(path, length);

			String value = toStringFromIntArray(rawFingerprint);

			realm.beginTransaction();
			try {
				r = realm.createObject(Fingerprint.class, id);
				r.setRawFingerprint(value);

				r = realm.copyFromRealm(r);

				realm.commitTransaction();
			} catch (Throwable e) {
				if (realm.isInTransaction()) {
					realm.cancelTransaction();
				}
				throw e;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	public static Fingerprint index(final String id, final String path, final Long length) {
		try (Realm realm = getDB()) {
			return index(realm, id, path, length);
		}
	}

	public static Fingerprint indexIfNot(Realm realm, final String id, final String path, final Long length) {
		Fingerprint r = realm.where(Fingerprint.class).equalTo("Id", id).findFirst();
		if (r == null) {
			r = index(id, path, length);
		} else {
			r = realm.copyFromRealm(r);
		}

		return r;
	}

	public static void removeAll(Realm realm) {
		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(@NonNull Realm realm) {
				realm.where(Fingerprint.class).findAll().deleteAllFromRealm();
			}
		});
	}

	public static long getSize() {
		long r = 0;
		try (Realm realm = getDB()) {
			if (realm != null) {
				r = realm.where(Fingerprint.class).count();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	//endregion

	//region Comparator

	public static String toStringFromIntArray(int[] value) {
		return StringUtils.join(value, ' ');
	}

	public static int[] toIntArrayFromString(String value) {
		String[] sints = value.split(" ");
		int[] result = new int[sints.length];
		for (int i = 0; i < sints.length; i++) {
			result[i] = Integer.parseInt(sints[i]);
		}
		return result;
	}

	public static Fingerprint search(Realm realm, int[] rawFingerprint, double min, double cutoff) {
		Log.d(TAG, "Search started!");

		Fingerprint r = null;

		String fp = toStringFromIntArray(rawFingerprint);

		Log.d(TAG, "Search on: " + fp);

		double max = 0;
		for (Fingerprint fingerprint : realm.where(Fingerprint.class).findAll()) {
			double score = match(fingerprint.getRawFingerprint(), fp);
			Log.d(TAG, "Search match: " + score + ", Id: " + fingerprint.getId());

			if (score >= min) {
				Log.d(TAG, "Search matched at " + score);

				r = realm.copyFromRealm(fingerprint);

				if (max < score)
					max = score;

				if (score >= cutoff) {
					Log.d(TAG, "Search over, found!");
					break;
				}
			}
		}

		if (r == null)
			Log.d(TAG, "Search over, NOT found!");
		else
			Log.d(TAG, "Search over, found!");

		return r;
	}

	public static Fingerprint search(int[] rawFingerprint, double min, double cutoff) {
		try (Realm realm = getDB()) {
			return search(realm, rawFingerprint, min, cutoff);
		}
	}

	public static double match(String x, String y) {
		double r;

		if (TextUtils.isEmpty(x) || TextUtils.isEmpty(y))
			return 0;

		r = fingerprint_distance(toIntArrayFromString(x), toIntArrayFromString(y));

		return r;
	}

	private static /*unsigned*/ int popcount_table_8bit[] = {
			0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
			1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
			1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
			2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
			1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
			2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
			2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
			3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8,
	};

	private static int popcount_lookup8(/*unsigned*/ int x) {
		return popcount_table_8bit[(x & 0xff) & 0xff] +
				popcount_table_8bit[((x >> 8) & 0xff) & 0xff] +
				popcount_table_8bit[((x >> 16) & 0xff) & 0xff] +
				popcount_table_8bit[(x >> 24) & 0xff];
	}

	public static double fingerprint_distance(int[] fingerprint1, int[] fingerprint2) {
		double r = 0;

		final int ACOUSTID_MAX_BIT_ERROR = 2 * 4;
		final int ACOUSTID_MAX_ALIGN_OFFSET = 120 * 8;

		int numcounts = fingerprint1.length + fingerprint2.length + 1;
		int[] counts = new int[numcounts];

		for (int i = 0; i < fingerprint1.length; i++) {
			int jbegin = Math.max(0, i - ACOUSTID_MAX_ALIGN_OFFSET);
			int jend = Math.min(fingerprint2.length, i + ACOUSTID_MAX_ALIGN_OFFSET);
			for (int j = jbegin; j < jend; j++) {
				int biterror = popcount_lookup8(fingerprint1[i] ^ fingerprint2[j]);
				if (biterror <= ACOUSTID_MAX_BIT_ERROR) {
					int offset = i - j + fingerprint2.length;
					counts[offset]++;
				}

			}
		}

		int topcount = 0;
		for (int i = 0; i < numcounts; i++) {
			if (counts[i] > topcount) {
				topcount = counts[i];
			}
		}

		r = (double) topcount / (double) Math.min(fingerprint1.length, fingerprint2.length);

		return r;
	}

	public static int lcs1(String s, String t) {
		if (s.isEmpty() || t.isEmpty()) {
			return 0;
		}
		int m = s.length();
		int n = t.length();
		int cost = 0;
		int maxLen = 0;
		int[] p = new int[n];
		int[] d = new int[n];
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < n; ++j) {
				if (s.charAt(i) != t.charAt(j)) {
					cost = 0;
				} else {
					if ((i == 0) || (j == 0)) {
						cost = 1;
					} else {
						cost = p[j - 1] + 1;
					}
				}
				d[j] = cost;

				if (cost > maxLen) {
					maxLen = cost;
				}
			}
			int[] swap = p;
			p = d;
			d = swap;
		}
		return maxLen;
	}

	public static String lcs2(String a, String b) {
		int[][] lengths = new int[a.length() + 1][b.length() + 1];

		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < a.length(); i++)
			for (int j = 0; j < b.length(); j++)
				if (a.charAt(i) == b.charAt(j))
					lengths[i + 1][j + 1] = lengths[i][j] + 1;
				else
					lengths[i + 1][j + 1] =
							Math.max(lengths[i + 1][j], lengths[i][j + 1]);

		// read the substring out from the matrix
		StringBuffer sb = new StringBuffer();
		for (int x = a.length(), y = b.length();
		     x != 0 && y != 0; ) {
			if (lengths[x][y] == lengths[x - 1][y])
				x--;
			else if (lengths[x][y] == lengths[x][y - 1])
				y--;
			else {
				assert a.charAt(x - 1) == b.charAt(y - 1);
				sb.append(a.charAt(x - 1));
				x--;
				y--;
			}
		}

		return sb.reverse().toString();
	}

	//endregion

}
