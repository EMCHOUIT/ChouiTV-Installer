package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** What the family reads on a saved settings card: the TV and a short date, never a version number or an ISO date. */
public class ProfileLabelTest {
    @Test public void theTvAndAShortDateInEachLanguage() {
        assertEquals("MiTV-MSSP3 · 28 oct.", ProfileLabel.meta("MiTV-MSSP3", "2026-10-28 18:00", "fr"));
        assertEquals("MiTV-MSSP3 · Oct 28", ProfileLabel.meta("MiTV-MSSP3", "2026-10-28 18:00", "en"));
        assertEquals("MiTV-MSSP3 · 28 أكتوبر", ProfileLabel.meta("MiTV-MSSP3", "2026-10-28 18:00", "ar"));
    }

    @Test public void aMissingPartIsLeftOut() {
        assertEquals("28 oct.", ProfileLabel.meta("", "2026-10-28", "fr"));
        assertEquals("MiTV-MSSP3", ProfileLabel.meta("MiTV-MSSP3", "", "fr"));
        assertEquals("MiTV-MSSP3", ProfileLabel.meta("MiTV-MSSP3", "not a date", "fr"));
        assertEquals("", ProfileLabel.meta(null, null, "fr"));
    }

    @Test public void theCityOfAFileIsRead() {
        assertEquals("الدار البيضاء", ProfileLabel.city("{\"settings\":{\"cityName\":\"الدار البيضاء\"}}"));
        assertEquals("", ProfileLabel.city("{\"settings\":{}}"));
        assertEquals("", ProfileLabel.city("nope"));
        assertEquals("", ProfileLabel.city(null));
    }
}
