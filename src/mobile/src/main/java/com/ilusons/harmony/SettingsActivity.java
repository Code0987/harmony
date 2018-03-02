package com.ilusons.harmony;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.appyvet.materialrangebar.RangeBar;
import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.HeadsetMediaButtonIntentReceiver;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.data.Analytics;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.ViewEx;
import com.ilusons.harmony.ref.inappbilling.IabBroadcastReceiver;
import com.ilusons.harmony.ref.inappbilling.IabHelper;
import com.ilusons.harmony.ref.inappbilling.IabResult;
import com.ilusons.harmony.ref.inappbilling.Inventory;
import com.ilusons.harmony.ref.inappbilling.Purchase;
import com.ilusons.harmony.views.AudioVFXViewFragment;
import com.ilusons.harmony.views.PlaybackUIActivity;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleResult;

import static com.ilusons.harmony.base.MusicService.SKU_PREMIUM;

public class SettingsActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = SettingsActivity.class.getSimpleName();

	private static final int REQUEST_SCAN_LOCATIONS_PICK = 11;

	// IAB
	private static final int REQUEST_SKU_PREMIUM = 1401;

	private IabHelper iabHelper;
	private IabBroadcastReceiver iabBroadcastReceiver;
	private IabBroadcastReceiver.IabBroadcastListener iabBroadcastListener = new IabBroadcastReceiver.IabBroadcastListener() {
		@Override
		public void receivedBroadcast() {
			try {
				iabHelper.queryInventoryAsync(gotInventoryListener);
			} catch (IabHelper.IabAsyncInProgressException e) {
				Log.d(TAG, "Error querying inventory. Another async operation in progress.", e);
			}
		}
	};
	private IabHelper.QueryInventoryFinishedListener gotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.\n" + result);

			if (iabHelper == null) return;

			if (result.isFailure()) {
				return;
			}

			try {
				Purchase purchase = inventory.getPurchase(SKU_PREMIUM);

				premiumChanged((purchase != null && MusicService.verifyDeveloperPayload(SettingsActivity.this, purchase)));
			} catch (Exception e) {
				Log.w(TAG, e);
			}

			loading(false);
		}
	};

	private IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished\n" + result + "\n" + purchase);

			if (iabHelper == null) return;

			if (result.isFailure()) {
				info("Error purchasing.", true);

				loading(false);

				return;
			}

			if (!MusicService.verifyDeveloperPayload(SettingsActivity.this, purchase)) {
				info("Error purchasing. Authenticity verification failed.", true);

				loading(false);

				return;
			}

			Log.d(TAG, "Purchase successful.\n" + purchase);

			if (purchase.getSku().equals(SKU_PREMIUM)) {
				info("Thank you for upgrading to premium! All premium features will work correctly after restart!", true);

				premiumChanged(true);

				loading(false);
			}
		}
	};
	private ImageButton premium;

	// UI
	private View root;
	private AVLoadingIndicatorView loading;

	private ViewEx.StaticViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Set view
		setContentView(R.layout.settings_activity);

		// Set views
		root = findViewById(R.id.root);
		loading = findViewById(R.id.loading);

		// IAB
		/* TODO: Free for limited time
		iabBroadcastReceiver = new IabBroadcastReceiver(iabBroadcastListener);

		iabHelper = new IabHelper(this, MusicService.LICENSE_BASE64_PUBLIC_KEY);
		if (BuildConfig.DEBUG)
			iabHelper.enableDebugLogging(true, TAG);
		iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				if (!result.isSuccess()) {
					info("Problem setting up in-app billing!");

					Log.w(TAG, result.toString());

					return;
				}

				if (iabHelper == null) return;

				// Important: Dynamically register for broadcast messages about updated purchases.
				// We register the receiver he re instead of as a <receiver> in the Manifest
				// because we always call getPurchases() at startup, so therefore we can ignore
				// any broadcasts sent while the app isn't running.
				// Note: registering this listener in an Activity is a bad idea, but is done here
				// because this is a SAMPLE. Regardless, the receiver must be registered after
				// IabHelper is setup, but before first call to getPurchases().
				IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
				registerReceiver(iabBroadcastReceiver, broadcastFilter);

				try {
					iabHelper.queryInventoryAsync(gotInventoryListener);
				} catch (IabHelper.IabAsyncInProgressException e) {
					Log.d(TAG, "Error querying inventory. Another async operation in progress.", e);
				}
			}
		});
		*/

		// Set close
		findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});

		// Set premium
		/* TODO: Free for limited time
		findViewById(R.id.premium).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isFinishing())
					return;

				String content;
				try (InputStream is = getResources().openRawResource(R.raw.notes_premium)) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
						content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY).toString();
					} else {
						content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>")).toString();
					}
				} catch (Exception e) {
					e.printStackTrace();

					content = "Error loading data!";
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle));
				builder.setTitle("Purchase premium?");
				builder.setMessage(content);
				builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						loading(true);

						dialog.dismiss();

						String payload = MusicService.getDeveloperPayload(SettingsActivity.this, SKU_PREMIUM);

						try {
							iabHelper.launchPurchaseFlow(SettingsActivity.this, SKU_PREMIUM, REQUEST_SKU_PREMIUM, purchaseFinishedListener, payload);
						} catch (IabHelper.IabAsyncInProgressException e) {
							info("Error launching purchase flow. Another async operation in progress.");

							loading(false);
						}
					}
				});
				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();

						info(":(");
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});
		*/
		findViewById(R.id.premium).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				info(getString(R.string.app_name) + " is Now FREE \uD83E\uDD2F for LIMITED Time!");
			}
		});
		premiumChanged(true);

		// Set views and tabs
		viewPager = (ViewEx.StaticViewPager) findViewById(R.id.viewPager);
		TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);

		// UI section
		onCreateBindUISection();

		// Library section
		onCreateBindLibrarySection();

		findViewById(R.id.reset_imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				(new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle))
						.setTitle("Sure?")
						.setMessage("App will become like new, all your personalized content will be lost!")
						.setCancelable(true)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								try {
									info("Reset initiated! App will restart in a moment!");

									IOEx.deleteCache(getApplicationContext());

									((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									AndroidEx.restartApp(SettingsActivity.this);
								}
							}
						})
						.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								dialogInterface.dismiss();
							}
						}))
						.show();
			}
		});

		// Player type
		RadioGroup player_type_radioGroup = (RadioGroup) findViewById(R.id.player_type_radioGroup);

		CompoundButton.OnCheckedChangeListener player_type_onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (!b)
					return;

				MusicService.setPlayerType(SettingsActivity.this, (MusicService.PlayerType) compoundButton.getTag());

				info("Player type will be changed after restart!");
			}
		};

		MusicService.PlayerType player_type = MusicService.getPlayerType(getApplicationContext());

		for (MusicService.PlayerType value : MusicService.PlayerType.values()) {
			RadioButton rb = new RadioButton(this);
			rb.setText(value.getFriendlyName());
			rb.setTag(value);
			rb.setId(value.ordinal());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				rb.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
			}
			player_type_radioGroup.addView(rb);

			if (player_type == value)
				rb.setChecked(true);

			rb.setOnCheckedChangeListener(player_type_onCheckedChangeListener);
		}

		// Headset
		CheckBox headset_auto_play_on_plug_checkBox = (CheckBox) findViewById(R.id.headset_auto_play_on_plug_checkBox);
		headset_auto_play_on_plug_checkBox.setChecked(HeadsetMediaButtonIntentReceiver.getHeadsetAutoPlayOnPlug(this));
		headset_auto_play_on_plug_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				HeadsetMediaButtonIntentReceiver.setHeadsetAutoPlayOnPlug(SettingsActivity.this, compoundButton.isChecked());

				info("Updated!");
			}
		});

		// Analytics
		createLFM();

		// DC
		createDC();

		loading(false);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		try {
			if (iabBroadcastReceiver != null) {
				unregisterReceiver(iabBroadcastReceiver);
			}

			if (iabHelper != null) {
				iabHelper.disposeWhenFinished();
				iabHelper = null;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult::" + requestCode + "," + resultCode + "," + data);

		if (requestCode == REQUEST_SCAN_LOCATIONS_PICK && resultCode == Activity.RESULT_OK) {
			try {
				List<Uri> files = Utils.getSelectedFilesFromResult(data);
				for (Uri uri : files) {
					File file = Utils.getFileForUri(uri);

					scanLocationsRecyclerViewAdapter.addData(file.getAbsolutePath());
				}


			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {

			if (iabHelper == null) return;

			if (!iabHelper.handleActivityResult(requestCode, resultCode, data)) {
				super.onActivityResult(requestCode, resultCode, data);
			}

		}
	}

	private void loading(boolean set) {
		if (set) loading.smoothToShow();
		else loading.smoothToHide();
	}

	private void premiumChanged(boolean isPremium) {
		if (isPremium) {
			findViewById(R.id.premium).setVisibility(View.GONE);
		} else {
			findViewById(R.id.premium).setVisibility(View.VISIBLE);
		}
	}

	public static void showReleaseNotesDialog(Context context) {
		String content;
		try (InputStream is = context.getResources().openRawResource(R.raw.notes_release)) {
			content = IOUtils.toString(is, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();

			content = "Error loading data!";
		}

		(new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme_AlertDialogStyle))
				.setTitle("Release notes")
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

	//region UI section

	private void onCreateBindUISection() {

		createPlaybackUIStyle();
		createAVFXType();

	}

	//region PlaybackUI style

	private Spinner playbackUIStyle_spinner;

	private void createPlaybackUIStyle() {
		playbackUIStyle_spinner = (Spinner) findViewById(R.id.playbackUIStyle_spinner);

		PlaybackUIActivity.PlaybackUIStyle[] items = PlaybackUIActivity.PlaybackUIStyle.values();

		playbackUIStyle_spinner.setAdapter(new ArrayAdapter<PlaybackUIActivity.PlaybackUIStyle>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText(text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		PlaybackUIActivity.PlaybackUIStyle lastMode = PlaybackUIActivity.getPlaybackUIStyle(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		playbackUIStyle_spinner.setSelection(i, true);

		playbackUIStyle_spinner.post(new Runnable() {
			public void run() {
				playbackUIStyle_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						PlaybackUIActivity.setPlaybackUIStyle(getApplicationContext(), (PlaybackUIActivity.PlaybackUIStyle) adapterView.getItemAtPosition(position));

						info(getString(R.string.will_apply_after_restart));
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	//endregion

	//region AVFXType

	private Spinner avfxtype_spinner;

	private void createAVFXType() {
		avfxtype_spinner = (Spinner) findViewById(R.id.avfxtype_spinner);

		AudioVFXViewFragment.AVFXType[] items = AudioVFXViewFragment.AVFXType.values();

		avfxtype_spinner.setAdapter(new ArrayAdapter<AudioVFXViewFragment.AVFXType>(this, 0, items) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) getDropDownView(position, convertView, parent);

				text.setText(text.getText());

				return text;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent) {
				CheckedTextView text = (CheckedTextView) convertView;

				if (text == null) {
					text = new CheckedTextView(getContext(), null, android.R.style.TextAppearance_Material_Widget_TextView_SpinnerItem);
					text.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
					ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
					lp.setMargins(px, px, px, px);
					text.setLayoutParams(lp);
					text.setPadding(px, px, px, px);
				}

				text.setText(getItem(position).friendlyName);

				return text;
			}
		});

		int i = 0;
		AudioVFXViewFragment.AVFXType lastMode = AudioVFXViewFragment.getAVFXType(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		avfxtype_spinner.setSelection(i, true);

		avfxtype_spinner.post(new Runnable() {
			public void run() {
				avfxtype_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						AudioVFXViewFragment.setAVFXType(getApplicationContext(), (AudioVFXViewFragment.AVFXType) adapterView.getItemAtPosition(position));
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	//endregion

	//endregion

	//region Library section

	public static final String TAG_BehaviourForAddScanLocationOnEmptyLibrary = TAG + "_BehaviourForAddScanLocationOnEmptyLibrary";

	private ScanLocationsRecyclerViewAdapter scanLocationsRecyclerViewAdapter;

	private void onCreateBindLibrarySection() {
		CheckBox library_scan_auto_checkBox = (CheckBox) findViewById(R.id.library_scan_auto_checkBox);
		library_scan_auto_checkBox.setChecked(MusicServiceLibraryUpdaterAsyncTask.getScanAutoEnabled(this));
		library_scan_auto_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				MusicServiceLibraryUpdaterAsyncTask.setScanAutoEnabled(SettingsActivity.this, compoundButton.isChecked());
			}
		});

		final EditText library_scan_auto_interval_editText = (EditText) findViewById(R.id.library_scan_auto_interval_editText);
		library_scan_auto_interval_editText.setText("");
		library_scan_auto_interval_editText.append(String.valueOf(MusicServiceLibraryUpdaterAsyncTask.getScanAutoInterval(this)));
		library_scan_auto_interval_editText.clearFocus();
		findViewById(R.id.library_scan_auto_interval_imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final HmsPickerDialogFragment.HmsPickerDialogHandlerV2 handler = new HmsPickerDialogFragment.HmsPickerDialogHandlerV2() {
					@Override
					public void onDialogHmsSet(int reference, boolean isNegative, int hours, int minutes, int seconds) {
						Long value = ((((hours * 60L) + minutes) * 60) + seconds) * 1000;

						if (!(value <= (48 * 60 * 60 * 1000) && value >= (7 * 60 * 60 * 1000))) {
							info("Enter value between [7hrs, 48hrs]", true);

							return;
						}

						MusicServiceLibraryUpdaterAsyncTask.setScanAutoInterval(SettingsActivity.this, value);

						library_scan_auto_interval_editText.setText("");
						library_scan_auto_interval_editText.append(String.valueOf(value));
						library_scan_auto_interval_editText.clearFocus();
					}
				};
				final HmsPickerBuilder hpb = new HmsPickerBuilder()
						.setFragmentManager(getSupportFragmentManager())
						.setStyleResId(R.style.BetterPickersDialogFragment);
				hpb.addHmsPickerDialogHandler(handler);
				hpb.setOnDismissListener(new OnDialogDismissListener() {
					@Override
					public void onDialogDismiss(DialogInterface dialoginterface) {
						hpb.removeHmsPickerDialogHandler(handler);
					}
				});
				hpb.setTimeInMilliseconds(MusicServiceLibraryUpdaterAsyncTask.getScanAutoInterval(SettingsActivity.this));
				hpb.show();
			}
		});

		// Set scan locations
		RecyclerView scan_locations_recyclerView = (RecyclerView) findViewById(R.id.scan_locations_recyclerView);
		scan_locations_recyclerView.setHasFixedSize(true);
		scan_locations_recyclerView.setItemViewCacheSize(3);
		scan_locations_recyclerView.setDrawingCacheEnabled(true);
		scan_locations_recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		scanLocationsRecyclerViewAdapter = new ScanLocationsRecyclerViewAdapter();
		scan_locations_recyclerView.setAdapter(scanLocationsRecyclerViewAdapter);

		scanLocationsRecyclerViewAdapter.setData(MusicServiceLibraryUpdaterAsyncTask.getScanLocations(this));

		findViewById(R.id.scan_locations_imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// This always works
				Intent i = new Intent(SettingsActivity.this, FilePickerActivity.class);
				// This works if you defined the intent filter
				// Intent i = new Intent(Intent.ACTION_GET_CONTENT);

				// Set these depending on your use case. These are the defaults.
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
				i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

				// Configure initial directory by specifying a String.
				// You could specify a String like "/storage/emulated/0/", but that can
				// dangerous. Always use Android's API calls to get paths to the SD-card or
				// internal memory.
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

				startActivityForResult(i, REQUEST_SCAN_LOCATIONS_PICK);
			}
		});

		// Set scan media store
		CheckBox scan_mediastore_enabled_checkBox = (CheckBox) findViewById(R.id.scan_mediastore_enabled_checkBox);
		scan_mediastore_enabled_checkBox.setChecked(MusicServiceLibraryUpdaterAsyncTask.getScanMediaStoreEnabled(this));
		scan_mediastore_enabled_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				MusicServiceLibraryUpdaterAsyncTask.setScanMediaStoreEnabled(SettingsActivity.this, compoundButton.isChecked());

				info("Updated!");
			}
		});

		// Set scan constraint min/max duration
		final RangeBar scan_constraint_min_max_duration_rangeBar = findViewById(R.id.scan_constraint_min_max_duration_rangeBar);

		final long FACTOR = 1000;

		try {
			scan_constraint_min_max_duration_rangeBar.setRangePinsByValue(
					MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMinDuration(this) / FACTOR,
					MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMaxDuration(this) / FACTOR);
		} catch (Exception e) {
			e.printStackTrace();
		}

		scan_constraint_min_max_duration_rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
			@Override
			public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
				final int INTERVAL = (int) scan_constraint_min_max_duration_rangeBar.getTickInterval();

				MusicServiceLibraryUpdaterAsyncTask.setScanConstraintMinDuration(SettingsActivity.this, leftPinIndex * INTERVAL * FACTOR);
				MusicServiceLibraryUpdaterAsyncTask.setScanConstraintMaxDuration(SettingsActivity.this, rightPinIndex * INTERVAL * FACTOR);
			}
		});

		// Library update fast mode
		CheckBox library_update_fastMode_checkBox = (CheckBox) findViewById(R.id.library_update_fastMode_checkBox);
		boolean savedLibraryUpdateFastMode = SPrefEx.get(getApplicationContext()).getBoolean(MusicService.TAG_SPREF_LIBRARY_UPDATE_FASTMODE, MusicService.LIBRARY_UPDATE_FASTMODE_DEFAULT);
		library_update_fastMode_checkBox.setChecked(savedLibraryUpdateFastMode);
		library_update_fastMode_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				SPrefEx.get(getApplicationContext())
						.edit()
						.putBoolean(MusicService.TAG_SPREF_LIBRARY_UPDATE_FASTMODE, compoundButton.isChecked())
						.apply();

				info("Updated!");
			}
		});

		// Check for behavior
		scan_locations_recyclerView.postDelayed(new Runnable() {
			@Override
			public void run() {
				boolean behaviourForAddScanLocationOnEmptyLibrary = false;
				try {
					behaviourForAddScanLocationOnEmptyLibrary = getIntent().getBooleanExtra(TAG_BehaviourForAddScanLocationOnEmptyLibrary, false);

					if (behaviourForAddScanLocationOnEmptyLibrary) {
						viewPager.setCurrentItem(3, true);
						findViewById(R.id.scan_locations_imageButton).performClick();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 777);
	}

	public class ScanLocationsRecyclerViewAdapter extends RecyclerView.Adapter<ScanLocationsRecyclerViewAdapter.ViewHolder> {

		private final ArrayList<String> data;

		public ScanLocationsRecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.settings_scan_locations_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final String d = data.get(position);
			final View v = holder.view;

			TextView text = (TextView) v.findViewById(R.id.text);
			text.setText(d);

			ImageButton remove = (ImageButton) v.findViewById(R.id.remove);
			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					removeData(d);
				}
			});

		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ViewHolder(View view) {
				super(view);

				this.view = view;
			}

		}

		public void setData(Collection<String> d) {
			data.clear();
			data.addAll(d);
			notifyDataSetChanged();
		}

		public void addData(String d) {
			data.add(d);

			Set<String> locations = new HashSet<>();
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				locations = new ArraySet<>();
			}
			locations.addAll(data);

			MusicServiceLibraryUpdaterAsyncTask.setScanLocations(SettingsActivity.this, locations);

			notifyDataSetChanged();

			try {
				Intent musicServiceIntent = new Intent(SettingsActivity.this, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
				musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
				startService(musicServiceIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void removeData(String d) {
			data.remove(d);

			Set<String> locations = new HashSet<>();
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
				locations = new ArraySet<String>();
			}
			locations.addAll(data);

			MusicServiceLibraryUpdaterAsyncTask.setScanLocations(SettingsActivity.this, locations);

			notifyDataSetChanged();

			try {
				Intent musicServiceIntent = new Intent(SettingsActivity.this, MusicService.class);
				musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
				musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
				startService(musicServiceIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	//endregion

	//region Analytics: LFM section

	public static final String ShowLFMSectionOnStart = TAG + ".ShowLFMSectionOnStart";

	private ImageView analytics_lfm_status;
	private EditText analytics_lfm_username_editText;
	private EditText analytics_lfm_password_editText;
	private Button analytics_lfm_save;
	private TextView analytics_lfm_logs;
	private TextInputLayout analytics_lfm_username;
	private TextInputLayout analytics_lfm_password;

	private void createLFM() {
		analytics_lfm_username = findViewById(R.id.analytics_lfm_username);
		analytics_lfm_password = findViewById(R.id.analytics_lfm_password);
		analytics_lfm_save = findViewById(R.id.analytics_lfm_save);

		analytics_lfm_status = findViewById(R.id.analytics_lfm_status);
		analytics_lfm_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Session session = Analytics.getInstance().getLastfmSession();
				if (session != null) {
					analytics_lfm_username_editText.setText("");
					analytics_lfm_password_editText.setText("");
					Analytics.getInstance().setLastfmCredentials("", "");
				}
				updateLFM();
			}
		});

		analytics_lfm_username_editText = findViewById(R.id.analytics_lfm_username_editText);
		analytics_lfm_username_editText.setText(Analytics.getInstance().getLastfmUsername());

		analytics_lfm_password_editText = findViewById(R.id.analytics_lfm_password_editText);
		analytics_lfm_password_editText.setText(Analytics.getInstance().getLastfmPassword());

		analytics_lfm_save = findViewById(R.id.analytics_lfm_save);
		analytics_lfm_save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String username = analytics_lfm_username_editText.getText().toString();
				String password = analytics_lfm_password_editText.getText().toString();

				Analytics.getInstance().setLastfmCredentials(username, password);

				updateLFM();
			}
		});

		analytics_lfm_logs = findViewById(R.id.analytics_lfm_logs);
		StringBuilder sb = new StringBuilder();
		for (ScrobbleResult sr : Analytics.getInstance().getScrobblerResultsForLastfm()) {
			sb.append(sr.toString()).append(System.lineSeparator());
		}
		analytics_lfm_logs.setText(sb.toString());

		updateLFMState();
		Session session = Analytics.getInstance().getLastfmSession();
		if (session == null && Analytics.getInstance().isLastfmScrobbledEnabled()) {
			updateLFM();
		}

		// Check for start flags
		analytics_lfm_status.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					if (getIntent().getBooleanExtra(ShowLFMSectionOnStart, false)) {
						viewPager.setCurrentItem(1, true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 777);

	}

	private void updateLFM() {

		(new RefreshLFM(this)).execute();

	}

	private void updateLFMState() {
		Session session = Analytics.getInstance().getLastfmSession();
		if (session == null) {
			analytics_lfm_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
			analytics_lfm_status.setImageResource(R.drawable.ic_exclamation);
		} else {
			analytics_lfm_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
			analytics_lfm_status.setImageResource(R.drawable.ic_cloud_refresh);
		}
		if (session == null) {
			analytics_lfm_username.setVisibility(View.VISIBLE);
			analytics_lfm_password.setVisibility(View.VISIBLE);
			analytics_lfm_save.setVisibility(View.VISIBLE);
		} else {
			analytics_lfm_username.setVisibility(View.GONE);
			analytics_lfm_password.setVisibility(View.GONE);
			analytics_lfm_save.setVisibility(View.GONE);
		}
	}

	private static class RefreshLFM extends AsyncTask<Void, Void, Void> {
		private WeakReference<SettingsActivity> contextRef;

		public RefreshLFM(SettingsActivity context) {
			this.contextRef = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			SettingsActivity context = contextRef.get();
			if (context == null)
				return null;

			Analytics.getInstance().initLastfm(context);

			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			SettingsActivity context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToShow();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			SettingsActivity context = contextRef.get();
			if (context == null)
				return;

			context.loading.smoothToHide();

			Session session = Analytics.getInstance().getLastfmSession();
			if (session == null) {
				Toast.makeText(context, "Failed to connect to last.fm!", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "Scrobbler is active now!", Toast.LENGTH_LONG).show();
			}

			context.updateLFMState();
		}
	}

	//endregion

	//region Analytics: DC

	private ImageView analytics_dc_status;

	private void createDC() {
		analytics_dc_status = findViewById(R.id.analytics_dc_status);

		analytics_dc_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (Analytics.getInstance().getDCEnabled()) {
					Analytics.getInstance().setDCEnabled(false);
				} else {
					Analytics.getInstance().setDCEnabled(true);
				}

				updateDCState();
			}
		});

		updateDCState();
	}

	private void updateDCState() {
		if (Analytics.getInstance().getDCEnabled()) {
			analytics_dc_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
			analytics_dc_status.setImageResource(R.drawable.ic_cloud_refresh);
		} else {
			analytics_dc_status.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
			analytics_dc_status.setImageResource(R.drawable.ic_exclamation);
		}
	}

	//endregion

	//region Theme

	public enum Theme {
		Default("Default"),
		Light("Light"),
		Dark("Dark"),;

		private String friendlyName;

		Theme(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_THEME = "theme";

	public static Theme getTheme(Context context) {
		try {
			return Theme.valueOf(SPrefEx.get(context).getString(TAG_SPREF_THEME, String.valueOf(Theme.Default)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Theme.Default;
	}

	public static void setTheme(Context context, Theme value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_THEME, String.valueOf(value))
				.apply();
	}

	public static int getThemeRes(Context context) {
		switch (getTheme(context)) {
			case Light:
				return R.style.AppTheme_Light;
			case Dark:
				return R.style.AppTheme_Dark;
			case Default:
			default:
				return R.style.AppTheme;
		}
	}

	//endregion

}
