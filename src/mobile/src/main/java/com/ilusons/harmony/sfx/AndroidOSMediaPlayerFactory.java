package com.ilusons.harmony.sfx;

import android.content.Context;

import com.h6ah4i.android.media.IBasicMediaPlayer;
import com.h6ah4i.android.media.IMediaPlayerFactory;
import com.h6ah4i.android.media.audiofx.IBassBoost;
import com.h6ah4i.android.media.audiofx.IEnvironmentalReverb;
import com.h6ah4i.android.media.audiofx.IEqualizer;
import com.h6ah4i.android.media.audiofx.IHQVisualizer;
import com.h6ah4i.android.media.audiofx.ILoudnessEnhancer;
import com.h6ah4i.android.media.audiofx.IPreAmp;
import com.h6ah4i.android.media.audiofx.IPresetReverb;
import com.h6ah4i.android.media.audiofx.IVirtualizer;
import com.h6ah4i.android.media.audiofx.IVisualizer;
import com.h6ah4i.android.media.standard.StandardMediaPlayer;
import com.h6ah4i.android.media.standard.audiofx.StandardBassBoost;
import com.h6ah4i.android.media.standard.audiofx.StandardEnvironmentalReverb;
import com.h6ah4i.android.media.standard.audiofx.StandardEqualizer;
import com.h6ah4i.android.media.standard.audiofx.StandardLoudnessEnhancer;
import com.h6ah4i.android.media.standard.audiofx.StandardPresetReverb;
import com.h6ah4i.android.media.standard.audiofx.StandardVirtualizer;
import com.h6ah4i.android.media.standard.audiofx.StandardVisualizer;

public class AndroidOSMediaPlayerFactory implements IMediaPlayerFactory {
	private Context mContext;

	public AndroidOSMediaPlayerFactory(Context context) {
		mContext = context.getApplicationContext();
	}

	@Override
	public void release() throws IllegalStateException, UnsupportedOperationException {
		mContext = null;
	}

	@Override
	public IBasicMediaPlayer createMediaPlayer()
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return new StandardMediaPlayer();
	}

	@Override
	public IBassBoost createBassBoost(int audioSession)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return new StandardBassBoost(1, audioSession);
	}

	@Override
	public IBassBoost createBassBoost(IBasicMediaPlayer player)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkIsStandardMediaPlayer(player);
		return new StandardBassBoost(1, ((StandardMediaPlayer) player).getAudioSessionId());
	}

	@Override
	public IEqualizer createEqualizer(int audioSession)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return new StandardEqualizer(1, audioSession);
	}

	@Override
	public IEqualizer createEqualizer(IBasicMediaPlayer player)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkIsStandardMediaPlayer(player);
		return new StandardEqualizer(1, ((StandardMediaPlayer) player).getAudioSessionId());
	}

	@Override
	public IVirtualizer createVirtualizer(int audioSession)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return new StandardVirtualizer(1, audioSession);
	}

	@Override
	public IVirtualizer createVirtualizer(IBasicMediaPlayer player)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkIsStandardMediaPlayer(player);
		return new StandardVirtualizer(1, ((StandardMediaPlayer) player).getAudioSessionId());
	}

	@Override
	public IVisualizer createVisualizer(int audioSession)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		return new StandardVisualizer(mContext, audioSession);
	}

	@Override
	public IVisualizer createVisualizer(IBasicMediaPlayer player)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		checkIsStandardMediaPlayer(player);
		return new StandardVisualizer(mContext, ((StandardMediaPlayer) player).getAudioSessionId());
	}

	@Override
	public ILoudnessEnhancer createLoudnessEnhancer(int audioSession) throws IllegalStateException,
			IllegalArgumentException, UnsupportedOperationException {
		final StandardLoudnessEnhancer effect = new StandardLoudnessEnhancer(audioSession);
		if (effect == null) {
			throw new UnsupportedOperationException("StandardLoudnessEnhancer is not supported");
		}
		return effect;
	}

	@Override
	public ILoudnessEnhancer createLoudnessEnhancer(IBasicMediaPlayer player)
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		final StandardLoudnessEnhancer effect = new StandardLoudnessEnhancer(((StandardMediaPlayer) player).getAudioSessionId());
		if (effect == null) {
			throw new UnsupportedOperationException("StandardLoudnessEnhancer is not supported");
		}
		return effect;
	}

	@Override
	public IPresetReverb createPresetReverb()
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		// NOTE: Auxiliary effects can be created for session 0 only
		return new StandardPresetReverb(1, 0);
	}

	@Override
	public IEnvironmentalReverb createEnvironmentalReverb()
			throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
		// NOTE: Auxiliary effects can be created for session 0 only
		return new StandardEnvironmentalReverb(1, 0);
	}

	@Override
	public IEqualizer createHQEqualizer() throws IllegalStateException,
			IllegalArgumentException, UnsupportedOperationException {
		throw new UnsupportedOperationException("HQEqualizer is not supported");
	}

	@Override
	public IHQVisualizer createHQVisualizer() throws IllegalStateException,
			IllegalArgumentException, UnsupportedOperationException {
		throw new UnsupportedOperationException("HQVisualizer is not supported");
	}

	@Override
	public IPreAmp createPreAmp() throws IllegalStateException, IllegalArgumentException,
			UnsupportedOperationException {
		throw new UnsupportedOperationException("PreAmp is not supported");
	}

	protected static void checkIsStandardMediaPlayer(IBasicMediaPlayer player) {
		if (player == null)
			throw new IllegalArgumentException("The argument 'player' is null");
		if (!(player instanceof StandardMediaPlayer))
			throw new IllegalArgumentException("The player is not instance of OpenSLMediaPlayer");
	}
}
