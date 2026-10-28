# T00 — source / toolchain / license inventory

Дата фиксации: 2026-10-27. Основной fork: `https://github.com/SergeySdv/hermes-mobile.git`.
Upstream: `https://github.com/Hy4ri/hermes-mobile.git`.

| Input | Pin / причина |
|---|---|
| Hermes | `f03426df599d36b003b10ce29516434e591aef82`, main, commit 2026-10-26. Текущий checkout совпал с upstream main по `git ls-remote`; не подменялся историческим snapshot плана. |
| Donor | `https://github.com/zero2005x/RokidAIAssistant.git`, main, `c2238f3460ab09e57114b0817eb8f36d2ee4b7bf`, commit 2026-10-26. Актуальный clone на дату аудита; исторический 770bc8d не использован. |
| План | Локальный незакоммиченный `hermes-rokid-implementation-plan/`; указанный пользователем `docs/rokid-plan/START_HERE.md` отсутствует. Пакет не переносился и не переписывался. |
| Hermes build checkout | `/tmp/hermes-t00-baseline`, detached worktree исходного checkout; дополнительно upstream bare clone `/tmp/hermes-t00-upstream.git` подтверждает тот же SHA. |
| Donor checkout | `/tmp/hermes-t00-donor`, независимый clone/worktree. Никакие donor source files не перенесены в Hermes. |
| Рабочая ветка отчёта | `docs/t00-baseline`; main не изменяется. |

## Toolchain

| | Hermes | Donor |
|---|---|---|
| Gradle wrapper | 9.8.2 | 9.6.1 |
| AGP | 9.4.1 | 9.0.0 |
| Kotlin | 2.4.21 | 2.2.10 |
| KSP | 2.3.12, standalone | 2.2.10-2.0.2 |
| Java bytecode / toolchain | 21 (`jvmToolchain(21)`); baseline Gradle provisioned Temurin 21.0.12.1+1, повтор с доступным Temurin 21.0.13+9 | bytecode 17, daemon 21 |
| Daemon | Первая попытка OpenJDK 22.0.2; style/retry Temurin 21 | pinned daemon 21; первая попытка с JDK 22 не смогла найти JDK 21; retry Temurin 21.0.13+9 |
| SDK | compile/target 37, min 26; Build Tools 37.0.0 | compile 36; phone target 36, glasses target 34; min 28 |
| Room | 3.0.3, SQLCipher 4.19.1 | 2.8.4, отдельный donor chat store не переносить |
| Modules / variants | `:app`, debug/release | `:common`, `:phone-app` github/play × debug/release, `:glasses-app` debug/release |

Host macOS arm64; SDK найден через local.properties в `$HOME/Library/Android/sdk`.
До запуска: platforms android-35/android-37.0; Build Tools 35.0.0/35.0.1/36.0.0.
Gradle установил недостающие компоненты по ранее принятым SDK licenses; это локальный
bootstrap, не установка APK. ANDROID_HOME задавался только процессам проверок.
Локальный untracked `gradle/gradle-daemon-jvm.properties` пользователя задаёт JDK 25:
в baseline worktree он не копировался и в исходном workspace не менялся.

Изолированный Temurin JDK скачан через Adoptium API для mac/aarch64/JDK21:
`/tmp/hermes-t00-jdk21/Contents/Home`, версия `21.0.13+9-LTS`.
SHA-256 архива: `bf036b35b071bfb2f54f2c0af8ee8d88afa95ee6eb3dce02c0f94cfc2524d04a`.
Это измеренный digest, не независимая проверка подписи поставщика.
ktlint 1.8.0 скачан из GitHub release pinterest/ktlint, `/tmp/hermes-t00-tools/ktlint`;
SHA-256 `a3fd620207d5c40da6ca789b95e7f823c54e854b7fade7f613e91096a3706d75`.
Глобальный PATH/настройки Java не менялись. Gradle дополнительно provisioned JDK 21.0.12.1+1
в своём cache для `jvmToolchain(21)` первого запуска; daemon JDK22 не означает
исполнение unit tests под Java22.

## License / binary inventory

| Scope | Evidence | Вывод |
|---|---|---|
| Hermes source | root LICENSE, Apache-2.0; SHA-256 `fa10380ce10fdb4fc9243469d67e5ebdd5f715dc23388c814e150a7b6fd1bf2b` | Сохранять лицензию и attribution при распространении. |
| Donor source | root LICENSE Apache-2.0; SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30` | Kotlin SPDX headers; не распространять source без обязательных notices. |
| Donor attribution | NOTICE; SHA-256 `ddac5bc236cb7bf659bf3b5f433b5aa1a3e6bb8b74839905bfd8b7ca5cdbe834` | Copyright 2026 Mazda Payrang; сохранить NOTICE при будущем переносе. |
| CXR phone | `githubImplementation("com.rokid.cxr:client-m:1.2.2")` | NOTICE прямо исключает proprietary SDK из Apache-2.0. Условия использования/redistribution не установлены; не считать автоматически разрешённым перенос. Play flavor его исключает. |
| CXR glasses | `com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45` | Разрешение распространения и транзитивный состав UNKNOWN. Отсутствие потребности для Camera2 не доказывает возможность убрать bridge из всей программы. |
| Прочие Gradle зависимости | pinned catalogs и build.gradle.kts обоих SHA | AndroidX/Compose, Kotlin/coroutines, Gson/serialization, OkHttp/Retrofit, Room, SQLCipher, Coil; donor дополнительно Google Generative AI. Полный resolved SBOM / transitive licenses не подтверждены до dependency resolution. Нельзя присваивать всем лицензию корневого репозитория. |
| Bundled native/model files | Source scan `.aar/.so/.onnx/.tflite/.pb/.bin` вне build/.gradle/.git: 0 в обоих checkout | Не исключает runtime/model download или native dependencies из Maven. Donor build подтвердил упаковку libcaps.so, libcxr-bridge-jni.so, libcxr-sock-proto-jni.so, libcxr-sock-rfcomm-jni.so, libflora-cli.so, libmutils.so (strip warning); лицензии этих бинарников не установлены. Wrapper JAR присутствует в обоих. APK/models/фото в отчёт не добавляются. |
| OCR/translation | Новых зависимостей нет | Выбор моделей, доставка, offline readiness и условия — UNKNOWN / G6, не часть T00. |

## Package/signature strategy (DESIGN, без установки)

Hermes уже отделяет debug package `com.m57.hermescontrol.dev` от release
`com.m57.hermescontrol`. Для будущего стенда использовать этот debug variant и
его repository debug keystore; release ключи не читать/не менять. Совпадение
package с ранее установленным debug требует проверки signer перед install;
не удалять прежний APK/данные для обхода signature mismatch.

Donor github phone: `com.example.rokidphone`, play phone:
`io.github.zero2005x.glassesaicompanion`, glasses: `com.example.rokidglasses`.
У donor debug нет отдельного applicationIdSuffix: возможен конфликт с релизом.
Перед G1 владелец отдельно утверждает package/signature/backup/rollback план.
Для будущего форка companion предпочтителен отдельный test applicationId и
отдельный test signer после проверки, что protocol/SDK не привязаны к package.
Это предложение, не сделанное изменение Gradle.

## Повторение

Checkout именно полных SHA выше; не `git pull` moving main перед повтором.
Использовать JDK 21, ANDROID_HOME, ktlint 1.8.0 в PATH; SDK components согласно таблице.
Точные запущенные команды и завершённые exit codes перечислены в G0-baseline.
Release signing secrets для compileReleaseKotlin не нужны; release APK не собирался.
Instrumented tasks автоматически устанавливают APK, поэтому в этой сессии не запускаются.

Повторный upstream worktree: `/tmp/hermes-t00-reproduce`, detached на том же SHA,
создан из независимого bare clone. Общий Gradle cache сохранён; cold/offline reproducibility
не проверялась. Два debug APK имеют одинаковый digest (см. artifacts.json).
