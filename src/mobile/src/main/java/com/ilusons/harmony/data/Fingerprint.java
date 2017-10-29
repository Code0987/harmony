package com.ilusons.harmony.data;

import android.support.annotation.NonNull;
import android.util.Log;

import com.ilusons.harmony.ref.MediaEx;

import org.acoustid.chromaprint.Chromaprint;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

			Log.d(TAG, "Fingerprint" + "\n" + path + "\n" + fp);

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

			Log.d(TAG, "Fingerprint" + "\n" + fp);

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
		return 0;
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

	public static Fingerprint search(Realm realm, int[] rawFingerprint) {
		Log.d(TAG, "Search started!");

		String fp = toStringFromIntArray(rawFingerprint);

		Log.d(TAG, "Search on: " + fp);

		for (Fingerprint fingerprint : realm.where(Fingerprint.class).findAll()) {
			double score = match(fingerprint.getRawFingerprint(), fp);
			Log.d(TAG, "Search match: " + score + ", Id: " + fingerprint.getId());

			if (score >= 0.75) {
				Log.d(TAG, "Search over, found!");

				return realm.copyFromRealm(fingerprint);
			}
		}

		Log.d(TAG, "Search over, NOT found!");

		return null;
	}

	public static Fingerprint search(int[] rawFingerprint) {
		try (Realm realm = getDB()) {
			return search(realm, rawFingerprint);
		}
	}

	public static double match(String x, String y) {
		double r = -1;

		r = ((double) lcs1(x, y) / (double) Math.min(x.length(), y.length()));

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
