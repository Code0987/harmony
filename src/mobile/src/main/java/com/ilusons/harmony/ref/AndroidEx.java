package com.ilusons.harmony.ref;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.v4.content.IntentCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;


public class AndroidEx {

	public static final String TAG = AndroidEx.class.getSimpleName();

	public static void restartApp(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
		ComponentName componentName = intent.getComponent();
		Intent mainIntent = IntentCompat.makeRestartActivityTask(componentName);
		context.startActivity(mainIntent);
		System.exit(0);
	}

	public static String getDeviceId(Context context) {
		String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		return androidId;
	}

	public static String getDeviceIdHashed(Context context) {
		return MD5(getDeviceId(context)).toUpperCase();
	}

	private static String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public static void disableEventBubbling(View... views) {
		for (View view : views) {
			if (view != null) {
				view.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						view.getParent().requestDisallowInterceptTouchEvent(true);
						return false;
					}
				});
			}
		}
	}

	public static boolean hasNullOrEmptyDrawable(ImageView iv) {
		Drawable drawable = iv.getDrawable();
		BitmapDrawable bitmapDrawable = drawable instanceof BitmapDrawable ? (BitmapDrawable) drawable : null;

		return bitmapDrawable == null || bitmapDrawable.getBitmap() == null;
	}

	public static int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

	public static int pxToDp(int px) {
		return (int) (px / Resources.getSystem().getDisplayMetrics().density);
	}

}
