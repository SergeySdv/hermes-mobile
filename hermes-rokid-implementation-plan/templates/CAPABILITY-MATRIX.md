# Матрица реального backend контракта

Backend version/image digest: UNKNOWN. Mobile commit: UNKNOWN. Проверенный container/profile/workspace: UNKNOWN.
Выбранный transport: UNDECIDED. Auth mode: UNKNOWN. Public origin: UNKNOWN.

Статус capability: SUPPORTED_TESTED / UNSUPPORTED_TESTED / UNKNOWN. Реклама в README даёт только UNKNOWN для установленного сервера.

| Capability | Обязательность | Реальный method/endpoint + schema source | Status | Test/evidence |
|---|---|---|---|---|
| Native login/auth refresh | Для выбранного пути | UNKNOWN | UNKNOWN | — |
| Profile identity/authorization | Обязательно | UNKNOWN | UNKNOWN | — |
| Create/resume owned session | Обязательно | UNKNOWN | UNKNOWN | — |
| Read canonical history | Обязательно | UNKNOWN | UNKNOWN | — |
| Submit text | Обязательно | UNKNOWN | UNKNOWN | — |
| Upload/attach image | Обязательно | UNKNOWN | UNKNOWN | — |
| Atomic image↔turn binding | Обязательно, native или coordinator-proven | UNKNOWN | UNKNOWN | — |
| Real vision model path | Обязательно | UNKNOWN | UNKNOWN | — |
| Persist/reaccess image context | Обязательно для G4, возможно ADR стратегия | UNKNOWN | UNKNOWN | — |
| Incremental text events | Обязательно для G5 | UNKNOWN | UNKNOWN | — |
| Authoritative terminal outcome | Обязательно | UNKNOWN | UNKNOWN | — |
| Query/reconcile operation/history | Обязательно для безопасного recovery policy | UNKNOWN | UNKNOWN | — |
| Replay cursor + gap/truncation handling | Опционально; иначе snapshot strategy | UNKNOWN | UNKNOWN | — |
| Native cancel | Опционально; честно unsupported | UNKNOWN | UNKNOWN | — |
| Native approval workflow | Обязательно для actions, иначе read-only restrictions | UNKNOWN | UNKNOWN | — |
| Server-side deduplication | Опционально; без него нет automatic resubmit | UNKNOWN | UNKNOWN | — |
| Tool progress typed events | Опционально | UNKNOWN | UNKNOWN | — |
| Attachment deletion/retention | Нужна определённая политика, не обязательно delete API | UNKNOWN | UNKNOWN | — |

## Scope проверки

Владелец session: …; можно ли создать/продолжить session другого surface: …; подтверждённые error codes: …
Очередь pending images принадлежит: SESSION / CONNECTION / ACTIVE_FOCUS / UNKNOWN.
Последовательность attach/submit и блокировка: …
Одновременные turns: серверная очередь/lease/отказ и client policy: …

## Idempotency/recovery

Если подтверждён dedup: поле/заголовок; scope по credential/profile; matching payload fields; retention; restart persistence; streaming differences; conflict behavior. Тест повторной отправки на безопасном fixture. Не переносить гарантии REST API на JSON-RPC без доказательства.

Если dedup отсутствует: UNKNOWN outcome UX; read-only reconciliation; запрет automatic resend; объяснение owner-visible retry.

## Серверное выполнение после disconnect

Продолжается / отменяется / зависит от метода / UNKNOWN. Подтверждающий тест: …
Наличие истории/статуса после server restart: …

## Transport decision

Native / alternative whole-conversation API adapter / BLOCKED.
ADR reference, обязательные непокрытые capabilities, какие endpoints/порты НЕ нужно включать, reviewer sign-off.
