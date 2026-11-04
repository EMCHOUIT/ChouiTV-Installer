package ma.chouitv.installer;

/** How the remote fits the screen it is on (any width, any height, any font scale). Pure, tested on the JVM (FitTest). All sizes in dp unless said. */
final class Fit {
    private Fit() {}

    /** The side of the pad: the width the screen gives, cut by what the page overflows, never above the cap nor below the floor. */
    static int ringSide(int width, int currentSideMinusOverflow, int cap, int floor) {
        int side = Math.min(width, cap);
        side = Math.min(side, currentSideMinusOverflow);
        return Math.max(floor, side);
    }

    /** What passes the visible height (0 when it all fits). */
    static int overflow(int contentBottom, int visibleHeight) { return Math.max(0, contentBottom - visibleHeight); }

    /** A shortcut key holds an icon and up to two lines of text: it grows when the font is enlarged, so nothing is squeezed or overlaps. */
    static int keyHeightDp(int baseDp, int textSp, float fontScale) {
        float lines = 2 * textSp * Math.max(1f, fontScale) * 1.35f;   // two lines of text
        int needed = Math.round(26 + 18 + lines);                      // icon + padding + text
        return Math.max(baseDp, needed);
    }

    /** The size (sp) of a key's label: what it looks like on screen (sp × font scale) stays under what the key can hold for this width, never above the design size, never below 9. */
    static float labelSp(float designSp, float widthDp, float fontScale) {
        float cap = Math.min(15f, (widthDp - 132f) / 20.7f);   // what a key can hold: 360 dp: 11; 420 dp: 13.9; 450 dp and more: 15
        float sp = Math.min(designSp, cap / Math.max(1f, fontScale));
        return Math.max(9f, sp);
    }

    /** The side padding (dp) of a page: the usual one, more on a wide screen so that the content stays at {@code maxContentDp} and centred. */
    static int sidePaddingDp(int widthDp, int usualDp, int maxContentDp) {
        return Math.max(usualDp, (widthDp - maxContentDp) / 2);
    }

    /** A short screen (under 720 dp tall) uses smaller keys, so the whole remote shows without scrolling. */
    static boolean compact(int heightDp) { return heightDp < 720; }

    static int tileHeight(int heightDp, int normalDp) { return compact(heightDp) ? Math.round(normalDp * 0.82f) : normalDp; }

    static int roundSize(int heightDp, int normalDp) { return compact(heightDp) ? Math.round(normalDp * 0.83f) : normalDp; }
}
