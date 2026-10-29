package ma.chouitv.installer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.junit.Test;

public class ManifestTrustTest {
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

    private static final byte[] FILE = "{\"versionCode\":180}".getBytes(StandardCharsets.UTF_8);

    @Test public void theOwnersSignatureIsAccepted() throws Exception {
        KeyPair owner = pair();
        assertTrue(ManifestTrust.verify(FILE, sign(owner, FILE), pub(owner)));
        assertTrue(ManifestTrust.verify(FILE, sign(owner, FILE) + "\n", pub(owner)));
    }

    @Test public void everythingElseIsRefused() throws Exception {
        KeyPair owner = pair(), other = pair();
        String sig = sign(owner, FILE);
        assertFalse(ManifestTrust.verify("{\"versionCode\":181}".getBytes(StandardCharsets.UTF_8), sig, pub(owner)));
        assertFalse(ManifestTrust.verify(FILE, sig, pub(other)));
        assertFalse(ManifestTrust.verify(FILE, "not a signature", pub(owner)));
        assertFalse(ManifestTrust.verify(FILE, "", pub(owner)));
        assertFalse(ManifestTrust.verify(FILE, null, pub(owner)));
        assertFalse(ManifestTrust.verify(null, sig, pub(owner)));
        assertFalse(ManifestTrust.verify(FILE, sig, "garbage"));
    }

    @Test public void theCarriedKeyIsAValidP256Key() throws Exception {
        // a signature by some other key must not pass the carried one, and the carried one must at least parse
        assertFalse(ManifestTrust.verify(FILE, sign(pair(), FILE)));
        assertTrue(ManifestTrust.PUBLIC_KEY.startsWith("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE"));
    }
}
