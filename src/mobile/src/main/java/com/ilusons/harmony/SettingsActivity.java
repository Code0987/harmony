package com.ilusons.harmony;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.IOEx;
import com.ilusons.harmony.ref.SPrefEx;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class SettingsActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String TAG_SPREF_UISTYLE = SPrefEx.TAG_SPREF + ".uistyle";

    public static final String TAG_SPREF_UIPLAYBACKAUTOOPEN = SPrefEx.TAG_SPREF + ".ui_playback_auto_open";
    public static final boolean UIPLAYBACKAUTOOPEN_DEFAULT = true;

    // UI
    private View root;

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

        // Set close
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
                try (InputStream is = getResources().openRawResource(R.raw.licenses)) {
                    content = IOUtils.toString(is, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();

                    content = "Error loading data!";
                }

                (new AlertDialog.Builder(SettingsActivity.this, R.style.AppTheme_AlertDialogStyle)
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
            rb.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
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
                if (!b)
                    return;

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
                        info("Enter value between [3, " + 7 * 24 + "]");

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
                if (!b)
                    return;

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
        });
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
