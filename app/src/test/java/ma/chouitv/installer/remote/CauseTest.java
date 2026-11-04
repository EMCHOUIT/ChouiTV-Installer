package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import org.junit.Test;

/** Why the TV does not answer, told from what the phone knows: its Wi-Fi, its network, and how the connection failed. */
public class CauseTest {
    @Test public void aPhoneOffWifiIsToldBeforeAnythingElse() {
        assertEquals(Cause.NO_WIFI, Cause.of(new SocketTimeoutException("failed to connect"), false, true, false));
        assertEquals("even when the failure looks like a refusal", Cause.NO_WIFI, Cause.of(new ConnectException("Connection refused"), false, false, true));
    }

    @Test public void anotherNetworkIsToldWhenTheWifiIsOn() {
        assertEquals(Cause.OTHER_NETWORK, Cause.of(new SocketTimeoutException("failed to connect"), true, false, false));
    }

    @Test public void aTvThatAsksForTheKeyIsToldFirstOnTheRightNetwork() {
        assertEquals(Cause.AUTH, Cause.of(new SocketTimeoutException("Read timed out"), true, true, true));
    }

    @Test public void aRefusalMeansTheTvIsThereButDebuggingIsOffOrStarting() {
        assertEquals(Cause.REFUSED, Cause.of(new ConnectException("failed to connect to /192.168.1.50 (port 5555): connect failed: ECONNREFUSED (Connection refused)"), true, true, false));
        assertEquals(Cause.REFUSED, Cause.of(new ConnectException("Connection refused"), true, true, false));
        assertEquals("the refusal can be the cause of the exception", Cause.REFUSED,
            Cause.of(new IOException("connect", new ConnectException("ECONNREFUSED")), true, true, false));
    }

    @Test public void silenceOrNoRouteMeansTheTvIsOffOrAsleep() {
        assertEquals(Cause.SILENT, Cause.of(new SocketTimeoutException("failed to connect to /192.168.1.50 (port 5555) from /192.168.1.60 (port 41234) after 1500ms"), true, true, false));
        assertEquals(Cause.SILENT, Cause.of(new NoRouteToHostException("No route to host"), true, true, false));
        assertEquals(Cause.SILENT, Cause.of(new ConnectException("failed to connect: EHOSTUNREACH (No route to host)"), true, true, false));
        assertEquals(Cause.SILENT, Cause.of(new IOException("the TV did not accept the connection"), true, true, false));
        assertEquals(Cause.SILENT, Cause.of(null, true, true, false));
    }
}
