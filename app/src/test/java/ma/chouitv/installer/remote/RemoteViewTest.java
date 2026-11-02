package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RemoteViewTest {
    private static int key(float x, float y) {
        Integer k = RemoteView.keyOf(RemoteView.zoneAt(x, y));
        return k == null ? -1 : k;
    }

    @Test public void theMiddleOfTheRingIsOkAndTheFourSidesAreTheArrows() {
        assertEquals(Remote.OK, key(110, 150));
        assertEquals(Remote.UP, key(110, 90));
        assertEquals(Remote.DOWN, key(110, 210));
        assertEquals(Remote.LEFT, key(50, 150));
        assertEquals(Remote.RIGHT, key(170, 150));
    }

    @Test public void theRoundButtonsAndTheVolumePillSendTheirKeys() {
        assertEquals(Remote.POWER, key(110, 46));
        assertEquals(Remote.BACK, key(68, 268));
        assertEquals(Remote.HOME, key(152, 268));
        assertEquals(Remote.MUTE, key(152, 370));
        assertEquals(Remote.VOLUME_UP, key(68, 340));
        assertEquals(Remote.VOLUME_DOWN, key(68, 398));
    }

    @Test public void theBodyBetweenTheButtonsSendsNothing() {
        assertNull(RemoteView.keyOf(RemoteView.zoneAt(10, 10)));
        assertNull(RemoteView.keyOf(RemoteView.zoneAt(110, 300)));
    }
}
