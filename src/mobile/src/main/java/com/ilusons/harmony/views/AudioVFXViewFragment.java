package com.ilusons.harmony.views;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.audiofx.Visualizer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.AVFXView;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.VisualizerEx;
import com.yalantis.waves.util.Horizon;

public class AudioVFXViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = AudioVFXViewFragment.class.getSimpleName();

    private FrameLayout root;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audio_vfx_view, container, false);

        root = (FrameLayout) v.findViewById(R.id.root);

        return v;
    }

    private VisualizerEx visualizerEx;

    public void reset(final MusicService musicService, AVFXType avfxType, int color) {
        if (musicService == null)
            return;

        // TODO: Optimize and update this vfx

        try {
            if (visualizerEx != null) {
                visualizerEx.setEnabled(false);
                visualizerEx.release();
            }

            visualizerEx = new VisualizerEx(getContext(), musicService.getAudioSessionId(), new VisualizerEx.OnFftDataCaptureListener() {
                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    if (horizon != null && horizonView != null && horizonView.isAttachedToWindow())
                        horizon.updateView(fft);
                    if (avfxView != null)
                        avfxView.onFftDataCapture(visualizer, fft, samplingRate);

                }
            });

            visualizerEx.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (avfxType) {
            case Horizon:
                setupFXHorizon(musicService, color);
                break;
            case AVFX:
                setupAVFXView(musicService, color);
            default:
                break;
        }

    }

    private Horizon horizon;
    private GLSurfaceView horizonView;

    private void setupFXHorizon(MusicService musicService, int color) {
        if (horizonView != null)
            root.removeView(horizonView);

        horizonView = new GLSurfaceView(getContext());

        horizonView.setZOrderOnTop(true);
        horizonView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        horizonView.getHolder().setFormat(PixelFormat.RGBA_8888);
        // horizonView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        root.addView(horizonView, 0);

        horizon = new Horizon(
                horizonView,
                Color.argb(255, 0, 0, 0),
                44100,
                1,
                16);

        horizon.setMaxVolumeDb(120);
    }

    private AVFXView avfxView;

    private void setupAVFXView(MusicService musicService, int color) {
        if (avfxView != null)
            root.removeView(avfxView);

        avfxView = new AVFXView(
                getContext(),
                root.getWidth(),
                root.getHeight(),
                color);

        root.addView(avfxView, 0);
    }

    public static AudioVFXViewFragment create() {
        AudioVFXViewFragment f = new AudioVFXViewFragment();
        Bundle b = new Bundle();
        f.setArguments(b);
        return f;
    }

    public enum AVFXType {
        AVFX,

        Horizon
    }

}

