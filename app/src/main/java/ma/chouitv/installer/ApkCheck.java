package ma.chouitv.installer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

/** Reads a downloaded APK with Android's own parser and holds it to Trust's rules before a single byte goes to the TV. */
final class ApkCheck {
    private ApkCheck() {}

    @SuppressWarnings("deprecation")
    static void verify(Context c, File apk, String pkg, int wantedCode) throws IOException {
        PackageManager pm = c.getPackageManager();
        PackageInfo pi;
        if (Build.VERSION.SDK_INT >= 28) pi = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNING_CERTIFICATES);
        else pi = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNATURES);
        if (pi == null) { apk.delete(); throw new IOException(pkg + ": the downloaded file is not an app"); }
        Signature[] sigs = null;
        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo si = pi.signingInfo;
            if (si != null) sigs = si.hasMultipleSigners() ? si.getApkContentsSigners() : si.getSigningCertificateHistory();
        } else sigs = pi.signatures;
        if (sigs == null || sigs.length == 0) {   // some phones do not fill signingInfo for a file that is not installed: the older field is asked
            PackageInfo old = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNATURES);
            if (old != null) sigs = old.signatures;
        }
        // ChouiTV is signed by one key: a file with several signers, or none, is not ours
        String signer = sigs != null && sigs.length == 1 ? sha256(sigs[0].toByteArray()) : null;
        try {
            Trust.requireDownloaded(pkg, pi.packageName, wantedCode, pi.versionCode, signer);
        } catch (IOException refused) {
            apk.delete();
            throw refused;
        }
    }

    private static String sha256(byte[] b) throws IOException {
        try {
            StringBuilder h = new StringBuilder();
            for (byte x : MessageDigest.getInstance("SHA-256").digest(b)) h.append(String.format("%02x", x));
            return h.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }
}
