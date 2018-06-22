package com.ilusons.harmony.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {

	public static final String TAG = IntroActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		enableLastSlideAlphaExitTransition(true);

		addSlide(new SlideFragmentBuilder()
						.backgroundColor(R.color.colorPrimaryDark)
						.buttonsColor(R.color.gradient43)
						.possiblePermissions(new String[]{Manifest.permission.RECORD_AUDIO})
						.neededPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
						.image(R.drawable.logo)
						.title(getString(R.string.app_name))
						.description("Next, we're gonna change\nthe way you play music!")
						.build(),
				new MessageButtonBehaviour(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity.gotoPlayStore(IntroActivity.this);
					}
				}, "Checkout at PlayStore!"));

		addSlide(new SlideFragmentBuilder()
						.backgroundColor(R.color.gradient11)
						.buttonsColor(R.color.gradient13)
						.image(R.drawable.ic_intro_tune)
						.title("What's your tune?")
						.description("Choose your tune from\nfine tuned presets!")
						.build(),
				new MessageButtonBehaviour(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						TunePresetsFragment.showAsDialog(IntroActivity.this);
					}
				}, "Choose!"));

		addSlide(new SlideFragmentBuilder()
						.backgroundColor(R.color.gradient11)
						.buttonsColor(R.color.gradient13)
						.image(R.drawable.logo_lastfm)
						.title("last.fm ® Scrobbler")
						.description("Do you have last.fm ® account?\nWhy not connect it?\nWe'll scrobble your music and\nprovide you smart recommendations!")
						.build(),
				new MessageButtonBehaviour(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(IntroActivity.this, SettingsActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(SettingsActivity.ShowLFMSectionOnStart, true);
						startActivity(intent);
					}
				}, "Connect!"));

		addSlide(new SlideFragmentBuilder()
						.backgroundColor(R.color.gradient41)
						.buttonsColor(R.color.gradient43)
						.image(R.drawable.ic_intro_folder)
						.title("Where's your local music?")
						.description("Don't worry if don't have any local music!\nJust skip this!")
						.build(),
				new MessageButtonBehaviour(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(IntroActivity.this, SettingsActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(SettingsActivity.TAG_BehaviourForAddScanLocationOnEmptyLibrary, true);
						startActivity(intent);
					}
				}, "Add folders!"));

	}

	@Override
	public void onFinish() {
		super.onFinish();

		Toast.makeText(this, ":)", Toast.LENGTH_SHORT).show();

		MainActivity.openDashboardActivity(getApplicationContext());
	}

}
