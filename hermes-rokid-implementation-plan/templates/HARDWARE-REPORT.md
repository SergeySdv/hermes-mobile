# Hardware report — реальные OnePlus и Rokid

Дата/оператор: …; gate: …; status: NOT_RUN.

## Устройства и версии

Phone model/OS/build: …
Glasses exact model/firmware/Android API: …
Official Rokid app/version/регион: …
Phone/glasses APK version/hash/signer: …
Source SHAs/protocol version: …
Serials: только в приватном журнале, в отчёте анонимные Device-P/Device-G.
Разрешённые действия владельца: …

## До начала

Штатное pairing/camera/display поведение, какие приложения запущены, baseline settings, заряд/brightness/сеть, восстановление после теста. Способ developer/ADB access; никаких неподтверждённых инструкций прошивки.

## Проверки

| Check | Trials | Success | Failure | Evidence |
|---|---:|---:|---:|---|
| Text RU/ZH | 20 | NOT_RUN | NOT_RUN | — |
| Still photo integrity | 10 | NOT_RUN | NOT_RUN | — |
| Reconnect | 5 | NOT_RUN | NOT_RUN | — |
| Camera busy / permission denial | … | NOT_RUN | NOT_RUN | — |
| Screen off/pocket | … | NOT_RUN | NOT_RUN | — |
| Official app restored | 1 | NOT_RUN | NOT_RUN | — |

## Латентность и качество

Сырые individual capture/transfer/display times, метод clock measurement, warm/cold, encoded size/resolution, readable text sample. Hash отправленного/принятого JPEG. Не сравнивать clocks двух устройств без синхронизации; не объявлять точный p95 на десяти измерениях.

## Реально используемый тракт

Camera2/CXR/другой: …; SPP/другой: …; нужен SDK bootstrap/secret: …; протокол и ограничения firmware: …
Не делать вывод «CXR не нужен» только по одному импортированному классу.

## Вывод

Supported tested scope, blockers, следующий безопасный шаг, restoration completed, gate reviewer. Визуальное подтверждение читаемости оператором и сохранение штатной работы обязательны.
