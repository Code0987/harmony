package com.ilusons.harmony;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.ilusons.harmony.base.BaseActivity;
import com.ilusons.harmony.base.MusicServiceLibraryUpdaterAsyncTask;
import com.ilusons.harmony.ref.SPrefEx;

public class SettingsActivity extends BaseActivity {

    // Logger TAG
    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String TAG_SPREF_UISTYLE = SPrefEx.TAG_SPREF + ".uistyle";

    // UI
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view
        setContentView(R.layout.settings_activity);

        // Set views
        root = findViewById(R.id.root);

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

        findViewById(R.id.scan_interval_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan_interval_editText.clearFocus();
            }
        });

        findViewById(R.id.bottom_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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

}
