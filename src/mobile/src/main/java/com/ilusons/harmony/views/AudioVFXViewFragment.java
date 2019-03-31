package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.BaseAVFXCanvasView;
import com.ilusons.harmony.avfx.BaseAVFXGLView;
import com.ilusons.harmony.avfx.CirclesView;
import com.ilusons.harmony.avfx.DotsView;
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

	private MusicService musicService;

	private IHQVisualizer visualizerHQ;
	private IVisualizer visualizer;

	private WaveformView waveformGLView;
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
		if (waveformCanvasView != null) {
			waveformCanvasView.onPause();
		}
		if (fftCanvasView != null) {
			fftCanvasView.onPause();
		}

		unbindVisualizer();
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
			if (fftCanvasView != null) {
				fftCanvasView.updateAudioData(fft, samplingRate);
			}
		}
	};

	private void startVisualizer() {
		try {
			stopVisualizer();

			if (musicService == null)
				return;

			if (MusicService.getPlayerType(getActivity().getApplicationContext()) == MusicService.PlayerType.AudioTrack || MusicService.getPlayerType(getActivity().getApplicationContext()) == MusicService.PlayerType.OpenSL)
				visualizerHQ = musicService.getVisualizerHQ();
			else
				visualizer = musicService.getVisualizer();

			if (visualizerHQ != null) {
				visualizerHQ.setEnabled(false);

				// use maximum rate & size
				int rate = visualizerHQ.getMaxCaptureRate();
				int size = 2048/*4096*/;

				// NOTE: min = 128, max = 32768
				size = Math.max(visualizerHQ.getCaptureSizeRange()[0], size);
				size = Math.min(visualizerHQ.getCaptureSizeRange()[1], size);

				try {
					visualizerHQ.setCaptureSize(size);
				} catch (Exception e) {
					e.printStackTrace();
				}

				visualizerHQ.setWindowFunction(IHQVisualizer.WINDOW_HAMMING | IHQVisualizer.WINDOW_OPT_APPLY_FOR_WAVEFORM);

				visualizerHQ.setDataCaptureListener(
						onDataCaptureListenerHQ,
						rate,
						waveformGLView != null || waveformCanvasView != null,
						fftCanvasView != null
				);

				visualizerHQ.setEnabled(true);
			}
			if (visualizer != null) {
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
						 fftCanvasView != null
				);
				visualizer.setMeasurementMode(IVisualizer.MEASUREMENT_MODE_PEAK_RMS);

				visualizer.setEnabled(true);
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void stopVisualizer() {
		if (visualizerHQ != null) try {
			visualizerHQ.setEnabled(false);

			visualizerHQ.release();

			visualizerHQ = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (visualizer != null) try {
			visualizer.setEnabled(false);

			visualizer.release();

			visualizer = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void unbindVisualizer() {
		if (visualizerHQ != null) try {
			visualizerHQ.setEnabled(false);

			visualizerHQ.setDataCaptureListener(null, 0, false, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (visualizer != null) try {
			visualizer.setEnabled(false);

			visualizer.setDataCaptureListener(null, 0, false, false);
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
			unbindVisualizer();

			stopVisualizer();

			if (isRemoving())
				return;

			this.musicService = musicService;

			root.removeAllViews();

			if (waveformGLView != null) {
				waveformGLView = null;
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
							new BaseAVFXGLView.FloatColor(g + b - r, b + r - g, r + g - b, a));

					root.addView(waveformGLView);
					break;

				case Particles:
					ParticlesView particles = new ParticlesView(getActivity());

					particles.setColor(color);

					root.addView(particles);

					waveformCanvasView = particles;
					break;

				case Circles:
					CirclesView circles = new CirclesView(getActivity());

					root.addView(circles);

					fftCanvasView = circles;
					break;

				case Dots:
					DotsView dots = new DotsView(getActivity());

					dots.setColor(color);

					root.addView(dots);

					fftCanvasView = dots;
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
		Particles("Particles"),
		Circles("Circles"),
		Dots("Dots");

		public String friendlyName;

		AVFXType(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_AVFXTYPE = SPrefEx.TAG_SPREF + ".avfx_type";

	public static AVFXType getAVFXType(Context context) {
		try {
			return AVFXType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_AVFXTYPE, String.valueOf(AVFXType.Circles)));
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

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_AVFXTYPE,
	};

}

