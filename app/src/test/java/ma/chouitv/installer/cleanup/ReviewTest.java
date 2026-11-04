package ma.chouitv.installer.cleanup;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ReviewTest {
    @Test public void aShortListIsShownWholeOneNamePerLine() {
        assertEquals("• Netflix\n• Prime Video", Review.lines(Arrays.asList("Netflix", "Prime Video"), 6, "%d autres"));
    }

    @Test public void aLongListIsCutWithTheNumberOfTheOthers() {
        java.util.List<String> names = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");
        assertEquals("• A\n• B\n• C\n• D\n• E\n• F\n+ 2 autres", Review.lines(names, 6, "%d autres"));
    }

    @Test public void theSameNameTwiceIsShownOnceAndBlanksAreDropped() {
        assertEquals("• Netflix\n• Drama Mp4", Review.lines(Arrays.asList("Netflix", " ", "Netflix", "", "Drama Mp4"), 6, "%d autres"));
    }

    @Test public void nothingToShowGivesAnEmptyText() {
        assertEquals("", Review.lines(Collections.<String>emptyList(), 6, "%d autres"));
        assertEquals("", Review.lines(null, 6, "%d autres"));
    }

    @Test public void theLimitIsAtLeastOneAndTheOthersAreCountedOnce() {
        assertEquals("• A\n+ 2 autres", Review.lines(Arrays.asList("A", "B", "C"), 0, "%d autres"));
    }
}
