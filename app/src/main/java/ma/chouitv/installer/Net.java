package ma.chouitv.installer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Everything that talks to the network but not to the TV: the release files, and the search for TVs on the Wi-Fi. */
final class Net {
    static final String MANIFEST = "https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json";
    static final String CHANNELS = "https://github.com/EMCHOUIT/ChouiTV/releases/download/channels/channels-release.apk";

    /** One APK of the release: package, URL and SHA-256 from latest.json. */
    static final class Apk { String pkg, url, sha; }

    static final class Release { String version; int code; String channelsSha = ""; List<Apk> apks = new ArrayList<>(); }

    /** The version file, accepted only if every line of it passes Trust: the home app (a legacy screensaver line is ignored), our releases, a checksum each, not older than allowed. */
    static Release latest() throws Exception {
        byte[] file = fetch(MANIFEST, Trust.MAX_MANIFEST_BYTES);
        String signature = new String(fetch(MANIFEST + ".sig", Trust.MAX_MANIFEST_BYTES), "UTF-8");
        if (!ManifestTrust.verify(file, signature)) throw new IOException("the version file is not signed by ChouiTV");
        JSONObject j = new JSONObject(new String(file, "UTF-8"));
        Release r = new Release();
        r.version = j.getString("versionName");
        r.code = j.getInt("versionCode");
        Trust.requireVersion(r.code, r.version);
        JSONObject ch = j.optJSONObject("channels");
        if (ch != null) r.channelsSha = ch.optString("sha256", "");
        JSONArray a = j.getJSONArray("apks");
        List<String> listed = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            String pkg = o.getString("package");
            listed.add(pkg);
            if (Trust.isLegacyListing(pkg)) continue;   // a 3.x version file: the old screensaver is never installed again
            Apk k = new Apk();
            k.pkg = pkg; k.url = o.getString("url"); k.sha = o.optString("sha256", "");
            Trust.requireApk(k.pkg, k.url, k.sha);
            r.apks.add(k);
        }
        Trust.installable(listed);   // the home app must be there
        return r;
    }

    static byte[] fetch(String url, int max) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        HttpURLConnection c = open(url);
        try (InputStream i = c.getInputStream()) { copy(i, b, max); } finally { c.disconnect(); }
        return b.toByteArray();
    }

    /** Downloads to a file and checks its SHA-256, which is mandatory: a file without a published checksum is not downloaded at all. */
    static File download(String url, File to, String sha, Adb.Progress p) throws Exception {
        if (!Trust.isSha256(sha)) throw new IOException("no checksum to check the download against");
        HttpURLConnection c = open(url);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long total = c.getContentLengthLong(), done = 0;
        if (total > Trust.MAX_APK_BYTES) { c.disconnect(); throw new IOException("the file is too big"); }
        try (InputStream i = c.getInputStream(); FileOutputStream o = new FileOutputStream(to)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = i.read(buf)) > 0) {
                o.write(buf, 0, n); md.update(buf, 0, n); done += n;
                if (done > Trust.MAX_APK_BYTES) throw new IOException("the file is too big");
                if (p != null) p.at(done, total);
            }
        } catch (IOException e) {
            to.delete();
            throw e;
        } finally { c.disconnect(); }
        StringBuilder h = new StringBuilder();
        for (byte x : md.digest()) h.append(String.format("%02x", x));
        if (!h.toString().equalsIgnoreCase(sha)) { to.delete(); throw new IOException("the downloaded file is not the published one (checksum)"); }
        if (to.length() < 100000) { to.delete(); throw new IOException("the downloaded file is too small"); }
        return to;
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new java.net.URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "ChouiTV-Installer");
        if (c.getResponseCode() / 100 != 2) throw new IOException("HTTP " + c.getResponseCode() + " " + url);
        return c;
    }

    private static void copy(InputStream i, java.io.OutputStream o, int max) throws IOException {
        byte[] buf = new byte[16384];
        int n, done = 0;
        while ((n = i.read(buf)) > 0) {
            o.write(buf, 0, n);
            done += n;
            if (done > max) throw new IOException("the answer is too long");
        }
    }

    /** Hosts of this phone's Wi-Fi network (a /24) that answer on the ADB port, found in about two seconds. */
    static List<String> findTvs(int port) {
        List<String> found = Collections.synchronizedList(new ArrayList<String>());
        String base = null, self = null;
        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements(); ) {
                NetworkInterface ni = e.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    String h = a.getHostAddress();
                    if (a instanceof java.net.Inet4Address && a.isSiteLocalAddress()) {
                        self = h; base = h.substring(0, h.lastIndexOf('.') + 1);
                    }
                }
            }
        } catch (Exception ignored) { }
        if (base == null) return found;
        ExecutorService ex = Executors.newFixedThreadPool(48);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 1; i < 255; i++) {
            final String ip = base + i;
            if (ip.equals(self)) continue;
            fs.add(ex.submit(new Callable<Void>() {
                public Void call() {
                    try (Socket s = new Socket()) { s.connect(new InetSocketAddress(ip, port), 600); found.add(ip); } catch (Exception ignored) { }
                    return null;
                }
            }));
        }
        for (Future<?> f : fs) { try { f.get(); } catch (Exception ignored) { } }
        ex.shutdown();
        List<String> sorted = new ArrayList<>(found);
        Collections.sort(sorted, new java.util.Comparator<String>() {
            public int compare(String a, String b) { return Integer.parseInt(a.substring(a.lastIndexOf('.') + 1)) - Integer.parseInt(b.substring(b.lastIndexOf('.') + 1)); }
        });
        return sorted;
    }
}
