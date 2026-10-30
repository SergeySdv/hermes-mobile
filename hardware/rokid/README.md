# S3 CXR-L / CXR-S hardware probe

First S3 checkpoint, **not completed S3**: authorize through installed Hi Rokid Global,
connect both CXR and Bluetooth, query/install/start the bundled companion, send a
unique test string, receive a view-update acknowledgement, separately confirm
physical visibility. JPEG capture and Hermes IPC wiring are intentionally pending
the requested successful text checkpoint. No new server transport exists here.

This standalone Gradle project is not included by Hermes settings. Hermes, its
SDK repositories, S1/S2 test add-on and backend on 19120 are unchanged. Temporary
platform-widget diagnostic screens deliberately do not import HermesScaffold or
Hermes's app/theme module; they are not new Hermes chat screens. UI/accessibility
and SDK compatibility on actual glasses remain NOT_RUN until installation approval.

## Dependencies and provenance

Base: `58510a67fd90fd04ac744edb3324d527747ab97e`, PR #4 (`feat/s2-real-backend`).
PR #5 is separate physical-phone S2 evidence, not the source of these changes.
Historical G0 and ADR state are unchanged.

The API sequence follows the [official CXR-L documentation](https://t.rokid.com/7w2d7a5l)
and paired official examples (API usage only; no donor chat/UI copied):

- [CXR-L sample 1.0.3](https://rokid-ota.oss-cn-hangzhou.aliyuncs.com/toB/Document/CXR-L/v1.0.3/cxrlsampe.zip)
- [CXR-S companion sample](https://rokid-ota.oss-cn-hangzhou.aliyuncs.com/toB/Document/CXR-L/v1.0.3/cxrssample.zip)
- Phone: `com.rokid.cxr:client-l:1.0.3`; its transitive bridge is
  `1.0-20260522.063600-105`.
- Glasses: `com.rokid.cxr:cxr-service-bridge:1.0-20260417.063502-103`, as pinned in
  that companion example. The different bridge versions are explicit; sample
  pairing is not proof of firmware compatibility.
- Hi Rokid observed on OnePlus: `com.rokid.sprite.global.aiapp`,
  `G1.15.6.1012`, versionCode `10150006`. Only Global authorization selected;
  no compatibility assumption from the CN product's name/version.
- Paired device alias: `Glasses_1006`; exact hardware model/ABI/API remain
  unverified. Hi Rokid System page observed current firmware
  `1.27.009-20261014-150203`, auto update Off. Offered OTA
  `1.28.016-20261029-158201` was NOT installed.

## Build and audit

Use JDK 21 and Android SDK 37; from repository root:

```sh
./gradlew -p hardware/rokid :addon:assembleDebug :companion:assembleDebug \
  :addon:testDebugUnitTest :addon:lintDebug :companion:lintDebug
python3 hardware/rokid/tools/audit_apks.py
```

`ANDROID_HOME` must point to SDK with build-tools 37.0.0; `JAVA_HOME` to JDK 21.
Outputs: `{addon,companion}/build/outputs/apk/debug/*-debug.apk`.
The add-on contains the exact companion APK as an asset. It reads this private
file through the SDK's ParcelFileDescriptor upload, not public storage access.

**SDK logging mitigation:** upstream CXR-L 1.0.3's `connect(token)` writes the
raw token to Android Log. These debug variants use optimized R8 (debuggable=false,
normal local debug signer), preserving Rokid JNI/reflection names and removing
Android logging/Throwable printStackTrace sinks. The audit disassembles every
DEX and fails if output calls or the token-log marker remain. This audits our
APK Java bytecode, not Hi Rokid's independently installed binary or native SDK
implementation. No token values were collected for this check. Release variants
are disabled for this probe. SDK transitive `androidx.test` artifacts are excluded.

## Approved-install checklist (not yet executed)

Before installation, review report APK hashes, signer fingerprints and exact targets.
Both packages have minSdk 31, targetSdk 36, version `0.1-s3-probe` / 1, ARM32/ARM64
native libraries. Both request **no Android manifest permissions**. Authorization
requests **glasses CAMERA permission through Hi Rokid**, for the next JPEG stage;
this build has no capture action. It requests no microphone permission.

1. Phone target: OnePlus Open CPH2551 / `a8bf1333`, Android 16. Check package absent,
   then `adb -s a8bf1333 install <addon-debug.apk>` (no `-r`, no data clearing).
2. Open add-on; authorize through Hi Rokid Global. Consent/denial are separate
   from connection. Press Connect and require CXR=true **and** Bluetooth=true.
3. Query companion on the glasses currently paired in Hi Rokid. If present, stop:
   this probe refuses an overwrite. With owner approval, install the bundled APK.
   This uses CXR-L `appUploadAndInstall`; direct Mac/glasses ADB is not needed.
4. Require install callback success, then Start and require start/resume success.
5. Send test string. Ack proves active companion view was updated, **not** light
   reaching the user's eyes. User reads the unique suffix on the glasses before
   selecting physical visibility confirmation. No ack after 10s is ACK_TIMEOUT.
6. Preserve observed stage/error and sanitized evidence. Do not collect broad
   Hi Rokid logcat or auth intents. Firmware/Hi Rokid/pairing must not be changed.

Removal after separate user action: use “Uninstall S3 companion only” in add-on
(CXR-L appUninstall for `com.m57.hermes.rokid.glasses.debug`), then uninstall phone
package with `adb -s a8bf1333 uninstall com.m57.hermes.rokid.addon.debug`.
Never uninstall Hi Rokid, Hermes or S1/S2 harness. No removal performed here.

## Remaining S3 gates

Installation, authorization, connection, text display, JPEG and full real-agent
cycle are all NOT_RUN. After text succeeds, add explicit JPEG capture/preview and
record received-byte hash, decode/dimensions and actual bytes. Do not claim
end-to-end hash verification without camera-side bytes/hash. Then reuse existing
IPC and its unchanged limits, draft/manual-send behavior and allowed-conversation
scope; oversize images must fail by contract without hidden recompression.
The add-on currently has no Hermes allowlist entry or IPC implementation.
