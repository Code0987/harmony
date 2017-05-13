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
/**
 * Created by Deepak on 13-05-2017.
 */

public class BrowserUILiteActivity extends BasePlaybackUIActivity {


    private static final String TAG=BrowserUILiteActivity.class.getSimpleName();


    LibraryUIDarkActivity.RecyclerViewAdapter adapter;

    private View root;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private AsyncTask<Void,Void,Collection<Music>> refreshTask= null;

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
        recyclerView.setItemViewCacheSize(7);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        adapter = new LibraryUIDarkActivity.RecyclerViewAdapter();
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


}
