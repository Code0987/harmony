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

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = LyricsViewFragment.class.getSimpleName();

    public static String KEY_CONTENT = "content";

    private String content = null;
    private boolean isContentProcessed = false;

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

    private static Pattern ts = Pattern.compile("(\\d+):(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE);
    private static Pattern lf = Pattern.compile("((\\[(.*)\\])+)(.*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private void processContent() {
        if (TextUtils.isEmpty(content)) {
            textView.setText("Embedded lyrics not found!");

            return;
        }

        contentProcessed.clear();

        Matcher m = lf.matcher(content);
        while (m.find()) {
            String c = m.group(4);

            if (c.trim().isEmpty()) // Ignore blank lines
                continue;

            Matcher mts = ts.matcher(m.group(3));
            while (mts.find()) { // Decode multiple time lines

                long ts = Long.parseLong(mts.group(1)) * 60000
                        + Long.parseLong(mts.group(2)) * 1000
                        + Long.parseLong(mts.group(3));

                contentProcessed.put(ts, c);
            }
        }

        if (contentProcessed.size() > 0) { // Re-build user friendly lyrics
            StringBuilder sb = new StringBuilder();

            for (TreeMap.Entry entry : contentProcessed.entrySet()) {
                sb.append(entry.getValue());
                sb.append(System.lineSeparator());
            }

            contentFormatted = sb.toString();
        } else {
            contentFormatted = content;
        }

        textView.setText(contentFormatted);

        isContentProcessed = true;
    }

    private double lastV = -0;
    private long lastTS = -1L;
    private int lastIndex = -1;

    public void updateScroll(double v, int p) {
        if (!isContentProcessed)
            return;

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
                int index = contentFormatted.indexOf(c, lastIndex);
                lastIndex = index;

                int line = textView.getLayout().getLineForOffset(index);
                int y = (int) ((line + 0.5) * textView.getLineHeight()) + textView.getTop();

                scrollView.smoothScrollTo(0, y - scrollView.getHeight() / 2);

                lastTS = ts;
            }

        }

        // For un-synced (no else to show little scroll always)

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
