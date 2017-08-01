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

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;
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

    private IHQVisualizer visualizerHQ;
    private IVisualizer visualizer;

    private WaveformAVFXView waveformAVFXView;
    private FFTAVFXView fftAVFXView;
    private GLAudioVisualizationView wavesView;
    private WaveDbmHandler waveDbmHandler;

    private FrameLayout root;

    private JavaEx.Action pendingActionForView = null;

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

                        if (pendingActionForView != null)
                            pendingActionForView.execute();

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

        waveformAVFXView = null;
        fftAVFXView = null;
        if (wavesView != null)
            wavesView.release();
        wavesView = null;
        waveDbmHandler = null;
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
        if (wavesView != null) {
            wavesView.onResume();
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
        if (wavesView != null) {
            wavesView.onPause();
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

            if (waveDbmHandler != null) {
                byte[] b = new byte[fft.length];
                int n = fft.length / 2;
                for (int i = 0; i < n; i++) {
                    b[i + 0] = (byte) (fft[i + 0] * 128);
                    b[i + 1] = (byte) (fft[i + 1] * 128);
                }
                waveDbmHandler.onDataReceived(b);
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

            if (waveDbmHandler != null) {
                waveDbmHandler.onDataReceived(fft);
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
                    visualizerHQ.setCaptureSize(size);
                    visualizerHQ.setDataCaptureListener(onDataCaptureListenerHQ, rate, waveformAVFXView != null, fftAVFXView != null || waveDbmHandler != null);

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

                    visualizer.setCaptureSize(size);
                    visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
                    visualizer.setDataCaptureListener(onDataCaptureListener, rate, waveformAVFXView != null, fftAVFXView != null || waveDbmHandler != null);
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
        }

        if (visualizer != null) {
            visualizer.setEnabled(false);
        }

        if (wavesView != null)
            wavesView.stopRendering();

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

        if (!isAdded() || !isVisible() || root == null || !hasPermissions) {
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

            if (MusicService.getPlayerType(getActivity().getApplicationContext()) == MusicService.PlayerType.OpenSL)
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
            if (wavesView != null) {
                wavesView.release();
                wavesView = null;
                waveDbmHandler = null;
            }

            float r = Color.red(color) / 255.0f;
            float g = Color.green(color) / 255.0f;
            float b = Color.blue(color) / 255.0f;
            float a = Color.alpha(color) / 255.0f;

            a = 1f;

            switch (avfxType) {
                case Waveform:
                    waveformAVFXView = new WaveformAVFXView(getActivity().getApplicationContext());

                    waveformAVFXView.setColor(
                            new BaseAVFXView.FloatColor(r, g, b, a),
                            new BaseAVFXView.FloatColor(r + g - b, g + b - r, b + r - g, a));

                    root.addView(waveformAVFXView);
                    break;
                case FFT:
                    fftAVFXView = new FFTAVFXView(getActivity().getApplicationContext());

                    fftAVFXView.setColor(
                            new BaseAVFXView.FloatColor(r, g, b, a),
                            new BaseAVFXView.FloatColor(r + g - b, g + b - r, b + r - g, a));

                    root.addView(fftAVFXView);
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
                            .setBubblesPerLayer(24)
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
        FFT("Bars / Lines"),
        Waves("Waves"),;

        private String friendlyName;

        AVFXType(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    public static final String TAG_SPREF_AVFXTYPE = SPrefEx.TAG_SPREF + ".avfx_type";

    public static AVFXType getAVFXType(Context context) {
        return AVFXType.valueOf(SPrefEx.get(context).getString(TAG_SPREF_AVFXTYPE, String.valueOf(AVFXType.Waves)));
    }

    public static void setAVFXType(Context context, AVFXType value) {
        SPrefEx.get(context)
                .edit()
                .putString(TAG_SPREF_AVFXTYPE, String.valueOf(value))
                .apply();
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


}

