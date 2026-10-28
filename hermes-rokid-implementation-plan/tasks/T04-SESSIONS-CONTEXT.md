# T04 — Общая сессия и долговечный контекст фотографии

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G3 PASS.
Целевой gate: G4.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Реализовать immutable conversation/backend scope mapping, Observation и минимальный PendingOperation ledger. Phone и glasses inputs используют один submit coordinator. Значимые записи сохранять до network side effect.

Закрыть attach→submit race: выясненный в G2 scope очереди определяет блокировку; другой chat/profile не может забрать pending photo. Session switching/Activity recreation не меняют цель начатой операции. Ошибка attach не отправляет dangling media со следующим вопросом.

Подключить server history и persistent media semantics. Выполнить CTX-01..04, дополнительно проверить resume после process death и controlled backend restart в разрешённой среде. Если нужны reattachment старых изображений, оформить корректную стратегию без дублирования истории.

Не копировать donor sliding six-message window/dynamic timestamp prompt. Локальный OCR/перевод хранить как аннотацию Observation, а не фиктивный assistant turn. Показать source/context attachment в том же chat UI.

## Deliverables

Room migrations/tests, scope/ledger integration, context fixtures, attachment lifecycle strategy; `reports/G4-context.md` с inspectable server/session evidence.

## Acceptance

Фото A → text follow-up → фото B → сравнение; restart/reload сохраняют mapping и доступ к media; другой разговор не получает A/B. Подтверждён backend image input, а не только видимая галерея.

## Stop/rollback и ограничения

Нельзя соединять native/API/Telegram histories одинаковым UUID. Если сервер не обеспечивает нужную media/history семантику — BLOCKED/ADR. Никаких destructive Room migrations.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
