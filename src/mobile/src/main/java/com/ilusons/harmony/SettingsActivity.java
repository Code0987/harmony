package com.ilusons.harmony;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.HeadsetMediaButtonIntentReceiver;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.StorageEx;
import com.ilusons.harmony.ref.ViewEx;
import com.ilusons.harmony.ref.inappbilling.IabBroadcastReceiver;
import com.ilusons.harmony.ref.inappbilling.IabHelper;
import com.ilusons.harmony.ref.inappbilling.IabResult;
import com.ilusons.harmony.ref.inappbilling.Inventory;
import com.ilusons.harmony.ref.inappbilling.Purchase;
import com.ilusons.harmony.views.AudioVFXViewFragment;
import com.ilusons.harmony.views.LibraryUIActivity;
import com.ilusons.harmony.views.PlaybackUIActivity;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ilusons.harmony.base.MusicService.SKU_PREMIUM;

public class SettingsActivity extends BaseActivity {

	// Logger TAG
	private static final String TAG = SettingsActivity.class.getSimpleName();

	public static final String TAG_SPREF_UISTYLE = SPrefEx.TAG_SPREF + ".uistyle";

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

	private ScanLocationsRecyclerViewAdapter scanLocationsRecyclerViewAdapter;

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
		loading = (AVLoadingIndicatorView) findViewById(R.id.loading);

		// IAB
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

		// Set close
		findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});

		// Set premium
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

		// Set views and tabs
		final ViewEx.StaticViewPager viewPager = (ViewEx.StaticViewPager) findViewById(R.id.viewPager);
		TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);

		// About section
		onCreateBindAboutSection();

		// Presets section
		onCreateBindPresetsSection();

		// UI section
		onCreateBindUISection();

		// AVFXType
		avfxtype_spinner = (Spinner) findViewById(R.id.avfxtype_spinner);

		createAVFXType();

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

			List<Uri> files = Utils.getSelectedFilesFromResult(data);
			for (Uri uri : files) {
				File file = Utils.getFileForUri(uri);

				scanLocationsRecyclerViewAdapter.addData(file.getAbsolutePath());
			}

		} else if (requestCode == REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
					FileInputStream fileInputStream = new FileInputStream(pfd.getFileDescriptor());

					if (importCurrentPreset(fileInputStream, uri))
						info("Preset successfully imported to current profile. Please restart to apply.");
					else
						info("Some problem while importing current preset.");

					fileInputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		} else if (requestCode == REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
					FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

					if (exportCurrentPreset(fileOutputStream, uri))
						info("Current preset successfully exported.");
					else
						info("Some problem while exporting selected preset.");

					fileOutputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

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
		}

	}

	//region About section

	private void onCreateBindAboutSection() {

		((TextView) findViewById(R.id.about_version)).setText(BuildConfig.VERSION_NAME);

		findViewById(R.id.about_license).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isFinishing())
					return;

				String content;
				try (InputStream is = getResources().openRawResource(R.raw.license)) {
					content = IOUtils.toString(is, "UTF-8");
				} catch (Exception e) {
					e.printStackTrace();

					content = "Error loading data!";
				}

				(new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle))
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

		findViewById(R.id.about_info).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isFinishing())
					return;

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

				(new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle))
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

		findViewById(R.id.about_release_notes).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isFinishing())
					return;

				showReleaseNotesDialog(SettingsActivity.this);
			}
		});

	}

	//endregion

	//region Presets section

	private static final int REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF = 58;
	private static final int REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF = 59;

	private void onCreateBindPresetsSection() {

		Button settings_presets_import = findViewById(R.id.settings_presets_import);
		settings_presets_import.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				String[] mimes = new String[]{"text/plain"};
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
				intent.putExtra(Intent.EXTRA_TITLE, "harmony.txt");
				intent.setType(StringUtils.join(mimes, '|'));
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF);
			}
		});

		RecyclerView settings_presets_import_recyclerView = (RecyclerView) findViewById(R.id.settings_presets_import_recyclerView);
		settings_presets_import_recyclerView.setHasFixedSize(true);
		settings_presets_import_recyclerView.setItemViewCacheSize(3);
		settings_presets_import_recyclerView.setDrawingCacheEnabled(true);
		settings_presets_import_recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

		RawPresetsAdapter adapter = new RawPresetsAdapter();
		settings_presets_import_recyclerView.setAdapter(adapter);

		adapter.refresh();

		Button settings_presets_export = findViewById(R.id.settings_presets_export);
		settings_presets_export.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				String[] mimes = new String[]{"text/plain"};
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
				intent.putExtra(Intent.EXTRA_TITLE, "harmony.txt");
				intent.setType(StringUtils.join(mimes, '|'));
				if (intent.resolveActivity(getPackageManager()) != null) {
					startActivityForResult(intent, REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF);
				} else {
					info("SAF not found!");
				}
			}
		});

	}

	private boolean exportCurrentPreset(OutputStream os, Uri uri) {
		boolean r = false;

		try {
			SharedPreferences pref = SPrefEx.get(this);

			ArrayList<String> keys = new ArrayList<>();

			keys.addAll(Arrays.asList(MusicService.ExportableSPrefKeys));
			keys.addAll(Arrays.asList(ExportableSPrefKeys));
			keys.addAll(Arrays.asList(AudioVFXViewFragment.ExportableSPrefKeys));
			keys.addAll(Arrays.asList(LibraryUIActivity.ExportableSPrefKeys));
			keys.addAll(Arrays.asList(PlaybackUIActivity.ExportableSPrefKeys));

			Map<String, ?> map = pref.getAll();
			map.keySet().retainAll(keys);

			String data = (new Gson()).toJson(map, new TypeToken<Map<String, ?>>() {
			}.getType());

			Log.d(TAG, "Export\n" + data);

			IOUtils.write(data, os, StandardCharsets.UTF_8);

			r = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return r;
	}

	@SuppressWarnings({"unchecked"})
	private boolean importCurrentPreset(InputStream is, Uri uri) {
		boolean r = false;

		try {
			String data = IOUtils.toString(is, StandardCharsets.UTF_8);

			SharedPreferences.Editor prefEdit = SPrefEx.get(this).edit();

			Map<String, ?> entries = (new Gson()).fromJson(data, new TypeToken<Map<String, ?>>() {
			}.getType());
			for (Map.Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, (Boolean) v);
				else if (v instanceof Float)
					prefEdit.putFloat(key, (Float) v);
				else if (v instanceof Integer)
					prefEdit.putInt(key, (Integer) v);
				else if (v instanceof Long)
					prefEdit.putLong(key, (Long) v);
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}

			prefEdit.apply();

			r = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return r;
	}

	public class RawPresetsAdapter extends RecyclerView.Adapter<RawPresetsAdapter.ViewHolder> {

		private final ArrayList<Pair<Integer, String>> data;

		public RawPresetsAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.settings_presets_import_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final Pair<Integer, String> d = data.get(position);
			final View v = holder.view;

			TextView text = (TextView) v.findViewById(R.id.text);
			text.setText(d.second);

			View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						InputStream is = getResources().openRawResource(d.first);

						if (importCurrentPreset(is, null))
							info("Preset successfully imported to current profile. Please restart to apply.");
						else
							info("Some problem while importing preset.");

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			v.setOnClickListener(listener);
			text.setOnClickListener(listener);

		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ViewHolder(View view) {
				super(view);

				this.view = view;
			}

		}

		public void refresh() {
			data.clear();

			Field[] fields = R.raw.class.getFields();
			for (int count = 0; count < fields.length; count++)
				try {
					Integer id = (Integer) fields[count].get(null);
					String name = fields[count].getName();

					if (name.startsWith("preset_"))
						data.add(Pair.create(id, name));

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

			notifyDataSetChanged();
		}

	}

	//endregion

	//region UI section

	private void onCreateBindUISection() {

		createUIStyle();
		createPlaybackUIStyle();

	}

	//endregion

	//region Library section

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

		// Set scan constraint min duration
		final EditText scan_constraint_min_duration_editView = (EditText) findViewById(R.id.scan_constraint_min_duration_editView);
		scan_constraint_min_duration_editView.setText("");
		scan_constraint_min_duration_editView.append(MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMinDuration(SettingsActivity.this).toString());
		scan_constraint_min_duration_editView.clearFocus();
		findViewById(R.id.scan_constraint_min_duration_imageButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final HmsPickerDialogFragment.HmsPickerDialogHandlerV2 handler = new HmsPickerDialogFragment.HmsPickerDialogHandlerV2() {
					@Override
					public void onDialogHmsSet(int reference, boolean isNegative, int hours, int minutes, int seconds) {
						MusicServiceLibraryUpdaterAsyncTask.setScanConstraintMinDuration(SettingsActivity.this, ((((hours * 60L) + minutes) * 60) + seconds) * 1000);

						scan_constraint_min_duration_editView.setText("");
						scan_constraint_min_duration_editView.append(MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMinDuration(SettingsActivity.this).toString());
						scan_constraint_min_duration_editView.clearFocus();
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
				hpb.setTimeInMilliseconds(MusicServiceLibraryUpdaterAsyncTask.getScanConstraintMinDuration(SettingsActivity.this));
				hpb.show();
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

	}

	//endregion

	//region UI style
	public enum UIStyle {
		Default("Default"),
		LUI5("Simple UI"),
		LUI2("UI Style 2"),
		LUI11("UI Style 11"),
		LUI12("UI Style 12");

		private String friendlyName;

		UIStyle(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static UIStyle getUIStyle(Context context) {
		try {
			return UIStyle.valueOf(SPrefEx.get(context).getString(TAG_SPREF_UISTYLE, String.valueOf(UIStyle.LUI12)));
		} catch (Exception e) {
			e.printStackTrace();

			return UIStyle.Default;
		}
	}

	public static void setUIStyle(Context context, UIStyle value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_UISTYLE, String.valueOf(value))
				.apply();
	}

	private Spinner uiStyle_spinner;

	private void createUIStyle() {
		uiStyle_spinner = (Spinner) findViewById(R.id.uiStyle_spinner);

		UIStyle[] items = UIStyle.values();

		uiStyle_spinner.setAdapter(new ArrayAdapter<UIStyle>(this, 0, items) {
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
		UIStyle lastMode = getUIStyle(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		uiStyle_spinner.setSelection(i, true);

		uiStyle_spinner.post(new Runnable() {
			public void run() {
				uiStyle_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setUIStyle(getApplicationContext(), (UIStyle) adapterView.getItemAtPosition(position));

						info("UI Style will be completely applied on restart!");
					}

					@Override
					public void onNothingSelected(AdapterView<?> adapterView) {
					}
				});
			}
		});
	}

	//endregion

	//region PlaybackUI style
	public enum PlaybackUIStyle {
		Default("Default"),
		PUI2("Lyrics"),
		PUI3("Art");

		private String friendlyName;

		PlaybackUIStyle(String friendlyName) {
			this.friendlyName = friendlyName;
		}
	}

	public static final String TAG_SPREF_PlaybackUIStyle = SPrefEx.TAG_SPREF + ".playback_ui_style";

	public static PlaybackUIStyle getPlaybackUIStyle(Context context) {
		return PlaybackUIStyle.valueOf(SPrefEx.get(context).getString(TAG_SPREF_PlaybackUIStyle, String.valueOf(PlaybackUIStyle.Default)));
	}

	public static void setPlaybackUIStyle(Context context, PlaybackUIStyle value) {
		SPrefEx.get(context)
				.edit()
				.putString(TAG_SPREF_PlaybackUIStyle, String.valueOf(value))
				.apply();
	}

	private Spinner playbackUIStyle_spinner;

	private void createPlaybackUIStyle() {
		playbackUIStyle_spinner = (Spinner) findViewById(R.id.playbackUIStyle_spinner);

		PlaybackUIStyle[] items = PlaybackUIStyle.values();

		playbackUIStyle_spinner.setAdapter(new ArrayAdapter<PlaybackUIStyle>(this, 0, items) {
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
		PlaybackUIStyle lastMode = getPlaybackUIStyle(this);
		for (; i < items.length; i++)
			if (items[i] == lastMode)
				break;
		playbackUIStyle_spinner.setSelection(i, true);

		playbackUIStyle_spinner.post(new Runnable() {
			public void run() {
				playbackUIStyle_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
						setPlaybackUIStyle(getApplicationContext(), (PlaybackUIStyle) adapterView.getItemAtPosition(position));

						info("Playback UI Style will be completely applied on restart!");
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

	public static String[] ExportableSPrefKeys = new String[]{
			TAG_SPREF_PlaybackUIStyle,
			TAG_SPREF_UISTYLE,
	};

}
