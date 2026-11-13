package com.tpypay.smsgateway;

import android.content.Context;
import android.content.Intent;

/** Single place that flips the ON/OFF switch (used by the button and the notification). */
final class Gateway {
    private Gateway() {}

    static final String ACTION_STATE = "com.tpypay.smsgateway.STATE";

    static void set(Context ctx, boolean on) {
        final Context app = ctx.getApplicationContext();
        Prefs.setOn(app, on);
        Notifier.update(app);
        app.sendBroadcast(new Intent(ACTION_STATE).setPackage(app.getPackageName()));
        if (on) {
            new Thread(() -> ApiClient.flushQueue(app)).start();
        }
    }
}
