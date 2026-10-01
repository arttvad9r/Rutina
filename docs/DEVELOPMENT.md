# Разработка Рутины

## Требования

JDK 17, Android SDK 36 и настроенный путь к SDK (`local.properties` или `ANDROID_HOME`).
Gradle запускается через wrapper из репозитория.

## Сборка и проверки

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleRelease
```

APK появляются в `app/build/outputs/apk/debug/` и `app/build/outputs/apk/release/`.
В проекте 63 модульных теста: календарь и серии, сроки курсов, напоминания,
разбивка доз кофеина, расчёты времени и проверка данных резервной копии. Результаты проверки версии 1.10 описаны
[в отчёте импорта и экспорта](data-backup/VERIFICATION.md).

Стек: Kotlin 2.1, Jetpack Compose (Material 3), Room. `minSdk 26`, `targetSdk 36`,
`compileSdk 36`. В release включён R8.

## Подпись release APK

Ключ хранится вне Git, в `keystore/rutina-release.jks`. Настройки читаются из
`keystore/keystore.properties`: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
Папки `keystore/` и `dist/` исключены из Git.

Для обновлений нужен тот же ключ, которым подписана установленная версия.
Сохраняйте ключ и его пароли отдельно от репозитория.

**В текущей конфигурации при отсутствии release-ключа сборка использует debug-подпись.**
Перед публикацией проверьте сертификат через `apksigner verify --print-certs`.
Такую сборку нельзя выдавать за обновление APK, подписанного release-ключом.

## Установка через adb

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release и debug используют один application ID, но разные ключи подписи.
Устанавливать обновление нужно поверх сборки того же типа.

## Разрешения и напоминания

- `POST_NOTIFICATIONS` — уведомления привычек и таймера.
- `USE_EXACT_ALARM` (Android 13+) / `SCHEDULE_EXACT_ALARM` (Android 12) — точные будильники.
- `RECEIVE_BOOT_COMPLETED` — восстановление будильников после перезагрузки.

Интернет-разрешения нет. Данные хранятся в SQLite через Room.
Если прошивка ограничивает напоминания, проверьте разрешения уведомлений,
автозапуск и настройки энергосбережения приложения.
