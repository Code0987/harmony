package com.ilusons.harmony.base;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ilusons.harmony.BuildConfig;
import com.ilusons.harmony.R;

import org.w3c.dom.Text;

public class FCMService extends FirebaseMessagingService {

	private static final String TAG = FCMService.class.getSimpleName();

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		Log.d(TAG, "onMessageReceived\n" + remoteMessage);

		try {
			String title = null;
			String content = null;
			String link = null;
			String type = null;

			// Check if message contains a notification payload.
			if (remoteMessage.getNotification() != null) {
				title = remoteMessage.getNotification().getTitle();
				content = remoteMessage.getNotification().getBody();
			}

			// Check if message contains a data payload.
			if (remoteMessage.getData().size() > 0) {
				if (TextUtils.isEmpty(title))
					title = remoteMessage.getData().get("title");
				if (TextUtils.isEmpty(content))
					content = remoteMessage.getData().get("content");
				link = remoteMessage.getData().get("link");
				type = remoteMessage.getData().get("type");
			}

			if (!BuildConfig.DEBUG && title.trim().equalsIgnoreCase("test"))
				return;

			if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
				if (!TextUtils.isEmpty(type) && type.startsWith("alert")) {
					showDialog(getString(R.string.app_name) + ": " + title, content, link);
				} else {
					sendNotification(getString(R.string.app_name) + ": " + title, content, link);
				}
			}

		} catch (Exception e) {
			Log.w(TAG, e);
		}

	}

	private void sendNotification(String title, String content, String link) {
		PendingIntent pendingIntent = null;
		if (!TextUtils.isEmpty(link)) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(link));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		}

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_announcement)
				.setContentTitle(title)
				.setContentText(content)
				.setAutoCancel(true)
				.setColor(getColor(R.color.accent))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				.setStyle(new NotificationCompat.BigTextStyle()
						.setBigContentTitle(title)
						.setSummaryText(content)
						.bigText(content));

		if (pendingIntent != null)
			notificationBuilder.setContentIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(0, notificationBuilder.build());
	}

	private void showDialog(String title, String content, final String link) {
		try {
			final AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_AlertDialogStyle))
					.setTitle(title)
					.setMessage(content)
					.setCancelable(true)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (!TextUtils.isEmpty(link)) {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(Uri.parse(link));
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
							}

							dialogInterface.dismiss();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
						}
					})
					.create();
			alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			alertDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
