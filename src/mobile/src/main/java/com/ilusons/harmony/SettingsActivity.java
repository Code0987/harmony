package com.ilusons.harmony;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.ArraySet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.codetroopers.betterpickers.OnDialogDismissListener;
import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.inappbilling.IabBroadcastReceiver;
import com.ilusons.harmony.ref.inappbilling.IabHelper;
import com.ilusons.harmony.ref.inappbilling.IabResult;
import com.ilusons.harmony.ref.inappbilling.Inventory;
import com.ilusons.harmony.ref.inappbilling.Purchase;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ilusons.harmony.base.MusicService.SKU_PREMIUM;

public class SettingsActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String TAG_SPREF_UISTYLE = SPrefEx.TAG_SPREF + ".uistyle";

    public static final String TAG_SPREF_UIPLAYBACKAUTOOPEN = SPrefEx.TAG_SPREF + ".ui_playback_auto_open";
    public static final boolean UIPLAYBACKAUTOOPEN_DEFAULT = true;

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

            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);

            premiumChanged((premiumPurchase != null && MusicService.verifyDeveloperPayload(SettingsActivity.this, premiumPurchase)));

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
                try (InputStream is = getResources().openRawResource(R.raw.notes)) {
                    content = IOUtils.toString(is, "UTF-8");
                    int start = content.indexOf("### Premium") + 4;
                    int end = content.indexOf("###", start);
                    content = content.substring(start, end);
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

        // Set about section
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

                            }
                        }))
                        .show();
            }
        });

        findViewById(R.id.about_notes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFinishing())
                    return;

                showReleaseNotesDialog(SettingsActivity.this);
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

        // UIStyle
        RadioGroup ui_style_radioGroup = (RadioGroup) findViewById(R.id.ui_style_radioGroup);

        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b)
                    return;

                String uiStyle = String.valueOf((UIStyle) compoundButton.getTag());

                SPrefEx.get(getApplicationContext())
                        .edit()
                        .putString(TAG_SPREF_UISTYLE, uiStyle)
                        .apply();

                info("UI Style will be completely applied on restart!");
            }
        };

        UIStyle savedUIStyle = getUIStyle(getApplicationContext());

        for (UIStyle value : UIStyle.values()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(value.friendlyName);
            rb.setTag(value);
            rb.setId(value.ordinal());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                rb.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
            }
            ui_style_radioGroup.addView(rb);

            if (savedUIStyle == value)
                rb.setChecked(true);

            rb.setOnCheckedChangeListener(onCheckedChangeListener);
        }

        // Playback UI auto open
        CheckBox ui_playback_auto_open_checkBox = (CheckBox) findViewById(R.id.ui_playback_auto_open_checkBox);

        ui_playback_auto_open_checkBox.setChecked(getUIPlaybackAutoOpen(this));

        ui_playback_auto_open_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPrefEx.get(getApplicationContext())
                        .edit()
                        .putBoolean(TAG_SPREF_UIPLAYBACKAUTOOPEN, compoundButton.isChecked())
                        .apply();

                info("Updated!");
            }
        });

        // Scan interval
        final EditText scan_interval_editText = (EditText) findViewById(R.id.scan_interval_editText);

        scan_interval_editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    EditText editText = (EditText) v;

                    Long value = Long.parseLong(editText.getText().toString().trim());

                    if (!(value <= 7 * 24 && value >= 3)) {
                        info("Enter value between [3, " + 7 * 24 + "]", true);

                        long savedScanInterval = SPrefEx.get(getApplicationContext()).getLong(MusicServiceLibraryUpdaterAsyncTask.TAG_SPREF_SCAN_INTERVAL, MusicServiceLibraryUpdaterAsyncTask.SCAN_INTERVAL_DEFAULT);

                        editText.setText("");
                        editText.append(String.valueOf(savedScanInterval / MusicServiceLibraryUpdaterAsyncTask.SCAN_INTERVAL_FACTOR));

                        return;
                    }

                    value *= MusicServiceLibraryUpdaterAsyncTask.SCAN_INTERVAL_FACTOR;

                    SPrefEx.get(getApplicationContext())
                            .edit()
                            .putLong(MusicServiceLibraryUpdaterAsyncTask.TAG_SPREF_SCAN_INTERVAL, value)
                            .apply();

                    info("Updated!");
                }
            }
        });

        long savedScanInterval = SPrefEx.get(getApplicationContext()).getLong(MusicServiceLibraryUpdaterAsyncTask.TAG_SPREF_SCAN_INTERVAL, MusicServiceLibraryUpdaterAsyncTask.SCAN_INTERVAL_DEFAULT);

        scan_interval_editText.setText("");
        scan_interval_editText.append(String.valueOf(savedScanInterval / MusicServiceLibraryUpdaterAsyncTask.SCAN_INTERVAL_FACTOR));
        scan_interval_editText.clearFocus();

        findViewById(R.id.scan_interval_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan_interval_editText.clearFocus();
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

        findViewById(R.id.reset_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                (new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle))
                        .setTitle("Sure?")
                        .setMessage("App will become like new, all your personalized content will be lost!")
                        .setCancelable(false)
                        .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (iabBroadcastReceiver != null) {
            unregisterReceiver(iabBroadcastReceiver);
        }

        if (iabHelper != null) {
            iabHelper.disposeWhenFinished();
            iabHelper = null;
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
        try (InputStream is = context.getResources().openRawResource(R.raw.gps_listing)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                content = Html.fromHtml(IOUtils.toString(is, "UTF-8").replace("\n", "<br>")).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();

            content = "Error loading data!";
        }

        (new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme_AlertDialogStyle))
                .setTitle("Notes")
                .setMessage(content)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

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

    public enum UIStyle {
        DarkUI("Dark UI"),
        LiteUI("Lite UI");

        private String friendlyName;

        UIStyle(String friendlyName) {
            this.friendlyName = friendlyName;
        }
    }

    public static UIStyle getUIStyle(Context context) {
        return UIStyle.valueOf(SPrefEx.get(context).getString(TAG_SPREF_UISTYLE, String.valueOf(UIStyle.DarkUI)));
    }

    public static boolean getUIPlaybackAutoOpen(Context context) {
        return SPrefEx.get(context).getBoolean(TAG_SPREF_UIPLAYBACKAUTOOPEN, UIPLAYBACKAUTOOPEN_DEFAULT);
    }

}
