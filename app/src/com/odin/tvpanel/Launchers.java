package com.odin.tvpanel;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Switching the home screen (launcher) to an installed alternative such as Projectivy or FLauncher.
 *
 * Safety rules:
 *  - only launchers that are installed and declare a HOME activity can be chosen,
 *  - the stock Android TV launcher (com.google.android.tvlauncher) is disabled only when the TV
 *    ignores the choice (Philips gives it priority); this is recorded and undone by "restore",
 *  - the Google TV home (launcherx) is NEVER disabled: on some devices that requires a factory reset,
 *  - every change is verified by resolving HOME again; on failure the previous state is restored.
 */
final class Launchers {
    static final String ATV_STOCK = "com.google.android.tvlauncher";
    static final String GTV_STOCK = "com.google.android.apps.tv.launcherx";
    private static final String[] STOCK = {ATV_STOCK, GTV_STOCK, "com.google.android.leanbacklauncher"};
    /** Offered even when not installed; choosing one opens its Play Store page on the TV. */
    static final String[][] RECOMMENDED = {
            {"com.spocky.projengmenu", "Projectivy Launcher", "launcher.projectivy"},
            {"me.efesser.flauncher", "FLauncher", "launcher.flauncher"},
    };
    private static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+");
    private static final String HOME_QUERY =
            "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1";

    private final Context ctx;
    private final Panel p;

    Launchers(Context ctx, Panel p) {
        this.ctx = ctx;
        this.p = p;
    }

    static final class Home {
        String component, pkg, label;
        boolean stock;
    }

    /** @param currentPkg the current home package (always listed, even if it is a system app) */
    List<Home> installed(String currentPkg) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<Home> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ResolveInfo r : pm.queryIntentActivities(i, PackageManager.MATCH_DISABLED_COMPONENTS)) {
            ActivityInfo a = r.activityInfo;
            if (a == null || a.packageName.equals(ctx.getPackageName())) continue;
            if (!seen.add(a.packageName)) continue;  // one entry per launcher app
            // System packages register many HOME stubs (setup, migration, fallbacks). Offer only the known
            // stock launchers and launchers the user installed (Projectivy, FLauncher, ...).
            boolean system = (a.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system && !isStock(a.packageName) && !a.packageName.equals(currentPkg)) continue;
            Home h = new Home();
            h.pkg = a.packageName;
            h.component = a.packageName + "/" + a.name;
            h.label = String.valueOf(r.loadLabel(pm));
            h.stock = isStock(a.packageName);
            out.add(h);
        }
        return out;
    }

    static boolean isStock(String pkg) {
        for (String s : STOCK) if (s.equals(pkg)) return true;
        return false;
    }

    private boolean isInstalled(String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isEnabled(String pkg) {
        try {
            return ctx.getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Opens the Play Store page of a recommended launcher on the TV (the user presses Install there). */
    Panel.Result install(String pkg) throws Exception {
        String label = null;
        for (String[] r : RECOMMENDED) if (r[0].equals(pkg)) label = r[1];
        if (label == null) return new Panel.Result(404, "home.unknown");
        String out = p.adb.shell("am start -a android.intent.action.VIEW -d " + Panel.shellQuote("market://details?id=" + pkg) + " 2>&1");
        if (out.contains("Error") || out.contains("unable to resolve")) {
            p.adb.shell("am start -a android.intent.action.VIEW -d " + Panel.shellQuote("https://play.google.com/store/apps/details?id=" + pkg));
        }
        p.log("opened store page: " + pkg);
        return new Panel.Result(200, "home.store", label);
    }

    String current() throws Exception {
        String c = p.adb.shell(HOME_QUERY).trim();
        return COMPONENT.matcher(c).matches() ? c : "";
    }

    JSONObject json() throws Exception {
        String cur = current();
        JSONObject state = p.settings.launcherState();
        JSONArray items = new JSONArray();
        for (Home h : installed(Panel.pkgOf(cur))) {
            items.put(new JSONObject().put("component", h.component).put("label", h.label)
                    .put("stock", h.stock).put("current", h.component.equals(cur) || Panel.pkgOf(cur).equals(h.pkg)));
        }
        JSONArray rec = new JSONArray();
        for (String[] r : RECOMMENDED) {
            if (!isInstalled(r[0])) rec.put(new JSONObject().put("pkg", r[0]).put("label", r[1]).put("desc", r[2]));
        }
        return new JSONObject().put("items", items).put("recommended", rec)
                .put("googleTv", isInstalled(GTV_STOCK))
                .put("canDisableStock", isInstalled(ATV_STOCK) && !isInstalled(GTV_STOCK))
                .put("stockDisabled", isInstalled(ATV_STOCK) && !isEnabled(ATV_STOCK))
                .put("changed", !isStock(Panel.pkgOf(cur)) || (isInstalled(ATV_STOCK) && !isEnabled(ATV_STOCK)));
    }

    Panel.Result set(String component) throws Exception {
        String before = current();
        Home target = null;
        for (Home h : installed(Panel.pkgOf(before))) if (h.component.equals(component)) target = h;
        if (target == null || !COMPONENT.matcher(component).matches()) return new Panel.Result(404, "home.unknown");

        JSONObject state = p.settings.launcherState();
        if (!state.has("original") && before.length() > 0) state.put("original", before);

        if (!isEnabled(target.pkg)) {
            // e.g. the stock Android TV launcher that was turned off earlier: turn it back on first
            p.adb.shell("pm enable " + target.pkg);
            if (target.pkg.equals(state.optString("disabledStock"))) state.remove("disabledStock");
        }
        p.adb.shell("cmd package set-home-activity " + Panel.shellQuote(component));
        boolean disabledNow = false;
        SystemClock.sleep(400);
        String now = current();
        boolean canDisable = !target.stock && isInstalled(ATV_STOCK) && isEnabled(ATV_STOCK) && !isInstalled(GTV_STOCK);
        if (!Panel.pkgOf(now).equals(target.pkg) && canDisable) {
            // Some TVs (Philips) give the stock Android TV launcher priority over the user's choice.
            // Turning it off is the only way; "restore" turns it back on.
            String r = p.adb.shell("pm disable-user --user 0 " + ATV_STOCK);
            if (r.contains("disabled")) {
                state.put("disabledStock", ATV_STOCK);
                disabledNow = true;
                p.adb.shell("cmd package set-home-activity " + Panel.shellQuote(component));
                SystemClock.sleep(400);
                now = current();
            }
        }
        if (!Panel.pkgOf(now).equals(target.pkg)) {
            // did not take effect: put everything back the way it was
            if (disabledNow) {
                p.adb.shell("pm enable " + ATV_STOCK);
                state.remove("disabledStock");
            }
            if (before.length() > 0) p.adb.shell("cmd package set-home-activity " + Panel.shellQuote(before));
            p.settings.setLauncherState(state);
            return new Panel.Result(500, "home.failed");
        }
        p.settings.setLauncherState(state);
        p.adb.shell("input keyevent KEYCODE_HOME");
        p.log("home screen -> " + component + (disabledNow ? " (stock launcher disabled)" : ""));
        return new Panel.Result(200, "home.ok", target.label);
    }

    /** Back to the TV's factory home screen: turn the stock launcher on again and make it the default. */
    Panel.Result restore() throws Exception {
        String stockPkg = isInstalled(GTV_STOCK) ? GTV_STOCK : isInstalled(ATV_STOCK) ? ATV_STOCK : null;
        if (stockPkg != null && !isEnabled(stockPkg)) p.adb.shell("pm enable " + stockPkg);
        String target = null;
        if (stockPkg != null) {
            for (Home h : installed(stockPkg)) if (h.pkg.equals(stockPkg)) target = h.component;
        }
        if (target == null) target = p.settings.launcherState().optString("original", "");
        if (COMPONENT.matcher(target).matches()) p.adb.shell("cmd package set-home-activity " + Panel.shellQuote(target));
        p.settings.setLauncherState(new JSONObject());
        p.adb.shell("input keyevent KEYCODE_HOME");
        p.log("home screen restored to " + target);
        return new Panel.Result(200, "home.restored");
    }
}
