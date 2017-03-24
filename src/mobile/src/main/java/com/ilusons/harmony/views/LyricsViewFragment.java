package com.ilusons.harmony.views;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.ref.ID3TagsEx;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = LyricsViewFragment.class.getSimpleName();

    public static String KEY_CONTENT = "content";

    private String content = null;

    private TextView textView;
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

        textView = (TextView) v.findViewById(R.id.lyrics);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        processContent();

        return v;
    }

    private String contentFormatted = null;
    private TreeMap<Long, String> contentProcessed = new TreeMap<>();

    private void processContent() {
        contentFormatted = ID3TagsEx.cleanLyrics(content);

        textView.setText(contentFormatted);

        contentProcessed.clear();

        Pattern p = Pattern.compile("(\\[(.*?)\\])(.*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Pattern d = Pattern.compile("(\\d+):(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE);

        Matcher m = p.matcher(content);
        while (m.find()) {
            String ts = m.group(2);
            String c = m.group(3);

            long t = 0;

            Matcher mt = d.matcher(ts);
            if (mt.matches()) {
                t = Long.parseLong(mt.group(1)) * 60000
                        + Long.parseLong(mt.group(2)) * 1000
                        + Long.parseLong(mt.group(3));
            } else {
                continue;
            }

            contentProcessed.put(t, c);
        }
    }

    private double lastV = -0;
    private long lastTS = -1L;

    public void updateScroll(double v, int p) {

        // For synced
        if (contentProcessed.size() > 0) {

            long ts = 0L;
            String c = null;

            for (Long k : contentProcessed.keySet())
                if (k <= p) {
                    ts = k;
                    c = contentProcessed.get(k);
                } else break;

            if (ts > lastTS && c != null) {
                int line = textView.getLayout().getLineForOffset(contentFormatted.indexOf(c));
                int y = (int) ((line + 0.5) * textView.getLineHeight()) + textView.getTop();

                scrollView.smoothScrollTo(0, y - scrollView.getHeight() / 2);

                lastTS = ts;

                Log.d(TAG, c);
            }

        }

        // For un-synced

        int dy = (int) Math.round(scrollView.getChildAt(0).getHeight() * (v - lastV));
        scrollView.smoothScrollBy(0, dy);
        lastV = v;

    }

    public static LyricsViewFragment create(String content) {
        LyricsViewFragment f = new LyricsViewFragment();
        Bundle b = new Bundle();
        b.putString(KEY_CONTENT, content);
        f.setArguments(b);
        return f;
    }

}
