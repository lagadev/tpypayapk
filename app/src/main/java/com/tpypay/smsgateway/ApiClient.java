package com.tpypay.smsgateway;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    private ApiClient() {}

    private static final Object LOCK = new Object();
    private static final int MAX_QUEUE = 50;

    private static final int OK = 0;
    private static final int DROP = 1;   // server rejected it for good (4xx)
    private static final int RETRY = 2;  // network / 5xx / 429

    private static String endpoint() {
        String base = BuildConfig.API_URL.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/api/sms/ingest";
    }

    private static JSONObject toJson(SmsParser.Parsed p) throws Exception {
        JSONObject o = new JSONObject();
        o.put("trxId", p.trxId);
        o.put("amount", p.amount);
        o.put("method", p.method);
        if (p.sender != null) o.put("senderNumber", p.sender);
        o.put("receivedAt", p.receivedAt);
        o.put("rawSms", p.raw);
        return o;
    }

    private static int post(JSONObject body) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(endpoint()).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(4000);
            c.setReadTimeout(5000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + BuildConfig.API_KEY);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) {
                os.write(bytes);
            }
            int code = c.getResponseCode();
            if (code >= 200 && code < 300) return OK;
            if (code == 429 || code >= 500) return RETRY;
            return DROP;
        } catch (Exception e) {
            return RETRY;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Send one parsed SMS. On temporary failure it is kept in a small retry queue. */
    static void submit(Context ctx, SmsParser.Parsed p) {
        try {
            JSONObject body = toJson(p);
            int r = post(body);
            if (r == OK) {
                Prefs.recordResult(ctx, true, p.method + " " + p.trxId + " ✓");
                flushQueue(ctx);
            } else if (r == DROP) {
                Prefs.recordResult(ctx, false, p.method + " " + p.trxId + " rejected");
            } else {
                enqueue(ctx, body);
                Prefs.recordResult(ctx, false, p.method + " " + p.trxId + " queued");
            }
        } catch (Exception ignored) {
        }
    }

    private static void enqueue(Context ctx, JSONObject body) {
        synchronized (LOCK) {
            try {
                JSONArray arr = new JSONArray(Prefs.queue(ctx));
                arr.put(body);
                JSONArray trimmed = new JSONArray();
                for (int i = Math.max(0, arr.length() - MAX_QUEUE); i < arr.length(); i++) {
                    trimmed.put(arr.get(i));
                }
                Prefs.setQueue(ctx, trimmed.toString());
            } catch (Exception ignored) {
            }
        }
    }

    /** Retry everything that failed earlier. Safe to call from a background thread only. */
    static void flushQueue(Context ctx) {
        synchronized (LOCK) {
            try {
                JSONArray arr = new JSONArray(Prefs.queue(ctx));
                if (arr.length() == 0) return;
                JSONArray left = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    int r = post(o);
                    if (r == OK) {
                        Prefs.recordResult(ctx, true, o.optString("method") + " " + o.optString("trxId") + " ✓ (retry)");
                    } else if (r == RETRY) {
                        left.put(o);
                    }
                }
                Prefs.setQueue(ctx, left.toString());
            } catch (Exception ignored) {
            }
        }
    }
}
