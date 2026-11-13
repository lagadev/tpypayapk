package com.tpypay.smsgateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

final class Notifier {
    private Notifier() {}

    private static final String CHANNEL = "gw_status";
    private static final int ID = 1001;
    static final String ACTION_TOGGLE = "com.tpypay.smsgateway.TOGGLE";

    static void update(Context ctx) {
        Context app = ctx.getApplicationContext();
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Gateway status", NotificationManager.IMPORTANCE_LOW));

        boolean on = Prefs.isOn(app);

        Intent open = new Intent(app, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(app, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent toggle = new Intent(app, NotifActionReceiver.class).setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getBroadcast(app, 1, toggle,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action action = new Notification.Action.Builder(
                null, on ? "Turn OFF" : "Turn ON", togglePi).build();

        Notification n = new Notification.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(on ? "SMS Gateway: ON" : "SMS Gateway: OFF")
                .setContentText(on
                        ? "Forwarding bKash/Nagad SMS · sent " + Prefs.sent(app)
                        : "Paused — tap Turn ON to start forwarding")
                .setContentIntent(openPi)
                .addAction(action)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build();

        nm.notify(ID, n);
    }
}
