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

The project is a fresh Android Studio template: AGP 9.5 (built-in Kotlin support, no separate
`kotlin-android` plugin), compileSdk/targetSdk 37, minSdk 28, Java 11, version catalog in
`gradle/libs.versions.toml`. Compose, Room, Navigation Compose, Coil, lifecycle/ViewModel and KSP
are **not yet added** — add them through the version catalog when the corresponding feature is built.
There is no `MainActivity` yet.

## Architecture (target, per spec)

- Kotlin + Jetpack Compose + Material 3, MVVM, package `com.example.healthmate`, feature-based packages:
  `data/local`, `data/repository`, `domain`, `ui/{theme,components,navigation,dashboard,records,addedit,detail,search}`.
- Strict layering: Screen → ViewModel → Repository → DAO. Screens never touch the DAO.
- Each ViewModel exposes one immutable `StateFlow<UiState>` plus event functions (one-way data flow).
- DAO returns `Flow`, so all screens refresh automatically when the database changes.
- Room table `health_records`: `date` stored as `yyyy-MM-dd` TEXT and unique (one record per day);
  optional `photo_path` points to a selfie copied into app-private storage (picked via Android Photo
  Picker, loaded with Coil). Deleting a record must also delete its photo file.
- ~90 days of dummy data are seeded once, on database creation, ending yesterday.
- Shared composables (StatCard, RecordCard, PrimaryButton, etc.) live in `ui/components`; colours and
  stat-colour mapping are defined in the spec's Design System section — keep the palette to those values.
