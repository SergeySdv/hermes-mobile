# T05 — Потоковый текст, paging и совместимость companion

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G4 PASS.
Целевой gate: G5.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Нормализовать backend events в canonical text snapshots с operation/message/stream identity. Terminal/error/tool progress различать. Не считать каждое сетевое сообщение чистым текстовым delta.

Добавить version/capability negotiation согласно проверенному legacy handshake; при невозможности согласования fail closed для mismatched pair. Ввести bounded coalesced display queue, revision/epoch guards, latest snapshot restore и delivery statuses.

Companion сохраняет page anchor при обновлении хвоста, поддерживает follow-tail/manual reading, RU/ZH/graphemes и safe Markdown projection. Local translation/agent answer — разные display modes. Tool payloads/секреты не показываются автоматически.

Реализовать capture/media vs control traffic priority без голодания final/error frames. Golden fixtures и tests должны исполняться на обеих сторонах protocol library. Проверить stale streams после смены разговора.

## Deliverables

Protocol evolution ADR, codecs/fixtures, companion renderer, phone display reducer, compatibility matrix; `reports/G5-streaming.md` и hardware evidence промежуточного ответа.

## Acceptance

DSP-01..04, минимум 20 stream fixtures и аппаратный тест. Первый полезный текст виден до terminal; page 2 не сбрасывается; reconnect не дублирует строки; bounded queue; несовместимая версия не приводит к crash.

## Stop/rollback и ограничения

Не отправлять каждый токен отдельным BT frame и не заменять unreadable длинный текст бесконечной прокруткой. Final-only compatibility fallback допустим только с собственным тестом и честной маркировкой.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
