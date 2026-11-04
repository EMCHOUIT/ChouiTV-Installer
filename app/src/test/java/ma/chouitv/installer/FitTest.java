package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The pad of the remote takes the room that is left on this screen: never wider than the screen, never taller than what the other keys leave. */
public class FitTest {
    @Test public void aRoomyScreenGivesTheWholeWidthUpToTheCap() {
        assertEquals(340, Fit.ringSide(430, 400, 340, 180));     // wider than the cap: the cap
        assertEquals(300, Fit.ringSide(300, 900, 340, 180));     // a narrower screen: its width
    }

    @Test public void aShortScreenShrinksThePadByWhatOverflows() {
        assertEquals(270, Fit.ringSide(340, 340 - 70, 340, 180) );   // 70 too tall: 70 smaller
    }

    @Test public void thePadNeverGetsSmallerThanItsFloor() {
        assertEquals(180, Fit.ringSide(340, 20, 340, 180));
        assertEquals(180, Fit.ringSide(100, 500, 340, 180));     // a screen narrower than the floor: the floor (the page then scrolls)
    }

    @Test public void theOverflowIsWhatPassesTheVisibleHeight() {
        assertEquals(0, Fit.overflow(700, 800));
        assertEquals(0, Fit.overflow(800, 800));
        assertEquals(50, Fit.overflow(850, 800));
    }

    @Test public void theTextOfAKeyKeepsRoomForTwoLinesAtAnyFontScale() {
        assertEquals(82, Fit.keyHeightDp(80, 14, 1.0f));
        assertEquals(87, Fit.keyHeightDp(80, 14, 1.15f));
        assertEquals(93, Fit.keyHeightDp(80, 14, 1.3f));
        assertEquals(104, Fit.keyHeightDp(80, 14, 1.6f));
        assertEquals(82, Fit.keyHeightDp(80, 14, 0.85f));
    }

    @Test public void aLabelNeverGetsBiggerOnTheScreenThanWhatItsKeyCanHold() {
        assertEquals(13.9f, Fit.labelSp(14, 420, 1.0f), 0.05f);                // a normal phone: close to the design size
        assertEquals(true, Fit.labelSp(14, 420, 1.15f) * 1.15f <= 13.95f);       // enlarged text: what it looks like on screen does not grow
        assertEquals(true, Fit.labelSp(14, 360, 1.3f) * 1.3f <= 11.8f);         // a narrow screen with big text (the floor of 9 sp holds)
        assertEquals(14f, Fit.labelSp(14, 800, 1.0f), 0.01f);                  // a wide screen: never above the design size
        assertEquals(true, Fit.labelSp(12, 360, 1.3f) >= 9f);                   // never unreadably small
    }

    @Test public void aWideScreenKeepsTheContentToAReadableWidthAndCentred() {
        assertEquals(22, Fit.sidePaddingDp(360, 22, 560));    // a phone: the usual padding
        assertEquals(22, Fit.sidePaddingDp(560, 22, 560));
        assertEquals(40, Fit.sidePaddingDp(640, 22, 560));    // 640 - 2 x 40 = 560
        assertEquals(220, Fit.sidePaddingDp(1000, 22, 560));  // 1000 - 2 x 220 = 560
    }

    @Test public void aShortScreenUsesSmallerKeys() {
        assertEquals(true, Fit.compact(640));
        assertEquals(true, Fit.compact(700));
        assertEquals(false, Fit.compact(720));
        assertEquals(false, Fit.compact(1000));
        assertEquals(66, Fit.tileHeight(640, 80));
        assertEquals(80, Fit.tileHeight(900, 80));
        assertEquals(50, Fit.roundSize(640, 60));
        assertEquals(60, Fit.roundSize(900, 60));
    }
}
