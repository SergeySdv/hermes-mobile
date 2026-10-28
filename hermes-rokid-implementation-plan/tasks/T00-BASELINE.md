# T00 — Зафиксировать исходники и рабочую исходную сборку

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: Нет. Выполнять первым.
Целевой gate: G0.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Прочитать upstream AGENTS.md, DESIGN.md, CONTRIBUTING.md, Gradle wrapper/version catalogs, manifests и этот пакет. Клонировать оба публичных репозитория в отдельные worktrees; записать полные SHA, branch/tag, дату и причину выбора. Не считать исторический `770bc8d` автоматически текущей рабочей базой.

Собрать исходный Hermes Mobile без новых функций. Собрать donor phone/glasses варианты, если toolchain доступен. Выписать реальные команды, JDK/SDK/AGP/Kotlin/Gradle; не выдавать непроверенную команду из README за успешную. Запустить baseline unit/style/lint; existing failures воспроизвести до исправлений.

Составить карту: chat repository, composer pipeline, attachment storage, backend adapter, auth lifecycle, Room schema/migrations, navigation/theme, foreground services. Проверить реальный `app/src/test/resources/gateway-contract/openrpc.json`, `data/ws/HermesWsClient.kt` и typed descriptors; class names в плане — не замена source search.

Составить donor map: common/protocol, Bluetooth SPP, photo transfer, camera manager, display pagination/settings, зависимости CXR. Отметить код, который НЕ переносится. Проверить LICENSE/NOTICE и сторонние binary/model dependencies. Записать feature-off baseline и определить test package/signature strategy.

## Deliverables

`reports/G0-baseline.md`, source/version/toolchain inventory, sanitized test logs, точная карта integration seams, license inventory, список UNKNOWN. Существующий код не переписывать; допустимы изолированные исправления baseline с отдельным объяснением.

## Acceptance

Повторный checkout тех же SHA воспроизводит baseline build; test failures не скрыты. Архив APK не коммитится. Документирование успешно без SDK не закрывает G0 build gate.

## Stop/rollback и ограничения

При отсутствии SDK/доступа — BLOCKED, подготовить bootstrap requirements. Никаких изменений VPS, установки APK на устройства, repo-wide upgrades или массового переноса donor.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
