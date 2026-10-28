# 09. Тесты, метрики и acceptance

Все численные пороги ниже — предложенные engineering targets версии плана 1.0, не результаты текущего приложения. На G0/G1 зафиксировать dataset, оборудование и метод измерений. Менять критерии только через ADR и явное принятие владельцем.

## Пирамида тестов

Unit: codecs, reducers, backpressure, state transitions, scope binding, display pagination, retry policy, image bounds, fake-clock timeouts.

Contract: golden phone↔glasses fixtures на обоих концах; выбранные backend requests/results из pinned схем; unsupported version/capability; RPC auth failures; SSE parsing только если выбран соответствующий transport.

Android integration: Room migrations, process restore, foreground service lifecycle, permission denial/revocation, fake Bluetooth/agent, Compose UI. Существующий regression suite Hermes Mobile обязателен.

Staging E2E: реальный Hermes с безопасным профилем и синтетическими изображениями; отключение сети/процесса; поток/история/approvals; неисполнение side effects дважды.

Hardware: реальные OnePlus и Rokid. Эмулятор не проверяет RF, Camera2 этой прошивки, читаемость дисплея, battery и OxygenOS pocket behavior.

## Матрица функциональных тестов

| ID | Сценарий | Ожидаемый результат | Gate |
|---|---|---|---|
| HW-01 | Install/start companion через штатный developer path | APK запускается, есть версии/permissions evidence | G1 |
| HW-02 | 20 текстов RU/ZH/числа/страницы | Корректный видимый текст; без сбросов | G1 |
| HW-03 | 10 снимков с передачей | Все checksum/размеры совпадают; кадр пригоден для текста | G1 |
| HW-04 | 5 reconnect и штатное приложение после теста | Связь восстанавливается, normal use сохранён | G1 |
| NET-01 | Login/native text/photo через HTTPS | Один turn/ответ, доступ только после auth | G2 |
| NET-02 | Attach A, switch session B, submit | A не уходит в B; scope закреплён | G2/G4 |
| NET-03 | Expired auth, WS reconnect | Re-auth без повторного prompt | G2/G7 |
| CTX-01 | Фото A → вопрос с телефона | Тот же server session/история/attachment | G4 |
| CTX-02 | Фото B → сравнение с A | Доступны A/B согласно documented strategy | G4 |
| CTX-03 | Process death и открытие разговора | IDs/история восстановлены, дублей нет | G4 |
| CTX-04 | Другой разговор/profile | Контекст/фото A/B не попадают туда | G4 |
| DSP-01 | Длинный stream | Промежуточный текст до terminal | G5 |
| DSP-02 | Чтение страницы 2 во время stream | Page anchor не сбрасывается | G5 |
| DSP-03 | Duplicate/out-of-order/stale stream | Текст не дублируется и не откатывается | G5 |
| DSP-04 | Disconnect до final, reconnect | Последний snapshot или честный delivery failure | G5 |
| OCR-01 | Чистая печать zh → ru | OCR/перевод соответствуют target качеству | G6 |
| OCR-02 | Сложный/нечитаемый кадр | Предупреждение/пересъёмка, не выдуманный результат | G6 |
| OCR-03 | Airplane mode после restart | Локальный перевод работает при готовых моделях | G6 |
| OCR-04 | Модель отсутствует/не загрузилась | Setup action, нет скрытого cloud fallback | G6 |
| OCR-05 | VPS завис, local translation | Полезный перевод не ждёт Hermes | G6 |
| OCR-06 | Agent response приходит во время чтения | Не вытесняет выбранный перевод самовольно | G6 |
| REC-01 | Kill между SUBMITTING и ACK | UNKNOWN/reconcile, ноль автодублей | G7 |
| REC-02 | Kill после ACK до final | Recover state/history, не resubmit | G7 |
| REC-03 | Token expiry, replay truncation | Точный scope, refetch без склейки по тексту | G7 |
| REC-04 | BT/network flaps, media ACK потерян | Нет повторного capture/agent request, bounded queues | G7 |
| REC-05 | Cancel/late terminal | Cancel requested не равно cancelled; история согласована | G7 |
| REC-06 | Force-stop/reboot/permission revoke | Честное состояние, ручной resume где требуется | G7 |
| SEC-01 | Wrong/no credentials | Нет config/history/private stream | G2/G7 |
| SEC-02 | Wrong profile/session ID | Reject без раскрытия соседних данных | G2/G7 |
| SEC-03 | Prompt injection в OCR | Данные не становятся policy/approval | G7 |
| SEC-04 | Local-only + packet inspection | Нет отправки изображения/OCR/перевода | G6/G7 |
| SEC-05 | Diagnostics/APK scan | Нет ключей, raw media и secret logs | G7/G8 |
| SEC-06 | Несовместимая/недоверенная BT сторона | Отказ/подтверждение pairing; нет sensitive display | G7 |
| OPS-01 | До/после изменения VPS: Telegram | Безвредные запросы проходят | G2/G8 |
| OPS-02 | Остальные агенты/сервисы | Нет перезапуска/изменения/нового доступа | G2/G8 |
| OPS-03 | Проверка внешних портов/route | Только согласованный TLS ingress | G2/G8 |
| OPS-04 | Rollback APK/config | Baseline работает, данные не удалены | G8 |
| UI-01 | Fold/unfold/rotate/font scale | Состояние сохранено, submit не дублируется | G3/G7 |
| UI-02 | Rokid off/core variant | Обычный чат не регрессирует; hardware background work нет | G3/G8 |

## Failure-injection campaign

Для каждого критичного boundary выполнить минимум 10 повторов с контролируемым моментом отказа: до записи ledger, после записи до send, после send до ACK, после ACK до текста, между event и DB commit, после terminal до display ACK. Для side effects применять безопасный staging counter tool с журналом operation IDs, не реальные покупки/удаления. Если production toolset запрещает такой fixture, использовать отдельный тестовый backend.

Gate требует ноль наблюдаемых дублей side effects, но это не математическая гарантия exactly-once. Зафиксировать число испытаний, покрытые boundaries и неподдержанные случаи. Одни happy-path тесты не удовлетворяют G7.

## Предлагаемые критерии качества

| Метрика | Target | Как считать |
|---|---|---|
| OCR CER, чистый поднабор | ≤ 8% суммарно | Levenshtein chars / total reference chars; политика whitespace/punctuation заранее |
| Числа/цены/единицы на чистых образцах | ≥ 95% точных полей | Сопоставление с заранее размеченными полями |
| Полезность RU перевода | ≥ 80% читаемых образцов с оценкой ≥ 4/5 | Человек оценивает смысл; имена/числа отдельно |
| Unreadable/blank subset | 5/5 корректно обозначены | Нет уверенного выдуманного перевода |
| Warm fast path после готового JPEG | p95 ≤ 3 s | PhotoStored → первый полезный translated frame applied |
| Still capture → полезный перевод | p50 ≤ 6 s, p95 ≤ 10 s | Нажатие → frame applied; включает camera/BT |
| App display overhead при готовом тексте | p95 ≤ 750 ms | Snapshot ready → glasses UI applied |
| Обычный streaming UI | Coalescing 250–500 ms, без накопления старых frames | Отдельно network wait и render time |
| Отправка эффекта после UNKNOWN | 0 автоматических повторов | Ledger + test backend execution log |
| Межпрофильные утечки | 0 | Scope/privacy suite |
| Crash/ANR в pocket campaign | 0 | Сценарий + device logs |

Низкое camera/BT быстродействие может сделать end-to-end target недостижимым на текущем donor. Тогда оптимизировать capture/transfer или открыто принять другой target до релиза; нельзя считать три секунды только OCR полным end-to-end временем.

## Измерения

Warm/cold разделены; исходные JPEG и версии моделей фиксированы. Не менее 30 повторов для latency отчёта, с p50/p95/max и количеством failures; для устойчивой оценки p95 при сравнении оптимизаций предпочтительно ≥100. На 10 кадрах G1 показывать все individual durations, не рекламировать точный p95.

Использовать monotonic clock внутри процесса/устройства. Время телефона и очков не вычитать напрямую без clock synchronization: network round-trip или локальные длительности по частям. После reboot epoch меняется. `frame applied` — инструментальный proxy, не оптическое измерение появления пикселей.

Hermes TTFT и total duration измеряются, но не задаются как гарантия мобильного клиента: зависят от модели/tools/сети. Обязателен быстрый честный local status и отсутствие блокировки fast path.

## Pocket/battery/thermal

60 минут, экран телефона погашен большую часть времени, минимум 30 пользовательских операций, управляемые BT/network разрывы и возврат. В журнале отметить foreground notification, battery policy и реальные OS states. Системный force-stop тестируется отдельно, не маскируется как recoverable RF outage.

Battery: минимум 3 парных прогона idle и active, одинаковая яркость/связь/начальный диапазон заряда, записанные длительность и фактический workload. Показывать raw battery delta и, если доступны, energy/thermal traces; проценты за 5 минут не экстраполировать в обещание полного дня.

При thermal warning, нестабильной камере или дискомфорте остановить испытание. Порог температуры не выдумывать; использовать доступные OEM/sensor показатели и документированные ограничения. При выключенном Rokid background overhead сравнивается с baseline и должен быть неразличим в пределах выбранного метода.

## Evidence bundle

Для gate: commit SHA всех трёх APK и IPC/wire versions, server version/image digest, firmware/OS, test dataset ID/hash, команды, начало/конец, result counts, sanitized logs, known limitations. Hardware видео/скриншот — с согласия, без людей/личных надписей. Failure должен быть воспроизводимым issue, не заметкой «иногда глючит».

## EXT — обязательные проверки отдельно устанавливаемого дополнения

Требования [ADR-003](../adr/ADR-003-OPTIONAL-ANDROID-ADDON.md), не результаты тестов.
Core lane использует synthetic fake APK, без donor/CXR; device installs требуют отдельного допуска.

| ID | Сценарий | Обязательный результат |
|---|---|---|
| EXT-01 | Add-on никогда не установлен | Core build/release без Rokid repositories; login/chat/history/attachments работают, нет Bluetooth permissions/scans/service и install nag |
| EXT-02 | Add-on установлен, но opt-in не дан / выключен | Нет grant, binding/side effects или утечки output; обычный чат идентичен baseline |
| EXT-03 | Отзыв в core / OS disable add-on во время copy и stream | Grant инвалидирован до commit/delivery, temp copy очищена, callback прекращён; ранее принятый turn не повторяется |
| EXT-04 | Uninstall/reinstall add-on, включая повторное использование UID | Чаты/auth Hermes сохранены; прежние handles/epoch не принимаются, новое явное согласие; TTL очищает экран очков |
| EXT-05 | Crash/ANR/force-stop/Binder death обеих сторон до/после ACK | Нет core ANR, zombie service/rebind loop или duplicate submit; UNKNOWN проверяется в core, старый grant не оживает |
| EXT-06 | N/N-1, major mismatch, capability missing; upgrade только одного APK | Совместимое additive evolution работает; несовместимость выключает дополнение, не ломает core; signer rotation fail closed |
| EXT-07 | Посторонний UID/package/signer, forwarded handle, spoofed package, debug key | Доступ отклонён на каждом IPC вызове; никакого списка сессий/секретов/данных до consent |
| EXT-08 | Смена conversation/profile/logout при приёме JPEG и queued callback | Старый scope закрыт; ни attachment, ни output не попадают в новый разговор; draft требует нового grant |
| EXT-09 | Invalid JPEG, oversized metadata/media, pipe stall, изменяемый FD, flood/slow consumer | Bounds/timeouts/очереди соблюдены, main thread не блокируется, temp files освобождены, обычный send работает |
| EXT-10 | Повтор operationId, другой payload, ACK loss, restart/expiry, reinstall | Одна staging operation, конфликт отклонён, auto-submit отсутствует; remote UNKNOWN не повторяется; receipt доступен только исходному caller/scope |

G3: все EXT сценарии с fake client + required real slice; G7: failure campaign с реальным
add-on/companion; G8: release-signed matrix и install/update/uninstall/rollback rehearsal.
Исходные Hermes unit/style/lint/release/Compose checks остаются независимыми обязательными
проверками. Core dependency/manifest scan запрещает Rokid/CXR/очковый codec и новые hardware
permissions; add-on scan запрещает VPS adapter/auth store/chat DB. Проверить отсутствие
секретов в IPC parcels/logs и history/tool/system messages в display projection.
