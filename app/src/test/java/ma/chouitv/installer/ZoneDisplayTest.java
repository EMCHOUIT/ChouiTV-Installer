package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What the family reads about a time zone: Casablanca is Africa/Casablanca (the TV keeps Africa/Abidjan inside, because its own zone data gives Casablanca +1 hour). */
public class ZoneDisplayTest {
    private static final String CASA = "{\"app\":\"ChouiTV\",\"settings\":{\"cityName\":\"الدار البيضاء\",\"cityTz\":\"Africa/Abidjan\"}}";
    private static final String CASA_LATIN = "{\"settings\":{\"cityName\":\"Casablanca\"}}";
    private static final String RABAT = "{\"settings\":{\"cityName\":\"الرباط\",\"cityTz\":\"Africa/Abidjan\"}}";

    @Test public void aFileOfCasablancaShowsAfricaCasablancaInTheChanges() {
        String text = "preview: 1 change(s)\n- منطقة المدينة: Africa/Abidjan  (كان Africa/Casablanca)";
        assertEquals("preview: 1 change(s)\n- منطقة المدينة: Africa/Casablanca  (كان Africa/Casablanca)", ZoneDisplay.show(text, CASA));
        assertTrue(ZoneDisplay.show("Africa/Abidjan", CASA_LATIN).equals("Africa/Casablanca"));
    }

    @Test public void anotherCityKeepsWhatTheTvSays() {
        assertEquals("Africa/Abidjan", ZoneDisplay.show("Africa/Abidjan", RABAT));
    }

    @Test public void aFileWithoutACityOrNotAFileChangesNothing() {
        assertEquals("Africa/Abidjan", ZoneDisplay.show("Africa/Abidjan", "{\"settings\":{}}"));
        assertEquals("Africa/Abidjan", ZoneDisplay.show("Africa/Abidjan", "not json"));
        assertEquals("Africa/Abidjan", ZoneDisplay.show("Africa/Abidjan", null));
        assertEquals("", ZoneDisplay.show(null, CASA));
    }

    @Test public void theCityIsRecognisedInArabicAndInLatinLetters() {
        assertTrue(ZoneDisplay.isCasablanca("الدار البيضاء"));
        assertTrue(ZoneDisplay.isCasablanca("Casablanca"));
        assertTrue(ZoneDisplay.isCasablanca("  casablanca "));
        assertFalse(ZoneDisplay.isCasablanca("Rabat"));
        assertFalse(ZoneDisplay.isCasablanca(null));
    }
}
