package ma.chouitv.installer.cleanup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class RecheckTest {
    private static final String DUMP = "ACTIVITY MANAGER SERVICES (dumpsys activity services)\n"
        + "  User 0 active services:\n"
        + "  * ServiceRecord{e94309d u0 mitv.service/.TvServer}\n"
        + "    intent={act=mitv.service.TvServer cmp=mitv.service/.TvServer}\n"
        + "    packageName=mitv.service\n"
        + "    processName=xiaomi.tvservice\n"
        + "  * ServiceRecord{aa u0 com.tcl.bi/.Collector}\n"
        + "    packageName=com.tcl.bi\n"
        + "  Connection bindings to services:\n"
        + "  * ConnectionRecord{1 u0 CR com.other.app/.S}\n"
        + "    packageName=com.should.not.count\n";

    @Test public void activeServicesAreReadFromTheActiveBlockOnly() {
        assertEquals(new HashSet<>(Arrays.asList("mitv.service", "com.tcl.bi")), Facts.activeServicePackages(DUMP));
        assertTrue(Facts.activeServicePackages(null).isEmpty());
        assertTrue(Facts.activeServicePackages("ACTIVITY MANAGER SERVICES\n  (nothing)\n").isEmpty());
    }

    @Test public void theRestorePointKeepsWhichOnesHadAServiceAndOldPointsStillLoad() throws Exception {
        Restore r = new Restore("MiTV-MSSP3", "2026-11-02", Arrays.asList("mitv.service", "com.tcl.bi", "com.x.y"))
            .withServices(new HashSet<>(Arrays.asList("mitv.service", "com.not.disabled")));
        assertEquals(Arrays.asList("mitv.service"), r.servicesBefore);
        Restore back = Restore.fromJson(r.toJson());
        assertEquals(Arrays.asList("mitv.service"), back.servicesBefore);
        Restore old = Restore.fromJson("{\"schema\":1,\"tv\":\"x\",\"date\":\"d\",\"packages\":[\"com.tcl.bi\"]}");
        assertTrue(old.servicesBefore.isEmpty());
        assertEquals(Arrays.asList("com.tcl.bi"), old.packages);
    }

    @Test public void aRecheckTellsWhatStayedOffFromWhatTheTvTurnedBackOn() {
        Restore r = new Restore("MiTV-MSSP3", "d", Arrays.asList("com.netflix.ninja", "com.tcl.bi", "mitv.service"))
            .withServices(new HashSet<>(Arrays.asList("mitv.service")));
        Recheck c = Recheck.compare(r, new HashSet<>(Arrays.asList("com.tcl.bi", "mitv.service")));
        assertEquals(Arrays.asList("com.tcl.bi", "mitv.service"), c.stillOff);
        assertEquals(Arrays.asList("com.netflix.ninja"), c.backOn);
        assertEquals(Arrays.asList("mitv.service"), c.suspects);
    }

    @Test public void puttingBackASystemServiceNeedsARestart() {
        Restore r = new Restore("m", "d", Arrays.asList("mitv.service", "com.tcl.bi")).withServices(new HashSet<>(Arrays.asList("mitv.service")));
        assertTrue(Recheck.needsRestartAfterEnable(r, Arrays.asList("mitv.service")));
        assertFalse(Recheck.needsRestartAfterEnable(r, Arrays.asList("com.tcl.bi")));
    }
}
