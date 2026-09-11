package com.ilusons.harmony.avfx;

import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Taps PCM from ExoPlayer and exposes waveform + FFT snapshots for the existing AVFX views.
 * Does not use RECORD_AUDIO.
 */
public final class PlaybackVisualizer {

	public interface Listener {
		void onWaveform(float[] waveform, int channels, int sampleRate);

		void onFft(float[] fft, int channels, int sampleRate);
	}

	private static final int CAPTURE = 2048;

	private final Handler main = new Handler(Looper.getMainLooper());
	private final float[] ring = new float[CAPTURE];
	private final float[] snapshot = new float[CAPTURE];
	private final float[] fftOut = new float[CAPTURE];
	private int writeIndex;
	private int sampleRate = 44100;
	private int channels = 1;
	private Listener listener;
	private boolean running;

	private final AudioProcessor processor = new BaseAudioProcessor() {
		@Override
		public void queueInput(ByteBuffer inputBuffer) {
			ByteBuffer copy = inputBuffer.asReadOnlyBuffer();
			copy.order(ByteOrder.nativeOrder());
			synchronized (ring) {
				while (copy.remaining() >= 2) {
					float sample = copy.getShort() / 32768f;
					ring[writeIndex] = sample;
					writeIndex = (writeIndex + 1) % ring.length;
				}
			}
			ByteBuffer out = replaceOutputBuffer(inputBuffer.remaining());
			out.put(inputBuffer);
			out.flip();
		}

		@Override
		protected AudioFormat onConfigure(AudioFormat inputAudioFormat) {
			sampleRate = inputAudioFormat.sampleRate;
			channels = Math.max(1, inputAudioFormat.channelCount);
			return inputAudioFormat;
		}
	};

	private final Runnable pump = new Runnable() {
		@Override
		public void run() {
			if (!running) {
				return;
			}
			Listener l = listener;
			if (l != null) {
				synchronized (ring) {
					int start = writeIndex;
					for (int i = 0; i < CAPTURE; i++) {
						snapshot[i] = ring[(start + i) % CAPTURE];
					}
				}
				l.onWaveform(snapshot, 1, sampleRate);
				fft(snapshot, fftOut);
				l.onFft(fftOut, 1, sampleRate);
			}
			main.postDelayed(this, 33);
		}
	};

	public AudioProcessor getProcessor() {
		return processor;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void start() {
		if (running) {
			return;
		}
		running = true;
		main.post(pump);
	}

	public void stop() {
		running = false;
		main.removeCallbacks(pump);
	}

	private static void fft(float[] time, float[] outInterleaved) {
		int n = time.length;
		double[] re = new double[n];
		double[] im = new double[n];
		for (int i = 0; i < n; i++) {
			// Hamming
			double w = 0.54 - 0.46 * Math.cos((2 * Math.PI * i) / (n - 1));
			re[i] = time[i] * w;
			im[i] = 0;
		}
		fftInPlace(re, im);
		for (int i = 0; i < n; i++) {
			outInterleaved[i] = (float) (i % 2 == 0 ? re[i / 2] : im[i / 2]);
		}
		// Store as re,im pairs packed into n floats (n/2 bins).
		for (int i = 0; i < n / 2; i++) {
			outInterleaved[2 * i] = (float) re[i];
			outInterleaved[2 * i + 1] = (float) im[i];
		}
	}

	private static void fftInPlace(double[] re, double[] im) {
		int n = re.length;
		int j = 0;
		for (int i = 1; i < n; i++) {
			int bit = n >> 1;
			for (; j >= bit; bit >>= 1) {
				j -= bit;
			}
			j += bit;
			if (i < j) {
				double tr = re[i];
				re[i] = re[j];
				re[j] = tr;
				double ti = im[i];
				im[i] = im[j];
				im[j] = ti;
			}
		}
		for (int len = 2; len <= n; len <<= 1) {
			double ang = -2 * Math.PI / len;
			double wlenRe = Math.cos(ang);
			double wlenIm = Math.sin(ang);
			for (int i = 0; i < n; i += len) {
				double wRe = 1;
				double wIm = 0;
				for (int k = 0; k < len / 2; k++) {
					int u = i + k;
					int v = u + len / 2;
					double tRe = re[v] * wRe - im[v] * wIm;
					double tIm = re[v] * wIm + im[v] * wRe;
					re[v] = re[u] - tRe;
					im[v] = im[u] - tIm;
					re[u] += tRe;
					im[u] += tIm;
					double nwRe = wRe * wlenRe - wIm * wlenIm;
					wIm = wRe * wlenIm + wIm * wlenRe;
					wRe = nwRe;
				}
			}
		}
	}
}
