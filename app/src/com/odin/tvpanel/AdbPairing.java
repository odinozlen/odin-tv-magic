package com.odin.tvpanel;

import android.net.ssl.SSLSockets;
import android.os.Build;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

/**
 * Android 11+ "Wireless debugging" pairing client, following AOSP
 * packages/modules/adb/pairing_connection and pairing_auth (Apache-2.0):
 *
 *  1. TLS 1.3 to the pairing port, both sides with self-signed certificates.
 *  2. password = pairing code + 64 bytes exported from the TLS session ("adb-label\0").
 *  3. SPAKE2 (client = alice) -> 64-byte key -> HKDF-SHA256 -> AES-128-GCM key.
 *  4. Exchange encrypted PeerInfo; ours carries our ADB public key, which adbd then trusts.
 *
 * A wrong code makes the TV's decryption fail and it drops the connection.
 */
final class AdbPairing {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final byte[] CLIENT_NAME = "adb pair client\0".getBytes(UTF8);
    private static final byte[] SERVER_NAME = "adb pair server\0".getBytes(UTF8);
    private static final int HEADER_VERSION = 1;
    private static final int TYPE_SPAKE2_MSG = 0;
    private static final int TYPE_PEER_INFO = 1;
    private static final int PEER_INFO_SIZE = 8192;
    private static final int PEER_RSA_PUB_KEY = 0;

    private AdbPairing() {
    }

    static void pair(String host, int port, String code, KeyPair keys, X509Certificate cert, String publicKeyLine)
            throws IOException, GeneralSecurityException {
        if (Build.VERSION.SDK_INT < 31) throw new IOException("pair.needs_android12");
        if (code == null || !code.matches("\\d{6}")) throw new IOException("pair.bad_code_format");

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        char[] pw = new char[0];
        ks.setKeyEntry("adb", keys.getPrivate(), pw, new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pw);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), new TrustManager[]{AdbClient.TRUST_LOOPBACK}, new SecureRandom());

        SSLSocket s = (SSLSocket) ctx.getSocketFactory().createSocket(host, port);
        try {
            s.setSoTimeout(15000);
            s.setEnabledProtocols(new String[]{"TLSv1.3"});
            s.startHandshake();
            byte[] ekm = SSLSockets.exportKeyingMaterial(s, "adb-label\0", null, 64);
            if (ekm == null) throw new IOException("TLS export failed");

            byte[] codeBytes = code.getBytes(UTF8);
            byte[] password = new byte[codeBytes.length + ekm.length];
            System.arraycopy(codeBytes, 0, password, 0, codeBytes.length);
            System.arraycopy(ekm, 0, password, codeBytes.length, ekm.length);

            DataInputStream in = new DataInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();

            Spake2 spake = new Spake2(Spake2.ALICE, CLIENT_NAME, SERVER_NAME);
            write(out, TYPE_SPAKE2_MSG, spake.generateMsg(password));
            byte[] theirMsg = read(in, TYPE_SPAKE2_MSG);
            byte[] keyMaterial = spake.processMsg(theirMsg);
            if (keyMaterial == null) throw new IOException("pair.failed");
            SecretKeySpec aes = new SecretKeySpec(
                    hkdfSha256(keyMaterial, "adb pairing_auth aes-128-gcm key".getBytes(UTF8), 16), "AES");

            byte[] peerInfo = new byte[PEER_INFO_SIZE];
            peerInfo[0] = PEER_RSA_PUB_KEY;
            byte[] pub = publicKeyLine.getBytes(UTF8);
            System.arraycopy(pub, 0, peerInfo, 1, Math.min(pub.length, PEER_INFO_SIZE - 2));
            write(out, TYPE_PEER_INFO, gcm(Cipher.ENCRYPT_MODE, aes, peerInfo));

            byte[] theirInfo;
            try {
                theirInfo = gcm(Cipher.DECRYPT_MODE, aes, read(in, TYPE_PEER_INFO));
            } catch (IOException | GeneralSecurityException e) {
                // The TV closes the connection (or sends garbage) when the code was wrong.
                throw new IOException("pair.wrong_code");
            }
            if (theirInfo.length != PEER_INFO_SIZE) throw new IOException("pair.failed");
        } finally {
            s.close();
        }
    }

    /** Nonce = 64-bit little-endian sequence number (always 0: one message each way) + 4 zero bytes. */
    private static byte[] gcm(int mode, SecretKeySpec key, byte[] data) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(mode, key, new GCMParameterSpec(128, new byte[12]));
        return c.doFinal(data);
    }

    /** RFC 5869 with an empty salt (= HashLen zero bytes). */
    static byte[] hkdfSha256(byte[] ikm, byte[] info, int len) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] out = new byte[len];
        byte[] t = new byte[0];
        int pos = 0;
        for (int i = 1; pos < len; i++) {
            mac.update(t);
            mac.update(info);
            mac.update((byte) i);
            t = mac.doFinal();
            int n = Math.min(t.length, len - pos);
            System.arraycopy(t, 0, out, pos, n);
            pos += n;
        }
        return out;
    }

    private static void write(OutputStream out, int type, byte[] payload) throws IOException {
        ByteBuffer h = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        h.put((byte) HEADER_VERSION).put((byte) type).putInt(payload.length);
        out.write(h.array());
        out.write(payload);
        out.flush();
    }

    private static byte[] read(DataInputStream in, int expectedType) throws IOException {
        byte[] h = new byte[6];
        in.readFully(h);
        ByteBuffer bb = ByteBuffer.wrap(h).order(ByteOrder.BIG_ENDIAN);
        int version = bb.get() & 0xff;
        int type = bb.get() & 0xff;
        int len = bb.getInt();
        if (version != HEADER_VERSION || type != expectedType || len <= 0 || len > PEER_INFO_SIZE * 2) {
            throw new IOException("pair.failed");
        }
        byte[] b = new byte[len];
        in.readFully(b);
        return b;
    }
}
