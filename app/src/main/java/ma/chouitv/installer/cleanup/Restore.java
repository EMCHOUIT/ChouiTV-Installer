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

    public Restore(String tvModel, String date, List<String> packages) {
        this.tvModel = tvModel == null ? "" : tvModel;
        this.date = date == null ? "" : date;
        for (String p : packages) if (Guard.validPackage(p) && !this.packages.contains(p)) this.packages.add(p);
    }

    public String toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("schema", 1).put("tv", tvModel).put("date", date).put("packages", new JSONArray(packages));
        return o.toString();
    }

    public static Restore fromJson(String s) throws Exception {
        JSONObject o = new JSONObject(s);
        List<String> p = new ArrayList<>();
        JSONArray a = o.getJSONArray("packages");
        for (int i = 0; i < a.length(); i++) p.add(a.getString(i));
        return new Restore(o.optString("tv"), o.optString("date"), p);
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
