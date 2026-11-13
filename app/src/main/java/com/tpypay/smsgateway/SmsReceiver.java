package com.tpypay.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        if (!Prefs.isOn(ctx)) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        String from = parts[0].getOriginatingAddress();
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : parts) sb.append(m.getMessageBody());

        final SmsParser.Parsed p = SmsParser.parse(from, sb.toString(), System.currentTimeMillis());
        if (p == null) return; // not a payment SMS — ignore

        final PendingResult pr = goAsync();
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                ApiClient.submit(app, p);
                Notifier.update(app);
                app.sendBroadcast(new Intent(Gateway.ACTION_STATE).setPackage(app.getPackageName()));
            } finally {
                pr.finish();
            }
        }).start();
    }
}
