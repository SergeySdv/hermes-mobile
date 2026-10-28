# 02. Gates — допуск между этапами

Gate — проверяемое условие, а не дата и не процент выполненного кода. Все gates изначально `NOT_RUN`. Протокол доказательств: [template](../templates/GATE-REPORT.md).

## Допуск архитектуры

ADR-003 PROPOSED: до review/ACCEPTED и допуска соответствующих gates функциональная реализация (включая prototypes) запрещена. Раздельные APK не закрывают и не сбрасывают G0; existing evidence остаётся историческим. G3 дополнительно требует IPC trust/compatibility и EXT-01..11; H-RELEASE — обычный Hermes; G8 — add-on/companion и совместимость с Hermes.

## Два независимых направления выпуска

**H-RELEASE (NOT_RUN)** — допуск конкретного обычного Hermes APK: собственные build/unit/style/lint,
release/R8/signing, UI/migrations и regression проверки; при наличии generic broker — IPC security,
compatibility и отсутствие дополнения с fake client. Требуется review evidence этого release candidate.
Он не зависит от исторического общего G0, donor build, CXR repositories или G1–G8 Rokid.
Результаты Hermes из G0 можно приложить как evidence, но не автоматически присвоить PASS.

**G8 (Rokid)** — выпуск add-on + glasses companion и проверка их совместимости с уже независимо
проверенной версией Hermes. Сохраняет G0–G7, hardware/pilot и owner acceptance. Общий G0 остаётся
BLOCKED; его разделение для выпуска core не закрывает T00. Полный комплект содержит три APK,
но выпуск дополнения не требует повторного выпуска неизменённого Hermes.

## Граф зависимостей (направление Rokid; H-RELEASE независимо)

```text
G0 Baseline
 ├── G1 Real glasses compatibility ─┐
 └── G2 Secure backend contract ────┤
                                   ↓
                          G3 Minimal vertical slice
                                   ↓
                          G4 Shared conversation/context
                                   ↓
                          G5 Incremental glasses output
                                   ↓
                          G6 Local OCR/translation
                                   ↓
                          G7 Failure recovery/pocket test
                                   ↓
                          G8 Pilot/release/rollback
                                   ↓ optional new scope
                          G9 Push-to-talk and video research
```

После G0 и ACCEPTED ADR-003 по допуску task разрешены mock-based development и анализ контрактов параллельно G1/G2. Глубокий перенос donor, интеграционный merge и заявление о работоспособности требуют обоих gates. Заблокированный путь не мешает независимым тестам, но не даёт фиктивный PASS.

## Матрица

| Gate | Задание | Ответственный за доказательство | Что блокируется при провале |
|---|---|---|---|
| G0 | T00 | Build/integration agent + reviewer | Любая функциональная интеграция |
| G1 | T01 | Hardware operator, owner confirms result | Аппаратная интеграция и обещание поддержки очков |
| G2 | T02 | Backend operator + reviewer | Реальный agent round-trip, выбор транспорта |
| G3 | T03 | Android agent + hardware operator | Дальнейшие сценарии поверх перенесённого кода |
| G4 | T04 | Integration agent + backend operator | Утверждение об общей сессии |
| G5 | T05 | Protocol/UI agent + hardware operator | Релиз со streaming label |
| G6 | T06 | Vision agent + owner quality review | Режим быстрого переводчика |
| G7 | T07 | QA/reliability agent + hardware operator | Повседневное использование |
| H-RELEASE | T08, core lane | Core release operator + reviewer | Выпуск обычного Hermes APK |
| G8 | T08, Rokid lane | Release operator + owner | Выпуск add-on/companion с заявленной совместимостью |
| G9 | T09 | Отдельный phase owner | Голос/видео; не блокирует уже выпущенный MVP |

## G0 — исходная точка и происхождение

Вход: доступ к репозиториям и рабочему окружению. Выход: pinned полные SHA обоих проектов; выбранная ветка/релиз и причина; версии JDK/SDK/Gradle/AGP; dependency/license inventory; baseline APK/test/lint logs и карта существующих chat/auth/persistence компонентов. Сведения о серверной версии могут остаться UNKNOWN до G2.

PASS: исходный клиент собирается воспроизводимо; базовые тесты пройдены; upstream ошибки отдельно воспроизведены и согласованы без скрытого отключения. Ссылка на чужой CI не заменяет собственную сборку. При отсутствии SDK — BLOCKED для build, разрешён документированный source audit.

## G1 — совместимость конкретных очков

Не прошивать устройство и не обходить ограничения OEM. Установка companion через поддерживаемый способ, запуск, разрешения, текст на дисплее, работа кнопок, камера и доставка JPEG на OnePlus. Hardware report фиксирует firmware и версии APK.

PASS: 20 последовательных текстовых сообщений, 10 снимков и 5 reconnect успешно проходят; для каждого снимка совпадают hash отправленного/принятого JPEG; кириллица и китайский текст читаемы; после теста сохраняются нужные штатные функции. Первый успешный кадр не заменяет весь набор. Один отказ расследовать и после исправления повторить весь набор на новой сборке.

Нужно измерить capture/transfer/display latency и определить пригодное разрешение для мелкой надписи. Медленный baseline не маскировать уменьшением до нечитаемого изображения.

FAIL: sideload/камера недоступны, соединение требует небезопасного обхода или невозможно восстановить штатное поведение. Альтернатива CXR разрешена только отдельным ADR и повторным G1. Сам факт покупки китайских очков не доказывает и не опровергает совместимость.

## G2 — безопасный доступ и реальный контракт

Подтвердить target container/profile, version/digest, process model и сети; безопасно подключить обычный Hermes Mobile. Матрица capabilities и ADR выбирают один транспорт.

PASS: login → text → image → follow-up → reload history; cross-session image isolation; stream interrupted/reconciled; cancellation и approvals проверены либо явно unavailable с безопасным поведением; unauthenticated access rejected; wrong-profile access rejected. Для обязательных возможностей `UNKNOWN` недопустим. Для необязательных достаточно проверенного отсутствия и задокументированного ограничения.

Внешне не появляются открытые 9119/8642; Telegram и остальные контейнеры проходят до/после проверки. Prod изменения выполняются только после зафиксированного допуска; без него gate остаётся BLOCKED, а patch/runbook можно подготовить.

## G3 — узкий сквозной сценарий

В форке включается Rokid, подключается устройство, выводится локальный текст. Затем фото приходит в composer того же клиента; явная отправка даёт один запрос и один ответ агента на телефон/очки. Никакого переносимого donor LLM stack.

PASS: минимальный slice повторён 10 раз; фото не теряется, нет двойной отправки; выключение feature останавливает сервис; общий Hermes APK проходит regression без дополнения. Companion и phone protocol совместимы; LICENSE/NOTICE сохранены.

## G4 — один разговор и визуальный контекст

PASS: фото A → текстовый вопрос с телефона → фото B → сравнение A/B в том же разговоре; подтверждена передача изображений backend, а не только текстового OCR. Повторить после process death телефона и серверного восстановления в разрешённом тестовом окружении. Отдельный разговор не получает A/B. Смена профиля во время upload не отправляет данные новому профилю.

Проверять техническую привязку истории, media и IDs, а не только правдоподобность ответа модели. Стратегия повторной подачи старых изображений, если backend их не сохраняет, требует отдельного ADR без создания дублей user turn.

## G5 — поток на дисплее

PASS: промежуточный текст виден до завершения ответа; порядок и revisions устойчивы к duplicate/gap/out-of-order событиям; reconnect получает snapshot; пользователь на второй странице не возвращается на первую при каждом обновлении. Срабатывают terminal/error/cancel states. Final frame доставляется или честно отмечается как не доставленный.

Проверять 20 fixtures: короткие/длинные ответы, CJK/кириллица, Markdown/code, late events и две сессии. Transport-level ACK и подтверждение отрисовки различаются.

## G6 — локальный переводчик

PASS: установленный Chinese OCR + RU translation работают без интернета; минимум 40 зафиксированных тестовых изображений с результатами качества; отсутствующие модели дают setup action, не скрытый cloud fallback. Fast path работает при полностью недоступном VPS; в local-only режиме сеть не получает фото/текст. Thresholds — в TEST-PLAN.

Музейный агент не задерживает вывод и не заменяет короткий перевод длинным ответом без выбранного display mode. Сам OCR не считается переводом.

## G7 — надёжность и безопасность

PASS: failure matrix пройдена; ноль выявленных повторных side effects и cross-profile leaks; unknown outcome не уходит в автоматическую повторную отправку. 60-минутный pocket test, минимум 30 операций, раздельно учитывает local translation, display и новые запросы по [TEST-PLAN](09-TEST-PLAN.md). Для новых запросов экран включается и отправка подтверждается в Hermes; истёкший grant не продлевается автоматически. Force-stop корректно требует ручного возвращения, а не обещает самовосстановление.

Battery/thermal baseline и активный режим измерены в повторяемых условиях. Нет crash/ANR, неограниченных очередей, нескончаемого mic/camera usage и сетевых retry storms.

## G8 — выпуск Rokid add-on и companion

PASS: пройдены G0–G7 и H-RELEASE выбранной версии Hermes, нет открытых critical/high рисков, проведён пилот в двух отдельных пользовательских сессиях, владелец принял UX/качество/расход батареи. Есть два подписанных артефакта Rokid (add-on и companion) и независимо допущенный H-RELEASE Hermes APK — три APK в полной compatibility matrix, checksums, license notices, release notes, SBOM/dependency list и проверенный rollback без потери baseline чатов.

Не объявлять APK совместимым со всеми Rokid: support statement содержит только реально проверенные модель/firmware/Android и серверную версию.

## G9 — последующий голос/видео

Отдельные критерии в [11-VOICE-VIDEO-LATER](11-VOICE-VIDEO-LATER.md). Push-to-talk, STT, TTS и непрерывное видео не закрываются одним общим «голос работает». Не блокировать MVP исследованиями видеостриминга.

## Правило изменения критериев

Любое снижение порога, исключение теста или замена hardware теста emulator тестом оформляется ADR с причиной, последствиями и подтверждением владельца. Ранее успешные gate reports привязаны к commit/build/firmware: существенная смена этих входов требует повторить затронутые проверки.
