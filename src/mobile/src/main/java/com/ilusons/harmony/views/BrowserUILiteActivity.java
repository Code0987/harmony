package com.ilusons.harmony.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BasePlaybackUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.StorageEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.Collection;

public class BrowserUILiteActivity extends BasePlaybackUIActivity {

    // Logger TAG
    private static final String TAG = BrowserUILiteActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    RecyclerViewAdapter adapter;

    // UI
    private View root;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private AsyncTask<Void, Void, Collection<Music>> refreshTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view
        setContentView(R.layout.browser_lite_activty);

        // Set views
        root = findViewById(R.id.root);

        // Set recycler
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView_lite);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(7);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        // Set swipe to refresh
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout_lite);
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
                        return Music.load(BrowserUILiteActivity.this);
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

        // Load player ui
        if (getMusicService() != null && getMusicService().isPlaying()) {
            Intent intent = new Intent(this, PlaybackUIDarkActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setType("audio/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(i, REQUEST_FILE_PICK);
            }
        });

        findViewById(R.id.fab_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent musicServiceIntent = new Intent(BrowserUILiteActivity.this, MusicService.class);
                musicServiceIntent.setAction(MusicService.ACTION_LIBRARY_UPDATE);
                musicServiceIntent.putExtra(MusicService.KEY_LIBRARY_UPDATE_FORCE, true);
                startService(musicServiceIntent);
            }
        });

        findViewById(R.id.fab_playback_ui).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BrowserUILiteActivity.this, PlaybackUIDarkActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
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
    public void OnMusicServiceOpen(String uri) {
        Intent intent = new Intent(BrowserUILiteActivity.this, PlaybackUIDarkActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void OnMusicServiceLibraryUpdated() {
        if (adapter != null)
            adapter.setData(Music.load(BrowserUILiteActivity.this));
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private ArrayList<Music> data;

        public RecyclerViewAdapter() {
            data = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            View view = inflater.inflate(R.layout.browser_item, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Music item = data.get(position);
            final View v = holder.view;

            // Bind data to view here!

            final AVLoadingIndicatorView loadingView = (AVLoadingIndicatorView) v.findViewById(R.id.loadingView);
            loadingView.show();

            MusicService musicService = getMusicService();
            if (musicService != null && musicService.getCurrentPlaylistItem() != null) {
                if (!musicService.getCurrentPlaylistItem().equals(item.Path))
                    loadingView.hide();
            } else {
                loadingView.hide();
            }

            final ImageView cover = (ImageView) v.findViewById(R.id.cover);
            cover.setImageBitmap(null);
            // HACK: This animates aw well as reduces load on image view
            final int coverSize = Math.max(cover.getWidth(), cover.getHeight());
            (new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    return item.getCover(BrowserUILiteActivity.this, coverSize);
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
                    loadingView.show();

                    Intent i = new Intent(BrowserUILiteActivity.this, MusicService.class);

                    i.setAction(MusicService.ACTION_OPEN);
                    i.putExtra(MusicService.KEY_URI, item.Path);

                    startService(i);
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public void setData(Collection<Music> d) {
            data.clear();
            data.addAll(d);
            notifyDataSetChanged();
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
