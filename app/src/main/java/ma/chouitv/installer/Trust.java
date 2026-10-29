package ma.chouitv.installer;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What the installer will and will not put on a TV. The apps come from one place (the ChouiTV releases on GitHub), each file must match
 * the SHA-256 the version file publishes, be the version that file announces, and be signed with the key ChouiTV is signed with.
 * No Android here, so the rules are tested on the JVM (TrustTest).
 */
final class Trust {
    private Trust() {}

    static final String RELEASES = "https://github.com/EMCHOUIT/ChouiTV/releases/download/";

    /**
     * SHA-256 of the certificate ChouiTV's two apps are signed with (the same line as .github/signer-sha256.txt in the ChouiTV repository).
     * An app signed by anything else is not ours, whatever its name and checksum say.
     */
    static final String SIGNER_SHA256 = "af6154f7763c8a894b235341d3c6e1df34536b3f7a11b1244cd2aaa96451f242";

    /** The Channels app is somebody else's build kept as it is; its checksum is pinned here when the version file does not carry one. */
    static final String CHANNELS_SHA256 = "af6502e7450326b1e1c02e01e3c01340e90b1742b3c127b4eac0ea6f1b46d638";

    /** An older version file than this is a replay: it is refused, so a TV is never taken back to a version with a known problem. */
    static final int MIN_VERSION_CODE = 176;

    static final int MAX_MANIFEST_BYTES = 64 * 1024;
    static final long MAX_APK_BYTES = 150L * 1024 * 1024;

    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern VERSION_NAME = Pattern.compile("\\d{1,3}\\.\\d{1,4}");

    static boolean isSha256(String s) { return s != null && SHA256.matcher(s).matches(); }

    static boolean isVersionName(String s) { return s != null && VERSION_NAME.matcher(s).matches(); }

    /** Only a file of the ChouiTV releases, over https, with nothing in the address that could lead elsewhere. */
    static boolean allowedUrl(String url) {
        if (url == null || !url.startsWith(RELEASES)) return false;
        String rest = url.substring(RELEASES.length());
        return !rest.isEmpty() && !rest.contains("..") && !rest.contains("@") && !rest.contains("?") && !rest.contains("#")
                && !rest.contains("\\") && !rest.contains("%2e") && !rest.contains("%2E");
    }

    /** One APK line of the version file: one of our two apps, from our releases, with a checksum. */
    static void requireApk(String pkg, String url, String sha) throws IOException {
        if (!Flow.SAVER.equals(pkg) && !Flow.HOME.equals(pkg)) throw new IOException("the version file lists an unknown app: " + pkg);
        if (!allowedUrl(url)) throw new IOException("the version file points outside the ChouiTV releases: " + pkg);
        if (!isSha256(sha)) throw new IOException("the version file has no checksum for " + pkg);
    }

    /** The version file as a whole. */
    static void requireVersion(int code, String name) throws IOException {
        if (code < MIN_VERSION_CODE) throw new IOException("the version file is older than the installer allows (" + code + ")");
        if (!isVersionName(name)) throw new IOException("the version file has no valid version name");
    }

    /** What was read from the downloaded file itself: its package, its version, and the SHA-256 of its signing certificate. */
    static void requireDownloaded(String pkg, String gotPkg, int wantedCode, int gotCode, String signerSha256) throws IOException {
        if (!pkg.equals(gotPkg)) throw new IOException("the downloaded file is not " + pkg);
        if (wantedCode != gotCode) throw new IOException(pkg + ": the file is version " + gotCode + ", the version file says " + wantedCode);
        if (signerSha256 == null || !SIGNER_SHA256.equals(signerSha256.toLowerCase(Locale.ROOT))) {
            throw new IOException(pkg + ": the file is not signed with the ChouiTV key");
        }
    }

    /** The checksum the Channels APK must have: the version file's when it has a valid one, the pinned one otherwise. */
    static String channelsSha(String fromManifest) { return isSha256(fromManifest) ? fromManifest : CHANNELS_SHA256; }
}
