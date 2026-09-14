package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ilusons.harmony.R;
import com.ilusons.harmony.avfx.LiteVfxView;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.SPrefEx;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;

public class AudioVFXViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = AudioVFXViewFragment.class.getSimpleName();

	private MusicService musicService;

	private LiteVfxView vfxView;

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
		hasPermissions = true;
		if (pendingActionForViewReference != null && pendingActionForViewReference.get() != null)
			pendingActionForViewReference.get().execute();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		vfxView = null;
	}

	public void reset(final MusicService musicService, final AVFXType avfxType, final int color) {
		if (musicService == null)
			return;

		if (!isAdded() || root == null || !hasPermissions) {
			pendingActionForViewReference = new WeakReference<JavaEx.Action>(new JavaEx.Action() {
				@Override
				public void execute() {
					reset(musicService, avfxType, color);
				}
			});
			return;
		}

		try {
			this.musicService = musicService;
			root.removeAllViews();

			vfxView = new LiteVfxView(getActivity());
			vfxView.setColor(color == 0 ? Color.CYAN : color);
			switch (avfxType) {
				case Waveform:
					vfxView.setStyle(LiteVfxView.Style.WAVE);
					break;
				case Particles:
					vfxView.setStyle(LiteVfxView.Style.DOTS);
					break;
				case Dots:
					vfxView.setStyle(LiteVfxView.Style.DOTS);
					break;
				case Circles:
				default:
					vfxView.setStyle(LiteVfxView.Style.RINGS);
					break;
			}
			root.addView(vfxView, new FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		} catch (Exception e) {
			Log.w(TAG, e);
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

