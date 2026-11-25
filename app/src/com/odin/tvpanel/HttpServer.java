package com.odin.tvpanel;

import android.content.res.AssetManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Tiny HTTP/1.1 server for the panel. Fixed routes only.
 *
 * Hardening:
 *  - only requests whose Host is an IP literal or localhost (blocks DNS-rebinding pages),
 *  - every /api call except hello/pair must carry a valid X-Auth signature (see Auth),
 *  - no CORS headers, so other websites cannot read responses,
 *  - strict CSP / nosniff / no-referrer / no framing,
 *  - bounded request line, header count, header size and body size.
 */
final class HttpServer implements Runnable {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern HOST_OK = Pattern.compile(
            "^(\\d{1,3}(\\.\\d{1,3}){3}|\\[[0-9a-fA-F:.]+\\]|localhost)(:\\d{1,5})?$");
    private static final int MAX_HEADERS = 40;
    private static final int MAX_LINE = 8192;
    private static final int MAX_BODY = 64 * 1024;

    private static final Map<String, String[]> STATIC = new HashMap<>();
    static {
        STATIC.put("/", new String[]{"web/index.html", "text/html; charset=utf-8"});
        STATIC.put("/index.html", new String[]{"web/index.html", "text/html; charset=utf-8"});
        STATIC.put("/app.js", new String[]{"web/app.js", "text/javascript; charset=utf-8"});
        STATIC.put("/manifest.json", new String[]{"web/manifest.json", "application/manifest+json"});
        STATIC.put("/icon-magic.png", new String[]{"web/icon-magic.png", "image/png"});
        STATIC.put("/icon-magic-512.png", new String[]{"web/icon-magic-512.png", "image/png"});
    }
    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: blob:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    private final ServerSocket server;
    private final Panel panel;
    private final AssetManager assets;
    private final ExecutorService pool = Executors.newFixedThreadPool(16);

    HttpServer(int[] ports, Panel panel, AssetManager assets) throws IOException {
        this.server = bind(ports);
        this.panel = panel;
        this.assets = assets;
    }

    private static ServerSocket bind(int[] ports) throws IOException {
        IOException last = null;
        for (int p : ports) {
            ServerSocket ss = new ServerSocket();
            try {
                ss.setReuseAddress(true);   // a restart right after an update must get the same port
                ss.bind(new java.net.InetSocketAddress(p), 50);
                return ss;
            } catch (IOException e) {
                last = e;
                ss.close();
            }
        }
        throw last != null ? last : new IOException("no port");
    }

    int port() {
        return server.getLocalPort();
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                final Socket c = server.accept();
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        handle(c);
                    }
                });
            } catch (IOException e) {
                if (server.isClosed()) return;
            }
        }
    }

    private static final class Req {
        String method, path, lang = "en";
        Map<String, String> headers = new HashMap<>();
        byte[] body = new byte[0];
    }

    private void handle(Socket s) {
        String lang = "en";
        try {
            s.setSoTimeout(10000);  // slow senders cannot hold a worker for long
            Req req = parse(s.getInputStream());
            if (req == null) return;
            s.setSoTimeout(30000);
            lang = req.lang;
            route(req, s.getOutputStream());
        } catch (Exception e) {
            try {
                json(s.getOutputStream(), 500, err(lang, e));
            } catch (Exception ignored) {
            }
        } finally {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static Req parse(InputStream in) throws IOException {
        String line = readLine(in);
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split(" ");
        if (parts.length != 3) throw new IOException("bad_request");
        Req r = new Req();
        r.method = parts[0];
        int q = parts[1].indexOf('?');
        r.path = q >= 0 ? parts[1].substring(0, q) : parts[1];
        int count = 0;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            if (++count > MAX_HEADERS) throw new IOException("too_large");
            int c = line.indexOf(':');
            if (c > 0) r.headers.put(line.substring(0, c).trim().toLowerCase(Locale.ROOT), line.substring(c + 1).trim());
        }
        r.lang = Strings.isTurkish(r.headers.get("x-lang")) ? "tr" : "en";
        String cl = r.headers.get("content-length");
        if (cl != null) {
            int n;
            try {
                n = Integer.parseInt(cl);
            } catch (NumberFormatException e) {
                throw new IOException("bad_request");
            }
            if (n < 0 || n > MAX_BODY) throw new IOException("too_large");
            r.body = new byte[n];
            int off = 0;
            while (off < n) {
                int k = in.read(r.body, off, n - off);
                if (k < 0) throw new IOException("bad_request");
                off += k;
            }
        }
        return r;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) >= 0) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
            if (b.size() > MAX_LINE) throw new IOException("too_large");
        }
        if (c < 0 && b.size() == 0) return null;
        return new String(b.toByteArray(), UTF8);
    }

    private void route(Req r, OutputStream out) throws Exception {
        String host = r.headers.get("host");
        if (host == null || !HOST_OK.matcher(host).matches()) {
            json(out, 403, msg(r.lang, "forbidden_host"));
            return;
        }
        String[] st = STATIC.get(r.path);
        if ("GET".equals(r.method) && st != null) {
            send(out, 200, st[1], readAsset(st[0]));
            return;
        }
        if (!r.path.startsWith("/api/")) {
            json(out, 404, msg(r.lang, "not_found"));
            return;
        }
        String p = r.path.substring("/api/".length());

        // ---- unauthenticated ----
        if ("GET".equals(r.method) && "hello".equals(p)) {
            json(out, 200, new JSONObject().put("ok", true).put("time", System.currentTimeMillis())
                    .put("version", Panel.VERSION));
            return;
        }
        if ("POST".equals(r.method) && "pair".equals(p)) {
            JSONObject b = body(r);
            Auth.Pairing pr = panel.auth.pair(b.optString("code"), b.optString("name"));
            if (pr == null) {
                json(out, 401, msg(r.lang, "pair.bad_code").put("reason", "pair"));
                return;
            }
            panel.log("phone paired: " + pr.id);
            MainActivity.notifyPaired();
            json(out, 200, new JSONObject().put("ok", true).put("id", pr.id).put("key", pr.key)
                    .put("msg", Strings.t(r.lang, "pair.ok")));
            return;
        }

        // ---- everything else needs a signature ----
        String[] dev = new String[1];
        int v = panel.auth.verify(r.method, r.path, r.headers.get("x-auth"), r.body, dev);
        if (v != Auth.OK) {
            JSONObject e = msg(r.lang, v == Auth.CLOCK ? "auth.clock" : "auth.required")
                    .put("reason", v == Auth.CLOCK ? "clock" : "pair");
            if (v == Auth.CLOCK) e.put("time", System.currentTimeMillis());
            json(out, 401, e);
            return;
        }
        JSONObject body = body(r);
        String lang = r.lang;

        if ("GET".equals(r.method)) {
            switch (p) {
                case "status":
                    json(out, 200, panel.status(lang));
                    return;
                case "apps":
                    json(out, 200, new JSONObject().put("ok", true).put("apps", panel.apps()));
                    return;
                case "screenshot":
                    send(out, 200, "image/jpeg", panel.screenshotJpeg());
                    return;
                case "volume":
                    json(out, 200, panel.volume());
                    return;
                case "picture":
                    json(out, 200, panel.picture.status());
                    return;
                case "philips/diag":
                    json(out, 200, panel.philipsDiagnostics());
                    return;
                case "settings":
                    json(out, 200, panel.settings.toJson().put("devices", panel.auth.list(dev[0])));
                    return;
                case "launchers":
                    json(out, 200, panel.launchers.json());
                    return;
                case "debloat":
                    json(out, 200, panel.debloat.json(lang));
                    return;
                case "addons": {
                    JSONObject a = panel.addons.json();
                    String ek = panel.addons.errorKey();
                    if (ek != null) a.getJSONObject("airplay").put("error", Strings.t(lang, ek));
                    json(out, 200, a);
                    return;
                }
                default:
                    if (p.startsWith("appicon/")) {
                        byte[] png = panel.apps.icon(URLDecoder.decode(p.substring(8), "UTF-8"));
                        if (png == null) json(out, 404, msg(lang, "not_found"));
                        else send(out, 200, "image/png", png, "private, max-age=86400");
                        return;
                    }
            }
        } else if ("POST".equals(r.method)) {
            Panel.Result res = null;
            if (p.startsWith("action/")) res = panel.action(p.substring(7));
            else if (p.startsWith("launch/")) res = panel.launch(URLDecoder.decode(p.substring(7), "UTF-8"));
            else if ("source".equals(p)) res = panel.source(body.optString("id"));
            else if (p.startsWith("remote/")) res = panel.remote(p.substring(7));
            else if ("keyboard".equals(p)) res = panel.keyboard(body.optString("text"));
            else if (p.startsWith("key/")) res = panel.key(p.substring(4));
            else if ("volume".equals(p)) res = panel.setVolume(body);
            else if (p.startsWith("philips/pair/")) res = panel.philipsPair(p.substring(13), body);
            else if (p.startsWith("ambilight/")) res = panel.ambilight(p.substring(10), body);
            else if ("picture/style".equals(p)) res = panel.picture.setStyle(body.optString("name"));
            else if ("picture/refresh".equals(p)) res = panel.picture.refresh();
            else if ("picture/brightness".equals(p)) res = panel.picture.setBrightness(body.optInt("value", -1));
            else if ("launcher".equals(p)) res = panel.launchers.set(body.optString("component"));
            else if ("launcher/restore".equals(p)) res = panel.launchers.restore();
            else if ("debloat/disable".equals(p)) res = panel.debloat.disable(body.optJSONArray("packages") == null ? new org.json.JSONArray() : body.getJSONArray("packages"));
            else if ("debloat/enable".equals(p)) res = panel.debloat.enable(body.optJSONArray("packages") == null ? new org.json.JSONArray() : body.getJSONArray("packages"));
            else if ("addons/airplay".equals(p)) res = panel.addons.installAirPlay();
            else if ("launcher/install".equals(p)) res = panel.launchers.install(body.optString("pkg"));
            else if ("adb/retry".equals(p)) {
                panel.adb.retry();
                res = new Panel.Result(200, "ok");
            }
            else if ("adb/pair".equals(p)) res = PanelService.pairWireless(panel, body.optString("code"), body.optInt("port", 0));
            else if ("settings".equals(p)) {
                panel.settings.update(body);
                res = new Panel.Result(200, "settings.saved");
            } else if ("devices/revoke".equals(p)) {
                res = panel.auth.revoke(body.optString("id"))
                        ? new Panel.Result(200, "device.revoked") : new Panel.Result(404, "not_found");
            } else if ("surprise".equals(p)) {
                if (!panel.settings.partyMode()) res = new Panel.Result(403, "party.off");
                else res = panel.surprise.start(body.optString("message"), lang)
                        ? new Panel.Result(200, "party.started") : new Panel.Result(409, "party.running");
            } else if ("surprise/stop".equals(p)) {
                panel.surprise.stop();
                res = new Panel.Result(200, "party.stopped");
            }
            if (res != null) {
                json(out, res.code, res.json(lang));
                return;
            }
        }
        json(out, 404, msg(lang, "not_found"));
    }

    private static JSONObject body(Req r) throws IOException {
        if (r.body.length == 0) return new JSONObject();
        try {
            return new JSONObject(new String(r.body, UTF8));
        } catch (JSONException e) {
            throw new IOException("bad_request");
        }
    }

    private byte[] readAsset(String name) throws IOException {
        InputStream in = assets.open(name);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }

    private static JSONObject msg(String lang, String key) throws JSONException {
        return new JSONObject().put("ok", false).put("msg", Strings.t(lang, key));
    }

    /** Errors thrown with a message key are translated; anything else becomes a short generic message. */
    private static JSONObject err(String lang, Exception e) {
        try {
            String m = e.getMessage();
            String text = Strings.has(m) ? Strings.t(lang, m)
                    : Strings.t(lang, "error", e.getClass().getSimpleName() + (m == null ? "" : ": " + m));
            return new JSONObject().put("ok", false).put("msg", text);
        } catch (JSONException x) {
            return new JSONObject();
        }
    }

    private static void json(OutputStream out, int code, JSONObject o) throws IOException {
        send(out, code, "application/json; charset=utf-8", o.toString().getBytes(UTF8));
    }

    private static void send(OutputStream out, int code, String type, byte[] body) throws IOException {
        send(out, code, type, body, "no-store");
    }

    private static void send(OutputStream out, int code, String type, byte[] body, String cache) throws IOException {
        String reason;
        switch (code) {
            case 200: reason = "OK"; break;
            case 401: reason = "Unauthorized"; break;
            case 403: reason = "Forbidden"; break;
            case 404: reason = "Not Found"; break;
            case 409: reason = "Conflict"; break;
            default: reason = "Error";
        }
        String head = "HTTP/1.1 " + code + " " + reason + "\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: " + cache + "\r\n"
                + "X-Content-Type-Options: nosniff\r\n"
                + "Referrer-Policy: no-referrer\r\n"
                + "X-Frame-Options: DENY\r\n"
                + (type.startsWith("text/html") ? "Content-Security-Policy: " + CSP + "\r\n" : "")
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(UTF8));
        out.write(body);
        out.flush();
    }
}
