package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ma.chouitv.installer.cleanup.Guard;

/** The apps of the TV that can be put on a button: read from the TV's answer, named in a way a person recognises. */
public final class Apps {
    private Apps() {}

    /** The apps that have a TV launcher entry (the ones that show on the TV's home). */
    public static final String LIST_COMMAND = "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER";

    private static final Pattern ACTIVITY = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)/\\S+");

    /** Reads either «package:name» lines or «name/activity» lines; each package once, in the order of the TV. */
    public static List<String> parse(String out) {
        List<String> r = new ArrayList<>();
        if (out == null) return r;
        for (String line : out.split("\\r?\\n")) {
            String l = line.trim(), p = null;
            if (l.startsWith("package:")) p = l.substring(8).trim();
            else { Matcher m = ACTIVITY.matcher(l); if (m.find()) p = m.group(1); }
            if (p != null && Guard.validPackage(p) && !r.contains(p)) r.add(p);
        }
        return r;
    }

    /** What the TV shows for an app: its real name and its icon (PNG bytes, may be absent). */
    public static final class Info {
        public final String label;
        public final byte[] icon;
        Info(String label, byte[] icon) { this.label = label; this.icon = icon; }
    }

    /** Reads the lines «package|name(base64)|icon(base64)» of the helper; a line with no usable name is left out. */
    public static java.util.Map<String, Info> parseInfo(String out) {
        java.util.Map<String, Info> r = new java.util.LinkedHashMap<>();
        if (out == null) return r;
        for (String line : out.split("\\r?\\n")) {
            String[] p = line.split("\\|", -1);
            if (p.length != 3 || !Guard.validPackage(p[0]) || p[1].isEmpty()) continue;
            try {
                String label = new String(java.util.Base64.getDecoder().decode(p[1]), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (label.isEmpty()) continue;
                byte[] icon = p[2].isEmpty() ? null : java.util.Base64.getDecoder().decode(p[2]);
                r.put(p[0], new Info(label, icon));
            } catch (IllegalArgumentException badBase64) { /* skipped */ }
        }
        return r;
    }

    /** The apps worth offering: not the system, and not ChouiTV (which has its own buttons). */
    public static List<String> choices(List<String> all) {
        List<String> r = new ArrayList<>();
        for (String p : all) {
            if (p.startsWith("com.android.") || p.startsWith("android.") || p.startsWith("ma.chouitv.")) continue;
            r.add(p);
        }
        return r;
    }

    public static String label(String pkg) {
        switch (pkg) {
            case "com.netflix.ninja": return "Netflix";
            case "com.google.android.youtube.tv": case "com.google.android.youtube.tvunplugged": return "YouTube";
            case "com.amazon.amazonvideo.livingroom": return "Prime Video";
            case "com.disney.disneyplus": return "Disney+";
            case "com.majnioui.tv.channels": return "Chaînes";
            case "com.google.android.apps.tv.launcherx": return "Google TV";
            case "com.spotify.tv.android": return "Spotify";
            default:
        }
        String last = pkg.substring(pkg.lastIndexOf('.') + 1);
        return last.isEmpty() ? pkg : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }
}
