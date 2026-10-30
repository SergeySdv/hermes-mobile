# S3 — physical phone baseline; glasses integration pending

Status: **phone repetition PASS; glasses camera/display/link NOT_RUN**.
This is an incremental S3 checkpoint, not completion of hardware integration.
Branch `feat/s3-rokid-hardware` depends on S2 PR #4 at `58510a67`.
Historical G0 remains BLOCKED. No merge or production change.

## OnePlus run, 2026-10-30

Owner authorized USB ADB installation of the already tested S2 APK pair on
OnePlus Open CPH2551, serial `a8bf1333`, Android 16 / API 36. Installed packages
were pulled back and hashed: [APK provenance](evidence/oneplus/installed-apks.json).
APK source is `ea8ffa69`; S2/S3 documentation commits do not rebuild these binaries.
Core signer: `c3053abf8d6f71fea74c0f2c9ae7dc693a6a08e3fa8b1a09c3763a52495f49f0`.
Harness signer: `0b9cc195e8bf7f6893e29450ea52f0a33d1f09f4dccb62d591613ae8731ee9ab`.
No existing package was removed or data cleared.

Backend is the unchanged isolated local S2 container on Mac loopback 19120,
server source `4d04303818b61bf3aa09278af48356315515965b`, OpenAI `gpt-4.1-mini`.
Device-specific `adb -s a8bf1333 reverse tcp:19120 tcp:19120` connects the phone.
The mock service on 19119 remains separate and was not used for this run.

| Requirement | Concrete check | Observed result |
|---|---|---|
| Ordinary login | Auth Login to `http://127.0.0.1:19120/`, normal test username/password | PASS: real conversation created; no credential injection or mock response |
| Scoped IPC consent | [Consent](evidence/oneplus/01-consent.png), matching XML | PASS: draft and display permissions explicitly selected |
| JPEG/text to existing composer | Synthetic 320×240 JPEG, Stage, Open Hermes; [composer](evidence/oneplus/02-composer.png) | PASS: exactly one attachment asserted; Send tapped in Hermes |
| Real image response | [Core](evidence/oneplus/03-hermes-image-answer.png), [add-on](evidence/oneplus/04-addon-image-answer.png) | PASS: exact matching response describes blue rectangle and gray background |
| Continuation | Existing composer, manual Send; [core](evidence/oneplus/05-hermes-follow-up.png), [add-on](evidence/oneplus/06-addon-follow-up.png) | PASS: both show `I saw a blue rectangle in that image.` |
| Reopen history | [History](evidence/oneplus/07-history.png), [reopened](evidence/oneplus/08-reopened-history.png), [assertions](evidence/oneplus/history-check.json) | PASS: same conversation, final response and JPEG accessibility description present |
| Real server persistence | Read-only local SQLite query, [metadata](evidence/oneplus/server-session.json) | PASS: session `20261030_045219_bfc25d`, 4 messages, 2 agent API calls, 0 tool calls, OpenAI endpoint |

The first input was `Describe the synthetic S1 photograph.`; the follow-up was
`What shape and color did you see in that image? Answer in one sentence. Do not use tools.`
The image contains a blue rectangle on gray. Image receipt evidence is behavioral,
not a captured provider request body; see the [S2 audit](../s2/REPORT.md).

## Observed setup issues and recovery

- Phone relocked during the first consent attempt. Owner unlocked it; USB
  stay-awake was temporarily enabled and its previous value restored after
  the completed run. No lock bypass was attempted.
- Fresh install had **Stay connected in background** off. A request while the
  core was backgrounded returned `CONVERSATION_UNAVAILABLE`. Enabling the
  existing Behavior setting through UI allowed normal consent and response
  projection. This setting remains enabled in the test app for S3.
- The History drawer retained its previous scroll position; the first lookup
  timed out, then scrolling to the existing History entry completed the check.
- OnePlus screencap prefixes its PNG stream with a multiple-display warning.
  Original PNG payloads were extracted without pixel changes and every PNG
  chunk CRC validated. Prefixes are retained in
  [capture diagnostics](evidence/oneplus/screencap-diagnostics.json). The add-on
  screenshot was visually inspected and agrees with its XML hierarchy.

All screenshots have sibling XML hierarchies. No Android Studio delegation was
needed. No application code changed for this phone repetition.

## Remaining S3 work

Rokid model was reported by the owner as **1006**, but no glasses ADB identity,
Android/API level, camera or display capability has been verified. Camera capture,
Bluetooth JPEG transfer, final display and repeated hardware cycles remain
NOT_RUN. Minimal companion/add-on builds are not yet prepared at this checkpoint.
No SDK compatibility is inferred from the device name. Obtain read-only device
identity/capability evidence, prepare the minimal components, then present exact
packages/signers/APKs for separate glasses installation approval. The S1/S2 test
add-on remains available for regressions.
