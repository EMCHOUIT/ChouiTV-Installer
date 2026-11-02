package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.junit.Test;

import ma.chouitv.installer.cleanup.CleanupData;

/** The online cleanup lists are believed only when the owner signed them, and they can never make the protected list smaller. */
public class CleanupSourceTest {
    private static String json(String keep, String home) {
        return "{\"schema\":1,\"consensusMin\":20,\"keep\":[" + keep + "],\"homes\":[" + home + "],"
            + "\"genericModels\":[],\"genericDevices\":[],\"packages\":[\"com.a.one\",\"com.b.two\"],\"groups\":[\"ads\",\"ads\"],"
            + "\"names\":{\"ar\":{}},\"lists\":[{\"id\":\"x\",\"source\":\"ours\",\"brands\":[\"ACME\"],\"models\":[],\"devices\":[],\"p\":[0,1]}]}";
    }

    private static KeyPair pair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        return g.generateKeyPair();
    }

    private static String pub(KeyPair k) { return Base64.getEncoder().encodeToString(k.getPublic().getEncoded()); }

    private static String sign(KeyPair k, byte[] data) throws Exception {
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(k.getPrivate());
        s.update(data);
        return Base64.getEncoder().encodeToString(s.sign());
    }

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    private static final String BUNDLED = json("\"mitv.service\",\"com.android.vending\"", "\"com.home.one\"");

    @Test public void aSignedOnlineCopyIsUsed() throws Exception {
        KeyPair owner = pair();
        byte[] online = b(json("\"mitv.service\",\"com.android.vending\",\"com.new.protected\"", "\"com.home.one\""));
        CleanupData d = CleanupSource.choose(CleanupData.parse(BUNDLED), online, sign(owner, online), pub(owner));
        assertTrue(d.keep.contains("com.new.protected"));
    }

    @Test public void anUnsignedOrWronglySignedOrTamperedCopyIsRefused() throws Exception {
        KeyPair owner = pair(), other = pair();
        CleanupData bundled = CleanupData.parse(BUNDLED);
        byte[] online = b(json("", ""));   // an empty keep: the dangerous case
        assertSame(bundled, CleanupSource.choose(bundled, online, null, pub(owner)));
        assertSame(bundled, CleanupSource.choose(bundled, online, "", pub(owner)));
        assertSame(bundled, CleanupSource.choose(bundled, online, sign(other, online), pub(owner)));
        byte[] signedThenChanged = b(json("\"mitv.service\"", ""));
        String sig = sign(owner, signedThenChanged);
        assertSame(bundled, CleanupSource.choose(bundled, b(json("", "")), sig, pub(owner)));
    }

    @Test public void anOnlineCopyThatCannotBeReadIsRefusedEvenIfSigned() throws Exception {
        KeyPair owner = pair();
        CleanupData bundled = CleanupData.parse(BUNDLED);
        byte[] junk = b("not json");
        assertSame(bundled, CleanupSource.choose(bundled, junk, sign(owner, junk), pub(owner)));
        assertSame(bundled, CleanupSource.choose(bundled, null, null, pub(owner)));
    }

    @Test public void theBundledProtectedListIsAFloorEvenForASignedCopy() throws Exception {
        KeyPair owner = pair();
        byte[] online = b(json("\"com.android.vending\"", ""));   // signed, but it forgot mitv.service and the home
        CleanupData d = CleanupSource.choose(CleanupData.parse(BUNDLED), online, sign(owner, online), pub(owner));
        assertTrue(d.keep.contains("mitv.service"));
        assertTrue(d.keep.contains("com.android.vending"));
        assertTrue(d.homes.contains("com.home.one"));
    }

    @Test public void theAppsOwnKeyIsTheOwnersManifestKey() {
        assertFalse(ManifestTrust.PUBLIC_KEY.isEmpty());
        assertEquals(ManifestTrust.PUBLIC_KEY, ManifestTrust.PUBLIC_KEY.trim());
    }
}
