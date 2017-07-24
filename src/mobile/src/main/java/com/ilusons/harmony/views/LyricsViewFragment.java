package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsViewFragment extends Fragment {

    // Logger TAG
    private static final String TAG = LyricsViewFragment.class.getSimpleName();

    public static String KEY_PATH = "path";
    private String path = null;
    private Music music = null;
    public static String KEY_LENGTH = "length";
    private Long length = -1L;

    private boolean isContentProcessed = false;

    private GestureDetectorCompat gestureDetector;

    private View root;

    private AVLoadingIndicatorView loading_view;

    private TextView textView;
    private ScrollView scrollView;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_PATH, path);
        savedInstanceState.putLong(KEY_LENGTH, length);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            path = (String) savedInstanceState.get(KEY_PATH);
            length = (Long) savedInstanceState.get(KEY_LENGTH);
        } else {
            path = (String) getArguments().get(KEY_PATH);
            length = (Long) getArguments().get(KEY_LENGTH);
        }

        Music.setCurrentLyricsView(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lyrics_view, container, false);

        root = v;
        gestureDetector = new GestureDetectorCompat(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_MIN_DISTANCE = 120;
            private static final int SWIPE_MAX_OFF_PATH = 320;
            private static final int SWIPE_THRESHOLD_VELOCITY = 200;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                        return false;

                    if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        reLoad();

                        return true;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "There was an error processing the Fling event:" + e);
                }

                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        loading_view = (AVLoadingIndicatorView) v.findViewById(R.id.loading_view);

        textView = (TextView) v.findViewById(R.id.lyrics);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!MusicService.IsPremium) {
                    MusicService.showPremiumFeatureMessage(getActivity().getApplicationContext());

                    return false;
                }

                if (music == null)
                    return false;

                try {
                    Context context = getActivity().getApplicationContext();

                    File file = music.getLyricsFile(getActivity().getApplicationContext());
                    Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);

                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setDataAndType(contentUri, "text/plain");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                    context.startActivity(Intent.createChooser(intent, "Edit lyrics for " + music.getText()));
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast.makeText(getActivity().getApplicationContext(), "Please install a text editor first!", Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });

        textView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return false;
                }
                return false;
            }
        });

        reset(path, length);

        return v;
    }

    public void reLoad() {
        loading_view.smoothToShow();
        try {
            Music.getLyricsOrDownload(music);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReloaded(String s) {
        loading_view.smoothToHide();

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

        if (getActivity() == null || getActivity().getApplicationContext() == null)
            return;

        loading_view.show();

        textView.setText(music.getTextDetailed());

        // Load lyrics
        String content = music.getLyrics(getActivity().getApplicationContext());
        // Check if need to download or not
        if (content == null) {
            // If download required, postpone function to later
            reLoad();
            return;
        }

        int lines = content.split(System.getProperty("line.separator")).length + 3;

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

            lines = sb.toString().split(System.getProperty("line.separator")).length + 3;
        } else {
            contentFormatted = music.getTextDetailed() + nl + nl + content;
        }

        scrollBy = ((float) textView.getLineHeight() * lines) / ((float) length / 1000);
        if (scrollBy < 1)
            scrollBy = 1;

        textView.setText(contentFormatted);

        loading_view.hide();

        isContentProcessed = true;
    }

    private float scrollBy = 1;
    private int lastPScroll;
    private int lastP;
    private long lastTS;
    private int lastIndex;

    public void updateScroll(int p) {
        if (!isContentProcessed)
            return;

        if (textView == null || textView.getLayout() == null)
            return;

        // Reset if seek-ed back
        if (lastP > p) {
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
        if (p - lastPScroll > 999) { // Scroll every 1 sec
            scrollView.smoothScrollBy(0, Math.round(scrollBy));
            lastPScroll = p;
        }

    }

    public void reset(String path, Long length) {
        this.path = path;
        this.music = Music.load(getActivity(), path);
        this.length = length;

        lastPScroll = 0;
        lastP = 0;
        lastTS = 0;
        lastIndex = -1;

        scrollView.smoothScrollTo(0, 0);

        processContent();
    }

    public void reset(Music music, Long length) {
        this.path = music.Path;
        this.music = music;
        this.length = length;

        lastPScroll = 0;
        lastP = 0;
        lastTS = 0;
        lastIndex = -1;

        scrollView.smoothScrollTo(0, 0);

        processContent();
    }

    public static LyricsViewFragment create(String path, Long length) {
        LyricsViewFragment f = new LyricsViewFragment();
        Bundle b = new Bundle();
        b.putString(KEY_PATH, path);
        b.putLong(KEY_LENGTH, length);
        f.setArguments(b);
        return f;
    }

}
