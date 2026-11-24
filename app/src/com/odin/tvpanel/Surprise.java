package com.odin.tvpanel;

import android.graphics.Color;
import android.os.SystemClock;

import org.json.JSONObject;

import java.util.List;

/**
 * Party mode ("Sürpriz"): a ~45 s "your TV got hacked" show for friends. Off by default,
 * switched on in the panel settings. HackerActivity draws the visuals; this class runs the
 * timeline, makes the TV "move by itself" and (on Philips) drives Ambilight.
 * Afterwards Ambilight, volume and the home screen are always restored.
 */
final class Surprise {
    // Shared with HackerActivity (same process).
    static final int PHASE_NONE = 0, PHASE_INTRO = 1, PHASE_FINALE = 2, PHASE_CLOSE = -1;
    static volatile int phase = PHASE_NONE;
    static volatile long phaseStart;
    static volatile String message = "";
    /** Language of the phone that started the show (the TV screen follows it, not the TV's language). */
    static volatile String lang = "en";

    private final Panel p;
    private volatile Thread worker;

    Surprise(Panel p) {
        this.p = p;
    }

    boolean running() {
        Thread w = worker;
        return w != null && w.isAlive();
    }

    synchronized boolean start(String msg, String language) {
        if (running()) return false;
        lang = Strings.isTurkish(language) ? "tr" : "en";
        String m = msg == null ? "" : msg.trim();
        if (m.length() > 60) m = m.substring(0, 60);
        message = m.isEmpty() ? Strings.t(lang, "party.default_msg") : m;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                show();
            }
        }, "surprise");
        worker.start();
        return true;
    }

    void stop() {
        Thread w = worker;
        if (w != null) w.interrupt();
    }

    private void show() {
        boolean amb = p.philips && p.js.paired();
        JSONObject volume = readVolume();
        p.log("party mode start");
        try {
            if (amb) {
                p.js.post("ambilight/power", new JSONObject().put("power", "On"));
                p.js.post("ambilight/mode", new JSONObject().put("current", "manual"));
            }

            // 1) alarm (0-4 s), hacker terminal (4-16 s), "control taken" (16-18 s)
            setPhase(PHASE_INTRO);
            open();
            if (amb) strobe(255, 0, 0, 4000);
            else sleep(4000);
            if (amb) color(0, 255, 60);
            sleep(14000);

            // 2) the TV "moves by itself"
            Thread rainbow = amb ? rainbow() : null;
            key("KEYCODE_HOME", "Home");
            sleep(1500);
            for (int i = 0; i < 4; i++) {
                key("KEYCODE_DPAD_RIGHT", "CursorRight");
                sleep(380);
            }
            for (int i = 0; i < 2; i++) {
                key("KEYCODE_DPAD_LEFT", "CursorLeft");
                sleep(380);
            }
            sleep(500);
            List<String> fav = p.apps.favorites(p.settings, p.apps.installed());
            if (fav.size() > 0) launch(fav.get(0));
            sleep(4000);
            if (volume != null) {
                // volume "goes crazy", then returns to exactly what it was
                for (int i = 0; i < 6; i++) {
                    key("KEYCODE_VOLUME_UP", "VolumeUp");
                    sleep(220);
                }
                sleep(700);
                restoreVolume(volume);
            }
            if (fav.size() > 1) launch(fav.get(1));
            sleep(3500);
            key("KEYCODE_HOME", "Home");
            sleep(1000);
            if (rainbow != null) rainbow.interrupt();

            // 3) finale: message + countdown
            setPhase(PHASE_FINALE);
            open();
            if (amb) pulse(140, 0, 255, 11000);
            else sleep(11000);
        } catch (InterruptedException e) {
            // stopped from the panel
        } catch (Exception e) {
            p.log("party mode error: " + e);
        } finally {
            phase = PHASE_CLOSE;
            try {
                if (amb) p.ambilightNormal();
            } catch (Exception ignored) {
            }
            try {
                if (volume != null) restoreVolume(volume);
            } catch (Exception ignored) {
            }
            try {
                SystemClock.sleep(600);
                key("KEYCODE_HOME", "Home");
            } catch (Exception ignored) {
            }
            p.log("party mode end");
        }
    }

    private void launch(String pkg) throws Exception {
        String cmd = p.apps.launchCommand(pkg);
        if (cmd != null) p.adb.shell(cmd);
    }

    private void setPhase(int ph) {
        phaseStart = SystemClock.uptimeMillis();
        phase = ph;
    }

    private void open() throws Exception {
        p.adb.shell("am start -n " + p.ctx.getPackageName() + "/.HackerActivity");
    }

    /** JointSpace when paired (instant); otherwise ADB (~0.4 s per key). */
    private void key(String adbKey, String jointSpaceKey) throws Exception {
        if (p.philips && p.js.paired()) {
            try {
                p.js.post("input/key", new JSONObject().put("key", jointSpaceKey));
                return;
            } catch (java.io.IOException ignored) {
                // fall back to ADB
            }
        }
        p.adb.shell("input keyevent " + adbKey);
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    // ---- Ambilight effects (manual mode) ----

    private void color(int r, int g, int b) throws Exception {
        p.js.post("ambilight/cached", new JSONObject().put("r", r).put("g", g).put("b", b));
    }

    private void strobe(int r, int g, int b, long ms) throws Exception {
        long end = SystemClock.uptimeMillis() + ms;
        boolean on = true;
        while (SystemClock.uptimeMillis() < end) {
            if (on) color(r, g, b);
            else color(0, 0, 0);
            on = !on;
            sleep(230);
        }
    }

    private void pulse(int r, int g, int b, long ms) throws Exception {
        long start = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() - start < ms) {
            double k = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin((SystemClock.uptimeMillis() - start) / 300.0));
            color((int) (r * k), (int) (g * k), (int) (b * k));
            sleep(120);
        }
    }

    private Thread rainbow() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                float hue = 0;
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        int c = Color.HSVToColor(new float[]{hue, 1f, 1f});
                        color(Color.red(c), Color.green(c), Color.blue(c));
                        hue = (hue + 24) % 360;
                        Thread.sleep(160);
                    }
                } catch (Exception ignored) {
                }
            }
        }, "rainbow");
        t.start();
        return t;
    }

    // ---- volume ----

    private JSONObject readVolume() {
        try {
            return p.volume();
        } catch (Exception e) {
            return null;
        }
    }

    private void restoreVolume(JSONObject v) throws Exception {
        p.setVolume(new JSONObject().put("value", v.getInt("current")).put("muted", v.optBoolean("muted")));
    }
}
