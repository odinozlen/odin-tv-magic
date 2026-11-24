package com.odin.tvpanel;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Philips picture style and brightness.
 *
 * JointSpace returns an empty picture-style list on this firmware, so the style is
 * changed through the Philips quick menu: open it, read the screen (uiautomator),
 * move with D-pad keys, confirm, read again. This works in any menu language because
 * it relies on the list structure (checkable rows, one checked), not on texts.
 *
 * Brightness = the "Backlight contrast" setting (the panel light level on Philips),
 * which JointSpace can write even though it reports Controllable=false.
 */
final class Picture {
    static final long NODE_BACKLIGHT = 2130968795L;
    private static final String QUICK = "QuickSettingsActivity";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Panel p;
    private final TvUi ui;
    private final File cacheFile;
    private List<String> styles = new ArrayList<>();
    private String current;

    Picture(Panel p, TvUi ui, File dir) {
        this.p = p;
        this.ui = ui;
        this.cacheFile = new File(dir, "picture.json");
        try {
            JSONObject o = new JSONObject(new String(AdbClient.readAll(cacheFile), UTF8));
            JSONArray a = o.getJSONArray("styles");
            for (int i = 0; i < a.length(); i++) styles.add(a.getString(i));
            current = o.optString("current", null);
        } catch (Exception ignored) {
            // not read yet
        }
    }

    synchronized JSONObject status() throws Exception {
        if (!p.philips) return new JSONObject().put("supported", false);
        Integer b = null;
        if (p.js.paired()) {
            try {
                b = brightness();
            } catch (Exception ignored) {
            }
        }
        return new JSONObject()
                .put("supported", true)
                .put("styles", new JSONArray(styles))
                .put("current", current == null ? JSONObject.NULL : current)
                .put("brightness", b == null ? JSONObject.NULL : b)
                .put("hidden", p.settings.hiddenStyles());
    }

    int brightness() throws Exception {
        JSONObject q = new JSONObject().put("nodes", new JSONArray().put(new JSONObject().put("nodeid", NODE_BACKLIGHT)));
        JSONObject r = new JSONObject(p.js.post("menuitems/settings/current", q));
        return r.getJSONArray("values").getJSONObject(0).getJSONObject("value").getJSONObject("data").getInt("value");
    }

    synchronized Panel.Result setBrightness(int v) throws Exception {
        if (!p.philips) return new Panel.Result(404, "philips.unsupported");
        if (!p.js.paired()) return new Panel.Result(409, "philips.not_paired");
        v = Math.max(0, Math.min(100, v));
        JSONObject value = new JSONObject()
                .put("Nodeid", NODE_BACKLIGHT).put("Controllable", "true").put("Available", "true")
                .put("data", new JSONObject().put("value", v));
        try {
            p.js.post("menuitems/settings/update", new JSONObject().put("values", new JSONArray().put(new JSONObject().put("value", value))));
        } catch (IOException ignored) {
            // verified by reading back below
        }
        SystemClock.sleep(300);
        int now = brightness();
        if (now == v) return new Panel.Result(200, "pic.brightness", v);
        return new Panel.Result(501, "pic.brightness_denied", now);
    }

    /** Re-reads the list from the TV (the quick menu shows briefly). */
    synchronized Panel.Result refresh() throws Exception {
        if (!p.philips) return new Panel.Result(404, "philips.unsupported");
        try {
            remember(openStyles());
        } finally {
            close();
        }
        return new Panel.Result(200, "pic.style", current);
    }

    synchronized Panel.Result setStyle(String name) throws Exception {
        if (!p.philips) return new Panel.Result(404, "philips.unsupported");
        try {
            List<TvUi.Node> items = openStyles();
            int target = -1, at = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).text.equals(name)) target = i;
                if (items.get(i).selected) at = i;
            }
            if (target < 0) {
                // HDR / Dolby Vision content has its own list: match "Vivid" <-> "HDR Vivid".
                for (int i = 0; i < items.size() && target < 0; i++) {
                    if (base(items.get(i).text).equals(base(name))) target = i;
                }
            }
            if (target < 0) {
                remember(items);
                return new Panel.Result(404, "pic.style_missing", name);
            }
            name = items.get(target).text;
            if (at < 0) {
                ui.keys("KEYCODE_DPAD_UP", items.size());
                at = 0;
            }
            int d = target - at;
            ui.keys(d > 0 ? "KEYCODE_DPAD_DOWN" : "KEYCODE_DPAD_UP", Math.abs(d));
            SystemClock.sleep(300);
            ui.key("KEYCODE_DPAD_CENTER");
            SystemClock.sleep(600);
            List<TvUi.Node> after = styleItems(ui.dump());
            if (!after.isEmpty()) remember(after);
            if (!name.equals(current)) return new Panel.Result(500, "pic.style_failed");
            p.log("picture style: " + name);
            return new Panel.Result(200, "pic.style", name);
        } finally {
            close();
        }
    }

    /** Opens Quick menu > Picture style and returns its rows. */
    private List<TvUi.Node> openStyles() throws Exception {
        p.adb.shell("am start -a org.droidtv.action.EXPERIENCE_MENU");
        long end = SystemClock.uptimeMillis() + 4000;
        while (!ui.foregroundActivity().contains(QUICK)) {
            // Never send keys before the menu is on screen; they would go to the app being watched.
            if (SystemClock.uptimeMillis() > end) throw new IOException("pic.menu_failed");
            SystemClock.sleep(250);
        }
        SystemClock.sleep(500);
        ui.keys("KEYCODE_DPAD_UP", 20);  // first row = picture style
        SystemClock.sleep(300);
        ui.key("KEYCODE_DPAD_RIGHT");
        SystemClock.sleep(700);
        List<TvUi.Node> all = ui.dump();
        List<TvUi.Node> items = styleItems(all);
        int checked = 0;
        for (TvUi.Node n : items) if (n.checked) checked++;
        // Make sure this really is the picture-style list (and not e.g. sound style) before touching it.
        boolean header = false;
        for (TvUi.Node n : all) if (!n.checkable && isPictureHeader(n.text)) header = true;
        if (!header || items.size() < 2 || checked != 1) throw new IOException("pic.list_failed");
        return items;
    }

    /** "Picture style" in the languages Philips ships, matched by word stem. */
    private static final String[] PICTURE_STEMS = {
            "picture", "görüntü", "bild", "image", "immagine", "imagen", "imagem", "beeld", "obraz",
            "kép", "billed", "kuva", "imagine", "slika", "izobraž", "изображ", "εικόν", "vaizd", "attēl", "pilt",
    };

    static boolean isPictureHeader(String text) {
        String t = text.toLowerCase(java.util.Locale.ROOT);
        if (t.isEmpty() || t.length() > 40) return false;
        for (String s : PICTURE_STEMS) if (t.contains(s)) return true;
        return false;
    }

    private static List<TvUi.Node> styleItems(List<TvUi.Node> all) {
        List<TvUi.Node> items = new ArrayList<>();
        for (TvUi.Node n : all) if (n.checkable && !n.text.isEmpty()) items.add(n);
        return items;
    }

    /** "HDR Vivid" -> "Vivid", "Dolby Vision Game" -> "Game" */
    static String base(String s) {
        return s.replaceFirst("^(HDR|HLG|Dolby Vision)\\s+", "").trim();
    }

    /** BACK only while the quick menu is still in front; otherwise it could close the app being watched. */
    private void close() throws Exception {
        for (int i = 0; i < 3 && ui.foregroundActivity().contains(QUICK); i++) {
            ui.key("KEYCODE_BACK");
            SystemClock.sleep(350);
        }
    }

    private void remember(List<TvUi.Node> items) {
        styles = new ArrayList<>();
        for (TvUi.Node n : items) {
            styles.add(n.text);
            if (n.checked) current = n.text;
        }
        try {
            AdbClient.writeAll(cacheFile, new JSONObject().put("styles", new JSONArray(styles))
                    .put("current", current == null ? JSONObject.NULL : current).toString().getBytes(UTF8));
        } catch (Exception ignored) {
        }
    }
}
