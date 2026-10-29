package ma.chouitv.installer;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Who may announce a new version. latest.json is read from GitHub, so the owner signs it on their own computer (optimize/sign-manifest.sh in the
 * ChouiTV repository) and the installer believes only a latest.json whose signature (latest.json.sig, next to it) matches the public key below.
 * Same rule and same key as the TVs'. No Android here, so it is tested on the JVM (ManifestTrustTest).
 */
final class ManifestTrust {
    private ManifestTrust() {}

    /** The public half of the owner's manifest key (ECDSA P-256, X.509, base64). Same text as optimize/manifest-pub.b64 in the ChouiTV repository. */
    static final String PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAElwR4egMT3VnU3EmrGp5he7b0hR0lJS5q+07NXc2sDTG2AvgOxrQW41/rE4ZSolH0k+bE3ier7FJxRQ/6eZ/4ew==";

    static boolean verify(byte[] manifest, String signature) { return verify(manifest, signature, PUBLIC_KEY); }

    static boolean verify(byte[] manifest, String signature, String publicKey) {
        if (manifest == null || signature == null || publicKey == null) return false;
        try {
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.trim())));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(key);
            s.update(manifest);
            return s.verify(Base64.getMimeDecoder().decode(signature.trim().getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception anythingWrong) {
            return false;
        }
    }
}
