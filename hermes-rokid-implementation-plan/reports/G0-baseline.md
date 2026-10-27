# G0 / T00-BASELINE

Status: BLOCKED
Дата: 2026-10-27. Оператор: Codex. Reviewer: NOT_RUN (владелец/интегратор).
Только исходная сборка, source audit и отчёт; функциональная интеграция не начата.

## Входы и evidence

- [Source/toolchain/license inventory](G0-inventory.md).
- [Карта компонентов и integration seams](G0-component-map.md).
- Sanitized logs: [evidence/G0](evidence/G0/).
- Hermes SHA: `f03426df599d36b003b10ce29516434e591aef82`.
- Donor SHA: `c2238f3460ab09e57114b0817eb8f36d2ee4b7bf`.
- Hermes debug APK: `com.m57.hermescontrol.dev`, versionCode 1, versionName `1.0-dev-dev`.
  Два checkout дали SHA-256 `c01e1e7eb65103e3fec05671520b403b22c93b268f36ef56a10bafa20ab51de8`;
  signer SHA-256 `c3053abf8d6f71fea74c0f2c9ae7dc693a6a08e3fa8b1a09c3763a52495f49f0`.
  `apksigner verify --print-certs` exit 0; [artifact evidence](evidence/G0/artifacts.json).
  Общий Gradle cache использовался; cold/offline build не проверялся. Donor phone github/play APK собраны и подписи проверены (exit 0);
  [donor artifacts](evidence/G0/donor-artifacts.json). Glasses APK не получен.
- Phone: OnePlus Open по плану; текущая OS/build UNKNOWN.
- Glasses model/firmware/Android: UNKNOWN. Protocol v2 — DESIGN, не реализован.
- Backend version/image digest/profile: UNKNOWN; fixture SHA не версия VPS.
- Предыдущие required gates: нет.
- Hardware/production approval: NOT_GRANTED. Для локального source/build audit: NOT_REQUIRED.

## Assumptions и тест-план

Найденный `hermes-rokid-implementation-plan/START_HERE.md` принят как искомый пакет:
`docs/rokid-plan/START_HERE.md` отсутствует. Исходное приложение проверяется на
неизменённом SHA в отдельном worktree, без локального untracked JDK25 override.
Никакие исходные ошибки не исправляются до воспроизведения. Форматирование всего
репозитория намеренно не выполнялось: это изменило бы baseline.

План: assembleDebug, testDebugUnitTest, ktlintCheck + checkColorLiterals,
lintDebug, compileReleaseKotlin; donor github/play phone и glasses debug builds,
common/phone/glasses unit tests и lint. Instrumented tests NOT_RUN: они устанавливают
APK, а допуск на установку в этой сессии не выдан. Аппаратные/backend тесты не заменяются моками.

## Запущенные команды

Рабочий каталог Hermes: `/tmp/hermes-t00-baseline`; ANDROID_HOME указывал на локальный
SDK. Первый запуск с OpenJDK 22.0.2:

```sh
./gradlew assembleDebug testDebugUnitTest ktlintCheck checkColorLiterals lintDebug compileReleaseKotlin --continue --console=plain
```

Первый PATH не содержал ktlint: `:app:ktlintCheck` завершился `command not found`.
Это ошибка окружения, не ошибка Kotlin style; `--continue` оставил остальные проверки
работать. После загрузки ktlint 1.8.0 была попытка отдельного запуска с JDK21:

```sh
./gradlew ktlintCheck checkColorLiterals --continue --console=plain
```

Она завершилась exit 1 по configuration-cache lock: первый Gradle ещё работал.
Это ошибка организации запуска оператора, не дефект приложения. Проверка затем повторена в независимом upstream worktree `/tmp/hermes-t00-reproduce`:
exit 0, `BUILD SUCCESSFUL`, обе задачи executed, ktlint 1.8.0.

Первый `compileReleaseKotlin` упал с `OutOfMemoryError: GC overhead limit exceeded`.
Повтор в upstream worktree на JDK21 с `--max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx4g`:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug compileReleaseKotlin --continue --console=plain --max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx4g
```

Повтор завершился exit 0 за 8m59s: debug build, unit tests, lint и release Kotlin compilation PASS.
Это настройка процесса сборки, source и gradle.properties не изменены.

Donor, `/tmp/hermes-t00-donor`:

```sh
./gradlew :phone-app:assembleGithubDebug :phone-app:assemblePlayDebug :glasses-app:assembleDebug :common:testDebugUnitTest :phone-app:testGithubDebugUnitTest :glasses-app:testDebugUnitTest :phone-app:lintGithubDebug :glasses-app:lintDebug --continue --console=plain
```

Первый запуск exit 1: daemon требует Java 21, локально не обнаружен, download URL
для macOS aarch64 не задан. Повтор — с изолированным Temurin 21.0.13+9,
без изменения source/toolchain constraints donor. Повтор с JDK21 остановлен оператором SIGINT после >10 минут ожидания dependency resolution
(exit 130, это не compiler failure). Следующий запуск той же группы целей убрал
`--continue` и добавил `-Dorg.gradle.internal.http.socketTimeout=15000
-Dorg.gradle.internal.http.connectionTimeout=15000`; repositories не менялись.
Этот запуск завершился exit 1 за 3m31s: Google Maven не отдал
`androidx.compose.foundation:foundation-android:1.10.1` (Read timed out).
Финальный повтор использовал socketTimeout=60000, connectionTimeout=15000;
исходники и repositories остались прежними. Повтор остановлен оператором SIGINT
после >11 минут (exit 130): phone github/play APK собраны, glasses assemble и
unit/lint не завершены. Thread dump показал HTTP content read в dependency resolution;
это незавершённый запуск, а не доказанный compiler failure. Новые повторы не запускались.
Play unit/lint отдельной целью
не запускались; full variant coverage не заявляется.

Дополнительно, Hermes:

```sh
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
```

Exit 0: 34 tests, 0 failures. Это тесты CI/release tooling, не Android regression gate.

## Результаты Hermes

| Проверка | Первый checkout | Независимый повтор того же SHA |
|---|---|---|
| assembleDebug | PASS | PASS; идентичный SHA-256 APK |
| testDebugUnitTest | PASS: 4085 tests / 361 suites | PASS: 4085 tests / 361 suites |
| Failures / errors / skipped | 0 / 0 / 0 | 0 / 0 / 0 |
| ktlintCheck | FAIL: ktlint отсутствовал в PATH | PASS после bootstrap 1.8.0 |
| checkColorLiterals | PASS | PASS |
| lintDebug | PASS: 274 Warning, 15 Hint, 0 Error/Fatal | PASS: 272 Warning, 15 Hint, 0 Error/Fatal |
| compileReleaseKotlin | FAIL: compiler heap OOM | PASS с heap 4 GiB и max-workers=2 |
| CI/release Python tests | PASS: 34 tests | Не повторялись |
| Instrumented / hardware / VPS | NOT_RUN | NOT_RUN |

Разница двух lint warnings относится только к GradleDependency; прочие категории
совпадают. Это не clean-lint заявление. Counts: [test-summary.json](evidence/G0/test-summary.json).
Ошибки первого запуска сохранены; никакие tests/checks не отключались.

## Итог G0 и donor

Hermes baseline PASS по выполненным build/unit/style/lint проверкам. Общий G0 BLOCKED:
полная donor baseline не завершена; reviewer NOT_RUN. Это не заявление о дефекте
исходников donor: воспроизведены ограничение окружения JDK и сетевой таймаут,
последний запуск остановлен оператором, без итогового Gradle SUCCESS/FAILURE.

| Donor check | Результат |
|---|---|
| phone assembleGithubDebug / assemblePlayDebug | PASS, два APK с проверенной подписью |
| glasses assembleDebug | BLOCKED: Kotlin/Java compiled, финальный APK не получен |
| common / phone github / glasses unit tests | NOT_RUN до результата: подготовка начата, итоговых XML нет |
| phone github / glasses lint | NOT_RUN |
| Повторный checkout donor build | NOT_RUN; воспроизведение подтверждено только для Hermes |
| Device/backend | NOT_RUN, допуск отсутствует |

Для снятия blocker: на тех же SHA и JDK21 завершить указанные donor goals при
стабильном доступе к Maven, проверить test XML/lint и повторяемость; затем review.
Не заменять repositories, SDK или source для маскировки ошибки.

## Изменённые файлы и внешние действия

Добавлены только три отчёта `G0-baseline.md`, `G0-component-map.md`, `G0-inventory.md`
и sanitized evidence в `reports/evidence/G0/`. Ветка `docs/t00-baseline`.
`STATUS.md` не менялся: T00 требует review перед обновлением статуса.
Исходники приложения, manifests, permissions, Gradle и Room schemas не менялись.
Предсуществующие untracked файлы пользователя не добавлялись/не удалялись.

Локально: cloned donor/upstream, detached baseline worktree, downloaded JDK/ktlint,
Gradle SDK/dependency cache. Ни VPS, ни ADB, ни установки/удаления APK, pairing,
запросов к моделям или отправки фото не было.

## Блокеры / UNKNOWN и конфликты

1. Hermes baseline воспроизведён. Завершённый donor build/test/lint и reviewer decision
   пока отсутствуют; успешная сборка Hermes не доказывает сборку donor.
2. `SettingsViewModel.logout():462`: clear credentials до disconnect против порядка
   AGENTS. Нужны review и отдельный минимальный regression fix; T00 не меняет auth.
3. `RawRpcParamsRatchetTest.BASELINE:316`: четыре clarify.respond site против
   требования пустого baseline. Не обнулять вслепую и не расширять исключения для очков.
4. План предполагает общий chat repository; фактически orchestration в ChatViewModel,
   persistence repository отдельный. Это архитектурное ограничение будущего seam,
   а не обоснование переписывать чат или давать сервису второй socket.
5. Min SDK donor 28 против Hermes 26; common Android-dependent; разные AGP/Kotlin/Room.
   Перенос модулей целиком поднимает compatibility риск. Решение — после hardware evidence.
6. CXR license/redistribution, required SDK/device credentials и resolved transitive
   licenses UNKNOWN. Apache-2.0 root не покрывает CXR.
7. Real glasses/OS/firmware, Camera2/SPP/Cyrillic, backend capability/ownership,
   atomic attach+submit, cancellation/replay/media retention UNKNOWN — G1/G2, не T00.

## Rollback

В product source diff отсутствует: возвращать данные/Room/firmware/server не требуется.
Удаление только добавленных report files отменяет документационное изменение; не
удалять весь незакоммиченный пакет плана пользователя. Worktrees удалять штатным
`git worktree remove` после сохранения evidence; downloaded tools в `/tmp` можно удалить.
Общий SDK/cache не откатывать удалением каталогов: их могут использовать другие проекты.
Никакие пользовательские app data не менялись; device rollback не проверялся.

## Следующее минимальное изменение и решение reviewer

Пока G0 не принят, следующее разрешённое задание — завершение T00 на этих же SHA,
снятие конкретных build blockers и повтор baseline checks. Не начинать T03.
Первый кандидат изменения production code — отдельный узкий fix порядка logout
(отключение WS перед очисткой credentials) с тестом порядка и regression auth/profile;
сначала review конфликта инструкции/кода, без расширения в перепись авторизации.

После принятого G0 — отдельно T01 hardware spike и T02 contract audit с конкретным
допуском на device/VPS действия. Минимальная будущая интеграция описана в component map:
scope-bound image intake в существующий composer pipeline, затем fake failure tests,
без новой истории, auth store и транспорта. В этой сессии не реализована.
