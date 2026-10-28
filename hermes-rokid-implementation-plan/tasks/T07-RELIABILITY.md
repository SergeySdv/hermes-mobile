# T07 — Восстановление, безопасность и телефон в кармане

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G6 PASS; foundational tests/ledger создаются в предыдущих tasks.
Целевой gate: G7.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Довести ledger/reconcile до полной state machine: unknown after send, cursor gaps, replay truncation, accepted vs completed, cancel requested vs confirmed, auth expiry, stale scope. Любой retry policy привязан к проверенному backend contract.

Реализовать persistent latest display/outbox policy, bounded caches/media TTL, backoff/jitter, отказ permissions, foreground service stop, device/phone process restart. Camera/mic resources должны освобождаться; feature-off не оставляет background work.

Провести failure injection по каждому critical boundary минимум 10 раз с безопасным staging counter fixture. Не исполнять реальные destructive поручения для проверки идемпотентности. Проверить cross-profile access, OCR injection, redaction, local-only network и отсутствие server secrets на очках.

Провести 60-минутный hardware pocket campaign с раздельными local translation/display/new request результатами и expiry grant по TEST-PLAN, без auto-renew; новые запросы требуют подтверждения в Hermes. Провести парные battery/thermal measurements. На OnePlus проверить фактическую OxygenOS policy; после force-stop требовать ручной запуск там, где это системно ожидается.

## Deliverables

`reports/G7-reliability.md`, failure matrix/counts, deterministic regression fixtures, privacy/security review, pocket trace и battery report; список residual limitations.

## Acceptance

EXT-01..11 на реальном add-on/companion, REC-01..06, SEC-01..06, UI-01/02, критерии G7. Ноль обнаруженных дублей side effects/утечек, ноль crash/ANR в campaign; unknown честно виден пользователю.

## Stop/rollback и ограничения

Не маскировать проблему всегда включённым wakelock, отключением TLS, обходом permissions или повторным submit. При несовместимом backend recovery — безопасное UNKNOWN, а не гарантия автоматического завершения.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
