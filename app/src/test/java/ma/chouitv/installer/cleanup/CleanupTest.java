package ma.chouitv.installer.cleanup;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ma.chouitv.installer.cleanup.CleanupData.Group;
import ma.chouitv.installer.cleanup.Planner.Item;
import ma.chouitv.installer.cleanup.Planner.Plan;

public class CleanupTest {
    // ---------- a small world of our own ----------
    private static final String SMALL = "{\"schema\":1,\"consensusMin\":20,"
        + "\"keep\":[\"com.android.vending\",\"com.google.android.katniss\"],"
        + "\"homes\":[\"com.google.android.leanbacklauncher.recommendations\"],"
        + "\"genericModels\":[\"SMARTTV\"],\"genericDevices\":[\"TV\"],"
        + "\"packages\":[\"com.tcl.bi\",\"com.google.android.feedback\",\"com.tcl.messagebox\",\"com.tcl.gamebar\",\"com.xm.webcontent\",\"com.mitv.tvhome.atv\",\"com.google.android.katniss\",\"com.google.android.tvrecommendations\"],"
        + "\"groups\":[\"tracking\",\"tracking\",\"ads\",\"optional\",\"preinstalled\",\"preinstalled\",\"preinstalled\",\"ads\"],"
        + "\"names\":{\"ar\":{\"com.tcl.bi\":\"x\"}},"
        + "\"lists\":["
        + "{\"id\":\"tcl\",\"source\":\"ours\",\"brands\":[\"TCL\"],\"models\":[],\"devices\":[],\"p\":[0,1,2,3]},"
        + "{\"id\":\"mi-a\",\"source\":\"community\",\"brands\":[\"XIAOMI\"],\"models\":[\"MITVMSSP3\"],\"devices\":[],\"p\":[4,6,1]},"
        + "{\"id\":\"mi-b\",\"source\":\"community\",\"brands\":[\"XIAOMI\"],\"models\":[\"MITVMSSP3\"],\"devices\":[],\"p\":[4,1]},"
        + "{\"id\":\"mi-c\",\"source\":\"community\",\"brands\":[\"XIAOMI\"],\"models\":[\"MIBOX3\"],\"devices\":[\"CAPRI\"],\"p\":[4,5,6]},"
        + "{\"id\":\"consensus\",\"source\":\"consensus\",\"brands\":[],\"models\":[],\"devices\":[],\"p\":[1,7]}"
        + "]}";

    private static CleanupData small() throws Exception { return CleanupData.parse(SMALL); }

    private static Planner.Tv tv(String maker, String model, String device, String... installed) {
        return new Planner.Tv(maker, maker, model, device, new HashSet<>(Arrays.asList(installed)), new HashSet<String>());
    }

    private static List<String> pkgs(Plan p) {
        List<String> l = new ArrayList<>();
        for (List<Item> g : p.groups.values()) for (Item i : g) l.add(i.pkg);
        return l;
    }

    // ---------- the guard ----------
    @Test public void guardAcceptsRealPackageNames() {
        assertTrue(Guard.validPackage("com.tcl.bi"));
        assertTrue(Guard.validPackage("android.autoinstalls.config.google.gtvpai"));
        assertTrue(Guard.validPackage("com.tcl.xian.StartandroidService"));
        assertEquals("pm disable-user --user 0 com.tcl.bi", Guard.disable("com.tcl.bi"));
        assertEquals("pm enable com.tcl.bi", Guard.enable("com.tcl.bi"));
    }

    @Test public void guardRefusesPackageNamesThatCouldInjectACommand() {
        String[] bad = {"com.tcl.bi; reboot", "com.tcl.bi && rm -rf /sdcard", "$(reboot)", "`reboot`", "com.tcl.bi|sh", "a b.c", "", "single", "com..x", "com.tcl.bi\nreboot", ".com.x", "1com.x", "com.x'", null};
        for (String b : bad) {
            assertFalse("should refuse: " + b, Guard.validPackage(b));
            try { Guard.disable(b); fail("disable accepted " + b); } catch (IllegalArgumentException expected) { }
            try { Guard.enable(b); fail("enable accepted " + b); } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void guardRefusesAPackageChangeSharedWithAnotherCommand() {
        assertNull(Guard.refusal("pm disable-user --user 0 com.tcl.bi"));
        assertNull(Guard.refusal("pm list packages -d"));
        assertNull(Guard.refusal("settings put secure screensaver_enabled 1"));
        String[] chained = {"pm disable-user --user 0 a.b; reboot", "pm enable a.b && reboot", "pm uninstall a.b | cat", "echo x; pm clear a.b", "pm disable-user --user 0 a.b $(id)",
            "pm disable-user --user 0 a.b `id`", "pm enable a.b\nreboot", "cmd package set-home-activity --user 0 a.b/.M; reboot", "pm grant a.b android.permission.X & id"};
        for (String c : chained) assertNotNull("should refuse: " + c, Guard.refusal(c));
    }

    // ---------- choosing the list ----------
    @Test public void aTclGetsOurList() throws Exception {
        Plan p = Planner.plan(small(), tv("TCL", "43S5400A", "C06", "com.tcl.bi", "com.google.android.feedback"));
        assertEquals("tcl", p.listId); assertEquals("ours", p.source);
        assertEquals(new HashSet<>(Arrays.asList("com.tcl.bi", "com.google.android.feedback")), new HashSet<>(pkgs(p)));
    }

    @Test public void oneCommunityListThatFitsTheModelIsUsed() throws Exception {
        Plan p = Planner.plan(small(), tv("Xiaomi", "MIBOX3", "", "com.xm.webcontent", "com.mitv.tvhome.atv"));
        assertEquals("mi-c", p.listId); assertEquals("model", p.why);
        assertTrue(pkgs(p).contains("com.xm.webcontent"));
    }

    @Test public void twoListsForTheSameModelMeanNoneIsUsed() throws Exception {
        Plan p = Planner.plan(small(), tv("Xiaomi", "MiTV-MSSP3", "", "com.xm.webcontent", "com.google.android.feedback", "com.google.android.tvrecommendations"));
        assertEquals("consensus", p.listId);
        assertEquals("model-ambiguous", p.why);
        assertFalse("the ambiguous lists must not leak in", pkgs(p).contains("com.xm.webcontent"));
    }

    @Test public void anUnknownModelGetsOnlyTheConsensus() throws Exception {
        Plan p = Planner.plan(small(), tv("Acme", "Zork5000", "zz", "com.xm.webcontent", "com.google.android.feedback", "com.google.android.tvrecommendations"));
        assertEquals("consensus", p.listId); assertEquals("unknown", p.why);
        assertEquals(new HashSet<>(Arrays.asList("com.google.android.feedback", "com.google.android.tvrecommendations")), new HashSet<>(pkgs(p)));
    }

    @Test public void aGenericOrShortModelNeverCountsAsAMatch() throws Exception {
        assertEquals("unknown", Planner.plan(small(), tv("Xiaomi", "Smart TV", "TV", "com.xm.webcontent")).why);
        assertEquals("unknown", Planner.plan(small(), tv("Xiaomi", "MI", "", "com.xm.webcontent")).why);
    }

    // ---------- what never reaches a plan ----------
    @Test public void aProtectedPackageIsNeverPlannedEvenWhenAListHasIt() throws Exception {
        Plan p = Planner.plan(small(), tv("Xiaomi", "MIBOX3", "", "com.xm.webcontent", "com.google.android.katniss"));
        assertFalse(pkgs(p).contains("com.google.android.katniss"));
        assertEquals(1, p.protectedCount);
    }

    @Test public void launchersAndSetupWizardsAreNeverPlanned() throws Exception {
        Plan p = Planner.plan(small(), tv("Xiaomi", "MIBOX3", "", "com.xm.webcontent", "com.mitv.tvhome.atv"));
        assertFalse(pkgs(p).contains("com.mitv.tvhome.atv"));
    }

    @Test public void aPackageNotOnTheTvIsLeftOut() throws Exception {
        Plan p = Planner.plan(small(), tv("TCL", "43S5400A", "C06", "com.tcl.bi"));
        assertEquals(Arrays.asList("com.tcl.bi"), pkgs(p));
    }

    @Test public void aPackageAlreadyOffMakesTheSecondRunEmpty() throws Exception {
        Planner.Tv t = new Planner.Tv("TCL", "TCL", "43S5400A", "C06", new HashSet<>(Arrays.asList("com.tcl.bi", "com.google.android.feedback")),
            new HashSet<>(Arrays.asList("com.tcl.bi", "com.google.android.feedback")));
        Plan p = Planner.plan(small(), t);
        assertEquals(0, p.total()); assertEquals(2, p.alreadyOff);
    }

    @Test public void packagesLandInTheirGroup() throws Exception {
        Plan p = Planner.plan(small(), tv("TCL", "43S5400A", "C06", "com.tcl.bi", "com.tcl.messagebox", "com.tcl.gamebar"));
        assertEquals(1, p.groups.get(Group.TRACKING).size());
        assertEquals(1, p.groups.get(Group.ADS).size());
        assertEquals(1, p.groups.get(Group.OPTIONAL).size());
        assertFalse(Plan.defaultOn(Group.OPTIONAL)); assertTrue(Plan.defaultOn(Group.TRACKING));
    }

    @Test public void theSameInputGivesTheSamePlan() throws Exception {
        Planner.Tv t = tv("TCL", "43S5400A", "C06", "com.tcl.bi", "com.google.android.feedback", "com.tcl.messagebox");
        assertEquals(pkgs(Planner.plan(small(), t)), pkgs(Planner.plan(small(), t)));
    }

    // ---------- the real lists, many random TVs ----------
    @Test public void onTheRealListsNoProtectedPackageEverReachesAPlan() throws Exception {
        CleanupData d;
        try (InputStream in = getClass().getResourceAsStream("/cleanup.json")) {
            byte[] b = new byte[in.available()]; int n = 0, r;
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while ((r = in.read(buf)) > 0) o.write(buf, 0, r);
            d = CleanupData.parse(new String(o.toByteArray(), "UTF-8"));
        }
        assertTrue("the real data is there", d.lists.size() > 100 && d.keep.size() >= 30);
        Random rnd = new Random(0x5EED);
        List<String> brands = Arrays.asList("TCL", "Xiaomi", "Sony", "Hisense", "Philips", "Acme", "Amazon");
        for (int i = 0; i < 400; i++) {
            CleanupData.Entry e = d.lists.get(rnd.nextInt(d.lists.size()));
            Set<String> installed = new HashSet<>(e.packages);
            for (int k = 0; k < 6; k++) installed.add(new ArrayList<>(d.keep).get(rnd.nextInt(d.keep.size())));
            Set<String> disabled = new HashSet<>();
            for (String p : e.packages) if (rnd.nextInt(5) == 0) disabled.add(p);
            String model = e.models.isEmpty() || rnd.nextBoolean() ? "X" + rnd.nextInt(99999) : e.models.iterator().next();
            String brand = e.brands.isEmpty() ? brands.get(rnd.nextInt(brands.size())) : e.brands.iterator().next();
            Plan p = Planner.plan(d, new Planner.Tv(brand, brand, model, "", installed, disabled));
            for (Item it : p.groups.values().stream().flatMap(List::stream).collect(java.util.stream.Collectors.toList())) {
                assertFalse("protected in a plan: " + it.pkg, d.keep.contains(it.pkg));
                assertFalse("home helper in a plan: " + it.pkg, d.homes.contains(it.pkg));
                assertFalse("already off in a plan: " + it.pkg, disabled.contains(it.pkg));
                assertTrue("not installed in a plan: " + it.pkg, installed.contains(it.pkg));
                assertTrue(Guard.validPackage(it.pkg));
                assertNull(Guard.refusal(Guard.disable(it.pkg)));
            }
        }
    }

    // ---------- running a plan on a fake TV ----------
    private static final Executor.Sleeper NO_SLEEP = ms -> { };
    private static final Executor.Listener QUIET = new Executor.Listener() {
        public void started(int total) { }
        public void package_(Group g, String pkg, boolean ok, int done, int total) { }
        public void groupDone(Group g, boolean healthy) { }
    };
    private static final String TOKEN = "0123456789abcdef";

    private static Plan planOn(FakeTv t, String... installed) throws Exception {
        return Planner.plan(small(), tv("TCL", "43S5400A", "C06", installed));
    }

    @Test public void aCleanRunTurnsEverythingOffChecksItAndStandsTheSafetyNetDown() throws Exception {
        FakeTv t = new FakeTv();
        Plan p = planOn(t, "com.tcl.bi", "com.google.android.feedback", "com.tcl.messagebox");
        Executor.Result r = new Executor(t, NO_SLEEP, TOKEN).run(p, EnumSet.of(Group.TRACKING, Group.ADS, Group.PREINSTALLED), QUIET);
        assertEquals(3, r.disabled.size()); assertTrue(r.refused.isEmpty()); assertNull(r.rolledBack);
        assertTrue(t.disabled.containsAll(r.disabled));
        assertTrue("the safety net was armed", t.log.stream().anyMatch(c -> c.contains("chouitv-cleanup-deadman-" + TOKEN)));
        assertTrue("and confirmed", t.files.contains(Deadman.okPath(TOKEN)));
    }

    @Test public void theSafetyNetIsArmedBeforeTheFirstChange() throws Exception {
        FakeTv t = new FakeTv();
        new Executor(t, NO_SLEEP, TOKEN).run(planOn(t, "com.tcl.bi"), EnumSet.of(Group.TRACKING), QUIET);
        int armed = -1, firstDisable = -1;
        for (int i = 0; i < t.log.size(); i++) {
            if (armed < 0 && t.log.get(i).startsWith("nohup sh ")) armed = i;
            if (firstDisable < 0 && t.log.get(i).startsWith("pm disable-user")) firstDisable = i;
        }
        assertTrue(armed >= 0 && firstDisable > armed);
    }

    @Test public void aTvThatRefusesAPackageIsNotRecordedAsDisabledAndTheRunGoesOn() throws Exception {
        FakeTv t = new FakeTv(); t.refuse.add("com.tcl.bi");
        Executor.Result r = new Executor(t, NO_SLEEP, TOKEN).run(planOn(t, "com.tcl.bi", "com.google.android.feedback"), EnumSet.of(Group.TRACKING), QUIET);
        assertEquals(Arrays.asList("com.google.android.feedback"), r.disabled);
        assertEquals(Arrays.asList("com.tcl.bi"), r.refused);
        assertNull(r.rolledBack);
    }

    @Test public void aGroupThatLeavesTheTvUnhealthyIsPutBackAndTheRunStops() throws Exception {
        FakeTv t = new FakeTv(); t.breakAfterDisables = 2;   // the home goes missing after the second package
        Plan p = planOn(t, "com.tcl.bi", "com.google.android.feedback", "com.tcl.messagebox");
        Executor.Result r = new Executor(t, NO_SLEEP, TOKEN).run(p, EnumSet.of(Group.TRACKING, Group.ADS), QUIET);
        assertEquals(Group.TRACKING, r.rolledBack);
        assertTrue("nothing stays off", t.disabled.isEmpty());
        assertTrue("the ads group never ran", t.log.stream().noneMatch(c -> c.contains("com.tcl.messagebox") && c.startsWith("pm disable-user")));
    }

    @Test public void aLostNetworkIsRetriedThenCountedAsUnhealthy() throws Exception {
        FakeTv t = new FakeTv(); t.answers = false;
        assertFalse(new Executor(t, NO_SLEEP, TOKEN).healthy());
    }

    @Test public void onlyTheChosenGroupsRun() throws Exception {
        FakeTv t = new FakeTv();
        Executor.Result r = new Executor(t, NO_SLEEP, TOKEN).run(planOn(t, "com.tcl.bi", "com.tcl.messagebox", "com.tcl.gamebar"), EnumSet.of(Group.TRACKING), QUIET);
        assertEquals(Arrays.asList("com.tcl.bi"), r.disabled);
        assertFalse(t.disabled.contains("com.tcl.gamebar"));
    }

    @Test public void everyCommandTheExecutorSendsPassesTheGuard() throws Exception {
        FakeTv t = new FakeTv();   // FakeTv throws on any command the guard would refuse
        new Executor(t, NO_SLEEP, TOKEN).run(planOn(t, "com.tcl.bi", "com.google.android.feedback", "com.tcl.messagebox", "com.tcl.gamebar"),
            EnumSet.allOf(Group.class), QUIET);
    }

    // ---------- restore ----------
    @Test public void restoreIsTheExactInverseOfWhatWasDone() throws Exception {
        FakeTv t = new FakeTv();
        Executor.Result r = new Executor(t, NO_SLEEP, TOKEN).run(planOn(t, "com.tcl.bi", "com.google.android.feedback", "com.tcl.messagebox"),
            EnumSet.of(Group.TRACKING, Group.ADS), QUIET);
        Restore saved = Restore.fromJson(new Restore("43S5400A", "2026-10-28", r.disabled).toJson());
        assertEquals(r.disabled, saved.packages);
        Restore.apply(t, saved.packages);
        assertTrue("everything is on again", t.disabled.isEmpty());
        assertTrue(saved.plainText().contains("pm enable com.tcl.bi"));
    }

    @Test public void restoreDropsAnythingThatIsNotAPackageName() throws Exception {
        Restore r = new Restore("x", "d", Arrays.asList("com.tcl.bi", "evil; reboot", "com.tcl.bi", "$(id)"));
        assertEquals(Arrays.asList("com.tcl.bi"), r.packages);
    }

    // ---------- the safety net script ----------
    @Test public void theSafetyNetScriptHasOnlyTheExpectedLines() {
        String s = Deadman.script(TOKEN, Arrays.asList("com.tcl.bi", "com.google.android.feedback"), 300);
        for (String line : s.split("\n")) {
            boolean fixed = line.equals("#!/system/bin/sh") || line.equals("i=0") || line.equals("while [ $i -lt 300 ]; do") || line.equals("  [ -f " + Deadman.okPath(TOKEN) + " ] && exit 0")
                || line.equals("  sleep 1") || line.equals("  i=$((i + 1))") || line.equals("done") || line.equals("input keyevent KEYCODE_HOME");
            boolean enable = line.matches("pm enable [A-Za-z0-9_.]+");
            assertTrue("unexpected line: " + line, fixed || enable);
        }
    }

    @Test public void theSafetyNetRefusesABadTokenOrPackage() {
        try { Deadman.script("../../x", Arrays.asList("com.tcl.bi"), 300); fail(); } catch (IllegalArgumentException expected) { }
        try { Deadman.script(TOKEN, Arrays.asList("com.tcl.bi; reboot"), 300); fail(); } catch (IllegalArgumentException expected) { }
    }
}
