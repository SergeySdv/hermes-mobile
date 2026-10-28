# 10. Сборка, релиз, эксплуатация и rollback

## Репозитории и происхождение

Форк Hermes Mobile — основной продукт. Donor-файлы фиксируются по полному commit SHA, а не по moving `main`; для каждого перенесённого файла записать origin path, license/NOTICE и локальные изменения. В hardware spike исходный donor build остаётся отдельно, чтобы можно было отделить проблему прошивки от ошибки переноса.

В репозитории хранить source и маленькие synthetic fixtures, не реальные музейные фото, APK, модельные weights, serial devices, `.env` и signing keys. Сохранить upstream LICENSE/NOTICE; закрытый CXR SDK и ML Kit/model licensing проверить отдельно, не считать их покрытыми лицензией donor. Для распространения нужен third-party notices документ и проверка attribution.

## Build matrix

Сначала прочитать текущие wrapper/version catalog/build files обоих репозиториев. Не предполагать одинаковые Gradle/AGP/Kotlin/SDK; не обновлять весь проект ради одного library conflict.

Предлагаемые outputs:

| Output | Назначение | Особенности |
|---|---|---|
| Hermes debug/release | Один общий основной APK | Без Rokid SDK/protocol/permissions; независимый CI/release |
| Optional Rokid add-on debug/release | Отдельный package/process | Свой CI/signing/licenses, IPC compatibility |
| Glasses companion debug/release | Hardware runtime | Ограниченные permissions, без cloud keys |

Rokid flavor основного APK не создаётся. Имена add-on tasks являются предложением, не уже существующими командами. После G3 фиксируются реальные команды в build report. В CI сохраняются исходные проверки Hermes Mobile, добавляются protocol unit/contract tests и compatibility fixtures. Минимум: compile, unit, lint/style, Android UI/migrations, release compilation/R8, dependency/secret scan.

Эмуляторные тесты не помечаются как hardware tests. Build скриншота UI недостаточно для APK acceptance. Не отключать R8 целиком ради reflection-проблемы SDK; минимальные keep rules с тестом release APK.

## Application ID и подписи

Для private fork выбрать отдельный application ID или suffix, чтобы тестовая установка не перезаписала официальный Hermes Mobile. Это решение фиксируется до сохранения production пользовательских чатов. Companion package change проверяется на CXR/bootstrap assumptions.

Release signing key хранится у владельца/в защищённом CI secret store. Public/debug keystore upstream не использовать как доверенную production identity. После выбора production package/signature их не менять без migration strategy. Несовпадение подписей не исправлять удалением пользовательских данных.

Для каждого из трёх независимо выпускаемых APK публиковать: versionCode/versionName, SHA-256, signer certificate fingerprint, protocol major/minor, source SHAs и проверенную compatibility matrix. Stable pairing проверяется и после обновления только одной стороны; incompatible версии дают безопасное предупреждение.

## Независимость основного клиента

CI/release Hermes не зависит от сборки, Maven repositories, proprietary SDK или hardware tests Rokid. Обязательны собственный regression suite, при наличии broker — generic IPC tests с fake client и применимые core-проверки EXT-01..11. Их аппаратные варианты не являются prerequisite core release. Add-on и companion имеют отдельные workflows/артефакты. Integration lane проверяет версии N/N-1 и fail-closed несовместимость; сбой add-on не блокирует обычный релиз core. Security regression broker блокирует core release. Детали trust/signers/rollback: [ADR-003](../adr/ADR-003-OPTIONAL-ANDROID-ADDON.md).

Обычный Hermes выпускается по H-RELEASE (пока NOT_RUN), независимо от общего G0 BLOCKED. Add-on/companion — по G8 с G0–G7 и H-RELEASE выбранной версии Hermes. Новые допуски не наследуют PASS из прежних проверок; критерии определены в [GATES](02-GATES.md).

## Release candidates направления Rokid

RC0: локальные текст/камера/transfer, без cloud permissions сверх необходимого.
RC1: полноценный photo/chat/context, streaming.
RC2: OCR/translation и reliability campaign.
RC3: 2 пользовательских пилотных сессии, release-signed APK, rollout/rollback.

RC labels — не замена gates. Любой critical/high defect, неизвестная утечка scope, unsafe retry или потеря baseline data блокирует G8. Cosmetic/low issues могут быть приняты владельцем с явным списком.

## Rollout

Сначала staging/test conversation, затем выбранный реальный профиль. Установка телефона не должна менять Telegram config. Очки включаются opt-in, local-only режим доступен до server upload. Owner получает короткую инструкцию: включение, закрепление чата, перевод и подтверждение draft в Hermes (auto-guide вне IPC v1), остановка, очистка дисплея, diagnostics и ограничения.

Не выпускать broad claim «поддержка всех Rokid». Support matrix перечисляет реально проверенные модель/firmware, phone OS, core IPC × add-on × companion compatibility matrix и Hermes version. При обновлении серверного контракта capability tests повторяются; отсутствующие methods становятся unsupported, а не вызывают молчаливые fallback turns.

## Rollback

Code rollback: отключение/откат add-on независимо от Hermes; предыдущие совместимые APK по matrix. Если Android не позволяет downgrade versionCode, подготовить rollback build с прежним кодом, прежней подписью и повышенным versionCode. Не рекомендовать `uninstall` как стандартный способ отката.

Data rollback: Room migration forward-compatible там, где возможно; backup/export до upgrade и tested restore. Нельзя открывать новую схему старым APK без явно проверенной совместимости. Не использовать destructive migration, очищающую существующие чаты.

Server rollback: только отдельный approved patch из VPS runbook; сохранение новых сообщений и state DB важно. Возврат old config не означает необходимость отката всех данных. Не останавливать другие контейнеры.

## Эксплуатационные состояния

Показывать версии телефона/очков/protocol/server, последний connect/reconcile, понятную причину offline. Diagnostics export redacted и opt-in. Обозначать отдельно «сервер ответил», «ответ сохранён», «доставлен на очки», «очковый UI применил frame».

Удаление pairing очищает локальные pairing credentials/device references add-on и прекращает соединение, но не удаляет чаты без отдельного подтверждения. Отключение приложения на очках освобождает camera/mic; штатное приложение должно продолжать работать в проверенном режиме.

## Финальный deliverable реализации

Core lane: один Hermes APK с собственными hashes/signing/evidence. Rokid lane: add-on и companion плюс проверенная версия Hermes (три APK в полном комплекте), hashes, source/patch links, origin/license inventory, capability matrix, passed gate reports G0–G8 для Rokid и H-RELEASE выбранного Hermes, установочная инструкция, release notes, known limitations и rollback runbook. Этот Markdown-пакет сам по себе не является ни одним из этих готовых APK и не подтверждает их тестирование.
