package com.ilusons.harmony.base;

import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.ilusons.harmony.R;

public class BaseUIFragment extends Fragment {

	public BaseUIActivity getBaseUIActivity() {
		if (getActivity() instanceof BaseUIActivity)
			return ((BaseUIActivity) getActivity());
		return null;
	}

	public MusicService getMusicService() {
		if (getBaseUIActivity() != null)
			return getBaseUIActivity().getMusicService();
		else
			return null;
	}

	public void info(String s) {
		try {
			if (getBaseUIActivity() != null)
				getBaseUIActivity().info(s);
			else
				Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void info(String s, boolean i) {
		if (getBaseUIActivity() != null)
			getBaseUIActivity().info(s, i);
		else
			Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
	}

}
