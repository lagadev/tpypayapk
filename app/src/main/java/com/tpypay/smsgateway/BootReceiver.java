package com.tpypay.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Notifier.update(ctx); // bring the ON/OFF notification back after reboot / app update
    }
}
