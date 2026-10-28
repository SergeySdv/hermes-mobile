# T02 — Проверить VPS и выбрать единый backend transport

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G0. G1 может идти параллельно. Production changes только после отдельного approval.
Целевой gate: G2.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Определить нужные container/profile/workspace, образ/версию, supervision, persistent mounts и сеть Caddy. Применить read-only runbook; секреты не выводить. Не запускать второй Telegram gateway.

Подготовить patch безопасного Dashboard/native ingress через существующий Caddy, auth и rollback. Сначала доказать, что ordinary Hermes Mobile подключается без модификаций. Режим доступа должен сохранять изоляцию и не публиковать 9119/8642.

Заполнить capability matrix для установленного сервера: реальный native method/schema, text/image/history, attachment queue scope, stream/replay, terminal semantics, cancel, approvals, retry/recovery. Провести NET-01..03 и отрицательные auth/scope тесты. Тест photo — синтетическая картинка, модель с подтверждённым vision path.

Выбрать native transport предпочтительно; optional API только по ADR для целого conversation type. Проверить image attach race и совместимость серверной версии с pinned mobile. Подготовить реальные fixtures для клиента, не изобретать методы.

После approved change повторить Telegram и остальные service smoke checks. Без production разрешения представить deploy patch/runbook и оставить интеграционный gate BLOCKED.

## Deliverables

`reports/G2-backend.md`, заполненная capability matrix, transport ADR, redacted request/result fixtures, reviewed deployment patch и rollback steps; без secrets/config dumps.

## Acceptance

G2 pass criteria и OPS-01..03. Text + image + follow-up + reload принадлежат одной доказанной сессии. Unsupported optional capability имеет безопасное UI-поведение; неизвестные обязательные capabilities не принимаются.

## Stop/rollback и ограничения

Не обновлять сразу все агенты, не использовать host/privileged mode и не расширять сети вслепую. Неподходящий контракт — BLOCKED/ADR, а не тайный второй LLM-клиент.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
