package com.ilusons.harmony.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class AboutViewFragment extends Fragment {

	// Logger TAG
	private static final String TAG = AboutViewFragment.class.getSimpleName();

	private View root;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Context
		ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.AppTheme);

		// Set view
		View v = inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.about_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		createAbout(v);

		return v;
	}

	private void createAbout(final View v) {
		/*
		try {
			LottieAnimationView about_animation_view = v.findViewById(R.id.about_animation_view);
			about_animation_view.pauseAnimation();
			about_animation_view.setAnimation("confetti.json", LottieAnimationView.CacheStrategy.Weak);
			about_animation_view.loop(true);
			about_animation_view.setScale(1);
			about_animation_view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			about_animation_view.clearColorFilters();
			about_animation_view.playAnimation();
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/

		((TextView) v.findViewById(R.id.about_version)).setText(BuildConfig.VERSION_NAME);

		v.findViewById(R.id.about_license).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String content;
				try (InputStream is = getResources().openRawResource(R.raw.license)) {
					content = IOUtils.toString(is, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();

					content = "Error loading data!";
				}

				(new AlertDialog.Builder(new ContextThemeWrapper(v.getContext(), R.style.AppTheme_AlertDialogStyle))
						.setTitle("Licenses")
						.setMessage(content)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
							}
						}))
						.show();
			}
		});

		v.findViewById(R.id.about_info).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String content;
				try (InputStream is = getResources().openRawResource(R.raw.gps_listing)) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY).toString();
					} else {
						content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>")).toString();
					}
				} catch (Exception e) {
					e.printStackTrace();

					content = "Error loading data!";
				}

				(new AlertDialog.Builder(new ContextThemeWrapper(v.getContext(), R.style.AppTheme_AlertDialogStyle))
						.setTitle("Information")
						.setMessage(content)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
							}
						}))
						.show();
			}
		});

		v.findViewById(R.id.about_release_notes).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SettingsActivity.showReleaseNotesDialog(v.getContext());
			}
		});

		v.findViewById(R.id.about_credits).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showAboutCredits();
			}
		});

	}

	private void showAboutCredits() {
		try {
			View v = ((LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.about_credits, null);

			AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_AlertDialogStyle);
			builder.setView(v);

			AlertDialog alert = builder.create();

			alert.requestWindowFeature(DialogFragment.STYLE_NO_TITLE);

			alert.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showAsDialog(Context context) {
		FragmentDialogActivity.show(context, AboutViewFragment.class, Bundle.EMPTY);
	}

	public static AboutViewFragment create() {
		AboutViewFragment f = new AboutViewFragment();
		return f;
	}

}
