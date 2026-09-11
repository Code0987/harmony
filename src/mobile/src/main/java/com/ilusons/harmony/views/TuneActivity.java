package com.ilusons.harmony.views;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Placeholder until the system-EQ Tune UI is ported. */
public class TuneActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		tv.setText("Tune is being rebuilt for system audio effects.");
		tv.setPadding(48, 48, 48, 48);
		setContentView(tv);
	}
}
