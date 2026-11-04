package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** The search for TVs when the app is adopted: what the screen says second by second, how a TV is told apart, and when to look again. */
public class AdoptionTest {
    @Test public void theHeadlineFollowsTheSearchAndGivesItsVerdictAtFourSeconds() {
        assertEquals(Adoption.Headline.SEARCHING, Adoption.headline(0, 0));
        assertEquals(Adoption.Headline.SEARCHING, Adoption.headline(0, 3999));
        assertEquals("no TV after the budget: the verdict", Adoption.Headline.NONE, Adoption.headline(0, 4000));
        assertEquals("a TV found at once is shown at once", Adoption.Headline.FOUND, Adoption.headline(1, 300));
        assertEquals(Adoption.Headline.FOUND, Adoption.headline(3, 9000));
    }

    @Test public void theCardForAMissingTvComesWithTheVerdictAndStays() {
        assertFalse(Adoption.showMissingCard(1000));
        assertTrue(Adoption.showMissingCard(4000));
        assertTrue(Adoption.showMissingCard(60_000));
    }

    @Test public void theSearchLooksAgainEveryTenSecondsForTwoMinutesThenEveryHalfMinute() {
        assertEquals(10_000, Adoption.rescanDelayMs(0));
        assertEquals(10_000, Adoption.rescanDelayMs(119_999));
        assertEquals(30_000, Adoption.rescanDelayMs(120_000));
        assertEquals(30_000, Adoption.rescanDelayMs(1_000_000));
    }

    @Test public void aTvThatIsReadyIsNeverSentBackToAsking() {
        assertEquals(Adoption.TvState.READY, Adoption.merge(Adoption.TvState.READY, Adoption.TvState.ASK));
        assertEquals(Adoption.TvState.READY, Adoption.merge(Adoption.TvState.READY, Adoption.TvState.CHECKING));
        assertEquals(Adoption.TvState.ASK, Adoption.merge(Adoption.TvState.CHECKING, Adoption.TvState.ASK));
        assertEquals("a TV that was asking and is now known by the TV (the person accepted)", Adoption.TvState.READY, Adoption.merge(Adoption.TvState.ASK, Adoption.TvState.READY));
        assertEquals(Adoption.TvState.CHECKING, Adoption.merge(null, Adoption.TvState.CHECKING));
    }

    @Test public void theRowsAreInTheOrderOfTheAddressesAndNotOfTheAnswers() {
        List<String> sorted = Adoption.sorted(Arrays.asList("192.168.1.9", "192.168.1.51", "192.168.1.50", "192.168.1.100"));
        assertEquals(Arrays.asList("192.168.1.9", "192.168.1.50", "192.168.1.51", "192.168.1.100"), sorted);
        assertEquals(Collections.emptyList(), Adoption.sorted(Collections.<String>emptyList()));
    }

    @Test public void aTvAlreadyKnownByTheAppComesFirstInTheList() {
        List<String> sorted = Adoption.sorted(Arrays.asList("192.168.1.9", "192.168.1.50", "192.168.1.51"), Collections.singleton("192.168.1.51"));
        assertEquals(Arrays.asList("192.168.1.51", "192.168.1.9", "192.168.1.50"), sorted);
    }

    @Test public void theNumberFoundIsTheNumberOfDifferentAddresses() {
        assertEquals(2, Adoption.count(Arrays.asList("192.168.1.50", "192.168.1.51", "192.168.1.50")));
        assertEquals(0, Adoption.count(null));
    }

    @Test public void aTvSeenByItsNameButWithItsPortClosedAsksForTheNetworkDebugging() {
        assertEquals(Adoption.TvState.DEBUG_OFF, Adoption.merge(Adoption.TvState.CHECKING, Adoption.TvState.DEBUG_OFF));
        assertEquals(Adoption.TvState.DEBUG_OFF, Adoption.merge(null, Adoption.TvState.DEBUG_OFF));
        assertEquals(Adoption.TvState.DEBUG_OFF, Adoption.merge(Adoption.TvState.DEBUG_OFF, Adoption.TvState.CHECKING));
    }

    @Test public void theDebuggingTurnedOnMakesTheTvAnswerAndTheRowFollows() {
        assertEquals(Adoption.TvState.ASK, Adoption.merge(Adoption.TvState.DEBUG_OFF, Adoption.TvState.ASK));
        assertEquals(Adoption.TvState.READY, Adoption.merge(Adoption.TvState.DEBUG_OFF, Adoption.TvState.READY));
    }

    @Test public void aTvThatAnswersIsNeverSentBackToDebuggingOff() {
        assertEquals(Adoption.TvState.READY, Adoption.merge(Adoption.TvState.READY, Adoption.TvState.DEBUG_OFF));
        assertEquals(Adoption.TvState.ASK, Adoption.merge(Adoption.TvState.ASK, Adoption.TvState.DEBUG_OFF));
    }

    @Test public void theNameOfARowIsTheOneTheFamilyChoseThenTheOneTheTvAnnouncesThenItsModel() {
        assertEquals("Salon", Adoption.title("Salon", "MiTV-MSSP3", "Xiaomi MiTV-MSSP3", "Télé"));
        assertEquals("MiTV-MSSP3", Adoption.title("", "MiTV-MSSP3", "Xiaomi MiTV-MSSP3", "Télé"));
        assertEquals("Xiaomi MiTV-MSSP3", Adoption.title(null, "  ", "Xiaomi MiTV-MSSP3", "Télé"));
        assertEquals("Télé", Adoption.title("", "", "", "Télé"));
        assertEquals("Télé", Adoption.title(null, null, null, "Télé"));
    }
}
