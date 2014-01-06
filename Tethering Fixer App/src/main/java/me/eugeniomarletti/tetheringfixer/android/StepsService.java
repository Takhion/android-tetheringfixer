/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Eugenio Marletti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.eugeniomarletti.tetheringfixer.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import me.eugeniomarletti.tetheringfixer.R;
import me.eugeniomarletti.tetheringfixer.Steps;
import me.eugeniomarletti.tetheringfixer.Utils;

public final class StepsService extends Service implements Steps.StepListListener
{
    private static final String TAG = "StepsService";

    public static final int NOTIFICATION_ID = 0;

    private static NotificationManager       notificationManager;
    private static Notification.Builder      notificationBuilder;
    private static Notification.BigTextStyle notificationStyle;

    public static ComponentName start()
    {
        final Context context = Application.getInstance();
        return context.startService(new Intent(context, StepsService.class));
    }

    public static boolean stop()
    {
        final Context context = Application.getInstance();
        return context.stopService(new Intent(context, StepsService.class));
    }

    private static NotificationManager getNotificationManager()
    {
        if (notificationManager == null) notificationManager =
                (NotificationManager)Application.getInstance().getSystemService(NOTIFICATION_SERVICE);
        return notificationManager;
    }

    private static void updateNotification()
    {
        getNotificationManager().notify(NOTIFICATION_ID, notificationStyle.build());
    }

    public static void cancelNotification()
    {
        getNotificationManager().cancel(NOTIFICATION_ID);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        final Application app = Application.getInstance();
        final PendingIntent activity = PendingIntent.getActivity(app, 0,
                                                                 new Intent(app, StepsActivity.class),
                                                                 PendingIntent.FLAG_UPDATE_CURRENT);

        final String info = getString(R.string.app_name);
        notificationBuilder = new Notification.Builder(this)
                .setContentInfo(info)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(activity);
        notificationStyle = new Notification.BigTextStyle(notificationBuilder)
                .setSummaryText("");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (Steps.isSuccess())
        {
            onStepsSuccess(-1);
            return Service.START_NOT_STICKY;
        }
        Steps.addListener(this);
        Steps.startOrRetry();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Steps.removeListener(this);
        Steps.shutdownIfNoListeners();
        notificationManager = null;
        notificationBuilder = null;
        notificationStyle = null;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onStepsStart()
    {
        cancelNotification();
        final String title = getString(R.string.service_executing);
        notificationBuilder
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .setTicker(null)
                .setContentTitle(title);
        notificationStyle.setBigContentTitle(title);
    }

    @Override
    public void onStepsRetry()
    {
    }

    @Override
    public void onAdvanceStep(int itemIndex)
    {
        final String label = itemIndex < 0 || itemIndex >= Steps.size() ? null : Steps.getLabels().get(itemIndex);
        notificationBuilder.setContentText(label);
        notificationStyle.bigText(label);
        updateNotification();
    }

    @Override
    public void onStepError(int itemIndex, String errorText)
    {
        final Application app = Application.getInstance();
        final PendingIntent retry = PendingIntent.getService(app, 0,
                                                             new Intent(app, StepsService.class),
                                                             PendingIntent.FLAG_UPDATE_CURRENT);

        final String title = getString(R.string.service_error);
        errorText = Utils.addEmoji(errorText);
        notificationBuilder
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(false)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(errorText)
                .addAction(R.drawable.ic_retry, getString(R.string.service_retry), retry);
        notificationStyle
                .setBigContentTitle(title)
                .bigText(errorText);
        updateNotification();
        stopSelf();
    }

    @Override
    public void onStepsSuccess(int itemIndex)
    {
        Log.d(TAG, "Finishing...");
        cancelNotification();
        stopSelf();
    }
}
