package com.ilusons.harmony.base;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ilusons.harmony.R;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = FCMService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived\n" + remoteMessage);

        try {
            String title = null;
            String content = null;
            String link = null;

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
            }

            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content) && !title.trim().toLowerCase().equals("test"))
                sendNotification(getString(R.string.app_name) + ": " + title, content, link);

        } catch (Exception e) {
            Log.w(TAG, e);
        }

    }

    private void sendNotification(String title, String content, String link) {
        PendingIntent pendingIntent = null;
        if (TextUtils.isEmpty(link)) {
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
                .setPriority(android.support.v7.app.NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        if (pendingIntent != null)
            notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

}
