package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.CacheEx;

import jp.wasabeef.blurry.Blurry;

public class PlaybackUIMiniFragment extends Fragment {

    // Logger TAG
    private static final String TAG = PlaybackUIMiniFragment.class.getSimpleName();

    protected Handler handler = new Handler();

    private MusicService musicService;

    private View root;

    private ImageView cover;

    private TextView title;
    private TextView info;

    private ImageView play_pause_stop;

    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.playback_ui_mini, container, false);

        root = v;

        cover = (ImageView) v.findViewById(R.id.cover);

        title = (TextView) v.findViewById(R.id.title);
        info = (TextView) v.findViewById(R.id.info);

        play_pause_stop = (ImageView) v.findViewById(R.id.play_pause_stop);

        play_pause_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicService == null) return;

                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.play();
                }
            }
        });
        play_pause_stop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (musicService == null) return false;

                musicService.stop();

                return true;
            }
        });

        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), PlaybackUIActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(getActivity(),
                                cover,
                                ViewCompat.getTransitionName(cover));
                startActivity(intent, options.toBundle());
            }
        });

        return v;
    }

    public void reset(MusicService ms) {
        if (ms == null || ms.getCurrentPlaylistItemMusic() == null)
            return;

        if (!isAdded())
            return;

        musicService = ms;

        Music m = ms.getCurrentPlaylistItemMusic();

        Bitmap bitmap = m.getCover(getActivity());
        if (bitmap == null)
            bitmap = CacheEx.getInstance().getBitmap(String.valueOf(R.drawable.logo));
        if (bitmap != null)
            Blurry.with(getActivity())
                    .radius(8)
                    .sampling(1)
                    .color(Color.argb(72, 0, 0, 0))
                    .async()
                    .animate(350)
                    .from(bitmap)
                    .into(cover);
        else
            cover.setImageResource(R.drawable.logo);

        title.setText(m.Title);
        info.setText(m.getTextExtraOnlySingleLine());

        progressBar.setMax(ms.getDuration());

        setupProgressHandler();

        if (musicService.isPlaying())
            onMusicServicePlay();
    }

    public void onMusicServicePlay() {
        if (musicService == null)
            return;

        if (!isAdded())
            return;

        play_pause_stop.setImageResource(R.drawable.ic_pause_black);

    }

    public void onMusicServicePause() {
        if (musicService == null)
            return;

        if (!isAdded())
            return;

        play_pause_stop.setImageResource(R.drawable.ic_play_black);
    }

    public void onMusicServiceStop() {
        if (musicService == null)
            return;

        if (!isAdded())
            return;

        play_pause_stop.setImageResource(R.drawable.ic_play_black);
    }


    private Runnable progressHandlerRunnable;

    private void setupProgressHandler() {
        if (progressHandlerRunnable != null)
            handler.removeCallbacks(progressHandlerRunnable);

        final int dt = (int) (1000.0 / 16.0);

        progressHandlerRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPlaying()) {
                    progressBar.setProgress(musicService.getPosition());
                }

                handler.removeCallbacks(progressHandlerRunnable);
                handler.postDelayed(progressHandlerRunnable, dt);
            }
        };
        handler.postDelayed(progressHandlerRunnable, dt);
    }

    public static PlaybackUIMiniFragment create() {
        PlaybackUIMiniFragment f = new PlaybackUIMiniFragment();
        return f;
    }

}
