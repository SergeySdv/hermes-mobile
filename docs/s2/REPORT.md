# S2 — real local Hermes round-trip verified

Status: **local S2 scenario PASS; review pending**. This does not change G0,
which remains BLOCKED, or establish release readiness / remote HTTPS / Rokid
hardware compatibility. No merge performed. PR #4 remains a Draft dependent on
S1 PR #3 (`feat/s1-extension-ipc`); its diff contains only S2 work.

## Tested versions and environment

- Stand implementation commit: `5f4a853f`.
- Unmodified server source: `4d04303818b61bf3aa09278af48356315515965b`, matching
  Android's gateway contract snapshot byte for byte. Contract SHA-256:
  `248ce12519dcc1e830a15c5819c93e6d27ef18404d19da38c960570710f83d00`.
- Android application source: `ea8ffa69`; the installed APK pair was pulled
  back from `emulator-5554` and hashed again. See [provenance](evidence/local/provenance.json).
  No Android code, transport, auth, history, or release configuration changed.
- Docker Desktop 4.49.0 / Engine 28.5.1 / Compose 2.40.2, linux/arm64.
  Mac: 12 CPUs / 64 GiB RAM; Docker: 10 CPUs / approximately 15.6 GiB RAM.
- Container `hermes-s2-local-hermes-1`, separate data/workspace volumes,
  `127.0.0.1:19120` only, adb reverse to Android 16 / API 36 emulator.
  Existing mock profile on 19119 retained separately.
- Model `gpt-4.1-mini`, provider `openai-api`, actual endpoint
  `https://api.openai.com/v1`. Owner entered the API key locally with hidden
  input. Separate random test dashboard credentials; no secrets in git or APK.

[Launch instructions](../../infra/hermes-s2-local/README.md),
[successful build log](evidence/local/build-success.log),
[runtime isolation checks](evidence/local/runtime-validation.json).

## Requirement → concrete check → observed result

| Requirement | Check / evidence | Actual result |
|---|---|---|
| Real Hermes independent of add-on | Ordinary core login and text Send; [UI](evidence/local/ui/01-hermes-direct-answer.png) | PASS: server accepted the request; returned `S2_SERVER_OK` plus initial onboarding greeting. One recorded agent API call, zero tool calls. |
| Matching real interface and auth | Exact contract hash; public `/api/status`; normal username/password login in Android | PASS: basic provider advertised; normal cookie/ticket path reached a new real conversation. No injected session, bypass or mock client. |
| Photo/text → IPC → existing composer | Harness synthetic JPEG, scoped permission, Binder staging; [composer](evidence/local/ui/02-ipc-composer.png) | PASS: exact harmless prompt and one image appeared in existing composer; existing Send was pressed explicitly. |
| Actual image-aware answer in both apps | [Hermes](evidence/local/ui/03-hermes-image-answer.png), [add-on](evidence/local/ui/04-addon-image-answer.png) | PASS: same real model text described a central blue square on a gray background. This verifies transport/display, not model visual accuracy. |
| Continuation without a second chat/transport | Follow-up typed and sent in Hermes; [Hermes](evidence/local/ui/05-hermes-follow-up.png), [add-on](evidence/local/ui/06-addon-follow-up.png) | PASS: both displayed `The blue shape in the image above was a square.` |
| Existing history survives leaving/reopening | Existing History list, same conversation reopened; [list](evidence/local/ui/07-history-list.png), [reopened](evidence/local/ui/08-reopened-history.png), [assertions](evidence/local/history-check.json) | PASS: two user turns, two assistant turns and JPEG representation present. |
| Real server persistence / no external actions | Read-only query of this new container's `state.db`; [metadata](evidence/local/server-sessions.json) | PASS: image conversation has 4 messages, 2 agent API calls, provider `openai-api`, 0 tool calls. Separate direct-text conversation has 2 messages / 1 call / 0 tools. These counters are not a complete billing audit of auxiliary calls. |
| Isolated local runtime | Rendered Compose checks and running-container inspection | PASS: nonroot UID 10000, init, read-only root, dropped capabilities, no-new-privileges configuration, 2 CPUs / 4 GiB / 256 PIDs, bounded logs, only private volumes + read-only secret mounts; loopback binding verified. |
| S1 trust/dedup/limits/regressions | Existing [S1 requirement matrix](../s1/REPORT.md) | Reused existing evidence; not rerun or reclassified as S2 tests. |

Each UI screenshot has a sibling XML hierarchy. History assertions inspect both
text and content descriptions: the JPEG filename is an accessibility description,
not a text node. The first text-only attachment assertion was too narrow; the
saved hierarchy confirms the attachment and the corrected assertion passes.
No pre-send server counter snapshot was taken for S2; the stronger zero-pre-send
counter assertion remains in existing S1 evidence, not a newly claimed S2 result.

## Boundaries and observations

The initial Docker registry requests failed (IPv6 socket/EOF), preserved in
[the failed build log](evidence/local/build-registry-failure.log). The same
Python 3.14.0 linux/arm64 base was downloaded over host IPv4, each manifest/blob
SHA-256 verified, and imported into Docker's cache. [Digests](evidence/local/base-image.json)
and [download result](evidence/local/base-fetch.log) are preserved. No system
network settings or other containers changed. The subsequent frozen dependency
build completed. A cold build still needs working registry access; the verified
base is cached on this Mac.

The minimal headless image intentionally lacks ffmpeg/node/npm/ripgrep; Hermes
reports those optional runtime tools as out of sync. They were not invoked by
these prompts. This image is scoped to the tested chat/image path, not general
agent tool execution. The server reports its version as `unknown` because the
build uses a source archive without `.git`; the exact source SHA and resulting
image ID are recorded independently. The automatically generated conversation
title mentions a gradient, although the synthetic image is blue on gray; no
claim about title/model accuracy is made.

The stand remains running for review. No production VPS, Caddy, Telegram,
OpenViking, existing container, personal device, signing material or historical
G0 evidence was modified. No Android Studio delegation was needed. The failed
`adb root` diagnostic reported a production emulator image; no elevated emulator
access was obtained or needed. No full upstream Hermes test suite was run.

## Preserved earlier evidence

The initial missing-connection blocker is superseded by the owner's authorization
of this isolated local stand. Its original narrative is preserved in
[PREFLIGHT.md](PREFLIGHT.md), with the original preflight JSON/screenshots intact.

## Review audit before S3

The audit started at `acf5245b75c859bb7fe6148a02e7e1c550c8052c`, matching
PR #4, with a clean working tree. Android APK-producing sources and build
configuration have no differences from the tested `ea8ffa69` source. The
published APKs therefore remain the tested binaries; this audit adds evidence
and documentation, not a new Android build.

- Core APK SHA-256: `270f3ab6245c990c8a95a0a062b74e4ce551d0c379bdfe5ebeb78806773d75b1`.
- Test add-on APK SHA-256: `6e3f8f97e5587379ce77c827cc2dc1c40d009903fd333bfcd4e6fb54cafb99c8`.
- Running server image: `sha256:16aba8d5705080b2dee390ecb926cef38ff5fad56e623cd75efe666166bce558`.
- A comparison of all 20,688 tracked regular upstream files against the pinned
  clean server checkout found zero mismatches in the running container:
  [comparison result](evidence/review-audit/server-source-comparison.json).
  `/opt/s2-entrypoint.py` is the separate local configuration/launch wrapper;
  there are no upstream server patches requiring additional function tests.

Image receipt evidence is the staged JPEG, ordinary image-upload/send path,
image-specific real response, subsequent context-aware response, and persisted
conversation metadata linked above. The prompt did not disclose the pictured
colors or shape. This is end-to-end behavioral evidence, **not a captured OpenAI
request body or independent provider-side attestation**. The database counters
alone do not prove that image bytes reached the model. No provider payload or
secret was dumped to strengthen that claim.

The remaining Python terminal (session `25895`, PID `71423` at inspection) is
`mock_gateway.py`, with working directory `/private/tmp/hermes-s1`. It is the
intentionally retained S1 server on port 19119, not a hung one-shot S2 script.
It is still running and therefore has no exit status yet. It was left running;
no unrelated process was stopped. Completed S2 UI runs exited zero; the earlier
attachment assertion failure and its correction remain disclosed above.

Physical-phone repetition belongs to S3 evidence and does not retroactively
change this emulator result. The owner separately authorized installation of
these two exact APKs on OnePlus Open CPH2551 (`a8bf1333`) through USB ADB.
Both installs succeeded and a device-specific reverse for port 19120 was set.
At the initial UI check the phone was locked; hardware UI success is not yet
claimed. Rokid installation remains separately gated by owner approval.
