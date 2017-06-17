package com.ilusons.harmony.views;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.h6ah4i.android.media.audiofx.IBassBoost;
import com.h6ah4i.android.media.audiofx.IEqualizer;
import com.h6ah4i.android.media.audiofx.ILoudnessEnhancer;
import com.h6ah4i.android.media.audiofx.IPreAmp;
import com.h6ah4i.android.media.audiofx.IVirtualizer;
import com.h6ah4i.android.media.opensl.OpenSLMediaPlayerContext;
import com.h6ah4i.android.media.opensl.audiofx.OpenSLHQEqualizer;
import com.h6ah4i.android.media.utils.AudioEffectSettingsConverter;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class TuneActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = TuneActivity.class.getSimpleName();

    private static final int SEEKBAR_MAX = 1000;

    private View root;

    private VerticalSeekBar preamp_seekBar;
    private View[] band_layouts;
    private VerticalSeekBar[] bands;
    private TextView[] freqs;
    private CircularSeekBar bassBoost_seekBar;
    private CircularSeekBar loudness_seekBar;
    private CircularSeekBar virtualizer_seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Init
        initializeEQ(this);

        // Set view
        setContentView(R.layout.tune_activity);

        // Set views
        root = findViewById(R.id.root);

        // Set close
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Set eq
        CheckBox preamp_checkBox = (CheckBox) findViewById(R.id.preamp_checkBox);
        preamp_checkBox.setChecked(MusicService.getPlayerPreAmpEnabled(this));
        preamp_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getMusicService() == null)
                    return;

                IPreAmp preAmp = getMusicService().getPreAmp();

                if (preAmp == null)
                    return;

                preAmp.setEnabled(b);

                MusicService.setPlayerPreAmpEnabled(TuneActivity.this, b);

                info("Updated, requires restart for complete effect!");
            }
        });

        preamp_seekBar = (VerticalSeekBar) findViewById(R.id.preamp_seekBar);
        preamp_seekBar.setMax(SEEKBAR_MAX);
        preamp_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (getMusicService() == null)
                    return;

                IPreAmp preAmp = getMusicService().getPreAmp();

                if (preAmp == null)
                    return;

                setNormalizedPreAmpLevel(preAmp, (float) progress / SEEKBAR_MAX);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        CheckBox eq_checkBox = (CheckBox) findViewById(R.id.eq_checkBox);
        eq_checkBox.setChecked(MusicService.getPlayerEQEnabled(this));
        eq_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getMusicService() == null)
                    return;

                IEqualizer equalizer = getMusicService().getEqualizer();

                if (equalizer == null)
                    return;

                equalizer.setEnabled(true);

                MusicService.setPlayerEQEnabled(TuneActivity.this, b);

                info("Updated, requires restart for complete effect!");
            }
        });

        band_layouts = new View[]{
                findViewById(R.id.band0_layout),
                findViewById(R.id.band1_layout),
                findViewById(R.id.band2_layout),
                findViewById(R.id.band3_layout),
                findViewById(R.id.band4_layout),
                findViewById(R.id.band5_layout),
                findViewById(R.id.band6_layout),
                findViewById(R.id.band7_layout),
                findViewById(R.id.band8_layout),
                findViewById(R.id.band9_layout),
        };
        bands = new VerticalSeekBar[]{
                (VerticalSeekBar) findViewById(R.id.band0_seekBar),
                (VerticalSeekBar) findViewById(R.id.band1_seekBar),
                (VerticalSeekBar) findViewById(R.id.band2_seekBar),
                (VerticalSeekBar) findViewById(R.id.band3_seekBar),
                (VerticalSeekBar) findViewById(R.id.band4_seekBar),
                (VerticalSeekBar) findViewById(R.id.band5_seekBar),
                (VerticalSeekBar) findViewById(R.id.band6_seekBar),
                (VerticalSeekBar) findViewById(R.id.band7_seekBar),
                (VerticalSeekBar) findViewById(R.id.band8_seekBar),
                (VerticalSeekBar) findViewById(R.id.band9_seekBar)
        };
        freqs = new TextView[]{
                (TextView) findViewById(R.id.band0_textView),
                (TextView) findViewById(R.id.band1_textView),
                (TextView) findViewById(R.id.band2_textView),
                (TextView) findViewById(R.id.band3_textView),
                (TextView) findViewById(R.id.band4_textView),
                (TextView) findViewById(R.id.band5_textView),
                (TextView) findViewById(R.id.band6_textView),
                (TextView) findViewById(R.id.band7_textView),
                (TextView) findViewById(R.id.band8_textView),
                (TextView) findViewById(R.id.band9_textView)
        };

        for (int i = 0; i < bands.length; i++) {
            band_layouts[i].setVisibility((i < NUMBER_OF_BANDS) ? View.VISIBLE : View.INVISIBLE);
            bands[i].setEnabled((i < NUMBER_OF_BANDS));
        }

        for (int i = 0; i < NUMBER_OF_BANDS; i++) {
            freqs[i].setText(formatFrequencyText(CENTER_FREQUENCY[i]));
            bands[i].setMax(SEEKBAR_MAX);
        }

        SeekBar.OnSeekBarChangeListener bands_OnSeekBarChangeListener = (new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (getMusicService() == null)
                    return;

                IEqualizer equalizer = getMusicService().getEqualizer();

                if (equalizer == null)
                    return;

                equalizer.setEnabled(true);

                short band;
                switch (seekBar.getId()) {
                    case R.id.band0_seekBar:
                        band = 0;
                        break;
                    case R.id.band1_seekBar:
                        band = 1;
                        break;
                    case R.id.band2_seekBar:
                        band = 2;
                        break;
                    case R.id.band3_seekBar:
                        band = 3;
                        break;
                    case R.id.band4_seekBar:
                        band = 4;
                        break;
                    case R.id.band5_seekBar:
                        band = 5;
                        break;
                    case R.id.band6_seekBar:
                        band = 6;
                        break;
                    case R.id.band7_seekBar:
                        band = 7;
                        break;
                    case R.id.band8_seekBar:
                        band = 8;
                        break;
                    case R.id.band9_seekBar:
                        band = 9;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }

                equalizer.setBandLevel(band, BandLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        for (int i = 0; i < NUMBER_OF_BANDS; i++) {
            bands[i].setOnSeekBarChangeListener(bands_OnSeekBarChangeListener);
        }

        // Set bass boost
        CheckBox bassboost_checkBox = (CheckBox) findViewById(R.id.bassboost_checkBox);
        bassboost_checkBox.setChecked(MusicService.getPlayerBassBoostEnabled(this));
        bassboost_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getMusicService() == null)
                    return;

                IBassBoost bassBoost = getMusicService().getBassBoost();

                if (bassBoost == null)
                    return;

                bassBoost.setEnabled(true);

                MusicService.setPlayerBassBoostEnabled(TuneActivity.this, b);

                info("Updated, requires restart for complete effect!");
            }
        });

        bassBoost_seekBar = (CircularSeekBar) findViewById(R.id.bassboost_seekBar);
        bassBoost_seekBar.setMax(SEEKBAR_MAX);
        bassBoost_seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (getMusicService() == null)
                    return;

                IBassBoost bassBoost = getMusicService().getBassBoost();

                if (bassBoost == null)
                    return;

                bassBoost.setStrength(BassboostStrengthNormalizer.denormalize(progress / SEEKBAR_MAX));
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        // Set loudness
        CheckBox loudness_checkBox = (CheckBox) findViewById(R.id.loudness_checkBox);
        loudness_checkBox.setChecked(MusicService.getPlayerLoudnessEnabled(this));
        loudness_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getMusicService() == null)
                    return;

                ILoudnessEnhancer loudnessEnhancer = getMusicService().getLoudnessEnhancer();

                if (loudnessEnhancer == null)
                    return;

                loudnessEnhancer.setEnabled(true);

                MusicService.setPlayerLoudnessEnabled(TuneActivity.this, b);

                info("Updated, requires restart for complete effect!");
            }
        });

        loudness_seekBar = (CircularSeekBar) findViewById(R.id.loudness_seekBar);
        loudness_seekBar.setMax(SEEKBAR_MAX);
        loudness_seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (getMusicService() == null)
                    return;

                ILoudnessEnhancer loudnessEnhancer = getMusicService().getLoudnessEnhancer();

                if (loudnessEnhancer == null)
                    return;

                loudnessEnhancer.setTargetGain(LoudnessStrengthNormalizer.denormalize(progress / SEEKBAR_MAX));
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        // Set virtualizer
        CheckBox virtualizer_checkBox = (CheckBox) findViewById(R.id.virtualizer_checkBox);
        virtualizer_checkBox.setChecked(MusicService.getPlayerVirtualizerEnabled(this));
        virtualizer_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getMusicService() == null)
                    return;

                IVirtualizer virtualizer = getMusicService().getVirtualizer();

                if (virtualizer == null)
                    return;

                virtualizer.setEnabled(true);

                MusicService.setPlayerVirtualizerEnabled(TuneActivity.this, b);

                info("Updated, requires restart for complete effect!");
            }
        });

        virtualizer_seekBar = (CircularSeekBar) findViewById(R.id.virtualizer_seekBar);
        virtualizer_seekBar.setMax(SEEKBAR_MAX);
        virtualizer_seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (!fromUser)
                    return;

                if (getMusicService() == null)
                    return;

                IVirtualizer virtualizer = getMusicService().getVirtualizer();

                if (virtualizer == null)
                    return;

                virtualizer.setStrength(VirtualizerStrengthNormalizer.denormalize(progress / SEEKBAR_MAX));
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

    }

    @Override
    protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
        super.OnMusicServiceChanged(className, musicService, isBound);

        IPreAmp preAmp = musicService.getPreAmp();
        if (preAmp != null)
            preamp_seekBar.setProgress((int) (SEEKBAR_MAX * getNormalizedPreAmpLevel(preAmp)));

        IEqualizer equalizer = musicService.getEqualizer();
        if (equalizer != null)
            for (int i = 0; i < NUMBER_OF_BANDS; i++)
                bands[i].setProgress((int) (SEEKBAR_MAX * BandLevelNormalizer.normalize(equalizer.getBandLevel((short) i))));

        IBassBoost bassBoost = getMusicService().getBassBoost();
        if (bassBoost != null)
            bassBoost_seekBar.setProgress((int) (BassboostStrengthNormalizer.normalize(bassBoost.getRoundedStrength()) * SEEKBAR_MAX));

        ILoudnessEnhancer loudnessEnhancer = getMusicService().getLoudnessEnhancer();
        if (loudnessEnhancer != null)
            loudness_seekBar.setProgress(LoudnessStrengthNormalizer.normalize((int) loudnessEnhancer.getTargetGain()) * SEEKBAR_MAX);

        IVirtualizer virtualizer = getMusicService().getVirtualizer();
        if (virtualizer != null)
            virtualizer_seekBar.setProgress((int) (VirtualizerStrengthNormalizer.normalize(virtualizer.getRoundedStrength()) * SEEKBAR_MAX));

    }

    private static String formatFrequencyText(int freq_millihertz) {
        final int freq = freq_millihertz / 1000;
        if (freq < 1000) {
            return freq + " Hz";
        } else {
            return (freq / 1000) + " kHz";
        }
    }

    protected static final class IntParameterNormalizer {
        private final int mMin;
        private final int mMax;

        public IntParameterNormalizer(int min, int max) {
            mMin = min;
            mMax = max;
        }

        public float normalize(int value) {
            return (float) (value - mMin) / (mMax - mMin);
        }

        public int denormalize(float value) {
            return (int) ((value) * (mMax - mMin) + mMin);
        }
    }

    protected static final class ShortParameterNormalizer {
        private final short mMin;
        private final short mMax;

        public ShortParameterNormalizer(short min, short max) {
            mMin = min;
            mMax = max;
        }

        public float normalize(short value) {
            return (float) (value - mMin) / (mMax - mMin);
        }

        public short denormalize(float value) {
            return (short) ((value) * (mMax - mMin) + mMin);
        }
    }

    public static int NUMBER_OF_BANDS;
    public static int NUMBER_OF_PRESETS;
    public static PresetInfo[] PRESETS;
    public static int[] CENTER_FREQUENCY;
    public static short BAND_LEVEL_MIN;
    public static short BAND_LEVEL_MAX;

    private static ShortParameterNormalizer BandLevelNormalizer;
    private static ShortParameterNormalizer BassboostStrengthNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);
    private static IntParameterNormalizer LoudnessStrengthNormalizer = new IntParameterNormalizer(0, 1000);
    private static ShortParameterNormalizer VirtualizerStrengthNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);

    public static class PresetInfo {
        public short index;
        public String name;
        public IEqualizer.Settings settings;
    }

    public static boolean IS_EQ_INITIALIZED = false;

    public static void initializeEQ(Context context) {
        if (IS_EQ_INITIALIZED)
            return;

        if (MusicService.getPlayerType(context) == MusicService.PlayerType.AndroidOS) {
            short numberOfBands = 0;
            short numberOfPresets = 0;
            PresetInfo[] presets = null;
            int[] centerFrequency = new int[0];
            short[] bandLevelRange = new short[2];

            MediaPlayer player = null;
            Equalizer eq = null;
            try {
                player = new MediaPlayer();
                eq = new Equalizer(0, player.getAudioSessionId());

                numberOfBands = eq.getNumberOfBands();
                numberOfPresets = eq.getNumberOfPresets();

                presets = new PresetInfo[numberOfPresets];
                for (short i = 0; i < numberOfPresets; i++) {
                    PresetInfo preset = new PresetInfo();

                    eq.usePreset(i);

                    preset.index = i;
                    preset.name = eq.getPresetName(preset.index);
                    preset.settings = AudioEffectSettingsConverter.convert(eq.getProperties());

                    presets[i] = preset;
                }

                centerFrequency = new int[numberOfBands];
                for (short i = 0; i < numberOfBands; i++) {
                    centerFrequency[i] = eq.getCenterFreq(i);
                }

                bandLevelRange = eq.getBandLevelRange();
            } finally {
                try {
                    if (eq != null) {
                        eq.release();
                    }
                } catch (Exception e) {
                }
                try {
                    if (player != null) {
                        player.release();
                    }
                } catch (Exception e) {
                }
                eq = null;
                player = null;
            }

            NUMBER_OF_BANDS = numberOfBands;
            NUMBER_OF_PRESETS = numberOfPresets;
            PRESETS = presets;
            CENTER_FREQUENCY = centerFrequency;
            BAND_LEVEL_MIN = bandLevelRange[0];
            BAND_LEVEL_MAX = bandLevelRange[1];
        } else {
            short numberOfBands = 0;
            short numberOfPresets = 0;
            PresetInfo[] presets = null;
            int[] centerFreqency = new int[0];
            short[] bandLevelRange = new short[2];

            OpenSLMediaPlayerContext oslmp_context = null;
            IEqualizer eq = null;
            try {
                OpenSLMediaPlayerContext.Parameters params = new OpenSLMediaPlayerContext.Parameters();
                params.options = OpenSLMediaPlayerContext.OPTION_USE_HQ_EQUALIZER;

                oslmp_context = new OpenSLMediaPlayerContext(null, params);
                eq = new OpenSLHQEqualizer(oslmp_context);

                numberOfBands = eq.getNumberOfBands();
                numberOfPresets = eq.getNumberOfPresets();

                presets = new PresetInfo[numberOfPresets];
                for (short i = 0; i < numberOfPresets; i++) {
                    PresetInfo preset = new PresetInfo();

                    eq.usePreset(i);

                    preset.index = i;
                    preset.name = eq.getPresetName(preset.index);
                    preset.settings = eq.getProperties();

                    presets[i] = preset;
                }

                centerFreqency = new int[numberOfBands];
                for (short i = 0; i < numberOfBands; i++) {
                    centerFreqency[i] = eq.getCenterFreq(i);
                }

                bandLevelRange = eq.getBandLevelRange();
            } catch (UnsupportedOperationException e) {
                // just ignore (maybe API level is less than 14)
            } finally {
                try {
                    if (eq != null) {
                        eq.release();
                    }
                } catch (Exception e) {
                }
                try {
                    if (oslmp_context != null) {
                        oslmp_context.release();
                    }
                } catch (Exception e) {
                }
                eq = null;
                oslmp_context = null;
            }

            NUMBER_OF_BANDS = numberOfBands;
            NUMBER_OF_PRESETS = numberOfPresets;
            PRESETS = presets;
            CENTER_FREQUENCY = centerFreqency;
            BAND_LEVEL_MIN = bandLevelRange[0];
            BAND_LEVEL_MAX = bandLevelRange[1];
        }

        BandLevelNormalizer = new ShortParameterNormalizer(BAND_LEVEL_MIN, BAND_LEVEL_MAX);

        IS_EQ_INITIALIZED = true;
    }

    private static final double PREAMP_LEVEL_RANGE_DB = 40;

    private static float getNormalizedPreAmpLevel(IPreAmp preAmp) {
        final float value = preAmp.getLevel();
        float ui_level;

        if (value <= 0.0f) {
            ui_level = 0.0f;
        } else {
            ui_level = (float) (Math.log10(value) * 20 / PREAMP_LEVEL_RANGE_DB + 1.0);
        }

        return ui_level;
    }

    private static void setNormalizedPreAmpLevel(IPreAmp preAmp, float value) {
        float log_level;

        if (value <= 0.0f) {
            log_level = 0.0f;
        } else {
            log_level = (float) Math.pow(10, (PREAMP_LEVEL_RANGE_DB * (value - 1.0) / 20));
        }

        preAmp.setLevel(log_level);
    }
}
