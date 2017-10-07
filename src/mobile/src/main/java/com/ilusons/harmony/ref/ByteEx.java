package com.ilusons.harmony.ref;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteEx {

	public static boolean AllZero(final byte[] array) {
		for (byte b : array)
			if (b != 0)
				return false;
		return true;
	}

	public short bytesToShort(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}

	public byte[] shortToBytes(short value) {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
	}


}
