package ma.chouitv.installer.remote;

/** Where the five buttons of the ring pad are, on a 300 x 300 grid: OK in the middle, four wedges around it, and a dead band along each diagonal so that a thumb between two wedges presses neither. */
public final class RingGeometry {
    private RingGeometry() {}

    public static final float SIZE = 300f, CENTER = 150f, OK_RADIUS = 64f, OUTER_RADIUS = 149f, DEAD_BAND = 8f;
    public static final int NONE = -1, UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3, OK = 4;

    /** The button under (x, y) of the grid. */
    public static int zoneAt(float x, float y) {
        float dx = x - CENTER, dy = y - CENTER;
        double d = Math.hypot(dx, dy);
        if (d <= OK_RADIUS) return OK;
        if (d > OUTER_RADIUS) return NONE;
        float ax = Math.abs(dx), ay = Math.abs(dy);
        if (Math.abs(ax - ay) / Math.sqrt(2) < DEAD_BAND) return NONE;
        return ax > ay ? (dx < 0 ? LEFT : RIGHT) : (dy < 0 ? UP : DOWN);
    }

    /** The key a button sends (null for none). */
    public static Integer keyOf(int zone) {
        switch (zone) {
            case UP: return Remote.UP;
            case RIGHT: return Remote.RIGHT;
            case DOWN: return Remote.DOWN;
            case LEFT: return Remote.LEFT;
            case OK: return Remote.OK;
            default: return null;
        }
    }
}
