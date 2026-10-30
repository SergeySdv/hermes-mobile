package com.m57.hermes.extension;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.security.MessageDigest;
import java.util.Locale;

/** No shared UID, arbitrary package claim, or implicit certificate rotation. */
public final class SignerTrust {
    private SignerTrust() {}
    public static boolean matches(PackageManager pm, int uid, String name, String expected) {
        if (expected == null || expected.length() != 64 || uid / 100000 != android.os.Process.myUid() / 100000) return false;
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null || packages.length != 1 || !name.equals(packages[0])) return false;
        try {
            PackageInfo info = pm.getPackageInfo(name, Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
            if (info.applicationInfo == null || info.applicationInfo.uid != uid) return false;
            android.content.pm.Signature[] current;
            if (Build.VERSION.SDK_INT >= 28) {
                if (info.signingInfo == null || info.signingInfo.hasMultipleSigners()) return false;
                current = info.signingInfo.getApkContentsSigners();
            } else {
                current = info.signatures;
            }
            if (current == null || current.length != 1) return false;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(current[0].toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return expected.equalsIgnoreCase(hex.toString());
        } catch (Exception failure) { return false; }
    }
}
