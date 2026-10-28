package ma.chouitv.installer.cleanup;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Turning apps back on: what is offered, what is never offered, and that the answer is checked on the TV. */
public class ReactivationTest {
    private static Set<String> set(String... s) { return new HashSet<>(Arrays.asList(s)); }

    private static CleanupData data() throws Exception {
        return CleanupData.parse("{\"schema\":1,\"consensusMin\":20,\"keep\":[],\"homes\":[\"com.stock.homehelper\"],\"genericModels\":[],\"genericDevices\":[],"
            + "\"packages\":[\"a.b\"],\"groups\":[\"tracking\"],\"lists\":[{\"id\":\"consensus\",\"source\":\"consensus\",\"brands\":[],\"models\":[],\"devices\":[],\"p\":[0]}]}");
    }

    @Test public void theAppsACleanupTurnedOffComeFirstAndTheOthersApart() throws Exception {
        Reactivation.Plan p = Reactivation.plan(set("com.netflix.ninja", "com.xm.webcontent", "other.app.z", "com.amazon.amazonvideo.livingroom"),
            set("com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "no.longer.off"), data());
        assertEquals(Arrays.asList("com.amazon.amazonvideo.livingroom", "com.netflix.ninja"), p.ours);
        assertEquals(Arrays.asList("com.xm.webcontent", "other.app.z"), p.others);
        assertEquals(4, p.total());
    }

    @Test public void homeScreensSetupWizardsAndInvalidNamesAreNeverOffered() throws Exception {
        Reactivation.Plan p = Reactivation.plan(set("com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx", "com.xiaomi.setupwraith",
            "com.some.setupwizard", "com.stock.homehelper", "bad name", "x;reboot", "fine.app.one"), set("com.google.android.tvlauncher"), data());
        assertEquals(Arrays.asList("fine.app.one"), p.others);
        assertTrue(p.ours.isEmpty());
    }

    @Test public void theTextCopyOfARestorePointIsReadBackToItsPackages() {
        String txt = "# ChouiTV cleanup restore point, 2026-10-28 13:04\n# Run each line with: adb shell <line>\npm enable com.miui.tv.analytics\r\npm enable fusion.android.tv.demo\n"
            + "pm enable bad;reboot\nsomething else\npm enable   com.xm.webcontent  \n";
        assertEquals(set("com.miui.tv.analytics", "fusion.android.tv.demo", "com.xm.webcontent"), Reactivation.parseRestoreText(txt));
        assertTrue(Reactivation.parseRestoreText(null).isEmpty());
    }

    @Test public void applyTurnsTheTickedAppsOnAndReportsOnlyThoseReallyOn() throws Exception {
        FakeTv t = new FakeTv();
        t.disabled.addAll(set("a.one.x", "b.two.y", "c.three.z"));
        List<String> back = Reactivation.apply(t, Arrays.asList("a.one.x", "b.two.y"));
        assertEquals(Arrays.asList("a.one.x", "b.two.y"), back);
        assertEquals(set("c.three.z"), t.disabled);
        for (String c : t.log) assertFalse("never touched: " + c, c.contains("c.three.z") && c.startsWith("pm enable"));
    }

    @Test public void anAppThatStaysOffIsNotReportedAsBack() throws Exception {
        final Set<String> off = set("a.one.x", "stubborn.app.y");
        Shell tv = c -> {
            if (c.equals("pm list packages -d")) { StringBuilder b = new StringBuilder(); for (String p : off) b.append("package:").append(p).append("\n"); return b.toString(); }
            if (c.startsWith("pm enable ") && !c.endsWith("stubborn.app.y")) off.remove(c.substring("pm enable ".length()));
            return "";
        };
        assertEquals(Arrays.asList("a.one.x"), Reactivation.apply(tv, Arrays.asList("a.one.x", "stubborn.app.y")));
    }

    @Test public void applyRefusesAnInjectedPackageName() {
        FakeTv t = new FakeTv();
        try { Reactivation.apply(t, Arrays.asList("ok.app.one", "x; reboot")); fail("accepted"); } catch (Exception expected) { }
        assertFalse(t.log.stream().anyMatch(c -> c.contains("reboot")));
    }
}
