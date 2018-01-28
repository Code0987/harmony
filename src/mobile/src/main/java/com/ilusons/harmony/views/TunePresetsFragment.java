package com.ilusons.harmony.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ilusons.harmony.R;
import com.ilusons.harmony.base.MusicService;
import com.ilusons.harmony.ref.ImageEx;
import com.ilusons.harmony.ref.SPrefEx;

import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class TunePresetsFragment extends Fragment {

	// Logger TAG
	private static final String TAG = TunePresetsFragment.class.getSimpleName();

	private static final int REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF = 58;
	private static final int REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF = 59;

	private View root;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Set view
		View v = inflater.inflate(R.layout.tune_presets, container, false);

		// Set views
		root = v.findViewById(R.id.root);

		createItems(v);

		return v;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "r");
					FileInputStream fileInputStream = new FileInputStream(pfd.getFileDescriptor());

					if (SPrefEx.importSPrefs(getContext(), fileInputStream, getKeys()))
						Toast.makeText(getContext(), "Preset successfully imported to current profile. Please restart to apply.", Toast.LENGTH_LONG).show();
					else
						Toast.makeText(getContext(), "Some problem while importing current preset.", Toast.LENGTH_LONG).show();

					fileInputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		} else if (requestCode == REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF && resultCode == Activity.RESULT_OK) {
			Uri uri = null;
			if (data != null) {
				uri = data.getData();

				try {
					ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "w");
					FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

					if (SPrefEx.exportSPrefs(getContext(), fileOutputStream, uri, getKeys()))
						Toast.makeText(getContext(), "Current preset successfully exported.", Toast.LENGTH_LONG).show();
					else
						Toast.makeText(getContext(), "Some problem while exporting selected preset.", Toast.LENGTH_LONG).show();

					fileOutputStream.close();
					pfd.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private RecyclerViewAdapter adapter;

	private void createItems(View v) {

		Button import_preset = v.findViewById(R.id.import_preset);
		import_preset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				String[] mimes = new String[]{"text/plain"};
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
				intent.putExtra(Intent.EXTRA_TITLE, "harmony_tune_preset.txt");
				intent.setType(StringUtils.join(mimes, '|'));
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, REQUEST_PRESETS_IMPORT_LOCATION_PICK_SAF);
			}
		});

		Button export_preset = v.findViewById(R.id.export_preset);
		export_preset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				String[] mimes = new String[]{"text/plain"};
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
				intent.putExtra(Intent.EXTRA_TITLE, "harmony_tune_preset.txt");
				intent.setType(StringUtils.join(mimes, '|'));
				if (intent.resolveActivity(getContext().getPackageManager()) != null) {
					startActivityForResult(intent, REQUEST_PRESETS_EXPORT_LOCATION_PICK_SAF);
				} else {
					Toast.makeText(getContext(), "SAF not found!", Toast.LENGTH_LONG).show();
				}
			}
		});

		adapter = new RecyclerViewAdapter();
		adapter.setHasStableIds(true);

		RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
		recyclerView.setHasFixedSize(true);
		recyclerView.setItemViewCacheSize(11);
		recyclerView.setDrawingCacheEnabled(true);
		recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
		recyclerView.setAdapter(adapter);

		adapter.refresh(v.getContext());
	}

	public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

		private final static String ASSETS_PRESETS = "presets/tune";
		private final static String EXT_PRESET_SQ = ".sqp";
		private final static String EXT_PRESET_HQ = ".hqp";

		private final ArrayList<String> data;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			View view = inflater.inflate(R.layout.tune_presets_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			holder.bind(position, data.get(position));
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ImageView image;
			public TextView text;

			public ViewHolder(View v) {
				super(v);

				view = v;

				image = v.findViewById(R.id.image);
				text = v.findViewById(R.id.text);
			}

			@SuppressLint("StaticFieldLeak")
			public void bind(int p, final String d) {
				final Context context = view.getContext();

				String name = d.substring(0, d.lastIndexOf('.'));

				try {
					Bitmap bitmap = ImageEx.getBitmapFromAsset(context.getAssets(), ASSETS_PRESETS + "/" + name + ".jpg");

					image.setImageBitmap(bitmap);
				} catch (Exception e) {
					e.printStackTrace();

					image.setImageDrawable(null);
				}
				text.setText(name.toUpperCase());

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						try {
							if (!SPrefEx.importSPrefs(view.getContext(), view.getContext().getAssets().open(ASSETS_PRESETS + "/" + d), getKeys()))
								throw new Exception();

							// Refresh sfx
							try {
								Intent musicServiceIntent = new Intent(view.getContext(), MusicService.class);
								musicServiceIntent.setAction(MusicService.ACTION_REFRESH_SFX);
								view.getContext().startService(musicServiceIntent);
							} catch (Exception e) {
								e.printStackTrace();
							}

							Toast.makeText(view.getContext(), "Preset applied successfully!", Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							e.printStackTrace();

							Toast.makeText(view.getContext(), "Preset import failed!!", Toast.LENGTH_LONG).show();
						}
					}
				});
			}

		}

		public void refresh(Context context) {
			data.clear();

			try {
				String suffix;
				switch (MusicService.getPlayerType(context)) {
					case OpenSL:
						suffix = EXT_PRESET_HQ;
						break;
					case AndroidOS:
					default:
						suffix = EXT_PRESET_SQ;
						break;
				}

				for (String name : Arrays.asList(context.getAssets().list(ASSETS_PRESETS)))
					if (name.toLowerCase().endsWith(suffix) && !data.contains(name))
						data.add(name);
			} catch (Exception e) {
				e.printStackTrace();
			}

			notifyDataSetChanged();
		}

	}

	public static ArrayList<String> getKeys() {
		ArrayList<String> keys = new ArrayList<>();

		keys.addAll(Arrays.asList(MusicService.ExportableSPrefKeys));
		//keys.addAll(Arrays.asList(AudioVFXViewFragment.ExportableSPrefKeys));
		//keys.addAll(Arrays.asList(LibraryViewFragment.ExportableSPrefKeys));
		//keys.addAll(Arrays.asList(PlaybackUIActivity.ExportableSPrefKeys));

		return keys;
	}

	public static void showAsDialog(Context context) {
		FragmentDialogActivity.show(context, TunePresetsFragment.class, Bundle.EMPTY);
	}

	public static TunePresetsFragment create() {
		TunePresetsFragment f = new TunePresetsFragment();
		return f;
	}

}
