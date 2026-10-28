package ma.chouitv.installer.guide;

/**
 * How an operation ended, in three honest words. «Success» only when everything asked was done and nothing went wrong on the way; «partial»
 * when some of it was done; «failed» when nothing was. Also the progress of a run of steps, as a percentage.
 */
public final class Outcome {
    public enum Status { SUCCESS, PARTIAL, FAILED }

    private Outcome() { }

    /** {@code asked} things were requested, {@code done} were done, {@code problem} is true when anything went wrong along the way. */
    public static Status of(int asked, int done, boolean problem) {
        if (asked <= 0) return problem ? Status.FAILED : Status.SUCCESS;
        if (done >= asked) return problem ? Status.PARTIAL : Status.SUCCESS;
        return done > 0 ? Status.PARTIAL : Status.FAILED;
    }

    /** The share of a run that is finished: whole steps done out of the total, 0..100. */
    public static int percent(int stepsDone, int steps) {
        if (steps <= 0) return 100;
        return Math.max(0, Math.min(100, stepsDone * 100 / steps));
    }
}
