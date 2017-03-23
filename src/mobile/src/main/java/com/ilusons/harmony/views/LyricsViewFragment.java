package com.ilusons.harmony.views;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ilusons.harmony.R;

public class LyricsViewFragment extends Fragment {

    public static String KEY_CONTENT = "content";
    public static String KEY_LENGTH = "length";

    private String content;
    private int length;

    private ScrollView scrollView;

    private Handler handler = new Handler();

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_CONTENT, content);
        savedInstanceState.putLong(KEY_LENGTH, length);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            content = (String) savedInstanceState.get(KEY_CONTENT);
            length = savedInstanceState.getInt(KEY_LENGTH);
        } else {
            content = (String) getArguments().get(KEY_CONTENT);
            length = getArguments().getInt(KEY_LENGTH);
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollView.post(new Runnable() {
            @Override
            public void run() {

                final int dt = Math.max(
                        10,
                        (int) Math.ceil((double) length / (double) scrollView.getChildAt(0).getHeight()));

                Runnable runnable = new Runnable() {
                    public void run() {
                        scrollView.scrollBy(0, 1);

                        handler.removeCallbacks(this);
                        handler.postDelayed(this, dt);
                    }
                };
                handler.postDelayed(runnable, dt);

            }
        });
    }

    public static LyricsViewFragment create(String content, int length) {
        LyricsViewFragment f = new LyricsViewFragment();
        Bundle b = new Bundle();
        b.putString(KEY_CONTENT, content);
        b.putInt(KEY_LENGTH, length);
        f.setArguments(b);
        return f;
    }

}
