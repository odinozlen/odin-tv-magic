package com.odin.tvpanel;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Minimal ADB client that talks to the TV's own adbd over loopback.
 *
 * Two transports:
 *  - classic "network debugging" on 127.0.0.1:5555 (most Android TV 9-11 sets, Philips),
 *  - Android 11+ "Wireless debugging": TLS port found via mDNS, after a one-time
 *    pairing with a 6-digit code (Google TV and newer sets). See {@link AdbPairing}.
 *
 * One long-lived connection; every command is its own ADB stream on it.
 * Protocol: packages/modules/adb/protocol.txt
 */
final class AdbClient {
    static final int A_CNXN = 0x4e584e43;
    static final int A_AUTH = 0x48545541;
    static final int A_OPEN = 0x4e45504f;
    static final int A_OKAY = 0x59414b4f;
    static final int A_CLSE = 0x45534c43;
    static final int A_WRTE = 0x45545257;
    static final int A_STLS = 0x534c5453;
    private static final int A_VERSION = 0x01000001;
    private static final int A_STLS_VERSION = 0x01000000;
    private static final int MAX_PAYLOAD = 256 * 1024;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private static final int KEY_BITS = 2048;
    private static final int KEY_BYTES = KEY_BITS / 8;
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    static final String KEY_NAME = "odintv@panel";

    // PKCS#1 v1.5 signature padding: 00 01 FF..FF 00 + SHA-1 DigestInfo, followed by the 20-byte token.
    private static final byte[] SIGNATURE_PADDING;
    static {
        byte[] digestInfo = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};
        int tokenLen = 20;
        SIGNATURE_PADDING = new byte[KEY_BYTES - tokenLen];
        SIGNATURE_PADDING[0] = 0x00;
        SIGNATURE_PADDING[1] = 0x01;
        int ffEnd = SIGNATURE_PADDING.length - digestInfo.length - 1;
        for (int i = 2; i < ffEnd; i++) SIGNATURE_PADDING[i] = (byte) 0xff;
        SIGNATURE_PADDING[ffEnd] = 0x00;
        System.arraycopy(digestInfo, 0, SIGNATURE_PADDING, ffEnd + 1, digestInfo.length);
    }

    /** Finds the wireless-debugging TLS port (mDNS on the TV). */
    interface PortFinder {
        int find(String serviceType) throws IOException;
    }

    private static final String LOOPBACK = "127.0.0.1";
    private static final int CLASSIC_PORT = 5555;

    private final File dir;
    private final KeyPair keys;
    private final PortFinder finder;
    private X509Certificate cert;
    private SSLContext tls;
    private int tlsPort = -1;

    /** Status text shown in the panel and on the TV screen (message key, see Strings). */
    volatile String state = "adb.not_connected";
    /** "classic", "tls" or null when not connected yet. */
    volatile String transport;

    AdbClient(File keyDir, PortFinder finder) throws GeneralSecurityException, IOException {
        this.dir = keyDir;
        this.finder = finder;
        this.keys = loadOrCreateKeys(keyDir);
    }

    /**
     * True while the TV shows "Allow debugging?" for our key. Requests then fail at once with that
     * message instead of queueing behind the connection that waits (up to a minute) for the user.
     */
    private volatile boolean awaitingApproval;

    private void notWhileApproving() throws IOException {
        if (awaitingApproval) throw new IOException("adb.waiting_approval");
    }

    String shell(String cmd) throws IOException, GeneralSecurityException {
        return new String(exec("shell:" + cmd), UTF8).trim();
    }

    /** Raw output (no pty): for binary data such as screenshots. */
    byte[] execOut(String cmd) throws IOException, GeneralSecurityException {
        return exec("exec:" + cmd);
    }

    void reboot() throws IOException, GeneralSecurityException {
        try {
            exec("reboot:");
        } catch (IOException e) {
            // the connection drops while the TV restarts
        }
    }

    boolean tlsPaired() {
        return new File(dir, "adb_tls_paired").exists();
    }

    /** One-time pairing with the code shown under Developer options > Wireless debugging > Pair device. */
    synchronized void pair(int port, String code) throws IOException, GeneralSecurityException {
        AdbPairing.pair(LOOPBACK, port, code, keys, certificate(), publicKeyLine());
        writeAll(new File(dir, "adb_tls_paired"), new byte[]{1});
        tlsPort = -1;
        dropSession();
        state = "adb.paired";
    }

    /**
     * A long-lived ADB stream (stdin + stdout), e.g. "exec:cmd package install -S n" fed with an APK,
     * or the key injector that stays open for fast remote keys.
     */
    Stream openStream(String service) throws IOException, GeneralSecurityException {
        return new Stream(openChannel(service, Channel.STREAM_OUTPUT_LIMIT));
    }

    static final class Stream implements java.io.Closeable {
        private static final int CHUNK = 32 * 1024;
        private final Channel ch;

        Stream(Channel ch) {
            this.ch = ch;
        }

        boolean isOpen() {
            return !ch.closed;
        }

        /** Writes to the process's stdin; waits for adbd to accept each chunk (ADB flow control). */
        synchronized void write(byte[] data) throws IOException {
            for (int off = 0; off < data.length; off += CHUNK) {
                int n = Math.min(CHUNK, data.length - off);
                byte[] chunk = new byte[n];
                System.arraycopy(data, off, chunk, 0, n);
                ch.write(chunk);
            }
        }

        /** Waits until the remote side closes the stream (process exit), then returns its output. */
        String awaitOutput(long timeoutMs) {
            ch.awaitClose(timeoutMs);
            return new String(ch.output(), UTF8);
        }

        @Override
        public void close() {
            ch.close();
        }
    }

    // ---------------- transport: one long-lived connection, one channel per command ----------------
    //
    // adbd multiplexes many streams over one connection (that is how the real `adb` works). Keeping a
    // single connection means the "Allow debugging?" approval is asked at most once per connection:
    // even when the TV does not remember "Always allow" (unticked, or Android 12's key-store bug),
    // the panel keeps working until the TV restarts. It also saves a handshake per command.

    private static final class Conn {
        Socket socket;
        DataInputStream in;
        OutputStream out;
    }

    private final Object sessionLock = new Object();
    private volatile Session session;

    private void dropSession() {
        Session s = session;
        session = null;
        if (s != null) s.kill();
    }

    /** The live session, connecting (and maybe asking for approval) when there is none. */
    private Session session() throws IOException, GeneralSecurityException {
        Session s = session;
        if (s != null && !s.dead) return s;
        notWhileApproving();
        synchronized (sessionLock) {
            s = session;
            if (s != null && !s.dead) return s;
            notWhileApproving();
            s = new Session(open());
            session = s;
            return s;
        }
    }

    private Channel openChannel(String service, int outputLimit) throws IOException, GeneralSecurityException {
        for (int attempt = 0; ; attempt++) {
            Session s = session();
            try {
                return s.open(service, outputLimit);
            } catch (StaleSessionException e) {
                // The connection died while idle (TV slept, adbd restarted): reconnect once.
                s.kill();
                if (attempt > 0) throw new IOException("adb.failed");
            }
        }
    }

    private byte[] exec(String service) throws IOException, GeneralSecurityException {
        Channel ch = openChannel(service, Integer.MAX_VALUE);
        try {
            if (!ch.awaitClose(60_000)) throw new IOException("adb.timeout");
            return ch.output();
        } finally {
            ch.close();
        }
    }

    private static final class StaleSessionException extends IOException {
        StaleSessionException() {
            super("adb.failed");
        }
    }

    private static final class Session {
        final Conn c;
        final java.util.concurrent.ConcurrentHashMap<Integer, Channel> channels = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger(1);
        volatile boolean dead;

        Session(Conn c) throws IOException {
            this.c = c;
            c.socket.setSoTimeout(0);   // idle connections are normal; channels have their own timeouts
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    readLoop();
                }
            }, "adb-session");
            t.setDaemon(true);
            t.start();
        }

        void send(int cmd, int arg0, int arg1, byte[] data) throws IOException {
            synchronized (c) {
                AdbClient.send(c.out, cmd, arg0, arg1, data);
            }
        }

        Channel open(String service, int outputLimit) throws IOException {
            if (dead) throw new StaleSessionException();
            Channel ch = new Channel(this, nextId.getAndIncrement(), outputLimit);
            channels.put(ch.localId, ch);
            try {
                send(A_OPEN, ch.localId, 0, (service + "\0").getBytes(UTF8));
            } catch (IOException e) {
                kill();
                throw new StaleSessionException();
            }
            int r = ch.awaitOpen(10_000);
            if (r == Channel.OPEN_OK) return ch;
            channels.remove(ch.localId);
            if (r == Channel.OPEN_TIMEOUT || dead) {
                kill();
                throw new StaleSessionException();
            }
            throw new IOException("stream refused: " + service);
        }

        private void readLoop() {
            try {
                while (!dead) {
                    Msg m = read(c.in);
                    Channel ch = channels.get(m.arg1);
                    if (m.cmd == A_OKAY) {
                        if (ch != null) ch.onOkay(m.arg0);
                    } else if (m.cmd == A_WRTE) {
                        if (ch != null) ch.onData(m.data);
                        send(A_OKAY, m.arg1, m.arg0, null);   // always acknowledge, or adbd stalls
                    } else if (m.cmd == A_CLSE) {
                        if (ch != null) {
                            channels.remove(ch.localId);
                            ch.onClose();
                        }
                    }
                }
            } catch (IOException ignored) {
                // connection dropped
            } finally {
                kill();
            }
        }

        void kill() {
            dead = true;
            try {
                c.socket.close();
            } catch (IOException ignored) {
            }
            for (Channel ch : channels.values()) ch.onClose();
            channels.clear();
        }
    }

    static final class Channel {
        static final int STREAM_OUTPUT_LIMIT = 64 * 1024;
        static final int OPEN_WAITING = 0, OPEN_OK = 1, OPEN_REFUSED = 2, OPEN_TIMEOUT = 3;
        final Session s;
        final int localId;
        private final int outputLimit;
        private volatile int remoteId;
        private int openState = OPEN_WAITING;
        volatile boolean closed;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final java.util.concurrent.Semaphore okay = new java.util.concurrent.Semaphore(0);

        Channel(Session s, int localId, int outputLimit) {
            this.s = s;
            this.localId = localId;
            this.outputLimit = outputLimit;
        }

        synchronized int awaitOpen(long ms) {
            long end = System.currentTimeMillis() + ms;
            while (openState == OPEN_WAITING && !closed) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) return OPEN_TIMEOUT;
                try {
                    wait(left);
                } catch (InterruptedException e) {
                    return OPEN_TIMEOUT;
                }
            }
            return openState == OPEN_OK ? OPEN_OK : OPEN_REFUSED;
        }

        synchronized void onOkay(int remote) {
            if (openState == OPEN_WAITING) {
                remoteId = remote;
                openState = OPEN_OK;
                notifyAll();
            } else {
                okay.release();
            }
        }

        void onData(byte[] d) {
            synchronized (out) {
                int room = outputLimit - out.size();
                if (room > 0) out.write(d, 0, Math.min(room, d.length));
            }
        }

        synchronized void onClose() {
            closed = true;
            if (openState == OPEN_WAITING) openState = OPEN_REFUSED;
            okay.release();
            notifyAll();
        }

        /** True when the remote side closed the stream within the time limit. */
        synchronized boolean awaitClose(long ms) {
            long end = System.currentTimeMillis() + ms;
            while (!closed) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) return false;
                try {
                    wait(left);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return true;
        }

        byte[] output() {
            synchronized (out) {
                return out.toByteArray();
            }
        }

        void write(byte[] chunk) throws IOException {
            if (closed) throw new IOException("stream closed");
            s.send(A_WRTE, localId, remoteId, chunk);
            try {
                if (!okay.tryAcquire(30, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("stream stalled");
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
            if (closed) throw new IOException("stream closed");
        }

        void close() {
            boolean wasOpen;
            synchronized (this) {
                wasOpen = !closed && openState == OPEN_OK;
                closed = true;
                notifyAll();
            }
            s.channels.remove(localId);
            if (wasOpen) {
                try {
                    s.send(A_CLSE, localId, remoteId, null);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** Paired with wireless debugging: encrypted TLS first, classic port as fallback. Otherwise classic only. */
    private Conn open() throws IOException, GeneralSecurityException {
        if (tlsPaired()) {
            try {
                Conn c = openTls();
                transport = "tls";
                return c;
            } catch (IOException tlsFailed) {
                try {
                    Conn c = connect(CLASSIC_PORT);
                    transport = "classic";
                    return c;
                } catch (ConnectException refused) {
                    state = "adb.wireless_off";
                    throw new ConnectException("adb.wireless_off");
                }
            }
        }
        try {
            Conn c = connect(CLASSIC_PORT);
            transport = "classic";
            return c;
        } catch (ConnectException refused) {
            // nothing listens on 5555: debugging is off (the message key is shown to the user)
            state = "adb.refused";
            throw new ConnectException("adb.refused");
        }
    }

    /** Wireless debugging: the port changes whenever it is switched on, so look it up again on failure. */
    private Conn openTls() throws IOException, GeneralSecurityException {
        IOException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (tlsPort <= 0 || attempt > 0) tlsPort = finder.find("_adb-tls-connect._tcp");
            try {
                return connect(tlsPort);
            } catch (ConnectException e) {
                last = e;
            }
        }
        throw last;
    }

    private Conn connect(int port) throws IOException, GeneralSecurityException {
        Conn c = new Conn();
        boolean handedOff = false;
        c.socket = new Socket();
        try {
            c.socket.connect(new InetSocketAddress(LOOPBACK, port), 3000);
            c.socket.setSoTimeout(10000);
            c.in = new DataInputStream(c.socket.getInputStream());
            c.out = c.socket.getOutputStream();
            send(c.out, A_CNXN, A_VERSION, MAX_PAYLOAD, "host::\0".getBytes(ASCII));
            boolean sentSignature = false;
            boolean sentKey = false;
            while (true) {
                Msg m = read(c.in);
                if (m.cmd == A_CNXN) {
                    state = "adb.connected";
                    awaitingApproval = false;
                    return c;
                }
                if (m.cmd == A_STLS) {
                    // TLS transport: answer, then switch the stream to TLS 1.3 with our client certificate.
                    send(c.out, A_STLS, A_STLS_VERSION, 0, null);
                    SSLSocket s = (SSLSocket) tlsContext().getSocketFactory()
                            .createSocket(c.socket, LOOPBACK, port, true);
                    s.setUseClientMode(true);
                    s.setEnabledProtocols(new String[]{"TLSv1.3"});
                    s.startHandshake();
                    c.socket = s;
                    c.in = new DataInputStream(s.getInputStream());
                    c.out = s.getOutputStream();
                    continue;
                }
                if (m.cmd != A_AUTH || m.arg0 != AUTH_TOKEN) continue;
                if (!sentSignature) {
                    send(c.out, A_AUTH, AUTH_SIGNATURE, 0, sign(m.data));
                    sentSignature = true;
                } else if (!sentKey) {
                    // Key unknown: the TV shows an "Allow debugging?" dialog; wait for the user.
                    send(c.out, A_AUTH, AUTH_RSAPUBLICKEY, 0, (publicKeyLine() + "\0").getBytes(ASCII));
                    sentKey = true;
                    // The TV now shows "Allow debugging?". Wait for the answer in the background so no
                    // request (and no phone screen) hangs meanwhile; this request reports the wait.
                    state = "adb.waiting_approval";
                    awaitingApproval = true;
                    final Conn pending = c;
                    pendingApproval = c;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            awaitApproval(pending);
                        }
                    }, "adb-approval").start();
                    handedOff = true;
                    throw new IOException("adb.waiting_approval");
                } else {
                    state = "adb.rejected";
                    throw new IOException("adb.rejected");
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            if (handedOff) throw e;
            awaitingApproval = false;
            c.socket.close();
            if (!(e instanceof ConnectException)) {
                // The approval dialog was cancelled or timed out; the next request shows it again.
                state = "adb.waiting_approval".equals(state) ? "adb.not_approved" : "adb.failed";
            }
            throw e;
        }
    }

    private volatile Conn pendingApproval;

    /**
     * Gives up on the prompt we are waiting for (it was dismissed, or never shown), so the next
     * request connects again and the TV shows a fresh "Allow debugging?" window.
     */
    void abandonApproval() {
        Conn c = pendingApproval;
        if (c == null) return;
        try {
            c.socket.close();  // awaitApproval ends with "not approved"
        } catch (IOException ignored) {
        }
        for (int i = 0; i < 20 && awaitingApproval; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** The user dismissed or declined the prompt: background polling must not raise it again. */
    boolean declined() {
        return "adb.not_approved".equals(state) || "adb.rejected".equals(state);
    }

    /** Explicit "show the prompt again" from the user (TV button or the phone's "Check again"). */
    void retry() {
        abandonApproval();
        if (declined()) state = "adb.not_connected";
    }

    /** Waits (up to 10 minutes) for the user to answer the TV's "Allow debugging?" prompt. */
    private void awaitApproval(Conn c) {
        try {
            c.socket.setSoTimeout(600_000);
            while (true) {
                Msg m = read(c.in);
                if (m.cmd == A_CNXN) {
                    // Approved: this very connection becomes the live session (no second prompt).
                    synchronized (sessionLock) {
                        Session old = session;
                        session = new Session(c);
                        if (old != null) old.kill();
                    }
                    transport = c.socket instanceof SSLSocket ? "tls" : "classic";
                    state = "adb.connected";
                    return;
                }
            }
        } catch (Exception e) {
            state = "adb.not_approved";
            try {
                c.socket.close();
            } catch (IOException ignored) {
            }
        } finally {
            pendingApproval = null;
            awaitingApproval = false;
        }
    }

    private synchronized SSLContext tlsContext() throws GeneralSecurityException, IOException {
        if (tls != null) return tls;
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        char[] pw = new char[0];
        ks.setKeyEntry("adb", keys.getPrivate(), pw, new java.security.cert.Certificate[]{certificate()});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pw);
        SSLContext ctx = SSLContext.getInstance("TLS");
        // adbd presents a self-signed certificate; we only ever connect to 127.0.0.1.
        ctx.init(kmf.getKeyManagers(), new TrustManager[]{TRUST_LOOPBACK}, new SecureRandom());
        tls = ctx;
        return ctx;
    }

    static final X509TrustManager TRUST_LOOPBACK = new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] c, String a) { }
        @Override public void checkServerTrusted(X509Certificate[] c, String a) { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    };

    synchronized X509Certificate certificate() throws GeneralSecurityException {
        if (cert == null) cert = X509Gen.selfSigned(keys, "odintv");
        return cert;
    }

    private byte[] sign(byte[] token) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, keys.getPrivate());
        c.update(SIGNATURE_PADDING);
        return c.doFinal(token);
    }

    /** adbd's mincrypt RSAPublicKey, base64 + " name" (same format as ~/.android/adbkey.pub). */
    String publicKeyLine() {
        RSAPublicKey pub = (RSAPublicKey) keys.getPublic();
        BigInteger n = pub.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = r32.subtract(n.remainder(r32).modInverse(r32));
        BigInteger rr = BigInteger.ONE.shiftLeft(KEY_BITS * 2).mod(n);

        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + KEY_BYTES + KEY_BYTES + 4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(KEY_BYTES / 4);
        bb.putInt(n0inv.intValue());
        bb.put(littleEndian(n));
        bb.put(littleEndian(rr));
        bb.putInt(pub.getPublicExponent().intValue());
        return Base64.encodeToString(bb.array(), Base64.NO_WRAP) + " " + KEY_NAME;
    }

    private static byte[] littleEndian(BigInteger v) {
        byte[] be = v.toByteArray();
        byte[] le = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES && i < be.length; i++) le[i] = be[be.length - 1 - i];
        return le;
    }

    private static KeyPair loadOrCreateKeys(File dir) throws GeneralSecurityException, IOException {
        File priv = new File(dir, "adbkey");
        File pub = new File(dir, "adbkey.pub");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        if (priv.exists() && pub.exists()) {
            return new KeyPair(kf.generatePublic(new X509EncodedKeySpec(readAll(pub))),
                    kf.generatePrivate(new PKCS8EncodedKeySpec(readAll(priv))));
        }
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(KEY_BITS);
        KeyPair kp = g.generateKeyPair();
        writeAll(priv, kp.getPrivate().getEncoded());
        writeAll(pub, kp.getPublic().getEncoded());
        return kp;
    }

    static byte[] readAll(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try {
            in.readFully(b);
        } finally {
            in.close();
        }
        return b;
    }

    static void writeAll(File f, byte[] b) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(b);
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (!tmp.renameTo(f)) throw new IOException("cannot write " + f.getName());
    }

    private static final class Msg {
        int cmd, arg0, arg1;
        byte[] data;
    }

    static void send(OutputStream out, int cmd, int arg0, int arg1, byte[] data) throws IOException {
        int len = data == null ? 0 : data.length;
        int sum = 0;
        for (int i = 0; i < len; i++) sum += data[i] & 0xff;
        ByteBuffer bb = ByteBuffer.allocate(24 + len).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(cmd).putInt(arg0).putInt(arg1).putInt(len).putInt(sum).putInt(~cmd);
        if (len > 0) bb.put(data);
        out.write(bb.array());
        out.flush();
    }

    static Msg read(DataInputStream in) throws IOException {
        byte[] h = new byte[24];
        in.readFully(h);
        ByteBuffer bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        Msg m = new Msg();
        m.cmd = bb.getInt();
        m.arg0 = bb.getInt();
        m.arg1 = bb.getInt();
        int len = bb.getInt();
        if (len < 0 || len > 1024 * 1024) throw new IOException("invalid ADB message");
        m.data = new byte[len];
        in.readFully(m.data);
        return m;
    }
}
