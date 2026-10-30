# S1: limited implementation authorization

Status: IN_PROGRESS. Owner authorization: 2026-10-28 conversation.

S1 may implement a generic Hermes extension interface and a separately installed
Android test add-on without Rokid SDK, donor source, or donor dependencies.
Incomplete donor baseline checks do not block this limited implementation.
Historical G0 remains BLOCKED; its evidence is unchanged. ADR-003 in PR #2
remains PROPOSED. This authorization does not accept the full architecture or
release gates and does not authorize hardware integration.

Scope: text/JPEG → real Android IPC → the existing Hermes composer → explicit
user send confirmation → the existing Hermes transport → a bounded answer
projection in the add-on. No second chat, history, auth store, or VPS transport.

Target: local Medium_Phone_API_36 emulator (Android 16). No personal-device
installation, production VPS change, or merge is authorized.

Validation: automated IPC authorization, expiry/revocation, size-boundary and
failure tests; debug APK builds; existing client checks; emulator execution with
recorded backend type and observed results. Mock evidence must be labelled as
such and cannot establish production or Rokid hardware compatibility.

The owner-provided [S1 handoff](../../hermes-rokid-implementation-plan/Hermes-Rokid-S1-Implementation-and-Studio-Handoff.md) defines scope and UI checks.
IPC follows ADR-003 and specs/03-CONTRACTS.md from architecture PR #2 at d044d1c7;
S1 is the explicit limited exception to its pre-acceptance implementation prohibition.
The implementation PR is separate from architecture PR #2 and T00 continuation.
