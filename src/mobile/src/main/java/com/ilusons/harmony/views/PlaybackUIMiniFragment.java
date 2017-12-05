package com.ilusons.harmony.views;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.AndroidTouchEx;
import com.ilusons.harmony.ref.CacheEx;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import jp.wasabeef.blurry.Blurry;

public class PlaybackUIMiniFragment extends Fragment {

	// Logger TAG
	private static final String TAG = PlaybackUIMiniFragment.class.getSimpleName();

	protected Handler handler = new Handler();

	private MusicService musicService;

	private View root;

	private TextView title;
	private TextView info;

	private ImageView play_pause_stop;
	private ImageView next_random;
	private ImageView jump;

	private WeakReference<View.OnClickListener> jumpOnClickListenerReference;

	private ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.playback_ui_mini, container, false);

		root = v;

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

		next_random = (ImageView) v.findViewById(R.id.next_random);

		next_random.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (musicService == null) return;

				musicService.next();
			}
		});
		next_random.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (musicService == null) return false;

				musicService.random();

				return true;
			}
		});

		jump = (ImageView) v.findViewById(R.id.jump);
		if (jumpOnClickListenerReference != null && jumpOnClickListenerReference.get() != null)
			jump.setOnClickListener(jumpOnClickListenerReference.get());

		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);

		final View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getActivity(), PlaybackUIActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(
						intent,
						ActivityOptionsCompat.makeCustomAnimation(view.getContext(), R.anim.scale_up, R.anim.shake).toBundle());
			}
		};

		title.setOnClickListener(onClickListener);
		info.setOnClickListener(onClickListener);
		jump.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				onClickListener.onClick(view);
				return true;
			}
		});
		root.setOnTouchListener(new AndroidTouchEx.OnSwipeTouchListener() {
			@Override
			public boolean onSwipeLeft() {
				if (musicService != null) {
					musicService.prev();

					root.startAnimation(AnimationUtils.loadAnimation(root.getContext(), R.anim.shake));

					AndroidEx.vibrate(root.getContext());
				}
				return true;
			}

			@Override
			public boolean onSwipeRight() {
				if (musicService != null) {
					musicService.next();

					root.startAnimation(AnimationUtils.loadAnimation(root.getContext(), R.anim.shake));

					AndroidEx.vibrate(root.getContext());
				}
				return true;
			}

			@Override
			public boolean onSwipeTop() {
				onClickListener.onClick(root);

				AndroidEx.vibrate(root.getContext());

				return true;
			}

			@Override
			public boolean onSwipeBottom() {
				try {
					getActivity().onBackPressed();
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}

				AndroidEx.vibrate(root.getContext());

				return false;
			}
		});

		return v;
	}

	@Override
	public void onDestroyView() {
		if (progressHandlerRunnable != null)
			handler.removeCallbacks(progressHandlerRunnable);
		handler.removeMessages(0);

		super.onDestroyView();
	}

	public void reset(MusicService ms) {
		if (ms == null || ms.getMusic() == null)
			return;

		if (!isAdded())
			return;

		musicService = ms;

		Music m = ms.getMusic();

		title.setText(m.getTitle());
		String s;
		try {
			s = m.getTextExtraOnlySingleLine(ms.getPlaylist().getItemIndex());
		} catch (Exception e) {
			e.printStackTrace();

			s = m.getTextExtraOnlySingleLine();
		}
		info.setText(s);

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

		play_pause_stop.setImageResource(R.drawable.ic_stop_black);
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

	public void setJumpOnClickListener(WeakReference<View.OnClickListener> onClickListenerReference) {
		if (jump == null)
			jumpOnClickListenerReference = onClickListenerReference;
		else {
			jump.setOnClickListener(onClickListenerReference.get());
			jumpOnClickListenerReference = null;
		}
	}

	public static PlaybackUIMiniFragment create() {
		PlaybackUIMiniFragment f = new PlaybackUIMiniFragment();
		return f;
	}

}
