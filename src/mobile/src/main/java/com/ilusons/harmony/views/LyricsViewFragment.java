package com.ilusons.harmony.views;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ilusons.harmony.R;

public class LyricsViewFragment extends Fragment {

    public static String KEY_CONTENT = "content";

    private String content;

    private ScrollView scrollView;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_CONTENT, content);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            content = (String) savedInstanceState.get(KEY_CONTENT);
        } else {
            content = (String) getArguments().get(KEY_CONTENT);
        }

        if (TextUtils.isEmpty(content))
            throw new IllegalArgumentException();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lyrics_view, container, false);

        ((TextView) v.findViewById(R.id.lyrics)).setText(content);

        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        return v;
    }

    public void setProgress(double v) {
        scrollView.smoothScrollTo(0, (int) (scrollView.getChildAt(0).getHeight() * v));
    }

    public static LyricsViewFragment create(String content) {
        LyricsViewFragment f = new LyricsViewFragment();
        Bundle b = new Bundle();
        b.putString(KEY_CONTENT, content);
        f.setArguments(b);
        return f;
    }

}
