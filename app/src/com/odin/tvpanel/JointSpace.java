package com.odin.tvpanel;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Client for the Philips JointSpace API v6 on the TV itself (https://127.0.0.1:1926):
 * pairing, Digest auth, Ambilight, volume, settings and Unicode key input.
 * The pairing flow and signature follow ha-philipsjs / pylips (MIT); the shared HMAC
 * key below is public in those projects.
 * Test vector: ts=1128 pin=0667 -> rdPzC+bdWQHQMcwe3X+LddeG/tA=
 */
final class JointSpace {
    private static final String BASE = "https://127.0.0.1:1926/6/";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final byte[] AUTH_SHARED_KEY = Base64.decode(
            "ZmVay1EQVFOaZhwQ4Kv81ypLAZNczV9sG4KkseXWn1NEk6cXmPKO/MCa9sryslvLCFMnNe4Z4CPXzToowvhHvA==",
            Base64.DEFAULT);

    // The TV uses a self-signed certificate; accepted only for 127.0.0.1.
    private static final SSLSocketFactory LOCAL_SSL;
    private static final HostnameVerifier LOCAL_HOST = new HostnameVerifier() {
        @Override
        public boolean verify(String host, SSLSession s) {
            return "127.0.0.1".equals(host);
        }
    };
    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] c, String a) { }
                @Override public void checkServerTrusted(X509Certificate[] c, String a) { }
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());
            LOCAL_SSL = ctx.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private final File credFile;
    private String user;      // device id
    private String password;  // auth_key
    private JSONObject pendingDevice;
    private String pendingKey;
    private long pendingTimestamp;

    JointSpace(File dir) {
        credFile = new File(dir, "jointspace.json");
        try {
            JSONObject o = new JSONObject(new String(readAll(credFile), UTF8));
            user = o.getString("id");
            password = o.getString("auth_key");
        } catch (IOException | JSONException ignored) {
            // not paired yet
        }
    }

    synchronized boolean paired() {
        return user != null;
    }

    /** Makes the TV show a PIN. */
    synchronized void pairStart() throws IOException, JSONException {
        byte[] idBytes = new byte[16];
        new SecureRandom().nextBytes(idBytes);
        StringBuilder id = new StringBuilder();
        for (byte b : idBytes) id.append(String.format(Locale.ROOT, "%02x", b));

        JSONObject device = new JSONObject()
                .put("device_name", "Odin TV Magic")
                .put("device_os", "Android")
                .put("type", "native")
                .put("id", id.toString())
                .put("app_id", "com.odin.tvpanel")
                .put("app_name", "Odin TV Magic");

        JSONObject access = new JSONObject().put("scope", new JSONArray().put("read").put("write").put("control"));
        try {
            JSONObject system = new JSONObject(request("GET", "system", null, null, null).body);
            if (system.has("featuring")) access.put("featuring", system.get("featuring"));
        } catch (IOException | JSONException ignored) {
            // "featuring" is optional
        }

        Resp r = request("POST", "pair/request", new JSONObject().put("access", access).put("device", device), null, null);
        JSONObject res = new JSONObject(r.body);
        if (!"SUCCESS".equals(res.optString("error_id"))) throw new IOException("pair.failed");
        pendingDevice = device;
        pendingKey = res.getString("auth_key");
        pendingTimestamp = res.getLong("timestamp");
    }

    synchronized void pairFinish(String pin) throws IOException, JSONException, GeneralSecurityException {
        if (pendingDevice == null) throw new IOException("philips.not_paired");
        String id = pendingDevice.getString("id");
        JSONObject auth = new JSONObject()
                .put("auth_appId", "1")
                .put("auth_timestamp", pendingTimestamp)
                .put("auth_signature", signature(String.valueOf(pendingTimestamp), pin))
                .put("pin", pin);
        Resp r = request("POST", "pair/grant", new JSONObject().put("auth", auth).put("device", pendingDevice), id, pendingKey);
        JSONObject res = new JSONObject(r.body);
        if (!"SUCCESS".equals(res.optString("error_id"))) throw new IOException("philips.pin_wrong");

        user = id;
        password = pendingKey;
        pendingDevice = null;
        writeAll(credFile, new JSONObject().put("id", user).put("auth_key", password).toString().getBytes(UTF8));
    }

    static String signature(String timestamp, String pin) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(AUTH_SHARED_KEY, "HmacSHA1"));
        mac.update(timestamp.getBytes(UTF8));
        mac.update(pin.getBytes(UTF8));
        return Base64.encodeToString(mac.doFinal(), Base64.NO_WRAP);
    }

    String get(String path) throws IOException {
        return authed("GET", path, null);
    }

    String post(String path, JSONObject body) throws IOException {
        return authed("POST", path, body);
    }

    private String authed(String method, String path, JSONObject body) throws IOException {
        String u, p;
        synchronized (this) {
            if (user == null) throw new IOException("philips.not_paired");
            u = user;
            p = password;
        }
        Resp r = request(method, path, body, u, p);
        if (r.code >= 300) throw new IOException("JointSpace " + path + ": HTTP " + r.code);
        return r.body;
    }

    private static final class Resp {
        int code;
        String body;
        String challenge;  // WWW-Authenticate
    }

    /** Answers a 401 with Digest auth when credentials are given. */
    private static Resp request(String method, String path, JSONObject body, String user, String pass) throws IOException {
        Resp r = once(method, path, body, null);
        if (r.code == 401 && user != null && r.challenge != null) {
            r = once(method, path, body, digest(method, "/6/" + path, r.challenge, user, pass));
        }
        return r;
    }

    private static Resp once(String method, String path, JSONObject body, String authHeader) throws IOException {
        HttpsURLConnection c = (HttpsURLConnection) new URL(BASE + path).openConnection();
        c.setSSLSocketFactory(LOCAL_SSL);
        c.setHostnameVerifier(LOCAL_HOST);
        c.setConnectTimeout(4000);
        c.setReadTimeout(8000);
        c.setRequestMethod(method);
        if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
        if (body != null) {
            byte[] b = body.toString().getBytes(UTF8);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setFixedLengthStreamingMode(b.length);
            OutputStream out = c.getOutputStream();
            out.write(b);
            out.close();
        }
        Resp r = new Resp();
        r.code = c.getResponseCode();
        r.challenge = c.getHeaderField("WWW-Authenticate");
        InputStream in = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
        r.body = in == null ? "" : new String(readStream(in), UTF8);
        c.disconnect();
        return r;
    }

    private static String digest(String method, String uri, String challenge, String user, String pass) throws IOException {
        Map<String, String> p = new HashMap<>();
        Matcher m = Pattern.compile("(\\w+)=(?:\"([^\"]*)\"|([^,\\s]*))").matcher(challenge);
        while (m.find()) p.put(m.group(1).toLowerCase(Locale.ROOT), m.group(2) != null ? m.group(2) : m.group(3));
        String realm = p.get("realm"), nonce = p.get("nonce"), qop = p.get("qop"), opaque = p.get("opaque");
        String ha1 = md5(user + ":" + realm + ":" + pass);
        String ha2 = md5(method + ":" + uri);
        StringBuilder h = new StringBuilder("Digest username=\"").append(user)
                .append("\", realm=\"").append(realm)
                .append("\", nonce=\"").append(nonce)
                .append("\", uri=\"").append(uri).append('"');
        if (qop != null && qop.contains("auth")) {
            String nc = "00000001";
            String cnonce = Long.toHexString(new SecureRandom().nextLong());
            h.append(", qop=auth, nc=").append(nc).append(", cnonce=\"").append(cnonce).append('"')
                    .append(", response=\"").append(md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2)).append('"');
        } else {
            h.append(", response=\"").append(md5(ha1 + ":" + nonce + ":" + ha2)).append('"');
        }
        if (opaque != null) h.append(", opaque=\"").append(opaque).append('"');
        h.append(", algorithm=MD5");
        return h.toString();
    }

    private static String md5(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(UTF8));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(Locale.ROOT, "%02x", x));
            return b.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static byte[] readAll(File f) throws IOException {
        return readStream(new FileInputStream(f));
    }

    private static void writeAll(File f, byte[] b) throws IOException {
        AdbClient.writeAll(f, b);
    }
}
