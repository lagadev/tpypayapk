package com.tpypay.smsgateway;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class NotifActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Notifier.ACTION_TOGGLE.equals(intent.getAction())) return;
        boolean turnOn = !Prefs.isOn(ctx);
        // Can't receive SMS without permission; send the user to the app to grant it.
        if (turnOn && ctx.checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ctx.startActivity(new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        Gateway.set(ctx, turnOn);
    }
}
