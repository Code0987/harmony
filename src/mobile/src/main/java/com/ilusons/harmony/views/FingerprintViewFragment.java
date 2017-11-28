package com.ilusons.harmony.views;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseDialogUIActivity;
import com.ilusons.harmony.base.BaseUIFragment;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Api;
import com.ilusons.harmony.data.FingerprintUpdaterAsyncTask;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AudioEx;
import com.ilusons.harmony.data.Fingerprint;
import com.ilusons.harmony.ref.JavaEx;
import com.ilusons.harmony.ref.permissions.PermissionsManager;
import com.ilusons.harmony.ref.permissions.PermissionsResultAction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FingerprintViewFragment extends BaseUIFragment {

	// Logger TAG
	private static final String TAG = FingerprintViewFragment.class.getSimpleName();

	private Context context;
	private BroadcastReceiver broadcastReceiver;

	private View root;

	private ImageView icon;
	private TextView text;
	private TextView info;
	private ImageButton local_updater_status;

	private FrameLayout avfx_layout;
	private WaveformView avfx_view;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		this.context = context;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.fingerprint_view, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		icon = v.findViewById(R.id.icon);

		text = v.findViewById(R.id.text);

		avfx_layout = v.findViewById(R.id.avfx_layout);

		icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isProcessing)
					stopProcessing();
				else
					startProcessing();
			}
		});

		info = v.findViewById(R.id.info);

		local_updater_status = v.findViewById(R.id.local_updater_status);

		local_updater_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (FingerprintUpdaterAsyncTask.getInstance() == null) {
					FingerprintUpdaterAsyncTask.run(getContext().getApplicationContext());

					info("Fingerprint updater is running!");
				} else {
					FingerprintUpdaterAsyncTask.cancel();

					info("Fingerprint updater stopped!");
				}

				local_updater_status.setVisibility(View.GONE);

				checkLocalUpdater();
			}
		});

		createLookup(v);

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (context != null) {
			destroyBroadcast(context);
		}
	}

	private boolean hasPermissions = false;

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(
				getActivity(),
				new String[]{
						android.Manifest.permission.RECORD_AUDIO
				},
				new PermissionsResultAction() {
					@Override
					public void onGranted() {
						hasPermissions = true;
					}

					@Override
					public void onDenied(String permission) {
						hasPermissions = false;
					}
				});

		/*
		if (((double) Fingerprint.getSize() / (double) Music.getSize()) <= 0.75) {
			if (FingerprintUpdaterAsyncTask.getInstance() == null) {
				FingerprintUpdaterAsyncTask.run(getContext().getApplicationContext());

				info("Fingerprint updater started.");
			}
		}
		*/

		checkLocalUpdater();
	}

	//region Broadcast

	private void createBroadcast(Context context) {
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				handleBroadcast(context, intent);
			}
		};
		IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(FingerprintUpdaterAsyncTask.ACTION_UPDATE_START);
		intentFilter.addAction(FingerprintUpdaterAsyncTask.ACTION_UPDATE_COMPLETED);

		context.registerReceiver(broadcastReceiver, intentFilter);
	}

	private void destroyBroadcast(Context context) {
		if (broadcastReceiver != null) {
			try {
				context.unregisterReceiver(broadcastReceiver);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void handleBroadcast(Context context, Intent intent) {
		if (intent == null || intent.getAction() == null)
			return;

		String action = intent.getAction();

		if (action.equalsIgnoreCase(FingerprintUpdaterAsyncTask.ACTION_UPDATE_START)) {
			info("Fingerprint updater started.");

			local_updater_status.setVisibility(View.VISIBLE);
		} else if (action.equalsIgnoreCase(FingerprintUpdaterAsyncTask.ACTION_UPDATE_START)) {
			info("Fingerprint updater stopped.");

			local_updater_status.setVisibility(View.VISIBLE);
		}

		checkLocalUpdater();
	}

	//endregion

	private void checkLocalUpdater() {
		try {
			double score = (double) Fingerprint.getSize() / (double) Music.getSize();

			String s = (int) (score * 100) + "% fingerprinted locally";

			info.setText(s);

			if (FingerprintUpdaterAsyncTask.getInstance() == null) {
				local_updater_status.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
				local_updater_status.setImageResource(R.drawable.ic_settings_remote_black);
			} else {
				local_updater_status.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
				local_updater_status.setImageResource(R.drawable.ic_dialog_close_dark);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//region Lookup

	private View lookup_layout;
	private ImageView lookup_image;
	private TextView lookup_text;

	private void createLookup(View v) {
		lookup_layout = v.findViewById(R.id.lookup_layout);

		lookup_image = v.findViewById(R.id.lookup_image);
		lookup_text = v.findViewById(R.id.lookup_text);

		lookup_layout.animate().alpha(0).setDuration(333).start();
	}

	private void updateLookup(final String uriOrPath, Bitmap image, String text, boolean local) {
		if (TextUtils.isEmpty(text)) {
			lookup_layout.animate().alpha(0).setDuration(333).start();
		} else {
			lookup_image.setImageBitmap(image);
			lookup_text.setText(text);

			lookup_layout.animate().alpha(1).setDuration(369).start();
		}

		if (!TextUtils.isEmpty(uriOrPath)) {
			if (local) {
				lookup_layout.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						try {
							Intent i = new Intent(getContext(), MusicService.class);

							i.setAction(MusicService.ACTION_OPEN);
							i.putExtra(MusicService.KEY_URI, uriOrPath);

							getContext().startService(i);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
			} else {
				lookup_layout.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						try {
							Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
							intent.putExtra(SearchManager.QUERY, uriOrPath);
							getContext().startActivity(intent);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
			}
		}
	}

	//endregion

	private boolean isProcessing = false;

	private byte[] buffer;
	private int bufferIndex;
	private AudioRecord recorder;
	private RecorderThread thread;

	private void startProcessing() {
		if (!hasPermissions) {
			info("Please give audio recording permissions!");

			return;
		}

		if (isProcessing)
			stopProcessing();

		isProcessing = true;

		icon.animate().alpha(0.5f).setDuration(253).start();

		text.setText("Recording ...");

		// FX
		if (avfx_view == null) {
			avfx_layout.setVisibility(View.VISIBLE);

			avfx_view = new WaveformView(getContext(), null);

			avfx_layout.removeAllViews();
			avfx_layout.addView(avfx_view);

			avfx_view.setChannels(1);
			avfx_view.setSampleRate(SAMPLE_RATE);
			avfx_view.setShowTextAxis(true);
		}

		int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);

		if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
			bufferSize = SAMPLE_RATE * 2;
		}

		recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
				SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSize);

		buffer = new byte[bufferSize * (60 * 3) * 2];

		if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
			Log.e(TAG, "Audio Record can't initialize!");

			return;
		}

		bufferIndex = 0;
		try {
			recorder.startRecording();

			Log.v(TAG, "Start recording");

			thread = new RecorderThread();
			thread.start();
		} catch (IllegalStateException e) {
			e.printStackTrace();

			text.setText("Error!");
		}

	}

	private void stopProcessing() {
		icon.animate().alpha(1).setDuration(333).start();

		text.setText("...");

		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}

		if (recorder != null) {
			recorder.stop();
			recorder = null;
		}

		// FX
		if (avfx_view != null) {
			avfx_layout.setVisibility(View.INVISIBLE);

			avfx_layout.removeAllViews();

			avfx_view = null;
		}

		isProcessing = false;
	}

	private static final int SAMPLE_RATE = 11025;
	private static final short CHANNELS = 1;
	private static final int TRY_MATCH_INTERVAL = 4000;

	private class RecorderThread extends Thread {
		private boolean processing = false;
		private boolean detected = false;
		private long lastMatchTryTime;

		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

			lastMatchTryTime = SystemClock.uptimeMillis();

			while (!isInterrupted() && bufferIndex < buffer.length) {
				try {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							text.setText("Recording ...");
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

				int read;
				synchronized (this) {
					read = recorder.read(buffer, bufferIndex, Math.min(512, buffer.length - bufferIndex));

					if (read == AudioRecord.ERROR_BAD_VALUE) {
						Log.e(TAG, "BAD_VALUE while reading recorder");
						break;
					} else if (read == AudioRecord.ERROR_INVALID_OPERATION) {
						Log.e(TAG, "INVALID_OPERATION while reading recorder");
						break;
					} else if (read >= 0) {
						bufferIndex += read;
					}
				}

				if (read >= 0) {
					if (bufferIndex > 10) {
						if (avfx_view != null) {
							short[] s = new short[buffer.length / 2];
							ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s);

							avfx_view.setSamples(s);
						}
					}

					long currentTime = SystemClock.uptimeMillis();
					if (currentTime - lastMatchTryTime >= TRY_MATCH_INTERVAL) {
						tryMatchCurrentBuffer();

						lastMatchTryTime = SystemClock.uptimeMillis();
					}
				}
			}

			if (!detected) {
				tryMatchCurrentBuffer();
			}

			while (processing) {
				try {
					if (isDetached())
						continue;

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							text.setText("Waiting ...");
						}
					});

					Thread.sleep(333);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			try {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						stopProcessing();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void tryMatchCurrentBuffer() {
			if (bufferIndex > 0) {
				new Thread() {
					public void run() {
						// Allow only one upload call at a time
						if (processing) {
							Log.d(TAG, "Not sending, data already sending");
							return;
						}

						processing = true;

						try {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									text.setText("Matching ...");
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}

						byte[] copy;
						int length;
						synchronized (RecorderThread.this) {
							length = bufferIndex;
						}

						copy = new byte[length];
						System.arraycopy(buffer, 0, copy, 0, length);

						try {
							int[] rawfp = Fingerprint.GenerateRawFingerprint(copy, CHANNELS, SAMPLE_RATE);
							if (rawfp == null || rawfp.length == 0)
								throw new Exception("No fingerprint");

							// Search local

							final Fingerprint fingerprint = Fingerprint.search(rawfp, 0.65, 0.95);
							if (fingerprint != null) {
								final Music music = Music.load(context, fingerprint.getId());
								if (music != null)
									getActivity().runOnUiThread(new Runnable() {
										@Override
										public void run() {
											updateLookup(music.getPath(), music.getCover(getContext(), -1), music.getText(), true);
										}
									});
							} else {

								// Search online

								String fp = Fingerprint.GenerateFingerprint(copy, CHANNELS, SAMPLE_RATE);
								if (TextUtils.isEmpty(fp))
									throw new Exception("No fingerprint");

								Api.LookupFingerprintDataAsyncTask asyncTask = Api.lookup(
										fp,
										(long) ((float) bufferIndex / (float) SAMPLE_RATE),
										new JavaEx.ActionT<Map<String, String>>() {
											@Override
											public void execute(final Map<String, String> result) {
												if (result.size() > 0)
													getActivity().runOnUiThread(new Runnable() {
														@Override
														public void run() {
															final String title = result.get("title");
															final String artist = result.get("artist");
															final String album = result.get("album");
															final String score = result.get("score");
															final String id = result.get("id");

															updateLookup(artist + " - " + title, null, artist + " - " + title, false);
														}
													});
											}
										},
										new JavaEx.ActionT<Exception>() {
											@Override
											public void execute(Exception e) {
												e.printStackTrace();
											}
										});
								asyncTask.get(3, TimeUnit.SECONDS);

							}
						} catch (Exception e) {
							e.printStackTrace();
						}

						processing = false;

						try {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									text.setText("...");
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			} else {
				Log.e(TAG, "0 bytes recorded!?");
			}
		}

	}

	public static void showAsDialog(Context context) {
		if (Music.getSize() >= 5) {
			BaseDialogUIActivity.show(context, FingerprintViewFragment.class, Bundle.EMPTY);
		} else {
			Toast.makeText(context, "Play some music first!", Toast.LENGTH_LONG).show();
		}
	}

	public class WaveformView extends View {
		public static final int MODE_RECORDING = 1;
		public static final int MODE_PLAYBACK = 2;

		private static final int HISTORY_SIZE = 6;

		private TextPaint mTextPaint;
		private Paint mStrokePaint, mFillPaint, mMarkerPaint;

		// Used in draw
		private int brightness;
		private Rect drawRect;

		private int width, height;
		private float xStep, centerY;
		private int mMode, mAudioLength, mMarkerPosition, mSampleRate, mChannels;
		private short[] mSamples;
		private LinkedList<float[]> mHistoricalData;
		private Picture mCachedWaveform;
		private Bitmap mCachedWaveformBitmap;
		private int colorDelta = 255 / (HISTORY_SIZE + 1);
		private boolean showTextAxis = true;

		public WaveformView(Context context) {
			super(context);
			init(context, null, 0);
		}

		public WaveformView(Context context, AttributeSet attrs) {
			super(context, attrs);
			init(context, attrs, 0);
		}

		public WaveformView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			init(context, attrs, defStyle);
		}

		private void init(Context context, AttributeSet attrs, int defStyle) {
			// Load attributes

			mMode = MODE_RECORDING;

			float strokeThickness = 1.1f;
			int mStrokeColor = ContextCompat.getColor(context, android.R.color.holo_purple);
			int mFillColor = ColorUtils.setAlphaComponent(mStrokeColor, 160);
			int mMarkerColor = Color.parseColor("#ffff66");
			int mTextColor = Color.parseColor("#ddffffdd");

			mTextPaint = new TextPaint();
			mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
			mTextPaint.setTextAlign(Paint.Align.CENTER);
			mTextPaint.setColor(mTextColor);
			mTextPaint.setTextSize(14);

			mStrokePaint = new Paint();
			mStrokePaint.setColor(mStrokeColor);
			mStrokePaint.setStyle(Paint.Style.STROKE);
			mStrokePaint.setStrokeWidth(strokeThickness);
			mStrokePaint.setAntiAlias(true);

			mFillPaint = new Paint();
			mFillPaint.setStyle(Paint.Style.FILL);
			mFillPaint.setAntiAlias(true);
			mFillPaint.setColor(mFillColor);

			mMarkerPaint = new Paint();
			mMarkerPaint.setStyle(Paint.Style.STROKE);
			mMarkerPaint.setStrokeWidth(0);
			mMarkerPaint.setAntiAlias(true);
			mMarkerPaint.setColor(mMarkerColor);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);

			width = getMeasuredWidth();
			height = getMeasuredHeight();
			xStep = width / (mAudioLength * 1.0f);
			centerY = height / 2f;
			drawRect = new Rect(0, 0, width, height);

			if (mHistoricalData != null) {
				mHistoricalData.clear();
			}
			if (mMode == MODE_PLAYBACK) {
				createPlaybackWaveform();
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			LinkedList<float[]> temp = mHistoricalData;
			if (mMode == MODE_RECORDING && temp != null) {
				brightness = colorDelta;
				for (float[] p : temp) {
					mStrokePaint.setAlpha(brightness);
					canvas.drawLines(p, mStrokePaint);
					brightness += colorDelta;
				}
			} else if (mMode == MODE_PLAYBACK) {
				if (mCachedWaveform != null) {
					canvas.drawPicture(mCachedWaveform);
				} else if (mCachedWaveformBitmap != null) {
					canvas.drawBitmap(mCachedWaveformBitmap, null, drawRect, null);
				}
				if (mMarkerPosition > -1 && mMarkerPosition < mAudioLength)
					canvas.drawLine(xStep * mMarkerPosition, 0, xStep * mMarkerPosition, height, mMarkerPaint);
			}
		}

		public int getMode() {
			return mMode;
		}

		public void setMode(int mMode) {
			mMode = mMode;
		}

		public short[] getSamples() {
			return mSamples;
		}

		public void setSamples(short[] samples) {
			mSamples = samples;
			calculateAudioLength();
			onSamplesChanged();
		}

		public int getMarkerPosition() {
			return mMarkerPosition;
		}

		public void setMarkerPosition(int markerPosition) {
			mMarkerPosition = markerPosition;
			postInvalidate();
		}

		public int getAudioLength() {
			return mAudioLength;
		}

		public int getSampleRate() {
			return mSampleRate;
		}

		public void setSampleRate(int sampleRate) {
			mSampleRate = sampleRate;
			calculateAudioLength();
		}

		public int getChannels() {
			return mChannels;
		}

		public void setChannels(int channels) {
			mChannels = channels;
			calculateAudioLength();
		}

		public boolean showTextAxis() {
			return showTextAxis;
		}

		public void setShowTextAxis(boolean showTextAxis) {
			this.showTextAxis = showTextAxis;
		}

		private void calculateAudioLength() {
			if (mSamples == null || mSampleRate == 0 || mChannels == 0)
				return;

			mAudioLength = AudioEx.calculateAudioLength(mSamples.length, mSampleRate, mChannels);
		}

		private void onSamplesChanged() {
			if (mMode == MODE_RECORDING) {
				if (mHistoricalData == null)
					mHistoricalData = new LinkedList<>();
				LinkedList<float[]> temp = new LinkedList<>(mHistoricalData);

				// For efficiency, we are reusing the array of points.
				float[] waveformPoints;
				if (temp.size() == HISTORY_SIZE) {
					waveformPoints = temp.removeFirst();
				} else {
					waveformPoints = new float[width * 4];
				}

				drawRecordingWaveform(mSamples, waveformPoints);
				temp.addLast(waveformPoints);
				mHistoricalData = temp;
				postInvalidate();
			} else if (mMode == MODE_PLAYBACK) {
				mMarkerPosition = -1;
				xStep = width / (mAudioLength * 1.0f);
				createPlaybackWaveform();
			}
		}

		void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {
			float lastX = -1;
			float lastY = -1;
			int pointIndex = 0;
			float max = Short.MAX_VALUE;

			// For efficiency, we don't draw all of the samples in the buffer, but only the ones
			// that align with pixel boundaries.
			for (int x = 0; x < width; x++) {
				int index = (int) (((x * 1.0f) / width) * buffer.length);
				short sample = buffer[index];
				float y = centerY - ((sample / max) * centerY);

				if (lastX != -1) {
					waveformPoints[pointIndex++] = lastX;
					waveformPoints[pointIndex++] = lastY;
					waveformPoints[pointIndex++] = x;
					waveformPoints[pointIndex++] = y;
				}

				lastX = x;
				lastY = y;
			}
		}

		Path drawPlaybackWaveform(int width, int height, short[] buffer) {
			Path waveformPath = new Path();
			float centerY = height / 2f;
			float max = Short.MAX_VALUE;

			short[][] extremes = AudioEx.getExtremes(buffer, width);


			waveformPath.moveTo(0, centerY);

			// draw maximums
			for (int x = 0; x < width; x++) {
				short sample = extremes[x][0];
				float y = centerY - ((sample / max) * centerY);
				waveformPath.lineTo(x, y);
			}

			// draw minimums
			for (int x = width - 1; x >= 0; x--) {
				short sample = extremes[x][1];
				float y = centerY - ((sample / max) * centerY);
				waveformPath.lineTo(x, y);
			}

			waveformPath.close();

			return waveformPath;
		}

		private void createPlaybackWaveform() {
			if (width <= 0 || height <= 0 || mSamples == null)
				return;

			Canvas cacheCanvas;
			if (Build.VERSION.SDK_INT >= 23 && isHardwareAccelerated()) {
				mCachedWaveform = new Picture();
				cacheCanvas = mCachedWaveform.beginRecording(width, height);
			} else {
				mCachedWaveformBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				cacheCanvas = new Canvas(mCachedWaveformBitmap);
			}

			Path mWaveform = drawPlaybackWaveform(width, height, mSamples);
			cacheCanvas.drawPath(mWaveform, mFillPaint);
			cacheCanvas.drawPath(mWaveform, mStrokePaint);
			drawAxis(cacheCanvas, width);

			if (mCachedWaveform != null)
				mCachedWaveform.endRecording();
		}

		private void drawAxis(Canvas canvas, int width) {
			if (!showTextAxis) return;
			int seconds = mAudioLength / 1000;
			float xStep = width / (mAudioLength / 1000f);
			float textHeight = mTextPaint.getTextSize();
			float textWidth = mTextPaint.measureText("10.00");
			int secondStep = (int) (textWidth * seconds * 2) / width;
			secondStep = Math.max(secondStep, 1);
			for (float i = 0; i <= seconds; i += secondStep) {
				canvas.drawText(String.format("%.2f", i), i * xStep, textHeight, mTextPaint);
			}
		}
	}

}
