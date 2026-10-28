# ADR-NNN — Название решения

Status: PROPOSED | ACCEPTED | SUPERSEDED. Дата, автор, reviewer/owner.

## Контекст

Какая конкретная проблема требует решения? Подтверждённые source/contract/hardware evidence и UNKNOWN. Связанные требования/gates/tasks.

## Варианты

| Вариант | Польза | Риски/стоимость | Доказательства |
|---|---|---|---|
| A | … | … | … |
| B | … | … | … |

## Решение

Выбранный вариант и ограничения применения. Что не входит в scope. Новые runtime dependencies/permissions/network reachability/secret handling.

## Контракт и миграция

Какие API, data schema, identity, ownership, wire versions изменяются? Как обновляются phone и glasses независимо? Что произойдёт со старыми чатами/operations?

## Проверки

Новые tests/metrics/negative cases. Какие старые gate reports потеряют силу и что нужно повторить? Нельзя снизить acceptance target без явной записи прежнего и нового значений.

## Rollback и решение владельца

Рабочий путь отката, риски data loss, scope production approval. Кто и что принял. До подтверждения PROPOSED не является разрешением на production изменение.
