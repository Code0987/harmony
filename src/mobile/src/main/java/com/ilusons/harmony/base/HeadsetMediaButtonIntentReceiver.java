/*
 * Copyright (C) 2007 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.ilusons.harmony.base;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.KeyEvent;

import com.ilusons.harmony.MainActivity;

/**
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: next track
 * Triple press: previous track
 * Long press: voice search
 */
public class HeadsetMediaButtonIntentReceiver extends WakefulBroadcastReceiver {

    // Logger TAG
    private static final String TAG = HeadsetMediaButtonIntentReceiver.class.getSimpleName();

    private static WakeLock wakeLock = null;

    private static final int MSG_LONGPRESS = 1;
    private static final int MSG_PRESS = 2;

    private static final int LONG_PRESS_DELAY = 1000;
    private static final int PRESS_DELAY = 800;

    private static int clickCounter = 0;
    private static long lastClickTime = 0;
    private static boolean down = false;
    private static boolean launched = false;

    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {

            switch (msg.what) {

                case MSG_LONGPRESS:
                    Log.v(TAG, "Handling longpress timeout, launched " + launched);

                    if (!launched) {
                        final Context context = (Context) msg.obj;

                        MainActivity.openPlaybackUIActivity(context);

                        launched = true;
                    }

                    break;

                case MSG_PRESS:
                    final int clickCount = msg.arg1;
                    final String command;

                    Log.v(TAG, "Handling headset click, count = " + clickCount);

                    switch (clickCount) {
                        case 1:
                            command = MusicService.ACTION_TOGGLE_PLAYBACK;
                            break;
                        case 2:
                            command = MusicService.ACTION_NEXT;
                            break;
                        case 3:
                            command = MusicService.ACTION_PREVIOUS;
                            break;
                        default:
                            command = null;
                            break;
                    }

                    if (command != null) {
                        final Context context = (Context) msg.obj;
                        startService(context, command);
                    }

                    break;
            }

            releaseWakeLockIfHandlerIdle();
        }
    };

    private static void startService(Context context, String action) {
        final Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        startWakefulService(context, intent);
    }

    private static void startService(Context context) {
        final Intent intent = new Intent(context, MusicService.class);
        startWakefulService(context, intent);
    }

    private static void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
        if (wakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);

            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.setReferenceCounted(false);
        }

        Log.v(TAG, "Acquiring wake lock and sending " + msg.what);

        wakeLock.acquire(10000);

        handler.sendMessageDelayed(msg, delay);
    }

    private static void releaseWakeLockIfHandlerIdle() {
        if (handler.hasMessages(MSG_LONGPRESS) || handler.hasMessages(MSG_PRESS)) {
            Log.v(TAG, "Handler still has messages pending, not releasing wake lock");
            return;
        }

        if (wakeLock != null) {
            Log.v(TAG, "Releasing wake lock");

            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive\n" + intent);

        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_BOOT_COMPLETED)) {

            startService(context);

        } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {

            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    startService(context, MusicService.ACTION_PAUSE);

                    Log.d(TAG, "Headset is unplugged");
                    break;
                case 1:
                    startService(context, MusicService.ACTION_PLAY);

                    Log.d(TAG, "Headset is plugged");
                    break;
                default:
                    Log.d(TAG, "I have no idea what the headset state is");
            }

        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {

            startService(context, MusicService.ACTION_PAUSE);

        } else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {

            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;

            Log.d(TAG, "onReceive\n" + event);

            final int k = event.getKeyCode();
            final int a = event.getAction();
            final long t = event.getEventTime();

            String command = null;
            switch (k) {
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MusicService.ACTION_PREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MusicService.ACTION_NEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MusicService.ACTION_PLAY;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MusicService.ACTION_PAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MusicService.ACTION_STOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MusicService.ACTION_TOGGLE_PLAYBACK;
                    break;
            }

            if (command != null) {

                if (a == KeyEvent.ACTION_DOWN) {
                    if (down) {
                        if (MusicService.ACTION_TOGGLE_PLAYBACK.equals(command)
                                || MusicService.ACTION_PLAY.equals(command)) {

                            if (lastClickTime != 0
                                    && t - lastClickTime > LONG_PRESS_DELAY) {

                                acquireWakeLockAndSendMessage(context,
                                        handler.obtainMessage(MSG_LONGPRESS, context),
                                        0);

                            }

                        }
                    } else if (event.getRepeatCount() == 0) {

                        if (k == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (t - lastClickTime >= PRESS_DELAY) {
                                clickCounter = 0;
                            }

                            clickCounter++;

                            Log.v(TAG, "Got press, count = " + clickCounter);

                            handler.removeMessages(MSG_PRESS);

                            Message msg = handler.obtainMessage(MSG_PRESS, clickCounter, 0, context);

                            long delay = clickCounter < 3 ? PRESS_DELAY : 0;
                            if (clickCounter >= 3) {
                                clickCounter = 0;
                            }
                            lastClickTime = t;
                            acquireWakeLockAndSendMessage(context, msg, delay);
                        } else {
                            startService(context, command);
                        }

                        launched = false;
                        down = true;
                    }

                } else {

                    handler.removeMessages(MSG_LONGPRESS);
                    down = false;

                }

                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }

                releaseWakeLockIfHandlerIdle();
            }
        }

    }

}
