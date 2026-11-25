package com.odin.tvpanel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Update check and one-tap update.
 *
 * Check: at most once a day (and when the user asks) the TV asks GitHub for the latest release of
 * this app. The request carries nothing about the TV or the user: a fixed URL and a generic
 * User-Agent ("OdinTVMagic/&lt;version&gt;"). It can be turned off in the panel's settings; then no
 * request is made at all.
 *
 * Update: only when a user taps "Update" in the panel. The APK is downloaded over HTTPS into the
 * app's private storage and installed only if (1) its SHA-256 equals the release's
 * odin-tv-magic.apk.sha256 file, (2) it is this app with a higher version, and (3) it is signed with
 * the same certificate as the installed app. Then it is installed through the app's own ADB
 * connection, exactly like `adb install -r`. Android keeps the app's data, so paired phones stay.
 */
final class Updater {
    private static final String REPO = "odinozlen/odin-tv-magic";
    static final String APK = "odin-tv-magic.apk";
    static final String SHA = APK + ".sha256";
    private static final long DAY = 24 * 3600_000L;
    private static final long MAX_APK = 40_000_000L;
    private static final Pattern TAG = Pattern.compile("v?(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})");
    private static final Pattern HEX64 = Pattern.compile("\\b([0-9a-fA-F]{64})\\b");

    /**
     * Test builds only (scripts/build.sh with ODIN_UPDATE_TEST_BASE): a local server instead of GitHub.
     * Release builds leave it empty and only talk to GitHub over HTTPS.
     */
    private static final String TEST_BASE = BuildInfo.UPDATE_TEST_BASE;
    private static final String API = TEST_BASE.isEmpty()
            ? "https://api.github.com/repos/" + REPO + "/releases/latest" : TEST_BASE + "/latest.json";
    /** Assets may only come from this repository's releases (GitHub then redirects to its CDN). */
    private static final String ASSETS = TEST_BASE.isEmpty()
            ? "https://github.com/" + REPO + "/releases/download/" : TEST_BASE + "/";
    private static final String NOTES = "https://github.com/" + REPO + "/releases/";

    private final Context ctx;
    private final Panel p;
    private final File stateFile;
    private final File apkFile;

    // last known release (persisted)
    private long lastCheck;
    private String latest;
    private String notesUrl;
    private String apkUrl;
    private String shaUrl;
    private long apkSize;

    private long lastAttempt;
    private volatile String state = "idle";   // idle, downloading, verifying, installing, error
    private volatile int percent;
    private volatile String errorKey;

    Updater(Context ctx, Panel p, File dir) {
        this.ctx = ctx;
        this.p = p;
        this.stateFile = new File(dir, "update.json");
        this.apkFile = new File(dir, "update.apk");
        apkFile.delete();  // left over from an update that replaced this process
        try {
            JSONObject o = new JSONObject(new String(AdbClient.readAll(stateFile), "UTF-8"));
            lastCheck = o.optLong("lastCheck");
            latest = o.optString("latest", null);
            notesUrl = o.optString("notes", null);
            apkUrl = o.optString("apk", null);
            shaUrl = o.optString("sha", null);
            apkSize = o.optLong("size");
        } catch (Exception ignored) {
            // never checked
        }
    }

    /** Hourly tick; checks GitHub when the last successful check is a day old (and checks are on). */
    void start() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        ex.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!p.settings.updateCheck()) return;
                long now = System.currentTimeMillis();
                boolean due;
                synchronized (Updater.this) {
                    due = now - lastCheck >= DAY || lastCheck > now + DAY;   // also when the clock jumped back
                }
                if (!due) return;
                try {
                    check();
                } catch (Exception e) {
                    p.log("update check failed: " + e);
                }
            }
        }, 90, 3600, TimeUnit.SECONDS);
    }

    // ---------------- check ----------------

    /** Asks GitHub for the latest release (user's tap). Throws a message key on failure. */
    Panel.Result checkNow() throws Exception {
        if (!p.settings.updateCheck()) return new Panel.Result(409, "update.disabled");
        if (busy()) return new Panel.Result(409, "update.busy");
        boolean recent;
        synchronized (this) {
            recent = System.currentTimeMillis() - lastAttempt < 20_000;
        }
        if (!recent) check();
        return available() ? new Panel.Result(200, "update.found", latest()) : new Panel.Result(200, "update.none", Panel.VERSION);
    }

    /** The network part runs without the lock, so status requests are not held up by a slow GitHub. */
    private void check() throws Exception {
        synchronized (this) {
            lastAttempt = System.currentTimeMillis();
        }
        JSONObject r;
        try {
            r = new JSONObject(new String(get(API, 512 * 1024, null), "UTF-8"));
        } catch (Exception e) {
            throw new IOException("update.net");
        }
        String tag = r.optString("tag_name");
        Matcher m = TAG.matcher(tag);
        if (!m.matches() || r.optBoolean("draft") || r.optBoolean("prerelease")) throw new IOException("update.net");
        String version = m.group(1) + "." + m.group(2) + "." + m.group(3);
        String apk = null, sha = null;
        long size = 0;
        JSONArray assets = r.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String url = a.optString("browser_download_url");
            if (!url.startsWith(ASSETS)) continue;
            if (APK.equals(a.optString("name"))) {
                apk = url;
                size = a.optLong("size");
            } else if (SHA.equals(a.optString("name"))) {
                sha = url;
            }
        }
        String html = r.optString("html_url");
        synchronized (this) {
            latest = version;
            notesUrl = html.startsWith(NOTES) ? html : NOTES + "latest";
            apkUrl = apk;
            shaUrl = sha;
            apkSize = size;
            lastCheck = System.currentTimeMillis();
            save();
            if ("error".equals(state)) {  // a fresh check starts over
                state = "idle";
                errorKey = null;
            }
        }
        if (available()) p.log("update available: " + version);
    }

    private void save() {
        try {
            JSONObject o = new JSONObject().put("lastCheck", lastCheck).put("latest", latest)
                    .put("notes", notesUrl).put("apk", apkUrl).put("sha", shaUrl).put("size", apkSize);
            AdbClient.writeAll(stateFile, o.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    synchronized boolean available() {
        return p.settings.updateCheck() && latest != null && newer(latest, Panel.VERSION);
    }

    synchronized String latest() {
        return latest;
    }

    private boolean busy() {
        return "downloading".equals(state) || "verifying".equals(state) || "installing".equals(state);
    }

    /** True when a is a higher x.y.z version than b. */
    static boolean newer(String a, String b) {
        int[] x = parse(a), y = parse(b);
        if (x == null || y == null) return false;
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) return x[i] > y[i];
        }
        return false;
    }

    private static int[] parse(String v) {
        Matcher m = TAG.matcher(v == null ? "" : v);
        if (!m.matches()) return null;
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }

    /** For GET /api/update and the status. */
    synchronized JSONObject json(String lang) throws Exception {
        boolean on = p.settings.updateCheck();
        JSONObject o = new JSONObject()
                .put("enabled", on)
                .put("current", Panel.VERSION)
                .put("latest", on && latest != null ? latest : JSONObject.NULL)
                .put("available", available())
                .put("notes_url", on && notesUrl != null ? notesUrl : JSONObject.NULL)
                .put("checked", lastCheck > 0 ? lastCheck : JSONObject.NULL)
                .put("state", state)
                .put("percent", percent);
        if ("error".equals(state) && errorKey != null) o.put("error", Strings.t(lang, errorKey));
        return o;
    }

    // ---------------- install ----------------

    /** Starts download → verify → install in the background. Only ever called from a user's tap. */
    synchronized Panel.Result install() throws Exception {
        if (!p.settings.updateCheck()) return new Panel.Result(409, "update.disabled");
        if (busy()) return new Panel.Result(409, "update.busy");
        if (!available()) return new Panel.Result(409, "update.none", Panel.VERSION);
        try {
            p.adb.shell("echo ok");
        } catch (Exception e) {
            return new Panel.Result(409, "update.needs_adb");
        }
        if (apkUrl == null) return new Panel.Result(409, "update.no_apk");
        if (shaUrl == null) return new Panel.Result(409, "update.no_sha");
        final String apk = apkUrl, sha = shaUrl, version = latest;
        final long size = apkSize;
        state = "downloading";
        percent = 0;
        errorKey = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.log("update to " + version + ": downloading");
                    String expected = expectedSha(sha);
                    String got = download(apk, size);
                    state = "verifying";
                    if (!got.equals(expected)) throw new IOException("update.verify");
                    verifyPackage();
                    state = "installing";
                    p.log("update to " + version + ": verified, installing");
                    // Android stops this process when the new version is in place; BootReceiver
                    // (MY_PACKAGE_REPLACED) starts the panel again.
                    adbInstall();
                    state = "idle";
                } catch (Exception e) {
                    errorKey = Strings.has(e.getMessage()) ? e.getMessage() : "update.failed";
                    state = "error";
                    p.log("update to " + version + " failed: " + e);
                } finally {
                    apkFile.delete();
                }
            }
        }, "update-install").start();
        return new Panel.Result(200, "update.started", version);
    }

    private String expectedSha(String url) throws IOException {
        String text;
        try {
            text = new String(get(url, 4096, null), "UTF-8");
        } catch (Exception e) {
            throw new IOException("update.net");
        }
        Matcher m = HEX64.matcher(text);
        if (!m.find()) throw new IOException("update.verify");
        return m.group(1).toLowerCase(Locale.ROOT);
    }

    private String download(String url, long size) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            get(url, MAX_APK, sha, size);
            return Auth.hex(sha.digest());
        } catch (IOException e) {
            throw Strings.has(e.getMessage()) ? e : new IOException("update.net");
        } catch (Exception e) {
            throw new IOException("update.net");
        }
    }

    /** The downloaded file must be this app, newer, and signed with the same certificate. */
    private void verifyPackage() throws IOException {
        PackageManager pm = ctx.getPackageManager();
        String path = apkFile.getAbsolutePath();
        PackageInfo mine, theirs;
        try {
            mine = pm.getPackageInfo(ctx.getPackageName(), signingFlags());
        } catch (PackageManager.NameNotFoundException e) {
            throw new IOException("update.failed");
        }
        theirs = pm.getPackageArchiveInfo(path, signingFlags());
        if (theirs == null || !ctx.getPackageName().equals(theirs.packageName)) throw new IOException("update.signature");
        if (versionCode(theirs) <= versionCode(mine)) throw new IOException("update.older");
        // Some Android versions (seen on 13) leave signingInfo empty for an archive; then both sides
        // are compared by their legacy signature list instead, so like is compared with like.
        boolean modern = hasSigningInfo(mine) && hasSigningInfo(theirs);
        Set<String> a = signers(mine, modern), b = signers(theirs, modern);
        if (a.isEmpty() || !a.equals(b)) throw new IOException("update.signature");
    }

    @SuppressWarnings("deprecation")
    private static int signingFlags() {
        return Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
    }

    @SuppressWarnings("deprecation")
    private static long versionCode(PackageInfo i) {
        return Build.VERSION.SDK_INT >= 28 ? i.getLongVersionCode() : i.versionCode;
    }

    private static boolean hasSigningInfo(PackageInfo i) {
        return Build.VERSION.SDK_INT >= 28 && i.signingInfo != null;
    }

    /** SHA-256 of each signing certificate. */
    @SuppressWarnings("deprecation")
    private static Set<String> signers(PackageInfo i, boolean modern) {
        Signature[] s;
        if (modern) {
            if (i.signingInfo.hasMultipleSigners()) {
                s = i.signingInfo.getApkContentsSigners();
            } else {
                // Key rotation history, oldest first: the current certificate is the last one.
                Signature[] h = i.signingInfo.getSigningCertificateHistory();
                s = h == null || h.length == 0 ? null : new Signature[]{h[h.length - 1]};
            }
        } else {
            s = i.signatures;
        }
        Set<String> out = new HashSet<>();
        if (s == null) return out;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature x : s) out.add(Auth.hex(md.digest(x.toByteArray())));
        } catch (Exception e) {
            return new HashSet<>();
        }
        return out;
    }

    private void adbInstall() throws Exception {
        AdbClient.Stream s = p.adb.openStream("exec:cmd package install -r -S " + apkFile.length());
        try {
            FileInputStream in = new FileInputStream(apkFile);
            try {
                byte[] buf = new byte[32 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) s.write(n == buf.length ? buf : Arrays.copyOf(buf, n));
            } finally {
                in.close();
            }
            // Normally this process is stopped before the answer arrives.
            String result = s.awaitOutput(180_000);
            if (!result.contains("Success")) {
                p.log("update install: " + result.trim());
                throw new IOException(result.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ? "update.signature" : "update.failed");
            }
        } finally {
            s.close();
        }
    }

    // ---------------- HTTP ----------------

    private byte[] get(String url, long max, MessageDigest sha) throws Exception {
        return get(url, max, sha, 0);
    }

    /** GET with a generic User-Agent; HTTPS only (except test builds). With sha set, streams into apkFile. */
    private byte[] get(String url, long max, MessageDigest sha, long expectedSize) throws Exception {
        if (!url.startsWith("https://") && TEST_BASE.isEmpty()) throw new IOException("update.net");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "OdinTVMagic/" + Panel.VERSION);
        c.setRequestProperty("Accept", url.equals(API) ? "application/vnd.github+json" : "*/*");
        try {
            if (c.getResponseCode() != 200) throw new IOException("http " + c.getResponseCode());
            if (TEST_BASE.isEmpty() && !"https".equals(c.getURL().getProtocol())) throw new IOException("update.net");
            long total = expectedSize > 0 ? expectedSize : c.getContentLengthLong();
            InputStream in = c.getInputStream();
            ByteArrayOutputStream mem = sha == null ? new ByteArrayOutputStream() : null;
            FileOutputStream fo = sha == null ? null : new FileOutputStream(apkFile);
            try {
                byte[] buf = new byte[64 * 1024];
                long n = 0;
                int r;
                while ((r = in.read(buf)) > 0) {
                    n += r;
                    if (n > max) throw new IOException(sha == null ? "update.net" : "update.verify");
                    if (sha == null) {
                        mem.write(buf, 0, r);
                    } else {
                        sha.update(buf, 0, r);
                        fo.write(buf, 0, r);
                        if (total > 0) percent = (int) Math.min(99, 100 * n / total);
                    }
                }
            } finally {
                if (fo != null) fo.close();
                in.close();
            }
            return mem == null ? null : mem.toByteArray();
        } finally {
            c.disconnect();
        }
    }
}
