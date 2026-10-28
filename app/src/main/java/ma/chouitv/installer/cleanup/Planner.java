package ma.chouitv.installer.cleanup;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ma.chouitv.installer.cleanup.CleanupData.Group;

/**
 * Turns the lists and what the TV says about itself into a plan. Pure: no Android, no network, no side effect, the same input gives
 * the same plan. Protected packages, launchers and setup wizards never reach a plan; a package already off is left out, so running
 * the same cleanup again yields nothing to do.
 */
public final class Planner {
    private Planner() {}

    /** What the TV says about itself. */
    public static final class Tv {
        public final String manufacturer, brand, model, device;
        public final Set<String> installed, disabled;
        public Tv(String manufacturer, String brand, String model, String device, Set<String> installed, Set<String> disabled) {
            this.manufacturer = manufacturer == null ? "" : manufacturer;
            this.brand = brand == null ? "" : brand;
            this.model = model == null ? "" : model;
            this.device = device == null ? "" : device;
            this.installed = installed;
            this.disabled = disabled;
        }
    }

    public static final class Item {
        public final String pkg; public final Group group;
        Item(String pkg, Group group) { this.pkg = pkg; this.group = group; }
    }

    public static final class Plan {
        public final String listId, source, why;
        public final Map<Group, List<Item>> groups = new EnumMap<>(Group.class);
        public int alreadyOff, protectedCount;
        Plan(String listId, String source, String why) { this.listId = listId; this.source = source; this.why = why; }
        public int total() { int n = 0; for (List<Item> l : groups.values()) n += l.size(); return n; }
        /** The groups on by default: everything but the optional ones. */
        public static boolean defaultOn(Group g) { return g != Group.OPTIONAL; }
    }

    private static final Pattern NOT_FOR_US = Pattern.compile("(launcher|tvhome|setupwraith|setupwizard|provision|\\.home$|\\.setup$)", Pattern.CASE_INSENSITIVE);

    static String norm(String s) { return s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", ""); }

    /** The list for this TV and why, in this order: ours for a TCL, the one community list that fits the model or device, else the consensus. */
    static CleanupData.Entry choose(CleanupData d, Tv tv, String[] why) {
        Set<String> makers = new LinkedHashSet<>();
        if (!norm(tv.manufacturer).isEmpty()) makers.add(norm(tv.manufacturer));
        if (!norm(tv.brand).isEmpty()) makers.add(norm(tv.brand));
        if (makers.contains("TCL") && d.list("tcl") != null) { why[0] = "tcl"; return d.list("tcl"); }
        String model = norm(tv.model), device = norm(tv.device);
        List<CleanupData.Entry> byModel = new ArrayList<>(), byDevice = new ArrayList<>();
        for (CleanupData.Entry e : d.lists) {
            if (!"community".equals(e.source)) continue;
            boolean brand = false;
            for (String m : makers) if (e.brands.contains(m)) brand = true;
            if (!brand) continue;
            if (model.length() >= 5 && !d.genericModels.contains(model) && e.models.contains(model)) byModel.add(e);
            if (device.length() >= 3 && !d.genericDevices.contains(device) && e.devices.contains(device)) byDevice.add(e);
        }
        if (byModel.size() == 1) { why[0] = "model"; return byModel.get(0); }
        if (byModel.size() > 1) { why[0] = "model-ambiguous"; return d.list("consensus"); }
        if (byDevice.size() == 1) { why[0] = "device"; return byDevice.get(0); }
        if (byDevice.size() > 1) { why[0] = "device-ambiguous"; return d.list("consensus"); }
        why[0] = "unknown"; return d.list("consensus");
    }

    public static Plan plan(CleanupData d, Tv tv) {
        String[] why = new String[1];
        CleanupData.Entry list = choose(d, tv, why);
        Plan p = new Plan(list == null ? "" : list.id, list == null ? "none" : list.source, why[0]);
        for (Group g : Group.values()) p.groups.put(g, new ArrayList<Item>());
        if (list == null) return p;
        Set<String> seen = new LinkedHashSet<>();
        for (String pkg : list.packages) {
            if (!seen.add(pkg)) continue;
            if (d.keep.contains(pkg)) { p.protectedCount++; continue; }
            if (d.homes.contains(pkg) || NOT_FOR_US.matcher(pkg).find() || !Guard.validPackage(pkg)) continue;
            if (!tv.installed.contains(pkg)) continue;
            if (tv.disabled.contains(pkg)) { p.alreadyOff++; continue; }
            Group g = d.group.containsKey(pkg) ? d.group.get(pkg) : Group.PREINSTALLED;
            p.groups.get(g).add(new Item(pkg, g));
        }
        return p;
    }
}
