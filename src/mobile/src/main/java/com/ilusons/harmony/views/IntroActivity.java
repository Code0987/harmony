package com.ilusons.harmony.views;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;

import androidx.core.app.ActivityCompat;

import io.github.dreierf.materialintroscreen.MaterialIntroActivity;
import io.github.dreierf.materialintroscreen.MessageButtonBehaviour;
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {

	public static final String TAG = IntroActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		enableLastSlideAlphaExitTransition(true);

		// The intro "Grant" button only works for permissions declared in the manifest.
		// Ask immediately so the user is not stuck if the library button is a no-op.
		ActivityCompat.requestPermissions(this, allIntroPermissions(), 15621);

		addSlide(new SlideFragmentBuilder()
						.backgroundColor(R.color.colorPrimaryDark)
						.buttonsColor(R.color.gradient43)
						.possiblePermissions(optionalPermissions())
						.neededPermissions(requiredAudioPermissions())
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
						Intent intent = new Intent(IntroActivity.this, TuneActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						startActivity(intent);
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

	private static String[] requiredAudioPermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			return new String[]{Manifest.permission.READ_MEDIA_AUDIO};
		}
		return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
	}

	private static String[] optionalPermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			return new String[]{Manifest.permission.POST_NOTIFICATIONS};
		}
		return new String[0];
	}

	private static String[] allIntroPermissions() {
		if (Build.VERSION.SDK_INT >= 33) {
			return new String[]{
					Manifest.permission.READ_MEDIA_AUDIO,
					Manifest.permission.POST_NOTIFICATIONS
			};
		}
		return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
	}

	@Override
	public void onFinish() {
		super.onFinish();

		Toast.makeText(this, ":)", Toast.LENGTH_SHORT).show();

		MainActivity.openDashboardActivity(getApplicationContext());
	}

}
