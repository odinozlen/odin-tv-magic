package com.odin.tvpanel;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Finds services this TV itself announces over mDNS: its wireless-debugging ports
 * (_adb-tls-connect._tcp / _adb-tls-pairing._tcp) and its AirPlay receiver. Other devices on the
 * network announce theirs too, so only a service whose address belongs to this TV is used.
 */
final class MdnsFinder implements AdbClient.PortFinder {
    private final long timeoutMs;
    private final Context ctx;

    MdnsFinder(Context ctx) {
        this(ctx, 8000);
    }

    MdnsFinder(Context ctx, long timeoutMs) {
        this.ctx = ctx;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public int find(String serviceType) throws IOException {
        return findOwn(serviceType).getPort();
    }

    /** The instance name this TV announces for a service type (e.g. its AirPlay name). */
    String findName(String serviceType) throws IOException {
        return unescape(findOwn(serviceType).getServiceName());
    }

    /** Some Android versions return DNS escapes ("Odin\\032TV"); undo them. */
    static String unescape(String n) {
        if (n == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == '\\' && i + 3 < n.length() && n.substring(i + 1, i + 4).matches("[0-9]{3}")) {
                b.append((char) Integer.parseInt(n.substring(i + 1, i + 4)));
                i += 3;
            } else if (c == '\\' && i + 1 < n.length()) {
                b.append(n.charAt(++i));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private NsdServiceInfo findOwn(String serviceType) throws IOException {
        NsdManager nsd = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
        final LinkedBlockingQueue<NsdServiceInfo> found = new LinkedBlockingQueue<>();
        final CountDownLatch started = new CountDownLatch(1);
        NsdManager.DiscoveryListener l = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String t, int e) { started.countDown(); }
            @Override public void onStopDiscoveryFailed(String t, int e) { }
            @Override public void onDiscoveryStarted(String t) { started.countDown(); }
            @Override public void onDiscoveryStopped(String t) { }
            @Override public void onServiceFound(NsdServiceInfo s) { found.offer(s); }
            @Override public void onServiceLost(NsdServiceInfo s) { }
        };
        nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, l);
        List<InetAddress> mine = localAddresses();
        long end = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < end) {
                NsdServiceInfo s = found.poll(end - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (s == null) break;
                NsdServiceInfo r = resolve(nsd, s);
                if (r != null && r.getHost() != null && (r.getHost().isLoopbackAddress() || mine.contains(r.getHost()))) {
                    return r;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                nsd.stopServiceDiscovery(l);
            } catch (RuntimeException ignored) {
            }
        }
        throw new IOException("mdns.not_found");
    }

    @SuppressWarnings("deprecation")
    private static NsdServiceInfo resolve(NsdManager nsd, NsdServiceInfo s) throws InterruptedException {
        final NsdServiceInfo[] out = new NsdServiceInfo[1];
        final CountDownLatch done = new CountDownLatch(1);
        nsd.resolveService(s, new NsdManager.ResolveListener() {
            @Override public void onResolveFailed(NsdServiceInfo i, int e) { done.countDown(); }
            @Override public void onServiceResolved(NsdServiceInfo i) { out[0] = i; done.countDown(); }
        });
        done.await(4, TimeUnit.SECONDS);
        return out[0];
    }

    private static List<InetAddress> localAddresses() {
        List<InetAddress> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address || !a.isLinkLocalAddress()) out.add(a);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
