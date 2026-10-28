package ma.chouitv.installer.profiles;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Named settings profiles, one small file each in a folder of the app: nothing leaves the phone. A profile holds the export of a TV
 * as it is (the TV checks it whole before any import), wrapped with the name, the date, the TV and the ChouiTV version.
 */
public final class ProfileStore {
    public static final int NAME_MAX = 60;
    private final File dir;

    public ProfileStore(File dir) { this.dir = dir; }

    /** Why this text is not a ChouiTV settings export, or null when it is one. */
    public static String validate(String json) {
        if (json == null || json.trim().isEmpty()) return "empty";
        try {
            JSONObject o = new JSONObject(json);
            if (!"ChouiTV".equals(o.optString("app"))) return "not a ChouiTV settings file";
            if (o.optInt("schema", -1) != 1) return "unknown schema";
            if (o.optJSONObject("settings") == null) return "no settings in it";
            return null;
        } catch (Exception e) {
            return "not valid JSON";
        }
    }

    /** A name without control characters, trimmed and cut; an empty one becomes the fallback. */
    public static String cleanName(String name, String fallback) {
        String n = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        if (n.length() > NAME_MAX) n = n.substring(0, NAME_MAX).trim();
        return n.isEmpty() ? fallback : n;
    }

    /** The newest first. A file that cannot be read is skipped: one bad file never hides the others. */
    public List<Profile> list() {
        List<Profile> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (!f.getName().endsWith(".json")) continue;
            try {
                JSONObject o = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                String settings = o.getJSONObject("export").toString(2);
                if (validate(settings) != null) continue;
                out.add(new Profile(o.getString("id"), o.getString("name"), o.optString("date"), o.optString("tv"), o.optString("version"), settings));
            } catch (Exception skip) { }
        }
        Collections.sort(out, new Comparator<Profile>() {
            @Override public int compare(Profile a, Profile b) {
                int c = b.date.compareTo(a.date);
                return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
            }
        });
        return out;
    }

    /** Saves an export under a name; the same name twice becomes «name (2)». Throws when the export is not a ChouiTV one. */
    public Profile save(String name, String date, String tv, String exportJson) throws IOException {
        String bad = validate(exportJson);
        if (bad != null) throw new IOException(bad);
        String version = "";
        try { version = new JSONObject(exportJson).optString("version"); } catch (Exception ignored) { }
        String unique = uniqueName(cleanName(name, "ChouiTV"), null);
        Profile p = new Profile(UUID.randomUUID().toString().replace("-", "").substring(0, 12), unique, date == null ? "" : date, tv == null ? "" : tv, version, exportJson);
        write(p);
        return p;
    }

    public Profile rename(String id, String newName) throws IOException {
        Profile old = find(id);
        if (old == null) throw new IOException("no such profile");
        Profile p = new Profile(old.id, uniqueName(cleanName(newName, old.name), id), old.date, old.tv, old.version, old.settingsJson);
        write(p);
        return p;
    }

    public boolean delete(String id) { return new File(dir, safe(id) + ".json").delete(); }

    public Profile find(String id) {
        for (Profile p : list()) if (p.id.equals(id)) return p;
        return null;
    }

    private String uniqueName(String name, String exceptId) {
        List<Profile> all = list();
        String candidate = name;
        for (int n = 2; ; n++) {
            boolean taken = false;
            for (Profile p : all) if (!p.id.equals(exceptId) && p.name.toLowerCase(Locale.ROOT).equals(candidate.toLowerCase(Locale.ROOT))) taken = true;
            if (!taken) return candidate;
            candidate = name + " (" + n + ")";
        }
    }

    private static String safe(String id) { return id.replaceAll("[^A-Za-z0-9]", ""); }

    private void write(Profile p) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        try {
            JSONObject o = new JSONObject();
            o.put("id", p.id).put("name", p.name).put("date", p.date).put("tv", p.tv).put("version", p.version).put("export", new JSONObject(p.settingsJson));
            File tmp = new File(dir, safe(p.id) + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(o.toString(2).getBytes(StandardCharsets.UTF_8)); }
            File dest = new File(dir, safe(p.id) + ".json");
            if (!tmp.renameTo(dest)) { dest.delete(); if (!tmp.renameTo(dest)) throw new IOException("cannot write " + dest); }
        } catch (org.json.JSONException e) {
            throw new IOException(e.getMessage());
        }
    }
}
