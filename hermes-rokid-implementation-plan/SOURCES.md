# Источники и границы подтверждения

План составлен 23 октября 2026. Использованы текущий разговор, приложенный пользовательский технический обзор и выборочно открытые первичные веб-страницы/исходники. Ссылки на moving `dev`/`main` — ориентиры; кодинг-агент обязан закрепить полный SHA в G0.

## Уровни уверенности

`USER_VERIFIED`: пользователь сообщил или показал вывод своего VPS.
`UPSTREAM_DOCUMENTED`: прочитано в первичном публичном документе/исходнике, но не проверено на его устройствах/сервере.
`HISTORICAL_REVIEW`: сведения из приложенного прежнего анализа, не повторённая здесь аппаратная/сборочная проверка.
`DESIGN`: предлагаемое решение/требование/численный target этого пакета.
`UNKNOWN`: требуется проверка.

## Первичные источники

| ID | Источник | Что он поддерживает в плане |
|---|---|---|
| S01 | [Hermes Mobile repository](https://github.com/Hy4ri/hermes-mobile) | Нативный клиент и подключение к Dashboard; это основной проект форка |
| S02 | [Hermes Mobile AGENTS.md](https://raw.githubusercontent.com/Hy4ri/hermes-mobile/dev/AGENTS.md) | Репозиторные правила, typed gateway calls, lifecycle и build conventions |
| S03 | [Gateway OpenRPC contract](https://raw.githubusercontent.com/Hy4ri/hermes-mobile/dev/app/src/test/resources/gateway-contract/openrpc.json) | Кандидаты `prompt.submit`, `image.attach_bytes`, session history/replay/resume. Схема не доказывает наличие на VPS |
| S04 | [Hermes Mobile DESIGN.md](https://raw.githubusercontent.com/Hy4ri/hermes-mobile/dev/DESIGN.md) | Сохранение существующих navigation/theme/UI conventions |
| S05 | [RokidAIAssistant repository](https://github.com/zero2005x/RokidAIAssistant) | Donor с отдельными common/phone/glasses частями; source provenance |
| S06 | [Hermes API Server](https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server) | API имеет собственные session ownership ограничения; поддержка images и idempotency зависит от режима/контракта |
| S07 | [Hermes Web Dashboard](https://hermes-agent.nousresearch.com/docs/user-guide/features/web-dashboard) | Auth/public URL/trusted proxy configuration; проверять по установленной версии |
| S08 | [Glasses setup](https://raw.githubusercontent.com/zero2005x/RokidAIAssistant/main/glasses-setup.md) | Companion устанавливается отдельно; sideload зависит от модели/firmware |
| S09 | [Donor device smoke log](https://raw.githubusercontent.com/zero2005x/RokidAIAssistant/main/doc/DEVICE_SMOKE_TEST.md) | Авторские hardware tests не равны тестированию пользовательских очков |
| S10 | [Android foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) | Типы foreground service и требования к real connected-device/mic/camera use |
| S11 | [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) | Версионные runtime permissions Bluetooth |
| S12 | [Android background FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) | Ограничения фонового запуска; нельзя предполагать произвольный автозапуск mic/camera |
| S13 | [ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2) | Поддерживаемые scripts; русский output не подтверждает Cyrillic OCR |
| S14 | [ML Kit OCR on Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) | Bundled/unbundled варианты и Android integration |
| S15 | [ML Kit Translation](https://developers.google.com/ml-kit/language/translation) | On-device translation, model management, ограничения качества и английский промежуточный язык |
| S16 | [Hermes Mobile build.gradle.kts](https://raw.githubusercontent.com/Hy4ri/hermes-mobile/dev/app/build.gradle.kts) | Реальные toolchain/signing/dependency настройки нужно переоценить на pinned SHA |
| S17 | [Donor GlassesCameraManager](https://raw.githubusercontent.com/zero2005x/RokidAIAssistant/main/glasses-app/src/main/java/com/example/rokidglasses/service/photo/GlassesCameraManager.kt) | Camera2 implementation как точка source audit, не гарантия latency |
| S18 | [Donor GlassesViewModel](https://raw.githubusercontent.com/zero2005x/RokidAIAssistant/main/glasses-app/src/main/java/com/example/rokidglasses/viewmodel/GlassesViewModel.kt) | Связь display/camera/protocol, кандидат на декомпозицию |
| S19 | [Hermes programmatic integration](https://hermes-agent.nousresearch.com/docs/developer-guide/programmatic-integration) | Различение доступных программных интерфейсов; не повод заводить лишний transport |
| S20 | [HermesWsClient](https://raw.githubusercontent.com/Hy4ri/hermes-mobile/dev/app/src/main/java/com/m57/hermescontrol/data/ws/HermesWsClient.kt) | Существующий WS client нужно использовать через актуальные typed interfaces |
| S21 | [Donor common build](https://raw.githubusercontent.com/zero2005x/RokidAIAssistant/main/common/build.gradle.kts) | `common` имеет Android build; перенос в pure Kotlin требует выделения кода |

Точные license files: [Hermes Mobile LICENSE](https://github.com/Hy4ri/hermes-mobile/blob/dev/LICENSE), [RokidAIAssistant LICENSE](https://github.com/zero2005x/RokidAIAssistant/blob/main/LICENSE), [NOTICE](https://github.com/zero2005x/RokidAIAssistant/blob/main/NOTICE). GitHub страницы обозначают Apache-2.0; перед переносом проверить файлы pinned revision и отдельные условия SDK/model dependencies. Это inventory-задача, не юридическое заключение.

## Пользовательский материал

[USER_REVIEW.md](sources/USER_REVIEW.md) — копия приложенного `Pasted text(8).txt` с пометкой происхождения и нейтрализацией недоступных sandbox-ссылок прежнего сеанса. Она описывает историческую проверку donor на `770bc8d` и обнаруженные ограничения. Эта подготовка плана не повторяла те эксперименты.

Для нового плана существенны: reusable photo/display/transport, отсутствие подтверждённой устойчивости audio/video, потребность связывать фото с разговором, отдельный fast path. Конкретные старые implementation details надо перепроверить, а не копировать в текущий source как факты.

## Уточнения по сравнению с прежним обсуждением

1. Второй OpenAI-compatible клиент для очков не нужен по умолчанию. Приоритет — native transport общего chat repository. S06 не разрешает произвольно привязать API session к чужой поверхности; совпадение ID не даёт общего разговора.
2. Нельзя обещать безусловное продолжение запроса после disconnect или universal exactly-once retry. Известная документация описывает разные режимы; реально установленный backend проверяется G2.
3. Встроенный supervisor/переменная запуска Dashboard в образе VPS не доказаны приведённым CLI help. Нужен аудит image/entrypoint.
4. Установка companion на конкретную китайскую прошивку, real pocket behavior и производительность не подтверждены. Gate G1/G7 обязателен.
5. Схемы новых модулей, контракты v2 и численные SLO — DESIGN, не обнаруженные готовые функции upstream.

## Что проверялось при подготовке этого пакета

Открыты перечисленные публичные документы и несколько исходных файлов. Git clone в рабочей среде не удался из-за DNS-доступа к GitHub, поэтому полный checkout, полный diff-аудит, фиксация независимого полного SHA, сборка, APK install и hardware test не выполнялись. Source snippets на moving branches могут изменяться и кэшироваться; поэтому ни одна веб-проверка не закрывает G0.

Не было доступа к VPS, Caddy config или устройствам пользователя; их состояние известно только из предоставленного терминального вывода. Детали, которые он не содержит, оставлены UNKNOWN. Документационная валидация пакета — отдельная проверка ссылок/структуры, не тест приложения.
