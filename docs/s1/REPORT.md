# S1 emulator review evidence — 2026-10-30

Status: functional prototype, Draft PR review requested. This is **UI + IPC
smoke with a synthetic backend**, not a production or hardware gate result.
Historical G0 remains BLOCKED and its evidence is unchanged. ADR-003 remains
PROPOSED; dependency on architecture PR #2 is documented in [README](README.md).

## Tested implementation

Final source revision: `ea8ffa69` (Hermes embedded GIT_SHA `ea8ffa6`).
`e39d603c` introduced the implementation; `5b262b1e` fixed repeated Connect and
UI readiness waits; `ea8ffa69` returns LIMIT_EXCEEDED for oversized metadata
from a trusted caller before unmarshalling it. The add-on APK bytes are unchanged
from `5b262b1e`; its final instrumentation tests were rebuilt at `ea8ffa69`.

Environment: JDK 21.0.13, local Android SDK, `emulator-5554`,
`Medium_Phone_API_36`, Android 16 / API 36. No new system image was needed for
these checks. Installation and UI actions used adb directly; no Studio task was
delegated. Only synthetic data and a loopback host fixture were used.

Debug APKs are available locally in
`/Users/sergei/Downloads/hermes-s1-ea8ffa69/`:

| APK | Bytes | SHA-256 |
| --- | ---: | --- |
| `hermes-s1-debug.apk` | 44705709 | `270f3ab6245c990c8a95a0a062b74e4ce551d0c379bdfe5ebeb78806773d75b1` |
| `extension-test-addon-debug.apk` | 969936 | `6e3f8f97e5587379ce77c827cc2dc1c40d009903fd333bfcd4e6fb54cafb99c8` |

Hashes/build revisions are also in [apk-hashes.json](evidence/apk-hashes.json).
[Installed package hashes](evidence/installed-apk-hashes.json) match both delivered APKs.
APKs and signing keys are not committed. The directory contains the full JUnit
XML archive. Hermes debug signer: `c3053abf8d6f71fea74c0f2c9ae7dc693a6a08e3fa8b1a09c3763a52495f49f0`.
Dedicated harness test signer: `0b9cc195e8bf7f6893e29450ea52f0a33d1f09f4dccb62d591613ae8731ee9ab`.
These are test identities, not production trust defaults.

## Results

| Check | Result | Evidence / qualification |
| --- | --- | --- |
| Existing unit suite | PASS | 4085 tests, 361 suites, zero failure/error/skip; [summary](evidence/unit-summary.json), [run](evidence/regression-build.log) |
| ktlint + color guard | PASS | [Final core validation](evidence/core-final-build.log) |
| Debug APKs + test APKs | PASS | Core validation and [final harness build](evidence/addon-final-build.log) |
| Release Kotlin compilation | PASS | Core validation; no test-only dependency referenced from main |
| Android lint | PASS with warnings | Zero errors; [counts](evidence/lint-summary.json), build logs. Not a warning-free claim |
| Ordinary Hermes without opt-in harness module | PASS | [Independent build](evidence/independent-core-build.log): assembleDebug + compileReleaseKotlin, no S1 properties |
| JPEG descriptor/content/bounds | PASS | Four tests: byte preservation, writable FD/pipe rejection, 5 MiB+1 and malformed rejection, exact 20 MP acceptance / above-limit rejection |
| Grant state boundaries | PASS | Instrumented broker test: UID/epoch mismatch, dedup/conflict, text bound, expired grant, conversation generation change; [5 core tests](evidence/core-instrumentation.txt) |
| Actual cross-package Binder | PASS | [Harness instrumentation](evidence/addon-instrumentation.txt): negotiation, invalid grant/version, oversized metadata, no credential/session keys |
| Photo/text → draft → Send → both answers | PASS | UI screenshots, hierarchy and request counters in [smoke evidence](evidence/smoke-final/counts.json); one attachment despite duplicate operation |
| No send before user confirmation | PASS | Upload and prompt counters unchanged during staging; exactly +1 each after Send |
| Grant revoke / incompatible version | PASS | Old grant refuses staging, answer cleared; no additional backend requests |
| Add-on stopped, disabled, removed | PASS | [Failure run](evidence/addon-failures.log); ordinary text send succeeds once in each state, no upload. Stop begins with a live grant; revoke control disappears |
| Wrong signing certificate | PASS | [Actual APK rejection](evidence/wrong-signer.log); SecurityException at Binder, unchanged backend counters; trusted APK restored |
| Authorized real-backend round-trip | NOT_RUN | Fixture does not call a model or production VPS |
| Rokid hardware | NOT_RUN | **Rokid hardware not tested** |
| Full existing UI instrumentation suite / remote CI | NOT_RUN locally | Targeted instrumentation and scripted UI checks only; remote CI is separate |

The full existing unit suite ran on the implementation worktree before the final
current-signer and metadata guards were committed; those guards were then built,
linted and exercised by the final targeted instrumentation. It was not rerun
solely for the harness reconnect/UI-script change. Gradle used its existing cache;
these are not clean-machine/reproducible-build claims.

Final photo/draft/send smoke and all six targeted instrumentation tests use
`ea8ffa69`. Package stop/disable/removal and actual wrong-certificate UI checks
use core `e39d603c` + harness `5b262b1e`; the later core change only alters the
oversized-metadata reply. Independent module-free build also used `e39d603c`;
the module/dependency graph did not change afterward.

## Observed failures and fixes

- Public `Os.fcntlInt` requires API 30. The main APK remains minSdk 26; JPEG
  staging now explicitly reports platform support and rejects unsupported media
  on older devices. No suppression, SDK increase or quality reduction was used.
- Oversized metadata originally threw a generic exception. The final guard now
  preserves the v1 LIMIT_EXCEEDED receipt for trusted callers; a real Binder
  instrumentation assertion verifies it. Wrong signers still fail closed.
- A cold start after instrumentation exceeded a fixed one-second UI delay.
  The script now waits for the actual enabled control. The first attempt is
  retained locally at `/tmp/hermes-s1-evidence/e39d603c-smoke.log`.
- Repeated Connect in an already bound harness left a misleading CONNECTING
  label. The harness now negotiates again; the repeated-connection failure log
  is retained locally at `/tmp/hermes-s1-evidence/e39d603c-failures.log`.
- Earlier fixture corrections supplied the existing attachment ACK shape,
  unique session IDs and WebSocket pong. Hermes transport was not changed to
  accommodate the fixture.

## Limits and next review

The fixture intentionally lacks durable server history and reconciliation.
Hermes may show an accepted/pending receipt while displaying the streamed answer;
this smoke does not prove durable delivery, server idempotency or history recovery.
No automatic retry was added. The add-on receives a bounded live assistant
projection, not history, credentials, server/session identifiers or tool output.

Expiry is forced through test-only reflection on the existing grant's deadline;
this tests rejection without a 15-minute wait or a production renewal API.
Conversation-change is a broker state test with a mocked composer target;
the positive staging and signature tests use actual cross-package Binder.
The size test uses a 5 MiB+1 sparse fixture and a valid 20 MP JPEG; it does not
claim to exercise physical glasses transport or every boundary permutation.

Remaining production-review work includes slow callback/flood behavior,
package replacement and cross-user/handle-forwarding adversarial tests, scope
change during media copy, logout UI, cache retention and REST-only response
projection. Review these before widening trust or accepting production gates.
Tests below API 30, real photos, personal phones, companion/SDK integration and
production VPS were outside this run. No merge or release was performed.

The next change should address review findings in this generic IPC slice, then
introduce the separately scoped Rokid adapter only after its own authorization
and gates. Do not import the donor's phone chat/history/network stack.

## Review requirement → test → observed result

The original evidence above is retained. This follow-up adds only the previously
missing deterministic scope-race test and a compiled-release isolation check.
No application source changed. New tests at `52e245f6` run against the installed
Hermes APK from `ea8ffa69`; [provenance](evidence/review/provenance.json) records
both the test APK and application hashes. Existing UI evidence uses the APK pair
in [apk-hashes.json](evidence/apk-hashes.json), not a newly asserted run.

| Requirement | Concrete test / evidence | Actual result |
| --- | --- | --- |
| Same input operation does not duplicate a draft | `ExtensionBrokerTest.deduplicationExpiryAndConversationChangeFailClosed`; `scripts/s1/ui_smoke.py` repeat operation and composer assertions | PASS: adapter called once for identical payload, changed payload gives PAYLOAD_CONFLICT; UI has one attachment and one text draft; one upload/prompt after Send |
| Conversation/profile change rejects in-flight and late work | `ExtensionBrokerTest.profileAndConversationSwitchDiscardInFlightDraftAndQueuedDisplay` | PASS for conversation ID, connection profile, agent profile and endpoint independently: pause hashing at IO boundary, queue DISPLAY, switch context, release IO; zero composer calls, REVOKED, no stale display/ACK. [Two broker tests](evidence/review/scope-tests.txt) passed on API 36 |
| Incompatible IPC version | `IpcBoundaryTest.actualBinderNegotiationAndUnissuedGrantAreFailClosed`; UI Test unsupported version | PASS: UNSUPPORTED_VERSION, no additional backend side effects; existing [instrumentation](evidence/addon-instrumentation.txt) and [smoke](evidence/smoke-final.log) |
| Image exceeds IPC limits | `ExtensionPhotoTest.rejectsTransportSizedPhotoOverIpcLimitAndMalformedJpeg`, `acceptsPixelBoundaryAndRejectsOverLimit` | PASS: 5 MiB+1 and >20 MP rejected with LIMIT_EXCEEDED; exact 20 MP accepted. Original bytes preserved by `copiesOriginalBytesAndHashesTextAndMedia`; [four photo tests](evidence/core-instrumentation.txt). No claim of a physical glasses transfer test |
| Mock backend uses normal send/response path | `scripts/s1/ui_smoke.py`, real separate packages/Binder, existing composer Send, host counters and both UI answers; [transport audit](evidence/review/transport-audit.json) | PASS: counters unchanged at staging, +1 upload and +1 prompt only after Send. Existing REST/WebSocket source files unchanged. Mock is an external loopback process reached through the ordinary connection profile, not an app-side fake response |
| No test backend/trust substitute in ordinary release | `scripts/s1/check_release_isolation.py` after `:app:compileReleaseJavaWithJavac`; [compiled result](evidence/review/release-isolation.json), prior [independent build](evidence/independent-core-build.log) | PASS: 5173 release classes scanned; no harness classes/mock markers, DEBUG=false, empty test signer, trusted() compiles to false. Ordinary build excludes optional harness. Structural compiled-class check, not a signed release APK/device test |

The first new scope-race test attempt failed because an unpaused text hash could
finish before the context switch (a legitimately accepted draft). Its [failure
log](evidence/review/first-race-test-failure.txt) is preserved. The corrected test
uses a test-only latch around hashing, without changing production behavior;
this establishes the intended race deterministically. Tests do not replace the
remaining production/adversarial review listed above. No donor baseline wait or
new architecture gate was added to S1/S2.
