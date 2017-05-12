package com.ilusons.harmony.views;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.JavaEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = LyricsViewFragment.class.getSimpleName();

    public static String KEY_PATH = "path";
    private String path = null;
    private Music music = null;

    private boolean isContentProcessed = false;

    private AVLoadingIndicatorView loading_view;

    private TextView textView;
    private ScrollView scrollView;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_PATH, path);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            path = (String) savedInstanceState.get(KEY_PATH);
        } else {
            path = (String) getArguments().get(KEY_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lyrics_view, container, false);

        loading_view = (AVLoadingIndicatorView) v.findViewById(R.id.loading_view);

        textView = (TextView) v.findViewById(R.id.lyrics);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        reset(path);

        return v;
    }

    public void reset(String path) {
        this.path = path;
        this.music = Music.load(getContext(), path);

        processContent();
    }

    public void reset(Music music) {
        this.path = music.Path;
        this.music = music;

        lastP = 0;
        lastV = 0;
        lastTS = 0;
        lastIndex = -1;

        processContent();
    }

    private String contentFormatted = null;
    private TreeMap<Long, String> contentProcessed = new TreeMap<>();

    private static Pattern ts = Pattern.compile("(\\d+):(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE);
    private static Pattern lf = Pattern.compile("((\\[(.*)\\])+)(.*)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private void processContent() {
        // If no data loaded return
        if (music == null)
            return;

        loading_view.show();

        textView.setText(music.getTextDetailed());

        // Load lyrics
        String content = music.getLyrics(getContext());
        // Check if need to download or not
        if (content == null) {
            // If download required, postpone function to later
            Music.getLyricsOrDownload(getContext(), music, new JavaEx.ActionT<String>() {
                @Override
                public void execute(String s) {
                    processContent();
                }
            });
            return;
        }

        // Format content
        String nl = System.getProperty("line.separator");

        contentProcessed.clear();

        Matcher m = lf.matcher(content);
        while (m.find()) {
            String c = m.group(4).trim();

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

            contentFormatted = music.getTextDetailed() + nl + nl + sb.toString();
        } else {
            contentFormatted = music.getTextDetailed() + nl + nl + content;
        }

        textView.setText(contentFormatted);

        loading_view.hide();

        isContentProcessed = true;
    }

    private int lastP = 0;
    private float lastV = 0;
    private long lastTS = 0;
    private int lastIndex = -1;

    public void updateScroll(float v, int p) {
        if (!isContentProcessed)
            return;

        if (textView == null || textView.getLayout() == null)
            return;

        // Reset if seek-ed back
        if (lastP > p) {
            lastV = 0;
            lastTS = 0;
            lastIndex = -1;
        }
        lastP = p;

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

        float dv = Math.abs(v - lastV);
        if (dv > 1.07 /* TODO: Magic! Make it better and real */) {
            scrollView.smoothScrollBy(0, Math.round(dv));
            lastV = v;
        } else {
            lastV += v;
        }

    }

    public static LyricsViewFragment create(String path) {
        LyricsViewFragment f = new LyricsViewFragment();
        Bundle b = new Bundle();
        b.putString(KEY_PATH, path);
        f.setArguments(b);
        return f;
    }

}
