# 07. Общий контекст, persistence и восстановление

Уточнение 2026-10-28: [ADR-003](../adr/ADR-003-OPTIONAL-ANDROID-ADDON.md) задаёт optional APK и IPC. История/auth/outbox/серверный scope только в Hermes; hardware/OCR в add-on. В v1 только draft с подтверждением в Hermes, автоматический guide/submit требует отдельного решения. До review ADR и gates функциональная реализация запрещена.

## Один разговор — один владелец

Единица владения: `(backendOrigin, accountScope, profile, transportKind, serverSessionRef)`. Локальный `conversationId` отображается в неё, но не заменяет её. Никаких глобальных переменных `lastSessionId` вне scope.

Android и очки пользуются одним repository. Telegram остаётся отдельным каналом. Общая долговременная память может зависеть от настроек провайдера и scope; она не заменяет transcript, фотографии и порядок turns. Импорт/объединение Telegram — отдельная будущая задача, не часть G4.

## Фото является частью turn, а не только галереей

Сохранить Observation с изображением и происхождением до отправки. Составить один user turn: явный вопрос, media attachment, необязательная OCR-аннотация с пометкой недоверенного распознавания. Запись о локальном переводе не становится сообщением модели.

Для follow-up про A/B проверить backend persistence: хранится ли само изображение, доступная media reference или только текстовый summary. Правдоподобное описание не доказывает наличие визуального контекста. В тесте использовать синтетические карточки с различными объектами/числами, inspectable request traces без настоящих пользовательских данных и server history evidence.

Если backend теряет доступ к старому media, допустимы: подтверждённые attachment references, либо явное повторное включение выбранных прошлых изображений в новый вопрос. Такое reattachment не должно копировать старые user turns и обнулять server history. Стратегия описывается ADR и тестом. До этого G4 не PASS.

## Особо опасная граница: attach → submit

В существующем gateway есть операции, которые могут помещать изображение «к следующему сообщению» [S03]. G2 обязан определить scope этой очереди: session, connection или active focus.

Если attach не является частью атомарного submit, один coordinator сериализует последовательность `select/activate session → upload/attach → submit` в нужном scope. При connection-global очереди блокировка охватывает все конкурирующие отправки этого соединения, а не только один conversation. Запретить параллельную отправку из другого UI path в обход coordinator.

При failure между attach и submit — запросить состояние/detach именно своего attachment при наличии подтверждённой операции. Не очищать чужую очередь и не отправлять фото с пустым вопросом. При смене активного экрана request остаётся привязан к исходной сессии. Если это невозможно гарантировать на установленном сервере, G2/G4 BLOCKED до другого контракта.

## Минимальный локальный ledger

Предлагаемые поля, адаптируемые к existing Room schema:

```text
PendingOperation:
  operationId, conversationId, immutableBackendScope
  origin, observationIds, preparedPayloadHash
  state, submissionAttemptCount
  serverRequestRef?, serverSessionRef?
  lastServerCursor?, lastConfirmedRevision?
  createdAt, updatedAt, outcomeReason?, retryPolicy

Observation:
  observationId, captureId, conversationId, scope
  privateMediaRef, encodedBytes, dimensions, orientation, sha256
  ocrText?, ocrEngineVersion?, translationText?, translationEngineVersion?
  captureState, uploadState, serverMediaRef?, associatedUserTurnRef?

DisplayProjection:
  deviceRef, conversationId, operationId, streamId
  revision, mode, pageAnchor, final, deliveryState
```

Это дизайн полей, не обязательные новые таблицы. Переиспользовать upstream entities при совпадающей семантике. Создание operation и draft message делать транзакционно. Локальная Room не является единственным источником истины для уже принятого сервером выполнения.

## Порядок отправки

1. Проверить scope, consent, readiness и media integrity.
2. Зафиксировать payload и immutable target, создать PREPARED.
3. До сетевой отправки записать SUBMITTING.
4. Выполнить один подтверждённый submit/attach sequence.
5. Полученный ACK сохранить вместе с server references; ACK означает accepted, не completed.
6. События применять идемпотентно к конкретному operation/message. Сначала durable update, затем UI/render side effect.
7. Terminal outcome сохранять отдельно от текста. После завершения очистить временные buffers, но не нужное context media.

Payload при допустимом retry не должен менять системные инструкции, время, изображение, профиль или историю. Иначе это уже новая операция, даже если key остался прежним.

## Правила восстановления

| Сбой | Действие | Запрещено |
|---|---|---|
| Нет сети до начала отправки | Сохранить draft; показать offline | Самостоятельно отправить позже опасное поручение |
| TCP/WS оборвался после отправки, ACK нет | UNKNOWN; reconcile по подтверждённым методам | Новый request «на всякий случай» |
| Есть ACK, пропал stream | Reattach/replay/history snapshot | Повторный submit для получения ответа |
| Replay cursor устарел | Refetch authoritative state, merge по IDs | Считать отсутствие events отсутствием выполнения |
| Token expired | Re-auth того же scope, затем reconcile | Автоматически повторить исходную операцию |
| Bluetooth пропал | Сохранить latest display; агент может работать согласно contract | Перезапускать запрос к Hermes |
| Process death телефона | Читать ledger и состояние backend | Создавать новый server session для старого чата |
| Profile switch | Заморозить прежние pending ops в прежнем scope | Доставлять их в новый profile |
| Server restart | Проверить persistence и terminal/history | Объявлять выполнение потерянным только из-за reconnect |
| Cancel timeout | CANCEL_REQUESTED/UNKNOWN | Говорить «отменено», если подтверждения нет |

Обычный reconnect и повтор effectful request — разные действия. Повтор idempotent status/read безопаснее повторного поручения, но тоже должен иметь backoff и ограничение частоты.

## Идемпотентность и неизвестный результат

OpenAI-совместимый API документирует разные гарантии для разных режимов; это не переносится автоматически на native JSON-RPC [S06]. Для выбранного transport зафиксировать: dedup key, scope, payload equality, retention, restart behavior, conflict codes, streaming/non-streaming differences.

Если remote dedup/recovery отсутствуют, MVP показывает unresolved operation и предлагает проверить результат/историю. Повтор по явному действию пользователя объясняет риск и создаёт новую operation только после принятого решения. «Exactly once» не заявлять: сторонний инструмент тоже может завершить действие до потери связи/сохранения результата.

## Event reducer

Reducer keyed by scope + operation + message. Cursor/revision относится к конкретному stream/epoch; глобальное `lastSeq` для всего приложения недопустимо. Snapshot не добавляется к старому тексту как delta. Duplicate event не создаёт второй message. Out-of-order terminal не переоткрывает уже завершённую операцию без явного server revision.

Если backend не имеет cursor, не изобретать `Last-Event-ID`. Использовать документированный authoritative history/state read и стабильные IDs; без них отражать ограничение, не склеивать сообщения по совпадению текста.

Локальные user stop display и remote cancel — две разные команды. Очистка дисплея не отменяет серверный агент. Remote cancel не откатывает уже выполненные внешние действия.

## Обязательные интеграционные сценарии

A→follow-up→B→compare; два разговора с одновременными снимками; смена профиля во время upload; crash после SUBMITTING до ACK; crash после terminal до display; replay duplicate; stale cursor; auth expiry; сервер продолжает/не продолжает после disconnect — оба поведения отображаются честно; attachments исчезли/превысили retention; bounded queues при длительном offline.
