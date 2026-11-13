package com.tpypay.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("gw", Context.MODE_PRIVATE);
    }

    static boolean isOn(Context c) { return sp(c).getBoolean("on", false); }
    static void setOn(Context c, boolean v) { sp(c).edit().putBoolean("on", v).apply(); }

    static int sent(Context c) { return sp(c).getInt("sent", 0); }
    static int failed(Context c) { return sp(c).getInt("failed", 0); }
    static String lastStatus(Context c) { return sp(c).getString("last", "—"); }

    static synchronized void recordResult(Context c, boolean ok, String note) {
        SharedPreferences.Editor e = sp(c).edit();
        if (ok) e.putInt("sent", sent(c) + 1); else e.putInt("failed", failed(c) + 1);
        e.putString("last", note);
        e.apply();
    }

    static String queue(Context c) { return sp(c).getString("queue", "[]"); }
    static void setQueue(Context c, String json) { sp(c).edit().putString("queue", json).apply(); }
}
