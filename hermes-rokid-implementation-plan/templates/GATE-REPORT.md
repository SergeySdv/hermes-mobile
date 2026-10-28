# Gate report — Gx / Task Tx

Status: NOT_RUN | IN_PROGRESS | PASS | FAIL | BLOCKED | DEFERRED. Оставить одно значение при заполнении.
Дата/оператор/reviewer: …

## Входы

| Поле | Значение |
|---|---|
| Hermes Mobile repository / full SHA | UNKNOWN |
| Donor repository / full SHA | UNKNOWN |
| Phone APK hash / version / signer | UNKNOWN |
| Glasses APK hash / version / signer | UNKNOWN |
| Protocol version | UNKNOWN |
| Phone model / OS / build | UNKNOWN |
| Glasses model / firmware / Android | UNKNOWN |
| Backend version / image digest / profile | UNKNOWN |
| Dataset/fixture ID / hash | UNKNOWN |
| Предыдущие required gates | UNKNOWN |
| Approval на hardware/production действия | NOT_GRANTED / NOT_REQUIRED с причиной |

## Изменения

Файлы/PR, цель каждого изменения, touched modules, миграции, изменение permissions/network. Отдельно перечислить действия на устройствах и VPS.

## Проверки

| Test ID | Команда/метод | Ожидаемое | Фактическое | Evidence | Result |
|---|---|---|---|---|---|
| … | … | … | NOT_RUN | — | NOT_RUN |

Привести exit codes и числа trials/failures. Для аппаратных действий — наблюдение оператора; для network contract — redacted requests/results. Сырые credentials/photos не прикладывать.

## Criteria checklist

Каждый критерий соответствующего gate: выполнен/не выполнен/не проверен, с прямой ссылкой на evidence. Даже один непроверенный обязательный критерий исключает PASS.

## Риски и отличия от плана

Что осталось UNKNOWN, unsupported capabilities, failed cases, согласованные ADR/waivers. Ограничения эмулятора, CI и mock tests описывать явно.

## Rollback

Как вернуть baseline, какие данные сохранятся, что уже проверено. Не «git revert» как универсальный ответ для Room/firmware/server changes.

## Решение reviewer

Статус и причина. Разрешённый следующий task. Если BLOCKED — одна конкретная следующая проверка владельца; независимая работа, которую можно продолжать без обхода gate.
