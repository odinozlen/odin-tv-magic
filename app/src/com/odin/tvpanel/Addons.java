package com.odin.tvpanel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;

import javax.net.ssl.HttpsURLConnection;

/**
 * Optional extras installed on request. Currently: an AirPlay receiver for TVs that lack one
 * (jqssun/android-airplay-server, GPL-3.0, an open-source AirPlay 2 receiver built on UxPlay).
 *
 * The APK is downloaded from F-Droid over HTTPS and must match a pinned size and SHA-256
 * (the developer's own signed build; the same file is published on GitHub). It is then
 * installed through ADB, exactly like `adb install`.
 */
final class Addons {
    static final String AIRPLAY = "io.github.jqssun.airplay";
    private static final String AIRPLAY_URL = "https://f-droid.org/repo/io.github.jqssun.airplay_36.apk";
    private static final String AIRPLAY_SHA256 = "b8c7c595ed93f91562feedce92c27800f08f9c84df424f6ac2045e82324cf2a4";
    private static final long AIRPLAY_SIZE = 27_335_496L;

    private final Context ctx;
    private final Panel p;
    private volatile String state = "idle";   // idle, downloading, installing, done, error
    private volatile int percent;
    private volatile String errorKey;

    Addons(Context ctx, Panel p) {
        this.ctx = ctx;
        this.p = p;
    }

    private boolean installed(String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private volatile Boolean builtIn;
    private volatile long builtInAt;

    /** Some TVs (many Sony, LG, Samsung, TCL…) have AirPlay built in: then there is nothing to add. */
    private boolean builtInAirPlay() {
        if (builtIn != null && SystemClock.elapsedRealtime() - builtInAt < 10 * 60_000L) return builtIn;
        boolean found;
        try {
            new MdnsFinder(ctx, 2500).find("_airplay._tcp");
            found = true;
        } catch (Exception e) {
            found = false;
        }
        builtIn = found;
        builtInAt = SystemClock.elapsedRealtime();
        return found;
    }

    JSONObject json() throws Exception {
        boolean ours = installed(AIRPLAY);
        return new JSONObject().put("airplay", new JSONObject()
                .put("installed", ours)
                .put("builtIn", !ours && builtInAirPlay())
                .put("state", state).put("percent", percent)
                .put("sizeMb", AIRPLAY_SIZE / 1_000_000));
    }

    String errorKey() {
        return errorKey;
    }

    synchronized Panel.Result installAirPlay() {
        if (installed(AIRPLAY)) return new Panel.Result(200, "addon.installed");
        if ("downloading".equals(state) || "installing".equals(state)) return new Panel.Result(409, "addon.busy");
        state = "downloading";
        percent = 0;
        errorKey = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File apk = new File(ctx.getCacheDir(), "airplay.apk");
                try {
                    download(apk);
                    state = "installing";
                    install(apk);
                    // The receiver service starts with its app; open it once, then go back home.
                    p.adb.shell("monkey -p " + AIRPLAY + " -c android.intent.category.LEANBACK_LAUNCHER 1");
                    SystemClock.sleep(4000);
                    p.adb.shell("input keyevent KEYCODE_HOME");
                    state = "done";
                    p.log("AirPlay receiver installed");
                } catch (Exception e) {
                    errorKey = Strings.has(e.getMessage()) ? e.getMessage() : "addon.failed";
                    state = "error";
                    p.log("AirPlay install failed: " + e);
                } finally {
                    apk.delete();
                }
            }
        }, "addon-install").start();
        return new Panel.Result(200, "addon.started");
    }

    private void download(File out) throws Exception {
        HttpsURLConnection c = (HttpsURLConnection) new URL(AIRPLAY_URL).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        long total = 0;
        InputStream in = c.getInputStream();
        FileOutputStream fo = new FileOutputStream(out);
        try {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > AIRPLAY_SIZE) throw new IOException("addon.verify");
                sha.update(buf, 0, n);
                fo.write(buf, 0, n);
                percent = (int) (100 * total / AIRPLAY_SIZE);
            }
        } finally {
            fo.close();
            in.close();
            c.disconnect();
        }
        if (total != AIRPLAY_SIZE || !Auth.hex(sha.digest()).equals(AIRPLAY_SHA256)) throw new IOException("addon.verify");
    }

    private void install(File apk) throws Exception {
        AdbClient.Stream s = p.adb.openStream("exec:cmd package install -S " + apk.length());
        try {
            FileInputStream in = new FileInputStream(apk);
            try {
                byte[] buf = new byte[32 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    byte[] chunk = n == buf.length ? buf : java.util.Arrays.copyOf(buf, n);
                    s.write(chunk);
                }
            } finally {
                in.close();
            }
            String result = s.awaitOutput(120_000);
            if (!result.contains("Success")) {
                throw new IOException(result.contains("NO_MATCHING_ABIS") ? "addon.abi" : "addon.failed");
            }
        } finally {
            s.close();
        }
    }
}
