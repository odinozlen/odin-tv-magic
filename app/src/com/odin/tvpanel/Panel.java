package com.odin.tvpanel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything the panel can do. Requests are mapped to fixed commands here;
 * nothing from the network is ever passed to the shell unchecked.
 */
final class Panel {
    static final String VERSION = BuildInfo.VERSION;

    // Philips (TP Vision) Android TVs leak threads in this logger until apps start failing (Netflix stuck at 86%).
    static final String FRAMEWORKLOGGER = "org.droidtv.tv.frameworklogger";
    static final int FL_THREAD_LIMIT = 500;
    private static final long WATCHDOG_EVERY_MIN = 30;
    private static final String AIRPLAY = "io.github.jqssun.airplay";

    final Context ctx;
    final AdbClient adb;
    final JointSpace js;
    final Auth auth;
    final Prefs settings;
    final Apps apps;
    final Sources sources;
    final Launchers launchers;
    final Debloat debloat;
    final Addons addons;
    final Picture picture;
    final Surprise surprise;
    final boolean philips;
    private final File log;
    private volatile String wdTs = null;
    private volatile String wdKey = "wd.never";
    private volatile Object[] wdArgs = new Object[0];

    Panel(Context ctx, AdbClient adb, File dir) {
        this.ctx = ctx;
        this.adb = adb;
        this.js = new JointSpace(dir);
        this.auth = new Auth(dir);
        this.settings = new Prefs(dir);
        this.apps = new Apps(ctx);
        this.sources = new Sources(ctx, this);
        this.launchers = new Launchers(ctx, this);
        this.debloat = new Debloat(ctx, this);
        this.addons = new Addons(ctx, this);
        this.log = new File(dir, "panel.log");
        this.philips = detectPhilips();
        this.picture = new Picture(this, new TvUi(adb), dir);
        this.surprise = new Surprise(this);
    }

    /** API result: HTTP code + message key (translated per request). */
    static final class Result {
        final int code;
        final String key;
        final Object[] args;

        Result(int code, String key, Object... args) {
            this.code = code;
            this.key = key;
            this.args = args;
        }

        JSONObject json(String lang) throws Exception {
            return new JSONObject().put("ok", code < 300).put("msg", Strings.t(lang, key, args));
        }
    }

    private boolean detectPhilips() {
        String m = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.ROOT);
        if (m.contains("philips") || m.contains("tpvision") || m.contains("tp vision")) return true;
        try {
            ctx.getPackageManager().getPackageInfo("org.droidtv.playtv", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // ---------------- status ----------------

    JSONObject status(String lang) throws Exception {
        Map<String, Long> m = meminfo();
        JSONObject out = new JSONObject()
                .put("version", VERSION)
                .put("model", Build.MODEL)
                .put("sdk", Build.VERSION.SDK_INT)
                .put("free_mb", get(m, "MemAvailable") / 1024)
                .put("total_mb", get(m, "MemTotal") / 1024)
                .put("uptime_h", Math.round(SystemClock.elapsedRealtime() / 360000.0) / 10.0)
                .put("party", settings.partyMode())
                .put("surprise", surprise.running())
                .put("philips", philips);
        JSONObject a = new JSONObject().put("state", Strings.t(lang, adb.state)).put("key", adb.state)
                .put("transport", adb.transport == null ? JSONObject.NULL : adb.transport)
                .put("tlsPaired", adb.tlsPaired());
        out.put("adb", a);
        // Philips features use the TV's own JointSpace API, not ADB: report them even while ADB
        // is not connected yet (otherwise the panel would look unpaired right after pairing).
        if (philips) {
            JSONObject p = new JSONObject().put("paired", js.paired());
            // Is the TV's JointSpace there at all, and does it say it has Ambilight? (null = unknown)
            js.system(false);
            p.put("reachable", js.reachable());
            Boolean amb = js.ambilightFeature();
            p.put("has_ambilight", amb == null ? JSONObject.NULL : amb);
            if (js.paired() && !Boolean.FALSE.equals(amb)) {
                try {
                    p.put("ambilight", new JSONObject(js.get("ambilight/power")).optString("power"));
                } catch (Exception e) {
                    p.put("ambilight", JSONObject.NULL);
                }
            }
            try {
                if (hasLogger()) {
                    p.put("fl_threads", flThreads()).put("fl_limit", FL_THREAD_LIMIT)
                            .put("watchdog", new JSONObject().put("ts", wdTs == null ? JSONObject.NULL : wdTs)
                                    .put("msg", Strings.t(lang, wdKey, wdArgs)));
                }
            } catch (Exception ignored) {
                // needs ADB
            }
            out.put("philips_info", p);
        }
        boolean jsOk = philips && js.paired();
        try {
            // Polling must not raise a prompt the user just dismissed; "Check again" does (adb/retry).
            if (adb.declined()) throw new IOException(adb.state);
            String fg = foreground();
            out.put("foreground", fg);
            out.put("foreground_label", label(fg));
            try {
                out.put("foreground_home", fg.length() > 0 && fg.equals(pkgOf(launchers.current())));
            } catch (Exception ignored) {
            }
            a.put("ok", true);
        } catch (Exception e) {
            a.put("ok", false).put("state", Strings.t(lang, adb.state)).put("key", adb.state);
            // Basic mode: no ADB yet. Philips still tells which app is on screen.
            String fg = jsOk ? jsForeground() : "";
            out.put("foreground", fg);
            if (fg.length() > 0) {
                out.put("foreground_label", label(fg));
                out.put("foreground_home", fg.equals(homePackage()));
            }
            out.put("caps", caps(false, jsOk));
            return out;
        }
        out.put("caps", caps(true, jsOk));
        out.put("sources", sources.json());
        if (!out.optBoolean("foreground_home")) {
            String active = sources.activeInput(LIVE_TV_APPS.contains(out.optString("foreground")));
            if (active != null) out.put("active_input", active);
        }
        return out;
    }

    /**
     * What works right now, for the phone to dim the rest. Without ADB ("basic mode") the panel still
     * has Android's own media keys and volume, and on Philips everything JointSpace offers.
     */
    JSONObject caps(boolean adbOk, boolean jsOk) throws Exception {
        boolean remote = adbOk || jsOk;
        return new JSONObject().put("adb", adbOk).put("nav", remote).put("text", remote).put("apps", remote)
                .put("sleep", remote).put("media", true).put("volume", remote || !audio().isVolumeFixed());
    }

    /** Philips JointSpace: package on screen ("" when unknown). */
    String jsForeground() {
        try {
            JSONObject c = new JSONObject(js.get("activities/current")).optJSONObject("component");
            String p = c == null ? "" : c.optString("packageName");
            return Apps.isPackageName(p) ? p : "";
        } catch (Exception e) {
            return "";
        }
    }

    String homePackage() {
        try {
            android.content.pm.ResolveInfo r = ctx.getPackageManager().resolveActivity(
                    new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
                    PackageManager.MATCH_DEFAULT_ONLY);
            return r == null || r.activityInfo == null ? "" : r.activityInfo.packageName;
        } catch (Exception e) {
            return "";
        }
    }

    boolean adbReady() {
        return "adb.connected".equals(adb.state);
    }

    android.media.AudioManager audio() {
        return (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    }

    /** Media keys without ADB: Android hands them to the app that plays (no permission needed). */
    void mediaKey(String keycodeName) {
        int code = android.view.KeyEvent.keyCodeFromString(keycodeName);
        android.media.AudioManager am = audio();
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code));
    }

    private static long get(Map<String, Long> m, String k) {
        Long v = m.get(k);
        return v == null ? 0 : v;
    }

    static Map<String, Long> meminfo() throws IOException {
        Map<String, Long> d = new HashMap<>();
        BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                int c = line.indexOf(':');
                if (c < 0) continue;
                String[] parts = line.substring(c + 1).trim().split("\\s+");
                try {
                    d.put(line.substring(0, c).trim(), Long.parseLong(parts[0]));
                } catch (NumberFormatException ignored) {
                }
            }
        } finally {
            r.close();
        }
        return d;
    }

    private volatile Boolean loggerInstalled;

    boolean hasLogger() {
        if (!philips) return false;
        if (loggerInstalled == null) {
            try {
                ctx.getPackageManager().getPackageInfo(FRAMEWORKLOGGER, 0);
                loggerInstalled = true;
            } catch (PackageManager.NameNotFoundException e) {
                loggerInstalled = false;
            }
        }
        return loggerInstalled;
    }

    int flThreads() throws Exception {
        String out = adb.shell("p=$(pidof " + FRAMEWORKLOGGER + "); [ -n \"$p\" ] && grep ^Threads /proc/$p/status");
        for (String tok : out.split("\\s+")) {
            if (tok.matches("\\d+")) return Integer.parseInt(tok);
        }
        return 0;
    }

    String foreground() throws Exception {
        String out = adb.shell("dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity'");
        for (String tok : out.split("\\s+")) {
            if (tok.contains("/") && tok.contains(".")) return tok.substring(0, tok.indexOf('/'));
        }
        return "";
    }

    // ---------------- fixed actions ----------------

    Result action(String name) throws Exception {
        switch (name) {
            case "clean":
                return clean();
            case "home":
                remote("home");
                return new Result(200, "act.home");
            case "sleep":
                if (philips && js.paired() && !adbReady()) js.post("input/key", new JSONObject().put("key", "Standby"));
                else adb.shell("input keyevent KEYCODE_SLEEP");
                return new Result(200, "act.sleep");
            case "reboot":
                log("reboot (panel)");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(1000);
                        try {
                            adb.reboot();
                        } catch (Exception ignored) {
                        }
                    }
                }).start();
                return new Result(200, "act.reboot");
            default:
                return new Result(404, "act.unknown");
        }
    }

    Result launch(String pkg) throws Exception {
        String cmd = apps.launchCommand(pkg);
        if (cmd == null) return new Result(404, "launch.unknown");
        String[] comp = apps.component(pkg);
        if (!adbReady() && philips && js.paired() && comp != null) {
            // Basic mode on Philips: the TV's own API starts the app.
            js.post("activities/launch", new JSONObject().put("intent", new JSONObject()
                    .put("component", new JSONObject().put("packageName", comp[0]).put("className", comp[1]))
                    .put("action", "empty")));
            log("launch " + pkg + " (JointSpace)");
            return new Result(200, "launch.ok", apps.label(pkg));
        }
        adb.shell(cmd);
        if (AIRPLAY.equals(pkg)) {
            // The receiver service starts with the app; go back home so the TV is usable.
            SystemClock.sleep(4000);
            adb.shell("input keyevent KEYCODE_HOME");
        }
        log("launch " + pkg);
        return new Result(200, "launch.ok", apps.label(pkg));
    }

    Result source(String id) throws Exception {
        Sources.Input in = sources.find(id);
        if (in == null) return new Result(404, "source.unknown");
        adb.shell(sources.switchCommand(in));
        return new Result(200, "source.ok", in.name);
    }

    /**
     * Pre-installed streaming apps that are often system apps (so not in `pm list packages -3`)
     * but are the biggest memory users. Closed like third-party apps.
     */
    private static final String[] HEAVY_SYSTEM_APPS = {
            "com.netflix.ninja", "com.google.android.youtube.tv", "com.google.android.youtube.tvmusic",
            "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus", "com.wbd.stream",
            "com.apple.atve.androidtv.appletv", "com.spotify.tv.android", "com.google.android.videos",
            "com.google.android.youtube.tvkids", "com.plexapp.android", "tv.twitch.android.app",
    };
    /** Receivers that must keep running in the background to be found by phones (AirPlay, cast). */
    private static final String[] RECEIVERS = {
            AIRPLAY, "com.ionitech.airscreen", "com.waxrain.airplaydmr", "com.softmedia.receiver.castapp",
    };

    /**
     * Closes apps waiting in the background: everything the user installed plus the big pre-installed
     * streaming apps. Never touches the app on screen, media that is actually playing, the home screen,
     * the keyboard, accessibility services, VPNs, AirPlay/cast receivers, or other system packages.
     */
    private Result clean() throws Exception {
        long before = get(meminfo(), "MemAvailable");
        Set<String> keep = new HashSet<>();
        keep.add(ctx.getPackageName());
        for (String r : RECEIVERS) keep.add(r);
        keep.add(foreground());
        keep.add(pkgOf(adb.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1")));
        keep.add(pkgOf(adb.shell("settings get secure default_input_method")));
        for (String s : adb.shell("settings get secure enabled_accessibility_services").split(":")) keep.add(pkgOf(s));
        keep.addAll(playingMedia(adb.shell("dumpsys media_session")));
        keep.addAll(vpnApps(adb.shell("dumpsys activity services | grep -E 'ServiceRecord\\{|BIND_VPN_SERVICE'")));

        Set<String> candidates = new HashSet<>();
        for (String line : adb.shell("pm list packages -3").split("\n")) {
            if (line.startsWith("package:")) candidates.add(line.substring(8).trim());
        }
        for (String h : HEAVY_SYSTEM_APPS) candidates.add(h);
        Set<String> running = new HashSet<>();
        for (String line : adb.shell("ps -A -o NAME").split("\n")) {
            String p = line.trim();
            int colon = p.indexOf(':');
            running.add(colon > 0 ? p.substring(0, colon) : p);
        }

        StringBuilder cmd = new StringBuilder();
        List<String> closed = new ArrayList<>();
        for (String p : candidates) {
            if (!running.contains(p) || keep.contains(p) || !Apps.isPackageName(p)) continue;
            cmd.append("am force-stop ").append(p).append("; ");
            closed.add(label(p));
        }
        if (hasLogger()) cmd.append("am force-stop ").append(FRAMEWORKLOGGER).append("; ");
        // No "am kill-all": on Philips it also kills persistent TV services (game switch, tuner
        // checks) that restart at once, so it frees nothing and only risks the TV's stability.
        if (cmd.length() > 0) adb.shell(cmd.toString());
        SystemClock.sleep(2500);
        long gained = Math.max(0, (get(meminfo(), "MemAvailable") - before) / 1024);
        log("clean: closed " + closed + ", +" + gained + " MB");
        if (closed.isEmpty()) return new Result(200, "clean.ok", gained);
        java.util.Collections.sort(closed);
        return new Result(200, "clean.closed", gained, android.text.TextUtils.join(", ", closed));
    }

    /** Packages running a VPN service (lines: "* ServiceRecord{… u0 pkg/.Svc}" then "permission=android.permission.BIND_VPN_SERVICE"). */
    static Set<String> vpnApps(String dump) {
        Set<String> out = new HashSet<>();
        Pattern rec = Pattern.compile("ServiceRecord\\{\\S+ u\\d+ ([^/\\s}]+)/");
        String current = null;
        for (String line : dump.split("\n")) {
            Matcher r = rec.matcher(line);
            if (r.find()) current = r.group(1);
            else if (line.contains("BIND_VPN_SERVICE") && current != null) out.add(current);
        }
        return out;
    }

    /** User-facing app name in the TV's language. */
    String label(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "";
        try {
            PackageManager pm = ctx.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS)));
        } catch (Exception e) {
            return pkg;
        }
    }

    static String pkgOf(String component) {
        if (component == null) return "";
        String c = component.trim();
        int slash = c.indexOf('/');
        return slash > 0 ? c.substring(0, slash) : c;
    }

    /** Packages whose media session is playing/buffering (dumpsys media_session). */
    static Set<String> playingMedia(String dump) {
        Set<String> out = new HashSet<>();
        String[] blocks = dump.split("\\n(?=\\s+package=)");
        Pattern pkg = Pattern.compile("^\\s+package=(\\S+)");
        Pattern st = Pattern.compile("state=PlaybackState \\{state=(\\d+)");
        for (String b : blocks) {
            Matcher p = pkg.matcher(b);
            Matcher s = st.matcher(b);
            if (p.find() && s.find()) {
                int state = Integer.parseInt(s.group(1));
                // 3 playing, 4 fast-forwarding, 5 rewinding, 6 buffering, 8 connecting
                if (state == 3 || state == 4 || state == 5 || state == 6 || state == 8) out.add(p.group(1));
            }
        }
        return out;
    }

    // ---------------- remote, keyboard, volume ----------------

    /** panel key -> {Philips JointSpace key, Android keycode} */
    private static final Map<String, String[]> REMOTE = new HashMap<>();
    static {
        String[][] k = {
                {"up", "CursorUp", "DPAD_UP"}, {"down", "CursorDown", "DPAD_DOWN"},
                {"left", "CursorLeft", "DPAD_LEFT"}, {"right", "CursorRight", "DPAD_RIGHT"},
                {"ok", "Confirm", "DPAD_CENTER"}, {"back", "Back", "BACK"}, {"home", "Home", "HOME"},
                {"playpause", "PlayPause", "MEDIA_PLAY_PAUSE"}, {"rewind", "Rewind", "MEDIA_REWIND"},
                {"forward", "FastForward", "MEDIA_FAST_FORWARD"}, {"volup", "VolumeUp", "VOLUME_UP"},
                {"voldown", "VolumeDown", "VOLUME_DOWN"}, {"mute", "Mute", "VOLUME_MUTE"},
                {"options", "Options", "MENU"}, {"chup", "ChannelStepUp", "CHANNEL_UP"},
                {"chdown", "ChannelStepDown", "CHANNEL_DOWN"},
        };
        for (String[] r : k) REMOTE.put(r[0], new String[]{r[1], "KEYCODE_" + r[2]});
    }

    /** The TVs' own live-TV apps (Philips, AOSP/Google "Live Channels", Sony). */
    static final java.util.Set<String> LIVE_TV_APPS = new java.util.HashSet<>(java.util.Arrays.asList(
            "org.droidtv.playtv", "com.android.tv", "com.google.android.tv", "com.sony.dtv.tvx"));

    private static final java.util.Set<String> MEDIA_KEYS = new java.util.HashSet<>(
            java.util.Arrays.asList("playpause", "rewind", "forward"));

    Result remote(String name) throws Exception {
        String[] k = REMOTE.get(name);
        if (k == null) return new Result(404, "key.unknown");
        // Media keys always go the Android way: Philips JointSpace turns "PlayPause" into a "." key
        // press (seen in its log as KEYCODE_DOT), so apps like YouTube never see it.
        if (philips && js.paired() && !MEDIA_KEYS.contains(name)) {
            try {
                js.post("input/key", new JSONObject().put("key", k[0]));
                return new Result(200, "ok");
            } catch (IOException ignored) {
                // fall back to ADB
            }
        }
        if (MEDIA_KEYS.contains(name) && !adbReady()) {
            mediaKey(k[1]);
            return new Result(200, "ok");
        }
        if (injectKey(k[1])) return new Result(200, "ok");
        adb.shell("input keyevent " + k[1]);
        return new Result(200, "ok");
    }

    // ---- fast key path: a warm key injector process (see KeyInjector) ----

    private AdbClient.Stream injector;
    private long injectorRetryAt;

    /** Returns false when the injector is unavailable; the caller then uses `input keyevent`. */
    synchronized boolean injectKey(String keycodeName) {
        int code = android.view.KeyEvent.keyCodeFromString(keycodeName);
        if (code <= 0) return false;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                if (injector == null || !injector.isOpen()) {
                    if (SystemClock.elapsedRealtime() < injectorRetryAt) return false;
                    startInjector();
                }
                injector.write((code + "\n").getBytes("US-ASCII"));
                return true;
            } catch (Exception e) {
                if (injector != null) injector.close();
                injector = null;
            }
        }
        injectorRetryAt = SystemClock.elapsedRealtime() + 5 * 60_000L;  // do not hammer a broken setup
        return false;
    }

    private void startInjector() throws Exception {
        AdbClient.Stream s = adb.openStream("exec:CLASSPATH=" + shellQuote(ctx.getPackageCodePath())
                + " app_process / " + KeyInjector.class.getName());
        long end = SystemClock.elapsedRealtime() + 5000;
        while (SystemClock.elapsedRealtime() < end && s.isOpen() && !s.awaitOutput(100).contains("ready")) {
            // wait for the VM to start
        }
        if (!s.isOpen() || !s.awaitOutput(0).contains("ready")) {
            s.close();
            throw new IOException("injector did not start");
        }
        injector = s;
        log("key injector started");
    }

    Result keyboard(String text) throws Exception {
        if (text == null || text.isEmpty()) return new Result(400, "kb.empty");
        if (text.length() > 200) return new Result(400, "kb.too_long");
        if (philips && js.paired()) {
            // JointSpace takes any Unicode character.
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                js.post("input/key", new JSONObject().put("unicode", new String(Character.toChars(cp))));
                i += Character.charCount(cp);
            }
            return new Result(200, "kb.sent");
        }
        adb.shell("input text " + shellQuote(asciiForInput(text)));
        return new Result(200, text.equals(asciiFold(text)) ? "kb.sent" : "kb.ascii_only");
    }

    Result key(String which) throws Exception {
        if ("del".equals(which)) adb.shell("input keyevent KEYCODE_DEL");
        else if ("enter".equals(which)) adb.shell("input keyevent KEYCODE_ENTER");
        else return new Result(404, "key.unknown");
        return new Result(200, "ok");
    }


    /** Philips: JointSpace (the real TV speaker volume). Others: Android music stream via ADB. */
    JSONObject volume() throws Exception {
        if (philips && js.paired()) {
            JSONObject v = new JSONObject(js.get("audio/volume"));
            return new JSONObject().put("current", v.optInt("current")).put("min", v.optInt("min", 0))
                    .put("max", v.optInt("max", 60)).put("muted", v.optBoolean("muted"));
        }
        if (!adbReady()) return androidVolume();
        // `dumpsys audio` has the music stream on every Android version (media_session volume needs 12+).
        String block = adb.shell("dumpsys audio | grep -A8 '^- STREAM_MUSIC'");
        Matcher max = Pattern.compile("Max: (\\d+)").matcher(block);
        Matcher min = Pattern.compile("Min: (\\d+)").matcher(block);
        if (!max.find()) throw new IOException("vol.unavailable");
        int cur = -1;
        Matcher sv = Pattern.compile("streamVolume:(\\d+)").matcher(block);
        if (sv.find()) {
            cur = Integer.parseInt(sv.group(1));
        } else {
            // "Current: 2 (speaker): 12, 400 (hdmi): 15"  +  "Devices: speaker"   (Android 8–11)
            // "Current: 40000000 (default): 11"          +  "Devices: speaker"   (default applies to all)
            Map<String, Integer> byDevice = new HashMap<>();
            Matcher pair = Pattern.compile("\\(([\\w_-]+)\\): (\\d+)").matcher(block);
            Integer first = null;
            while (pair.find()) {
                int v = Integer.parseInt(pair.group(2));
                byDevice.put(pair.group(1), v);
                if (first == null) first = v;
            }
            Matcher dev = Pattern.compile("Devices: (\\S+)").matcher(block);
            String active = dev.find() ? dev.group(1) : "speaker";
            Integer v = byDevice.get(active);
            if (v == null) v = byDevice.get("default");
            if (v == null) v = first;
            if (v != null) cur = v;
        }
        if (cur < 0) throw new IOException("vol.unavailable");
        Matcher dev = Pattern.compile("Devices: (\\S+)").matcher(block);
        boolean fixed = fixedVolume(adb.shell("dumpsys audio | grep -E 'mUseFixedVolume|mFixedVolumeDevices|mFullVolumeDevices'"),
                dev.find() ? dev.group(1) : "speaker");
        return new JSONObject().put("current", cur).put("min", min.find() ? Integer.parseInt(min.group(1)) : 0)
                .put("max", Integer.parseInt(max.group(1))).put("fixed", fixed)
                .put("muted", Pattern.compile("^\\s*Muted: true", Pattern.MULTILINE).matcher(block).find());
    }

    /** Basic mode: the music stream through Android's AudioManager (no ADB, no permission). */
    JSONObject androidVolume() throws Exception {
        android.media.AudioManager am = audio();
        int s = android.media.AudioManager.STREAM_MUSIC;
        return new JSONObject().put("current", am.getStreamVolume(s))
                .put("min", Build.VERSION.SDK_INT >= 28 ? am.getStreamMinVolume(s) : 0)
                .put("max", am.getStreamMaxVolume(s)).put("muted", am.isStreamMute(s))
                .put("fixed", am.isVolumeFixed());
    }

    Result setAndroidVolume(JSONObject body) throws Exception {
        android.media.AudioManager am = audio();
        int s = android.media.AudioManager.STREAM_MUSIC;
        // Fixed volume (HDMI sticks, soundbars over ARC): only the remote's keys reach the TV, that needs ADB.
        if (am.isVolumeFixed()) throw new IOException("vol.needs_setup");
        int flags = android.media.AudioManager.FLAG_SHOW_UI;
        if (body.has("step")) {
            int step = Math.max(-10, Math.min(10, body.getInt("step")));
            for (int i = 0; i < Math.abs(step); i++) {
                am.adjustStreamVolume(s, step > 0 ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER, flags);
            }
            return new Result(200, "vol.set", am.getStreamVolume(s));
        }
        if (body.has("value")) {
            int min = Build.VERSION.SDK_INT >= 28 ? am.getStreamMinVolume(s) : 0;
            am.setStreamVolume(s, Math.max(min, Math.min(am.getStreamMaxVolume(s), body.getInt("value"))), flags);
        }
        if (body.has("muted")) {
            am.adjustStreamVolume(s, body.getBoolean("muted") ? android.media.AudioManager.ADJUST_MUTE : android.media.AudioManager.ADJUST_UNMUTE, flags);
        }
        return am.isStreamMute(s) ? new Result(200, "vol.muted") : new Result(200, "vol.set", am.getStreamVolume(s));
    }

    /** Output device name (dumpsys audio) -> AudioSystem device bit. */
    private static final Map<String, Integer> OUT_DEVICES = new HashMap<>();
    static {
        OUT_DEVICES.put("speaker", 0x2);
        OUT_DEVICES.put("hdmi", 0x400);
        OUT_DEVICES.put("hdmi_arc", 0x40000);
        OUT_DEVICES.put("hdmi_earc", 0x40001);
        OUT_DEVICES.put("spdif", 0x80000);
        OUT_DEVICES.put("line", 0x20000);
    }

    /**
     * HDMI sticks (Chromecast, Fire-style boxes) and TVs playing through a soundbar (HDMI ARC) keep
     * Android's own level fixed; the volume keys are passed on to the TV / soundbar over HDMI-CEC.
     * The panel then shows − / + only, which send the same keys as the remote.
     */
    static boolean fixedVolume(String dump, String activeDevice) {
        if (dump.contains("mUseFixedVolume=true")) return true;
        Integer bit = OUT_DEVICES.get(activeDevice);
        if (bit == null) return false;
        Matcher m = Pattern.compile("m(?:Fixed|Full)VolumeDevices=([^\\n]*)").matcher(dump);
        while (m.find()) {
            for (String part : m.group(1).split("[,\\s\\[\\]]+")) {
                String hex = part.replaceAll("\\(.*", "").trim();
                if (!hex.startsWith("0x")) continue;
                try {
                    if (Integer.parseInt(hex.substring(2), 16) == bit) return true;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    Result setVolume(JSONObject body) throws Exception {
        if (body.has("step")) {
            // − / +: exactly like the remote's volume keys (works on every TV, also with fixed volume).
            int step = Math.max(-10, Math.min(10, body.getInt("step")));
            if (philips && js.paired()) {
                JSONObject v = new JSONObject(js.get("audio/volume"));
                int cur = Math.max(v.optInt("min", 0), Math.min(v.optInt("max", 60), v.optInt("current") + step));
                js.post("audio/volume", new JSONObject().put("current", cur).put("muted", false));
                return new Result(200, "vol.set", cur);
            }
            if (!adbReady()) return setAndroidVolume(body);
            String key = step > 0 ? "KEYCODE_VOLUME_UP" : "KEYCODE_VOLUME_DOWN";
            for (int i = 0; i < Math.abs(step); i++) {
                if (!injectKey(key)) adb.shell("input keyevent " + key);
            }
            try {
                JSONObject v = volume();
                if (!v.optBoolean("fixed")) return new Result(200, "vol.set", v.getInt("current"));
            } catch (Exception ignored) {
            }
            return new Result(200, step > 0 ? "vol.up" : "vol.down");
        }
        if (!(philips && js.paired()) && !adbReady()) return setAndroidVolume(body);
        JSONObject now = volume();
        int cur = body.has("value")
                ? Math.max(now.getInt("min"), Math.min(now.getInt("max"), body.getInt("value"))) : now.getInt("current");
        boolean muted = body.has("muted") ? body.getBoolean("muted") : now.getBoolean("muted");
        if (philips && js.paired()) {
            js.post("audio/volume", new JSONObject().put("current", cur).put("muted", muted));
        } else {
            // "media_session volume --set" changes the default device, not the active output on many TVs.
            // Volume keys always reach the right output, exactly like the remote.
            int delta = cur - now.getInt("current");
            if (body.has("value") && delta != 0) {
                StringBuilder k = new StringBuilder("input keyevent");
                String code = delta > 0 ? " KEYCODE_VOLUME_UP" : " KEYCODE_VOLUME_DOWN";
                for (int i = 0; i < Math.min(Math.abs(delta), 100); i++) k.append(code);
                adb.shell(k.toString());
                SystemClock.sleep(150);
            }
            if (body.has("muted") && muted != now.getBoolean("muted")) adb.shell("input keyevent KEYCODE_VOLUME_MUTE");
        }
        return muted ? new Result(200, "vol.muted") : new Result(200, "vol.set", cur);
    }

    static String asciiFold(String s) {
        String from = "çÇğĞıİöÖşŞüÜâÂîÎûÛäÄéÉèÈêÊáÁàÀñÑ", to = "cCgGiIoOsSuUaAiIuUaAeEeEeEaAaAnN";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            int i = from.indexOf(c);
            char o = i >= 0 ? to.charAt(i) : c;
            if (o >= 0x20 && o < 0x7f) b.append(o);
        }
        return b.toString();
    }

    /** `input text` wants %s for spaces. */
    private static String asciiForInput(String s) {
        return asciiFold(s).replace(" ", "%s");
    }

    static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    // ---------------- Philips: Ambilight ----------------

    Result philipsPair(String what, JSONObject body) throws Exception {
        if (!philips) return new Result(404, "philips.unsupported");
        try {
            if ("start".equals(what)) {
                js.pairStart();
                return new Result(200, "philips.pin");
            }
            if ("finish".equals(what)) {
                js.pairFinish(body.optString("pin"));
                log("philips paired");
                return new Result(200, "philips.paired");
            }
        } catch (JointSpace.PairError e) {
            log("philips pairing refused: " + e.detail);
            return new Result(502, e.getMessage(), e.detail);
        } catch (IOException e) {
            if (Strings.has(e.getMessage())) log("philips pairing: " + e.getMessage());
            throw e;
        }
        return new Result(404, "not_found");
    }

    /** For a bug report: what the TV's JointSpace says about itself (no ids, keys or addresses). */
    JSONObject philipsDiagnostics() throws Exception {
        JSONObject d = new JSONObject().put("ok", true).put("app", VERSION)
                .put("manufacturer", Build.MANUFACTURER).put("brand", Build.BRAND).put("model", Build.MODEL)
                .put("sdk", Build.VERSION.SDK_INT).put("philips", philips);
        if (philips) {
            d.put("jointspace", js.diagnostics());
            d.put("picture", picture.diagnostics());
        }
        return d;
    }

    Result ambilight(String what, JSONObject body) throws Exception {
        if (!philips) return new Result(404, "philips.unsupported");
        if (!js.paired()) return new Result(409, "philips.not_paired");
        if (Boolean.FALSE.equals(js.ambilightFeature())) return new Result(404, "amb.unsupported");
        // The TV accepting a request does not mean it did anything (seen on newer firmware):
        // read the state back and say so when it did not change.
        switch (what) {
            case "on":
                js.post("ambilight/power", new JSONObject().put("power", "On"));
                return checkAmbilight("power", "power", "On", "amb.on");
            case "off":
                js.post("ambilight/power", new JSONObject().put("power", "Off"));
                return checkAmbilight("power", "power", "Off", "amb.off");
            case "video":
                ambilightStyle("FOLLOW_VIDEO", "STANDARD");
                return checkAmbilight("currentconfiguration", "styleName", "FOLLOW_VIDEO", "amb.video");
            case "music":
                ambilightStyle("FOLLOW_AUDIO", "ENERGY_ADAPTIVE_BRIGHTNESS");
                return checkAmbilight("currentconfiguration", "styleName", "FOLLOW_AUDIO", "amb.music");
            case "color":
                ambilightColor(body.optInt("r"), body.optInt("g"), body.optInt("b"));
                return checkAmbilight("mode", "current", "manual", "amb.color");
            default:
                return new Result(404, "not_found");
        }
    }

    private Result checkAmbilight(String path, String field, String want, String okKey) throws Exception {
        String got = null;
        for (int i = 0; i < 3; i++) {
            SystemClock.sleep(250);
            try {
                got = new JSONObject(js.get("ambilight/" + path)).optString(field, null);
            } catch (Exception e) {
                got = null;  // cannot be read back on this TV: trust the accepted request
                break;
            }
            if (want.equalsIgnoreCase(got)) return new Result(200, okKey);
        }
        if (got == null) return new Result(200, okKey);
        js.noteError("ambilight/" + path + ": wanted " + want + ", TV reports " + got);
        log("ambilight " + path + " did not change: " + got);
        return new Result(502, "amb.no_effect");
    }

    void ambilightStyle(String style, String setting) throws Exception {
        js.post("ambilight/power", new JSONObject().put("power", "On"));
        // Only when the mode really differs: on Philips (seen on 2020 MediaTek firmware) a redundant
        // mode=internal makes the Ambilight driver think "follow video" is already applied, so
        // music -> video was ignored and the light kept following the sound.
        String mode = null;
        try {
            mode = new JSONObject(js.get("ambilight/mode")).optString("current", null);
        } catch (Exception ignored) {
            // unknown: set it
        }
        if (!"internal".equalsIgnoreCase(mode)) js.post("ambilight/mode", new JSONObject().put("current", "internal"));
        js.post("ambilight/currentconfiguration", new JSONObject()
                .put("styleName", style).put("isExpert", false).put("menuSetting", setting));
    }

    void ambilightColor(int r, int g, int b) throws Exception {
        js.post("ambilight/mode", new JSONObject().put("current", "manual"));
        js.post("ambilight/cached", new JSONObject().put("r", clamp(r)).put("g", clamp(g)).put("b", clamp(b)));
    }

    void ambilightNormal() throws Exception {
        js.post("ambilight/mode", new JSONObject().put("current", "internal"));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ---------------- screenshot ----------------

    byte[] screenshotJpeg() throws Exception {
        byte[] png = adb.execOut("screencap -p");
        // With protected content (Netflix etc.) some TVs return nothing at all.
        if (png.length == 0) throw new IOException("shot.protected");
        Bitmap bmp = BitmapFactory.decodeByteArray(png, 0, png.length);
        if (bmp == null) throw new IOException("shot.decode");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 82, out);
        bmp.recycle();
        return out.toByteArray();
    }

    // ---------------- maintenance ----------------

    void startWatchdog() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        ex.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!hasLogger()) return;
                String now = new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date());
                try {
                    int n = flThreads();
                    if (n > FL_THREAD_LIMIT) {
                        adb.shell("am force-stop " + FRAMEWORKLOGGER);
                        wdKey = "wd.reset";
                        log("watchdog: logger " + n + " threads, restarted");
                    } else {
                        wdKey = "wd.ok";
                    }
                    wdArgs = new Object[]{n};
                } catch (Exception e) {
                    wdKey = "wd.error";
                    wdArgs = new Object[]{Strings.t("en", adb.state)};
                }
                wdTs = now;
            }
        }, 30, WATCHDOG_EVERY_MIN * 60, TimeUnit.SECONDS);
    }

    synchronized void log(String msg) {
        try {
            if (log.length() > 200_000) log.renameTo(new File(log.getPath() + ".1"));
            FileWriter w = new FileWriter(log, true);
            w.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()) + "  " + msg + "\n");
            w.close();
        } catch (IOException ignored) {
        }
    }

    JSONArray apps() throws Exception {
        return apps.list(settings);
    }
}
