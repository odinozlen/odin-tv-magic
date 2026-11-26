package com.odin.tvpanel;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import io.nayuki.qrcodegen.QrCode;


/**
 * The tile on the TV: starts the panel, guides through the one-time ADB setup (developer mode,
 * USB debugging, approving the prompt) and shows how to pair a phone (QR code + one-time code,
 * valid only while this screen is open). Phones can pair before the setup is done; the panel
 * then runs in basic mode.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 14, 23);
    private static final int CARD = Color.rgb(24, 29, 45);
    private static final int DIM = Color.rgb(139, 147, 167);
    private static final int FAINT = Color.rgb(100, 106, 122);
    private static final int ACCENT = Color.rgb(107, 133, 255);
    private static final int BTN = Color.rgb(40, 44, 60);

    private static volatile boolean pairedEvent;

    private final Handler handler = new Handler();
    private final AtomicBoolean attempting = new AtomicBoolean();
    private ImageView qr;
    private TextView url, codeLabel, code, codeValid, lead, scan, status, update, note, warning;
    private LinearLayout setupCard;
    private TextView stepNo, stepTitle, stepBody, stepPath;
    private Button stepButton, forget;
    private String shownCode;
    private boolean visible;
    private int qrBig, qrSmall;
    private Setup.Kind kind;
    private boolean tv;
    private Setup.Step shownStep;
    /** An attempt has finished since the user came back / changed a setting: its result is current. */
    private volatile boolean fresh;
    private long lastAttempt;
    private ContentObserver observer;

    static void notifyPaired() {
        pairedEvent = true;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        PanelService.start(this);
        kind = Setup.kind(this);
        tv = getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK);
        float d = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        boolean portrait = dm.heightPixels > dm.widthPixels;  // phones/tablets; TVs are landscape
        root.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding((int) (40 * d), (int) (24 * d), (int) (40 * d), (int) (24 * d));

        qrBig = (int) (300 * d);
        qrSmall = (int) (210 * d);
        qr = new ImageView(this);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(qrBig, qrBig);
        if (portrait) qp.bottomMargin = (int) (20 * d); else qp.rightMargin = (int) (36 * d);
        qr.setLayoutParams(qp);
        root.addView(qr);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.START);
        root.addView(col, portrait
                ? new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        col.addView(text(Strings.tv("tv.title"), 30, Color.WHITE, true));

        // ---- setup guide ----
        setupCard = new LinearLayout(this);
        setupCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setCornerRadius(16 * d);
        cbg.setColor(CARD);
        setupCard.setBackground(cbg);
        setupCard.setPadding((int) (18 * d), (int) (12 * d), (int) (18 * d), (int) (14 * d));
        stepNo = text("", 12, ACCENT, true);
        stepNo.setLetterSpacing(0.08f);
        stepTitle = text("", 21, Color.WHITE, true);
        stepBody = text("", 14, Color.rgb(196, 202, 218), false);
        stepBody.setLineSpacing(0, 1.15f);
        stepPath = text("", 12, FAINT, false);
        stepButton = button("", true);
        stepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepAction();
            }
        });
        setupCard.addView(stepNo);
        setupCard.addView(stepTitle);
        setupCard.addView(stepBody);
        setupCard.addView(stepPath);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = (int) (8 * d);
        setupCard.addView(stepButton, bp);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = (int) (8 * d);
        sp.bottomMargin = (int) (10 * d);
        col.addView(setupCard, sp);
        setupCard.setVisibility(View.GONE);

        // ---- phone pairing ----
        lead = text(Strings.tv("tv.lead"), 22, Color.WHITE, true);
        col.addView(lead);
        scan = text(Strings.tv("tv.scan"), 16, DIM, false);
        col.addView(scan);
        url = text("", 16, DIM, false);
        col.addView(url);
        codeLabel = text(Strings.tv("tv.code"), 14, DIM, false);
        col.addView(codeLabel);
        code = text("······", 50, Color.WHITE, true);
        code.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        code.setLetterSpacing(0.25f);
        col.addView(code);
        codeValid = text(Strings.tv("tv.code_valid"), 13, DIM, false);
        col.addView(codeValid);
        status = text(Strings.tv("tv.starting"), 14, DIM, false);
        status.setPadding(0, (int) (10 * d), 0, (int) (4 * d));
        col.addView(status);
        update = text("", 14, ACCENT, true);
        update.setVisibility(View.GONE);
        col.addView(update);

        forget = button(Strings.tv("tv.forget"), false);
        forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Panel p = PanelService.panel;
                if (p == null) return;
                android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(MainActivity.this)
                        .setMessage(Strings.tv("tv.forget_confirm"))
                        .setNegativeButton(Strings.tv("tv.cancel"), null)
                        .setPositiveButton(Strings.tv("tv.forget"), new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dd, int w) {
                                p.auth.revokeAll();
                                p.log("all phones forgotten (TV screen)");
                                note.setText(Strings.tv("tv.forgotten"));
                            }
                        })
                        .create();
                dlg.show();
                // Default to "Cancel": pressing OK twice on the remote must not wipe every phone.
                android.widget.Button cancel = dlg.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
                if (cancel != null) cancel.requestFocus();
            }
        });
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.topMargin = (int) (6 * d);
        col.addView(forget, fp);

        note = text(Strings.tv("tv.background"), 13, FAINT, false);
        note.setPadding(0, (int) (10 * d), 0, 0);
        col.addView(note);
        warning = text(Strings.tv("tv.warning"), 12, FAINT, false);
        col.addView(warning);
        col.addView(text("Odin TV Magic " + Panel.VERSION + "  ·  Odin Enes Özlen (@odinozlen)  ·  github.com/odinozlen/odin-tv-magic", 12, FAINT, false));

        // Small TVs / large fonts: everything stays reachable.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));
        setContentView(scroll);
        // Start with focus on the QR code, not on the destructive "forget" button.
        qr.setFocusable(true);
        qr.requestFocus();

        // Back from Settings by itself: when the switch the user went for turns on, return here.
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean self, Uri uri) {
                Setup.Step was = shownStep;
                if (was == null) return;
                if (repromptOnAdb && Setup.usbDebugging(MainActivity.this)) {
                    // The user turned USB debugging off and on as told: show the window again by itself.
                    repromptOnAdb = false;
                    resetStuckPrompt();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            reprompt();
                        }
                    }, 3000);
                    return;
                }
                boolean done = (was == Setup.Step.DEV_OFF && Setup.developerMode(MainActivity.this))
                        || ((was == Setup.Step.ADB_OFF || was == Setup.Step.DEV_OFF) && Setup.usbDebugging(MainActivity.this));
                if (!done) return;
                fresh = false;
                // Not while the TV may be showing "Allow debugging?": coming to the front would close
                // that window unanswered, and Android then skips every new one (only turning USB
                // debugging off and on helps).
                Panel pp = PanelService.panel;
                if (pp != null && "adb.waiting_approval".equals(pp.adb.state)) return;
                if (!visible) {
                    try {
                        startActivity(new Intent(MainActivity.this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    } catch (Exception ignored) {
                        // not allowed from the background on this TV: the user presses Back
                    }
                }
            }
        };
        try {
            getContentResolver().registerContentObserver(Settings.Global.getUriFor("development_settings_enabled"), false, observer);
            getContentResolver().registerContentObserver(Settings.Global.getUriFor("adb_enabled"), false, observer);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            getContentResolver().unregisterContentObserver(observer);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        shownCode = null;
        fresh = false;
        lastAttempt = 0;
        handler.post(tick);
    }

    @Override
    public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        // Back from Settings or the prompt: the next thing to press is the step's button.
        if (has) focusStepButton();
    }

    /** After layout: a view that was just made visible cannot take focus yet. */
    private void focusStepButton() {
        stepButton.post(new Runnable() {
            @Override
            public void run() {
                if (setupCard.getVisibility() == View.VISIBLE && stepButton.getVisibility() == View.VISIBLE
                        && !forget.hasFocus()) {
                    // On a TV, also leave touch mode (an air mouse or a tap may have left it on),
                    // so the remote's OK presses this button.
                    if (tv) stepButton.requestFocusFromTouch();
                    else stepButton.requestFocus();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        visible = false;
        handler.removeCallbacks(tick);
        Panel p = PanelService.panel;
        if (p != null) p.auth.clearPairingCode();
        super.onPause();
    }

    /** One connection attempt in the background; the TV shows "Allow debugging?" if needed. */
    private void tryConnect(final Panel p) {
        if (!attempting.compareAndSet(false, true)) return;
        lastAttempt = SystemClock.elapsedRealtime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.adb.shell("echo ok");
                } catch (Exception ignored) {
                    // the state says why
                } finally {
                    fresh = true;
                    attempting.set(false);
                }
            }
        }, "setup-connect").start();
    }

    private void stepAction() {
        Panel p = PanelService.panel;
        Setup.Step s = shownStep;
        if (s == null || p == null) return;
        if (s == Setup.Step.APPROVE && promptStuck) {
            // "Show the window again" did not bring it: open Developer options to turn USB debugging off/on.
            for (Intent i : Setup.intents(Setup.Step.ADB_OFF)) {
                try {
                    startActivity(i);
                    return;
                } catch (ActivityNotFoundException | SecurityException ignored) {
                    // next one
                }
            }
            return;
        }
        if (s == Setup.Step.APPROVE) {
            reprompt();
            return;
        }
        for (Intent i : Setup.intents(s)) {
            try {
                startActivity(i);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // next one
            }
        }
        stepPath.setText(Strings.tv("setup.no_settings") + "\n" + Setup.path(s, kind));
    }

    /**
     * Dismissed, hidden behind this screen or never shown: drop the wait and connect again, which
     * makes the TV show a new "Allow debugging?" window on top.
     */
    private void reprompt() {
        Panel p = PanelService.panel;
        if (p == null) return;
        {
            fresh = false;
            repromptAt = SystemClock.elapsedRealtime();
            final Panel pp = p;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pp.adb.retry();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            tryConnect(pp);
                        }
                    });
                }
            }, "setup-reprompt").start();
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Panel p = PanelService.panel;
            if (PanelService.error != null) {
                status.setText(Strings.tv("tv.error", PanelService.error));
            } else if (p == null) {
                status.setText(Strings.tv("tv.starting"));
            } else {
                int cs = p.auth.codeState();
                if (shownCode == null || pairedEvent || (cs == Auth.CODE_EXPIRED && shownCode.length() == 6)) {
                    if (pairedEvent) note.setText("✓ " + Strings.tv("pair.ok"));
                    pairedEvent = false;
                    showCode(p.auth.newPairingCode());
                } else if (cs == Auth.CODE_BURNED && shownCode.length() == 6) {
                    shownCode = "burned";
                    code.setText("······");
                    qr.setImageBitmap(null);
                    note.setText(Strings.tv("tv.burned"));
                }

                Setup.Step step = Setup.step(MainActivity.this, p.adb.state, !fresh);
                boolean tryable = step == Setup.Step.CHECKING || step == Setup.Step.NET_OFF;
                if (tryable && !attempting.get() && SystemClock.elapsedRealtime() - lastAttempt > (fresh ? 4000 : 0)) {
                    tryConnect(p);
                }
                showStep(step);
                checkStuckPrompt(step);

                StringBuilder s = new StringBuilder();
                if (step == Setup.Step.DONE) {
                    s.append(Strings.tv("tv.adb", Strings.tv(p.adb.state)));
                }
                if (p.philips) {
                    if (s.length() > 0) s.append("   ·   ");
                    s.append(Strings.tv("tv.philips", Strings.tv(p.js.paired() ? "tv.philips_on" : "tv.philips_off")));
                }
                if (s.length() > 0) s.append("   ·   ");
                s.append(Strings.tv("tv.devices", p.auth.count()));
                status.setText(s);
                String newer = p.updater.available() ? p.updater.latest() : null;
                update.setText(newer == null ? "" : "⬆ " + Strings.tv("tv.update", newer));
                update.setVisibility(newer == null ? View.GONE : View.VISIBLE);
            }
            if (visible) handler.postDelayed(this, 1000);
        }
    };

    private long repromptAt;
    private boolean promptStuck, repromptOnAdb;

    /**
     * If "Show the window again" brought nothing within 10 s, Android is still waiting for an earlier
     * prompt that was closed unanswered and skips new ones ("prompt currently pending"). Say so and
     * turn the button into "Open Developer options": switching USB debugging off and on resets it.
     */
    private void checkStuckPrompt(Setup.Step step) {
        Panel p = PanelService.panel;
        if (step != Setup.Step.APPROVE) {
            repromptAt = 0;
            promptStuck = false;
            return;
        }
        if (promptStuck && p != null && !"adb.waiting_approval".equals(p.adb.state)) {
            // The old wait ended (ADB restarted, e.g. USB debugging off/on): "Show the window again" works again.
            resetStuckPrompt();
            return;
        }
        if (promptStuck || repromptAt == 0 || SystemClock.elapsedRealtime() - repromptAt < 10_000) return;
        promptStuck = true;
        repromptOnAdb = true;
        stepBody.setText(Strings.tv("setup.b.approve_stuck"));
        stepButton.setText(Strings.tv("setup.btn.dev"));
        stepButton.setVisibility(View.VISIBLE);
        focusStepButton();
    }

    private void resetStuckPrompt() {
        boolean was = promptStuck;
        promptStuck = false;
        repromptAt = 0;
        if (was && shownStep == Setup.Step.APPROVE) {
            stepBody.setText(Setup.body(Setup.Step.APPROVE, kind));
            String label = Setup.button(Setup.Step.APPROVE);
            if (label != null) stepButton.setText(label);
        }
    }

    private void showStep(Setup.Step step) {
        if (step == shownStep) return;
        Setup.Step before = shownStep;
        shownStep = step;
        boolean setup = step != Setup.Step.DONE;
        setupCard.setVisibility(setup ? View.VISIBLE : View.GONE);
        // While the guide is shown the pairing part gets smaller; the phone can pair already.
        LinearLayout.LayoutParams qp = (LinearLayout.LayoutParams) qr.getLayoutParams();
        qp.width = qp.height = setup ? qrSmall : qrBig;
        qr.setLayoutParams(qp);
        scan.setVisibility(setup ? View.GONE : View.VISIBLE);
        lead.setVisibility(setup ? View.GONE : View.VISIBLE);
        codeValid.setVisibility(setup ? View.GONE : View.VISIBLE);
        warning.setVisibility(setup ? View.GONE : View.VISIBLE);
        code.setTextSize(setup ? 36 : 50);
        if (shownCode != null && shownCode.length() == 6) url.setText(pairLine());
        if (!setup) {
            if (before != null) note.setText("✓ " + Strings.tv("tv.setup_done"));
            if (stepButton.hasFocus()) qr.requestFocus();
            return;
        }
        Panel pp = PanelService.panel;
        note.setText(Strings.tv(pp != null && pp.philips ? "tv.basic_now.philips" : "tv.basic_now"));
        stepNo.setText(Strings.tv("setup.step", Setup.number(step)));
        stepTitle.setText(Setup.title(step));
        stepBody.setText(Setup.body(step, kind));
        String path = Setup.path(step, kind);
        stepPath.setText(path == null ? "" : path);
        stepPath.setVisibility(path == null ? View.GONE : View.VISIBLE);
        String label = Setup.button(step);
        stepButton.setVisibility(label == null ? View.GONE : View.VISIBLE);
        if (label != null) {
            stepButton.setText(label);
            focusStepButton();
        } else if (stepButton.hasFocus()) {
            qr.requestFocus();
        }
    }

    private String pairLine() {
        String base = "http://" + ip() + ":" + PanelService.port;
        return shownStep != null && shownStep != Setup.Step.DONE
                ? Strings.tv("tv.pair_short", base) : Strings.tv("tv.or_open", base);
    }

    private void showCode(String c) {
        shownCode = c;
        code.setText(c);
        String base = "http://" + ip() + ":" + PanelService.port;
        url.setText(pairLine());
        qr.setImageBitmap(qrBitmap(base + "/#pair=" + c));
    }

    private static Bitmap qrBitmap(String text) {
        QrCode q = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        int border = 3, scale = 10, n = q.size + border * 2;
        Bitmap b = Bitmap.createBitmap(n * scale, n * scale, Bitmap.Config.RGB_565);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean dark = q.getModule(x - border, y - border);
                int color = dark ? Color.BLACK : Color.WHITE;
                for (int dy = 0; dy < scale; dy++) for (int dx = 0; dx < scale; dx++) b.setPixel(x * scale + dx, y * scale + dy, color);
            }
        }
        return b;
    }

    private Button button(String label, final boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(primary ? 16 : 14);
        if (primary) b.setTypeface(Typeface.DEFAULT_BOLD);
        float d = getResources().getDisplayMetrics().density;
        b.setPadding((int) (20 * d), (int) (8 * d), (int) (20 * d), (int) (8 * d));
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * d);
        bg.setColor(primary ? Color.rgb(58, 70, 120) : BTN);
        bg.setStroke((int) (2 * d), Color.TRANSPARENT);
        b.setBackground(bg);
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                bg.setColor(has ? ACCENT : primary ? Color.rgb(58, 70, 120) : BTN);
                bg.setStroke((int) (2 * getResources().getDisplayMetrics().density), has ? Color.WHITE : Color.TRANSPARENT);
            }
        });
        return b;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setPadding(0, 4, 0, 4);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static String ip() {
        return Net.ip(PanelService.appContext);
    }
}
