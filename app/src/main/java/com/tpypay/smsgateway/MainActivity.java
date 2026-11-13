package com.tpypay.smsgateway;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 7;

    private static final int BG = Color.parseColor("#F1F5F9");
    private static final int INK = Color.parseColor("#0F172A");
    private static final int MUTED = Color.parseColor("#64748B");
    private static final int GREEN = Color.parseColor("#16A34A");
    private static final int GREEN_SOFT = Color.parseColor("#DCFCE7");
    private static final int GREY = Color.parseColor("#94A3B8");
    private static final int GREY_SOFT = Color.parseColor("#E2E8F0");
    private static final int RED = Color.parseColor("#DC2626");
    private static final int AMBER = Color.parseColor("#D97706");

    private Button toggle;
    private View ring;
    private TextView pill, title, subtitle, sentNum, failedNum, queuedNum, lastText, warn;

    private final BroadcastReceiver stateRx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refresh(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
    }

    // ---------- small UI helpers ----------

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(shape(Color.WHITE, 20));
        c.setElevation(dp(2));
        return c;
    }

    private LinearLayout.LayoutParams lpMatch(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        return lp;
    }

    // ---------- layout ----------

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        // ----- header: logo + name + live pill -----
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setElevation(dp(3));
        header.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, 0, 0);
        names.addView(text("TPY Pay", 20, INK, true));
        names.addView(text("SMS Gateway", 13, MUTED, false));
        header.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        pill = text("", 12, Color.WHITE, true);
        pill.setPadding(dp(12), dp(6), dp(12), dp(6));
        header.addView(pill);
        root.addView(header);

        // ----- main toggle card -----
        LinearLayout main = card();
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setPadding(dp(20), dp(28), dp(20), dp(28));

        FrameLayout stack = new FrameLayout(this);
        ring = new View(this);
        stack.addView(ring, new FrameLayout.LayoutParams(dp(210), dp(210), Gravity.CENTER));

        toggle = new Button(this);
        toggle.setAllCaps(false);
        toggle.setTextSize(30);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        toggle.setTextColor(Color.WHITE);
        toggle.setStateListAnimator(null);
        toggle.setElevation(dp(6));
        toggle.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(
                    () -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
            onToggle();
        });
        stack.addView(toggle, new FrameLayout.LayoutParams(dp(164), dp(164), Gravity.CENTER));
        main.addView(stack, new LinearLayout.LayoutParams(dp(210), dp(210)));

        title = text("", 18, INK, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(18);
        main.addView(title, tlp);

        subtitle = text("", 13, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        main.addView(subtitle, lpMatch(4));
        root.addView(main, lpMatch(20));

        // ----- stat cards -----
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        sentNum = text("0", 22, GREEN, true);
        failedNum = text("0", 22, RED, true);
        queuedNum = text("0", 22, AMBER, true);
        stats.addView(statCard(sentNum, "Sent"), statLp(false));
        stats.addView(statCard(failedNum, "Failed"), statLp(true));
        stats.addView(statCard(queuedNum, "Queued"), statLp(true));
        root.addView(stats, lpMatch(14));

        // ----- last activity -----
        LinearLayout last = card();
        last.setPadding(dp(18), dp(14), dp(18), dp(14));
        last.addView(text("LAST ACTIVITY", 11, MUTED, true));
        lastText = text("—", 15, INK, false);
        lastText.setPadding(0, dp(4), 0, 0);
        last.addView(lastText);
        root.addView(last, lpMatch(14));

        // ----- warning -----
        warn = text("", 13, RED, false);
        warn.setGravity(Gravity.CENTER);
        warn.setPadding(dp(8), dp(14), dp(8), 0);
        root.addView(warn);

        // ----- footer -----
        TextView foot = text("Forwarding to " + hostOnly(BuildConfig.API_URL), 11, GREY, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(16), 0, 0);
        root.addView(foot);

        setContentView(scroll);
    }

    private LinearLayout statCard(TextView num, String label) {
        LinearLayout c = card();
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(8), dp(14), dp(8), dp(14));
        num.setGravity(Gravity.CENTER);
        c.addView(num);
        TextView l = text(label, 12, MUTED, false);
        l.setGravity(Gravity.CENTER);
        c.addView(l);
        return c;
    }

    private LinearLayout.LayoutParams statLp(boolean margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (margin) lp.leftMargin = dp(10);
        return lp;
    }

    private static String hostOnly(String url) {
        String s = url.replaceFirst("^https?://", "");
        int i = s.indexOf('/');
        return i > 0 ? s.substring(0, i) : s;
    }

    // ---------- behaviour ----------

    private boolean hasSms() {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermissions() {
        List<String> need = new ArrayList<>();
        if (!hasSms()) need.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), REQ_PERMS);
    }

    private void onToggle() {
        if (Prefs.isOn(this)) {
            Gateway.set(this, false);
        } else if (!hasSms()) {
            askPermissions();
        } else {
            Gateway.set(this, true);
        }
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasSms() && !Prefs.isOn(this)) Gateway.set(this, true);
        Notifier.update(this);
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(Gateway.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateRx, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateRx, f);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(stateRx); } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 33) askPermissions();
        Notifier.update(this);
        refresh();
        final Context app = getApplicationContext();
        new Thread(() -> {
            ApiClient.flushQueue(app);
            runOnUiThread(this::refresh);
        }).start();
    }

    private int queuedCount() {
        try {
            return new JSONArray(Prefs.queue(this)).length();
        } catch (Exception e) {
            return 0;
        }
    }

    private void refresh() {
        boolean on = Prefs.isOn(this);

        toggle.setBackground(oval(on ? GREEN : GREY));
        toggle.setText(on ? "ON" : "OFF");
        ring.setBackground(oval(on ? GREEN_SOFT : GREY_SOFT));

        pill.setText(on ? "● LIVE" : "● PAUSED");
        pill.setBackground(shape(on ? GREEN : GREY, 50));

        title.setText(on ? "Gateway is running" : "Gateway is paused");
        subtitle.setText(on
                ? "bKash & Nagad payment SMS are being forwarded"
                : "Tap the button to start forwarding SMS");

        sentNum.setText(String.valueOf(Prefs.sent(this)));
        failedNum.setText(String.valueOf(Prefs.failed(this)));
        queuedNum.setText(String.valueOf(queuedCount()));
        lastText.setText(Prefs.lastStatus(this));

        if (BuildConfig.API_KEY.isEmpty()) {
            warn.setText("API key is missing in this build. Set the API_KEY secret in GitHub and rebuild.");
        } else if (!hasSms()) {
            warn.setText("SMS permission is needed. Tap the button to allow it.");
        } else {
            warn.setText("");
        }
    }
}
