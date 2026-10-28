# T06 — Быстрый локальный переводчик

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G5 PASS. Engine/dataset research разрешены раньше как независимая работа.
Целевой gate: G6.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Реализовать FastVisionPort с candidate Chinese OCR и local RU translation. Проверить зависимости/модели/лицензии; setup/download state и airplane-mode readiness. Зафиксировать поддержанные input scripts, не обещать кириллический OCR из наличия RU UI.

Создать pipeline orientation/ROI → OCR blocks → reading order → translation → Observation/display. Добавить unreadable/model missing/cancel states. Избежать многократного открытия engine и неограниченного количества кадров.

Режим Translate only не отправляет данные. Ask about this создаёт draft с готовым OCR как недоверенной аннотацией; отправка только после подтверждения в Hermes и проверки grant. Автоматический Guide/submit вне v1. Поздний OCR не запускает дополнительный скрытый turn. Локальный ответ не записывается как ответ Hermes.

Подготовить 40-image dataset с human ground truth, включая цены/числа и нечитаемые изображения. Измерить отдельно OCR accuracy, translation usefulness, photo transfer и useful-frame latency.

## Deliverables

Engine adapter, setup/privacy UI, offline fixtures и performance report; `reports/G6-ocr.md` с dataset ID, engine versions, quality scores и перечисленными ограничениями.

## Acceptance

OCR-01..06, local-only packet inspection, TEST-PLAN quality/latency thresholds. Пользователь видит короткий перевод при недоступном VPS; поздний agent answer не вытесняет его самовольно.

## Stop/rollback и ограничения

Не скрывать скачивание моделей/внешний перевод и не выбирать low-resolution video frame вместо читаемого still. Недостаточное качество → engine/ROI optimization или согласованный ADR, а не автоматический PASS.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
