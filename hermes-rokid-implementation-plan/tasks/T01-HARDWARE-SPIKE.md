# T01 — Доказать совместимость конкретных Rokid

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G0. Отдельный допуск на установку APK/аппаратные действия.
Целевой gate: G1.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Заполнить hardware report: модель/firmware/OS очков, ОС OnePlus, official app version и поддерживаемый developer install path. Снять baseline штатной работы. Выбрать устройства явно по serial; не публиковать serial в git.

Сначала запустить неизменённую pinned donor пару. Проверить установку, permissions, pairing, текст, страницы, кириллицу/китайские символы и одно фото. Не использовать LLM для первого text display/photo transport теста: локального тестового ответа достаточно.

Затем выполнить HW-02..04: 20 текстов, 10 JPEG с integrity checks, 5 reconnect. Измерить capture, transfer, display по отдельности. Сохранить sample image quality evidence локально. Проверить, действительно ли работает SPP/Camera2 без CXR bootstrap; любую необходимость SDK/секретов документировать, не предполагать.

Проверить camera busy/permission denial и штатную работу официального приложения после завершения. Составить минимальный список donor компонентов для переноса и измеренный photo profile.

## Deliverables

`reports/G1-hardware.md`, compatibility matrix, sanitized logs, private evidence references, exact APK hashes/signatures, working donor protocol notes и перечень ограничений.

## Acceptance

Все аппаратные критерии G1, integrity изображений, readable RU/ZH и восстановление штатной работы. Success автора donor или emulator screenshot не считаются evidence этой пары устройств.

## Stop/rollback и ограничения

Если sideload/камера/протокол запрещены или недоступны — BLOCKED/FAIL с точной причиной. Не root/flash/reset устройство. Разрешены независимые backend/mock работы, но не глубокий hardware port.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
