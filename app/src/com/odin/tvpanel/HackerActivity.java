package com.odin.tvpanel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Visuals of the party-mode show. Timing comes from Surprise.phase / phaseStart. */
public final class HackerActivity extends Activity {
    private ToneGenerator tone;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (RuntimeException e) {
            tone = null;  // no audio: the show continues silently
        }
        setContentView(new Screen(this));
    }

    @Override
    protected void onDestroy() {
        if (tone != null) tone.release();
        super.onDestroy();
    }

    void beep(int type, int ms) {
        if (tone != null) tone.startTone(type, ms);
    }

    private final class Screen extends View {
        private static final int GREEN = 0xff39ff7a;
        private static final int RED = 0xffff2a3d;
        private static final String GLYPHS = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄ0123456789ODIN#$%&";

        private final Random rnd = new Random();
        private final Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint();
        private final String[] lines;
        private final long[] lineStart;
        private final long[] lineDur;
        private int[] drops;
        private float colW;
        private int lastBeepLine = -1;
        private int lastCount = -1;
        private boolean sirenDone, takeoverBeep;

        Screen(Context c) {
            super(c);
            mono.setTypeface(Typeface.MONOSPACE);
            big.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            big.setTextAlign(Paint.Align.CENTER);

            String ram = "?";
            try {
                Map<String, Long> m = Panel.meminfo();
                ram = String.valueOf(m.get("MemTotal") / 1024);
            } catch (Exception ignored) {
            }
            lines = new String[]{
                    "$ ./odin_breach --target " + MainActivity.ip(),
                    Strings.t(Surprise.lang, "hk.l1", Build.MODEL),
                    Strings.t(Surprise.lang, "hk.l2", Build.DEVICE, Build.VERSION.RELEASE, String.valueOf(System.getProperty("os.version")).split("-")[0]),
                    Strings.t(Surprise.lang, "hk.l3", Runtime.getRuntime().availableProcessors(), ram),
                    Strings.t(Surprise.lang, "hk.l4"),
                    Strings.t(Surprise.lang, "hk.l5"),
                    Strings.t(Surprise.lang, "hk.l6"),
                    Strings.t(Surprise.lang, PanelService.panel != null && PanelService.panel.philips ? "hk.l7" : "hk.l7b"),
                    Strings.t(Surprise.lang, "hk.l8"),
                    Strings.t(Surprise.lang, "hk.l9"),
            };
            // Line timing: the terminal starts at 4 s and ends at 16 s.
            lineStart = new long[lines.length];
            lineDur = new long[lines.length];
            long t = 4300;
            for (int i = 0; i < lines.length; i++) {
                boolean dyn = lines[i].contains("%");
                lineDur[i] = dyn ? 1900 : Math.max(350, lines[i].length() * 16L);
                lineStart[i] = t;
                t += lineDur[i] + 180;
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            int phase = Surprise.phase;
            if (phase == Surprise.PHASE_CLOSE || phase == Surprise.PHASE_NONE) {
                finish();
                return;
            }
            long t = SystemClock.uptimeMillis() - Surprise.phaseStart;
            int w = getWidth(), h = getHeight();
            if (drops == null) initRain(w, h);
            c.drawColor(Color.BLACK);

            if (phase == Surprise.PHASE_INTRO) {
                if (t < 4000) alert(c, w, h, t);
                else if (t < 16200) terminal(c, w, h, t);
                else takeover(c, w, h, t);
            } else {
                finale(c, w, h, t);
            }
            postInvalidateOnAnimation();
        }

        // ---- scenes ----

        private void alert(Canvas c, int w, int h, long t) {
            if (!sirenDone) {
                sirenDone = true;
                beep(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 3800);
            }
            noise(c, w, h, 900);
            boolean on = (t / 280) % 2 == 0;
            if (on) {
                fill.setColor(0x55ff0000);
                c.drawRect(0, 0, w, h, fill);
            }
            big.setTextSize(h * 0.075f);
            glitchText(c, Strings.t(Surprise.lang, "hk.alert"), w / 2f, h * 0.47f, on ? RED : 0xffaa1020, 14);
            big.setTextSize(h * 0.032f);
            glitchText(c, Strings.t(Surprise.lang, "hk.unauth"), w / 2f, h * 0.57f, 0xffdddddd, 4);
        }

        private void terminal(Canvas c, int w, int h, long t) {
            rain(c, h, 0x55);
            float left = w * 0.08f, top = h * 0.14f, right = w * 0.92f, bottom = h * 0.86f;
            fill.setColor(0xd9000000);
            c.drawRoundRect(new RectF(left, top, right, bottom), 18, 18, fill);
            fill.setColor(0x6639ff7a);
            fill.setStyle(Paint.Style.STROKE);
            fill.setStrokeWidth(2);
            c.drawRoundRect(new RectF(left, top, right, bottom), 18, 18, fill);
            fill.setStyle(Paint.Style.FILL);

            mono.setTextSize(h * 0.03f);
            mono.setColor(0x9939ff7a);
            c.drawText("root@odin-breach:~", left + 28, top + h * 0.045f, mono);

            mono.setTextSize(h * 0.034f);
            float y = top + h * 0.11f, lh = h * 0.062f;
            for (int i = 0; i < lines.length; i++) {
                if (t < lineStart[i]) break;
                long lt = t - lineStart[i];
                String s = render(i, lt);
                boolean last = i == lines.length - 1;
                mono.setColor(last ? RED : lines[i].startsWith("[+]") ? 0xffffffff : GREEN);
                c.drawText(s, left + 28, y, mono);
                if (i != lastBeepLine) {
                    lastBeepLine = i;
                    beep(last ? ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD : ToneGenerator.TONE_PROP_BEEP, last ? 500 : 40);
                }
                y += lh;
            }
            // blinking cursor
            if ((t / 400) % 2 == 0) {
                fill.setColor(GREEN);
                c.drawRect(left + 28, y - lh * 0.55f, left + 28 + h * 0.018f, y, fill);
            }
        }

        private String render(int i, long lt) {
            String s = lines[i];
            if (s.contains("%BAR%")) {
                int pct = (int) Math.min(100, lt * 100 / lineDur[i]);
                int n = pct / 5;
                StringBuilder bar = new StringBuilder("[");
                for (int k = 0; k < 20; k++) bar.append(k < n ? '█' : '░');
                return s.replace("%BAR%", bar + "] " + pct + "%");
            }
            if (s.contains("%HEX%")) {
                if (lt >= lineDur[i]) return s.replace("%HEX%", "0x3F9A·C0DE·E21C  ✓");
                return s.replace("%HEX%", String.format(Locale.ROOT, "0x%04X·%04X·%04X",
                        rnd.nextInt(0x10000), rnd.nextInt(0x10000), rnd.nextInt(0x10000)));
            }
            int n = (int) Math.min(s.length(), lt * s.length() / lineDur[i] + 1);
            return s.substring(0, n);
        }

        private void takeover(Canvas c, int w, int h, long t) {
            if (!takeoverBeep) {
                takeoverBeep = true;
                beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900);
            }
            rain(c, h, 0xcc);
            if ((t / 120) % 2 == 0) {
                fill.setColor(0x3339ff7a);
                c.drawRect(0, 0, w, h, fill);
            }
            big.setTextSize(h * 0.09f);
            glitchText(c, Strings.t(Surprise.lang, "hk.takeover"), w / 2f, h * 0.52f, GREEN, 18);
        }

        private void finale(Canvas c, int w, int h, long t) {
            if (t < 1400) {
                noise(c, w, h, 1200);
                return;
            }
            rain(c, h, 0x77);
            String msg = Surprise.message;
            float size = h * 0.1f;
            big.setTextSize(size);
            while (big.measureText(msg) > w * 0.9f && size > h * 0.04f) {
                size *= 0.92f;
                big.setTextSize(size);
            }
            glitchText(c, msg, w / 2f, h * 0.46f, 0xffffffff, 10);

            if (t > 5800) {
                int left = (int) Math.max(1, 5 - (t - 5800) / 1000);
                if (left != lastCount) {
                    lastCount = left;
                    beep(ToneGenerator.TONE_PROP_BEEP, 120);
                }
                big.setTextSize(h * 0.034f);
                big.setColor(0xff8b93a7);
                c.drawText(Strings.t(Surprise.lang, "hk.restore"), w / 2f, h * 0.62f, big);
                big.setTextSize(h * 0.09f);
                big.setColor(GREEN);
                c.drawText(String.valueOf(left), w / 2f, h * 0.76f, big);
            }
        }

        // ---- effects ----

        private void initRain(int w, int h) {
            colW = h / 34f;
            drops = new int[(int) (w / colW) + 1];
            for (int i = 0; i < drops.length; i++) drops[i] = -rnd.nextInt(40);
        }

        private void rain(Canvas c, int h, int alpha) {
            mono.setTextSize(colW * 0.9f);
            int rows = (int) (h / colW) + 1;
            for (int i = 0; i < drops.length; i++) {
                int head = drops[i];
                for (int k = 0; k < 16; k++) {
                    int row = head - k;
                    if (row < 0 || row > rows) continue;
                    int a = k == 0 ? alpha : alpha * (16 - k) / 18;
                    mono.setColor(k == 0 ? Color.argb(a, 210, 255, 220) : Color.argb(a, 57, 255, 122));
                    char g = GLYPHS.charAt(rnd.nextInt(GLYPHS.length()));
                    c.drawText(String.valueOf(g), i * colW, row * colW, mono);
                }
                if (rnd.nextInt(3) > 0) drops[i]++;
                if (drops[i] - 16 > rows && rnd.nextInt(20) == 0) drops[i] = -rnd.nextInt(10);
            }
        }

        private void noise(Canvas c, int w, int h, int n) {
            for (int i = 0; i < n; i++) {
                int v = rnd.nextInt(256);
                fill.setColor(Color.argb(140 + rnd.nextInt(115), v, v, v));
                float x = rnd.nextInt(w), y = rnd.nextInt(h);
                c.drawRect(x, y, x + 2 + rnd.nextInt(10), y + 2 + rnd.nextInt(4), fill);
            }
            // tear lines
            for (int i = 0; i < 4; i++) {
                fill.setColor(Color.argb(90, 255, 255, 255));
                float y = rnd.nextInt(h);
                c.drawRect(0, y, w, y + 1 + rnd.nextInt(6), fill);
            }
        }

        /** RGB-shifted "glitch" text. */
        private void glitchText(Canvas c, String s, float x, float y, int color, int maxShift) {
            float dx = rnd.nextInt(10) < 3 ? rnd.nextInt(maxShift + 1) : 2;
            big.setColor(0xaaff2a6d);
            c.drawText(s, x - dx, y, big);
            big.setColor(0xaa05d9e8);
            c.drawText(s, x + dx, y, big);
            big.setColor(color);
            c.drawText(s, x, y, big);
        }
    }
}
