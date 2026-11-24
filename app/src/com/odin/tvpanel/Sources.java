package com.odin.tvpanel;

import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TV inputs through the standard Android TV input framework (works on any brand): the built-in
 * tuner ("watch TV") and the HDMI inputs.
 * Name priority: the label the user gave the input in TV settings, then the HDMI-CEC
 * device name, then "HDMI n". Only connected inputs are listed as active.
 */
final class Sources {
    private static final Pattern DIGITS = Pattern.compile("(\\d+)\\s*$");
    private static final Pattern CEC = Pattern.compile(
            "display_name: (.+?) power_status: \\S+ physical_address: \\S+ port_id: (\\d+)");

    static final class Input {
        String id, name, label;
        int port;
        boolean connected, tuner;
    }

    private final Context ctx;
    private final Panel panel;
    private volatile List<Input> cache;
    private volatile long cacheAt;

    Sources(Context ctx, Panel panel) {
        this.ctx = ctx;
        this.panel = panel;
    }

    List<Input> list() {
        List<Input> c = cache;
        if (c != null && SystemClock.elapsedRealtime() - cacheAt < 30_000) return c;
        TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
        List<Input> out = new ArrayList<>();
        if (tim == null) return out;

        List<TvInputInfo> all = tim.getTvInputList();
        Map<String, String> childNames = new HashMap<>();
        for (TvInputInfo i : all) {
            // HDMI-CEC devices show up as child inputs of their HDMI port.
            if (i.getParentId() != null) childNames.put(i.getParentId(), String.valueOf(i.loadLabel(ctx)));
        }
        Map<Integer, String> cec = cecNames();
        Input tv = tuner(tim, all);
        if (tv != null) out.add(tv);
        for (TvInputInfo i : all) {
            if (i.getType() != TvInputInfo.TYPE_HDMI || i.getParentId() != null || !i.isPassthroughInput()) continue;
            Input in = new Input();
            in.id = i.getId();
            in.label = String.valueOf(i.loadLabel(ctx));
            in.port = port(in.id, in.label);
            CharSequence custom = i.loadCustomLabel(ctx);
            String name = custom == null ? null : custom.toString();
            // Philips stores the device type after "&" (e.g. "Receiver&HD_DIGITAL_RECEIVER").
            if (name != null && name.indexOf('&') >= 0) name = name.substring(0, name.indexOf('&'));
            if (name != null && (name.trim().isEmpty() || name.trim().equals(in.label))) name = null;
            if (name != null) name = name.trim();
            if (name == null) name = childNames.get(in.id);
            if (name == null && in.port > 0) name = cec.get(in.port);
            in.name = name == null ? in.label : name;
            int state;
            try {
                state = tim.getInputState(in.id);
            } catch (RuntimeException e) {
                state = TvInputManager.INPUT_STATE_CONNECTED;
            }
            // Some sets report every port as connected; a known device name is a better hint there.
            in.connected = state != TvInputManager.INPUT_STATE_DISCONNECTED && (name != null || state == TvInputManager.INPUT_STATE_CONNECTED);
            out.add(in);
        }
        java.util.Collections.sort(out, (a, b) -> a.port - b.port);   // tuner has port 0: first
        cache = out;
        cacheAt = SystemClock.elapsedRealtime();
        return out;
    }

    JSONArray json() throws Exception {
        JSONArray a = new JSONArray();
        for (Input in : list()) {
            a.put(new JSONObject().put("id", in.id).put("name", in.name).put("label", in.label)
                    .put("port", in.port).put("connected", in.connected).put("tuner", in.tuner));
        }
        return a;
    }

    /**
     * The input currently on screen, from the TV input framework's main session
     * ("mainSessionToken" -> SessionState.inputId). Null when the TV shows apps / home.
     */
    String activeInput(boolean liveTvOnScreen) {
        try {
            String d = panel.adb.shell("dumpsys tv_input");
            Matcher main = Pattern.compile("mainSessionToken: (\\S+)").matcher(d);
            if (!main.find() || "null".equals(main.group(1))) {
                // The TV's live-TV app is on screen without a "main" session (e.g. a tuner with no
                // channels yet shows its own message): its open tuner session is what is shown.
                if (!liveTvOnScreen) return null;
                Matcher any = Pattern.compile("inputId: (\\S+)").matcher(d);
                while (any.find()) {
                    String id = any.group(1).trim();
                    if (tunerIds.contains(id)) return tileFor(id);
                }
                return null;
            }
            String token = main.group(1), inputId = null;
            for (String line : d.split("\n")) {
                String t = line.trim();
                if (t.startsWith("inputId: ")) inputId = t.substring(9).trim();
                else if (t.startsWith("sessionToken: ") && t.substring(14).trim().equals(token)) return tileFor(inputId);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * The built-in TV tuner as one "watch TV" source, named the way the TV names it (the user's own
     * label first). Many sets expose several tuner inputs (antenna/cable, satellite); the TV's own
     * "watch TV" action picks the right one and the last channel, so they become one tile.
     */
    private Input tuner(TvInputManager tim, List<TvInputInfo> all) {
        Input best = null;
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (TvInputInfo i : all) {
            if (i.getType() != TvInputInfo.TYPE_TUNER || i.getParentId() != null || i.isPassthroughInput()) continue;
            // Only the TV's own broadcast tuner: not internet-channel services (Philips "Akış"
            // FAST channels, DVB-I) and not third-party live-channel apps.
            String svc = i.getServiceInfo().packageName;
            if (NOT_BROADCAST.contains(svc)) continue;
            if ((i.getServiceInfo().applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) continue;
            int state;
            try {
                state = tim.getInputState(i.getId());
            } catch (RuntimeException e) {
                state = TvInputManager.INPUT_STATE_CONNECTED;
            }
            if (state == TvInputManager.INPUT_STATE_DISCONNECTED) continue;
            CharSequence custom = i.loadCustomLabel(ctx);
            String name = custom == null ? null : custom.toString();
            if (name != null && name.indexOf('&') >= 0) name = name.substring(0, name.indexOf('&'));
            String label = String.valueOf(i.loadLabel(ctx)).trim();
            Input in = new Input();
            in.id = i.getId();
            in.label = label;
            in.name = name != null && !name.trim().isEmpty() ? name.trim() : panel.philips ? philipsWatchTvLabel() : label;
            in.tuner = true;
            in.connected = true;
            ids.add(in.id);
            if (best == null) best = in;
        }
        tunerIds = ids;
        return best;
    }

    private static final java.util.Set<String> NOT_BROADCAST = new java.util.HashSet<>(java.util.Arrays.asList(
            "org.droidtv.fastip", "org.droidtv.dvbi"));

    /**
     * Philips names its tuner "Watch TV" in the Sources menu (Turkish "TV izleme"…), from the TV app's
     * own resources, in the TV's language. The input's own label is technical ("Tuner(DTV)").
     */
    private String philipsWatchTvLabel() {
        try {
            android.content.res.Resources r = ctx.getPackageManager().getResourcesForApplication("org.droidtv.playtv");
            int id = r.getIdentifier("MAIN_LABEL_WATCH_TV_LOCAL", "string", "org.droidtv.playtv");
            if (id != 0) return r.getString(id);
        } catch (Exception ignored) {
        }
        return "TV";
    }

    /** Every tuner input id: all of them map to the single "watch TV" tile. */
    private volatile java.util.Set<String> tunerIds = new java.util.HashSet<>();

    /** Maps any tuner input to the one tuner tile. */
    private String tileFor(String inputId) {
        if (inputId == null || !tunerIds.contains(inputId)) return inputId;
        for (Input in : list()) if (in.tuner) return in.id;
        return inputId;
    }

    Input find(String id) {
        for (Input in : list()) if (in.id.equals(id)) return in;
        return null;
    }

    /**
     * HDMI: the standard "passthrough" URI; the TV's own live-TV app switches to the input.
     * Tuner: Philips' own "watch TV" action (same as the TV key: last channel); elsewhere the
     * standard channel list of that input, which the TV's live-TV app opens.
     */
    String switchCommand(Input in) {
        if (in.tuner) {
            if (panel.philips) return "am start -a org.droidtv.playtv.WATCH_TV";
            return "am start -a android.intent.action.VIEW -d "
                    + Panel.shellQuote(TvContract.buildChannelsUriForInput(in.id).toString());
        }
        String uri = TvContract.buildChannelUriForPassthroughInput(in.id).toString();
        return "am start -a android.intent.action.VIEW -d " + Panel.shellQuote(uri);
    }

    /** port -> HDMI-CEC device name, from the HDMI control service (needs ADB). */
    private Map<Integer, String> cecNames() {
        Map<Integer, String> m = new HashMap<>();
        try {
            String out = panel.adb.shell("dumpsys hdmi_control | grep 'display_name:'");
            Matcher x = CEC.matcher(out);
            while (x.find()) {
                String n = x.group(1).trim();
                if (!n.isEmpty()) m.put(Integer.parseInt(x.group(2)), n);
            }
        } catch (Exception ignored) {
            // no ADB yet, or no HDMI-CEC
        }
        return m;
    }

    private static int port(String id, String label) {
        // Philips: ".../HDMIInputService/HW5" = HDMI 1
        Matcher hw = Pattern.compile("HDMIInputService/HW(\\d+)$").matcher(id);
        if (hw.find()) {
            int p = Integer.parseInt(hw.group(1)) - 4;
            if (p >= 1 && p <= 8) return p;
        }
        Matcher d = DIGITS.matcher(label);
        return d.find() ? Integer.parseInt(d.group(1)) : 0;
    }
}
