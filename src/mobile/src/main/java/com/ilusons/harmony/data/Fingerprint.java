package com.ilusons.harmony.data;

import android.util.Log;

import com.eaio.stringsearch.BNDMWildcardsCI;
import com.eaio.stringsearch.StringSearch;
import com.ilusons.harmony.ref.MediaEx;

import org.acoustid.chromaprint.Chromaprint;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

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

	public static double compare(String x, String y) {
		BNDMWildcardsCI algo = new BNDMWildcardsCI(' ');



		return 0;
	}

	//endregion

}
