# 04. Границы Hermes Mobile и Android-дополнения

[ADR-003](../adr/ADR-003-OPTIONAL-ANDROID-ADDON.md) имеет приоритет: ниже DESIGN для трёх APK, не разрешение реализации. Основной клиент содержит только generic broker/consent, add-on — hardware UI/service/OCR. Никаких Rokid permissions или SDK в Hermes.

Опора: существующие UI, navigation, typed RPC, auth и storage проекта; [S01–S04](../SOURCES.md). Предлагаемые имена новых типов — проектные, существующие найти в T00.

## 1. Минимальный seam в существующем чате

Найти путь: composer → ViewModel → repository → backend → events → persisted messages. На этой границе добавить `AgentConversationPort` или небольшой адаптер; не давать дополнению доступ к WebSocket singleton или VPS. IPC draft делегирует существующему composer pipeline; repository не считается готовым без source audit.

В `PreparedTurn` зафиксировать выбранный conversation/account scope и происхождение `PHONE`/`GLASSES`. Подключить фото очков к существующей модели вложений. Если upstream модель не допускает origin/observation, добавить совместимые nullable/default поля с Room migration. Не вводить параллельный список сообщений Rokid.

Отправка с телефона и очков идёт через один очередь/координатор. Захват с очков при закрытом composer либо использует явно закреплённый разговор, либо создаёт локальный draft. Не выбирать автоматически «последний разговор вообще», особенно после смены профиля.

## 2. UI

В Hermes только «Расширения»: identity, grant на выбранный разговор, права, отзыв и draft confirmation. Rokid device/mode/font/connection/model-download UI из таблицы живёт в add-on; chat/recovery cards — в Hermes. Не добавлять второй chat screen.

| Экран/элемент | Содержимое | Отказ/пустое состояние |
|---|---|---|
| Настройки → Rokid | Enable, устройство, совместимость, mode, шрифт/область, privacy, diagnostics | Пояснение permission/firmware/protocol blocker |
| Connection panel | Bluetooth и VPS показаны отдельно | Очки могут быть connected, а агент offline |
| Composer action | Сделать снимок, выбрать «перевод»/«спросить агента» | Busy, unsupported camera, no permission |
| Observation card | Фото, OCR, локальный перевод, статус отправки, выбранная сессия | Capture error, unreadable, model missing |
| Display controls | Закрепить разговор, вывести ответ, стоп/очистить | Недоставленный ответ остаётся в чате |
| Recovery card | Accepted/running/unknown; проверить состояние | Никакой кнопки «retry» без объяснения риска повторения |
| Setup/download | OCR и translation модели, размер, сеть, progress | Можно пользоваться обычным чатом без моделей |

Существующий внешний вид, русские строки и accessibility conventions сохранить. Статусы не кодируются одним цветом. Длинные тексты и ошибки должны помещаться на внешнем экране OnePlus Open. Проверить раскрытие/складывание во время capture и stream, landscape, font scale 1.3/1.5, screen reader, отсутствие повторного запроса после recreation.

Голосовые/видеопункты MVP либо отсутствуют, либо явно помечены недоступными; не оставлять работающую на громкость кнопку «wake word».

## 3. Foreground service и lifecycle

В отдельно устанавливаемом add-on предлагаемый `RokidConnectionService` нужен только во включённом пользовательском сеансе работы с очками. Запуск из видимого UI, persistent notification со статусом и действиями «отключить», «открыть», при необходимости «очистить дисплей». `connectedDevice` используется для реального поддержания связи; разрешения и runtime prerequisites сверяются с target SDK [S10–S12].

Не объявлять mic/camera permissions телефона только потому, что они есть на очках. Передача уже записанного на очках файла не равна использованию микрофона телефона. Для будущей записи на самом телефоне нужен отдельный разрешённый lifecycle и microphone service type.

Foreground service не делает процесс бессмертным. Существующий outbox Hermes хранит судьбу server operations; add-on восстанавливает только device state без второй chat DB. После force-stop или явного Stop пользователем приложение не обещает автоматический запуск. Boot не начинает камеру/микрофон и не отправляет накопленные поручения.

WorkManager использовать только для подходящих конечных задач cleanup/доставки, когда это безопасно по контракту. Не пытаться поддерживать SPP socket, постоянную запись и бесконечный агентный stream через цепочку workers.

Нужно различать условия: Activity stopped, screen off, Doze, process killed, force-stop, permission revoked, device disconnected. Для каждого — отдельный тест и документированный результат.

## 4. Разрешения

Разрешения запрашиваются по функции и версии ОС, не все на первом запуске. Для Bluetooth на Android 12+ проверить `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN`, необходимость advertising и поведение старых версий; companion association не отменяет автоматически всех требований. Список pairing устройств и serial не отправлять в telemetry [S11].

В телефонном add-on (не Hermes): Bluetooth + notification для активного режима; camera только для дополнительной телефонной съёмки; microphone только для отдельной voice функции. На очках: их собственные camera/mic permissions. Не требовать root, Accessibility Service, device admin, overlays или полного доступа ко всем файлам для этой архитектуры.

Не отключать системную оптимизацию батареи автоматически. Возможная инструкция для OxygenOS появляется только после воспроизведённой проблемы и с объяснением trade-off; базовая функция не должна скрывать её зависимость.

## 5. Хранилище

Переиспользовать текущую Room database и механизм защиты credential material. Сначала исследовать фактическое шифрование DB/backup, не считать название библиотеки достаточным доказательством.

Device settings, временные фото/OCR и bounded display projection принадлежат add-on. Mapping conversation/backend и pending server operations остаются в существующем Hermes store. Новая schema не предполагается заранее; только обоснованная совместимая migration. В таблицах — URI/relative private file references, размер/hash/status, но не большие blobs/base64.

Снимок сначала хранится app-private, после валидации появляется карточка. Фото не публикуется в системную галерею без отдельной команды. В backup исключить credentials, raw media и cache; поведение restore тестировать. Оригиналы локально по умолчанию хранить 7 дней, производные текстовые аннотации — с политикой истории пользователя. Это предлагаемый default, отображаемый в настройках, не тихая серверная политика.

У cloud uploads собственная retention/deletion семантика: локальное удаление не выдавать за подтверждённое удаление на VPS/у провайдера. UI показывает, что именно удалено.

## 6. Авторизация и scope

Не создавать второе хранилище ключей внутри feature. Только Hermes использует существующий auth слой. Дополнение и очки никогда не получают credentials; IPC grant не является VPS credential.

Logout: disconnect/deny new submission; остановить sensitive output; очистить pending display/approval intents; зафиксировать unresolved remote operations в старом scope без повторной отправки. Profile switch создаёт новый scope для будущих действий, не перепривязывает старые. Auth refresh не должен повторить сам prompt.

Ссылка/домен из OCR не становится автоматическим server URL. Redirect с credentials на другой origin запрещён. Нельзя добавлять permissive trust manager или отключать certificate verification ради быстрого теста.

## 7. Перенос donor code

Переносить transport, transfer, display config, camera companion и протокольные DTO с происхождением каждого файла. Не переносить donor conversation DB, AI provider registry, системный промпт с часами, local Gemma runtime, Gemini Live или wake service в MVP.

Зависимости CXR допускаются только внутри отдельного add-on/companion; основной Hermes build всегда собирается без CXR и Rokid repositories. Если работающий SPP путь всё-таки требует CXR для bootstrap, это фиксируется G1, а не скрывается за утверждением «SDK не нужен».

## 8. Feature-off и тестируемость

Выключенный Rokid не сканирует устройства, не держит service/wakelock, не просит hardware permissions и не меняет отправку чата. Основной Hermes APK всегда исключает аппаратные/proprietary зависимости Rokid; отдельного core/rokid flavor нет.

Fake glasses backend должен воспроизводить задержки, disconnect, stale events, фото и malformed frames. Fake agent transport — accepted/no ACK, partial text, late terminal, denial и expiry. Системные clock/file/network интерфейсы внедряемы для deterministic tests; не использовать реальные таймеры на десятки секунд в unit suite.
