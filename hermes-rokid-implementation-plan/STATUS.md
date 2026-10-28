# Состояние реализации

Обновлено: 2026-10-28 по review PR #2. Текущий статус ниже; baseline сборки и проверки описаны в G0 report. Функциональная интеграция не начата, ADR-003 остаётся PROPOSED, review ожидается. Новых PASS нет.

| Gate | Status | Evidence | Blocker/следующий шаг |
|---|---|---|---|
| G0 | BLOCKED | [G0 baseline](reports/G0-baseline.md) | Review ожидается; продолжить незавершённый T00 |
| G1 | NOT_RUN | — | G0; физические очки/телефон и допуск оператора |
| G2 | NOT_RUN | Терминальный контекст описан, но gate не пройден | G0; target/profile, VPS access и approved changes |
| G3 | NOT_RUN | — | G1 + G2 |
| G4 | NOT_RUN | — | G3 |
| G5 | NOT_RUN | — | G4 |
| G6 | NOT_RUN | — | G5 |
| G7 | NOT_RUN | — | G6 |
| G8 (Rokid) | NOT_RUN | — | G0–G7 + H-RELEASE выбранного Hermes + owner acceptance |
| G9 | DEFERRED | — | G8 + отдельный scope |
| H-RELEASE | NOT_RUN | — | Независимые проверки и review конкретного Hermes release candidate; не зависит от G0–G8 Rokid |

## Текущие решения

Уточнение владельца 2026-10-28: общий Hermes APK + необязательный отдельно устанавливаемый Rokid add-on + companion очков. Техническое решение [ADR-003](adr/ADR-003-OPTIONAL-ANDROID-ADDON.md) — PROPOSED, ожидает review; функциональная реализация запрещена до review и gates. Agent остаётся на VPS, transport/history/auth только в Hermes.

После review — продолжение незавершённого T00 на зафиксированных SHA, без нового проектирования системы. Текущая таблица указывает на существующее evidence, не переписывает его.

Pinned upstream уже записан в G0. Требуют evidence: аппаратная совместимость, target контейнер/профиль, real backend methods, atomic attachment semantics, retention media, режим idempotency/recovery и performance.

## Обновление агентом

Для каждого изменения статуса добавить ссылку на gate report, commit/build/firmware/server version и reviewer. `PASS` без evidence недействителен. Статус подготовки Markdown и проверки ссылок не является статусом разработки или тестирования APK.

## Исторический planning snapshot — 2026-10-23

Не текущий статус; до baseline builds. Сохранён для истории планирования.

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
