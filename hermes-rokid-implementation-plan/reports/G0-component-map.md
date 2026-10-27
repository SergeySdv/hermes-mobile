# T00 — карта переиспользования на pinned source

Дата: 2026-10-27. Уровень фактов: UPSTREAM_DOCUMENTED (прочитанные исходники),
не проверка устройства/VPS. SHA и результаты сборки: [G0-baseline](G0-baseline.md).
Пути Hermes ниже относительно `app/src/main/java/com/m57/hermescontrol/`.

## Hermes Mobile

| Область | Реальное место | Решение для будущей интеграции |
|---|---|---|
| Composer / submit | `ui/chat/ChatScreen.kt`, `ChatViewModel.kt:3473` (`sendMessage`), `:3823` (`dispatchPrompt`) | Использовать действующий pipeline; не отправлять параллельный RPC из Bluetooth callback. Здесь проверки готовности, duplicate guard, busy queue, attachments, receipt и сохранение сообщения. |
| Владелец разговора | `ChatViewModel.kt:2725` (`sendScope`, `SendOwner`, `withCurrentSendOwner`); `data/session/ActiveSessionHolder.kt` | Scope включает endpoint, connection profile, agent profile; storage session отличается от runtime session. Сохранить generation fencing. Один UUID не связывает Android с Telegram. |
| Repository | `ui/chat/ChatPersistenceRepository.kt` | Реальный repository сохраняет/загружает сообщения, страницы, canonical identity и local anchors. Это НЕ готовый общий repository отправки/наблюдения из плана. Оркестрация остаётся в ViewModel; не создавать второй chat store. |
| Асинхронное добавление фото | `ui/chat/ChatAttachmentTarget.kt`, `ChatViewModel.captureAttachmentTarget()` / `addPastedAttachments()` (`:4237`) | Наиболее узкий существующий seam для снимка: захватить session/generation/baseUrl/два profile ID до I/O, отказать при несовпадении после I/O. Сейчас API internal и рассчитан на composer, не на background service. |
| Android share intake | `share/ShareInbox.kt`, `ShareStaging.kt`, `ShareTargetActivity.kt`, `ChatViewModel.stageSharedContent()` | Уже есть bounded app-private staging и ручное подтверждение через composer. Однако inbox ждёт будущий подключённый чат без заранее закреплённого scope: не использовать напрямую для автоматически отправляемого снимка очков. Переиспользовать файловые helpers, сохранить capture-time target. |
| Файлы / retention | `ChatPastedImageStore.kt`, `ChatAttachmentsDelegate.kt`, `ChatViewModel.kt:3214`, `AttachmentBase64Encoder.kt`, `data/model/Attachment.kt` | Ввод временно в app cache, очередь копирует файл в `filesDir/chat-send/<message-id>`; recovery использует durable file URIs. Есть release/cleanup snapshots. Не хранить фото только в StateFlow/ByteArray; не считать cache долговременной историей. Retention на сервере UNKNOWN. |
| Image upload | `ChatViewModel.kt:3997`, `data/ws/contract/ChatCommands.kt` | `IMAGE_ATTACH_BYTES` с `session_id`, `content_base64`, `filename`, `ext`, затем существующий submit. Проверяется `attached`; операция не атомарна по одному факту наличия двух RPC. Подтверждение semantics — G2. |
| Очередь / uncertain outcome | `ui/chat/ChatSendStore.kt`, `ChatPersistedReceipts.kt` и reconciliation в ViewModel | SharedPreferences outbox с QUEUED/PARKED/SENDING/ACCEPTED/UNKNOWN/REJECTED. После crash SENDING становится UNKNOWN; безусловный auto-retry запрещён. Сохранить exact REST row identity и reconciliation. |
| Streaming / display | `ui/chat/ChatStreamingController.kt`, `ChatWsEventReducer.kt`, `ChatViewModel.uiState`, `data/ws/ReplayTracker.kt` | Будущий дисплей получает отфильтрованный accumulated assistant snapshot выбранного разговора, а не второй socket и не сырые tool/auth events. Background lifetime нужно отдельно спроектировать узким адаптером, не удерживать screen ViewModel случайно. |
| Gateway | `data/ws/HermesWsClient.kt`, `WsTransport.kt`, `RpcChannel.kt`, `WsProfileParams.kt`, `contract/RpcMethod.kt` | Существующий singleton, typed descriptors, profile decoration, correlation/replay. `RpcMethods` находится в `RpcMethod.kt`, не в воображаемом новом API. |
| REST / auth | `data/remote/ApiClient.kt`, `OkHttpProvider.kt`, `TokenRefreshAuthenticator.kt`, `data/local/AuthManager.kt`, `ui/authlogin/AuthLoginViewModel.kt` | Сохранить token/cookie modes, refresh, custom TLS/headers. REST bearer применяется для token mode; cookie для gated mode. `WsTransport` запрашивает `api/auth/ws-ticket`; не передавать настройки/credentials очкам. |
| Смена профиля | `data/session/ProfileSwitchCoordinator.kt` | Сериализует смену, обновление auth/REST, события инвалидирования и reconnect. Новый coordinator должен отменять capture/display/pending intent на смене контекста. |
| Logout | `ui/settings/SettingsViewModel.kt:462` | Найден конфликт: credentials очищаются ДО disconnect, тогда как AGENTS требует обратный порядок. В T00 не исправлялось; отдельный review/узкий regression fix до подключения долгоживущего I/O. |
| Persistence | `data/local/HermesDatabase.kt`, `ChatMessageDao.kt`, `ChatMessageEntity.kt`; `app/schemas/.../11.json` | Room 3 / SQLCipher, schema 11, migrations 2→3…10→11. Не создавать новую историю и не менять schema в T00. Entity/DAO индексируют историю по sessionId; полноценную account isolation нельзя объявлять доказанной одним outbox scope — проверить G2/G4. |
| Navigation / theme | `NavigationController.kt`, `NavigationKeys.kt`, `ScreenRegistry.kt`, `ui/common/HermesScaffold.kt`, `theme/Theme.kt`, `ThemeRegistry.kt`, `PaletteTemplate.kt` | Расширять существующие settings; navigateTo(), screen-owned drawer gestures, semantic tokens. Новые UI не нужны для baseline. |
| Фон | `notification/ChatNotificationService.kt`, `BackgroundConnectionController.kt`; AndroidManifest.xml | Существующий FGS имеет тип remoteMessaging. Нет Bluetooth permissions/connectedDevice FGS. Нельзя считать этот сервис готовым transport service для очков; добавить отдельно только после G1/G2 и scope/lifecycle review. |

## Контракт и существующие тесты

Snapshot `app/src/test/resources/gateway-contract/openrpc.json`: 257 методов;
SHA-256 `248ce12519dcc1e830a15c5819c93e6d27ef18404d19da38c960570710f83d00`.
`SOURCE` указывает Hermes Agent `4d04303818b61bf3aa09278af48356315515965b`, 2026-10-21.
Это provenance фикстуры, НЕ версия пользовательского VPS.

Проверены реальные имена `prompt.submit`, `image.attach_bytes`, `file.attach`,
`session.resume`, `session.events.since`, `session.interrupt` и их descriptor/params.
`PromptSubmitParams`: session_id, text, optional queued; здесь нет универсального
idempotency key. `session.interrupt` не означает подтверждённую отмену tool side effects.

Переиспользовать `GatewayContractTest`, `GatewayContractFramesTest`,
`HermesWsClientTypedSendTest`, `ReplayTrackerTest`, `ChatSendStoreTest`,
`AttachmentBase64EncoderTest`, `ProfileSwitchCoordinatorTest`,
`HermesDatabaseMigrationsTest` и Compose recovery/attachment tests.

Второй конфликт с AGENTS: `RawRpcParamsRatchetTest.BASELINE` содержит четыре
`CLARIFY_RESPOND` call sites (строки 316–322), хотя инструкция требует пустой baseline.
Не скрывать allowlist и не объявлять typed migration завершённой. Это исходное состояние,
не причина добавлять новые raw вызовы для Rokid.

## Donor map

Пути относительно donor; phone prefix:
`phone-app/src/main/java/io/github/zero2005x/glassesaicompanion/`, glasses prefix:
`glasses-app/src/main/java/com/example/rokidglasses/`.

| Кандидат | Source | Граница переноса |
|---|---|---|
| Envelope / DTO | `common/.../protocol/Message.kt`, `MessageType.kt`, `ConnectionState.kt` | JSON id/type/timestamp/payload/binaryData; unknown type возвращает null. Message использует android.util.Base64 / org.json, common — Android library minSdk 28. Не объявлять pure JVM без выделения и fixture tests. |
| SPP | phone `service/BluetoothSppManager.kt`, glasses `service/BluetoothSppClient.kt` | Телефон RFCOMM server, очки client; общий service UUID. Есть insecure/secure SDP и channel fallback. Сохранить framing и сериализацию writes; compatibility/trust проверять G1. |
| Фото | glasses `service/photo/PhotoTransferProtocol.kt`, phone `service/photo/BluetoothPhotoReceiver.kt`, `PhotoRepository.kt`; common `protocol/photo/PacketUtils.kt`, `PhotoTransferConstants.kt` | START/DATA/END + ACK/RETRY, CRC32 чанков и MD5 целого фото; max photo 5 MiB, max chunks 2048, retry 3, ACK timeout 5 s. Это integrity, не authentication. Не заменять legacy предел на проектные 8 MiB без тестов. |
| Камера | glasses `service/photo/GlassesCameraManager.kt`, `CameraService.kt`, `sdk/UnifiedCameraManager.kt` | Camera2, YUV ImageReader → JPEG, выбор размера/ориентации и lifecycle. UnifiedCameraManager выбирает Camera2; это не доказательство доступа на китайской прошивке. |
| Дисплей | glasses `viewmodel/GlassesViewModel.kt`; common `GlassesDisplayConfig.kt`, `GlassesDisplayMetrics.kt` | Реальные pagination/currentPage, настройки шрифта/страницы, поток UI. Выделять только display policy; весь ViewModel содержит чужую AI-оркестрацию. Перепагинация иногда сбрасывает currentPage — не обещать G5 anchor stability. |
| CXR | phone `githubImplementation(client-m:1.2.2)`; glasses `cxr-service-bridge:1.0-20250519.061355-45` | Play phone flavor исключает CXR, glasses build всё ещё зависит от bridge. Необходимость SDK для конкретного Camera2/SPP пути UNKNOWN до G1. |

НЕ переносить donor phone chat/history/Room, AI provider catalog, Gemini/OpenAI network
clients, developer key BuildConfig, prompts, собственную navigation/settings систему,
облачную обработку фото, audio/video и весь phone-app. Полный donor phone APK допустим
лишь в отдельно разрешённом hardware spike, не как второе постоянное приложение.

## Feature-off и минимальный seam

Baseline состоит только из `:app`; Rokid flag/flavors/modules пока не существуют.
Feature-off означает отсутствие новых permissions, Bluetooth scan/connect, camera,
сервисов, зависимостей CXR и изменений обычной send/history/auth логики. Это source
baseline; поведенческий PASS требует сборки и regression suite.

После G0/G1/G2 минимальный функциональный кандидат: узкий scope-bound метод приёма
app-private фото рядом с existing attachment target, делегирующий текущему composer/send
pipeline. Сначала fake input и тесты late photo после session/profile switch/logout,
oversized/unreadable file и отсутствие auto-resubmit UNKNOWN. Не вводить второй
repository/WS/Room и не переносить все 7k строк ViewModel ради интерфейса. До этих gates
разрешено только завершение T00 и документирование конфликтов.
