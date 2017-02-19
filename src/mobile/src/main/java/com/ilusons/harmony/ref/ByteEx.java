package com.ilusons.harmony.ref;

public class ByteEx {

	public static boolean AllZero(final byte[] array) {
		for (byte b : array)
			if (b != 0)
				return false;
		return true;
	}
}
