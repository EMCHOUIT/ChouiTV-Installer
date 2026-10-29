package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

public class TrustTest {
    private static final String SHA = "fab082f66cb9c8f192da04639a67936e7c07226ed8ec3789aa66cf0f5c6b36cb";
    private static final String OK = Trust.RELEASES + "v3.177/chouitv-home.apk";

    private static void refused(String pkg, String url, String sha) {
        try { Trust.requireApk(pkg, url, sha); fail("accepted " + pkg + " " + url + " " + sha); } catch (IOException expected) { }
    }

    @Test public void aNormalLineIsAccepted() throws Exception {
        Trust.requireApk(Flow.HOME, OK, SHA);
        Trust.requireApk(Flow.SAVER, Trust.RELEASES + "v3.177/chouitv-screensaver.apk", SHA.toUpperCase());
    }

    @Test public void aChecksumIsMandatory() {
        refused(Flow.HOME, OK, "");
        refused(Flow.HOME, OK, null);
        refused(Flow.HOME, OK, SHA.substring(1));
        refused(Flow.HOME, OK, SHA.replace('f', 'z'));
    }

    @Test public void onlyOurReleasesAreAllowed() {
        refused(Flow.HOME, "https://evil.example/chouitv-home.apk", SHA);
        refused(Flow.HOME, "http://github.com/EMCHOUIT/ChouiTV/releases/download/v3.177/a.apk", SHA);
        refused(Flow.HOME, "https://github.com/EMCHOUIT/other/releases/download/v1/a.apk", SHA);
        refused(Flow.HOME, "https://github.com/EMCHOUIT/ChouiTV/releases/download/../../../evil/a.apk", SHA);
        refused(Flow.HOME, Trust.RELEASES + "v3.177/a.apk?x=1", SHA);
        refused(Flow.HOME, Trust.RELEASES + "x@evil.example/a.apk", SHA);
        refused(Flow.HOME, Trust.RELEASES, SHA);
        refused(Flow.HOME, null, SHA);
    }

    @Test public void onlyOurTwoAppsAreListed() {
        refused("com.majnioui.tv.channels", OK, SHA);   // Channels has its own fixed address
        refused("com.evil.app", OK, SHA);
        refused(null, OK, SHA);
    }

    @Test public void theVersionFileIsChecked() throws Exception {
        Trust.requireVersion(177, "3.177");
        try { Trust.requireVersion(Trust.MIN_VERSION_CODE - 1, "3.175"); fail("an old file was accepted"); } catch (IOException expected) { }
        try { Trust.requireVersion(177, ""); fail("no name was accepted"); } catch (IOException expected) { }
        try { Trust.requireVersion(177, "3.177; rm -rf"); fail("a strange name was accepted"); } catch (IOException expected) { }
    }

    @Test public void aDownloadedFileMustBeTheRightAppVersionAndKey() throws Exception {
        Trust.requireDownloaded(Flow.HOME, Flow.HOME, 177, 177, Trust.SIGNER_SHA256);
        Trust.requireDownloaded(Flow.HOME, Flow.HOME, 177, 177, Trust.SIGNER_SHA256.toUpperCase());
        try { Trust.requireDownloaded(Flow.HOME, Flow.SAVER, 177, 177, Trust.SIGNER_SHA256); fail("another app was accepted"); } catch (IOException expected) { }
        try { Trust.requireDownloaded(Flow.HOME, Flow.HOME, 177, 176, Trust.SIGNER_SHA256); fail("another version was accepted"); } catch (IOException expected) { }
        try { Trust.requireDownloaded(Flow.HOME, Flow.HOME, 177, 177, SHA); fail("another key was accepted"); } catch (IOException expected) { }
        try { Trust.requireDownloaded(Flow.HOME, Flow.HOME, 177, 177, null); fail("no key was accepted"); } catch (IOException expected) { }
    }

    @Test public void channelsUsesTheVersionFilesChecksumOrTheOneItKnows() {
        assertEquals(SHA, Trust.channelsSha(SHA));
        assertEquals(Trust.CHANNELS_SHA256, Trust.channelsSha(""));
        assertEquals(Trust.CHANNELS_SHA256, Trust.channelsSha("not a hash"));
        assertTrue(Trust.isSha256(Trust.CHANNELS_SHA256));
        assertFalse(Trust.isSha256(null));
    }
}
