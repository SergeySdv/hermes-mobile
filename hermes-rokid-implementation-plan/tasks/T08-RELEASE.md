# T08 — Пилот, две подписанные сборки и откат

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G7 PASS и approval владельца на pilot/install/release.
Целевой gate: G8.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Собрать release варианты телефона/очков с проверкой R8 и без debug/private secrets. Подтвердить application IDs, signer fingerprint, protocol compatibility и воспроизводимые inputs. Проверить standalone core regression и EXT-01..10; отдельный core CI/release без Rokid dependencies.

Провести две отдельные пользовательские pilot sessions с text/photo/context/OCR/stream. Собрать замечания владельца по читаемости, latency, noise, battery и обычному использованию телефона. Закрыть critical/high defects.

Проверить update/rollback на сохранённых данных, включая Room schema и повышенный versionCode rollback build при необходимости. Production server patch, если был, откатывается по отдельному runbook без остановки других агентов.

Подготовить release notes, supported device/server matrix, instructions, third-party notices, dependency/SBOM inventory, hashes и private diagnostics procedure. Никаких неподтверждённых обещаний all-day voice или universal Rokid compatibility.

## Deliverables

Три независимо подписанных APK с SHA-256/signature metadata, installation/update/rollback guide, release notes и `reports/G8-release.md`. Артефакты лежат в согласованном release storage, не автоматически публикуются в открытом репозитории.

## Acceptance

G0–G7 evidence согласованы с точной compatibility matrix core IPC × add-on × companion; OPS-01..04; rollback rehearsal; owner acceptance; нет открытых critical/high блокеров.

## Stop/rollback и ограничения

Не публиковать без допуска, не вшивать signing keys, не использовать upstream public debug keystore для production trust. Подготовка Markdown/planning не заменяет этот task.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
