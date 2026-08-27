package com.abosultan.darbaktools;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class NetworkUtils {
    private NetworkUtils() {}

    public static String localIpv4(Context context) {
        String wifi = wifiIpv4(context);
        if (isUsable(wifi)) return wifi;

        List<AddressCandidate> candidates = candidates();
        return candidates.isEmpty() ? null : candidates.get(0).address;
    }

    public static List<String> allLocalIpv4() {
        List<AddressCandidate> candidates = candidates();
        List<String> result = new ArrayList<>();
        for (AddressCandidate candidate : candidates) {
            if (!result.contains(candidate.address)) result.add(candidate.address);
        }
        return result;
    }

    private static String wifiIpv4(Context context) {
        if (context == null) return null;
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            WifiInfo info = wm.getConnectionInfo();
            if (info == null) return null;
            int ip = info.getIpAddress();
            if (ip == 0) return null;
            return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<AddressCandidate> candidates() {
        List<AddressCandidate> result = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;
                String interfaceName = networkInterface.getName() == null ? "" : networkInterface.getName().toLowerCase();
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress() || address.isLinkLocalAddress()) continue;
                    String host = address.getHostAddress();
                    if (!isUsable(host)) continue;
                    result.add(new AddressCandidate(host, score(interfaceName, host)));
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(result, new Comparator<AddressCandidate>() {
            @Override public int compare(AddressCandidate a, AddressCandidate b) {
                return b.score - a.score;
            }
        });
        return result;
    }

    private static int score(String name, String host) {
        int score = isPrivate(host) ? 100 : 0;
        if (name.startsWith("wlan") || name.contains("wifi")) score += 80;
        else if (name.startsWith("eth")) score += 60;
        else if (name.startsWith("ap") || name.contains("hotspot")) score += 50;
        else if (name.startsWith("rmnet") || name.startsWith("ccmni")) score -= 80;
        else if (name.startsWith("tun") || name.startsWith("ppp")) score -= 100;
        return score;
    }

    private static boolean isUsable(String host) {
        return host != null && host.length() > 0 && !host.startsWith("127.") && !host.startsWith("169.254.") && !"0.0.0.0".equals(host);
    }

    private static boolean isPrivate(String host) {
        if (host == null) return false;
        if (host.startsWith("10.")) return true;
        if (host.startsWith("192.168.")) return true;
        if (host.startsWith("172.")) {
            try {
                String[] p = host.split("\\.");
                int second = Integer.parseInt(p[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static final class AddressCandidate {
        final String address;
        final int score;
        AddressCandidate(String address, int score) {
            this.address = address;
            this.score = score;
        }
    }
}
