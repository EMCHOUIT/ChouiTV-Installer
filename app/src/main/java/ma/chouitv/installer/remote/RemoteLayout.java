package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The layout of the remote: a grid of 12 columns whose unit is a square of about 26 dp, the keys placed on it, ready-made templates, and the guards
 * that keep the remote usable whatever is done to it (the pad, Back and one form of the volume always stay; nothing overlaps or leaves the grid).
 * Immutable and pure: every edit returns a new layout, or says why it was refused. Tested on the JVM (RemoteLayoutTest).
 */
public final class RemoteLayout {
    public static final int COLS = 12;
    private static final String VERSION = "v1";

    public enum Kind { PAD, BACK, HOME, MUTE, POWER, VOL_ROCKER, VOL_BAR, VOL_PILL,
        LISTEN, CHANNELS, APPS, SAVER, WRITE, TVSET, CH_ROCKER, YOUTUBE, NETFLIX, KEYBOARD }

    public enum Template { CLASSIC, RIGHT, LEFT, ELDER, COMPACT, FIVE }

    public enum Why { OFF_GRID, TOO_TALL, OVERLAP, REQUIRED, BAD_SIZE, ALREADY, NO_ROOM, VOLUME_FORM }

    public static final class Block {
        public final Kind kind;
        public final int col, row, w, h;
        Block(Kind kind, int col, int row, int w, int h) { this.kind = kind; this.col = col; this.row = row; this.w = w; this.h = h; }
        boolean overlaps(Block o) { return col < o.col + o.w && o.col < col + w && row < o.row + o.h && o.row < row + h; }
    }

    /** The result of an edit: the new layout, or the reason for the refusal (the layout is then null). */
    public static final class Edit {
        public final RemoteLayout layout;
        public final Why why;
        Edit(RemoteLayout layout, Why why) { this.layout = layout; this.why = why; }
        public boolean ok() { return layout != null; }
    }

    private final List<Block> blocks;

    private RemoteLayout(List<Block> blocks) { this.blocks = Collections.unmodifiableList(blocks); }

    // ---- what a key is
    public static boolean isVolume(Kind k) { return k == Kind.VOL_ROCKER || k == Kind.VOL_BAR || k == Kind.VOL_PILL; }

    /** The pad, Back and the volume are the keys that stay. */
    public static boolean required(Kind k) { return k == Kind.PAD || k == Kind.BACK || isVolume(k); }

    /** The sizes planned for a key, (width, height) in grid units; the first one is its usual size. */
    public static int[][] sizes(Kind k) {
        switch (k) {
            case PAD: return new int[][] {{12, 12}, {9, 9}};
            case VOL_ROCKER: case CH_ROCKER: return new int[][] {{3, 6}};
            case VOL_BAR: return new int[][] {{12, 3}};
            case VOL_PILL: return new int[][] {{6, 3}, {8, 3}};
            case BACK: case HOME: return new int[][] {{4, 3}, {3, 3}, {6, 3}};
            case LISTEN: case CHANNELS: case APPS: case SAVER: case WRITE: case TVSET:
                return new int[][] {{4, 3}, {3, 3}, {8, 3}};
            default: return new int[][] {{4, 3}, {3, 3}};
        }
    }

    private static boolean sizeAllowed(Kind k, int w, int h) {
        for (int[] s : sizes(k)) if (s[0] == w && s[1] == h) return true;
        return false;
    }

    // ---- reading
    public List<Block> blocks() { return blocks; }

    public Block get(Kind k) { for (Block b : blocks) if (b.kind == k) return b; return null; }

    public boolean has(Kind k) { return get(k) != null; }

    /** The rows the layout uses (the bottom of its lowest key). */
    public int rows() { int r = 0; for (Block b : blocks) r = Math.max(r, b.row + b.h); return r; }

    /** How many rows a screen offers for the keys, given the height left for them and the size of a grid unit (both dp). */
    public static int maxRows(int heightDp, int unitDp) { return unitDp <= 0 ? 0 : Math.max(0, heightDp / unitDp); }

    /** Null when the layout is usable on a screen of {@code maxRows} rows, otherwise what is wrong ("size", "grid", "overlap", "missing", "volume", "tall"). */
    public String problem(int maxRows) {
        int volumes = 0;
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (!sizeAllowed(b.kind, b.w, b.h)) return "size";
            if (b.col < 0 || b.row < 0 || b.col + b.w > COLS) return "grid";
            for (int j = i + 1; j < blocks.size(); j++) {
                Block o = blocks.get(j);
                if (o.kind == b.kind) return "overlap";   // one of each
                if (b.overlaps(o)) return "overlap";
            }
            if (isVolume(b.kind)) volumes++;
        }
        if (!has(Kind.PAD) || !has(Kind.BACK)) return "missing";
        if (volumes != 1) return "volume";
        if (rows() > maxRows) return "tall";
        return null;
    }

    // ---- editing
    private List<Block> copyWithout(Kind k) {
        List<Block> l = new ArrayList<>();
        for (Block b : blocks) if (b.kind != k) l.add(b);
        return l;
    }

    private static Why placeable(List<Block> others, Block nb, int maxRows) {
        if (!sizeAllowed(nb.kind, nb.w, nb.h)) return Why.BAD_SIZE;
        if (nb.col < 0 || nb.row < 0 || nb.col + nb.w > COLS) return Why.OFF_GRID;
        if (nb.row + nb.h > maxRows) return Why.TOO_TALL;
        for (Block o : others) if (o.overlaps(nb)) return Why.OVERLAP;
        return null;
    }

    public Edit move(Kind k, int col, int row, int maxRows) {
        Block b = get(k);
        if (b == null) return new Edit(null, Why.NO_ROOM);
        List<Block> others = copyWithout(k);
        Block nb = new Block(k, col, row, b.w, b.h);
        Why why = placeable(others, nb, maxRows);
        if (why != null) return new Edit(null, why);
        others.add(nb);
        return new Edit(new RemoteLayout(others), null);
    }

    public Edit resize(Kind k, int w, int h, int maxRows) {
        Block b = get(k);
        if (b == null) return new Edit(null, Why.NO_ROOM);
        List<Block> others = copyWithout(k);
        Block nb = new Block(k, b.col, b.row, w, h);
        Why why = placeable(others, nb, maxRows);
        if (why != null) return new Edit(null, why);
        others.add(nb);
        return new Edit(new RemoteLayout(others), null);
    }

    /** The first place, scanning from the top left, where a key of one of its sizes fits; null when there is none. */
    private static Block firstFree(List<Block> others, Kind k, int maxRows) {
        for (int[] s : sizes(k)) {
            for (int r = 0; r + s[1] <= maxRows; r++) {
                for (int c = 0; c + s[0] <= COLS; c++) {
                    Block nb = new Block(k, c, r, s[0], s[1]);
                    if (placeable(others, nb, maxRows) == null) return nb;
                }
            }
        }
        return null;
    }

    public Edit add(Kind k, int maxRows) {
        if (has(k)) return new Edit(null, Why.ALREADY);
        if (isVolume(k)) for (Block b : blocks) if (isVolume(b.kind)) return new Edit(null, Why.ALREADY);
        List<Block> l = new ArrayList<>(blocks);
        Block nb = firstFree(l, k, maxRows);
        if (nb == null) return new Edit(null, Why.NO_ROOM);
        l.add(nb);
        return new Edit(new RemoteLayout(l), null);
    }

    public Edit remove(Kind k) {
        if (!has(k)) return new Edit(null, Why.NO_ROOM);
        if (required(k)) return new Edit(null, Why.REQUIRED);
        return new Edit(new RemoteLayout(copyWithout(k)), null);
    }

    /** Changes the form of the volume (rocker, bar, pill): it keeps its place when the new form fits there, otherwise it takes the first free place. */
    public Edit swapVolume(Kind form, int maxRows) {
        if (!isVolume(form)) return new Edit(null, Why.VOLUME_FORM);
        Block old = null;
        for (Block b : blocks) if (isVolume(b.kind)) old = b;
        List<Block> others = copyWithout(old == null ? form : old.kind);
        if (old != null) {
            int[] s = sizes(form)[0];
            Block same = new Block(form, old.col, old.row, s[0], s[1]);
            if (placeable(others, same, maxRows) == null) { others.add(same); return new Edit(new RemoteLayout(others), null); }
        }
        Block nb = firstFree(others, form, maxRows);
        if (nb == null) return new Edit(null, Why.NO_ROOM);
        others.add(nb);
        return new Edit(new RemoteLayout(others), null);
    }

    // ---- templates
    private static Block b(Kind k, int col, int row, int w, int h) { return new Block(k, col, row, w, h); }

    public static RemoteLayout template(Template t) {
        List<Block> l = new ArrayList<>();
        switch (t) {
            case CLASSIC:
                shortcutsTwoRows(l);
                l.add(b(Kind.PAD, 0, 6, 12, 12));
                l.add(b(Kind.BACK, 0, 18, 4, 3)); l.add(b(Kind.HOME, 4, 18, 4, 3)); l.add(b(Kind.MUTE, 8, 18, 4, 3));
                l.add(b(Kind.VOL_BAR, 0, 21, 12, 3));
                break;
            case RIGHT:
                shortcutsTwoRows(l);
                l.add(b(Kind.PAD, 0, 6, 9, 9));
                l.add(b(Kind.BACK, 0, 15, 3, 3)); l.add(b(Kind.HOME, 3, 15, 3, 3)); l.add(b(Kind.MUTE, 6, 15, 3, 3));
                l.add(b(Kind.VOL_ROCKER, 9, 12, 3, 6));
                break;
            case LEFT:
                return mirror(template(Template.RIGHT));
            case ELDER:
                l.add(b(Kind.LISTEN, 0, 0, 4, 3)); l.add(b(Kind.CHANNELS, 4, 0, 4, 3)); l.add(b(Kind.APPS, 8, 0, 4, 3));
                l.add(b(Kind.PAD, 0, 3, 12, 12));
                l.add(b(Kind.BACK, 0, 15, 6, 3)); l.add(b(Kind.HOME, 6, 15, 6, 3));
                l.add(b(Kind.VOL_PILL, 2, 18, 8, 3));
                break;
            case FIVE:
                l.add(b(Kind.LISTEN, 2, 0, 8, 3));
                l.add(b(Kind.PAD, 0, 3, 12, 12));
                l.add(b(Kind.BACK, 0, 15, 6, 3));
                l.add(b(Kind.VOL_PILL, 6, 15, 6, 3));
                break;
            default:   // COMPACT
                l.add(b(Kind.LISTEN, 0, 0, 3, 3)); l.add(b(Kind.CHANNELS, 3, 0, 3, 3)); l.add(b(Kind.APPS, 6, 0, 3, 3)); l.add(b(Kind.WRITE, 9, 0, 3, 3));
                l.add(b(Kind.PAD, 0, 3, 9, 9));
                l.add(b(Kind.BACK, 0, 12, 3, 3)); l.add(b(Kind.HOME, 3, 12, 3, 3)); l.add(b(Kind.MUTE, 6, 12, 3, 3));
                l.add(b(Kind.VOL_ROCKER, 9, 6, 3, 6));
                break;
        }
        return new RemoteLayout(l);
    }

    private static void shortcutsTwoRows(List<Block> l) {
        Kind[] k = {Kind.LISTEN, Kind.CHANNELS, Kind.APPS, Kind.SAVER, Kind.WRITE, Kind.TVSET};
        for (int i = 0; i < 6; i++) l.add(b(k[i], (i % 3) * 4, (i / 3) * 3, 4, 3));
    }

    /** The same layout seen in a mirror (the left-handed one). */
    public static RemoteLayout mirror(RemoteLayout src) {
        List<Block> l = new ArrayList<>();
        for (Block x : src.blocks) l.add(b(x.kind, COLS - x.col - x.w, x.row, x.w, x.h));
        return new RemoteLayout(l);
    }

    // ---- saving
    public String encode() {
        StringBuilder sb = new StringBuilder(VERSION);
        for (Block x : blocks) sb.append(';').append(x.kind.name()).append(':').append(x.col).append(',').append(x.row).append(',').append(x.w).append(',').append(x.h);
        return sb.toString();
    }

    /** What was saved, or null when it is missing, unreadable, of a version we do not know, or not usable (it is never trusted blindly). */
    public static RemoteLayout decode(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(";");
        if (parts.length < 2 || !VERSION.equals(parts[0])) return null;
        List<Block> l = new ArrayList<>();
        try {
            for (int i = 1; i < parts.length; i++) {
                int c = parts[i].indexOf(':');
                if (c < 0) return null;
                Kind k = Kind.valueOf(parts[i].substring(0, c));
                String[] n = parts[i].substring(c + 1).split(",");
                if (n.length != 4) return null;
                l.add(b(k, Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2]), Integer.parseInt(n[3])));
            }
        } catch (IllegalArgumentException e) { return null; }
        RemoteLayout r = new RemoteLayout(l);
        return r.problem(60) == null ? r : null;
    }

    public static RemoteLayout orClassic(String s) {
        RemoteLayout r = decode(s);
        return r != null ? r : template(Template.CLASSIC);
    }
}
