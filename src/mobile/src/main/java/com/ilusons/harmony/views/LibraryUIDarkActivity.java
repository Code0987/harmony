package com.ilusons.harmony.views;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.MainActivity;
import com.ilusons.harmony.R;
import com.ilusons.harmony.SettingsActivity;
import com.ilusons.harmony.base.BaseUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.AndroidEx;
import com.ilusons.harmony.ref.SPrefEx;
import com.ilusons.harmony.ref.StorageEx;

import java.util.ArrayList;
import java.util.Collection;

public class LibraryUIDarkActivity extends BaseUIActivity {

    // Logger TAG
    private static final String TAG = LibraryUIDarkActivity.class.getSimpleName();

    public static final String TAG_SPREF_LIBRARY_VIEW_mp3 = SPrefEx.TAG_SPREF + ".library_view_mp3";
    public static boolean LIBRARY_VIEW_mp3_DEFAULT = true;
    public static final String TAG_SPREF_LIBRARY_VIEW_m4a = SPrefEx.TAG_SPREF + ".library_view_m4a";
    public static boolean LIBRARY_VIEW_m4a_DEFAULT = true;
    public static final String TAG_SPREF_LIBRARY_VIEW_mp4 = SPrefEx.TAG_SPREF + ".library_view_mp4";
    public static boolean LIBRARY_VIEW_mp4_DEFAULT = false;
    public static final String TAG_SPREF_LIBRARY_VIEW_flac = SPrefEx.TAG_SPREF + ".library_view_flac";
    public static boolean LIBRARY_VIEW_flac_DEFAULT = true;

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    RecyclerViewAdapter adapter;

    // UI
    private View root;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private View settings_layout;
    private SearchView search_view;

    private AsyncTask<Void, Void, Collection<Music>> refreshTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view
        setContentView(R.layout.library_ui_dark_activity);

        // Set views
        root = findViewById(R.id.root);

        // Set recycler
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(11);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // Set swipe to refresh
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        final SwipeRefreshLayout.OnRefreshListener swipeRefreshLayoutOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (refreshTask != null && !refreshTask.isCancelled()) {
                    refreshTask.cancel(true);

                    try {
                        refreshTask.get();
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                }

                refreshTask = (new AsyncTask<Void, Void, Collection<Music>>() {
                    @Override
                    protected Collection<Music> doInBackground(Void... voids) {
                        return Music.load(LibraryUIDarkActivity.this);
                    }

                    @Override
                    protected void onPreExecute() {
                        swipeRefreshLayout.setRefreshing(true);
                    }

                    @Override
                    protected void onPostExecute(Collection<Music> data) {
                        adapter.setData(data);

                        swipeRefreshLayout.setRefreshing(false);

                        refreshTask = null;
                    }
                });

                refreshTask.execute();
            }
        };
        swipeRefreshLayout.setOnRefreshListener(swipeRefreshLayoutOnRefreshListener);

        // Load data
        swipeRefreshLayoutOnRefreshListener.onRefresh();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFabItems();
            }
        });

        findViewById(R.id.fab_item1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setType("audio/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_FILE_PICK);

                toggleFabItems();
            }
        });

        findViewById(R.id.fab_item2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent musicServiceIntent = new Intent(LibraryUIDarkActivity.this, MusicService.class);
                musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
                musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
                startService(musicServiceIntent);

                toggleFabItems();
            }
        });

        findViewById(R.id.fab_item3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LibraryUIDarkActivity.this, PlaybackUIDarkActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                toggleFabItems();
            }
        });

        settings_layout = findViewById(R.id.settings_layout);

        findViewById(R.id.fab_item4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings_layout.animate()
                        .alpha(1)
                        .setDuration(270)
                        .start();
                settings_layout.setVisibility(View.VISIBLE);

                toggleFabItems();

                info("Long press to open app settings!");
            }
        });

        findViewById(R.id.fab_item4).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(LibraryUIDarkActivity.this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                toggleFabItems();

                return true;
            }
        });

        Switch mp3_switch = (Switch) findViewById(R.id.mp3_switch);
        mp3_switch.setChecked(SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_VIEW_mp3, LIBRARY_VIEW_mp3_DEFAULT));
        mp3_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPrefEx.get(LibraryUIDarkActivity.this)
                        .edit()
                        .putBoolean(TAG_SPREF_LIBRARY_VIEW_mp3, b)
                        .apply();

                if (search_view != null)
                    adapter.filter("" + search_view.getQuery());
            }
        });

        Switch m4a_switch = (Switch) findViewById(R.id.m4a_switch);
        m4a_switch.setChecked(SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_VIEW_m4a, LIBRARY_VIEW_m4a_DEFAULT));
        m4a_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPrefEx.get(LibraryUIDarkActivity.this)
                        .edit()
                        .putBoolean(TAG_SPREF_LIBRARY_VIEW_m4a, b)
                        .apply();

                if (search_view != null)
                    adapter.filter("" + search_view.getQuery());
            }
        });

        Switch mp4_switch = (Switch) findViewById(R.id.mp4_switch);
        mp4_switch.setChecked(SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_VIEW_mp4, LIBRARY_VIEW_mp4_DEFAULT));
        mp4_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPrefEx.get(LibraryUIDarkActivity.this)
                        .edit()
                        .putBoolean(TAG_SPREF_LIBRARY_VIEW_mp4, b)
                        .apply();

                if (search_view != null)
                    adapter.filter("" + search_view.getQuery());
            }
        });

        Switch flac_switch = (Switch) findViewById(R.id.flac_switch);
        flac_switch.setChecked(SPrefEx.get(this).getBoolean(TAG_SPREF_LIBRARY_VIEW_flac, LIBRARY_VIEW_flac_DEFAULT));
        flac_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPrefEx.get(LibraryUIDarkActivity.this)
                        .edit()
                        .putBoolean(TAG_SPREF_LIBRARY_VIEW_flac, b)
                        .apply();

                if (search_view != null)
                    adapter.filter("" + search_view.getQuery());
            }
        });

        search_view = (SearchView) findViewById(R.id.search_view);

        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);

                return false;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                Intent i = new Intent();
                i.setType("audio/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_FILE_PICK);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult\nrequestCode=" + requestCode + "\nresultCode=" + resultCode + "\ndata=" + data);

        if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK) {
            Uri uri = Uri.parse(StorageEx.getPath(this, data.getData()));

            Intent i = new Intent(this, MusicService.class);

            i.setAction(MusicService.ACTION_OPEN);
            i.putExtra(MusicService.KEY_URI, uri.toString());

            startService(i);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {

            if (settings_layout != null && settings_layout.getVisibility() == View.VISIBLE) {
                Rect r = new Rect(0, 0, 0, 0);
                settings_layout.getHitRect(r);
                if (!r.contains((int) ev.getX(), (int) ev.getY())) {
                    settings_layout.animate()
                            .alpha(0)
                            .setDuration(220)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    settings_layout.setVisibility(View.INVISIBLE);
                                }
                            })
                            .start();

                    return true;
                }
            }

        }

        return super.dispatchTouchEvent(ev);
    }

    public void toggleFabItems() {
        try {
            View fab = findViewById(R.id.fab);
            View fab_item1_layout = findViewById(R.id.fab_item1_layout);
            View fab_item2_layout = findViewById(R.id.fab_item2_layout);
            View fab_item3_layout = findViewById(R.id.fab_item3_layout);
            View fab_item4_layout = findViewById(R.id.fab_item4_layout);

            boolean open = fab.getRotation() > 0;

            if (!open) {
                ViewCompat.animate(fab)
                        .rotation(45.0F)
                        .withLayer()
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(10.0F))
                        .start();

                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_items_open);

                fab_item1_layout.startAnimation(animation);
                fab_item2_layout.startAnimation(animation);
                fab_item3_layout.startAnimation(animation);
                fab_item4_layout.startAnimation(animation);
            } else {
                ViewCompat.animate(fab)
                        .rotation(0.0F)
                        .withLayer()
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(10.0F))
                        .start();

                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_items_close);

                fab_item1_layout.startAnimation(animation);
                fab_item2_layout.startAnimation(animation);
                fab_item3_layout.startAnimation(animation);
                fab_item4_layout.startAnimation(animation);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void OnMusicServicePlay() {
        super.OnMusicServicePlay();
    }

    @Override
    public void OnMusicServicePause() {
        super.OnMusicServicePause();
    }

    @Override
    public void OnMusicServiceStop() {
        super.OnMusicServiceStop();
    }

    @Override
    public void OnMusicServiceOpen(String uri) {
        if (SettingsActivity.getUIPlaybackAutoOpen(this))
            MainActivity.openPlaybackUIActivity(this);
    }

    @Override
    public void OnMusicServiceLibraryUpdateBegins() {
        swipeRefreshLayout.setRefreshing(true);

        info("Library update is on progress!");
    }

    @Override
    public void OnMusicServiceLibraryUpdated() {
        if (adapter != null)
            adapter.setData(Music.load(LibraryUIDarkActivity.this));

        swipeRefreshLayout.setRefreshing(false);

        info("Library updated!");
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final int ITEMS_PER_AD = 8;
        private AdListener lastAdListener = null;

        private ArrayList<Music> data;
        private ArrayList<Object> dataFiltered;

        public RecyclerViewAdapter() {
            data = new ArrayList<>();
            dataFiltered = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            View view = inflater.inflate(R.layout.library_ui_dark_item, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Object d = dataFiltered.get(position);
            final View v = holder.view;

            // Bind data to view here!

            if (d instanceof NativeExpressAdView && lastAdListener == null) {

                CardView cv = (CardView) v.findViewById(R.id.cardView);

                final NativeExpressAdView adView = (NativeExpressAdView) d;

                adView.setAdSize(new AdSize((int) ((cv.getWidth() - cv.getPaddingLeft() - cv.getPaddingRight()) / getResources().getDisplayMetrics().density), 96));
                adView.setAdUnitId(BuildConfig.AD_UNIT_ID_NE1);

                cv.addView(adView, new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT));

                lastAdListener = new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();

                        lastAdListener = null;
                    }

                    @Override
                    public void onAdFailedToLoad(int errorCode) {

                        lastAdListener = null;
                    }
                };
                adView.setAdListener(lastAdListener);

                AdRequest adRequest;
                if (BuildConfig.DEBUG) {
                    adRequest = new AdRequest.Builder()
                            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                            .addTestDevice(AndroidEx.getDeviceIdHashed(LibraryUIDarkActivity.this))
                            .build();
                } else {
                    adRequest = new AdRequest.Builder()
                            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                            .build();
                }

                adView.loadAd(adRequest);


            } else if (d instanceof Music) {

                final Music item = (Music) dataFiltered.get(position);

                final ImageView cover = (ImageView) v.findViewById(R.id.cover);
                cover.setImageBitmap(null);
                // HACK: This animates aw well as reduces load on image view
                final int coverSize = Math.max(cover.getWidth(), cover.getHeight());
                (new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        return item.getCover(LibraryUIDarkActivity.this, coverSize);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        TransitionDrawable d = new TransitionDrawable(new Drawable[]{
                                cover.getDrawable(),
                                new BitmapDrawable(getResources(), bitmap)
                        });

                        cover.setImageDrawable(d);

                        d.setCrossFadeEnabled(true);
                        d.startTransition(200);
                    }
                }).execute();

                TextView title = (TextView) v.findViewById(R.id.title);
                title.setText(item.Title);

                TextView artist = (TextView) v.findViewById(R.id.artist);
                artist.setText(item.Artist);

                TextView album = (TextView) v.findViewById(R.id.album);
                album.setText(item.Album);

                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(LibraryUIDarkActivity.this, MusicService.class);

                        i.setAction(MusicService.ACTION_OPEN);
                        i.putExtra(MusicService.KEY_URI, item.Path);

                        startService(i);
                    }
                });

            }

        }

        @Override
        public int getItemCount() {
            return dataFiltered.size();
        }

        public void setData(Collection<Music> d) {
            data.clear();
            data.addAll(d);

            filter(null);
        }

        public void filter(final String text) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    dataFiltered.clear();

                    for (Music item : data) {
                        boolean f = false;

                        SharedPreferences spref = SPrefEx.get(LibraryUIDarkActivity.this);

                        f |= item.Path.toLowerCase().endsWith(".mp3") && spref.getBoolean(TAG_SPREF_LIBRARY_VIEW_mp3, LIBRARY_VIEW_mp3_DEFAULT);
                        f |= item.Path.toLowerCase().endsWith(".m4a") && spref.getBoolean(TAG_SPREF_LIBRARY_VIEW_m4a, LIBRARY_VIEW_m4a_DEFAULT);
                        f |= item.Path.toLowerCase().endsWith(".mp4") && spref.getBoolean(TAG_SPREF_LIBRARY_VIEW_mp4, LIBRARY_VIEW_mp4_DEFAULT);
                        f |= item.Path.toLowerCase().endsWith(".flac") && spref.getBoolean(TAG_SPREF_LIBRARY_VIEW_flac, LIBRARY_VIEW_flac_DEFAULT);

                        f &= TextUtils.isEmpty(text) || text.length() < 1 || item.getTextDetailed().toLowerCase().contains(text);

                        if (f)
                            dataFiltered.add(item);
                    }

                    // Add ads
                    final int n = dataFiltered.size();
                    for (int i = 0; i <= n; i += ITEMS_PER_AD)
                        try {
                            final NativeExpressAdView adView = new NativeExpressAdView(LibraryUIDarkActivity.this);
                            dataFiltered.add(i, adView);
                        } catch (Exception e) {
                            Log.w(TAG, e);
                        }

                    (LibraryUIDarkActivity.this).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View view;

        public ViewHolder(View view) {
            super(view);

            this.view = view;
        }

    }

}
