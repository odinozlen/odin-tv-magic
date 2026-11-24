package com.odin.tvpanel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Minimal self-signed X.509 v3 certificate (SHA256withRSA) for the ADB TLS client
 * certificate. adbd only looks at the public key inside it, so no extensions are needed.
 */
final class X509Gen {
    private static final byte[] OID_SHA256_RSA = {0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x0b};
    private static final byte[] OID_CN = {0x55, 0x04, 0x03};

    private X509Gen() {
    }

    static X509Certificate selfSigned(KeyPair keys, String cn) throws GeneralSecurityException {
        byte[] algId = seq(cat(oid(OID_SHA256_RSA), new byte[]{0x05, 0x00}));
        byte[] name = seq(set(seq(cat(oid(OID_CN), tlv(0x0c, utf8(cn))))));
        SimpleDateFormat utc = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.ROOT);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        byte[] validity = seq(cat(
                tlv(0x17, utf8(utc.format(new Date(System.currentTimeMillis() - 86_400_000L)))),
                tlv(0x17, utf8("491231235959Z"))));
        byte[] serial = new BigInteger(63, new SecureRandom()).add(BigInteger.ONE).toByteArray();
        byte[] tbs = seq(cat(
                tlv(0xa0, tlv(0x02, new byte[]{0x02})),   // version v3
                tlv(0x02, serial),
                algId, name, validity, name,
                keys.getPublic().getEncoded()));          // SubjectPublicKeyInfo
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(keys.getPrivate());
        s.update(tbs);
        byte[] sig = s.sign();
        byte[] bitString = new byte[sig.length + 1];
        System.arraycopy(sig, 0, bitString, 1, sig.length);
        byte[] der = seq(cat(tbs, algId, tlv(0x03, bitString)));
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));
    }

    private static byte[] utf8(String s) {
        return s.getBytes(java.nio.charset.Charset.forName("UTF-8"));
    }

    private static byte[] oid(byte[] body) {
        return tlv(0x06, body);
    }

    private static byte[] seq(byte[] body) {
        return tlv(0x30, body);
    }

    private static byte[] set(byte[] body) {
        return tlv(0x31, body);
    }

    private static byte[] tlv(int tag, byte[] body) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(tag);
        int n = body.length;
        if (n < 0x80) {
            o.write(n);
        } else if (n < 0x100) {
            o.write(0x81);
            o.write(n);
        } else if (n < 0x10000) {
            o.write(0x82);
            o.write(n >> 8);
            o.write(n);
        } else {
            o.write(0x83);
            o.write(n >> 16);
            o.write(n >> 8);
            o.write(n);
        }
        o.write(body, 0, n);
        return o.toByteArray();
    }

    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) o.write(p, 0, p.length);
        return o.toByteArray();
    }
}
