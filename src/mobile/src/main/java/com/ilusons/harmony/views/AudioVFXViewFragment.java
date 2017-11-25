package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;
import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.BarsView;
import com.ilusons.harmony.avfx.BaseAVFXCanvasView;
import com.ilusons.harmony.avfx.BaseAVFXGLView;
import com.ilusons.harmony.avfx.CircleBarsView;
import com.ilusons.harmony.avfx.CirclesView;
import com.ilusons.harmony.avfx.DotsView;
import com.ilusons.harmony.avfx.FFTView;
import com.ilusons.harmony.avfx.ParticlesView;
import com.ilusons.harmony.avfx.WaveformView;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.permissions.PermissionsManager;
import com.ilusons.harmony.ref.permissions.PermissionsResultAction;

import java.lang.ref.WeakReference;

public class AudioVFXViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = AudioVFXViewFragment.class.getSimpleName();

	private IHQVisualizer visualizerHQ;
	private IVisualizer visualizer;

	private WaveformView waveformGLView;
	private FFTView fftGLView;
	private GLAudioVisualizationView wavesView;
	private WaveDbmHandler waveDbmHandler;
	private BaseAVFXCanvasView waveformCanvasView;
	private BaseAVFXCanvasView fftCanvasView;

	private FrameLayout root;

	private WeakReference<JavaEx.Action> pendingActionForViewReference = null;

	private boolean hasPermissions = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.audio_vfx_view, container, false);

		root = (FrameLayout) v.findViewById(R.id.root);

		return v;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
				getActivity(),
				new String[]{
						android.Manifest.permission.RECORD_AUDIO
				},
				new PermissionsResultAction() {
					@Override
					public void onGranted() {
						hasPermissions = true;

						if (pendingActionForViewReference != null && pendingActionForViewReference.get() != null)
							pendingActionForViewReference.get().execute();

					}

					@Override
					public void onDenied(String permission) {
						hasPermissions = false;
					}
				});

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		waveformGLView = null;
		fftGLView = null;
		if (wavesView != null)
			wavesView.release();
		wavesView = null;
		waveDbmHandler = null;
		waveformCanvasView = null;
		fftCanvasView = null;
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (waveformGLView != null) {
			waveformGLView.onResume();
		}
		if (fftGLView != null) {
			fftGLView.onResume();
		}
		if (wavesView != null) {
			wavesView.onResume();
		}
		if (waveformCanvasView != null) {
			waveformCanvasView.onResume();
		}
		if (fftCanvasView != null) {
			fftCanvasView.onResume();
		}

		startVisualizer();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (waveformGLView != null) {
			waveformGLView.onPause();
		}
		if (fftGLView != null) {
			fftGLView.onPause();
		}
		if (wavesView != null) {
			wavesView.onPause();
		}
		if (waveformCanvasView != null) {
			waveformCanvasView.onPause();
		}
		if (fftCanvasView != null) {
			fftCanvasView.onPause();
		}

		cleanupVisualizer();
	}

	private IHQVisualizer.OnDataCaptureListener onDataCaptureListenerHQ = new IHQVisualizer.OnDataCaptureListener() {

		@Override
		public void onWaveFormDataCapture(IHQVisualizer visualizerHQ, float[] waveform, int numChannels, int samplingRate) {
			if (waveformGLView != null) {
				waveformGLView.updateAudioData(waveform, numChannels, samplingRate);
			}

			if (waveformCanvasView != null) {
				waveformCanvasView.updateAudioData(waveform, numChannels, samplingRate);
			}
		}

		@Override
		public void onFftDataCapture(IHQVisualizer visualizerHQ, float[] fft, int numChannels, int samplingRate) {
			if (fftGLView != null) {
				fftGLView.updateAudioData(fft, numChannels, samplingRate);
			}

			if (waveDbmHandler != null) {
				byte[] b = new byte[fft.length];
				int n = fft.length / 2;
				for (int i = 0; i < n; i++) {
					b[i + 0] = (byte) (fft[i + 0] * 128);
					b[i + 1] = (byte) (fft[i + 1] * 128);
				}
				waveDbmHandler.onDataReceived(b);
			}

			if (fftCanvasView != null) {
				fftCanvasView.updateAudioData(fft, numChannels, samplingRate);
			}
		}

	};

	private IVisualizer.OnDataCaptureListener onDataCaptureListener = new IVisualizer.OnDataCaptureListener() {

		@Override
		public void onWaveFormDataCapture(IVisualizer visualizerHQ, byte[] waveform, int samplingRate) {
			if (waveformGLView != null) {
				waveformGLView.updateAudioData(waveform, samplingRate);
			}

			if (waveformCanvasView != null) {
				waveformCanvasView.updateAudioData(waveform, samplingRate);
			}
		}

		@Override
		public void onFftDataCapture(IVisualizer visualizerHQ, byte[] fft, int samplingRate) {
			if (fftGLView != null) {
				fftGLView.updateAudioData(fft, samplingRate);
			}

			if (waveDbmHandler != null) {
				waveDbmHandler.onDataReceived(fft);
			}

			if (fftCanvasView != null) {
				fftCanvasView.updateAudioData(fft, samplingRate);
			}
		}
	};

	private void startVisualizer() {
		try {
			stopVisualizer();

			if (MusicService.getPlayerType(getActivity().getApplicationContext()) == MusicService.PlayerType.OpenSL) {
				if (visualizerHQ != null) {
					// stop visualizerHQ
					stopVisualizer();

					visualizerHQ.setEnabled(false);

					// use maximum rate & size
					int rate = visualizerHQ.getMaxCaptureRate();
					int size = 4096;

					// NOTE: min = 128, max = 32768
					size = Math.max(visualizerHQ.getCaptureSizeRange()[0], size);
					size = Math.min(visualizerHQ.getCaptureSizeRange()[1], size);

					visualizerHQ.setWindowFunction(IHQVisualizer.WINDOW_HANN | IHQVisualizer.WINDOW_OPT_APPLY_FOR_WAVEFORM);
					try {
						visualizerHQ.setCaptureSize(size);
					} catch (Exception e) {
						e.printStackTrace();
					}
					visualizerHQ.setDataCaptureListener(
							onDataCaptureListenerHQ,
							rate,
							waveformGLView != null || waveformCanvasView != null,
							fftGLView != null || waveDbmHandler != null || fftCanvasView != null
					);

					visualizerHQ.setEnabled(true);
				}
			} else {
				if (visualizer != null) {
					// stop visualizer
					stopVisualizer();

					visualizer.setEnabled(false);

					// use maximum rate & size
					int rate = visualizer.getMaxCaptureRate();
					int size = visualizer.getCaptureSizeRange()[1];

					try {
						visualizer.setCaptureSize(size);
					} catch (Exception e) {
						try {
							visualizer.setEnabled(false);
							visualizer.setCaptureSize(size);
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
					visualizer.setScalingMode(Visualizer.SCALING_MODE_AS_PLAYED);
					visualizer.setDataCaptureListener(
							onDataCaptureListener,
							rate,
							waveformGLView != null || waveformCanvasView != null,
							fftGLView != null || waveDbmHandler != null || fftCanvasView != null
					);
					visualizer.setMeasurementMode(IVisualizer.MEASUREMENT_MODE_PEAK_RMS);

					visualizer.setEnabled(true);
				}
			}

			if (wavesView != null)
				wavesView.startRendering();
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void stopVisualizer() {
		if (visualizerHQ != null) {
			visualizerHQ.setEnabled(false);

			visualizerHQ.release();
		}

		if (visualizer != null) {
			visualizer.setEnabled(false);

			visualizer.release();
		}

		if (wavesView != null)
			wavesView.stopRendering();

	}

	private void cleanupVisualizer() {
		try {
			if (visualizerHQ != null) {
				visualizerHQ.setEnabled(false);

				visualizerHQ.setDataCaptureListener(null, 0, false, false);
			}

			if (visualizer != null) {
				visualizer.setEnabled(false);

				visualizer.setDataCaptureListener(null, 0, false, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reset(final MusicService musicService, final AVFXType avfxType, final int color) {
		if (musicService == null)
			return;

		if (!isAdded() || !isVisible() || root == null || !hasPermissions) {
			pendingActionForViewReference = new WeakReference<JavaEx.Action>(new JavaEx.Action() {
				@Override
				public void execute() {
					reset(musicService, avfxType, color);
				}
			});

			return;
		}

		try {
			stopVisualizer();
			cleanupVisualizer();

			if (MusicService.getPlayerType(getActivity().getApplicationContext()) == MusicService.PlayerType.OpenSL)
				visualizerHQ = musicService.getVisualizerHQ();
			else
				visualizer = musicService.getVisualizer();

			if (isRemoving())
				return;

			root.removeAllViews();
			root.setBackground(null);

			if (waveformGLView != null) {
				waveformGLView = null;
			}
			if (fftGLView != null) {
				fftGLView = null;
			}
			if (wavesView != null) {
				wavesView.release();
				wavesView = null;
				waveDbmHandler = null;
			}
			if (waveformCanvasView != null) {
				waveformCanvasView = null;
			}
			if (fftCanvasView != null) {
				fftCanvasView = null;
			}

			float r = Color.red(color) / 255.0f;
			float g = Color.green(color) / 255.0f;
			float b = Color.blue(color) / 255.0f;
			float a = Color.alpha(color) / 255.0f;

			a = 1f;

			switch (avfxType) {
				case Waveform:
					waveformGLView = new WaveformView(getActivity().getApplicationContext());

					waveformGLView.setColor(
							new BaseAVFXGLView.FloatColor(r, g, b, a),
							new BaseAVFXGLView.FloatColor(r + g - b, g + b - r, b + r - g, a));

					root.addView(waveformGLView);
					break;

				case FFT:
					fftGLView = new FFTView(getActivity().getApplicationContext());

					fftGLView.setColor(
							new BaseAVFXGLView.FloatColor(r, g, b, a),
							new BaseAVFXGLView.FloatColor(r + g - b, g + b - r, b + r - g, a));

					root.addView(fftGLView);
					break;

				case Bars:
					BarsView bars = new BarsView(getActivity());

					bars.getPaint().setColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
					bars.getPaint().setAlpha(Color.alpha(color));
					bars.setCycleColorEnabled(false);

					root.addView(bars);

					fftCanvasView = bars;
					break;

				case Particles:
					ParticlesView particles = new ParticlesView(getActivity());

					particles.setColor(color);

					root.addView(particles);

					fftCanvasView = particles;
					root.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.lomo));
					break;

				case Circles:
					CirclesView circles = new CirclesView(getActivity());

					root.addView(circles);

					fftCanvasView = circles;
					root.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.lomo));
					break;

				case Dots:
					DotsView dots = new DotsView(getActivity());

					dots.setColor(color);

					root.addView(dots);

					fftCanvasView = dots;
					root.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.lomo));
					break;

				case CircleBars:
					CircleBarsView circleBarsView = new CircleBarsView(getActivity());

					circleBarsView.setColor(color);

					root.addView(circleBarsView);

					waveformCanvasView = circleBarsView;
					root.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.lomo));
					break;

				case Waves:
				default:
					int layers = 5;
					int[] layerColors = new int[layers];
					float[] hsv = new float[3];
					Color.colorToHSV(color, hsv);
					float v = hsv[2];
					for (int i = layers / 2; i >= 0; i--) {
						hsv[2] = (float) (v * (2.0 / (2 * (i / 2.0 + 1))));
						layerColors[i] = Color.HSVToColor(hsv);
					}
					for (int i = layers / 2; i < layers; i++) {
						hsv[2] = (float) (v * (2.0 / (2 * (i / 2.0 + 1))));
						layerColors[i] = Color.HSVToColor(hsv);
					}

					wavesView = new GLAudioVisualizationView.Builder(getActivity())
							.setBubblesPerLayer(16)
							.setBubblesRandomizeSize(true)
							.setBubblesSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()))
							.setLayersCount(layers)
							.setLayerColors(layerColors)
							.setWavesCount(6)
							.setWavesHeight(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()))
							.setWavesFooterHeight(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()))
							.setBackgroundColor(layerColors[layerColors.length - 1])
							.build();

					waveDbmHandler = new WaveDbmHandler();
					wavesView.linkTo(waveDbmHandler);

					root.addView(wavesView);
					break;
			}

			startVisualizer();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static AudioVFXViewFragment create() {
		AudioVFXViewFragment f = new AudioVFXViewFragment();
		Bundle b = new Bundle();
		f.setArguments(b);
		return f;
	}

	public enum AVFXType {
		Waveform("Waveform"),
		FFT("FFT"),
		Waves("Waves"),
		Bars("Bars"),
		Particles("Particles"),
		Circles("Circles"),
		Dots("Dots"),
		CircleBars("CircleBars");

		public String friendlyName;

		AVFXType(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_AVFXTYPE = SPrefEx.TAG_SPREF + ".avfx_type";

	public static AVFXType getAVFXType(Context context) {
		try {
			return AVFXType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_AVFXTYPE, String.valueOf(AVFXType.Particles)));
		} catch (Exception e) {
			e.printStackTrace();

			return AVFXType.Circles;
		}
	}

	public static void setAVFXType(Context context, AVFXType value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_AVFXTYPE, String.valueOf(value))
				.apply();
	}

	public static AVFXType getNextAVFXType(Context context) {
		try {
			AVFXType avfxTypeCurrent = getAVFXType(context);

			int i = 0;
			AVFXType[] avfxTypes = AVFXType.values();
			for (; i < avfxTypes.length; i++)
				if (avfxTypes[i] == avfxTypeCurrent) {
					i++;
					break;
				}

			AVFXType avfxTypeNext = avfxTypeCurrent;
			if (i != -1)
				avfxTypeNext = avfxTypes[i % avfxTypes.length];

			return avfxTypeNext;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getAVFXType(context);
	}

	public static final String TAG_SPREF_AVFXTYPE_ENABLED = SPrefEx.TAG_SPREF + ".avfx_enabled";

	public static boolean getAVFXEnabled(Context context) {
		return SPrefEx.get(context).getBoolean(TAG_SPREF_AVFXTYPE_ENABLED, false);
	}

	public static void setAVFXEnabled(Context context, boolean value) {
		SPrefEx.get(context)
				.edit()
				.putBoolean(TAG_SPREF_AVFXTYPE_ENABLED, value)
				.apply();
	}

	public class WaveDbmHandler extends DbmHandler<byte[]> {

		private static final float MAX_DB_VALUE = 76;

		private float[] dbs;
		private float[] allAmps;
		private final float[] coefficients = new float[]{
				80 / 44100f,
				350 / 44100f,
				2500 / 44100f,
				10000 / 44100f,
		};

		@Override
		public void onDataReceivedImpl(byte[] fft, int layersCount, float[] dBmArray, float[] ampArray) {
			// calculate dBs and amplitudes
			int dataSize = fft.length / 2 - 1;
			if (dbs == null || dbs.length != dataSize) {
				dbs = new float[dataSize];
			}
			if (allAmps == null || allAmps.length != dataSize) {
				allAmps = new float[dataSize];
			}
			for (int i = 0; i < dataSize; i++) {
				float re = fft[2 * i];
				float im = fft[2 * i + 1];
				float sqMag = re * re + im * im;
				dbs[i] = (float) (20 * Math.log10(0.000001f + sqMag));
				float k = 1;
				if (i == 0 || i == dataSize - 1) {
					k = 2;
				}
				allAmps[i] = (float) (k * Math.sqrt(sqMag) / dataSize);
			}
			for (int i = 0; i < layersCount; i++) {
				int index = (int) (coefficients[i] * fft.length);
				float db = dbs[index];
				float amp = allAmps[index];
				dBmArray[i] = db / MAX_DB_VALUE;
				ampArray[i] = amp;
			}
		}
	}

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_AVFXTYPE,
	};

}

