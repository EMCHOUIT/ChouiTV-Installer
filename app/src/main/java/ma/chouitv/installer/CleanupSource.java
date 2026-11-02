package ma.chouitv.installer;

import ma.chouitv.installer.cleanup.CleanupData;

/**
 * Which cleanup lists the app believes. The copy published online (optimize/lists/cleanup.json in the ChouiTV repository) is used
 * only when its signature (cleanup.json.sig, made with the owner\x27s manifest key, the one that signs latest.json) matches; an unsigned,
 * wrongly signed, changed or unreadable copy is ignored and the lists built into the app are used. Even a signed copy cannot make the
 * protected list smaller: what the app was built with is a floor, the online lists can only add to it.
 */
final class CleanupSource {
    private CleanupSource() {}

    static CleanupData choose(CleanupData bundled, byte[] online, String signature) {
        return choose(bundled, online, signature, ManifestTrust.PUBLIC_KEY);
    }

    static CleanupData choose(CleanupData bundled, byte[] online, String signature, String publicKey) {
        if (online == null || !ManifestTrust.verify(online, signature, publicKey)) return bundled;
        CleanupData d;
        try {
            d = CleanupData.parse(new String(online, "UTF-8"));
        } catch (Exception unreadable) {
            return bundled;
        }
        d.keep.addAll(bundled.keep);
        d.homes.addAll(bundled.homes);
        // the names this app was built with fill any name the published lists do not have yet
        for (java.util.Map.Entry<String, String> e : bundled.display.entrySet()) d.display.putIfAbsent(e.getKey(), e.getValue());
        for (java.util.Map.Entry<String, String> e : bundled.namesAr.entrySet()) d.namesAr.putIfAbsent(e.getKey(), e.getValue());
        for (java.util.Map.Entry<String, String> e : bundled.namesFr.entrySet()) d.namesFr.putIfAbsent(e.getKey(), e.getValue());
        return d;
    }
}
