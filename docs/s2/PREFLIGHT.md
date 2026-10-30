# Historical connection preflight — superseded by the local S2 run

Status: **BLOCKED — real endpoint / existing login context not supplied**.
This is not a failed server connection or an agent response, and is independent
of donor baseline. No mock response is counted as S2 evidence.

Branch `feat/s2-real-backend` is stacked on `feat/s1-extension-ipc` at `823ef34b`
(PR #3, still OPEN). This PR targets that branch, so its diff contains only S2
work. After S1 merges, retarget/rebase S2 onto main without duplicating S1.
No merge is performed by the agent.

## Observed prerequisite

On the authorized `emulator-5554` (Android 16 / API 36), Hermes Settings →
Connection shows **Saved Profiles (1)**. Its only endpoint is
`http://127.0.0.1:19119/`, the S1 host mock. This was checked through the actual
running UI; [screenshot](evidence/connection-profile.png),
[hierarchy](evidence/connection-profile.xml) and
[preflight + installed APK hashes](evidence/preflight.json) are preserved.
No second real connection was available to select. Project baseline inventory
also leaves the real backend version/profile UNKNOWN. No real endpoint was
specified in the current task. Secrets were not read from encrypted preferences.

Missing condition: identify the already agreed real Hermes URL and the existing
connection profile/login method usable from this emulator, or complete normal
Hermes login on this emulator. Do not paste credentials into public reports/PRs.
An asynchronous question was sent to the owner for this information. No answer
was substituted with a guessed host, SSH access or changes to server auth.

Real endpoint connectivity, image/model compatibility, agent round-trip,
follow-up and history reopening are all **NOT_RUN** until that prerequisite is
met. An unavailable configuration does not establish a network/server failure.

## Fixed continuation on the existing client

Production Android source: `ea8ffa69`; APK pair is the unchanged S1 pair:

- Hermes SHA-256: `270f3ab6245c990c8a95a0a062b74e4ce551d0c379bdfe5ebeb78806773d75b1`.
- Test add-on SHA-256: `6e3f8f97e5587379ce77c827cc2dc1c40d009903fd333bfcd4e6fb54cafb99c8`.
- Local files: `/Users/sergei/Downloads/hermes-s1-ea8ffa69/`.

Once the real connection is selected through existing Hermes UI:

1. Use existing Test Connection and record success/error without credentials.
   Confirm the selected profile is the agreed real endpoint, not port 19119.
2. Create one test conversation in Hermes, grant the harness access to that
   conversation, select its synthetic grey/blue JPEG and stage this text:
   “Describe only the visible colors and shapes in this synthetic image.
   Reply in chat only. Do not use tools, modify files or take external actions.”
3. Confirm the actual attachment and text in the existing composer. Until
   explicit Send there must be no agent turn. Press existing Send once.
4. Record the real agent answer in Hermes and the same permitted text in the
   harness. Do not equate a streaming answer with durable storage success.
5. Return to Hermes and manually ask: “Which shape was blue in the image
   above? Answer from this conversation only; do not use tools or take actions.”
   Record the continuation answer in both applications.
6. Leave the conversation and reopen it through existing History. Verify both
   user turns, original attachment representation and both real answers remain
   associated with the same conversation. Inspect existing errors/pending
   receipts rather than inventing history or bypassing reconciliation.
7. Preserve sanitized screenshots, observed turn counts/receipts, selected
   profile identity (no secrets), app/device versions and APK hashes. On failure,
   capture the exact ordinary-client error and the first failing step.

No second chat, history, auth layer, network client or backend shim is required.
The synthetic photo already exists in the harness. adb UI control is available;
no Android Studio delegation was launched or needed. A tool/approval request
would not be approved as part of this harmless test.

No VPS/Caddy/container/auth/Telegram changes, personal-device installation or
hardware action was made. Rokid hardware and production readiness remain
unverified; historical G0 remains BLOCKED. S1 review and S2 connection setup do
not need to wait for donor baseline or a new architecture cycle.
