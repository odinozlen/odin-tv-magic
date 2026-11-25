package com.odin.tvpanel;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;

/** Foreground service that runs while the TV is on: web panel + maintenance. */
public final class PanelService extends Service {
    /** First choice 8080; if another app (Kodi, a media server…) holds it, the next free one. */
    static final int[] PORTS = {8080, 8081, 8082, 8090, 8099, 18080};
    static volatile int port = PORTS[0];
    static volatile Context appContext;
    private static final String CHANNEL = "panel";

    static volatile Panel panel;
    static volatile String error;
    private static volatile boolean started;

    static void start(Context ctx) {
        appContext = ctx.getApplicationContext();
        ctx.startForegroundService(new Intent(ctx, PanelService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Odin TV Magic", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Odin TV Magic")
                .setContentText("http://" + MainActivity.ip() + ":" + port)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .build();
        startForeground(1, n);

        if (started) return;
        started = true;
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    keepWirelessDebuggingOn(app);
                    AdbClient adb = new AdbClient(getFilesDir(), new MdnsFinder(app));
                    Panel p = new Panel(app, adb, getFilesDir());
                    HttpServer http = new HttpServer(portOrder(getFilesDir()), p, getAssets());
                    port = http.port();
                    rememberPort(getFilesDir(), port);
                    panel = p;
                    p.log("panel started " + Panel.VERSION + ", port " + port);
                    p.startWatchdog();
                    p.updater.start();
                    warmUp(p);
                    http.run();
                } catch (Exception e) {
                    error = e.toString();
                    started = false;
                }
            }
        }, "panel-http").start();
    }

    /** The port used last time comes first, so the phone's saved link keeps working. */
    private static int[] portOrder(java.io.File dir) {
        int last = 0;
        try {
            java.io.File f = new java.io.File(dir, "port");
            if (f.exists()) last = Integer.parseInt(new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8").trim());
        } catch (Exception ignored) {
        }
        int[] out = new int[PORTS.length + 1];
        out[0] = last > 0 ? last : PORTS[0];
        System.arraycopy(PORTS, 0, out, 1, PORTS.length);
        return out;
    }

    private static void rememberPort(java.io.File dir, int p) {
        try {
            java.nio.file.Files.write(new java.io.File(dir, "port").toPath(), String.valueOf(p).getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    /** Connect to ADB early so the "Allow debugging?" prompt appears while the user is at the TV. */
    private static void warmUp(final Panel p) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    try {
                        p.adb.shell("echo ok");
                        grantSecureSettings(p);
                        return;
                    } catch (Exception e) {
                        SystemClock.sleep(5000);
                    }
                }
            }
        }, "adb-warmup").start();
    }

    /**
     * Wireless debugging switches itself off after a reboot, and Android refuses to turn it on
     * until Wi-Fi is connected (only on a network the user marked as trusted). If we were allowed
     * to (WRITE_SECURE_SETTINGS, granted once over ADB after pairing), keep switching it back on:
     * every 15 s for the first 5 minutes after start, then every 2 minutes.
     */
    static void keepWirelessDebuggingOn(final Context ctx) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long started = SystemClock.elapsedRealtime();
                while (true) {
                    enableWirelessDebugging(ctx);
                    boolean early = SystemClock.elapsedRealtime() - started < 5 * 60_000L;
                    SystemClock.sleep(early ? 15_000 : 120_000);
                }
            }
        }, "wireless-debugging").start();
    }

    static void enableWirelessDebugging(Context ctx) {
        if (!new java.io.File(ctx.getFilesDir(), "adb_tls_paired").exists()) return;
        if (ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return;
        if (!onWifi(ctx)) return;
        try {
            if (Settings.Global.getInt(ctx.getContentResolver(), "adb_wifi_enabled", 0) == 0) {
                Settings.Global.putInt(ctx.getContentResolver(), "adb_wifi_enabled", 1);
                android.util.Log.i("OdinTvMagic", "wireless debugging switched back on");
            }
        } catch (Exception e) {
            android.util.Log.w("OdinTvMagic", "cannot enable wireless debugging: " + e);
        }
    }

    private static boolean onWifi(Context ctx) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (nc != null && nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return true;
        // The active network may be Ethernet while Wi-Fi is also up; Android requires Wi-Fi either way.
        for (android.net.Network n : cm.getAllNetworks()) {
            android.net.NetworkCapabilities c = cm.getNetworkCapabilities(n);
            if (c != null && c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return true;
        }
        return false;
    }

    /** After a wireless-debugging connection works, keep the right to re-enable it after reboots. */
    static void grantSecureSettings(Panel p) {
        if (!p.adb.tlsPaired()) return;
        if (p.ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) return;
        try {
            p.adb.shell("pm grant " + p.ctx.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS");
        } catch (Exception ignored) {
        }
    }

    /** Pairs with Developer options > Wireless debugging > "Pair device with pairing code". */
    static Panel.Result pairWireless(Panel p, String code, int port) throws Exception {
        if (code == null || !code.trim().matches("\\d{6}")) return new Panel.Result(400, "pair.bad_code_format");
        if (port <= 0 || port > 65535) {
            try {
                port = new MdnsFinder(p.ctx).find("_adb-tls-pairing._tcp");
            } catch (Exception e) {
                return new Panel.Result(404, "pair.no_port");
            }
        }
        p.adb.pair(port, code.trim());
        p.log("wireless debugging paired");
        try {
            p.adb.shell("echo ok");
            grantSecureSettings(p);
        } catch (Exception ignored) {
            // connection is retried on the next request
        }
        return new Panel.Result(200, "adb.paired");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
