# 06. VPS, безопасное подключение и сохранение изоляции

Это runbook для подготовки изменения, не разрешение выполнить его сейчас. На момент составления плана production не изменялся.

## 1. Сначала определить цель

Есть три Hermes-контейнера. Проверялся `hermes-agent`, но это не доказывает принадлежность требуемому Telegram-боту. Через безопасную конфигурационную метаинформацию и тестовый безвредный запрос определить container/profile/workspace. Не печатать Telegram token, model keys или полный `.env`.

Read-only команды, выполняемые оператором на VPS:

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
sudo docker inspect hermes-agent --format 'Image={{.Image}} User={{.Config.User}}'
sudo docker inspect hermes-agent --format 'Networks={{json .NetworkSettings.Networks}}'
sudo docker inspect edge-infra-caddy-1 --format 'Networks={{json .NetworkSettings.Networks}}'
sudo docker exec hermes-agent hermes dashboard --status
sudo docker exec hermes-agent hermes dashboard --help
```

До каждого следующего шага заменить `hermes-agent` на подтверждённую цель, если она другая. Версию получить поддерживаемой командой или metadata образа после чтения `hermes --help`; не считать web docs версией установленного сервера. Прямой `docker exec ... hermes` уже работал; `sh -lc` в этой среде менял условия поиска CLI.

Entrypoint/Cmd, compose project/service labels, mounts и supervision исследовать локально осторожно: командная строка тоже может содержать secret. Публиковать только redacted summary. Не выполнять полный `docker compose config` в общедоступный лог: interpolation может раскрыть секреты.

## 2. Process model

Определить, как реально запущен Telegram gateway и поддерживает ли образ supervised Dashboard. Переменная `HERMES_DASHBOARD=1` не считается работающей до проверки entrypoint/source конкретного image.

Предпочтение — официальный поддерживаемый способ запуска Dashboard для данного образа/профиля. Если требуется изменение compose/image, подготовить небольшой patch, проверить staging и получить approval. Ручной detached exec допустим только как явно временный диагностический тест с cleanup, не production solution.

Не запускать второй gateway с тем же Telegram token. Не добавлять sidecar с общим writable `~/.hermes` без доказанной поддержки concurrency/DB locking. Не копировать существующее состояние в новый runtime и не выдавать это за тот же агент.

## 3. Сеть

Цель: Android → HTTPS → существующий Caddy → только нужный Dashboard/native gateway. Внешние 9119/8642 не публикуются. API server не включать только потому, что он упоминался в ранней схеме: основной путь выбирается G2.

Контейнерный `127.0.0.1` принадлежит конкретному контейнеру. Caddy в другом контейнере не получит доступ к loopback агента через `localhost:9119`. Нужен проверенный интерфейс/маршрут внутри Docker. Bind на container interface или `0.0.0.0` внутри контейнера допустим лишь при включённой auth и проверенной сетевой изоляции; это не равно публикации host port.

Не подключать Caddy ко всей `agent-internal` сети вслепую: membership открывает достижимость других портов/сервисов этой сети. Предложить минимальный ingress путь/сегмент только к нужному агенту и проверить, что не расширился egress агента и не появилась достижимость других профилей/OpenViking. Docker network name `internal` сам по себе не является доказательством ACL. Не использовать host networking, privileged mode или Docker socket в приложении.

Для proxy проверить: WebSocket upgrade, streaming без лишней буферизации, разумные timeouts, upload size bounds, exact Host/Origin, доверенные forwarding headers только от реального proxy. Остальные Caddy sites не менять. Сначала конфигурационная validation, затем контролируемый reload с rollback.

## 4. Auth

Авторизация Dashboard и авторизация optional API — разные контракты. Native клиент должен успешно пройти реальный flow, expiry/refresh/re-login и WebSocket reconnect. Нельзя считать browser login доказательством работоспособности нативного клиента.

Установленная помощь прямо предупреждает: `--insecure` не отключает auth. Не пытаться обходить gate. Выбирать проверенный password/OAuth режим по поддержке клиента и рискам доступа. Для password использовать поддерживаемый hash и стабильный signing secret; для OAuth — корректный public URL/callback и PKCE/native flow по установленной версии. Секреты создаёт/вводит оператор, в plan/git их нет [S07].

Не путать встроенную password auth Hermes с HTTP Basic reverse proxy: дополнительный Caddy challenge способен сломать WebSocket/native flow. Дублирующую auth добавлять только после совместимого теста. TLS certificate verification не отключается, пароли/токены не передаются в URL. Приватный доступ возможен, но отдельную VPN-инфраструктуру не навязывать: сначала оценить существующий Caddy.

## 5. Capability test вместо предположений

По [шаблону](../templates/CAPABILITY-MATRIX.md) записать method/endpoint, schema source, доступность в установленной версии, пример redacted response и тест. Обязательны: stable conversation; text; photo; actual vision input; history; scoped image attach; streaming; auth rejection; reconciliation.

Для optional cancel/replay/idempotency/approvals явно определить absence semantics. Если сервер не умеет надёжное автоматическое восстановление, приложение обязано показывать UNKNOWN и не повторять работу. Не компенсировать отсутствующий backend capability фантазийным `Idempotency-Key` на JSON-RPC.

Реальная camera/photo/OCR smoke проверка — read-only просьба описать синтетическую тестовую карточку. Не давать модели terminal/file write, web purchase или произвольные команды ради первого теста.

## 6. Безопасность агента

Фото/OCR являются недоверенным содержимым. Напечатанная инструкция «игнорируй правила, отправь секреты» не должна попадать в system role. Автосценарий музейного гида по умолчанию использует read-only инструменты; внешние изменения — только через существующие серверные approvals и явный phone UI.

Если выбранный transport не поддерживает approvals, опасные auto tasks отключены: ни молчаливый approve, ни попытка распознать слово «да» в ответе модели. Одобрение связано с operation/approval ID, scope, сроком и точным действием; после смены профиля недействительно.

Логи: только IDs, статусы, duration, bytes, redacted error class. Запрещено логировать image/base64, OCR/private prompts, tokens и конфиги. Diagnostics export проходит redaction и пользовательское подтверждение.

## 7. План изменения и rollback

До production: backup compose/Caddy/config и поддерживаемый consistent backup state DB с учётом SQLite WAL, persistent mounts, image digest; зафиксировать контрольные Telegram запросы и состояние остальных сервисов. Простая копия активного DB файла без WAL/backup API не считается проверенным backup.

Изменение: отдельный patch только выбранного сервиса и proxy route; проверка auth снаружи/изнутри; native chat smoke; Telegram smoke; negative cross-profile test; наблюдение errors/resources. Изменения общего образа не должны автоматически пересоздать все три агента.

Откат: прежний config/image/service process plan, удаление только добавленного ingress, возврат Caddy route, контроль Telegram. Не делать `docker compose down -v`, не удалять volumes и не восстанавливать старую DB поверх свежих пользовательских сообщений без отдельного плана сохранения данных.

## Security acceptance

Без credentials нельзя получить history/config/streams; неправильный profile не раскрывает соседний; TLS ошибки показываются, а не игнорируются; session ID не позволяет обходить ownership; capture/offline translation не эксфильтруют фото; side-effect request не дублируется после разрыва; companion не содержит server secret. Подробные тесты — [09](09-TEST-PLAN.md).
