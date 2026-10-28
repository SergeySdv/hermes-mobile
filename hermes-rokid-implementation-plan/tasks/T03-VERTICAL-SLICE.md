# T03 — Generic IPC, отдельный add-on и минимальный photo/chat/display сценарий

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: ACCEPTED ADR-003, G0/G1/G2 PASS и допуск task. До review ADR функциональные prototypes запрещены.
Целевой gate: G3.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Создать generic IPC seam Hermes и отдельно собираемые add-on/companion из ADR-003. Сначала caller/grant/version/fake-client tests. Rokid dependencies/protocol остаются вне core. Сохранить upstream navigation/theme/auth/repository. Начать с fake glasses, затем перенести только проверенный donor protocol/transport/display/photo code с provenance.

В core добавить generic consent/revoke и draft UI; в add-on — toggle, settings/connection panel и hardware service/permissions. При выключенной функции обычный chat path не меняется. Выполнить local text → glasses без VPS, затем capture → phone draft → явный submit через существующий repository → agent response → phone/glasses.

На этом этапе допускается final-only display, но стабильный target scope и отсутствие двойного submit обязательны. Нельзя скопировать donor AI provider layer или создать вторую базу чатов. Передача файла — app-private ref с integrity validation, не oversized UI byte array.

Интегрировать companion build или pinned build-unit; пока не менять wire behavior без потребности. Добавить codec/transfer unit tests, permissions UI tests и smoke отсутствующего/отключённого/удалённого/упавшего add-on (EXT-01..11). Проверить отказ connection/camera, не только happy path.

## Deliverables

Маленькие PR с modules/seam, fake device, production adapter, settings и одним рабочим flow; `reports/G3-slice.md`; source origin manifest и реальные build commands.

## Acceptance

IPC trust/compatibility/absence suite PASS; основной клиент независимо собирается без Rokid repositories. 10 повторов slice на реальных устройствах; один capture и один server turn на действие; image integrity; явный failure UI; UI-01/02; baseline chat/login/logout/attachments не сломаны.

## Stop/rollback и ограничения

Не проводить тотальную модульную перепись, не встраивать voice/video. Если затрагивается upstream DB/schema, migration тест обязателен уже в этом PR. Rollback — feature off/предыдущий APK без удаления данных.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
