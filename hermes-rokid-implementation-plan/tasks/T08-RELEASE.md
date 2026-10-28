# T08 — Независимый выпуск Hermes и выпуск Rokid с compatibility matrix

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости core lane: собственные RC проверки и review, без donor/CXR/G0–G8 Rokid.
Зависимости Rokid lane: G0–G7 PASS, H-RELEASE выбранной версии Hermes и approval владельца на pilot/install/release.
Целевые допуски: H-RELEASE (NOT_RUN), G8 (NOT_RUN); PASS не переносится автоматически.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Core lane: собрать и проверить обычный Hermes APK независимо от Rokid. При наличии broker проверить generic IPC/security/absence с fake client; реальные очки не требуются.

Rokid lane: собрать release варианты add-on/companion с проверкой R8 и без debug/private secrets. Подтвердить application IDs, signer fingerprint, protocol compatibility и воспроизводимые inputs. Проверить standalone core regression и EXT-01..11; отдельный core CI/release без Rokid dependencies.

Провести две отдельные пользовательские pilot sessions с text/photo/context/OCR/stream. Собрать замечания владельца по читаемости, latency, noise, battery и обычному использованию телефона. Закрыть critical/high defects.

Проверить update/rollback на сохранённых данных, включая Room schema и повышенный versionCode rollback build при необходимости. Production server patch, если был, откатывается по отдельному runbook без остановки других агентов.

Подготовить release notes, supported device/server matrix, instructions, third-party notices, dependency/SBOM inventory, hashes и private diagnostics procedure. Никаких неподтверждённых обещаний all-day voice или universal Rokid compatibility.

## Deliverables

Core lane: один Hermes APK и собственный release report. Rokid lane: два новых APK (add-on/companion) и ссылка на проверенный Hermes; полный комплект из трёх APK с SHA-256/signature metadata, installation/update/rollback guide, release notes и `reports/G8-release.md`. Артефакты лежат в согласованном release storage, не автоматически публикуются в открытом репозитории.

## Acceptance

Core: H-RELEASE по собственному RC evidence, без аппаратных gates Rokid.

Rokid: G0–G7 и H-RELEASE evidence согласованы с точной compatibility matrix core IPC × add-on × companion; OPS-01..04; rollback rehearsal; owner acceptance; нет открытых critical/high блокеров.

## Stop/rollback и ограничения

Не публиковать без допуска, не вшивать signing keys, не использовать upstream public debug keystore для production trust. Подготовка Markdown/planning не заменяет этот task.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
