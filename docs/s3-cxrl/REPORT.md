# S3 CXR-L text checkpoint — installation review

Source commit: `57820d27ef46917207d15d65084c319aebaf01d7`. Parent functional base: PR #4, `58510a67`; PR #5 phone evidence remains separate. No Hermes code or historical evidence changed.

**Overall S3: IN_PROGRESS. Physical installation and runtime checks: NOT_RUN.** JPEG and full IPC cycle await the requested successful text checkpoint. This report does not close G0 or accept ADR-003.

## Validation

| Requirement | Concrete check | Result |
|---|---|---|
| Independent phone/glasses builds | `:addon:assembleDebug`, `:companion:assembleDebug` | PASS |
| Invalid version/frame and UTF-8 size boundary | `TextProtocolTest` accepts 4096 bytes, rejects 4098, wrong version/ID/shape | PASS |
| Late, duplicate, disconnected and timed-out acknowledgement | Five receipt/state tests including distinct user confirmation | PASS; total 8 tests, 0 failed/skipped |
| Static Android checks | Both `lintDebug` tasks | PASS, warnings below |
| Source style | ktlint 1.8.0 standalone on this isolated project | PASS; not a claim about root CI |
| No packaged token-log sink | `tools/audit_apks.py`, all DEX disassembly | PASS: no Android logging output, printStackTrace or PrintStream calls; token log marker absent |
| Embedded companion identity | Exact byte comparison with standalone APK | PASS |
| APK signatures and permissions | apksigner verify / aapt dump | PASS; Android permissions empty |
| Hi Rokid auth / both links / install / start / visible text | Physical run after owner approval | NOT_RUN |
| Real JPEG / manual agent send / answer on glasses | Subsequent S3 checkpoints | NOT_RUN, not yet implemented |

Validation first executed tasks and then repeated the command at the committed revision using the same Gradle cache (up-to-date reuse, not an independent clean build). Both runs exited 0. XML evidence records the actual test execution. SDK logging removal required optimized R8: these are **debug-signed, non-debuggable debug variants**, not production releases. SDK-native and installed Hi Rokid logging are outside the bytecode audit. No authorization was attempted and no token was captured.

Lint warnings are retained, not suppressed: target API 36 vs newest 37, missing probe launcher icon, hardcoded diagnostic strings, resource shrinking not enabled; companion newer-SDK notice is intentional pinning to the official sample. Data backup/transfer is explicitly excluded. Native SDK strip-symbol warnings do not establish ABI compatibility.

## Installation proposal — approval required

- Phone: OnePlus Open CPH2551 / `a8bf1333`, Android 16. USB ADB install without `-r` after checking package absence.
- Glasses: currently paired `Glasses_1006`, observed firmware `1.27.009-20261014-150203`. Exact hardware model, Android API and ABI remain unverified. Install candidate via Hi Rokid Global CXR-L, not direct Mac ADB; stop on incompatibility, do not update firmware.
- Hi Rokid Global: `com.rokid.sprite.global.aiapp`, `G1.15.6.1012` / `10150006`. No assumption of CN compatibility.
- Both artifacts: version `0.1-s3-probe` / 1; min API 31, target 36; ARM64/ARM32. No Android manifest permissions. Phone authorization separately requests glasses CAMERA through Hi Rokid; no microphone.
- Existing companion package causes an installation refusal. Stock apps, pairing and S1/S2 packages are unchanged. Backend remains 19120.
- Removal: add-on confirmation button calls appUninstall only for this companion; phone removal uses `adb -s a8bf1333 uninstall com.m57.hermes.rokid.addon.debug`. Neither has been executed.

| Artifact | Package ID | SHA-256 |
|---|---|---|
| addon-debug.apk | `com.m57.hermes.rokid.addon.debug` | `c2dbe0211c8d27d95cafdb9c59e8a013e0a08db9baa50a57fb2599378aa318c8` |
| companion-debug.apk | `com.m57.hermes.rokid.glasses.debug` | `de30616050690023c1ca26723b2509a0e1a67fe4451299e60b8fd64d81f12eb0` |

Both signer certificate SHA-256: `be7558a33ba6253842b260a780f3c1977f563df12cf00ce5576b46fdb3e1b021` (local Android Debug certificate).

Local artifact directory: `/Users/sergei/Downloads/hermes-s3-57820d27/`. Neither APK installed.

## Evidence and next action

- [APK audit](evidence/57820d27/apk-audit.json)
- [Executed validation](evidence/57820d27/gradle-executed.log) and [committed/cache validation](evidence/57820d27/gradle-committed.log)
- [Unit test XML](evidence/57820d27/unit-tests.xml)
- [Phone lint](evidence/57820d27/addon-lint.xml), [companion lint](evidence/57820d27/companion-lint.xml), [style output](evidence/57820d27/ktlint.log)
- [Build/provenance/install instructions](../../hardware/rokid/README.md)

After approval, install these exact APKs and record each hardware stage independently. A view-update ack is not physical display proof. If text succeeds, implement real JPEG capture/preview and then existing IPC; unchanged IPC image limits, no hidden compression, no new chat/auth/backend client.

## Physical phone attempt and S3-AUTH-01 fix

Owner approved original pair. USB install of original add-on on OnePlus user 0
exited 0 (Success), package previously absent; activity launched. No companion
installation or firmware change occurred. Authorization button showed a false
negative before calling Hi Rokid. UI evidence:
[precondition failure](evidence/device-first-install/authorization-precondition.xml).

Root cause in our code: `isConnectHiRokid()` initially returns false and is set by
`requestAuthorization()`, not `isRequiredHiRokidInstalled()`. Verified against
SDK 1.0.3 bytecode. Global installed versionCode 10150006 exceeds the SDK's
Global threshold 10070014; this does not prove full hardware compatibility.

Fix source commit `4f7dc0bb`: preflight requires Global present and compatible CN
absent (SDK otherwise prioritizes CN); selected Global route checked after auth
result. No update to Hi Rokid required. Build/unit task/lint exited 0; tests used
cache, APK audit re-executed PASS. New runtime validation pending installation.

Replacement add-on: `/Users/sergei/Downloads/hermes-s3-4f7dc0bb/addon-debug.apk`.
SHA-256 `f8b61e245bb810572be05dae5c6ab8b59de53a88564914de71fd2a85261da685`.
Same package, version, certificate and permissions. Companion bytes unchanged.
Replacement installation pending approval because prior approval named exact APKs.
[Updated audit](evidence/device-first-install/fixed-apk-audit.json),
[build log](evidence/device-first-install/fixed-build.log).
