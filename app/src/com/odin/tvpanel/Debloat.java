package com.odin.tvpanel;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * "Speed up the TV": suggests pre-installed apps that can be turned off.
 *
 * Only packages from the bundled, reviewed lists (assets/debloat/*.txt) can ever be touched, and only
 * with `pm disable-user` (never uninstall); every one can be turned back on from the same screen.
 * Never offered: anything on the keep list, the current home screen or keyboard, and on Google TV
 * the Google TV app and launcher packages.
 */
final class Debloat {
    private static final Set<String> GOOGLE_TV_PROTECTED = new HashSet<>();
    static {
        String[] p = {"com.google.android.videos", "com.google.android.apps.tv.launcherx",
                "com.google.android.tungsten.setupwraith", "com.google.android.tvrecommendations",
                "com.google.android.katniss", "com.google.android.leanbacklauncher.recommendations"};
        for (String s : p) GOOGLE_TV_PROTECTED.add(s);
    }
    private static final int MAX_PER_REQUEST = 10;

    static final class Item {
        String pkg, group, en, tr;
    }

    private final Context ctx;
    private final Panel p;

    Debloat(Context ctx, Panel p) {
        this.ctx = ctx;
        this.p = p;
    }

    /** Suggestions for this TV: common list, plus the Philips list on Philips sets. */
    private Map<String, Item> list() {
        Map<String, Item> m = new LinkedHashMap<>();
        if (p.philips) read("debloat/philips-android11.txt", m);
        read("debloat/android-tv-common.txt", m);
        Set<String> keep = keep();
        boolean googleTv = installed("com.google.android.apps.tv.launcherx");
        String home = "", ime = "";
        try {
            home = Panel.pkgOf(p.launchers.current());
            ime = Panel.pkgOf(p.adb.shell("settings get secure default_input_method"));
        } catch (Exception ignored) {
        }
        java.util.Iterator<Map.Entry<String, Item>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Item i = it.next().getValue();
            boolean drop = keep.contains(i.pkg) || i.pkg.equals(home) || i.pkg.equals(ime)
                    || (googleTv && GOOGLE_TV_PROTECTED.contains(i.pkg))
                    // launcher switching lives in "TV look"; its helpers only once another launcher is home
                    || Launchers.isStock(i.pkg)
                    || ("launcher".equals(i.group) && Launchers.isStock(home));
            if (drop || !installed(i.pkg)) it.remove();
        }
        return m;
    }

    private void read(String asset, Map<String, Item> into) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open(asset), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t");
                if (f.length < 3 || !Apps.isPackageName(f[0]) || into.containsKey(f[0])) continue;
                Item i = new Item();
                i.pkg = f[0];
                i.group = f[1];
                i.en = f[2];
                i.tr = f.length > 3 ? f[3] : f[2];
                into.put(i.pkg, i);
            }
            r.close();
        } catch (Exception ignored) {
            // list missing: nothing to suggest
        }
    }

    private Set<String> keep() {
        Set<String> s = new HashSet<>();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open("debloat/philips-keep.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) s.add(line.split("\t")[0]);
            }
            r.close();
        } catch (Exception ignored) {
        }
        String[] always = {"com.google.android.gms", "com.google.android.gsf", "com.android.vending",
                "com.android.systemui", "com.android.tv.settings", "com.android.settings", ctx.getPackageName()};
        for (String a : always) s.add(a);
        return s;
    }

    private boolean installed(String pkg) {
        try {
            ctx.getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean enabled(String pkg) {
        try {
            return ctx.getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String label(String pkg) {
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
            String l = String.valueOf(pm.getApplicationLabel(ai));
            return l.equals(pkg) ? pkg.substring(pkg.lastIndexOf('.') + 1) : l;
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    JSONObject json(String lang) throws Exception {
        JSONArray items = new JSONArray();
        for (Item i : list().values()) {
            items.put(new JSONObject().put("pkg", i.pkg).put("label", label(i.pkg)).put("group", i.group)
                    .put("reason", Strings.isTurkish(lang) ? i.tr : i.en).put("enabled", enabled(i.pkg)));
        }
        return new JSONObject().put("items", items).put("max", MAX_PER_REQUEST);
    }

    /** Turns off up to 10 suggested apps; each one is checked against the list again. */
    Panel.Result disable(JSONArray pkgs) throws Exception {
        if (pkgs.length() == 0 || pkgs.length() > MAX_PER_REQUEST) return new Panel.Result(400, "debloat.limit");
        Map<String, Item> allowed = list();
        // Only packages from the reviewed suggestion list, never anything else.
        for (int i = 0; i < pkgs.length(); i++) {
            if (!allowed.containsKey(pkgs.optString(i))) return new Panel.Result(403, "debloat.not_allowed");
        }
        int done = 0;
        for (int i = 0; i < pkgs.length(); i++) {
            String pkg = pkgs.optString(i);
            if (!enabled(pkg)) continue;
            String out = p.adb.shell("pm disable-user --user 0 " + pkg);
            if (out.contains("disabled")) {
                done++;
                p.log("debloat: disabled " + pkg);
            }
        }
        return new Panel.Result(200, "debloat.disabled", done);
    }

    Panel.Result enable(JSONArray pkgs) throws Exception {
        Map<String, Item> allowed = list();
        int done = 0;
        for (int i = 0; i < pkgs.length(); i++) {
            if (!allowed.containsKey(pkgs.optString(i))) return new Panel.Result(403, "debloat.not_allowed");
        }
        for (int i = 0; i < pkgs.length() && i < 100; i++) {
            String pkg = pkgs.optString(i);
            if (enabled(pkg)) continue;
            p.adb.shell("pm enable " + pkg);
            done++;
            p.log("debloat: enabled " + pkg);
        }
        return new Panel.Result(200, "debloat.enabled", done);
    }
}
