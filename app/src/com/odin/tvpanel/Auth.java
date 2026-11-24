package com.odin.tvpanel;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Who may use the panel.
 *
 * Pairing: the TV screen shows a 6-digit one-time code (also inside a QR code). It is
 * valid for 10 minutes while that screen is open, and burns after 5 wrong attempts,
 * so it cannot be brute-forced from the network. A paired phone receives a random
 * 256-bit key.
 *
 * Requests: the key never travels again. Every API call carries
 *   X-Auth: id.timestamp.nonce.signature
 * where signature = HMAC-SHA256(key, method \n path \n timestamp \n nonce \n SHA-256(body)).
 * Timestamps must be within 2 minutes of the TV clock and nonces are single-use,
 * so a sniffed request cannot be replayed.
 */
final class Auth {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long CODE_TTL_MS = 10 * 60_000L;
    private static final int CODE_MAX_ATTEMPTS = 5;
    private static final long SKEW_MS = 120_000L;
    private static final int MAX_DEVICES = 20;

    private final File file;
    private final SecureRandom rnd = new SecureRandom();
    private JSONArray devices = new JSONArray();

    private String code;
    private long codeExpires;
    private int codeAttempts;
    private long lastPairAttempt;

    private final LinkedHashMap<String, Long> nonces = new LinkedHashMap<>();

    Auth(File dir) {
        file = new File(dir, "devices.json");
        try {
            devices = new JSONArray(new String(AdbClient.readAll(file), UTF8));
        } catch (Exception ignored) {
            // no phones paired yet
        }
    }

    // ---------------- pairing ----------------

    /** Called when the TV pairing screen becomes visible. */
    synchronized String newPairingCode() {
        code = String.format(Locale.ROOT, "%06d", rnd.nextInt(1_000_000));
        codeExpires = System.currentTimeMillis() + CODE_TTL_MS;
        codeAttempts = 0;
        burned = false;
        return code;
    }

    /** Called when the TV pairing screen goes away. */
    synchronized void clearPairingCode() {
        code = null;
        burned = false;
    }

    static final int CODE_ACTIVE = 0, CODE_EXPIRED = 1, CODE_BURNED = 2;
    private boolean burned;

    /**
     * The TV screen renews an expired code by itself, but a code burned by wrong guesses is only
     * renewed when the screen is opened again. Otherwise an attacker could burn codes in a loop.
     */
    synchronized int codeState() {
        if (burned) return CODE_BURNED;
        if (code == null || System.currentTimeMillis() > codeExpires) return CODE_EXPIRED;
        return CODE_ACTIVE;
    }

    static final class Pairing {
        String id, key;
    }

    /** Returns the new credentials, or null for a wrong/expired code. Throttled to one try per second. */
    synchronized Pairing pair(String attempt, String name) throws Exception {
        long now = System.currentTimeMillis();
        if (now - lastPairAttempt < 1000) return null;
        lastPairAttempt = now;
        if (code == null || now > codeExpires || attempt == null) return null;
        if (!MessageDigest.isEqual(code.getBytes(UTF8), attempt.trim().getBytes(UTF8))) {
            if (++codeAttempts >= CODE_MAX_ATTEMPTS) {
                code = null;
                burned = true;
            }
            return null;
        }
        code = null;  // single use

        byte[] key = new byte[32];
        rnd.nextBytes(key);
        byte[] idb = new byte[8];
        rnd.nextBytes(idb);
        Pairing p = new Pairing();
        p.id = hex(idb);
        p.key = Base64.encodeToString(key, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);

        String clean = name == null ? "" : name.replaceAll("[^\\p{L}\\p{N} ._()-]", "").trim();
        if (clean.length() > 40) clean = clean.substring(0, 40);
        if (clean.isEmpty()) clean = "Phone";
        while (devices.length() >= MAX_DEVICES) devices.remove(0);
        devices.put(new JSONObject().put("id", p.id).put("name", clean).put("key", p.key).put("created", now));
        save();
        return p;
    }

    // ---------------- request verification ----------------

    static final int OK = 0, MISSING = 1, BAD = 2, CLOCK = 3;

    /** Returns OK, or why the request was rejected. On OK, deviceId[0] is set. */
    synchronized int verify(String method, String path, String header, byte[] body, String[] deviceId) {
        if (header == null || header.isEmpty()) return MISSING;
        String[] p = header.split("\\.");
        if (p.length != 4) return BAD;
        JSONObject dev = find(p[0]);
        if (dev == null) return BAD;
        long ts;
        try {
            ts = Long.parseLong(p[1]);
        } catch (NumberFormatException e) {
            return BAD;
        }
        String nonce = p[2];
        if (nonce.length() < 8 || nonce.length() > 64) return BAD;
        long now = System.currentTimeMillis();
        try {
            byte[] key = Base64.decode(dev.getString("key"), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String msg = method + "\n" + path + "\n" + p[1] + "\n" + nonce + "\n" + hex(MessageDigest.getInstance("SHA-256").digest(body));
            byte[] expected = hex(mac.doFinal(msg.getBytes(UTF8))).getBytes(UTF8);
            if (!MessageDigest.isEqual(expected, p[3].toLowerCase(Locale.ROOT).getBytes(UTF8))) return BAD;
        } catch (Exception e) {
            return BAD;
        }
        // Signature is valid; now freshness. Checked after the signature so an attacker cannot probe the clock.
        if (Math.abs(now - ts) > SKEW_MS) return CLOCK;
        expireNonces(now);
        String nk = p[0] + ":" + nonce;
        if (nonces.containsKey(nk)) return BAD;  // replay
        nonces.put(nk, now);
        deviceId[0] = p[0];
        return OK;
    }

    private void expireNonces(long now) {
        Iterator<Map.Entry<String, Long>> it = nonces.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now - e.getValue() > 2 * SKEW_MS || nonces.size() > 20_000) it.remove();
            else break;
        }
    }

    // ---------------- device list ----------------

    synchronized JSONArray list(String currentId) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < devices.length(); i++) {
            JSONObject d = devices.getJSONObject(i);
            out.put(new JSONObject().put("id", d.getString("id")).put("name", d.optString("name"))
                    .put("created", d.optLong("created")).put("current", d.getString("id").equals(currentId)));
        }
        return out;
    }

    synchronized int count() {
        return devices.length();
    }

    synchronized boolean revoke(String id) {
        for (int i = 0; i < devices.length(); i++) {
            if (devices.optJSONObject(i) != null && devices.optJSONObject(i).optString("id").equals(id)) {
                devices.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    synchronized void revokeAll() {
        devices = new JSONArray();
        nonces.clear();
        save();
    }

    private JSONObject find(String id) {
        for (int i = 0; i < devices.length(); i++) {
            JSONObject d = devices.optJSONObject(i);
            if (d != null && d.optString("id").equals(id)) return d;
        }
        return null;
    }

    private void save() {
        try {
            AdbClient.writeAll(file, devices.toString().getBytes(UTF8));
        } catch (Exception ignored) {
        }
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte x : b) s.append(String.format(Locale.ROOT, "%02x", x));
        return s.toString();
    }
}
