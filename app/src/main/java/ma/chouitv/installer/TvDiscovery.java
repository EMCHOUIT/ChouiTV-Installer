package ma.chouitv.installer;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.net.Inet4Address;
import java.util.ArrayDeque;

/**
 * Android TVs announce themselves on the home network by their name (the «Android TV remote» service). This listens for them, so that a TV can be named and
 * shown even before it was ever authorised, and even when its network debugging is off (nothing answers on the debugging port, but the TV is seen).
 */
final class TvDiscovery {
    interface Seen { void tv(String ip, String name); }

    private static final String TYPE = "_androidtvremote2._tcp";
    private final NsdManager nsd;
    private final Seen seen;
    private NsdManager.DiscoveryListener listener;
    private volatile boolean on;
    private final ArrayDeque<NsdServiceInfo> queue = new ArrayDeque<>();
    private boolean resolving;
    private long resolveStart;

    TvDiscovery(Context c, Seen seen) {
        this.nsd = (NsdManager) c.getSystemService(Context.NSD_SERVICE);
        this.seen = seen;
    }

    void start() {
        if (nsd == null || on) return;
        on = true;
        listener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String t, int e) { on = false; }
            @Override public void onStopDiscoveryFailed(String t, int e) { }
            @Override public void onDiscoveryStarted(String t) { }
            @Override public void onDiscoveryStopped(String t) { }
            @Override public void onServiceFound(NsdServiceInfo i) { enqueue(i); }
            @Override public void onServiceLost(NsdServiceInfo i) { }
        };
        try { nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, listener); } catch (Exception e) { on = false; }
    }

    void stop() {
        if (!on) return;
        on = false;
        try { nsd.stopServiceDiscovery(listener); } catch (Exception ignored) { }
    }

    private synchronized void enqueue(NsdServiceInfo i) {
        queue.add(i);
        next();
    }

    /** One name is resolved at a time (Android refuses two at once); a try that never answers is given up after three seconds. */
    private synchronized void next() {
        long now = System.currentTimeMillis();
        if (!on || queue.isEmpty() || (resolving && now - resolveStart < 3000)) return;
        resolving = true;
        resolveStart = now;
        NsdServiceInfo i = queue.poll();
        try {
            nsd.resolveService(i, new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo s, int e) { done(); }
                @Override public void onServiceResolved(NsdServiceInfo s) {
                    if (s.getHost() instanceof Inet4Address) seen.tv(s.getHost().getHostAddress(), s.getServiceName());
                    done();
                }
            });
        } catch (Exception e) { done(); }
    }

    private synchronized void done() {
        resolving = false;
        next();
    }
}
