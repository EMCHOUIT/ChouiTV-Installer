package ma.chouitv.installer.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        /** What the TV says about the apps its owner installed; null when it was not asked. */
        public final Facts facts;
        public Tv(String manufacturer, String brand, String model, String device, Set<String> installed, Set<String> disabled) {
            this(manufacturer, brand, model, device, installed, disabled, null);
        }
        public Tv(String manufacturer, String brand, String model, String device, Set<String> installed, Set<String> disabled, Facts facts) {
            this.facts = facts;
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

    /** An app the person installed: what makes it a candidate and whether it is recommended. */
    public static final class UserApp {
        public final String pkg;
        public final boolean autostart, recommended;
        public final int ramMb, daysSinceUse;   // -1 = unknown
        UserApp(String pkg, boolean autostart, int ramMb, int daysSinceUse, boolean recommended) {
            this.pkg = pkg; this.autostart = autostart; this.ramMb = ramMb; this.daysSinceUse = daysSinceUse; this.recommended = recommended;
        }
    }

    public static final class Plan {
        public final String listId, source, why;
        public final Map<Group, List<Item>> groups = new EnumMap<>(Group.class);
        public final Map<String, UserApp> userApps = new LinkedHashMap<>();
        public int alreadyOff, protectedCount;
        Plan(String listId, String source, String why) { this.listId = listId; this.source = source; this.why = why; }
        public int total() { int n = 0; for (List<Item> l : groups.values()) n += l.size(); return n; }
        /** The groups on by default: the tracking, the ads and the preinstalled apps; never the streaming or the person's own apps as a whole. */
        public static boolean defaultOn(Group g) { return g != Group.OPTIONAL && g != Group.USER; }

        /** The packages ticked when the plan opens: the three default groups, and the person's apps we recommend one by one. */
        public Set<String> defaultSelection() {
            Set<String> s = new LinkedHashSet<>();
            for (Group g : Group.values()) {
                for (Item i : groups.get(g)) {
                    if (g == Group.USER ? userApps.get(i.pkg).recommended : defaultOn(g)) s.add(i.pkg);
                }
            }
            return s;
        }

        /** Every package of the given groups, for a run by group. */
        public Set<String> packagesOf(Set<Group> chosen) {
            Set<String> s = new LinkedHashSet<>();
            for (Group g : Group.values()) if (chosen.contains(g)) for (Item i : groups.get(g)) s.add(i.pkg);
            return s;
        }
    }

    /** Apps that are still listed but never recommended: the TV is likely to need them as they are. */
    private static final Pattern NEVER_RECOMMEND = Pattern.compile("(vpn|shizuku|remap|keyboard|accessib|inputmethod|launcher|remote|tvhome)", Pattern.CASE_INSENSITIVE);

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
        Set<String> seen = new LinkedHashSet<>();
        if (list != null) fromList(d, tv, list, p, seen);
        if (tv.facts != null) userApps(d, tv, p, seen);
        return p;
    }

    private static void fromList(CleanupData d, Tv tv, CleanupData.Entry list, Plan p, Set<String> seen) {
        for (String pkg : list.packages) {
            if (!seen.add(pkg)) continue;
            if (d.keep.contains(pkg)) { p.protectedCount++; continue; }
            if (d.homes.contains(pkg) || NOT_FOR_US.matcher(pkg).find() || !Guard.validPackage(pkg)) continue;
            if (!tv.installed.contains(pkg)) continue;
            if (tv.disabled.contains(pkg)) { p.alreadyOff++; continue; }
            Group g = d.group.containsKey(pkg) ? d.group.get(pkg) : Group.PREINSTALLED;
            p.groups.get(g).add(new Item(pkg, g));
        }
    }

    /** The apps the person installed that are worth a look: never ours, Google's, protected, home screens, keyboards or accessibility. */
    private static void userApps(CleanupData d, Tv tv, Plan p, Set<String> seen) {
        Facts f = tv.facts;
        List<UserApp> found = new ArrayList<>();
        for (String pkg : f.user) {
            if (seen.contains(pkg) || tv.disabled.contains(pkg) || !Guard.validPackage(pkg)) continue;
            if (d.keep.contains(pkg) || d.homes.contains(pkg) || NOT_FOR_US.matcher(pkg).find() || f.sensitive.contains(pkg)) continue;
            if (pkg.startsWith("ma.chouitv.") || pkg.startsWith("com.google.") || pkg.startsWith("com.android.") || pkg.equals("android")) continue;
            boolean boot = f.autostart.contains(pkg);
            int ram = f.ramMb.containsKey(pkg) ? f.ramMb.get(pkg) : -1;
            int days = !f.usageKnown ? -1 : (f.daysSinceUse.containsKey(pkg) ? f.daysSinceUse.get(pkg) : Integer.MAX_VALUE);
            boolean rec = boot && days >= Facts.LONG_UNUSED_DAYS && !NEVER_RECOMMEND.matcher(pkg).find();
            found.add(new UserApp(pkg, boot, ram, days == Integer.MAX_VALUE ? 9999 : days, rec));
        }
        Collections.sort(found, new Comparator<UserApp>() {
            @Override public int compare(UserApp a, UserApp b) {
                if (a.recommended != b.recommended) return a.recommended ? -1 : 1;
                if (a.autostart != b.autostart) return a.autostart ? -1 : 1;
                if (a.ramMb != b.ramMb) return b.ramMb - a.ramMb;
                return a.pkg.compareTo(b.pkg);
            }
        });
        for (UserApp u : found) {
            p.userApps.put(u.pkg, u);
            p.groups.get(Group.USER).add(new Item(u.pkg, Group.USER));
        }
    }
}
