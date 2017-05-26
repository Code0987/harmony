package com.ilusons.harmony.views;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.BaseAVFXView;
import com.ilusons.harmony.avfx.FFTAVFXView;
import com.ilusons.harmony.avfx.WaveformAVFXView;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.JavaEx;

public class AudioVFXViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = AudioVFXViewFragment.class.getSimpleName();

    private IHQVisualizer visualizer;

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

    private IHQVisualizer.OnDataCaptureListener onDataCaptureListener = new IHQVisualizer.OnDataCaptureListener() {

        @Override
        public void onWaveFormDataCapture(IHQVisualizer visualizer, float[] waveform, int numChannels, int samplingRate) {
            WaveformAVFXView view = waveformAVFXView;

            if (view != null) {
                view.updateAudioData(waveform, numChannels, samplingRate);
            }
        }

        @Override
        public void onFftDataCapture(IHQVisualizer visualizer, float[] fft, int numChannels, int samplingRate) {
            FFTAVFXView view = fftAVFXView;

            if (view != null) {
                view.updateAudioData(fft, numChannels, samplingRate);
            }
        }

    };

    private void startVisualizer() {
        stopVisualizer();

        if (visualizer != null) {
            // stop visualizer
            stopVisualizer();

            // use maximum rate & size
            int rate = visualizer.getMaxCaptureRate();
            int size = 4096;

            // NOTE: min = 128, max = 32768
            size = Math.max(visualizer.getCaptureSizeRange()[0], size);
            size = Math.min(visualizer.getCaptureSizeRange()[1], size);

            visualizer.setWindowFunction(IHQVisualizer.WINDOW_HANN | IHQVisualizer.WINDOW_OPT_APPLY_FOR_WAVEFORM);
            visualizer.setCaptureSize(size);
            visualizer.setDataCaptureListener(onDataCaptureListener, rate, waveformAVFXView != null, fftAVFXView != null);

            visualizer.setEnabled(true);
        }
    }

    private void stopVisualizer() {
        if (visualizer != null) {
            visualizer.setEnabled(false);
        }
    }

    private void cleanupVisualizer() {
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

            visualizer = musicService.getVisualizerHQ();

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
                            new BaseAVFXView.FloatColor(r, g, b, a));

                    root.addView(waveformAVFXView);
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
        Waveform,
        FFT,

        Particles
    }

}

