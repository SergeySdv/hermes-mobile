package com.m57.hermes.extension;

/** IPC v1.0. Transaction IDs are explicit in AIDL; additive response keys only. */
public final class ExtensionProtocol {
    private ExtensionProtocol() {}
    public static final int MAJOR = 1;
    public static final int MINOR = 0;
    public static final int DRAFT_WRITE = 1;
    public static final int ASSISTANT_DISPLAY = 2;
    public static final int CAPABILITIES = 3;
    public static final int MAX_TEXT_BYTES = 16 * 1024;
    public static final int MAX_JPEG_BYTES = 5 * 1024 * 1024;
    public static final long MAX_PIXELS = 20_000_000L;
    public static final long LEASE_MS = 15 * 60 * 1000L;
}
