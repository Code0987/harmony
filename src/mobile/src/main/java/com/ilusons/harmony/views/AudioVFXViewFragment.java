package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.BaseAVFXView;
import com.ilusons.harmony.avfx.FFTAVFXView;
import com.ilusons.harmony.avfx.WaveformAVFXView;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

public class AudioVFXViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = AudioVFXViewFragment.class.getSimpleName();

    private boolean isHQMode = false;

    private IHQVisualizer visualizerHQ;
    private IVisualizer visualizer;

    private WaveformAVFXView waveformAVFXView;
    private FFTAVFXView fftAVFXView;

    private FrameLayout root;

    private JavaEx.Action pendingActionForView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audio_vfx_view, container, false);

        root = (FrameLayout) v.findViewById(R.id.root);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (pendingActionForView != null)
            pendingActionForView.execute();

        isHQMode = MusicService.getPlayerType(getContext()) == MusicService.PlayerType.OpenSL;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        waveformAVFXView = null;
        fftAVFXView = null;
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

        if (waveformAVFXView != null) {
            waveformAVFXView.onResume();
        }
        if (fftAVFXView != null) {
            fftAVFXView.onResume();
        }

        startVisualizer();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (waveformAVFXView != null) {
            waveformAVFXView.onPause();
        }
        if (fftAVFXView != null) {
            fftAVFXView.onPause();
        }

        cleanupVisualizer();
    }

    private IHQVisualizer.OnDataCaptureListener onDataCaptureListenerHQ = new IHQVisualizer.OnDataCaptureListener() {

        @Override
        public void onWaveFormDataCapture(IHQVisualizer visualizerHQ, float[] waveform, int numChannels, int samplingRate) {
            WaveformAVFXView view = waveformAVFXView;

            if (view != null) {
                view.updateAudioData(waveform, numChannels, samplingRate);
            }
        }

        @Override
        public void onFftDataCapture(IHQVisualizer visualizerHQ, float[] fft, int numChannels, int samplingRate) {
            FFTAVFXView view = fftAVFXView;

            if (view != null) {
                view.updateAudioData(fft, numChannels, samplingRate);
            }
        }

    };

    private IVisualizer.OnDataCaptureListener onDataCaptureListener = new IVisualizer.OnDataCaptureListener() {

        @Override
        public void onWaveFormDataCapture(IVisualizer visualizerHQ, byte[] waveform, int samplingRate) {
            WaveformAVFXView view = waveformAVFXView;

            if (view != null) {
                view.updateAudioData(waveform, samplingRate);
            }
        }

        @Override
        public void onFftDataCapture(IVisualizer visualizerHQ, byte[] fft, int samplingRate) {
            FFTAVFXView view = fftAVFXView;

            if (view != null) {
                view.updateAudioData(fft, samplingRate);
            }
        }
    };

    private void startVisualizer() {
        stopVisualizer();

        if (isHQMode) {
            if (visualizerHQ != null) {
                // stop visualizerHQ
                stopVisualizer();

                // use maximum rate & size
                int rate = visualizerHQ.getMaxCaptureRate();
                int size = 4096;

                // NOTE: min = 128, max = 32768
                size = Math.max(visualizerHQ.getCaptureSizeRange()[0], size);
                size = Math.min(visualizerHQ.getCaptureSizeRange()[1], size);

                visualizerHQ.setWindowFunction(IHQVisualizer.WINDOW_HANN | IHQVisualizer.WINDOW_OPT_APPLY_FOR_WAVEFORM);
                visualizerHQ.setCaptureSize(size);
                visualizerHQ.setDataCaptureListener(onDataCaptureListenerHQ, rate, waveformAVFXView != null, fftAVFXView != null);

                visualizerHQ.setEnabled(true);
            }
        } else {
            if (visualizer != null) {
                // stop visualizer
                stopVisualizer();

                // use maximum rate & size
                int rate = visualizer.getMaxCaptureRate();
                int size = visualizer.getCaptureSizeRange()[1];

                visualizer.setCaptureSize(size);
                visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
                visualizer.setDataCaptureListener(onDataCaptureListener, rate, waveformAVFXView != null, fftAVFXView != null);
                visualizer.setMeasurementMode(IVisualizer.MEASUREMENT_MODE_PEAK_RMS);

                visualizer.setEnabled(true);
            }
        }

    }

    private void stopVisualizer() {
        if (visualizerHQ != null) {
            visualizerHQ.setEnabled(false);
        }

        if (visualizer != null) {
            visualizer.setEnabled(false);
        }
    }

    private void cleanupVisualizer() {
        if (visualizerHQ != null) {
            visualizerHQ.setEnabled(false);

            visualizerHQ.setDataCaptureListener(null, 0, false, false);
        }

        if (visualizer != null) {
            visualizer.setEnabled(false);

            visualizer.setDataCaptureListener(null, 0, false, false);
        }
    }

    public void reset(final MusicService musicService, final AVFXType avfxType, final int color) {
        if (musicService == null)
            return;

        if (!isAdded() || !isVisible() || root == null) {
            pendingActionForView = new JavaEx.Action() {
                @Override
                public void execute() {
                    reset(musicService, avfxType, color);
                }
            };

            return;
        }

        try {
            stopVisualizer();
            cleanupVisualizer();

            if (isHQMode)
                visualizerHQ = musicService.getVisualizerHQ();
            else
                visualizer = musicService.getVisualizer();

            if (isRemoving())
                return;

            root.removeAllViews();

            if (waveformAVFXView != null) {
                waveformAVFXView = null;
            }
            if (fftAVFXView != null) {
                fftAVFXView = null;
            }

            float r = Color.red(color) / 255.0f;
            float g = Color.green(color) / 255.0f;
            float b = Color.blue(color) / 255.0f;
            float a = Color.alpha(color) / 255.0f;

            a = 1f;

            switch (avfxType) {
                case Waveform:
                    waveformAVFXView = new WaveformAVFXView(getContext());

                    waveformAVFXView.setColor(
                            new BaseAVFXView.FloatColor(r, g, b, a),
                            new BaseAVFXView.FloatColor(r + g - b, g + b - r, b + r - g, a));

                    root.addView(waveformAVFXView);
                    break;
                case FFT:
                    fftAVFXView = new FFTAVFXView(getContext());

                    fftAVFXView.setColor(
                            new BaseAVFXView.FloatColor(r, g, b, a),
                            new BaseAVFXView.FloatColor(r + g - b, g + b - r, b + r - g, a));

                    root.addView(fftAVFXView);
                default:
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
        FFT("Bars / Lines");

        private String friendlyName;

        AVFXType(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    public static final String TAG_SPREF_AVFXTYPE = SPrefEx.TAG_SPREF + ".avfx_type";

    public static AVFXType getAVFXType(Context context) {
        return AVFXType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_AVFXTYPE, String.valueOf(AVFXType.Waveform)));
    }

    public static void setAVFXType(Context context, AVFXType value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_AVFXTYPE, String.valueOf(value))
                .apply();
    }

}

