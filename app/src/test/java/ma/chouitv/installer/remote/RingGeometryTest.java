package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RingGeometryTest {
    private static int zone(float dx, float dy) { return RingGeometry.zoneAt(150 + dx, 150 + dy); }

    @Test public void theMiddleIsOkAndTheFourSidesAreTheArrows() {
        assertEquals(RingGeometry.OK, zone(0, 0));
        assertEquals(RingGeometry.OK, zone(40, 30));
        assertEquals(RingGeometry.UP, zone(0, -100));
        assertEquals(RingGeometry.DOWN, zone(0, 100));
        assertEquals(RingGeometry.LEFT, zone(-100, 0));
        assertEquals(RingGeometry.RIGHT, zone(100, 0));
    }

    @Test public void theBandAlongEachDiagonalPressesNothing() {
        assertEquals(RingGeometry.NONE, zone(90, 90));
        assertEquals(RingGeometry.NONE, zone(-90, 90));
        assertEquals(RingGeometry.NONE, zone(-90, -90));
        assertEquals(RingGeometry.NONE, zone(95, 88));      // a hair off the diagonal: still in the band
        assertEquals(RingGeometry.RIGHT, zone(100, 60));    // clearly on the right wedge
        assertEquals(RingGeometry.DOWN, zone(60, 100));
    }

    @Test public void outsideTheRingIsNothing() {
        assertEquals(RingGeometry.NONE, zone(0, 151));
        assertEquals(RingGeometry.NONE, RingGeometry.zoneAt(0, 0));
        assertEquals(RingGeometry.NONE, zone(200, 0));
    }

    @Test public void everyWedgeIsWideEnoughForAThumb() {
        // at the inner edge of the wedges (radius 60 units) the free width between the dead bands is more than 40 units
        float width = (float) (2 * 71.5 * Math.sin(Math.toRadians(45)) - 2 * RingGeometry.DEAD_BAND * Math.sqrt(2));
        assertTrue("width " + width, width > 40f);
    }

    @Test public void eachButtonSendsItsKey() {
        assertEquals(Integer.valueOf(Remote.UP), RingGeometry.keyOf(RingGeometry.UP));
        assertEquals(Integer.valueOf(Remote.LEFT), RingGeometry.keyOf(RingGeometry.LEFT));
        assertEquals(Integer.valueOf(Remote.OK), RingGeometry.keyOf(RingGeometry.OK));
        assertNull(RingGeometry.keyOf(RingGeometry.NONE));
    }
}
