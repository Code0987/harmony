package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.media.audiofx.IBassBoost;
import com.h6ah4i.android.media.audiofx.IEnvironmentalReverb;
import com.h6ah4i.android.media.audiofx.IEqualizer;
import com.h6ah4i.android.media.audiofx.ILoudnessEnhancer;
import com.h6ah4i.android.media.audiofx.IPreAmp;
import com.h6ah4i.android.media.audiofx.IPresetReverb;
import com.h6ah4i.android.media.audiofx.IVirtualizer;
import com.h6ah4i.android.media.opensl.OpenSLMediaPlayerContext;
import com.h6ah4i.android.media.opensl.audiofx.OpenSLHQEqualizer;
import com.h6ah4i.android.media.utils.AudioEffectSettingsConverter;
import com.h6ah4i.android.media.utils.EnvironmentalReverbPresets;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.ViewEx;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import jonathanfinerty.once.Once;
import me.tankery.lib.circularseekbar.CircularSeekBar;

public class TuneActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = TuneActivity.class.getSimpleName();

	private static final int SEEKBAR_MAX = 10000;

	private View root;

	private AudioVFXViewFragment audioVFXViewFragment;

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

		// Set views and tabs
		final ViewEx.StaticViewPager viewPager = (ViewEx.StaticViewPager) findViewById(R.id.viewPager);
		TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);

		// Set avfx
		audioVFXViewFragment = AudioVFXViewFragment.create();
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.avfx_layout, audioVFXViewFragment)
				.commit();

		// SFX
		createEq();

		createPreAmp();

		createBassBoost();

		createLoudnessEnhancer();

		createVirtualizer();

		createEnvReverb();

		createPresetReverb();

		// Info
		final String tag_msg_info = TAG + ".msg_info";
		if (!Once.beenDone(Once.THIS_APP_VERSION, tag_msg_info)) {
			(new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
					.setTitle("A note ...")
					.setMessage("Tune allows you to deeply customize the sound you want to hear. It's currently in experimental ☢ state. Tune functions are totally device dependent. Thank you.")
					.setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
						}
					}))
					.show();

			Once.markDone(tag_msg_info);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		try {
			MusicService musicService = getMusicService();
			if (musicService != null) {

				musicService.saveEqualizer();

				musicService.savePreAmp();

				musicService.saveBassBoost();

				musicService.saveLoudnessEnhancer();

				musicService.saveVirtualizer();

				musicService.saveEnvironmentalReverb();

				musicService.savePresetReverb();

			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	@Override
	protected void OnMusicServiceChanged(ComponentName className, MusicService musicService, boolean isBound) {
		super.OnMusicServiceChanged(className, musicService, isBound);

		if (audioVFXViewFragment != null && audioVFXViewFragment.isAdded()) {
			audioVFXViewFragment.reset(
					musicService,
					AudioVFXViewFragment.AVFXType.Bars,
					ContextCompat.getColor(getApplicationContext(), R.color.accent));
		}

		updateEq(musicService.getEqualizer());

		updatePreAmp(musicService.getPreAmp());

		updateBassBoost(musicService.getBassBoost());

		updateLoudnessEnhancer(musicService.getLoudnessEnhancer());

		updateVirtualizer(musicService.getVirtualizer());

		updateEnvReverb(musicService.getEnvironmentalReverb());

		updatePresetReverb(musicService.getPresetReverb());

	}

	//region EQ

	private CheckBox eq_checkBox;
	private View[] band_layouts;
	private VerticalSeekBar[] bands;
	private TextView[] freqs;
	private Spinner eq_preset_spinner;

	private void createEq() {
		eq_checkBox = (CheckBox) findViewById(R.id.eq_checkBox);

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
			band_layouts[i].setVisibility((i < NUMBER_OF_BANDS) ? View.VISIBLE : View.GONE);
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

				if (!eq_checkBox.isChecked())
					eq_checkBox.setChecked(true);

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

				try {
					equalizer.setBandLevel(band, BandLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();

					info("Update failed, try another preset or settings!");
				}
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

		eq_preset_spinner = (Spinner) findViewById(R.id.eq_preset_spinner);
		ArrayList<String> presets = new ArrayList<>();
		presets.add("Custom");
		for (int i = 0; i < NUMBER_OF_PRESETS; i++)
			presets.add(PRESETS[i].name);
		final ArrayAdapter<String> eq_preset_spinner_adapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, presets);
		eq_preset_spinner_adapter.setDropDownViewResource(R.layout.spinner_layout);
		eq_preset_spinner.setAdapter(eq_preset_spinner_adapter);
		eq_preset_spinner.post(new Runnable() {
			public void run() {
				eq_preset_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
						if (getMusicService() == null)
							return;

						IEqualizer equalizer = getMusicService().getEqualizer();

						if (equalizer == null)
							return;

						try {
							short preset = (short) (i - 1);
							short eqPreset = equalizer.getCurrentPreset();

							if (eqPreset == preset)
								return;

							if (eqPreset == IEqualizer.PRESET_UNDEFINED && preset == (short) -1)
								return;

							if (preset == IEqualizer.PRESET_UNDEFINED)
								return;

							equalizer.usePreset(preset);

							updateEq(equalizer);
						} catch (Exception e) {
							e.printStackTrace();

							info("Update failed, try another preset or settings!");
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {

					}
				});
			}
		});

	}

	private void updateEq(final IEqualizer equalizer) {
		if (equalizer == null) {
			eq_checkBox.setOnClickListener(null);
			eq_checkBox.setChecked(false);
			eq_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setEqualizer(b);

					updateEq(getMusicService().getEqualizer());
				}
			});

			for (int i = 0; i < NUMBER_OF_BANDS; i++)
				bands[i].setEnabled(false);

			eq_preset_spinner.setEnabled(false);

			return;
		} else {
			eq_checkBox.setOnClickListener(null);
			eq_checkBox.setChecked(true);
			eq_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setEqualizer(b);

					updateEq(getMusicService().getEqualizer());
				}
			});

			for (int i = 0; i < NUMBER_OF_BANDS; i++)
				bands[i].setEnabled(true);

			eq_preset_spinner.setEnabled(true);
		}

		try {
			for (int i = 0; i < NUMBER_OF_BANDS; i++)
				bands[i].setProgress((int) (SEEKBAR_MAX * BandLevelNormalizer.normalize(equalizer.getBandLevel((short) i))));

			for (int i = 0; i < NUMBER_OF_BANDS; i++)
				bands[i].setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			for (int i = 0; i < NUMBER_OF_BANDS; i++)
				bands[i].setEnabled(false);
		}

		try {
			eq_preset_spinner.setSelection(equalizer.getCurrentPreset() + 1);
		} catch (Exception e) {
			e.printStackTrace();

			eq_preset_spinner.setEnabled(false);
		}
	}

	//endregion

	//region PreAmp

	private CheckBox preamp_checkBox;
	private VerticalSeekBar preamp_seekBar;

	private void createPreAmp() {
		preamp_checkBox = (CheckBox) findViewById(R.id.preamp_checkBox);

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

				if (!preamp_checkBox.isChecked())
					preamp_checkBox.setChecked(true);

				setNormalizedPreAmpLevel(preAmp, (float) progress / SEEKBAR_MAX);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

	}

	private void updatePreAmp(final IPreAmp preAmp) {
		if (preAmp == null) {
			preamp_checkBox.setOnClickListener(null);
			preamp_checkBox.setChecked(false);
			preamp_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setPreAmp(b);

					updatePreAmp(getMusicService().getPreAmp());
				}
			});
			preamp_seekBar.setEnabled(false);

			return;
		} else {
			preamp_checkBox.setOnClickListener(null);
			preamp_checkBox.setChecked(true);
			preamp_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setPreAmp(b);

					updatePreAmp(getMusicService().getPreAmp());
				}
			});

			preamp_seekBar.setEnabled(true);
		}

		try {
			preamp_seekBar.setProgress((int) (SEEKBAR_MAX * getNormalizedPreAmpLevel(preAmp)));

			preamp_seekBar.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			preamp_seekBar.setEnabled(false);
		}
	}

	//endregion

	//region BassBoost

	private CheckBox bassBoost_checkBox;
	private CircularSeekBar bassBoost_seekBar;

	private void createBassBoost() {
		bassBoost_checkBox = (CheckBox) findViewById(R.id.bassboost_checkBox);

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

				try {
					bassBoost.setStrength(BassboostNormalizer.denormalize(progress / SEEKBAR_MAX));
				} catch (Exception e) {
					info("Update failed, try another preset or settings!");
				}
			}

			@Override
			public void onStopTrackingTouch(CircularSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(CircularSeekBar seekBar) {

			}
		});

	}

	private void updateBassBoost(final IBassBoost bassBoost) {
		if (bassBoost == null) {
			bassBoost_checkBox.setOnClickListener(null);
			bassBoost_checkBox.setChecked(false);
			bassBoost_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setBassBoost(b);

					updateBassBoost(getMusicService().getBassBoost());
				}
			});
			bassBoost_seekBar.setEnabled(false);

			return;
		} else {
			bassBoost_checkBox.setOnClickListener(null);
			bassBoost_checkBox.setChecked(true);
			bassBoost_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setBassBoost(b);

					updateBassBoost(getMusicService().getBassBoost());
				}
			});

			bassBoost_seekBar.setEnabled(true);
		}

		try {
			bassBoost_seekBar.setProgress((int) (BassboostNormalizer.normalize(bassBoost.getRoundedStrength()) * SEEKBAR_MAX));

			bassBoost_seekBar.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			bassBoost_seekBar.setEnabled(false);
		}
	}

	//endregion

	//region LoudnessEnhancer

	private CheckBox loudnessEnhancer_checkBox;
	private CircularSeekBar loudnessEnhancer_seekBar;

	private void createLoudnessEnhancer() {
		loudnessEnhancer_checkBox = (CheckBox) findViewById(R.id.loudness_checkBox);

		loudnessEnhancer_seekBar = (CircularSeekBar) findViewById(R.id.loudness_seekBar);
		loudnessEnhancer_seekBar.setMax(SEEKBAR_MAX);
		loudnessEnhancer_seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
			@Override
			public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				ILoudnessEnhancer loudnessEnhancer = getMusicService().getLoudnessEnhancer();

				if (loudnessEnhancer == null)
					return;

				try {
					loudnessEnhancer.setTargetGain(LoudnessNormalizer.denormalize(progress / SEEKBAR_MAX));
				} catch (Exception e) {
					info("Update failed, try another preset or settings!");
				}
			}

			@Override
			public void onStopTrackingTouch(CircularSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(CircularSeekBar seekBar) {

			}
		});

	}

	private void updateLoudnessEnhancer(final ILoudnessEnhancer loudnessEnhancer) {
		if (loudnessEnhancer == null) {
			loudnessEnhancer_checkBox.setOnClickListener(null);
			loudnessEnhancer_checkBox.setChecked(false);
			loudnessEnhancer_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setLoudnessEnhancer(b);

					updateLoudnessEnhancer(getMusicService().getLoudnessEnhancer());
				}
			});
			loudnessEnhancer_seekBar.setEnabled(false);

			return;
		} else {
			loudnessEnhancer_checkBox.setOnClickListener(null);
			loudnessEnhancer_checkBox.setChecked(true);
			loudnessEnhancer_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setLoudnessEnhancer(b);

					updateLoudnessEnhancer(getMusicService().getLoudnessEnhancer());
				}
			});

			loudnessEnhancer_seekBar.setEnabled(true);
		}

		try {
			loudnessEnhancer_seekBar.setProgress(LoudnessNormalizer.normalize((int) loudnessEnhancer.getTargetGain()) * SEEKBAR_MAX);

			loudnessEnhancer_seekBar.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			loudnessEnhancer_seekBar.setEnabled(false);
		}
	}

	//endregion

	//region Virtualizer

	private CheckBox virtualizer_checkBox;
	private CircularSeekBar virtualizer_seekBar;

	private void createVirtualizer() {
		virtualizer_checkBox = (CheckBox) findViewById(R.id.virtualizer_checkBox);

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

				try {
					virtualizer.setStrength(VirtualizerNormalizer.denormalize(progress / SEEKBAR_MAX));
				} catch (Exception e) {
					info("Update failed, try another preset or settings!");
				}
			}

			@Override
			public void onStopTrackingTouch(CircularSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(CircularSeekBar seekBar) {

			}
		});

	}

	private void updateVirtualizer(final IVirtualizer virtualizer) {
		if (virtualizer == null) {
			virtualizer_checkBox.setOnClickListener(null);
			virtualizer_checkBox.setChecked(false);
			virtualizer_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setVirtualizer(b);

					updateVirtualizer(getMusicService().getVirtualizer());
				}
			});
			virtualizer_seekBar.setEnabled(false);

			return;
		} else {
			virtualizer_checkBox.setOnClickListener(null);
			virtualizer_checkBox.setChecked(true);
			virtualizer_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setVirtualizer(b);

					updateVirtualizer(getMusicService().getVirtualizer());
				}
			});

			virtualizer_seekBar.setEnabled(true);
		}

		try {
			virtualizer_seekBar.setProgress((int) (VirtualizerNormalizer.normalize(virtualizer.getRoundedStrength()) * SEEKBAR_MAX));

			virtualizer_seekBar.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			virtualizer_seekBar.setEnabled(false);
		}
	}

	//endregion

	//region EnvReverb

	private CheckBox reverb_env_checkBox;
	private Spinner reverb_env_preset_spinner;
	private SeekBar reverb_env_decay_hf_ratio_seekBar;
	private SeekBar reverb_env_decay_time_seekBar;
	private SeekBar reverb_env_density_seekBar;
	private SeekBar reverb_env_diffusion_seekBar;
	private SeekBar reverb_env_reflections_delay_seekBar;
	private SeekBar reverb_env_reflections_level_seekBar;
	private SeekBar reverb_env_reverb_delay_seekBar;
	private SeekBar reverb_env_reverb_level_seekBar;
	private SeekBar reverb_env_room_hf_level_seekBar;
	private SeekBar reverb_env_room_level_seekBar;

	private void createEnvReverb() {
		reverb_env_checkBox = (CheckBox) findViewById(R.id.reverb_env_checkBox);

		reverb_env_preset_spinner = (Spinner) findViewById(R.id.reverb_env_preset_spinner);
		ArrayList<String> reverb_env_presets = new ArrayList<>();
		reverb_env_presets.add(0, "Custom");
		for (Field field : FieldUtils.getAllFields(EnvironmentalReverbPresets.class)) {
			int modifiers = field.getModifiers();
			if ((Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier
					.isFinal(modifiers))) {
				reverb_env_presets.add(field.getName());
			}
		}
		final ArrayAdapter<String> reverb_env_preset_spinner_adapter = new ArrayAdapter<String>(this, R.layout.spinner_layout, reverb_env_presets);
		reverb_env_preset_spinner_adapter.setDropDownViewResource(R.layout.spinner_layout);
		reverb_env_preset_spinner.setAdapter(reverb_env_preset_spinner_adapter);
		reverb_env_preset_spinner.post(new Runnable() {
			@Override
			public void run() {
				reverb_env_preset_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
						if (getMusicService() == null)
							return;

						if (((String) adapterView.getItemAtPosition(i)).equalsIgnoreCase("Custom"))
							return;

						IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

						if (environmentalReverb == null)
							return;

						Field field = null;
						for (Field f : FieldUtils.getAllFields(EnvironmentalReverbPresets.class)) {
							if (f.getName().equals((String) adapterView.getItemAtPosition(i))) {
								field = f;
								break;
							}
						}

						if (field == null)
							return;

						try {
							IEnvironmentalReverb.Settings settings = (IEnvironmentalReverb.Settings) field.get(null);
							environmentalReverb.setProperties(settings);

							reverb_env_decay_hf_ratio_seekBar.setProgress((int) (DecayHFRatioNormalizer.normalize(settings.decayHFRatio) * SEEKBAR_MAX));
							reverb_env_decay_time_seekBar.setProgress((int) (DecayTimeNormalizer.normalize(settings.decayTime) * SEEKBAR_MAX));
							reverb_env_density_seekBar.setProgress((int) (DensityNormalizer.normalize(settings.density) * SEEKBAR_MAX));
							reverb_env_diffusion_seekBar.setProgress((int) (DiffusionNormalizer.normalize(settings.diffusion) * SEEKBAR_MAX));
							reverb_env_reflections_delay_seekBar.setProgress((int) (ReflectionsDelayNormalizer.normalize(settings.reflectionsDelay) * SEEKBAR_MAX));
							reverb_env_reflections_level_seekBar.setProgress((int) (ReflectionsLevelNormalizer.normalize(settings.reflectionsLevel) * SEEKBAR_MAX));
							reverb_env_reverb_delay_seekBar.setProgress((int) (ReverbDelayNormalizer.normalize(settings.reverbDelay) * SEEKBAR_MAX));
							reverb_env_reverb_level_seekBar.setProgress((int) (ReverbLevelNormalizer.normalize(settings.reverbLevel) * SEEKBAR_MAX));
							reverb_env_room_hf_level_seekBar.setProgress((int) (RoomHFLevelNormalizer.normalize(settings.roomHFLevel) * SEEKBAR_MAX));
							reverb_env_room_level_seekBar.setProgress((int) (RoomLevelNormalizer.normalize(settings.roomLevel) * SEEKBAR_MAX));

							info("Updated, requires restart for complete effect!");
						} catch (Exception e) {
							info("Updated failed, try another preset or settings!");

							e.printStackTrace();
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {

					}
				});
			}
		});

		reverb_env_decay_hf_ratio_seekBar = (SeekBar) findViewById(R.id.reverb_env_decay_hf_ratio_seekBar);
		reverb_env_decay_hf_ratio_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_decay_hf_ratio_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setDecayHFRatio(DecayHFRatioNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_decay_time_seekBar = (SeekBar) findViewById(R.id.reverb_env_decay_time_seekBar);
		reverb_env_decay_time_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_decay_time_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setDecayTime(DecayTimeNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_density_seekBar = (SeekBar) findViewById(R.id.reverb_env_density_seekBar);
		reverb_env_density_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_density_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setDensity(DensityNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_diffusion_seekBar = (SeekBar) findViewById(R.id.reverb_env_diffusion_seekBar);
		reverb_env_diffusion_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_diffusion_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setDiffusion(DiffusionNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_reflections_delay_seekBar = (SeekBar) findViewById(R.id.reverb_env_reflections_delay_seekBar);
		reverb_env_reflections_delay_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_reflections_delay_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setReflectionsDelay(ReflectionsDelayNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_reflections_level_seekBar = (SeekBar) findViewById(R.id.reverb_env_reflections_level_seekBar);
		reverb_env_reflections_level_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_reflections_level_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setReflectionsLevel(ReflectionsLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_reverb_delay_seekBar = (SeekBar) findViewById(R.id.reverb_env_reverb_delay_seekBar);
		reverb_env_reverb_delay_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_reverb_delay_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setReverbDelay(ReverbDelayNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_reverb_level_seekBar = (SeekBar) findViewById(R.id.reverb_env_reverb_level_seekBar);
		reverb_env_reverb_level_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_reverb_level_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setReverbLevel(ReverbLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_room_hf_level_seekBar = (SeekBar) findViewById(R.id.reverb_env_room_hf_level_seekBar);
		reverb_env_room_hf_level_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_room_hf_level_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setRoomHFLevel(RoomHFLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		reverb_env_room_level_seekBar = (SeekBar) findViewById(R.id.reverb_env_room_level_seekBar);
		reverb_env_room_level_seekBar.setMax(SEEKBAR_MAX);
		reverb_env_room_level_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				if (getMusicService() == null)
					return;

				IEnvironmentalReverb environmentalReverb = getMusicService().getEnvironmentalReverb();

				if (environmentalReverb == null)
					return;

				try {
					environmentalReverb.setRoomLevel(RoomLevelNormalizer.denormalize((float) progress / SEEKBAR_MAX));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
	}

	private void updateEnvReverb(IEnvironmentalReverb environmentalReverb) {
		if (environmentalReverb == null) {
			reverb_env_checkBox.setOnClickListener(null);
			reverb_env_checkBox.setChecked(false);
			reverb_env_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setEnvironmentalReverb(b);

					updateEnvReverb(getMusicService().getEnvironmentalReverb());
				}
			});

			reverb_env_preset_spinner.setEnabled(false);
			reverb_env_decay_hf_ratio_seekBar.setEnabled(false);
			reverb_env_decay_time_seekBar.setEnabled(false);
			reverb_env_density_seekBar.setEnabled(false);
			reverb_env_diffusion_seekBar.setEnabled(false);
			reverb_env_reflections_delay_seekBar.setEnabled(false);
			reverb_env_reflections_level_seekBar.setEnabled(false);
			reverb_env_reverb_delay_seekBar.setEnabled(false);
			reverb_env_reverb_level_seekBar.setEnabled(false);
			reverb_env_room_hf_level_seekBar.setEnabled(false);
			reverb_env_room_level_seekBar.setEnabled(false);

			return;
		} else {
			reverb_env_checkBox.setOnClickListener(null);
			reverb_env_checkBox.setChecked(true);
			reverb_env_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setEnvironmentalReverb(b);

					updateEnvReverb(getMusicService().getEnvironmentalReverb());
				}
			});

			reverb_env_preset_spinner.setEnabled(true);
			reverb_env_decay_hf_ratio_seekBar.setEnabled(true);
			reverb_env_decay_time_seekBar.setEnabled(true);
			reverb_env_density_seekBar.setEnabled(true);
			reverb_env_diffusion_seekBar.setEnabled(true);
			reverb_env_reflections_delay_seekBar.setEnabled(true);
			reverb_env_reflections_level_seekBar.setEnabled(true);
			reverb_env_reverb_delay_seekBar.setEnabled(true);
			reverb_env_reverb_level_seekBar.setEnabled(true);
			reverb_env_room_hf_level_seekBar.setEnabled(true);
			reverb_env_room_level_seekBar.setEnabled(true);
		}

		try {
			reverb_env_decay_hf_ratio_seekBar.setProgress((int) (DecayHFRatioNormalizer.normalize(environmentalReverb.getDecayHFRatio()) * SEEKBAR_MAX));
			reverb_env_decay_time_seekBar.setProgress((int) (DecayTimeNormalizer.normalize(environmentalReverb.getDecayTime()) * SEEKBAR_MAX));
			reverb_env_density_seekBar.setProgress((int) (DensityNormalizer.normalize(environmentalReverb.getDensity()) * SEEKBAR_MAX));
			reverb_env_diffusion_seekBar.setProgress((int) (DiffusionNormalizer.normalize(environmentalReverb.getDiffusion()) * SEEKBAR_MAX));
			reverb_env_reflections_delay_seekBar.setProgress((int) (ReflectionsDelayNormalizer.normalize(environmentalReverb.getReflectionsDelay()) * SEEKBAR_MAX));
			reverb_env_reflections_level_seekBar.setProgress((int) (ReflectionsLevelNormalizer.normalize(environmentalReverb.getReflectionsLevel()) * SEEKBAR_MAX));
			reverb_env_reverb_delay_seekBar.setProgress((int) (ReverbDelayNormalizer.normalize(environmentalReverb.getReverbDelay()) * SEEKBAR_MAX));
			reverb_env_reverb_level_seekBar.setProgress((int) (ReverbLevelNormalizer.normalize(environmentalReverb.getReverbLevel()) * SEEKBAR_MAX));
			reverb_env_room_hf_level_seekBar.setProgress((int) (RoomHFLevelNormalizer.normalize(environmentalReverb.getRoomHFLevel()) * SEEKBAR_MAX));
			reverb_env_room_level_seekBar.setProgress((int) (RoomLevelNormalizer.normalize(environmentalReverb.getRoomLevel()) * SEEKBAR_MAX));

			reverb_env_preset_spinner.setEnabled(true);
			reverb_env_decay_hf_ratio_seekBar.setEnabled(true);
			reverb_env_decay_time_seekBar.setEnabled(true);
			reverb_env_density_seekBar.setEnabled(true);
			reverb_env_diffusion_seekBar.setEnabled(true);
			reverb_env_reflections_delay_seekBar.setEnabled(true);
			reverb_env_reflections_level_seekBar.setEnabled(true);
			reverb_env_reverb_delay_seekBar.setEnabled(true);
			reverb_env_reverb_level_seekBar.setEnabled(true);
			reverb_env_room_hf_level_seekBar.setEnabled(true);
			reverb_env_room_level_seekBar.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			try {
				environmentalReverb.setProperties(EnvironmentalReverbPresets.DEFAULT);
			} catch (Exception e2) {
				e2.printStackTrace();

//                reverb_env_preset_spinner.setEnabled(false);
//                reverb_env_decay_hf_ratio_seekBar.setEnabled(false);
//                reverb_env_decay_time_seekBar.setEnabled(false);
//                reverb_env_density_seekBar.setEnabled(false);
//                reverb_env_diffusion_seekBar.setEnabled(false);
//                reverb_env_reflections_delay_seekBar.setEnabled(false);
//                reverb_env_reflections_level_seekBar.setEnabled(false);
//                reverb_env_reverb_delay_seekBar.setEnabled(false);
//                reverb_env_reverb_level_seekBar.setEnabled(false);
//                reverb_env_room_hf_level_seekBar.setEnabled(false);
//                reverb_env_room_level_seekBar.setEnabled(false);
			}

		}

	}

	//endregion

	//region PresetReverb

	private CheckBox reverb_checkBox;
	private Spinner reverb_preset_spinner;

	private void createPresetReverb() {
		reverb_checkBox = (CheckBox) findViewById(R.id.reverb_checkBox);

		reverb_preset_spinner = (Spinner) findViewById(R.id.reverb_preset_spinner);
		final ArrayAdapter<CharSequence> reverb_preset_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.aux_preset_reverb_preset_names, R.layout.spinner_layout);
		reverb_preset_spinner_adapter.setDropDownViewResource(R.layout.spinner_layout);
		reverb_preset_spinner.setAdapter(reverb_preset_spinner_adapter);
		reverb_preset_spinner.post(new Runnable() {
			public void run() {
				reverb_preset_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
						if (getMusicService() == null)
							return;

						IPresetReverb presetReverb = getMusicService().getPresetReverb();

						if (presetReverb == null)
							return;

						try {
							presetReverb.setPreset((short) i);

							info("Updated!");
						} catch (Exception e) {
							info("Update failed, try another preset or settings!");
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {

					}
				});
			}
		});
	}

	private void updatePresetReverb(final IPresetReverb presetReverb) {
		if (presetReverb == null) {
			reverb_checkBox.setOnClickListener(null);
			reverb_checkBox.setChecked(false);
			reverb_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setPresetReverb(b);

					updatePresetReverb(getMusicService().getPresetReverb());
				}
			});
			reverb_preset_spinner.setEnabled(false);

			return;
		} else {
			reverb_checkBox.setOnClickListener(null);
			reverb_checkBox.setChecked(true);
			reverb_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (getMusicService() == null)
						return;

					getMusicService().setPresetReverb(b);

					updatePresetReverb(getMusicService().getPresetReverb());
				}
			});

			reverb_preset_spinner.setEnabled(true);
		}

		try {
			reverb_preset_spinner.setSelection(presetReverb.getPreset());

			reverb_preset_spinner.setEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();

			reverb_preset_spinner.setEnabled(false);
		}
	}

	//endregion

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
	private static ShortParameterNormalizer BassboostNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);
	private static IntParameterNormalizer LoudnessNormalizer = new IntParameterNormalizer(0, 1000);
	private static ShortParameterNormalizer VirtualizerNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);

	private static final ShortParameterNormalizer DecayHFRatioNormalizer = new ShortParameterNormalizer((short) 100, (short) 2000);
	private static final IntParameterNormalizer DecayTimeNormalizer = new IntParameterNormalizer(100, 7000);
	private static final ShortParameterNormalizer DensityNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);
	private static final ShortParameterNormalizer DiffusionNormalizer = new ShortParameterNormalizer((short) 0, (short) 1000);
	private static final IntParameterNormalizer ReflectionsDelayNormalizer = new IntParameterNormalizer(0, 0);
	private static final ShortParameterNormalizer ReflectionsLevelNormalizer = new ShortParameterNormalizer((short) 0, (short) 0);
	private static final IntParameterNormalizer ReverbDelayNormalizer = new IntParameterNormalizer(0, 0);
	private static final ShortParameterNormalizer ReverbLevelNormalizer = new ShortParameterNormalizer((short) -9000, (short) 2000);
	private static final ShortParameterNormalizer RoomHFLevelNormalizer = new ShortParameterNormalizer((short) -9000, (short) 0);
	private static final ShortParameterNormalizer RoomLevelNormalizer = new ShortParameterNormalizer((short) -9000, (short) 0);

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
			} catch (Exception e) {
				Toast.makeText(context, "Tune might not work in your device!", Toast.LENGTH_LONG).show();

				Log.w(TAG, e);
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
