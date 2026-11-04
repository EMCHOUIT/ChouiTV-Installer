package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Which network the search for TVs looks at: the Wi-Fi of the home, never the mobile data that also has a private address. */
public class SubnetTest {
    private static List<String[]> ifaces(String... pairs) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) l.add(new String[] {pairs[i], pairs[i + 1]});
        return l;
    }

    @Test public void theWifiIsChosenEvenWhenTheMobileDataHasAPrivateAddressToo() {
        // the real case on the family phone (Android 16): rmnet_data3 has 10.188.167.47, wlan0 has 192.168.1.60
        assertEquals("192.168.1.", Subnet.pick(ifaces("rmnet_data3", "10.188.167.47", "wlan0", "192.168.1.60"))[1]);
        assertEquals("192.168.1.", Subnet.pick(ifaces("wlan0", "192.168.1.60", "rmnet_data3", "10.188.167.47"))[1]);
        assertEquals("192.168.1.60", Subnet.pick(ifaces("rmnet_data3", "10.188.167.47", "wlan0", "192.168.1.60"))[0]);
    }

    @Test public void anEthernetOrAnotherWifiNameIsAlsoAHomeNetwork() {
        assertEquals("192.168.0.", Subnet.pick(ifaces("rmnet0", "10.1.2.3", "eth0", "192.168.0.7"))[1]);
        assertEquals("172.16.5.", Subnet.pick(ifaces("ccmni1", "10.0.0.2", "wlan1", "172.16.5.9"))[1]);
    }

    @Test public void withoutAnyWifiNameThePrivateAddressThatIsNotMobileDataIsUsed() {
        assertEquals("192.168.43.", Subnet.pick(ifaces("rmnet_data3", "10.188.167.47", "ap0", "192.168.43.1"))[1]);
        assertEquals("10.0.0.", Subnet.pick(ifaces("bond0", "10.0.0.5"))[1]);
    }

    @Test public void onlyMobileDataOrNothingGivesNoNetworkToSearch() {
        assertNull(Subnet.pick(ifaces("rmnet_data3", "10.188.167.47")));
        assertNull(Subnet.pick(ifaces("wlan0", "169.254.3.4")));          // no real address yet
        assertNull(Subnet.pick(ifaces("wlan0", "8.8.8.8")));              // not a private network
        assertNull(Subnet.pick(new ArrayList<String[]>()));
    }

    @Test public void anAddressThatIsNotWellFormedIsIgnored() {
        assertEquals("192.168.1.", Subnet.pick(ifaces("wlan0", "bad", "wlan1", "192.168.1.9"))[1]);
    }
}
