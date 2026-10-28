# Backlog и последовательность PR

Все оценки сложности относительные; это не календарный график. Число PR можно разделять, но границы gates и критерии нельзя пропускать.

| Task | Объём | Зависимость | Выход | Gate |
|---|---|---|---|---|
| [T00](tasks/T00-BASELINE.md) | Pin, build, source map, licenses | Нет | Baseline evidence | G0 |
| [T01](tasks/T01-HARDWARE-SPIKE.md) | Donor на конкретных очках | G0 | Hardware report, transfer baseline | G1 |
| [T02](tasks/T02-BACKEND-CONTRACT.md) | Target VPS, auth, native contract | G0; prod approval для changes | Capability matrix и transport ADR | G2 |
| [T03](tasks/T03-VERTICAL-SLICE.md) | Generic IPC + optional add-on + photo/chat/display | ACCEPTED ADR-003 + G0/G1/G2 | Общий Hermes APK, отдельные add-on и companion | G3 |
| [T04](tasks/T04-SESSIONS-CONTEXT.md) | Persistent IDs, context, atomic attachment binding | G3 | Один разговор телефона/очков | G4 |
| [T05](tasks/T05-DISPLAY-STREAMING.md) | Snapshot stream, pages, version negotiation | G4 | Промежуточный текст на очках | G5 |
| [T06](tasks/T06-LOCAL-OCR.md) | Offline Chinese OCR → RU | G5 | Независимый fast path | G6 |
| [T07](tasks/T07-RELIABILITY.md) | Ledger recovery, failure injection, pocket/security | G6 | Надёжный MVP | G7 |
| [T08](tasks/T08-RELEASE.md) | Signing, pilot, rollback, release notes | G7 | Независимые releases + compatibility matrix | G8 |
| [T09](tasks/T09-VOICE-VIDEO.md) | Push-to-talk; отдельные audio/video spikes | G8 + scope approval | Независимые subphase reports | G9 |

## Рекомендуемая нарезка PR

PR-00 baseline/docs/CI repair без изменения поведения.
PR-01 donor compatibility fixtures и hardware report, без импорта phone UI.
PR-02 backend contract fixtures/adapter seam и reviewed deploy patch отдельно от его исполнения.
PR-03 после ADR review: generic IPC contract/broker + fake client и security/absence tests. Rokid protocol/transport только в отдельном add-on build-unit.
PR-04 minimal connection/settings/photo integration.
PR-05 shared observation/context + Room migrations.
PR-06 incremental display + companion protocol compatibility.
PR-07 local OCR/translation setup и privacy modes.
PR-08 recovery/security/bounded queues и failure tests.
PR-09 release build/signing/docs/rollback.

Нельзя считать успешный PR-03 завершённым G3: gate требует реальный сквозной сценарий. Формально смешивать небольшие соседние PR можно после review, но не скрывать failure cases в «следующей задаче», если они являются acceptance текущего gate.

## Что не включать в эту очередь

Новая SaaS-платформа, автоматический merge Telegram sessions, свой cloud message broker, universal plugin store, переписывание всех настроек Hermes, runtime агента на Android, continuous video/wake word до G8. Появление такого scope оформляется отдельным ADR и не отодвигает закрытие текущего MVP без решения владельца.
