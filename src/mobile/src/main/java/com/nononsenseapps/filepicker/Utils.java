package com.nononsenseapps.filepicker;

import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Utils {
	private Utils() {
	}

	public static List<Uri> getSelectedFilesFromResult(Intent data) {
		return new ArrayList<>();
	}

	public static File getFileForUri(Uri uri) {
		return uri == null ? null : new File(uri.getPath() == null ? "" : uri.getPath());
	}
}
