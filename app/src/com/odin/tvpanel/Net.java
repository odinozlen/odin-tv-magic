package com.odin.tvpanel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/** The TV's address on the home network, as a phone on the same Wi-Fi reaches it. */
final class Net {
    private Net() {
    }

    static String ip(Context ctx) {
        // 1) Wi-Fi or Ethernet network (never a VPN, Wi-Fi Direct or a container bridge).
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                String best = null;
                for (Network n : cm.getAllNetworks()) {
                    NetworkCapabilities c = cm.getNetworkCapabilities(n);
                    LinkProperties lp = cm.getLinkProperties(n);
                    if (c == null || lp == null || c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue;
                    boolean eth = c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                    boolean wifi = c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    if (!eth && !wifi) continue;
                    for (LinkAddress la : lp.getLinkAddresses()) {
                        InetAddress a = la.getAddress();
                        if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                            if (eth) return a.getHostAddress();      // wired wins: that is the active one
                            if (best == null) best = a.getHostAddress();
                        }
                    }
                }
                if (best != null) return best;
            }
        } catch (Exception ignored) {
        }
        // 2) Fallback: a private IPv4 on a wlan*/eth* interface, then any private IPv4.
        String any = null;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String name = ni.getName();
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    if (name.startsWith("wlan") || name.startsWith("eth")) return a.getHostAddress();
                    if (any == null && !name.startsWith("p2p") && !name.startsWith("tun")) any = a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return any == null ? "?" : any;
    }
}
