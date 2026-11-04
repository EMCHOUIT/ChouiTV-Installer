package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Which TV on the network is the one the family had at another address (its address changed after a cut of the box). */
public class TvMatchTest {
    private static TvMatch.Candidate c(String ip, String serial, String model) { return new TvMatch.Candidate(ip, serial, model); }

    @Test public void theSameSerialIsTheSameTv() {
        List<TvMatch.Candidate> l = Arrays.asList(c("192.168.1.60", "AAA", "Xiaomi MiTV-MSSP3"), c("192.168.1.61", "BBB", "Xiaomi MiTV-MSSP3"));
        assertEquals(1, TvMatch.pick("BBB", "Xiaomi MiTV-MSSP3", l));
        assertEquals(0, TvMatch.pick("AAA", "Xiaomi MiTV-MSSP3", l));
    }

    @Test public void twoTvsOfTheSameModelAreNotMixedUpWhenSerialsDiffer() {
        List<TvMatch.Candidate> l = Arrays.asList(c("192.168.1.60", "AAA", "Xiaomi MiTV-MSSP3"), c("192.168.1.61", "BBB", "Xiaomi MiTV-MSSP3"));
        assertEquals("the serial of the saved TV is on no candidate", -1, TvMatch.pick("CCC", "Xiaomi MiTV-MSSP3", l));
    }

    @Test public void withoutASavedSerialTheOnlyTvOfThatModelIsTheOne() {
        // a TV saved before the serial was read
        List<TvMatch.Candidate> one = Arrays.asList(c("192.168.1.60", "AAA", "Xiaomi MiTV-MSSP3"), c("192.168.1.61", "BBB", "TCL 55P615"));
        assertEquals(0, TvMatch.pick("", "Xiaomi MiTV-MSSP3", one));
        List<TvMatch.Candidate> two = Arrays.asList(c("192.168.1.60", "AAA", "Xiaomi MiTV-MSSP3"), c("192.168.1.61", "BBB", "Xiaomi MiTV-MSSP3"));
        assertEquals("two of that model: no guess", -1, TvMatch.pick("", "Xiaomi MiTV-MSSP3", two));
    }

    @Test public void nothingIsGuessedWhenNothingIsKnown() {
        assertEquals(-1, TvMatch.pick("", "", Arrays.asList(c("192.168.1.60", "AAA", "X"))));
        assertEquals(-1, TvMatch.pick("AAA", "X", new ArrayList<TvMatch.Candidate>()));
        assertEquals(-1, TvMatch.pick("AAA", "X", null));
    }

    @Test public void aCandidateWithoutSerialIsMatchedByModelWhenTheSavedTvHasNone() {
        List<TvMatch.Candidate> l = Arrays.asList(c("192.168.1.60", "", "TCL 55P615"));
        assertEquals(0, TvMatch.pick("", "TCL 55P615", l));
        assertEquals("a saved serial cannot be checked against a candidate without one", -1, TvMatch.pick("AAA", "TCL 55P615", l));
    }
}
