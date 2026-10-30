# S1 emulator prototype

This is a debug-only trust configuration for a generic extension broker and a
separately installed test harness. It uses no donor code, CXR or Rokid SDK.
Architecture dependency: [PR #2](https://github.com/SergeySdv/hermes-mobile/pull/2),
ADR-003 and `specs/03-CONTRACTS.md` at `d044d1c7`. Historical G0 remains BLOCKED;
ADR-003 remains PROPOSED. See [limited authorization](SCOPE.md).

## Build

Use JDK 21 and the existing Android SDK. The ordinary app builds without the
harness: `./gradlew :app:assembleDebug :app:compileReleaseKotlin`.
The contract library is local and has no vendor dependencies. No extra Maven
repository is introduced. The harness is included only with `-Ps1Harness=true`.

Create a dedicated local **test** key (never commit it), then obtain its SHA-256
certificate digest with `keytool -list -v`. Supply the digest without colons:

```sh
keytool -genkeypair -keystore /absolute/test-harness.jks -alias s1 \
  -storepass android -keypass android -keyalg RSA -validity 30 -dname CN=S1-Test
./gradlew -Ps1Harness=true \
  -Ps1HarnessKeystore=/absolute/test-harness.jks \
  -Ps1HarnessSigner=YOUR_64_HEX_CERTIFICATE_SHA256 \
  :app:assembleDebug :extension-test-addon:assembleDebug \
  :app:assembleDebugAndroidTest :extension-test-addon:assembleDebugAndroidTest
```

`CORE_SIGNER` in the harness pins this checkout's existing Hermes debug signer.
If using a different local debug certificate, deliberately update that test pin
and verify both APK certificates; do not remove signature verification.
The Hermes release variant has an empty test signer and rejects this harness.
The harness has no INTERNET permission, backend URL, auth storage or history.

## Run on the authorized emulator

Only `emulator-5554` (Android 16 / API 36) was authorized for this run. Check
`adb -s emulator-5554 shell getprop sys.boot_completed` before installing.
Install the two debug APKs from their modules' `build/outputs/apk/debug/`.
Run `python3 scripts/s1/mock_gateway.py` on the host, then:

```sh
adb -s emulator-5554 reverse tcp:19119 tcp:19119
```

In the existing Hermes connection/login UI, connect to
`http://127.0.0.1:19119`. The fixture supplies a synthetic token through the
ordinary login path. Enable the existing Session Integrations → Stay connected
in background option so the current conversation stays connected while the
harness is foreground. No app data clearing or auth bypass is needed.

Run `python3 scripts/s1/ui_smoke.py`. `S1_EVIDENCE=/absolute/new-directory`
selects a new output directory. The script uses real UI controls, a generated
JPEG file, Binder between separate packages, existing composer and Send.
Host counters assert zero uploads/prompts before Send and exactly one of each
after Send. The answer explicitly says no real model was called.

The fixture is loopback-only and is test tooling, not an Android transport or
production backend. Do not expose it or point this run at a production VPS.
No Studio delegation was needed: adb and the emulator were used directly.

## IPC behavior and limits

`extension-contract` contains AIDL with fixed transaction IDs and protocol 1.0.
Every service method checks kernel UID, a single-package UID, Android user,
package identity and a pinned single signing certificate. An explicit component
bind alone is not authorization. The handshake intersects capability bits:
`1` draft write, `2` assistant display. It returns limits, never credentials.

`openSession` requires an existing connected conversation. Hermes displays its
name, extension package, signer and separate capability choices. Approval issues
opaque `grant` and process `epoch` values for 15 minutes of elapsed time. All
operations recheck caller, grant, epoch, auth scope and captured conversation /
profile generation. Revocation, expiry, context change and callback death clear
access; there is no renewal, server submission or context switching API.

`stageInput` accepts UTF-8 text (16 KiB) and an optional read-only regular JPEG FD
(5 MiB / 20 megapixels). Metadata is limited to 64 KiB; at most eight distinct
operations and one in-flight import per grant. Media is copied off main into
private cache, validated and passed unchanged to the existing attachment path.
Pipes, writable FDs, malformed JPEG and over-limit media are rejected. No hidden
resize/recompression occurs. JPEG staging currently requires API 30 or newer to
validate descriptor access through a public Android API. Negotiation reports
`minJpegApi=30` and `jpegSupported`; older devices reject photo input with
`INVALID_MEDIA`, while text and the ordinary Hermes client remain available.
This older-device behavior has not been emulator-tested.

A staging call initially returns `UNKNOWN`; the callback reports
`AWAITING_USER` only after the existing composer accepts the draft. Repeating
an operation with identical text/media returns its receipt; different content
returns `PAYLOAD_CONFLICT`. Querying status does not submit anything. Explicit
Send remains in Hermes. After confirmation the staging receipt becomes UNKNOWN;
it does not promise backend idempotency or cause automatic retries.

Display contains only bounded new live assistant text after explicit Send,
excluding pre-grant rows and REST-hydrated history, tool/approval/clarification
fields and reasoning. It is not a history API. The harness clears its volatile
display on revocation, expiry or service disconnection.

## Review limitations

This is UI + IPC smoke with a synthetic backend. Authorized real-backend
round-trip is NOT_RUN. **Rokid hardware not tested.** No production extension
trust enrollment or release approval is supplied. The test harness uses native
Android widgets and synthetic media, not a second chat interface.

Review slow-callback/flood behavior and cache retention before broadening the
prototype. Projection currently covers live streaming assistant rows only;
REST-only completion and complex assistant/tool flows require further tests.
Full instrumentation regression and CI are separate from the targeted emulator
checks. APKs, keystores and personal data must not be committed as evidence.
