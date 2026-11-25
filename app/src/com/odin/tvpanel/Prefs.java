package com.odin.tvpanel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;

/** Panel preferences stored on the TV, shared by every paired phone. */
final class Prefs {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final File file;
    private JSONObject o = new JSONObject();

    Prefs(File dir) {
        file = new File(dir, "settings.json");
        try {
            o = new JSONObject(new String(AdbClient.readAll(file), UTF8));
        } catch (Exception ignored) {
            // defaults
        }
    }

    synchronized boolean partyMode() {
        return o.optBoolean("partyMode", false);
    }

    /** Daily update check against GitHub (on by default; off = no request at all). */
    synchronized boolean updateCheck() {
        return o.optBoolean("updateCheck", true);
    }

    synchronized JSONArray favorites() {
        JSONArray a = o.optJSONArray("favorites");
        return a == null ? null : a;
    }

    synchronized JSONArray hiddenStyles() {
        JSONArray a = o.optJSONArray("hiddenStyles");
        return a == null ? new JSONArray() : a;
    }

    /** What the launcher switch changed, so it can be undone: {original, disabledStock}. */
    synchronized JSONObject launcherState() throws Exception {
        JSONObject l = o.optJSONObject("launcher");
        return l == null ? new JSONObject() : new JSONObject(l.toString());
    }

    synchronized void setLauncherState(JSONObject state) throws Exception {
        o.put("launcher", state);
        save();
    }

    synchronized JSONObject toJson() throws Exception {
        return new JSONObject()
                .put("partyMode", partyMode())
                .put("updateCheck", updateCheck())
                .put("hiddenStyles", hiddenStyles());
    }

    /** Applies only known keys, with validation. */
    synchronized void update(JSONObject in) throws Exception {
        if (in.has("partyMode")) o.put("partyMode", in.getBoolean("partyMode"));
        if (in.has("updateCheck")) o.put("updateCheck", in.getBoolean("updateCheck"));
        if (in.has("hiddenStyles")) o.put("hiddenStyles", strings(in.getJSONArray("hiddenStyles"), 30, 40));
        if (in.has("favorites")) o.put("favorites", packages(in.getJSONArray("favorites")));
        save();
    }

    static JSONArray packages(JSONArray in) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < in.length() && out.length() < 16; i++) {
            String p = in.getString(i);
            if (Apps.isPackageName(p)) out.put(p);
        }
        return out;
    }

    private static JSONArray strings(JSONArray in, int maxItems, int maxLen) throws Exception {
        JSONArray out = new JSONArray();
        for (int i = 0; i < in.length() && out.length() < maxItems; i++) {
            String s = in.getString(i);
            if (s.length() <= maxLen) out.put(s);
        }
        return out;
    }

    private void save() {
        try {
            AdbClient.writeAll(file, o.toString().getBytes(UTF8));
        } catch (Exception ignored) {
        }
    }
}
