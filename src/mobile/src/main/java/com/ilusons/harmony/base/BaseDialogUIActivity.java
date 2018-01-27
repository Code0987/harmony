package com.ilusons.harmony.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.ilusons.harmony.R;
import com.ilusons.harmony.ref.AndroidEx;

public class BaseDialogUIActivity extends BaseUIActivity {

	public static final String FRAGMENT_CLASS = "fragmentClass";
	public static final String FRAGMENT_ARGUMENTS = "fragmentArguments";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.height = Resources.getSystem().getDisplayMetrics().heightPixels - AndroidEx.dpToPx(2 * 48);
		params.width = Resources.getSystem().getDisplayMetrics().widthPixels - AndroidEx.dpToPx(2 * 32);
		params.alpha = 1.0f;
		params.dimAmount = 0.85f;
		getWindow().setAttributes(params);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		if (getSupportActionBar() != null)
			getSupportActionBar().hide();

		if (savedInstanceState == null) {
			Fragment fragment = Fragment.instantiate(this, getIntent().getStringExtra(FRAGMENT_CLASS));

			fragment.setArguments(getIntent().getBundleExtra(FRAGMENT_ARGUMENTS));

			getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
		}

	}

	public static void show(Context context, Class<?> fragmentClass, Bundle arguments) {
		Intent intent = new Intent(context, BaseDialogUIActivity.class);
		intent.putExtra(FRAGMENT_CLASS, fragmentClass.getName());
		intent.putExtra(FRAGMENT_ARGUMENTS, arguments);
		context.startActivity(intent);
	}

}
