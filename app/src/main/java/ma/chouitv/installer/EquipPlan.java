package ma.chouitv.installer;

/** The preview before a set-up («Set up Salon»): the clean-up is the only step the person can leave out; the rest is fixed. Pure, tested on the JVM (EquipPlanTest). */
final class EquipPlan {
    private EquipPlan() {}

    /** The step of the guided path to start at: 0 the clean-up, 1 the install. */
    static int startStep(boolean cleanUp) { return cleanUp ? 0 : 1; }

    /** About how many minutes the set-up takes. */
    static int minutes(boolean cleanUp) { return cleanUp ? 8 : 4; }

    /** A clean-up left out is still offered once at the end (it is never forgotten, never asked twice). */
    static boolean offerCleanUpAtTheEnd(boolean cleanUp) { return !cleanUp; }
}
