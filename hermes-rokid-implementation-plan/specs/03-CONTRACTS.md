# 03. Внутренние контракты, идентичность и wire protocol

Ниже — проектные контракты. Это не существующий Hermes API и не готовый patch. Реальные backend fields заполняются только после G2, реальные donor frames — после G1.

## Отдельная IPC-граница

[ADR-003](../adr/ADR-003-OPTIONAL-ANDROID-ADDON.md) задаёт внешний IPC v1. Таблицы и Kotlin-порты ниже — внутренние модели соответствующего владельца, не IPC payload. accountScopeId, conversationId и serverSessionRef остаются в Hermes; add-on получает opaque grant/epoch и operation receipts. GlassesPort реализуется только в add-on.

## Идентификаторы

| ID | Кто создаёт | Инвариант |
|---|---|---|
| `installationId` | Телефон при установке | Не является правом доступа |
| `accountScopeId` | Существующий auth/profile слой | Разделяет credentials и локальное хранилище |
| `conversationId` | Chat repository | Стабильный локальный UUID, не равен автоматически server ID |
| `serverSessionRef` | Backend adapter из server response | Включает endpoint/profile/owner/transport; opaque value |
| `operationId` | Телефон до любых side effects | Одна пользовательская операция, сохраняется до network I/O |
| `captureId` | Координатор съёмки | Один физический снимок; reconnect не создаёт новый |
| `observationId` | Repository | Фото + OCR + перевод + связь с user turn |
| `requestId` | Backend или подтверждённый protocol mapping | Не придумывать несуществующее remote поле |
| `streamId` | Coordinator per displayed answer | Поздний поток не перезаписывает более новый |
| `deviceSessionId` | Phone/glasses при handshake | Новое соединение имеет новую epoch |
| `commandId` | Sender до enqueue команды | Дедупликация capture/display, не LLM execution |

ID не дают авторизацию. Имя контейнера, UUID разговора и profile string не должны позволять прочитать чужую историю.

## Внутренние порты — эскиз Kotlin

```kotlin
// Design sketch: names and supporting types are not upstream APIs.
interface GlassesPort {
    val state: StateFlow<GlassesConnectionState>
    val events: Flow<GlassesEvent>
    suspend fun connect(device: PairedDeviceRef): ConnectResult
    suspend fun capture(request: CaptureRequest): CaptureTicket
    suspend fun render(frame: DisplayFrame): DeliveryResult
    suspend fun disconnect(reason: DisconnectReason)
}

interface AgentConversationPort {
    suspend fun capabilities(scope: AccountScope): VerifiedCapabilities
    suspend fun submit(turn: PreparedTurn): SubmissionResult
    fun observe(conversation: ConversationRef): Flow<AgentEvent>
    suspend fun reconcile(operation: OperationRef): ReconcileResult
    suspend fun cancel(operation: OperationRef): CancelResult
}

interface FastVisionPort {
    suspend fun recognize(image: LocalImageRef, options: OcrOptions): OcrResult
    suspend fun translate(text: RecognizedText, target: Language): TranslationResult
}
```

`CaptureTicket` означает принятую команду, не готовый JPEG. `SubmissionResult` различает rejected, accepted и outcomeUnknown. `CancelResult` различает unsupported, requested и confirmed. Тяжёлые image bytes передаются через app-private file reference/stream, не через глобальный `StateFlow<ByteArray>` или строки состояния UI.

## Канонические события внутри приложения

`CaptureRequested`, `CaptureStarted`, `PhotoStored`, `OcrReady`, `TranslationReady`, `SubmissionAccepted`, `AssistantTextSnapshot`, `ToolProgress`, `ApprovalRequired`, `TurnCompleted`, `TurnFailed`, `CancelRequested`, `TurnCancelled`, `DeliveryFailed`, `OutcomeUnknown`.

Каждое событие имеет scope, conversation, operation/capture reference и источник. `AssistantTextSnapshot` содержит полный накопленный текст конкретного message/stream, revision и terminal flag. Backend delta нормализуется адаптером; display слой не разбирает JSON-RPC/SSE.

Статус tools из модели в обычном тексте не считать настоящим `ToolProgress`. System/tool payload на очки автоматически не отправлять.

## State machine запроса

```text
PREPARED → SUBMITTING → ACCEPTED → RUNNING → COMPLETED
                │           │         ├→ WAITING_APPROVAL → RUNNING
                │           │         ├→ FAILED
                │           │         └→ CANCEL_REQUESTED → CANCELLED / RUNNING / UNKNOWN
                └───────────┴────────────→ UNKNOWN → reconciled terminal or manual resolution
```

`SUBMITTING` записывается до первого сетевого байта. Crash между отправкой и ACK приводит к UNKNOWN, а не PREPARED. Transaction фиксации outcome и event cursor атомарна с соответствующим message update. `COMPLETED` требует явного terminal outcome; закрытый stream или полученный final text сами по себе недостаточны.

## State machine соединения очков

`Disabled → PermissionNeeded → Disconnected → Connecting → Negotiating → Ready → Reconnecting → Ready`.

Также `Incompatible`, `DeviceBusy`, `UserStopped`, `PermissionRevoked`. `UserStopped` не запускает автоматический reconnect. Reconnect имеет backoff/jitter и верхнюю границу; при logout очистить pending sensitive display jobs. Не смешивать Bluetooth connection state и backend auth state.

## Legacy wire и расширение v2

G3 сначала работает с donor-протоколом без несовместимых изменений. Нельзя просто отправить `HELLO_V2` неизвестному парсеру: сначала исследовать допустимый discovery/handshake и поведение на unknown messages. При отсутствии безопасного negotiation использовать явно выбранный парный protocol version и fail-closed экран «обновить оба приложения».

Проектный v2 envelope:

```json
{
  "protocolVersion": 2,
  "deviceSessionId": "opaque-epoch",
  "commandId": "uuid",
  "type": "DISPLAY_SNAPSHOT",
  "payload": {
    "streamId": "uuid",
    "revision": 12,
    "mode": "TRANSLATION",
    "text": "Пример короткого текста",
    "final": false
  }
}
```

Это пример собственного phone↔glasses контракта, не JSON для Hermes. Поля и ограничения фиксируются golden fixtures. Companion объявляет поддержку capture, display snapshot, page controls, media limits, microphone; отсутствующее capability считается false.

Сначала snapshots, затем при доказанной необходимости deltas. Очки принимают только новые revisions текущего stream/device epoch. Пропущенные промежуточные snapshots допустимы; финальный snapshot и error не теряются без явного delivery failure. При reconnect телефон посылает актуальный snapshot и display settings, а не повторяет старую очередь токенов.

## Photo transfer

Сохранить работающий donor chunk/ACK/retry механизм, обернув его correlation IDs. Начальные проектные bounds для нового протокола: JPEG до 8 MiB, decoded image до 32 MP, metadata до 4 KiB, frame до 64 KiB; рабочий chunk size подобрать по G1, не менять legacy без тестов. Размеры — защита ресурсов, а не рекомендуемый вес каждой фотографии.

Приём: проверить transfer ID/epoch, общий размер, offset/chunk range, отсутствие overflow; писать в temp file; проверить фактическую длину и SHA-256; безопасно проверить MIME/размеры/ориентацию; atomic rename; только затем `PhotoStored`. Hash подтверждает целостность, не подлинность отправителя.

Ограничить число одновременных transfer (MVP: один), число повторов и TTL temp files. Повторный ACK/кусок не создаёт вторую фотографию. После потери ACK capture-команда с тем же ID не должна снимать заново; cached result/status или честный `unknown_capture`.

Base64 увеличивает размер HTTP/RPC payload: лимиты backend/proxy учитывать отдельно. Не загружать целиком несколько больших кадров в память. Не передавать фотографии через generic log/events.

## Приоритеты и backpressure

На Bluetooth control/errors/final display выше image chunks, промежуточные display snapshots coalesced до последней revision. Bounded queue; переполнение даёт явный busy/backpressure, не бесконечное накопление. Камера однозадачна; второй capture не перепутает результаты первого.

Для request retry server-side идемпотентность допускается только после конкретной проверки выбранного метода/версии, credential scope, retention и streaming mode. Наличие локального `operationId` не даёт exactly-once execution на сервере.

## Минимальные contract tests

Round-trip и malformed fixtures; oversized metadata/media; duplicate chunk/ACK; interrupted transfer; unknown version; old companion; stale epoch; UTF-8 boundary; out-of-order display revision; session switch mid-transfer; profile logout; accepted-but-no-ACK submit. Каждый sender/receiver должен проходить те же fixtures, а не две независимо написанные несовместимые интерпретации.
