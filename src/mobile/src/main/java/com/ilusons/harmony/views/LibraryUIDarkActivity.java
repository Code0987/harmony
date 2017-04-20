package com.ilusons.harmony.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.BaseMediaBroadcastReceiver;
import com.ilusons.harmony.base.BasePlaybackUIActivity;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.data.Music;
import com.ilusons.harmony.ref.StorageEx;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.Collection;

public class LibraryUIDarkActivity extends BasePlaybackUIActivity {

    // Logger TAG
    private static final String TAG = LibraryUIDarkActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_FILE_PICK = 4684;

    RecyclerViewAdapter adapter;

    BaseMediaBroadcastReceiver broadcastReceiver = new BaseMediaBroadcastReceiver() {
        @Override
        public void open(String uri) {
            Intent intent = new Intent(LibraryUIDarkActivity.this, PlaybackUIDarkActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        @Override
        public void libraryUpdated() {
            if (adapter != null)
                adapter.setData(Music.load(LibraryUIDarkActivity.this));
        }
    };

    // UI
    private View root;

    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set view
        setContentView(R.layout.library_ui_dark_activity);

        // Set views
        root = findViewById(R.id.root);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        adapter = new RecyclerViewAdapter();

        recyclerView.setAdapter(adapter);

        adapter.setData(Music.load(this));

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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            broadcastReceiver.unRegister();
        } catch (final Throwable e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        broadcastReceiver.register(this);
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


    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private ArrayList<Music> data;

        public RecyclerViewAdapter() {
            data = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            View view = inflater.inflate(R.layout.library_ui_dark_item, parent, false);

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

            ImageView cover = (ImageView) v.findViewById(R.id.cover);
            Bitmap coverBitmap = item.getCover(LibraryUIDarkActivity.this);
            if (coverBitmap != null)
                cover.setImageBitmap(coverBitmap);

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

                    Intent i = new Intent(LibraryUIDarkActivity.this, MusicService.class);

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
