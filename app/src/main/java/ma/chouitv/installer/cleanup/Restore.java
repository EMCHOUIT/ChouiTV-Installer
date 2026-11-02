package ma.chouitv.installer.cleanup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The restore point: what a cleanup turned off, kept on the phone and on the TV, and its exact inverse. */
public final class Restore {
    public final String tvModel, date;
    public final List<String> packages = new ArrayList<>();
    /** Of those, the ones that had a service running when the cleanup began: if a TV feature (the picture menu) fails after a restart, they are the first suspects. */
    public final List<String> servicesBefore = new ArrayList<>();

    public Restore(String tvModel, String date, List<String> packages) {
        this.tvModel = tvModel == null ? "" : tvModel;
        this.date = date == null ? "" : date;
        for (String p : packages) if (Guard.validPackage(p) && !this.packages.contains(p)) this.packages.add(p);
    }

    public String toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("schema", 1).put("tv", tvModel).put("date", date).put("packages", new JSONArray(packages)).put("services", new JSONArray(servicesBefore));
        return o.toString();
    }

    /** Remembers which of the turned-off packages had a service running (read from dumpsys before the first change). */
    public Restore withServices(java.util.Set<String> activeServicePackages) {
        for (String p : packages) if (activeServicePackages.contains(p) && !servicesBefore.contains(p)) servicesBefore.add(p);
        return this;
    }

    public static Restore fromJson(String s) throws Exception {
        JSONObject o = new JSONObject(s);
        List<String> p = new ArrayList<>();
        JSONArray a = o.getJSONArray("packages");
        for (int i = 0; i < a.length(); i++) p.add(a.getString(i));
        Restore r = new Restore(o.optString("tv"), o.optString("date"), p);
        JSONArray sv = o.optJSONArray("services");   // absent in the restore points of 1.6 and before
        if (sv != null) for (int i = 0; i < sv.length(); i++) if (r.packages.contains(sv.getString(i))) r.servicesBefore.add(sv.getString(i));
        return r;
    }

    /** The commands that undo it: one «pm enable» per package. */
    public List<String> commands() {
        List<String> c = new ArrayList<>();
        for (String p : packages) c.add(Guard.enable(p));
        return c;
    }

    /** A plain text copy any «adb shell» can run, kept on the TV. */
    public String plainText() {
        StringBuilder b = new StringBuilder("# ChouiTV cleanup restore point, ").append(date).append("\n# Run each line with: adb shell <line>\n");
        for (String c : commands()) b.append(c).append("\n");
        return b.toString();
    }

    /** Puts every package back (or just one) and returns the ones that are on again. */
    public static List<String> apply(Shell tv, List<String> pkgs) throws IOException {
        List<String> back = new ArrayList<>();
        for (String p : pkgs) {
            tv.run(Guard.enable(p));
            back.add(p);
        }
        return back;
    }
}
