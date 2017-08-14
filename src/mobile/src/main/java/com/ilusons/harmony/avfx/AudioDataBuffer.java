package com.ilusons.harmony.avfx;

public class AudioDataBuffer {

	public static final class Buffer {

		public byte[] bData;
		public float[] fData;
		public int channels;
		public int sr;

		public void update(byte[] data, int numChannels, int samplingRate) {
			if (bData != null && bData.length == data.length) {
				System.arraycopy(data, 0, bData, 0, data.length);
			} else {
				bData = data.clone();
			}
			fData = null;
			channels = numChannels;
			sr = samplingRate;
		}

		public void update(float[] data, int numChannels, int samplingRate) {
			if (fData != null && fData.length == data.length) {
				System.arraycopy(data, 0, fData, 0, data.length);
			} else {
				fData = data.clone();
			}
			bData = null;
			channels = numChannels;
			sr = samplingRate;
		}

		public boolean valid() {
			return (bData != null || fData != null) && channels != 0 && sr != 0;
		}
	}

	public static final class DoubleBufferingManager {

		private Buffer[] buffers;
		private int index;
		private boolean updated;

		public DoubleBufferingManager() {
			reset();
		}

		public synchronized void reset() {
			if (buffers == null)
				buffers = new Buffer[2];
			buffers[0] = new Buffer();
			buffers[1] = new Buffer();
			index = 0;
			updated = false;
		}

		public synchronized void update(byte[] data, int numChannels, int samplingRate) {
			buffers[index ^ 1].update(data, numChannels, samplingRate);
			updated = true;

			notify();
		}

		public synchronized void update(float[] data, int numChannels, int samplingRate) {
			buffers[index ^ 1].update(data, numChannels, samplingRate);
			updated = true;

			notify();
		}

		public synchronized Buffer getAndSwapBuffer() {
			if (updated) {
				index ^= 1;
				updated = false;
			}

			return buffers[index];
		}
	}

}