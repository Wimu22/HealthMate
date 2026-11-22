# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

HealthMate is a native Android health & wellness tracker (university coursework): users record daily
steps, calories burned and water intake, then review, search, edit and delete them. Fully offline,
local storage only.

**`docs/design-spec.md` is the source of truth** for design, architecture, database schema, screens,
validation messages, navigation and the build plan. Read the relevant section before implementing anything.

## Working rules

- Always follow `docs/design-spec.md` for design, architecture and features. If something is not covered
  or seems to conflict, ask before deviating.
- Work on one feature at a time.
- After each feature:
  1. Build the app to check it compiles (`./gradlew assembleDebug`).
  2. Commit with a clear message describing the change (e.g. "Add Room entity and DAO for health records").
  3. Push to GitHub (`git push origin master`).
- After each feature, explain in simple words what was built and why, so the user can understand and
  defend it in their viva.

## Commands

Run from the repo root (use `./gradlew` in Git Bash, `.\gradlew.bat` in PowerShell):

```
./gradlew assembleDebug                 # build debug APK
./gradlew testDebugUnitTest             # JVM unit tests
./gradlew testDebugUnitTest --tests "com.example.healthmate.SomeTest"            # single test class
./gradlew testDebugUnitTest --tests "com.example.healthmate.SomeTest.methodName" # single test
./gradlew connectedDebugAndroidTest     # instrumented tests (Room DAO tests) – needs device/emulator
./gradlew lintDebug                     # Android lint
```

## Current state of the build

AGP 9.5 (built-in Kotlin support, no separate `kotlin-android` plugin), compileSdk/targetSdk 37,
minSdk 28, Java 11. All dependencies go through the version catalog `gradle/libs.versions.toml`.
Room + KSP, Compose (BOM) + Material 3 + icons-extended, Coil, Navigation Compose and
lifecycle-viewmodel-compose / lifecycle-runtime-compose are set up. `MainActivity` (edge-to-edge,
`HealthMateTheme`) is the launcher activity and hosts `ui/navigation/HealthMateNavHost`.
Routes are string-based, defined in the sealed `Screen` class (`ui/navigation/Screen.kt`); screens get
navigation lambdas, never the NavController. Unit tests that touch `viewModelScope` use `MainDispatcherRule` (test sources).

**Kotlin version constraint:** AGP 9.5.1 bundles Kotlin 2.2.10, so the compiler can only read
libraries built with Kotlin ≤ 2.3. A library whose pom depends on `kotlin-stdlib` 2.4+ (e.g. Coil
3.5+) fails with "Module was compiled with an incompatible version of Kotlin". Check a new
library's stdlib version before adding it. The Compose compiler plugin version (`kotlin` in the
catalog) must equal the bundled Kotlin version.

## Architecture (target, per spec)

- Kotlin + Jetpack Compose + Material 3, MVVM, package `com.example.healthmate`, feature-based packages:
  `data/local`, `data/repository`, `domain`, `ui/{theme,components,navigation,dashboard,records,addedit,detail,search}`.
- Strict layering: Screen → ViewModel → Repository → DAO. Screens never touch the DAO.
- Each ViewModel exposes one immutable `StateFlow<UiState>` plus event functions (one-way data flow).
- No DI library: each ViewModel has a `companion object { val Factory }` (`viewModelFactory { initializer { … } }`)
  that builds its repository from `HealthMateDatabase.getInstance(app)`. Inject "today" as a
  `() -> LocalDate` parameter so date logic is testable (see `DashboardViewModel`).
- DAO returns `Flow`, so all screens refresh automatically when the database changes.
- Room table `health_records`: `date` stored as `yyyy-MM-dd` TEXT and unique (one record per day);
  optional `photo_path` points to a selfie copied into app-private storage (picked via Android Photo
  Picker, loaded with Coil). Deleting a record must also delete its photo file.
- ~90 days of dummy data are seeded once, on database creation, ending yesterday.
- Shared composables (StatCard, RecordCard, PrimaryButton, etc.) live in `ui/components`; colours and
  stat-colour mapping are defined in the spec's Design System section — keep the palette to those values.
