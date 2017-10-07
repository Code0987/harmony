package com.ilusons.harmony.ref;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;

public class MediaEx {

	/*
	This class opens a file, reads the first audio channel it finds, and returns raw audio data.

	      MediaDecoder codec = new MediaDecoder(".m4a");
	      short[] data;
	      while ((data = codec.readShortData()) != null) {
	         // process data here
	      }
	*/

	public static class MediaDecoder implements AutoCloseable {
		private MediaExtractor extractor = new MediaExtractor();
		private MediaCodec codec;
		private MediaFormat inputFormat;
		private boolean end_of_input_file;
		private ByteBuffer[] outputBuffers;
		private int outputBufferIndex = -1;

		public MediaDecoder(String inputFilename) throws Exception {
			extractor.setDataSource(inputFilename);

			// Select the first audio track we find.
			int numTracks = extractor.getTrackCount();
			for (int i = 0; i < numTracks; ++i) {
				MediaFormat format = extractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime.startsWith("audio/")) {
					extractor.selectTrack(i);
					codec = MediaCodec.createDecoderByType(mime);
					codec.configure(format, null, null, 0);
					inputFormat = format;
					break;
				}
			}

			if (codec == null) {
				throw new IllegalArgumentException("No codec for file format");
			}

			codec.start();
			outputBuffers = codec.getOutputBuffers();
			end_of_input_file = false;
		}

		@Override
		public void close() throws Exception {

		}

		// Read the raw data from MediaCodec.
		// The caller should copy the data out of the ByteBuffer before calling this again
		// or else it may get overwritten.
		@SuppressLint("WrongConstant")
		private ByteBuffer readData(BufferInfo info) throws Exception {
			if (codec == null)
				return null;

			for (; ; ) {
				// Read data from the file into the codec.
				if (!end_of_input_file) {
					int inputBufferIndex = codec.dequeueInputBuffer(10000);
					if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);

						int size = extractor.readSampleData(inputBuffer, 0);
						if (size < 0) {
							// End Of File
							codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
							end_of_input_file = true;
						} else {
							codec.queueInputBuffer(inputBufferIndex, 0, size, extractor.getSampleTime(), 0);
							extractor.advance();
						}
					}
				}

				// Read the output from the codec.
				if (outputBufferIndex >= 0)
					// Ensure that the data is placed at the start of the buffer
					outputBuffers[outputBufferIndex].position(0);

				outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
				if (outputBufferIndex >= 0) {
					// Handle EOF
					if (info.flags != 0) {
						codec.stop();
						codec.release();
						codec = null;
						return null;
					}

					// Release the buffer so MediaCodec can use it again.
					// The data should stay there until the next time we are called.
					// codec.releaseOutputBuffer(outputBufferIndex, false);

					return outputBuffers[outputBufferIndex];

				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					// This usually happens once at the start of the file.
					outputBuffers = codec.getOutputBuffers();
				}
			}
		}

		// Return the Audio sample rate, in samples/sec.
		public int getSampleRate() {
			return inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		}

		public int getChannels() {
			return inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
		}

		// Read the raw audio data in 16-bit format
		// Returns null on EOF
		public short[] readShortData() throws Exception {
			BufferInfo info = new BufferInfo();
			ByteBuffer data = readData(info);

			if (data == null)
				return null;

			short[] returnData = new short[info.size / 2];
			// Converting the ByteBuffer to an array doesn't actually make a copy
			// so we must do so or it will be overwritten later.
			data.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(returnData);

			codec.releaseOutputBuffer(outputBufferIndex, false);

			return returnData;
		}

	}

}