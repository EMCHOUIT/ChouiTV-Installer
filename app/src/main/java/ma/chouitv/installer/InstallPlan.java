package ma.chouitv.installer;

/** What to do about an app the TV may already have: pure decisions, tested on the JVM (InstallPlanTest). */
final class InstallPlan {
    private InstallPlan() {}

    /**
     * The TV already has this version or a newer one, so nothing is downloaded or installed for it.
     * An unreadable installed version (-1) or a manifest without a code (0, Channels) never skips: those install as before.
     */
    static boolean upToDate(int installedCode, int manifestCode) {
        return installedCode >= 0 && manifestCode > 0 && installedCode >= manifestCode;
    }

    /** The TV refused the install because its version is newer than the file's. */
    static boolean isDowngrade(String installerOutput) {
        return installerOutput != null && installerOutput.contains("INSTALL_FAILED_VERSION_DOWNGRADE");
    }

    /** That refusal is not a failure when the TV is at least at the announced version: there is nothing to do. */
    static boolean harmlessDowngrade(String installerOutput, int installedCode, int announcedCode) {
        return isDowngrade(installerOutput) && upToDate(installedCode, announcedCode);
    }
}
