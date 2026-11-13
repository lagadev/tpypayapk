package com.tpypay.smsgateway;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts TrxID / amount / sender from bKash and Nagad "money received" SMS. */
final class SmsParser {
    private SmsParser() {}

    static final class Parsed {
        String method;
        String trxId;
        double amount;
        String sender;
        String raw;
        long receivedAt;
    }

    private static final Pattern AMOUNT =
            Pattern.compile("(?:tk\\.?|bdt|৳)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRX =
            Pattern.compile("(?:trx\\s*id|txn\\s*id|transaction\\s*id)\\s*[:\\-]?\\s*([A-Za-z0-9]{6,20})",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(01[3-9]\\d{8})(?!\\d)");

    static Parsed parse(String from, String body, long ts) {
        if (body == null) return null;
        String f = from == null ? "" : from.toLowerCase(Locale.ROOT);
        String b = body.toLowerCase(Locale.ROOT);

        String method = null;
        if (f.contains("bkash")) method = "bkash";
        else if (f.contains("nagad")) method = "nagad";
        else if (b.contains("bkash")) method = "bkash";
        else if (b.contains("nagad")) method = "nagad";
        if (method == null) return null;

        // Incoming money only
        if (!(b.contains("received") || b.contains("cash in"))) return null;

        Matcher mt = TRX.matcher(body);
        if (!mt.find()) return null;
        Matcher ma = AMOUNT.matcher(body);
        if (!ma.find()) return null;

        double amount;
        try {
            amount = Double.parseDouble(ma.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) return null;

        Parsed p = new Parsed();
        p.method = method;
        p.trxId = mt.group(1).toUpperCase(Locale.ROOT);
        p.amount = amount;
        Matcher mp = PHONE.matcher(body);
        p.sender = mp.find() ? mp.group(1) : null;
        p.raw = body.length() > 900 ? body.substring(0, 900) : body;
        p.receivedAt = ts;
        return p;
    }
}
