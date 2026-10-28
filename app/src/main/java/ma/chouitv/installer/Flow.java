package ma.chouitv.installer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ma.chouitv.installer.cleanup.Facts;
import ma.chouitv.installer.cleanup.Planner;
import ma.chouitv.installer.cleanup.Shell;

/** What the installer does to the TV, step by step. Every method blocks: call them from a worker thread. */
final class Flow {
    static final String SAVER = "ma.chouitv.screensaver", HOME = "ma.chouitv.home", CHANNELS = "com.majnioui.tv.channels";
    static final String VERIFIER = "verifier_verify_adb_installs";

    private final Adb tv;
    private final File cache;
    /** true when Play Protect refused an install over ADB at least once (for the TV report) */
    boolean playProtect;

    Flow(Adb tv, File cache) { this.tv = tv; this.cache = cache; }

    // ---------- what the cleanup needs ----------

    /** The TV's shell, as the cleanup's own small interface (it is tested on a fake TV). */
    Shell shell() { return tv::shell; }

    private Set<String> packages(String cmd) throws IOException {
        Set<String> s = new HashSet<>();
        for (String line : tv.shell(cmd).split("\n")) {
            line = line.trim();
            if (line.startsWith("package:")) s.add(line.substring(8));
        }
        return s;
    }

    /** What the TV says about itself and which apps it has, on and off, and about the apps its owner installed. */
    Planner.Tv tvInfo() throws IOException {
        return new Planner.Tv(prop("ro.product.manufacturer"), prop("ro.product.brand"), prop("ro.product.model"), prop("ro.product.device"),
            packages("pm list packages"), packages("pm list packages -d"), facts());
    }

    /** Each fact is optional: a TV that does not answer one only means fewer recommendations, never a failed plan. */
    private Facts facts() {
        Facts f = new Facts();
        try { f.user.addAll(Facts.packages(tv.shell("pm list packages -3"))); } catch (IOException ignored) { }
        try { f.autostart.addAll(Facts.componentPackages(tv.shell("cmd package query-receivers --brief -a android.intent.action.BOOT_COMPLETED"))); } catch (IOException ignored) { }
        try { f.ramMb.putAll(Facts.memory(tv.shell("dumpsys meminfo"))); } catch (IOException ignored) { }
        try {
            long today = Long.parseLong(tv.shell("date +%s").trim()) / 86400;
            f.usage(tv.shell("dumpsys usagestats | grep 'package=.*lastTimeUsed='"), today);
        } catch (IOException | NumberFormatException ignored) { }
        try { f.sensitive.addAll(Facts.componentList(tv.shell("ime list -s"))); } catch (IOException ignored) { }
        try { f.sensitive.addAll(Facts.componentList(tv.shell("settings get secure enabled_accessibility_services"))); } catch (IOException ignored) { }
        try { f.sensitive.addAll(Facts.componentPackages(tv.shell("cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null"))); } catch (IOException ignored) { }
        return f;
    }

    // ---------- the steps of the installation ----------

    String model() throws IOException { return tv.shell("getprop ro.product.model").trim(); }

    String androidVersion() throws IOException { return tv.shell("getprop ro.build.version.release").trim(); }

    String prop(String name) throws IOException { return tv.shell("getprop " + name).trim(); }

    File download(String pkg, String url, String sha, Adb.Progress p) throws Exception {
        return Net.download(url, new File(cache, pkg + ".apk"), sha, p);
    }

    /** Sends a downloaded APK and installs it; the same recovery as optimize/install.sh (Play Protect, another signing key). */
    void installFile(String pkg, File f, Adb.Progress up) throws Exception {
        try {
            String out = tv.install(f, up);
            if (out.contains("INSTALL_FAILED_VERIFICATION_FAILURE")) {
                playProtect = true;
                // Play Protect refused an install over ADB: its check is off for this install only and put back after
                String was = tv.shell("settings get global " + VERIFIER).trim();
                tv.shell("settings put global " + VERIFIER + " 0");
                try { out = tv.install(f, up); }
                finally {
                    if (was.isEmpty() || was.equals("null")) tv.shell("settings delete global " + VERIFIER);
                    else tv.shell("settings put global " + VERIFIER + " " + was);
                }
            }
            if (out.contains("UPDATE_INCOMPATIBLE") || out.contains("signatures do not match")) {
                // an older build signed with another key: only then is the installed app removed
                tv.shell("pm uninstall " + pkg);
                out = tv.install(f, up);
            }
            if (!out.contains("Success")) throw new IOException(pkg + ": " + out);
            Thread.sleep(3000);
            if (!tv.shell("pm list packages " + pkg).contains("package:" + pkg)) {
                throw new IOException(pkg + " was removed by this TV right after the install");
            }
        } finally { f.delete(); }
    }

    /** The permissions the home app needs. */
    void grantPermissions() throws IOException {
        tv.shell("pm grant " + HOME + " android.permission.ACCESS_FINE_LOCATION");
        tv.shell("appops set " + HOME + " SCHEDULE_EXACT_ALARM allow");
    }

    /** The screensaver, then the home role. */
    void setDefaults() throws IOException {
        tv.shell("settings put secure screensaver_enabled 1");
        tv.shell("settings put secure screensaver_components " + SAVER + "/.ScreenDream");
        tv.shell("settings put secure screensaver_default_component " + SAVER + "/.ScreenDream");
        tv.shell("cmd role add-role-holder --user 0 android.app.role.HOME " + HOME);
        tv.shell("cmd package set-home-activity --user 0 " + HOME + "/.MainActivity");
    }


    // ---------- making ChouiTV the home screen, with a safety net (optimize/install.sh does the same) ----------

    enum Home { OURS, KEPT_STOCK, FAILED, UNSURE }

    private static final String WRAITH = "com.google.android.tungsten.setupwraith";
    /** Runs ON the TV: if /data/local/tmp/home_ok does not appear in time, it puts the stock home back (optimize/deadman.sh). */
    private static final String DEADMAN = "#!/system/bin/sh\n"
        + "STOCK=\"${1:-com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity}\"\n"
        + "LIMIT=\"${2:-180}\"\n"
        + "i=0\n"
        + "while [ $i -lt \"$LIMIT\" ]; do\n"
        + "  [ -f /data/local/tmp/home_ok ] && exit 0\n"
        + "  sleep 1\n"
        + "  i=$((i + 1))\n"
        + "done\n"
        + "pm enable \"${STOCK%%/*}\"\n"
        + "pm enable " + WRAITH + "\n"
        + "cmd role add-role-holder --user 0 android.app.role.HOME \"${STOCK%%/*}\"\n"
        + "cmd package set-home-activity --user 0 \"$STOCK\"\n"
        + "am start -n \"$STOCK\"\n"
        + "input keyevent KEYCODE_HOME\n";

    /**
     * Turns the TV's own home off so the Home button opens ChouiTV. Before anything is changed a safety switch is armed on the TV: it
     * puts the stock home back in 5 minutes unless ChouiTV is confirmed on screen. A TV that refuses to turn its home off
     * (the Nokia Streaming Box 8000 answers «This command is illegal») keeps it, and the person is told.
     */
    Home switchHome() throws Exception {
        boolean gx = tv.shell("pm list packages com.google.android.apps.tv.launcherx").contains("launcherx");
        String stockPkg = gx ? "com.google.android.apps.tv.launcherx" : "com.google.android.tvlauncher";
        String stock = gx ? stockPkg + "/" + stockPkg + ".home.HomeActivity" : stockPkg + "/.MainActivity";
        String homeQuery = "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2> /dev/null; "
            + "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME";
        // the setup wizard goes too, but only when it answers Home and cannot take the saved Wi-Fi network with it
        boolean wraithOff = false;
        if (tv.shell(homeQuery).contains(WRAITH)) {
            String w = tv.shell("dumpsys wifi");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("cname=\\S+").matcher(w);
            StringBuilder owners = new StringBuilder();
            while (m.find()) owners.append(m.group());
            if (owners.length() == 0 || owners.toString().contains("setupwraith")) return Home.KEPT_STOCK;   // owned or unknown: left on
            wraithOff = true;
        }
        // the safety switch, armed before anything changes
        File f = new File(cache, "deadman.sh");
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) { o.write(DEADMAN.getBytes("UTF-8")); }
        try { tv.pushShell(f, "/data/local/tmp/deadman.sh", null); } finally { f.delete(); }
        tv.shell("rm -f /data/local/tmp/home_ok");
        tv.shell("nohup sh /data/local/tmp/deadman.sh " + stock + " 300 > /dev/null 2>&1 &");
        // the switch: the HOME role, the setup wizard, then the stock home (this order avoided the black screen)
        tv.shell("cmd role add-role-holder --user 0 android.app.role.HOME " + HOME);
        tv.shell("cmd package set-home-activity --user 0 " + HOME + "/.MainActivity");
        if (wraithOff) tv.shell("pm disable-user --user 0 " + WRAITH);
        String off = tv.shell("pm disable-user --user 0 " + stockPkg);
        if (off.toLowerCase(java.util.Locale.ROOT).contains("illegal")) {
            // this TV keeps its own home: put everything back and stop the switch
            tv.shell("cmd role add-role-holder --user 0 android.app.role.HOME " + stockPkg);
            tv.shell("cmd package set-home-activity --user 0 " + stock);
            tv.shell("pm enable " + WRAITH);
            tv.shell("touch /data/local/tmp/home_ok");
            tv.shell("input keyevent KEYCODE_HOME");
            return Home.KEPT_STOCK;
        }
        tv.shell("cmd package set-home-activity --user 0 " + HOME + "/.MainActivity");
        tv.shell("am force-stop com.android.tv.settings");
        tv.shell("input keyevent KEYCODE_HOME");
        // is ChouiTV really on screen? a few tries: a slow TV
        boolean ours = false;
        for (int i = 0; i < 3 && !ours; i++) {
            Thread.sleep(6000);
            if (tv.shell("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'").contains(HOME)) ours = true;
            else tv.shell("input keyevent KEYCODE_HOME");
        }
        if (!ours) return Home.FAILED;   // the safety switch puts the stock home back by itself
        tv.shell("touch /data/local/tmp/home_ok");
        String res = tv.shell("cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN");
        return res.contains(HOME) ? Home.OURS : Home.FAILED;
    }

    /** Everything is there: the three apps are installed. Returns true when the home key opens our home. */
    boolean verify() throws IOException {
        for (String p : new String[] {SAVER, CHANNELS, HOME}) {
            if (!tv.shell("pm list packages " + p).contains("package:" + p)) throw new IOException(p + " is not installed");
        }
        return tv.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME").contains(HOME);
    }

    // ---------- the screensaver's own receiver (the same one optimize/saver.sh talks to) ----------

    /** One broadcast to the screensaver's receiver; the receiver's answer, or an IOException on «error:». */
    String saver(String action, String extras) throws IOException {
        String out = tv.shell("am broadcast -n " + SAVER + "/.SaverReceiver -a " + SAVER + "." + action + (extras.isEmpty() ? "" : " " + extras));
        int i = out.indexOf("data=\"");
        String data = i < 0 ? "" : out.substring(i + 6);
        if (data.endsWith("\"")) data = data.substring(0, data.length() - 1);
        if (data.startsWith("error:")) throw new IOException(data);
        return data.trim();
    }

    static String q(String v) {
        if (v.indexOf('\'') >= 0) throw new IllegalArgumentException("no apostrophes");
        return "'" + v + "'";
    }

    /** «region: city، city…» lines the receiver prints: region → cities. */
    List<String[]> cities() throws IOException {
        List<String[]> r = new ArrayList<>();
        for (String line : saver("CITIES", "").split("\n")) {
            int c = line.indexOf(": ");
            if (c < 0) continue;
            r.add(new String[] {line.substring(0, c), line.substring(c + 2)});
        }
        return r;
    }

    void setCity(String city) throws IOException { saver("SET", "--es city " + q(city)); }

    void setTz(String tz) throws IOException { saver("SET", "--es tzchoice " + q(tz)); }

    void setMethod(String m) throws IOException { saver("SET", "--es pmchoice " + q(m)); }

    void rescheduleAdhan() throws IOException {
        tv.shell("am broadcast -n " + HOME + "/.tilawah.MusicReceiver -a " + HOME + ".ADHAN_RESCHEDULE");
    }

    void adhan(boolean on) throws IOException {
        tv.shell("am broadcast -n " + HOME + "/.tilawah.MusicReceiver -a " + HOME + ".ADHAN_" + (on ? "ON" : "OFF"));
    }

    /** The settings of this TV as one JSON export (the screensaver app answers the EXPORT broadcast). */
    String exportSettings() throws IOException {
        String out = saver("EXPORT", "");
        int open = out.indexOf('{'), close = out.lastIndexOf('}');   // the broadcast adds its own quote and line ends around the JSON
        if (open < 0 || close < open) throw new IOException("the TV sent no settings");
        return out.substring(open, close + 1);
    }

    /** A settings file made by «export»: what it would change (apply=false), or applies it (apply=true). A big one travels as a file. */
    String importSettings(String b64, boolean apply) throws IOException {
        String tail = apply ? " --es apply yes" : "";
        if (b64.length() <= 3000) return saver("IMPORT", "--es json_b64 " + b64 + tail);
        File f = new File(cache, "profile.b64");
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) { o.write(b64.getBytes("US-ASCII")); }
        try { tv.pushShell(f, "/data/local/tmp/chouitv-profile.b64", null); } finally { f.delete(); }
        try { return saver("IMPORT", "--es json_b64 \"$(cat /data/local/tmp/chouitv-profile.b64)\"" + tail); }
        finally { tv.shell("rm -f /data/local/tmp/chouitv-profile.b64"); }
    }

    /** Puts the settings from before the last import back. */
    String undoImport() throws IOException {
        return saver("IMPORT", "--ez undo true --es apply yes");
    }

    void openOnTv() throws IOException {
        tv.shell("input keyevent KEYCODE_HOME");
    }
}
