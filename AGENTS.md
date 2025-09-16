# Repository Guidelines

## Project Structure & Module Organization
The Android application is contained in the single `app/` module. Production code lives in `app/src/main/java/com/example/roonplayer`, grouped by feature domains such as `network/` and `api/`; UI resources are in `app/src/main/res`. Static launch assets and release builds are stored under `app/release`. Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/`) enable consistent builds, and project-wide configuration sits in `build.gradle`, `settings.gradle`, and `gradle.properties` at the root.

## Build, Test & Development Commands
Use `./gradlew assembleDebug` to build a debuggable APK and `./gradlew installDebug` to push it to a connected device or emulator. Run unit tests with `./gradlew testDebugUnitTest`, and execute instrumented UI tests with `./gradlew connectedDebugAndroidTest` (requires a device). `./gradlew lint` runs Android Lint; fix issues before publishing. Android Studio users should sync the project before running these tasks to download dependencies.

## Coding Style & Naming Conventions
Code is Kotlin-only; keep files Kotlin 1.9 compatible. Follow Android Studio’s default formatter: four-space indentation, trailing commas disabled, and imports organized alphabetically with AndroidX before project packages. Use PascalCase for classes (`SmartConnectionManager`), camelCase for functions and properties, and SCREAMING_SNAKE_CASE for constants (see `MainActivity#companion object`). Resource files should follow Android naming (`activity_main.xml`, `ic_play.xml`). Prefer `val` over `var`, wrap long parameter lists across lines, and include concise KDoc when exposing new APIs.

## Testing Guidelines
Add unit tests under `app/src/test/java` and instrumentation tests under `app/src/androidTest/java`. Mirror the production package path so helpers like `com.example.roonplayer.network` stay aligned. Name test classes with the `*Test` suffix and describe cases with `@Test` methods using backticked strings if sentences aid readability. Aim to cover new network flows and caching logic, and document any untestable hardware dependencies in the pull request.

## Commit & Pull Request Guidelines
Use imperative, scope-prefixed commit subjects (example: `feat: add reconnection watchdog`). Keep bodies wrapped at 72 characters and note any follow-up TODOs. Pull requests should include: a summary of changes, screenshots or screencasts if UI is touched, reproduction steps for bug fixes, and references to GitHub issues or linear tasks. Confirm CI or local test runs in the description and call out areas needing focused review.

## Configuration & Environment
Copy `local.properties.example` to `local.properties` with your Android SDK path before building. Do not commit real keystores; the placeholder assets in `app/release` are for local debugging only. When pointing the app at a Roon Core, store secrets in Android’s encrypted preferences or development-only config files that stay out of version control.
