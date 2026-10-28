# ADR-003 — Необязательное отдельно устанавливаемое Android-дополнение

Status: PROPOSED. Дата: 2026-10-28. Автор: Codex. Reviewer/owner: review ожидается.
Требование владельца о раздельных APK зафиксировано; технический IPC-дизайн ниже — DESIGN.
Заменяет прежнее решение ADR-001 о встроенных Rokid-модулях в specs/01-ARCHITECTURE.md.
Не меняет G0, не допускает реализацию, установку APK или действия на VPS.

## Контекст и варианты

Основной Hermes Mobile должен оставаться общим APK для всех пользователей. G0 source audit
обнаружил orchestration в ChatViewModel, отдельный persistence repository и существующий
attachment target с generation fencing; общего готового chat repository отправки нет.
Нельзя оправдывать IPC переписыванием чата или переносом donor phone-app целиком.

| Вариант | Оценка |
|---|---|
| Rokid внутри Hermes / отдельный rokid flavor | Отклонён требованием владельца: связывает зависимости, permissions и выпуск |
| Устанавливаемый APK с ограниченным IPC | Выбран для review: изоляция процессов, отдельный выпуск; цена — lifecycle и trust boundary |
| Второй LLM-клиент с прямым VPS доступом | Отклонён: дублирует auth, транспорт и историю |
| Загружаемый dex/plugin store | Вне scope; APK устанавливается средствами Android, код не загружается в процесс Hermes |

## Границы и данные

| Компонент | Владеет | Не получает / не содержит |
|---|---|---|
| Hermes Mobile | Единственные chat/history/auth/backend transport; approvals; generic extension broker; выдача/отзыв разрешений; существующий composer/outbox | Rokid SDK, CXR, SPP, очковый wire codec, hardware permissions и OCR-модели дополнения |
| Rokid Android add-on | Отдельный package/UID/process, pairing, Bluetooth FGS, transfer, device UI, bounded temporary media, локальные OCR/перевод, display projection | VPS endpoint/токены/cookie/WS tickets, базу истории, chat UI, выбор моделей, прямые вызовы VPS |
| Glasses companion | Camera/buttons/display, локальные permissions, протокол связи с add-on | Hermes IPC grants, серверные секреты, LLM-клиент, долговременную историю |

Поток: companion ⇄ add-on ⇄ generic IPC broker Hermes ⇄ существующий chat pipeline ⇄ VPS.
Только add-on понимает Rokid wire protocol. IPC и очковый protocol имеют разные версии.
Local-only OCR работает внутри add-on без выдачи разрешения на разговор и без upload.
Временное отображение ответа не становится второй историей: очистка при отзыве/истечении
lease; сохранение чата и recovery remote operations выполняет только Hermes.

## IPC v1: минимальное предложение для review

Explicit bound service Hermes, AIDL/Binder, без localhost HTTP, broadcast с содержимым
чата и передачи ViewModel/Room entities. Небольшая независимая contract library содержит
только DTO/IDL и synthetic fixtures, не тянет Hermes app, Rokid SDK или donor common.
Имена ниже проектные, не существующие Android/серверные API.

| Операция | Контракт / результат |
|---|---|
| `negotiate` | major/minor, capability intersection и пределы; до проверки caller никаких conversation data |
| `openSession` | Только после подтверждения в UI Hermes: opaque grant handle, connection epoch, expiry и выданные capabilities; callback связан с тем же caller |
| `stageInput` | grant, epoch, operationId, text и optional read-only ParcelFileDescriptor JPEG → staging receipt; создаёт draft, не отправляет prompt |
| `observe` | Подписка на разрешённую display projection с sequence; bounded snapshots текущего разрешённого assistant output и локального статуса |
| `getOperationStatus` | Только операции этого caller/grant; STAGED / AWAITING_USER / ACCEPTED / UNKNOWN / REJECTED; ACCEPTED не означает remote completion |
| `closeSession` | Отписка/отзыв lease и cleanup; не обещает отмену уже принятого сервером действия |

В v1 нет listConversations, history export, raw RPC, arbitrary URL, выбора serverSessionId,
auto-submit, approveTool, смены профиля или отмены remote tools. Submit подтверждается
в Hermes и проходит существующие проверки и outbox. Режим автоматического гида —
последующее отдельное решение, не скрытое расширение stageInput.

Major mismatch → UNSUPPORTED_VERSION, никаких данных/side effects. Minor только additive,
неизвестная capability не выдаётся; отсутствие обязательной capability → отказ функции.
Фиксированные transaction IDs и совместимые DTO evolution требуют golden fixtures N/N-1;
неизвестные enum values отклоняются. Смена major не ломает обычный чат.

Предложенные пределы v1: metadata ≤64 KiB, текст ≤16 KiB UTF-8, один JPEG ≤5 MiB,
≤20 megapixels после проверки декодером, один импорт одновременно на grant, очередь ≤8,
lease ≤15 минут, чтение FD timeout 30 s. Это DESIGN, не измеренные свойства Android.
Пределы согласуются handshake, превышение → LIMIT_EXCEEDED; никакого base64 в Binder.
Нельзя принимать file paths/произвольные content URI от дополнения. Hermes копирует FD
в своё private staging, проверяет фактический размер/MIME/decode/hash и scope повторно
перед commit. FD недоверенный: pipe/изменяемый файл/неполный JPEG не должны зависнуть
на main/Binder thread. При отзыве остановить чтение и удалить незавершённую копию.

Фото, допустимое transport, но превышающее IPC bounds, отклоняется как LIMIT_EXCEEDED без draft/submit и без скрытого ухудшения качества; точный порядок — specs/03-CONTRACTS.md, тест EXT-11. Численные пределы не меняются.

operationId дедуплицируется в Hermes в scope caller+grant+epoch. Повтор с другим payload
отклоняется. Потеря ACK → status query, не новый submit. После перезапуска core/grant expiry
старые handles недействительны; unresolved действия видны в существующем чате, повторная
отправка запрещена. IPC idempotency не выдаётся за exactly-once на VPS.

## Модель доверия и разрешённый разговор

MVP допускает только проверенные package+signer identities из reviewed allowlist; пользователь
не может доверить произвольный APK одним совпадением имени. Отдельные release signing keys
допустимы и предпочтительны для независимого выпуска; signature permission сама по себе
не подходит как единственный механизм при разных ключах. Нет sharedUserId/shared storage.

Каждая Binder transaction проверяет реальный calling UID, Android user, package membership
и signing certificate/утверждённую lineage, а затем grant. UID фиксируется до async dispatch
или clearCallingIdentity; package string из payload не является identity. Shared UID с
неразрешёнными packages отклоняется. Add-on также проверяет signer/component Hermes до
bind; package visibility задаётся адресно, без QUERY_ALL_PACKAGES. Binder handle не заменяет
проверку UID: передача handle третьему приложению не даёт доступ. Debug trust отделён от
release, публичный upstream debug key не является production identity.

Trust list update и key rotation требуют signed reviewed client update; без проверки новой
identity расширение отключается. Точные package IDs/fingerprints пока UNKNOWN, не генерировать
их из текущего debug APK как production defaults.

UI Hermes показывает идентичность дополнения, выбранный разговор и отдельно права
`draft.write` / `assistant.display`; пользователь выдаёт только нужные. Core хранит mapping
handle → endpoint/account/connection profile/agent profile/storage+runtime session/generation,
caller identity, capabilities, epoch, expiry. Дополнение получает лишь opaque handle и
минимальную display label, не реальные session/profile identifiers. Оно не перечисляет
разговоры и не переносит grant на другой разговор.

Смена активного разговора/профиля, logout, истечение lease, отключение расширения,
package removal/replacement, signer change или Binder death → немедленная инвалидизация
в core до очередного I/O/event delivery. Повторный выбор — новое подтверждение в Hermes.
Уже выданные байты нельзя отозвать из злонамеренного процесса: доверие к signer и
минимизация output обязательны; best-effort clear дисплея не считается гарантией стирания.
Companion должен очищать projection по собственному TTL при потере add-on.

В projection нет tool/system/auth messages, approvals, старой истории или данных другого
scope. Содержимое ответа само может быть чувствительным — право display означает передачу
этого текста дополнению и очкам. OCR/изображения всегда недоверенный пользовательский ввод.

## Lifecycle, независимый выпуск и rollback

Core не запускает discovery/scanning/pairing и не ждёт дополнения для старта или send.
Add-on отсутствует/disabled/incompatible/crashed → broker fail closed, обычный Hermes работает.
Binder death, timeout и backpressure не блокируют UI/core send; callbacks coalesce snapshots,
медленный consumer отсоединяется. Add-on владеет своим connectedDevice FGS/notifications;
core не превращается в службу Bluetooth и не обещает пережить force-stop.

Три независимых build units и артефакта: общий Hermes APK, optional add-on APK, companion APK.
Core CI/release не скачивает и не собирает Rokid/CXR/companion; его gate — существующие
проверки + generic IPC security/compatibility/absence tests с fake client. Add-on CI владеет
Rokid dependencies/licenses/permissions; integration CI отдельно проверяет compatibility
matrix core IPC × add-on × companion protocol. Невыпущенное дополнение не блокирует обычный
релиз Hermes; небезопасный generic broker блокирует релиз core.

Каждый артефакт имеет собственные versionCode/signature/SBOM и release approval. N/N-1
проверяется в обе стороны; несовместимость отключает расширение, не требует downgrade core.
Отключение/удаление add-on отзывает grants и удаляет его временные данные, не историю Hermes.
Rollback core не нужен для отката дополнения; schema downgrade без проверенной migration
не допускается. Никакие установки, публикации или signing changes данным ADR не разрешены.

## Проверки, gates и решение review

Обязательная матрица EXT-01..11 находится в specs/09-TEST-PLAN.md. Security tests проверяют
UID/signer/handle replay, scope race после FD copy, callback leak, malformed/oversize input,
UNKNOWN без resend, flood/timeout и package lifecycle. Review отдельно принимает trust policy,
capabilities, consent UX, FD bounds, contract evolution и lifecycle core broker.

G0 остаётся как зафиксирован в reports/G0-baseline.md (BLOCKED); STATUS указывает на этот BLOCKED и ожидающий review; старый planning snapshot отделён. H-RELEASE обычного Hermes независим от Rokid G0–G8 и пока NOT_RUN.
Исторический G0 source audit не переписывается под новый дизайн. ADR review не закрывает G0,
G1 или G2. До ACCEPTED и допуска соответствующих gates — только документация, без functional
prototypes. После review: закрыть G0, выполнить G1/G2 по отдельному допуску; T03 начинает
малый generic broker + fake client, затем отдельно add-on. Не извлекать весь ChatViewModel.

Основа Android-механизмов: [AIDL](https://developer.android.com/develop/background-work/services/aidl),
[IPC security](https://developer.android.com/privacy-and-security/security-tips),
[Binder caller identity](https://developer.android.com/reference/android/os/Binder#getCallingUid()).
Выбор allowlist, grants, limits и методов — наше предложение, а не гарантия платформы.
