package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** When an app is already up to date on the TV and must not be installed again (INSTALL_FAILED_VERSION_DOWNGRADE came from there). */
public class InstallPlanTest {
    @Test public void sameVersionIsSkipped() { assertTrue(InstallPlan.upToDate(400, 400)); }

    @Test public void aNewerVersionOnTheTvIsSkipped() { assertTrue(InstallPlan.upToDate(401, 400)); }

    @Test public void anOlderVersionOnTheTvIsInstalled() { assertFalse(InstallPlan.upToDate(399, 400)); }

    @Test public void anUnreadableOrMissingVersionInstallsAsBefore() {
        assertFalse("not installed", InstallPlan.upToDate(-1, 400));
        assertFalse("the manifest has no code (Channels)", InstallPlan.upToDate(300, 0));
        assertFalse(InstallPlan.upToDate(-1, 0));
    }

    @Test public void theDowngradeRefusalIsRecognised() {
        assertTrue(InstallPlan.isDowngrade("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]"));
        assertTrue(InstallPlan.isDowngrade("ma.chouitv.home: Performing Streamed Install\nFailure [INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected]"));
        assertFalse(InstallPlan.isDowngrade("Success"));
        assertFalse(InstallPlan.isDowngrade("Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]"));
        assertFalse(InstallPlan.isDowngrade(null));
    }

    @Test public void aDowngradeRefusalIsHarmlessOnlyWhenTheTvIsNotBehind() {
        assertTrue(InstallPlan.harmlessDowngrade("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]", 400, 400));
        assertTrue(InstallPlan.harmlessDowngrade("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]", 410, 400));
        assertFalse("older on the TV: a real problem", InstallPlan.harmlessDowngrade("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]", 390, 400));
        assertFalse("unreadable: not assumed fine", InstallPlan.harmlessDowngrade("Failure [INSTALL_FAILED_VERSION_DOWNGRADE]", -1, 400));
        assertFalse("another failure is never harmless", InstallPlan.harmlessDowngrade("Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]", 500, 400));
    }
}
