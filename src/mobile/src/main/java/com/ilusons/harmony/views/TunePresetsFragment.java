package com.ilusons.harmony.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Full Tune UI is restored in a later phase. */
public class TunePresetsFragment extends Fragment {
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		TextView tv = new TextView(getContext());
		tv.setText("Tune presets will return with the system-EQ port.");
		tv.setPadding(48, 48, 48, 48);
		return tv;
	}

	public static TunePresetsFragment create() {
		return new TunePresetsFragment();
	}

	public static final String PRESET_HQ_GENERAL = "general.hqp";
	public static final String PRESET_SQ_GENERAL = "general.sqp";

	public static void applyPreset(android.content.Context context, String preset) {
	}
}
