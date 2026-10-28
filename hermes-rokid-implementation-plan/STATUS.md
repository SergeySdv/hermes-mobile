# Состояние реализации

Обновлено: 2026-10-23. Подготовлен только план. Код приложений/VPS не менялся, собственная сборка APK и hardware tests не выполнялись.

| Gate | Status | Evidence | Blocker/следующий шаг |
|---|---|---|---|
| G0 | NOT_RUN | — | T00: clone/pin, SDK, baseline build |
| G1 | NOT_RUN | — | G0; физические очки/телефон и допуск оператора |
| G2 | NOT_RUN | Терминальный контекст описан, но gate не пройден | G0; target/profile, VPS access и approved changes |
| G3 | NOT_RUN | — | G1 + G2 |
| G4 | NOT_RUN | — | G3 |
| G5 | NOT_RUN | — | G4 |
| G6 | NOT_RUN | — | G5 |
| G7 | NOT_RUN | — | G6 |
| G8 | NOT_RUN | — | G7 + owner acceptance |
| G9 | DEFERRED | — | G8 + отдельный scope |

## Текущие решения

Уточнение владельца 2026-10-28: общий Hermes APK + необязательный отдельно устанавливаемый Rokid add-on + companion очков. Техническое решение [ADR-003](adr/ADR-003-OPTIONAL-ANDROID-ADDON.md) — PROPOSED, ожидает review; функциональная реализация запрещена до review и gates. Agent остаётся на VPS, transport/history/auth только в Hermes.

Таблица gates выше — прежний planning snapshot, оставлен без изменения статусов. Фактический T00 [G0 report](reports/G0-baseline.md) фиксирует BLOCKED; обновление таблицы требует отдельного review и не выполняется архитектурным уточнением.

Требуют evidence: конкретный pinned upstream, аппаратная совместимость, target контейнер/профиль, real backend methods, atomic attachment semantics, retention media, режим idempotency/recovery и performance.

## Обновление агентом

Для каждого изменения статуса добавить ссылку на gate report, commit/build/firmware/server version и reviewer. `PASS` без evidence недействителен. Статус подготовки Markdown и проверки ссылок не является статусом разработки или тестирования APK.
