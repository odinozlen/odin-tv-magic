package com.odin.tvpanel;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.util.Locale;

/**
 * The TV-side setup guide: works out which step is missing (developer mode, USB debugging,
 * network debugging, approving the prompt) from the system settings any app may read, and which
 * settings screen to open for it. No app can switch these on by itself; the guide only takes
 * the user straight to the right switch.
 */
final class Setup {
    enum Step { DEV_OFF, ADB_OFF, NET_OFF, CHECKING, APPROVE, DONE }

    /** Which kind of TV, for the menu names in the instructions. */
    enum Kind { ANDROID_TV, GOOGLE_TV, FIRE_TV }

    private Setup() {
    }

    static Kind kind(Context ctx) {
        if ("amazon".equalsIgnoreCase(Build.MANUFACTURER)) return Kind.FIRE_TV;
        try {
            ctx.getPackageManager().getPackageInfo("com.google.android.apps.tv.launcherx", 0);
            return Kind.GOOGLE_TV;
        } catch (PackageManager.NameNotFoundException e) {
            return Kind.ANDROID_TV;
        }
    }

    /** -1 when the setting cannot be read on this TV. */
    static int global(ContentResolver cr, String name) {
        try {
            return Settings.Global.getInt(cr, name, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    static boolean developerMode(Context ctx) {
        return global(ctx.getContentResolver(), "development_settings_enabled") == 1;
    }

    static boolean usbDebugging(Context ctx) {
        return global(ctx.getContentResolver(), "adb_enabled") == 1;
    }

    /**
     * @param adbState  AdbClient.state
     * @param attempting a connection attempt is running (its result is not known yet)
     */
    static Step step(Context ctx, String adbState, boolean attempting) {
        if ("adb.connected".equals(adbState)) return Step.DONE;
        ContentResolver cr = ctx.getContentResolver();
        int dev = global(cr, "development_settings_enabled");
        int usb = global(cr, "adb_enabled");
        int wifi = global(cr, "adb_wifi_enabled");
        // Switches first: with debugging off, an older "waiting for approval" state is stale.
        if (dev == 0 && wifi != 1) return Step.DEV_OFF;
        if (usb == 0 && wifi != 1) return Step.ADB_OFF;
        if ("adb.waiting_approval".equals(adbState) || "adb.not_approved".equals(adbState)
                || "adb.rejected".equals(adbState)) return Step.APPROVE;
        if (attempting || "adb.not_connected".equals(adbState) || "adb.paired".equals(adbState)) return Step.CHECKING;
        if ("adb.refused".equals(adbState) || "adb.wireless_off".equals(adbState)) {
            // Settings unreadable: we cannot tell which switch is missing; start from developer options.
            return usb == -1 ? Step.ADB_OFF : Step.NET_OFF;
        }
        return Step.CHECKING;
    }

    static int number(Step s) {
        switch (s) {
            case DEV_OFF: return 1;
            case ADB_OFF: case NET_OFF: return 2;
            default: return 3;
        }
    }

    /** Settings screens to try, best first; the last one is the plain settings home. */
    static Intent[] intents(Step s) {
        String first = s == Step.DEV_OFF ? Settings.ACTION_DEVICE_INFO_SETTINGS : Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS;
        return new Intent[]{new Intent(first), new Intent(Settings.ACTION_SETTINGS)};
    }

    static String key(Kind k) {
        return k.name().toLowerCase(Locale.ROOT);
    }

    // ---- texts (TV language) ----

    static String title(Step s) {
        return Strings.tv("setup.t." + s.name().toLowerCase(Locale.ROOT));
    }

    static String body(Step s, Kind k) {
        String n = s.name().toLowerCase(Locale.ROOT);
        String specific = "setup.b." + n + "." + key(k);
        if (s == Step.NET_OFF) {
            return Strings.tv(Build.VERSION.SDK_INT >= 30 ? "setup.b.net_off" : "setup.b.net_off_old");
        }
        return Strings.has(specific) ? Strings.tv(specific) : Strings.tv("setup.b." + n);
    }

    /** "If the button does not work" path, or null. */
    static String path(Step s, Kind k) {
        if (s == Step.DEV_OFF) return Strings.tv("setup.path.about." + key(k));
        if (s == Step.ADB_OFF || s == Step.NET_OFF) return Strings.tv("setup.path.dev." + key(k));
        return null;
    }

    static String button(Step s) {
        switch (s) {
            case DEV_OFF: return Strings.tv("setup.btn.about");
            case ADB_OFF: case NET_OFF: return Strings.tv("setup.btn.dev");
            case APPROVE: return Strings.tv("setup.btn.prompt");
            default: return null;
        }
    }
}
