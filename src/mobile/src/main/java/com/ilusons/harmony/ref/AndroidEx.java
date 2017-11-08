package com.ilusons.harmony.ref;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.IntentCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


public class AndroidEx {

	public static final String TAG = AndroidEx.class.getSimpleName();

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

	/**
	 * Using reflection to override default typeface
	 * NOTICE: DO NOT FORGET TO SET TYPEFACE FOR APP THEME AS DEFAULT TYPEFACE WHICH WILL BE OVERRIDDEN
	 *
	 * @param context                    to work with assets
	 * @param defaultFontNameToOverride  for example "monospace"
	 * @param customFontFileNameInAssets file name of the font from assets
	 */
	public static void overrideFont(Context context, String defaultFontNameToOverride, String customFontFileNameInAssets) {

		final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), customFontFileNameInAssets);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Map<String, Typeface> newMap = new HashMap<String, Typeface>();
			newMap.put("serif", customFontTypeface);
			try {
				final Field staticField = Typeface.class
						.getDeclaredField("sSystemFontMap");
				staticField.setAccessible(true);
				staticField.set(null, newMap);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			try {
				final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
				defaultFontTypefaceField.setAccessible(true);
				defaultFontTypefaceField.set(null, customFontTypeface);
			} catch (Exception e) {
				Log.e(TAG, "Can not set custom font " + customFontFileNameInAssets + " instead of " + defaultFontNameToOverride);
			}
		}
	}

	public static void restartApp(Context c) {
		try {
			//check if the context is given
			if (c != null) {
				//fetch the packagemanager so we can get the default launch activity
				// (you can replace this intent with any other activity if you want
				PackageManager pm = c.getPackageManager();
				//check if we got the PackageManager
				if (pm != null) {
					//create the intent with the default start activity for your application
					Intent mStartActivity = pm.getLaunchIntentForPackage(
							c.getPackageName()
					);
					if (mStartActivity != null) {
						mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						//create a pending intent so the application is restarted after System.exit(0) was called.
						// We use an AlarmManager to call this intent in 100ms
						int mPendingIntentId = 223344;
						PendingIntent mPendingIntent = PendingIntent
								.getActivity(c, mPendingIntentId, mStartActivity,
										PendingIntent.FLAG_CANCEL_CURRENT);
						AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
						//kill the application
						System.exit(0);
					} else {
						Log.e(TAG, "Was not able to restart application, mStartActivity null");
					}
				} else {
					Log.e(TAG, "Was not able to restart application, PM null");
				}
			} else {
				Log.e(TAG, "Was not able to restart application, Context null");
			}
		} catch (Exception ex) {
			Log.e(TAG, "Was not able to restart application");
		}
	}

}
