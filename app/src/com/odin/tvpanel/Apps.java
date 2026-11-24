package com.odin.tvpanel;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Apps that can be opened from the panel: whatever is installed and launchable on the TV. */
final class Apps {
    private static final Pattern PKG = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");
    private static final Pattern CLS = Pattern.compile("[A-Za-z0-9_.$]+");

    /** Shown first when the user has not picked favorites yet (only if installed). */
    private static final String[] POPULAR = {
            "com.netflix.ninja", "com.google.android.youtube.tv", "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom", "com.wbd.stream", "com.apple.atve.androidtv.appletv",
            "com.spotify.tv.android", "com.plexapp.android", "com.stremio.one", "org.xbmc.kodi",
            "io.github.jqssun.airplay", "com.google.android.youtube.tvmusic",
    };

    static final class App {
        String pkg, label, activity;
        boolean leanback;
    }

    private final Context ctx;
    private final Map<String, byte[]> icons = new HashMap<>();

    Apps(Context ctx) {
        this.ctx = ctx;
    }

    static boolean isPackageName(String s) {
        return s != null && s.length() <= 200 && PKG.matcher(s).matches();
    }

    /** package -> launchable app, leanback (TV) entry preferred. */
    Map<String, App> installed() {
        PackageManager pm = ctx.getPackageManager();
        Map<String, App> out = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                ActivityInfo a = r.activityInfo;
                if (a == null || out.containsKey(a.packageName) || a.packageName.equals(ctx.getPackageName())) continue;
                App app = new App();
                app.pkg = a.packageName;
                app.activity = a.name;
                app.label = String.valueOf(r.loadLabel(pm));
                app.leanback = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(cat);
                out.put(app.pkg, app);
            }
        }
        return out;
    }

    JSONArray list(Prefs settings) throws Exception {
        Map<String, App> all = installed();
        List<String> fav = favorites(settings, all);
        JSONArray a = new JSONArray();
        for (String p : fav) a.put(json(all.get(p), true));
        List<App> rest = new ArrayList<>(all.values());
        java.util.Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        for (App app : rest) if (!fav.contains(app.pkg)) a.put(json(app, false));
        return a;
    }

    List<String> favorites(Prefs settings, Map<String, App> all) {
        List<String> fav = new ArrayList<>();
        JSONArray saved = settings.favorites();
        if (saved != null) {
            for (int i = 0; i < saved.length(); i++) {
                String p = saved.optString(i);
                if (all.containsKey(p)) fav.add(p);
            }
            return fav;
        }
        for (String p : POPULAR) if (all.containsKey(p) && fav.size() < 8) fav.add(p);
        return fav;
    }

    private static JSONObject json(App app, boolean fav) throws Exception {
        return new JSONObject().put("pkg", app.pkg).put("label", app.label).put("fav", fav).put("tv", app.leanback);
    }

    /** Shell command that opens the app, or null if it is not installed/launchable. */
    String launchCommand(String pkg) {
        if (!isPackageName(pkg)) return null;
        App app = installed().get(pkg);
        if (app == null || !CLS.matcher(app.activity).matches()) return null;
        return "am start -n " + Panel.shellQuote(app.pkg + "/" + app.activity);
    }

    /** {package, activity class} of the app's launcher entry, or null. */
    String[] component(String pkg) {
        if (!isPackageName(pkg)) return null;
        App app = installed().get(pkg);
        return app == null || !CLS.matcher(app.activity).matches() ? null : new String[]{app.pkg, app.activity};
    }

    String label(String pkg) {
        App app = installed().get(pkg);
        return app == null ? pkg : app.label;
    }

    /** 96x96 PNG of the app icon (banner-free), cached. */
    synchronized byte[] icon(String pkg) throws Exception {
        if (!isPackageName(pkg)) return null;
        byte[] cached = icons.get(pkg);
        if (cached != null) return cached;
        if (!installed().containsKey(pkg)) return null;
        Drawable d = ctx.getPackageManager().getApplicationIcon(pkg);
        Bitmap b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, 96, 96);
        d.draw(c);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        b.recycle();
        byte[] png = out.toByteArray();
        icons.put(pkg, png);
        return png;
    }
}
