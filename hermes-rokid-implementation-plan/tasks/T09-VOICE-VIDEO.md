# T09 — Отдельная фаза голосового и видеорежима

Статус задания: NOT_STARTED (для T09 — DEFERRED).
Зависимости: G8 PASS и отдельно утверждённый scope.
Целевой gate: G9.

## Прочитать

[Правила агента](../AGENTS.md), [gates](../specs/02-GATES.md), [архитектура](../specs/01-ARCHITECTURE.md), [контракты](../specs/03-CONTRACTS.md), [test plan](../specs/09-TEST-PLAN.md), [STATUS](../STATUS.md). Затем профильные specs по содержанию задания.

## Работа

Начать с push-to-talk, не с постоянного микрофона. Доказать источник аудио очков при телефоне в кармане, реализовать ограниченную запись, STT adapter, language selection и submit через тот же repository.

TTS/audio routing, true wake word и continuous video оформить отдельными subphase задачами V2/V3/V4. Наличие записи на телефоне не закрывает аудиопуть очков. Наличие JPEG sequence не закрывает video streaming.

Для видео заново исследовать выбранный donor/SDK, CN firmware compatibility, лицензии, codec/network, thermal/battery и usefulness. Передавать в агент ограниченные observations/keyframes вместо turn на каждый frame.

Сохранить стабильный MVP при выключенных новых функциях. Protocol/schema changes требуют повторных соответствующих gates и release tests.

## Deliverables

Отдельные ADR и reports по V1–V4 с конкретными аппаратными измерениями; никакого объединённого PASS «голос и видео есть», если проверена только одна часть.

## Acceptance

Acceptance подфаз из 11-VOICE-VIDEO-LATER; real microphone routing, permission/privacy tests, bounded media pipeline и независимый rollback. Непройденное видео не отменяет release MVP.

## Stop/rollback и ограничения

Не использовать amplitude trigger как wake word и не запускать background mic/camera вопреки OS rules. Без отдельного scope эта задача DEFERRED.

## Отчёт агенту-интегратору

Использовать [GATE-REPORT](../templates/GATE-REPORT.md): commit/build inputs, изменённые файлы, команды/exit codes, тесты passed/failed/not run, evidence, risks, rollback и следующий разрешённый task. Обновить STATUS только после review. Проверки на устройстве/VPS без реального доступа не подменять моками.
